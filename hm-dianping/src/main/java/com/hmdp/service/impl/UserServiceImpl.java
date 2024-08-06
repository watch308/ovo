package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Result sendCode(String phone, HttpSession session, HttpServletRequest request) {

        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号错误");
        }
        String phoneExist = GET_CODE_KEY + phone;
        // 使用Redis限流
        Boolean success = redisTemplate.opsForValue().setIfAbsent(phoneExist, "yes", GET_CODE_TIME, TimeUnit.SECONDS);
        if (success != null && success) {
            // 如果上锁成功，可以继续处理请求

            String code = RandomUtil.randomNumbers(6);
            // 生成验证码到phone里
            redisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.SECONDS);
            log.debug("验证码：" + code);

            return Result.ok();
        } else {
            Long expire = redisTemplate.getExpire(phoneExist);
            return Result.fail(expire + "秒后再发送");
        }
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号错误");
        }
        // 获取手机号对应的验证码
        String serverCode = redisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (serverCode == null || !serverCode.equals(loginForm.getCode())) {
            return Result.fail("验证码错误");
        }

        User user = userMapper.selectUserByPhone(phone);
        // 新用户注册
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setNickName("user" + RandomUtil.randomString(5));
            userMapper.insertSelective(user);
        }
        // 转为DTO
        UserDTO userDTO = new UserDTO();
        userDTO.toDTO(user);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO);
        // 生成随机token
        String token = UUID.randomUUID().toString(true);
        // 转换Long id
        map.replaceAll((k, v) -> String.valueOf(v));
        // 保存登录信息到 Redis
        redisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, map);
        redisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.SECONDS);

        return Result.ok(token);
    }

    @Override
    public Result loginWithPassword(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号错误");
        }
        String remotePassword = loginForm.getPassword();
        if(StrUtil.isBlank(remotePassword)){
            return Result.fail("密码为空");
        }
        User user = userMapper.selectUserByPhone(phone);
        // 新用户注册
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setNickName("user" + RandomUtil.randomString(5));
            user.setPassword(remotePassword);
            userMapper.insertSelective(user);
            return Result.ok("注册成功，重新登录");
        }
        else if(StrUtil.isBlank(user.getPassword())){
            //老用户没有设置密码
            return Result.fail("登陆失败");
        }
        else {
            String password = userMapper.selectPasswordByPhone(phone);
            if (password == null || !password.equals(remotePassword)) {
                return Result.fail("密码错误");
            }

            // 转为DTO
            UserDTO userDTO = new UserDTO();
            userDTO.toDTO(user);
            Map<String, Object> map = BeanUtil.beanToMap(userDTO);
            // 生成随机token
            String token = UUID.randomUUID().toString(true);
            // 转换Long id
            map.replaceAll((k, v) -> String.valueOf(v));
            // 保存登录信息到 Redis
            redisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, map);
            redisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.SECONDS);

            return Result.ok(token);
        }
    }

    @Override
    public Result logout(HttpServletRequest request) {
        String authorization = request.getHeader("authorization");
        if (StrUtil.isBlank(authorization))
            return Result.fail("未登录");
        // 删除token
        redisTemplate.delete(LOGIN_USER_KEY + authorization);
        return Result.ok();
    }
}
