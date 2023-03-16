package com.itheima.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.common.BaseContext;
import com.itheima.dto.DishDto;
import com.itheima.dto.OrdersDto;
import com.itheima.entity.*;
import com.itheima.exception.BusinessException;
import com.itheima.mapper.OrdersMapper;
import com.itheima.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private UserService userService;
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private OrderDetailService orderDetailService;


    @Override
    public void submit(Orders orders) {
        //获取当前用户的id
        Long id = BaseContext.getCurrentId();
        //获得条件查询对象
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        //查询当前用户的订单信息
        queryWrapper.eq(ShoppingCart::getUserId, id);
        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);
        //判断当前购物车是否有信息--购物车订单表
        if (list == null || list.size() == 0) {
            throw new BusinessException("购物车内没有商品不能下单");
        }
        //查询用户数据--手机号
        User user = userService.getById(id);
        //查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null) {
            throw new BusinessException("用户地址信息有误");
        }
        //向订单表保存数据,前端传过来三个数据--地址id，备注，支付方式
        long orderId = IdWorker.getId();//订单号

        AtomicInteger amount = new AtomicInteger(0);

        List<OrderDetail> orderDetails = list.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());


        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));//总金额
        orders.setUserId(id);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));

        //向表中插入数据
        this.save(orders);
        //像订单明细表中插入数据
        orderDetailService.saveBatch(orderDetails);
        //清空购物车信息
        shoppingCartService.remove(queryWrapper);
    }
    //查看订单信息

    @Override
    public Page<OrdersDto> page(int page, int pageSize) {
        //
        Page<Orders> ipage = new Page<>(page, pageSize);
        Page<OrdersDto> dtoPage = new Page<>();
        //构造条件查询对象
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //调用查询方法
        this.page(ipage, queryWrapper);

        //复制除属性以外的数据
        BeanUtils.copyProperties(ipage, dtoPage, "records");

        List<Orders> records = ipage.getRecords();

        List<OrdersDto> list = records.stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();

            BeanUtils.copyProperties(item, ordersDto);

            Long id = item.getId();//订单id
            //根据id查询订单明细
            LambdaQueryWrapper<OrderDetail> detailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            detailLambdaQueryWrapper.eq(id != null, OrderDetail::getOrderId, id);
            List<OrderDetail> list1 = orderDetailService.list(detailLambdaQueryWrapper);

            if (list1 != null) {
               ordersDto.setOrderDetails(list1);
            }
            return ordersDto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(list);
        return dtoPage;
    }
}
