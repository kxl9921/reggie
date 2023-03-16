package com.itheima.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.dto.DishDto;
import com.itheima.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

     void saveWithFlavor(DishDto dishDto);

     DishDto getByIdWithFlavor(Long id);

     void updateWithFlavor(DishDto dishDto);

     void deleteWithFlavor(List<Long> ids);




}
