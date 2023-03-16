package com.itheima.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.R;
import com.itheima.dto.DishDto;
import com.itheima.dto.SetmealDto;
import com.itheima.entity.Category;
import com.itheima.entity.Dish;
import com.itheima.entity.Setmeal;
import com.itheima.entity.SetmealDish;
import com.itheima.service.CategoryService;
import com.itheima.service.DishService;
import com.itheima.service.SetmealDishService;
import com.itheima.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;


    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;


    /**
     * 分页查询功能
     *
     * @param page     页面
     * @param pageSize 页面大小
     * @return {@link R}<{@link Page}>
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //构造分页查询
        Page<Setmeal> ipage = new Page<>(page, pageSize);
        Page<SetmealDto> dtoPage = new Page<>();
        //构造条件查询器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(name != null, Setmeal::getName, name);
        //按照修改时间进行排序
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        //调用分页查询方法
        setmealService.page(ipage, queryWrapper);

        //查询完后进行对象的拷贝
        BeanUtils.copyProperties(ipage, dtoPage, "records");
        List<Setmeal> records = ipage.getRecords();

        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);

            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(list);
        return R.success(dtoPage);
    }

    /**
     * 保存套餐同时保存菜品
     *
     * @param setmealDto setmeal dto
     * @return {@link R}<{@link String}>
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        setmealService.saveWithDish(setmealDto);

        return R.success("添加成功");
    }

    /**
     * 删除和批量删除
     *
     * @param ids id
     * @return {@link R}<{@link String}>
     */
    @DeleteMapping
    public R<String> delete(@RequestParam("ids") List<Long> ids) {
        //删除套餐及对应的菜品
        setmealService.deleteWithDish(ids);

        return R.success("删除成功");
    }

    /**
     * 更新状态
     *
     * @param ids    id
     * @param status 状态
     * @return {@link R}<{@link String}>
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable int status, @RequestParam("ids") List<Long> ids) {
        //构造条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.in(ids != null, Setmeal::getId, ids);
        //根据传入的id集合进行批量查询
        List<Setmeal> list = setmealService.list(queryWrapper);
        //循环取出每一个菜品对象，设置相应的状态
        for (Setmeal setmeal : list) {
            if (setmeal != null) {
                setmeal.setStatus(status);
                setmealService.updateById(setmeal);
            }
        }
        return R.success("修改成功");
    }

    /**
     * 回显数据
     *
     * @param id id
     * @return {@link R}<{@link SetmealDto}>
     */
    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable Long id) {
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        return R.success(setmealDto);
    }

    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        setmealService.updateWithDish(setmealDto);

        return R.success("修改成功");
    }

    /**
     * 移动端查看当前套餐下的菜品
     *
     * @param setmeal setmeal
     * @return {@link R}<{@link List}<{@link Setmeal}>>
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        //构造条件查询器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //添加条件
        queryWrapper.eq(setmeal.getCategoryId()!=null,Setmeal::getCategoryId, setmeal.getCategoryId())
                .eq(setmeal.getStatus()!=null,Setmeal::getStatus,setmeal.getStatus());
        //添加排序条件
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        //
        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);
    }
}
