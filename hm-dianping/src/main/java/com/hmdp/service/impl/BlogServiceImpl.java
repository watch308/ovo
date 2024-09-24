package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

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
    FollowMapper followMapper;
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
        queryBlogCreate(blog);

        //查询是否点赞
        queryIsLike( blog);
        return Result.ok(blog);
    }

    private void queryIsLike( Blog blog) {
        Long userId = UserHolder.getUser().getId();
        Double score = stringRedisTemplate.opsForZSet().score(BLOG_LIKED_KEY + blog.getId(), String.valueOf(userId));
        if (score != null) {
            blog.setIsLike(true);
        }
    }

    private void queryBlogCreate(Blog blog) {
        Long blogUserId = blog.getUserId();
        User user = userMapper.selectOneById(blogUserId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
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
        records.forEach(blog -> {
            // 查询blog用户
            queryBlogCreate(blog);
            // 查询是否点赞
            UserDTO user = UserHolder.getUser();
            if (user != null) {
                queryIsLike(blog);
            }
        });
        return Result.ok(records);
    }

    @Override
    public Result queryBlogLikes(Long id) {
        Set<String> range = stringRedisTemplate.opsForZSet().range(BLOG_LIKED_KEY + id, 0, 4);
        if (range == null || range.isEmpty()) {
            return Result.ok();
        }
        List<Long> collect = range.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> users = userMapper.searchAllById(collect);

        return Result.ok(users);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        Long blogUserId = user.getId();
        blog.setUserId(blogUserId);
        // 保存探店博文
        blogService.save(blog);
        //获取粉丝
        List<Long> followIds = followMapper.queryUserIdByFollowUserId(blogUserId);
        long time = System.currentTimeMillis();
        //保存到用户信箱
        for (Long followId : followIds) {
            String key = FEED_KEY + followId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), time);
        }
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryFollowBlog(Long lastId, Long offset) {

        //获取用户信箱
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("未登录");
        }
        Long userId = user.getId();
        String key = FEED_KEY + userId;
        //获取博客ID
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, lastId, offset, 5);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        //
        List<Long> blogId = new ArrayList<>();
        ZSetOperations.TypedTuple<String> next = typedTuples.iterator().next();
        long minTime = next.getScore().longValue();
        int count = 1;
        for (ZSetOperations.TypedTuple<String> blogTuple : typedTuples) {
            String value = blogTuple.getValue();
            long score = blogTuple.getScore().longValue();
            if (score < minTime) {
                count = 1;
                minTime = score;
            } else if (score == minTime) {
                count++;
            }
            blogId.add(Long.valueOf(value));
        }
        //获取博客内容
        List<Blog> blogs = blogMapper.queryByIdOrderById(blogId);
        //返回值
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(count);
        scrollResult.setMinTime(minTime);
        for (Blog blog : blogs) {
            queryBlogCreate(blog);
            queryIsLike(blog);
        }
        return Result.ok(scrollResult);
    }

}

