package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private UserMapper userMapper;
    @Override
    public Result follow(Long followId, Boolean isFollow) {
        UserDTO user = UserHolder.getUser();
        if (user==null){
            return Result.fail("未登录");
        }

        Long id = user.getId();
        Follow follow = new Follow();
        follow.setUserId(id);
        follow.setFollowUserId(followId);

        if(Boolean.TRUE.equals(isFollow)){
            //关注
            followMapper.insertSelective(follow);
        }
        else {
            //取关
            followMapper.delByUserIdAndFollowUserId(id,followId);
        }
        return Result.ok();
    }

    @Override
    public Result followOrNot(Long followId) {
        UserDTO user = UserHolder.getUser();
        if (user==null){
            return Result.fail("未登录");
        }
        int count = followMapper.countByUserIdAndFollowUserId(user.getId(), followId);
        return Result.ok(count>0);

    }

    @Override
    public Result commonFollow(Long targetId) {
        UserDTO user = UserHolder.getUser();
        if (user==null)
            return Result.fail("未登录");
        //获取共同关注ID
        List<Long> integers = followMapper.commonFollowId(user.getId(), targetId);
        if (integers==null||integers.isEmpty()){
            return Result.ok();
        }
        //获取Id对应的用户
        List<UserDTO> userDTOS = userMapper.selectAllById(integers);
        if (userDTOS==null||userDTOS.isEmpty()){
            return Result.ok();
        }
        return Result.ok(userDTOS);
    }
}
