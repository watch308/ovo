package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ShopMapper shopMapper;

    private static final ExecutorService CACHE_REBUILT_EXECUTOR = Executors.newFixedThreadPool(10);

    @Override
    public Result queryShopById(Long id) {
        Shop shop;
        // 判断是否为热点
        if(hotId(id)){
            shop = queryShopWithLogicalExpire(id);
        }
        else {
           shop = queryShopWithPassThrough(id);
        }
        if (shop==null){
            return Result.fail("无");
        }
        return Result.ok(shop);
    }

    private boolean hotId(Long id) {
        return id==1;
    }

    public Shop queryShopWithLogicalExpire(Long id) {
        // 在redis上查询
        String shopKey = CACHE_SHOP_KEY + id;
        Map<Object, Object> shopRedisMap = stringRedisTemplate.opsForHash().entries(shopKey);
        if (shopRedisMap.isEmpty()) {
            return null;
        }
        // 存在 判断是否过期
        LocalDateTime expireTime = LocalDateTime.parse(shopRedisMap.get("expireTime").toString());
        // 分解为 shop
        shopRedisMap.remove("expireTime");
        Shop shop = BeanUtil.fillBeanWithMap(shopRedisMap, new Shop(), false);
        if (LocalDateTime.now().isBefore(expireTime)) {
            // 未过期 返回店铺信息
            return shop;
        }

        //过期 获取锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = lock(lockKey);
        if (isLock) {
            //成功
            CACHE_REBUILT_EXECUTOR.submit(() -> {
                try {
                    //重建缓存
                    saveShop2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException();
                } finally {
                    //释放锁
                    unlock(lockKey);
                }
            });
        }

        return shop;

    }

    @Transactional
    @Override
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺不存在");
        }
        // 更新数据库
        shopMapper.update(shop);
        // 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    //将entity转成Map<String,String>
    private <T> Map<String, String> object2Str(T data) {
        Map<String, Object> map = new HashMap<>();

        if (data instanceof Shop) {
            map = BeanUtil.beanToMap(data);
        } else if (data instanceof RedisData) {
            RedisData redisData = (RedisData) data;
            map = BeanUtil.beanToMap(redisData.getData());
            map.put("expireTime", redisData.getExpireTime());
        }
        else {
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

    // 保存热点数据到Redis
    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        // 查店铺信息
        Shop shop = shopMapper.selectById(id);
        Thread.sleep(200);

        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        Map<String, String> stringStringMap = object2Str(redisData);
        stringRedisTemplate.opsForHash().putAll(CACHE_SHOP_KEY + id, stringStringMap);
    }

    boolean lock(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "locked", LOCK_SHOP_TTL, TimeUnit.SECONDS));
    }

    void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

   public Shop queryShopWithPassThrough(Long id) {
        // 在redis上查询
        String shopKey = CACHE_SHOP_KEY + id;
        Map<Object, Object> shopRedisMap = stringRedisTemplate.opsForHash().entries(shopKey);
        // 存在 返回店铺
        if (!shopRedisMap.isEmpty()) {
            // 空map 返回null
            if (shopRedisMap.containsKey("empty")) {
                shopRedisMap.get("empty");
                return null;
            }

           return BeanUtil.fillBeanWithMap(shopRedisMap, new Shop(), false);
        }
        // 不存在 查数据库
        Shop shopData = shopMapper.selectById(id);

        // 数据库存在
        if (shopData != null) {
            // 转换为String,处理空值
            Map<String, String> shopStrMap = object2Str(shopData);

            // 写入Redis
            stringRedisTemplate.opsForHash().putAll(shopKey, shopStrMap);
            stringRedisTemplate.expire(shopKey, CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return BeanUtil.fillBeanWithMap(shopStrMap, new Shop(), false);
        }

        //不存在 加入特定key
        stringRedisTemplate.opsForHash().put(shopKey, "empty", "");
        stringRedisTemplate.expire(shopKey, CACHE_NULL_TTL, TimeUnit.MINUTES);
        return null;

    }
}
