package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.common.BaseContext;
import com.itheima.common.R;
import com.itheima.entity.ShoppingCart;
import com.itheima.exception.BusinessException;
import com.itheima.service.ShoppingCartService;
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

    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        //取出id
        Long id = BaseContext.getCurrentId();
        shoppingCart.setUserId(id);
        //查询菜品是否在购物车中
        Long dishId = shoppingCart.getDishId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();

        //判断添加进来的是菜品还是套餐
        if (dishId != null) {
            //菜品id不为空添加菜品
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        } else {
            //添加套餐
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        //判断当前购物车是否有这个套餐或者菜品
        ShoppingCart one = shoppingCartService.getOne(queryWrapper);
        if (one != null) {
            //购物车中有这个菜品
            //从数据库查询出来的数量+1
            one.setNumber(one.getNumber() + 1);
            shoppingCartService.updateById(one);
        } else {
            //购物车中没有这个菜品
            //直接调用保存方法
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            one = shoppingCart;
        }
        return R.success(one);
    }

    @PostMapping("/sub")
    public R<ShoppingCart> sun(@RequestBody ShoppingCart shoppingCart) {
        //构造条件查询器对象
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //条件为传来的菜品id还是套餐id
        Long dishId = shoppingCart.getDishId();
        if (dishId != null) {
            //菜品id不为空
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        } else {
            //传进来的为套餐id
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        //查询出来的结果
        ShoppingCart one = shoppingCartService.getOne(queryWrapper);
        if (one != null) {
            //购物车中有这个菜品或者套餐
            //从数据库查询出来的数量+1
            Integer number = one.getNumber();
            if (number > 1) {
                one.setNumber(number - 1);
                shoppingCartService.updateById(one);
            } else if (number == 1) {
                //删除这个菜品或者套餐
                shoppingCartService.remove(queryWrapper);
            }
        }
        return R.success(one);
    }

    /**
     * 购物车中所有信息
     *
     * @return {@link R}<{@link List}<{@link ShoppingCart}>>
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        //当前用户的订单信息
        LambdaQueryWrapper<ShoppingCart> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 清除当前用户购物车内容
     *
     * @return {@link R}<{@link String}>
     */
    @DeleteMapping("/clean")
    public R<String> clean() {
        //获得当前用户的id
        Long id = BaseContext.getCurrentId();
        //构造条件查询器
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //条件为当前用户的id
        queryWrapper.eq(ShoppingCart::getUserId, id);
        //调用删除方法删除购物车中的菜品和套餐
        shoppingCartService.remove(queryWrapper);

        return R.success("删除成功");
    }
}
