package com.reggie.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.Result;
import com.reggie.dto.DishDto;
import com.reggie.dto.SetmealDto;
import com.reggie.pojo.*;
import com.reggie.service.CategoryService;
import com.reggie.service.DishService;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetMealController{
    @Autowired
    private SetmealDishService setMealDishService;
    @Autowired
    private SetmealService setMealService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private DishService dishService;
    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "setmealCache",allEntries = true)
    public Result<String> save(@RequestBody SetmealDto setmealDto){
        log.info("套餐信息:{}",setmealDto);
        setMealService.saveWithDish(setmealDto);
        return Result.success("新增套餐成功");
    }

    /**
     * 分页查询
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
     public Result<Page> page(int page, int pageSize,String name){
         Page<Setmeal> pageInfo = new Page<>(page,pageSize);
         Page<SetmealDto> dishDtoPage = new Page<>();
         LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
         //进行条件查询
        queryWrapper.like(name != null,Setmeal::getName,name);
         queryWrapper.orderByDesc(Setmeal::getUpdateTime);
         //进行分页查询
         setMealService.page(pageInfo,queryWrapper);
         //拷贝对象
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            //分类id
            Long categoryId = item.getCategoryId();
            BeanUtils.copyProperties(item, setmealDto);
            //根据分类id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                //分类名称
                String categoryName = category.getName();
                category.setName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(list);
         return Result.success(dishDtoPage);
     }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
     @DeleteMapping
     @CacheEvict(value = "setmealCache",allEntries = true)
     public Result<String> delete(@RequestParam List<Long> ids){
         log.info("ids:{}",ids);
         setMealService.removeWithDish(ids);
        return Result.success("套餐数据删除成功");
     }
     @PostMapping("/status/{status}")
     public Result<String> stop(@PathVariable Integer status,@RequestParam List<Long> ids){
         log.info("status:{},ids:{},name:{}",status,ids);
         for (Long id : ids) {
             LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
             queryWrapper.eq(Setmeal::getId, id);
             Setmeal setmeal = setMealService.getOne(queryWrapper);
             log.info("setmeal:{}",setmeal);
             setmeal.setStatus(status);
             setMealService.update(setmeal,queryWrapper);
         }
         return Result.success("禁用套餐成功");
     }
     @GetMapping("/dish/{id}")
    public Result<Dish> dish(@PathVariable Long id){
         log.info("dish_id:{}",id);
         LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
         queryWrapper.eq(Dish::getId,id);
         Dish dish = dishService.getOne(queryWrapper);
         return Result.success(dish);
    }

    /**
     * 规格口味订单
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId + '_' + #setmeal.status")
    public Result<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(Setmeal::getStatus,setmeal.getStatus());
        List<Setmeal> list = setMealService.list(queryWrapper);
        log.info("list:{}",list);
        return Result.success(list);
    }
}
