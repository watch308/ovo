package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Autowired
    BlogMapper blogMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    IBlogService blogService;

    @Override
    public Result queryBlog(Long id) {
        //查询blog
        Blog blog = blogMapper.queryOneById(id);
        if (blog == null) {
            return Result.fail("blog不存在");
        }
        //查询blog用户
        Long blogUserId = blog.getUserId();
        User user = userMapper.selectOneById(blogUserId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());

        //查询是否点赞
        Long userId = UserHolder.getUser().getId();
        Double score = stringRedisTemplate.opsForZSet().score(BLOG_LIKED_KEY + id, String.valueOf(userId));
        if (score != null) {
            blog.setIsLike(true);
        }
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        Double score = stringRedisTemplate.opsForZSet().score(BLOG_LIKED_KEY + id, String.valueOf(userId));
        if (score == null) {
            //未点赞
            boolean success = blogMapper.icrLikedById(id);
            if (success) {
                stringRedisTemplate.opsForZSet().add(BLOG_LIKED_KEY + id, String.valueOf(userId), System.currentTimeMillis());
            }
        } else {
            boolean success = blogMapper.dcrLikedById(id);
            if (success) {
                stringRedisTemplate.opsForZSet().remove(BLOG_LIKED_KEY + id, String.valueOf(userId));
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询blog用户
        records.forEach(blog -> {
            Long blogUserId = blog.getUserId();
            User blogUser = userMapper.selectOneById(blogUserId);
            blog.setName(blogUser.getNickName());
            blog.setIcon(blogUser.getIcon());
            // 查询是否点赞
            UserDTO user = UserHolder.getUser();
            if (user != null) {
                Long userId = user.getId();
                Long id = blog.getId();

                Double score = stringRedisTemplate.opsForZSet().score(BLOG_LIKED_KEY + id, String.valueOf(userId));
                if (score!=null) {
                    blog.setIsLike(true);
                }
            }
        });
        return Result.ok(records);
    }

    @Override
    public Result queryBlogLikes(Long id) {
        Set<String> range = stringRedisTemplate.opsForZSet().range(BLOG_LIKED_KEY + id, 0, 4);
        if (range==null||range.isEmpty()){
            return Result.ok();
        }
        List<Long> collect = range.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> users = userMapper.searchAllById(collect);

        return  Result.ok(users);
    }
}
