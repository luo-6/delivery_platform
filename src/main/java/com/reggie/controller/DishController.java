package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.dto.DishDto;
import com.reggie.pojo.Category;
import com.reggie.pojo.Dish;
import com.reggie.pojo.DishFlavor;
import com.reggie.service.CategoryService;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.expression.Sets;

import java.security.Key;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public Result<String> save(@RequestBody DishDto dishDto){
        log.info("DishDto:{}",dishDto);
        dishService.saveWithFlavor(dishDto);
        return Result.success("新增菜品成功");
    }

    /**
     * 菜品信息分页
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public Result<Page> page(int page, int pageSize,String name){
        //分页构造器
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();
        //条件构造器对象
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper();
        //过滤条件
        lambdaQueryWrapper.like(name != null,Dish::getName,name);
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        //进行分页查询
        dishService.page(pageInfo,lambdaQueryWrapper);
        //对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");
        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item)->{
            DishDto dishDto = new DishDto();
            Long categoryId = item.getCategoryId();//分类id
            BeanUtils.copyProperties(item,dishDto);
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category!=null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(list);
        return Result.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应口味
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<DishDto> get(@PathVariable Long id){
        DishDto flavor = dishService.getByIdWithFlavor(id);
        return Result.success(flavor);
    }
    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public Result<DishDto> update(@RequestBody DishDto dishDto){
        log.info("DishDto:{}",dishDto);
        dishService.updateWithFlavor(dishDto);
        //清理所有缓存
        //Set keys = redisTemplate.keys("dish_*");
        //只是清理某个分类下面的keys缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return Result.success(dishDto);
    }
    @PostMapping ("/status/{status}")
    public Result<String> stop(@PathVariable Integer status, @RequestParam("ids") List<String> ids){
        for (String id : ids) {
            LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
            log.info("id:{}",id);
            queryWrapper.eq(Dish::getId,id);
            Dish dish = dishService.getOne(queryWrapper);
            dish.setStatus(status);
            dishService.update(dish,queryWrapper);
        }
        return Result.success("更新成功");
    }

    /**
     * 根据条件查询对应菜品数据
     * @param dish
     * @return
     */
//    @GetMapping("/list")
//    public Result<List<Dish>> list(Dish dish){
//        //构造查询条件
//        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
//        //添加查询状态，查询状态为1（起售状态）的菜品
//        queryWrapper.eq(Dish::getStatus,1);
//        //添加排序条件
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//        List<Dish> list = dishService.list(queryWrapper);
//        return Result.success(list);
//    }
    @GetMapping("/list")
    public Result<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList = null;
        String key = "dish_" + dish.getCategoryId() + "_"+dish.getStatus();
        //先从redis获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        if (dishDtoList != null){
            //如果存在，直接返回，无需返回数据库
            return Result.success(dishDtoList);
        }

        //如果不存在就查询数据库转存redis
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        //添加查询状态，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus,1);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);
        dishDtoList = list.stream().map((item)->{
            DishDto dishDto = new DishDto();
            Long categoryId = item.getCategoryId();//分类id
            BeanUtils.copyProperties(item,dishDto);
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category!=null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //当前菜品id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> dishQueryWrapper = new LambdaQueryWrapper<>();
            dishQueryWrapper.eq(DishFlavor::getDishId,dishId);
            List<DishFlavor> flavorList = dishFlavorService.list(dishQueryWrapper);
            dishDto.setFlavors(flavorList);
            return dishDto;
        }).collect(Collectors.toList());
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);
        return Result.success(dishDtoList);
    }

}
