package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.DishDto;
import com.reggie.pojo.Dish;

public interface DishService extends IService<Dish> {
    //新增菜品，同时插入菜品口味，同时操作dish,dish_flavor两张表
    public void saveWithFlavor(DishDto dishDto);
    //根据id查询菜品信息和对应口味
    public DishDto getByIdWithFlavor(Long id);

    public void updateWithFlavor(DishDto dishDto);

}
