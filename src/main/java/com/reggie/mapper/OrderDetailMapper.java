package com.reggie.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.pojo.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 订单明细表 Mapper 接口
 * </p>
 *
 * @author cc
 * @since 2022-05-30
 */
@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

}
