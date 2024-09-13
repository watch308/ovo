package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.apache.ibatis.javassist.ClassPath;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String V_PREFIX = UUID.randomUUID().toString(true);
    private static final String PREFIX="lock:";
    private static DefaultRedisScript<String> unLockScript;
    static {
        unLockScript = new DefaultRedisScript<>();
        unLockScript.setLocation(new ClassPathResource("unLock.lua"));
    }
    public SimpleRedisLock(String name,StringRedisTemplate stringRedisTemplate){
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    public boolean tryLock(long timeoutSec) {
        String v = V_PREFIX+Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(PREFIX + name, v, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {

        ArrayList<String> keys = new ArrayList<>();
        keys.add(PREFIX + name);
        stringRedisTemplate.execute(unLockScript,
                keys,
                V_PREFIX+Thread.currentThread().getId());
    }

//    @Override
//    public void unlock() {
//        String id = stringRedisTemplate.opsForValue().get(PREFIX + name);
//        String currentId = V_PREFIX+Thread.currentThread().getId();
//        if(id!=null&&id.equals(currentId))
//            stringRedisTemplate.delete(PREFIX+name);
//    }

}
