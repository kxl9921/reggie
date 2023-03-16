package com.itheima.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.dto.OrdersDto;
import com.itheima.entity.Orders;

public interface OrdersService extends IService<Orders> {

    void submit(Orders orders);

    Page<OrdersDto> page(int page, int pageSize);
}
