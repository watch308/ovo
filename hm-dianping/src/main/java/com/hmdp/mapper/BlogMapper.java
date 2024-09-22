package com.hmdp.mapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface BlogMapper extends BaseMapper<Blog> {
    Blog queryOneById(@Param("id") Long id);

    int updateLikedById(@Param("liked") Long liked, @Param("id") Long id);

    boolean icrLikedById(@Param("id") Long id);
    boolean dcrLikedById(@Param("id") Long id);

}
