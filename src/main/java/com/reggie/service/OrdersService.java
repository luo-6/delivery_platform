package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.pojo.Orders;

/**
 * <p>
 * 订单表 服务类
 * </p>
 *
 * @author cc
 * @since 2022-05-30
 */
public interface OrdersService extends IService<Orders> {

    public void submit(Orders orders);
}
