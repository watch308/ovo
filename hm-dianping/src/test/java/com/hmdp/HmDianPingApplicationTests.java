package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Map;

import static cn.hutool.core.lang.Console.log;

@SpringBootTest()
@RunWith(SpringJUnit4ClassRunner.class)
public class HmDianPingApplicationTests {


    @Resource
    private ShopServiceImpl shopService;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Test
    public void testSave() throws InterruptedException {
        shopService.saveShop2Redis(1L,10L);
        Map<Object, Object> shopRedisMap = stringRedisTemplate.opsForHash().entries("cache:shop:1");

        // 存在 判断是否过期

        System.out.println(LocalDateTime.now());
        System.out.println(shopRedisMap.get("expireTime"));
    }
}
