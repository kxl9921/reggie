package com.itheima.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.R;
import com.itheima.entity.Employee;
import com.itheima.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;


    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        //登录功能
        // 1、将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        //将加密后的密码重新赋值给密码
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        //2、根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());

        Employee emp = employeeService.getOne(queryWrapper);
        // 3、如果没有查询到则返回登录失败结果
        if (emp == null) {
            return R.error("登陆失败1");
        }
        // 4、密码比对，如果不一致则返回登录失败结果
        if (!emp.getPassword().equals(password)) {
            return R.error("登陆失败");
        }
        //5、查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if (emp.getStatus() == 0) {
            return R.error("员工账号已禁用");
        }
        // 6、登录成功，将员工id存入Session并返回登录成功结果
        request.getSession().setAttribute("employee", emp.getId());

        return R.success(emp);
    }

    //退出功能
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request) {
        //将页面存储的Session删除
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }


    /**
     * 新增
     *
     * @param request  请求
     * @param employee 员工
     * @return {@link R}<{@link String}>
     */
    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee) {

        //设置初始密码为123456，进行md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
      //  employee.setCreateTime(LocalDateTime.now());
       // employee.setUpdateTime(LocalDateTime.now());
        //获取当前用户的id
       // Long empId = (Long) request.getSession().getAttribute("employee");
       // employee.setCreateUser(empId);
       // employee.setUpdateUser(empId);
        employeeService.save(employee);
        return R.success("新增成功");
    }



    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //构造分页构造器
        Page<Employee> iPage = new Page<>(page, pageSize);
        //构造条件查询器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        //查询条件
        queryWrapper.like(name != null, Employee::getName, name);
        //添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        //执行查询
        employeeService.page(iPage, queryWrapper);

        return R.success(iPage);

   }
    //修改状态
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee) {
        //获取当前存入Session的id
      //  Long id = (Long) request.getSession().getAttribute("employee");
        //修改 修改时间和对象
       // employee.setUpdateTime(LocalDateTime.now());
      //  employee.setUpdateUser(id);
       // long id = Thread.currentThread().getId();
       // log.info("id为：{}"+id);

        //调用方法
        employeeService.updateById(employee);

        return R.success("修改成功");
    }
    //编辑操作
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id){
        //根据id查询内容
        Employee emp = employeeService.getById(id);

        if(emp!=null){
        return R.success(emp);
        }
        return R.error("查询失败");
    }
}
