package com.hmdp.mapper;
import org.apache.ibatis.annotations.Param;

import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface FollowMapper extends BaseMapper<Follow> {
    int insertSelective(Follow follow);

    int delByUserIdAndFollowUserId(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

    int countByUserIdAndFollowUserId(@Param("userId") Long userId, @Param("followUserId") Long followUserId);
    List<Long> commonFollowId(@Param("userId") Long userId, @Param("targetId") Long targetId);
}
