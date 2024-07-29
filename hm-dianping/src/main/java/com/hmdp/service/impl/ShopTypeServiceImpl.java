package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
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
        String shopTypeKey = CACHE_SHOPTYPE_KEY;
        Long size = stringRedisTemplate.opsForList().size(shopTypeKey);
        List<ShopType> shopTypeList = new ArrayList<>();

        // Redis存在 返回
        if (size != null && size == CACHE_SHOPTYPE_SIZE) {
            List<String> shopTypeJsonList = stringRedisTemplate.opsForList().range(shopTypeKey, 0, -1);
            if (shopTypeJsonList != null) {
                for (String shopTypeJson : shopTypeJsonList) {
                    shopTypeList.add(JSONUtil.toBean(shopTypeJson, ShopType.class));
                }

            }
            return Result.ok(shopTypeList);
        }
        // Redis 不存在 查数据库
        List<ShopType> shopTypeListBase = shopTypeMapper.queryAll();

        if (shopTypeListBase!=null&&shopTypeListBase.size()==CACHE_SHOPTYPE_SIZE) {
            //清空错误的值
            stringRedisTemplate.delete(CACHE_SHOPTYPE_KEY);
            // 写回Redis
            for (ShopType shopType : shopTypeListBase) {
                String shopTypeJson = JSONUtil.toJsonStr(shopType);
                stringRedisTemplate.opsForList().rightPush(shopTypeKey, shopTypeJson);
            }
            stringRedisTemplate.expire(shopTypeKey, CACHE_SHOPTYPE_TTL, TimeUnit.MINUTES);

            return Result.ok(shopTypeListBase);
        }
        return Result.fail("无商店分类");
    }
}
