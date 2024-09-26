package com.hmdp;

import com.hmdp.mapper.SeckillTestMapper;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderTestMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest()
@RunWith(SpringJUnit4ClassRunner.class)
public class SeckillTest {
    @Autowired
    private SeckillTestMapper seckillTestMapper;
    @Autowired
    private VoucherOrderTestMapper voucherOrderTestMapper;
    @Test
    public void preTest(){
        // 秒杀卷库存重置为100，清空订单
        seckillTestMapper.updateStockByVoucherId(200,11L);
        voucherOrderTestMapper.delete();

    }
}
