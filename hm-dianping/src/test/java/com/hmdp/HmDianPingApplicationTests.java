package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cn.hutool.core.lang.Console.log;

@SpringBootTest()
@RunWith(SpringJUnit4ClassRunner.class)
public class HmDianPingApplicationTests {


    @Resource
    private ShopServiceImpl shopService;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    private ShopMapper shopMapper;
    @Resource
    CacheClient cacheClient;

    @Test
    public void testSave() throws InterruptedException {
        shopService.saveShop2Redis(1L, 10L);
        Map<Object, Object> shopRedisMap = stringRedisTemplate.opsForHash().entries("cache:shop:1");

        // 存在 判断是否过期

        System.out.println(LocalDateTime.now());
        System.out.println(shopRedisMap.get("expireTime"));
    }

    @Test
    public void testEntity2Map() {

        Shop shop = shopMapper.selectById(1);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(10));
        cacheClient.set("2",redisData,100L,TimeUnit.SECONDS);
        Map<Object, Object> shopRedisMap = stringRedisTemplate.opsForHash().entries("2");
        RedisData redisData1 = BeanUtil.fillBeanWithMap(shopRedisMap, new RedisData(), false);
        System.out.println(redisData1);
        System.out.println(redisData1.getData());
        Shop shop1 = (Shop) redisData1.getData();
    }
    @Test
    public void testEmptyHash(){

    }
}
