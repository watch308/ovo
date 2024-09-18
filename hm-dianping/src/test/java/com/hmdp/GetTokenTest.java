package com.hmdp;

import cn.hutool.core.lang.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

@SpringBootTest()
@RunWith(SpringJUnit4ClassRunner.class)
public class GetTokenTest {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void createTokensAndWrite() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String token = UUID.randomUUID().toString(true);
            // 转换Long id
            Map<String, String> map = new HashMap<>();
            Random random = new Random();
            String v = String.valueOf(random.nextInt(10000));
            map.put("id", v);
            // 保存登录信息到 Redis
            redisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, map);
            tokens.add(token);
        }
        //把tokens写到文件里
        String filePath = "C:\\java\\jmeterFile\\hmdp\\tokens.txt";
        try {
            Files.write(Paths.get(filePath), tokens);
            System.out.println("Tokens written to " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing tokens to file: " + e.getMessage());
        }
    }

    @Test
    public void createOneTokens() {
        List<String> tokens = new ArrayList<>();
        String token = UUID.randomUUID().toString(true);
        // 转换Long id
        Map<String, String> map = new HashMap<>();
        Random random = new Random();
        String v = String.valueOf(random.nextInt(10000));
        map.put("id", v);
        // 保存登录信息到 Redis
        redisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, map);
        tokens.add(token);
        System.out.println(token);

        //把tokens写到文件里
        String filePath = "C:\\java\\jmeterFile\\hmdp\\tokens.txt";
        try {
            Files.write(Paths.get(filePath), tokens);
            System.out.println("Tokens written to " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing tokens to file: " + e.getMessage());
        }

    }
}
