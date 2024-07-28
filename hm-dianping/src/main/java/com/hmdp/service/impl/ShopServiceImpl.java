package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

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

    @Override
    public Result queryShopById(Long id) {
        // 在redis上查询
        String shopKey = CACHE_SHOP_KEY + id;
        Map<Object, Object> shopRedisMap = stringRedisTemplate.opsForHash().entries(shopKey);
        // 存在 返回
        if (!shopRedisMap.isEmpty()) {

            Shop shop = BeanUtil.fillBeanWithMap(shopRedisMap, new Shop(), false);
            // 更新时间
            stringRedisTemplate.expire(shopKey, CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.ok(shop);
        }
        // 不存在 查数据库
        Shop shopData = shopMapper.selectById(id);

        if (shopData != null) {
            // 写回redis
            Map<String, Object> shopMap = BeanUtil.beanToMap(shopData);

            // 转换为String,处理空值
            Map<String, String> shopStrMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : shopMap.entrySet()) {
                String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
                shopStrMap.put(entry.getKey(), value);
            }

            stringRedisTemplate.opsForHash().putAll(shopKey, shopStrMap);
            stringRedisTemplate.expire(shopKey, CACHE_SHOP_TTL, TimeUnit.MINUTES);

            Shop shop = BeanUtil.fillBeanWithMap(shopMap, new Shop(), false);

            return Result.ok(shop);
        }

        return Result.fail("不存在");

    }
}
