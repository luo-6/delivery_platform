package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.Result;
import com.reggie.mapper.OrdersMapper;
import com.reggie.pojo.OrderDetail;
import com.reggie.pojo.Orders;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrdersService;
import com.sun.org.apache.xpath.internal.operations.Or;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
@Slf4j
public class orderController {
    @Autowired
    private OrdersService ordersService;
    @Autowired
    private OrderDetailService orderDetailService;
    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody Orders orders){
        ordersService.submit(orders);
        return Result.success("下单成功");
    }
    @GetMapping("/userPage")
    public Result<Page> userPage(int page,int pageSize){
        Page<OrderDetail> orderDetailPage = new Page<>(page,pageSize);
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId,userId);
        List<Orders> list = ordersService.list(queryWrapper);
        List<Long> ordersIds = list.stream().map(item->{
            Long id = item.getId();
            return id;
        }).collect(Collectors.toList());
        LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
        orderDetailLambdaQueryWrapper.in(OrderDetail::getOrderId,ordersIds);
        Page<OrderDetail> detailPage = orderDetailService.page(orderDetailPage,orderDetailLambdaQueryWrapper);
        return Result.success(detailPage);
    }
}
