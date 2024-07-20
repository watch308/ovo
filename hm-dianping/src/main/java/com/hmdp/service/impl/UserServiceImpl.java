package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

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
    private static final int GET_CODE_TIME = 60;
    private static final long CODE_EXPIRE = 60*5;
    @Override
    public Result sendCode(String phone, HttpSession session, HttpServletRequest request) {

        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号错误");
        }

        // 使用Redis限流
        Boolean success = redisTemplate.opsForValue().setIfAbsent(phone, "exist", GET_CODE_TIME, TimeUnit.SECONDS);
        if (success != null && success) {
            // 如果上锁成功，可以继续处理请求

            String code = RandomUtil.randomNumbers(6);
            // 生成验证码到phoneCode里
            redisTemplate.opsForValue().set(phone+"Code",code,CODE_EXPIRE,TimeUnit.SECONDS);
            log.debug("验证码："+code);

            return Result.ok();
        } else {
            Long expire = redisTemplate.getExpire(phone);
            return Result.fail(expire+"秒后再发送");
        }
    }
}
