package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.VoucherOrder;

public interface VoucherOrderTestMapper extends BaseMapper<VoucherOrder> {
    // 清空订单
    int delete();
}
