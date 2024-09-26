package com.hmdp.mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.io.Serializable;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ShopMapper extends BaseMapper<Shop> {
    @Override
    Shop selectById(Serializable id);

    List<Shop> selectByIdOrderById( List<Long> id);
    void update(Shop shop);

    List<Shop> selectXAndIdAndYAndTypeId();
}
