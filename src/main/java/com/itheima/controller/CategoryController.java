package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.R;
import com.itheima.entity.Category;
import com.itheima.mapper.CategoryMapper;
import com.itheima.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增
     *
     * @param category 类别
     * @return {@link R}<{@link String}>
     */
    @PostMapping
    public R<String> save(@RequestBody Category category) {
        categoryService.save(category);
        return R.success("添加成功");
    }

    /**
     * 分页查询
     *
     * @param page     页面
     * @param pageSize 页面大小
     * @return {@link R}<{@link Page}>
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize) {
        //构造分页构造器
        Page<Category> iPage = new Page<>(page, pageSize);
        //构造条件查询器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        //排序条件
        queryWrapper.orderByAsc(Category::getSort);
        //执行查询
        categoryService.page(iPage,queryWrapper);

        return R.success(iPage);
    }

    @DeleteMapping
    public R<String> delete(Long ids){
        //根据id删除
       categoryService.remove(ids);

        return R.success("删除成功");
    }

    @PutMapping
    public R<String> update(@RequestBody Category category){
        //修改
        categoryService.updateById(category);
        return R.success("修改成功");
    }
    @GetMapping("/list")
    public R<List<Category>> list(Category category){
            //条件构造器
            LambdaQueryWrapper<Category> queryWrapper=new LambdaQueryWrapper<>();
            //查询条件
            queryWrapper.eq(category.getType()!=null,Category::getType,category.getType());
            //添加排序条件
            queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);
            //查询当前类下所有的套餐或者菜品
            List<Category> list = categoryService.list(queryWrapper);

            return R.success(list);
    }

}
