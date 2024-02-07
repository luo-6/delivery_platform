package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.Result;
import com.reggie.pojo.ShoppingCart;
import com.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 点单的菜品数增加
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public Result<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        Long userId = BaseContext.getCurrentId();//获取用户名
        shoppingCart.setUserId(userId);
        log.info("shoppingCart:{}", shoppingCart);
        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,userId);
        if (dishId != null){
            //如果是单个菜品就查询dish_id
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
        }
        else {
            //如果是套餐就查询setmealId
            queryWrapper.eq(ShoppingCart::getSetmealId,setmealId);

        }
        //并且根据上述获取到的数据用于查询
        ShoppingCart shoppingCart1 = shoppingCartService.getOne(queryWrapper);
        if (shoppingCart1!=null){
            //如果菜品已经订过一次了，就数量加一
            Integer number = shoppingCart1.getNumber() + 1;
            shoppingCart1.setNumber(number);
            shoppingCartService.updateById(shoppingCart1);
        }
        else {
            //如果没有，先创建一个新对象并且将初始值设为1
            shoppingCart1 = new ShoppingCart();
            shoppingCart1.setNumber(1);
            shoppingCart1.setCreateTime(LocalDateTime.now());
            shoppingCart1 = shoppingCart;
            shoppingCartService.save(shoppingCart1);
        }
        shoppingCart =shoppingCart1;
        log.info("shopping:{}",shoppingCart);
        return Result.success(shoppingCart);
    }
    @PostMapping("/sub")
    public Result<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart){
        log.info("shoppingcart菜单减少:{}",shoppingCart);
        //获取用户id
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        //根据菜单id或者套餐id以及用户id查询数据
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        Long setmealId = shoppingCart.getSetmealId();
        Long dishId = shoppingCart.getDishId();
        queryWrapper.eq(ShoppingCart::getUserId,userId);
        queryWrapper.eq(setmealId!=null,ShoppingCart::getSetmealId,setmealId);
        queryWrapper.eq(dishId!=null,ShoppingCart::getDishId,dishId);

        ShoppingCart shoppingCartServiceOne = shoppingCartService.getOne(queryWrapper);
        shoppingCartServiceOne.setNumber(shoppingCartServiceOne.getNumber() - 1);
        shoppingCartService.updateById(shoppingCartServiceOne);
        return Result.success(shoppingCartServiceOne);
    }

    /**
     * @return 购物车列表
     */
    @GetMapping("/list")
    public Result<List<ShoppingCart>> list(){
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,userId);
        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);
        return Result.success(list);
    }

    /**
     * 订单清空
     * @return
     */
    @DeleteMapping("/clean")
    public Result<String> clean(){
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,userId);
        shoppingCartService.remove(queryWrapper);
        return Result.success("订单清空完成");
    }

}
