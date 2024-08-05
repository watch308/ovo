package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static cn.hutool.core.lang.Console.log;
import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    public void set(String key, Object object, Long time, TimeUnit unit) {
        Map<String, String> stringStringMap = entity2StrMap(object);
        stringRedisTemplate.opsForHash().putAll(key, stringStringMap);
        stringRedisTemplate.expire(key, time, unit);
    }

    public void setWithLogicalExpire(String key, Object object, Long time, TimeUnit unit) {

        RedisData redisData = new RedisData();
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(unit.toSeconds(time));
        redisData.setData(object);
        redisData.setExpireTime(expireTime);
        Map<String, String> stringStringMap = entity2StrMap(redisData);

        stringRedisTemplate.opsForHash().putAll(key, stringStringMap);

    }

    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dataBaseFunction
            , Long time, TimeUnit unit) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // 在redis上查询
        String cacheKey = keyPrefix + id;
        Map<Object, Object> redisMap = stringRedisTemplate.opsForHash().entries(cacheKey);
        // 存在 返回
        if (!redisMap.isEmpty()) {
            // 判断是否存在特定key
            if (redisMap.containsKey("empty")) {
                redisMap.get("empty");
                return null;
            }

            return BeanUtil.fillBeanWithMap(redisMap, type.newInstance(), false);
        }
        // 不存在 查数据库
        R r = dataBaseFunction.apply(id);

        // 数据库存在
        if (r != null) {
            // 写入Redis
            this.set(cacheKey, r, time, unit);
            return r;
        }

        //不存在 加入特定key

        stringRedisTemplate.opsForHash().put(cacheKey, "empty", "");
        stringRedisTemplate.expire(cacheKey, CACHE_NULL_TTL, TimeUnit.MINUTES);
        return null;


    }

    private static final ExecutorService CACHE_REBUILT_EXECUTOR = Executors.newFixedThreadPool(10);

    public <R, ID> R queryShopWithLogicalExpire(String cacheKeyPrefix, String lockKeyPrefix, ID id, Class<R> type, Function<ID, R> dataBaseFunction
            , Long time, TimeUnit unit) throws InstantiationException, IllegalAccessException {
        // 在redis上查询
        String cacheKey = cacheKeyPrefix + id;
        Map<Object, Object> redisMap = stringRedisTemplate.opsForHash().entries(cacheKey);
        if (redisMap.isEmpty()) {
            return null;
        }
        LocalDateTime expireTime = LocalDateTime.parse(redisMap.get("expireTime").toString());

        // 转化为entity
        redisMap.remove("expireTime");
        R r = BeanUtil.fillBeanWithMap(redisMap, type.newInstance(), false);

        // 判断是否过期
        if (LocalDateTime.now().isBefore(expireTime)) {
            // 未过期 返回店铺信息
            return r;
        }

        //过期 获取锁
        String lockKey = lockKeyPrefix + id;
        boolean isLock = lock(lockKey);
        if (isLock) {
            //成功
            CACHE_REBUILT_EXECUTOR.submit(() -> {
                try {
                    //重建缓存
                    R r1 = dataBaseFunction.apply(id);

                    this.setWithLogicalExpire(cacheKey, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException();
                } finally {
                    //释放锁
                    unlock(lockKey);
                }
            });
        }

        return r;

    }

    boolean lock(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "locked", LOCK_SHOP_TTL, TimeUnit.SECONDS));
    }

    void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    private <T> Map<String, String> entity2StrMap(T data) {
        Map<String, Object> map = new HashMap<>();

        if (data instanceof Shop) {
            map = BeanUtil.beanToMap(data);
        } else if (data instanceof RedisData) {
            RedisData redisData = (RedisData) data;
            map = BeanUtil.beanToMap(redisData.getData());
            map.put("expireTime", redisData.getExpireTime());
        } else {
            return null;
        }

        // 转换为String,处理空值
        Map<String, String> shopStrMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            shopStrMap.put(entry.getKey(), value);
        }
        return shopStrMap;
    }
}
