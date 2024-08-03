package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ShopTypeMapper shopTypeMapper;
    private int shopTypeSize;

    @Override
    public Result queryTypeList() {
        shopTypeSize = queryShopTypeSize();
        if (shopTypeSize == 0) {
            return Result.fail("无分类");
        }
        List<ShopType> shopTypeList = queryTypeListWithMutex();
        if (shopTypeList == null)
            return Result.fail("无分类");
        return Result.ok(shopTypeList);
    }


    public List<ShopType> queryTypeListWithMutex() {

        String shopTypeKey = CACHE_SHOP_TYPE_KEY;
        Long size = stringRedisTemplate.opsForList().size(shopTypeKey);
        List<ShopType> shopTypeList = new ArrayList<>();

        // Redis存在 返回
        if (size != null && size == shopTypeSize) {
            List<String> shopTypeJsonList = stringRedisTemplate.opsForList().range(shopTypeKey, 0, -1);
            if (shopTypeJsonList != null) {
                for (String shopTypeJson : shopTypeJsonList) {
                    shopTypeList.add(JSONUtil.toBean(shopTypeJson, ShopType.class));
                }

            }
            return shopTypeList;
        }

        try {
            // 获取锁
            boolean isLock = lock(LOCK_SHOP_KEY);
            // 失败则重试
            if (!isLock) {
                Thread.sleep(50);
                return queryTypeListWithMutex();
            }
            // Redis 不存在 查数据库
            List<ShopType> shopTypeListBase = shopTypeMapper.queryAll();
            Thread.sleep(200);
            if (shopTypeListBase != null && !shopTypeListBase.isEmpty()) {
                //清空错误的值
                stringRedisTemplate.delete(CACHE_SHOP_TYPE_KEY);
                // 写回Redis
                for (ShopType shopType : shopTypeListBase) {
                    String shopTypeJson = JSONUtil.toJsonStr(shopType);
                    stringRedisTemplate.opsForList().rightPush(shopTypeKey, shopTypeJson);
                }
                stringRedisTemplate.expire(shopTypeKey, CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
                return shopTypeListBase;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(LOCK_SHOP_TYPE_KEY);
        }
        return null;
    }



    boolean lock(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "locked", 10, TimeUnit.SECONDS));
    }

    void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public int queryShopTypeSize() {
        String shopTypeSizeKey = CACHE_SHOP_TYPE_SIZE_KEY;
        String lockKey = LOCK_SHOP_TYPE_SIZE_KEY;
        String shopTypeSizeStr = stringRedisTemplate.opsForValue().get(shopTypeSizeKey);
        if (StrUtil.isNotBlank(shopTypeSizeStr)) {
            return Integer.parseInt(Objects.requireNonNull(shopTypeSizeStr));
        }
        // 获取锁
        try {
            // 不存在 查数据库 写入Redis
            boolean isLock = lock(lockKey);
            if (!isLock) {
                Thread.sleep(10);
                queryShopTypeSize();
            }
            String size = stringRedisTemplate.opsForValue().get(shopTypeSizeKey);
            if (StrUtil.isNotBlank(size)) {
                return Integer.parseInt(Objects.requireNonNull(size));
            }
            int shopTypeSize = shopTypeMapper.querySize();
            Thread.sleep(200);
            stringRedisTemplate.opsForValue().set(shopTypeSizeKey, String.valueOf(shopTypeSize)
                    , CACHE_SHOP_TYPE_SIZE_TTL, TimeUnit.MINUTES);
            return shopTypeSize;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey);
        }
    }
}
