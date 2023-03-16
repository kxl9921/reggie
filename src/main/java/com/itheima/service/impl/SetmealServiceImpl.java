package com.itheima.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.dto.SetmealDto;
import com.itheima.entity.DishFlavor;
import com.itheima.entity.Setmeal;
import com.itheima.entity.SetmealDish;
import com.itheima.exception.BusinessException;
import com.itheima.mapper.SetmealMapper;
import com.itheima.service.SetmealDishService;
import com.itheima.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 删除套餐同时删除菜品
     *
     * @param ids id
     */
    @Override
    @Transactional
    public void deleteWithDish(List<Long> ids) {
        //构造条件查询对象
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //查询条件
        queryWrapper.in(ids != null, Setmeal::getId, ids);
        //根据传入的id集合进行批量查询
        List<Setmeal> list = this.list(queryWrapper);
        for (Setmeal setmeal : list) {
            if (setmeal.getStatus() == 0) {
                this.removeById(setmeal.getId());
            } else {
                throw new BusinessException("当前套餐正在售卖无法删除");
            }
        }
        //清理当前套餐下的菜品
        LambdaQueryWrapper<SetmealDish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(dishLambdaQueryWrapper);

    }

    /**
     * 回显数据
     *
     * @param id id
     */
    @Override
    public SetmealDto getByIdWithDish(Long id) {
        Setmeal setmeal = this.getById(id);
        //对象拷贝
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);
        //根据套餐id查询
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(list);

        return setmealDto;
    }

    /**
     * 保存套餐的同时保存套餐下的菜品
     *
     * @param setmealDto setmeal dto
     */
    @Override
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐
        this.save(setmealDto);
        //获取套餐的id
        Long id = setmealDto.getId();
        //把获取到的套餐id设置进去
        List<SetmealDish> list = setmealDto.getSetmealDishes();
        for (SetmealDish setmealDish : list) {
            setmealDish.setSetmealId(id);
        }
        //保存套餐下的菜品
        setmealDishService.saveBatch(setmealDto.getSetmealDishes());

    }

    @Override
    public void updateWithDish(SetmealDto setmealDto) {
        //更新套餐表的信息
        this.updateById(setmealDto);
        //清理当前套餐的对应的菜品数据-
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getDishId, setmealDto.getId());
        setmealDishService.remove(queryWrapper);
        //添加当前提交过来的口味数据
        List<SetmealDish> list = setmealDto.getSetmealDishes();

        for (SetmealDish setmealDish : list) {
            setmealDish.setSetmealId(setmealDto.getId());
        }
        setmealDishService.saveBatch(list);


    }
}
