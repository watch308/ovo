package com.hmdp.mapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {
    int countByVoucherIdAndUserId(@Param("voucherId") Long voucherId, @Param("userId") Long userId);
}
