package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

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
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        try {
            if (!simpleRedisLock.tryLock(120)) {
                //失败
                return Result.fail("不允许重复下单");
            }
            IVoucherOrderService target = (IVoucherOrderService) AopContext.currentProxy();
            return target.createVoucherOrder(voucherId);
        } finally {
            simpleRedisLock.unlock();
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
}
