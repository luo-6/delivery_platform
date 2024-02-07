package com.reggie.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.pojo.Orders;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 订单表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2022-05-30
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

}
