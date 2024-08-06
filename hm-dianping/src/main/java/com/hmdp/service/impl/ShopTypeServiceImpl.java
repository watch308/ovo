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

    @Override
    public Result queryTypeList() {

        List<ShopType> shopTypeList = queryTypeListWithMutex();
        if (shopTypeList == null)
            return Result.fail("无分类");
        return Result.ok(shopTypeList);
    }


    public List<ShopType> queryTypeListWithMutex() {
        String shopTypeKey = CACHE_SHOP_TYPE_KEY;
        List<ShopType> shopTypeList = new ArrayList<>();
        List<String> shopTypeJsonList = stringRedisTemplate.opsForList().range(shopTypeKey, 0, -1);

        // Redis存在 返回
        if (shopTypeJsonList != null && !shopTypeJsonList.isEmpty()) {
            for (String shopTypeJson : shopTypeJsonList) {
                shopTypeList.add(JSONUtil.toBean(shopTypeJson, ShopType.class));
            }
            return shopTypeList;
        }

        try {
            // 获取锁
            boolean isLock = lock(LOCK_SHOP_TYPE_KEY);
            // 失败则重试
            if (!isLock) {
                Thread.sleep(50);
                return queryTypeListWithMutex();
            }
            // Redis 不存在 查数据库
            List<ShopType> shopTypeListBase = shopTypeMapper.queryAll();
            if (shopTypeListBase != null && !shopTypeListBase.isEmpty()) {
                //清空错误的值
                stringRedisTemplate.delete(CACHE_SHOP_TYPE_KEY);
                Thread.sleep(200);
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
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "locked", LOCK_SHOP_TTL, TimeUnit.SECONDS));
    }

    void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

}
