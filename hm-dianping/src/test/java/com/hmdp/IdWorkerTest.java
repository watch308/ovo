package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.concurrent.*;

@SpringBootTest()
@RunWith(SpringJUnit4ClassRunner.class)
public class IdWorkerTest {
    @Resource
    RedisIdWorker redisIdWorker;

    ExecutorService es = new ThreadPoolExecutor(500, 500, 1L
            , TimeUnit.SECONDS, new ArrayBlockingQueue<>(100)
            , new ThreadPoolExecutor.AbortPolicy());

    @Test
    public void idWorkerTest() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("test");
                System.out.println("id:" + id);
            }
            countDownLatch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        countDownLatch.await();

        long end = System.currentTimeMillis();
        System.out.println(end - begin);
    }
}
