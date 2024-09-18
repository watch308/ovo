package com.hmdp.mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.SeckillVoucher;


public interface SeckillTestMapper extends BaseMapper<SeckillVoucher> {

    int updateStockByVoucherId(@Param("stock") Integer stock, @Param("voucherId") Long voucherId);

}
