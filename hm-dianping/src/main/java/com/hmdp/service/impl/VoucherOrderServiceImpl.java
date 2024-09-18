package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.*;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private VoucherOrderMapper voucherOrderMapper;
    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private static final ArrayBlockingQueue<VoucherOrder> voucherOrderBlockingQueue = new ArrayBlockingQueue<>(10000);
    //代理对象
    private IVoucherOrderService target;
    private static final DefaultRedisScript<Long> seckillScript;
    private static final ExecutorService executors = newSingleThreadExecutor();

    static {
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setLocation(new ClassPathResource("seckill.lua"));
        seckillScript.setResultType(Long.class);
    }

    @PostConstruct
    private void init() {
        // 提交异步修改数据库
        executors.submit(new VoucherOrderHandle());
    }


    @Override
    public Result seckillVoucher(long voucherId) {
        Long userId = UserHolder.getUser().getId();
        //执行lua脚本
        Long result = stringRedisTemplate.execute(seckillScript,
                Collections.emptyList(),
                String.valueOf(voucherId),
                userId.toString()
        );
        int i = result.intValue();
        if(i==3)return Result.fail("抢购卷不存在");
        if (i != 0) {
            return i == 1 ? Result.fail("订购失败") : Result.fail("不能重复下单");
        }
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        UserDTO user = UserHolder.getUser();
        voucherOrder.setUserId(user.getId());
        voucherOrder.setVoucherId(voucherId);
        // TODO 保存到阻塞队列
        try {
            voucherOrderBlockingQueue.put(voucherOrder);
            this.target = (IVoucherOrderService) AopContext.currentProxy();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 返回订单编号
        return Result.ok(orderId);
    }

    private class VoucherOrderHandle implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    VoucherOrder voucherOrder = voucherOrderBlockingQueue.take();
                    createOrder(voucherOrder);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void createOrder(VoucherOrder voucherOrder) {
        //获取锁
        // SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + voucherOrder.getUserId());
        if (!lock.tryLock()) {
            //失败
            return;
        }
        try {
            target.saveOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public void saveOrder(VoucherOrder voucherOrder) {
        Long voucherId = voucherOrder.getVoucherId();
        Long userId = voucherOrder.getUserId();
        // 一人一单
        int count = voucherOrderMapper.countByVoucherIdAndUserId(voucherId, userId);
        if (count > 0) {
            return;
        }
        //减少库存
        boolean success = seckillVoucherMapper.updateStockByVoucherId(voucherId);
        if (!success) {
            return;
        }
        save(voucherOrder);
    }

    /*
    //同步抢购
    @Override
    public Result seckillVoucher(long voucherId) {
        //查询优惠劵
        SeckillVoucher voucher = seckillVoucherMapper.selectByVoucherId(voucherId);
        //判断是否开始结束
        if (LocalDateTime.now().isBefore(voucher.getBeginTime()) || LocalDateTime.now().isAfter(voucher.getEndTime())) {
            return Result.fail("时间错误");
        }
        //判断是否有票
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        //获取锁
        // SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        if (!lock.tryLock()) {
            //失败
            return Result.fail("不允许重复下单");
        }
        try {
            // 获取代理对象
            IVoucherOrderService target = (IVoucherOrderService) AopContext.currentProxy();
            return target.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }

    }

    @Override
    @Transactional
    public Result createVoucherOrder(long voucherId) {
        // 一人一单
        Long userId = UserHolder.getUser().getId();
        int count = voucherOrderMapper.countByVoucherIdAndUserId(voucherId, userId);
        if (count > 0) {
            return Result.fail("不允许重复下单");
        }
        //减少库存
        boolean success = seckillVoucherMapper.updateStockByVoucherId(voucherId);
        if (!success) {
            return Result.fail("库存不足");
        }
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long id = redisIdWorker.nextId("order");
        voucherOrder.setId(id);
        UserDTO user = UserHolder.getUser();
        voucherOrder.setUserId(user.getId());
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(id);

    }
    */
}
