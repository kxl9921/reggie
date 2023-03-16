package com.itheima.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.dto.DishDto;
import com.itheima.entity.Dish;
import com.itheima.entity.DishFlavor;
import com.itheima.exception.BusinessException;
import com.itheima.mapper.DishMapper;
import com.itheima.service.DishFlavorService;
import com.itheima.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;


    /**
     * 新增菜品同时保存味道
     *
     * @param dishDto 菜dto
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //新增菜品
        this.save(dishDto);
        //获取菜品dishId
        Long id = dishDto.getId();
        //获取到口味的集合把id设置进去
        List<DishFlavor> flavors = dishDto.getFlavors();

        for (DishFlavor flavor : flavors) {
            flavor.setDishId(id);
        }
        //保存口味
        dishFlavorService.saveBatch(dishDto.getFlavors());

    }

    @Override
    public DishDto getByIdWithFlavor(Long id) {
        Dish dish = this.getById(id);
        //对象拷贝
        DishDto dishDto=new DishDto();
        BeanUtils.copyProperties(dish,dishDto);

        //构建条件查询
        LambdaQueryWrapper<DishFlavor> queryWrapper=new LambdaQueryWrapper<>();
        //条件
        queryWrapper.eq(DishFlavor::getDishId,id);
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);


        return dishDto;
    }


    @Override
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表信息
        this.updateById(dishDto);
        //清理当前菜品的对应的口味数据--dishflavor表
        LambdaQueryWrapper<DishFlavor> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());
        dishFlavorService.remove(queryWrapper);
        //添加当前提交过来的口味数据
        List<DishFlavor> flavors = dishDto.getFlavors();

        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishDto.getId());
        }
        dishFlavorService.saveBatch(flavors);

    }

    @Override
    @Transactional
    public void deleteWithFlavor(List<Long> ids) {
        //构造条件查询器
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
       //查询当前id的状态
        queryWrapper.in(ids!=null,Dish::getId,ids);
        //查询传过来的id对应的状态
        List<Dish> list = this.list(queryWrapper);
        for (Dish dish : list) {
            if(dish.getStatus()==0){
                this.removeById(dish.getId());
            }else {
                throw new BusinessException("当前商品正在售卖无法删除");
            }
        }
        //清理当前菜品的对应的口味数据--dishflavor表
        LambdaQueryWrapper<DishFlavor> dishQueryWrapper=new LambdaQueryWrapper<>();
        dishQueryWrapper.in(DishFlavor::getDishId,ids);
        dishFlavorService.remove(dishQueryWrapper);

    }

}
