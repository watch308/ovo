package com.hmdp.mapper;
import java.util.List;

import com.hmdp.entity.SeckillVoucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {

    boolean updateStockByVoucherId( @Param("voucherId") Long voucherId);

    SeckillVoucher selectByVoucherId(@Param("voucherId") Long voucherId);
}
