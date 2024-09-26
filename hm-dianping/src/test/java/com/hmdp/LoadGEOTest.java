package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.ws.Action;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest()
@RunWith(SpringJUnit4ClassRunner.class)
public class LoadGEOTest {

    @Autowired
    ShopMapper shopMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    public void load() {
        List<Shop> shops = shopMapper.selectXAndIdAndYAndTypeId();
        Map<Long, List<Shop>> collect = shops.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long, List<Shop>> e : collect.entrySet()) {
            List<RedisGeoCommands.GeoLocation<String>> stringGeoLocation = new ArrayList<>(collect.size());
            List<Shop> value = e.getValue();
            for (Shop shop : value) {
                stringGeoLocation.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())));
            }
            String key = SHOP_GEO_KEY + e.getKey();
            stringRedisTemplate.opsForGeo().add(key, stringGeoLocation);
        }
    }
}
