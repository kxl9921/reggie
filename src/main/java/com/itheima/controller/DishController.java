package com.itheima.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.R;
import com.itheima.dto.DishDto;
import com.itheima.entity.Category;
import com.itheima.entity.Dish;
import com.itheima.entity.DishFlavor;
import com.itheima.service.CategoryService;
import com.itheima.service.DishFlavorService;
import com.itheima.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 分页查询
     *
     * @param page     页面
     * @param pageSize 页面大小
     * @param name     名字
     * @return {@link R}<{@link Page}>
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //分页构造器
        Page<Dish> ipage = new Page<>(page, pageSize);
        Page<DishDto> dtoPage = new Page<>();
        //构造条件查询器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, Dish::getName, name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        //调用查询方法
        dishService.page(ipage, queryWrapper);
        //对象拷贝
        BeanUtils.copyProperties(ipage, dtoPage, "records");

        List<Dish> records = ipage.getRecords();

        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);

            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(list);
        return R.success(dtoPage);
    }

    /**
     * 添加
     *
     * @param dishDto 菜dto
     * @return {@link R}<{@link String}>
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);
        //清理修改某个分类下菜品的缓存数据
        String key="dish_"+ dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);
        return R.success("添加成功");
    }

    /**
     * 回显数据
     *
     * @param id id
     * @return {@link R}<{@link DishDto}>
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id) {

        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改
     *
     * @param dishDto 菜dto
     * @return {@link R}<{@link String}>
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {

        dishService.updateWithFlavor(dishDto);
        //清理所有的菜品缓存数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);
        //清理修改某个分类下菜品的缓存数据
        String key="dish_"+ dishDto.getCategoryId()+"_1";
        redisTemplate.delete(key);

        return R.success("修改成功");
    }


    /**
     * 删除
     *
     * @return {@link R}<{@link String}>
     */
    @DeleteMapping
    public R<String> delete(@RequestParam("ids") List<Long> ids) {
        //删除菜品以及口味
        dishService.deleteWithFlavor(ids);

        return R.success("删除成功");
    }


    /**
     * 更新状态
     *
     * @param status 状态
     * @param ids    id
     * @return {@link R}<{@link String}>
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, Long[] ids) {
        //构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.in(ids != null, Dish::getId, ids);
        //根据传入的id集合进行批量查询
        List<Dish> list = dishService.list(queryWrapper);
        //循环取出每一个菜品对象，设置相应的状态
        for (Dish dish : list) {
            if (dish != null) {
                dish.setStatus(status);
                dishService.updateById(dish);
            }
        }
        return R.success("修改成功");
    }

    /**
     * 把菜
     * 根据套餐id查询当前套餐下的所有菜品
     *
     * @param dish 菜
     * @return {@link R}<{@link List}<{@link Dish}>>
     */
   /* @GetMapping("/list")
    public R<List<Dish>> getDish(Dish dish){
        //构造条件查询器
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
        //根据套餐id查询对应的菜品
        //查询status为1的菜品
        queryWrapper.eq(dish.getCategoryId()!=null,Dish::getCategoryId,dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus,1);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }*/

    /**
     * 移动端展示菜品以及口味
     * 根据套餐id查询当前套餐下的所有菜品
     *
     * @param dish 菜
     * @return {@link R}<{@link List}<{@link Dish}>>
     */
    @GetMapping("/list")
    public R<List<DishDto>> getDish(Dish dish) {
        //先从redis查询是否有数据
        List<DishDto> list1=null;
        String key="dish_"+dish.getCategoryId()+"_"+dish.getStatus();
        list1= (List<DishDto>) redisTemplate.opsForValue().get(key);
        if(list1!=null){
            //如果存在不需要查询数据库
            return R.success(list1);
        }
        //不存在查询数据库

        //构造条件查询器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //根据套餐id查询对应的菜品
        //查询status为1的菜品
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        queryWrapper.eq(Dish::getStatus, 1);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);

       list1 = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);

            //分类id
            Long categoryId = item.getCategoryId();
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //当前菜品的id
            Long id = item.getId();
            //根据菜品id查询口味
            LambdaQueryWrapper<DishFlavor> queryWrapper1=new LambdaQueryWrapper<>();
            queryWrapper1.eq(DishFlavor::getDishId,id);
            List<DishFlavor> list2 = dishFlavorService.list(queryWrapper1);
            dishDto.setFlavors(list2);
            return dishDto;


        }).collect(Collectors.toList());
        //将查询到的数据存到redis中
        redisTemplate.opsForValue().set(key,list1,60, TimeUnit.MINUTES);


        return R.success(list1);
    }
}
