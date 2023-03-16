package com.itheima.filter;


import com.alibaba.fastjson.JSON;
import com.itheima.common.BaseContext;
import com.itheima.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    //路径匹配器支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        //1.获取本次请求的URI
        String requestURI = httpServletRequest.getRequestURI();

        log.info("Url为： {}", httpServletRequest.getRequestURI());
        //定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login"
        };
        //2.判断本次请求是否处理
        boolean check = check(requestURI, urls);

        //3.如果不需要处理直接放行
        if (check) {
            chain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }
        //4.网页版需要处理判断登陆状态
        if (httpServletRequest.getSession().getAttribute("employee") != null) {
            log.info("用户已经登录，id为：{}", httpServletRequest.getSession().getAttribute("employee"));
            Long empId = (Long) httpServletRequest.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);
            chain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }
        //log.info("用户未登录");
        //4.移动端需要处理判断登陆状态
        if (httpServletRequest.getSession().getAttribute("user") != null) {
            log.info("用户已经登录，id为：{}", httpServletRequest.getSession().getAttribute("user"));
            Long userId = (Long) httpServletRequest.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);
            chain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }
        log.info("用户未登录");
        //5.如果未登录跳回登陆页面
        httpServletResponse.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }


    /**
     * 检查本次请求是否处理
     *
     * @param requestURI 请求uri
     * @param urls       url
     * @return boolean
     */
    public boolean check(String requestURI, String[] urls) {
        for (String url : urls) {
            if (PATH_MATCHER.match(url, requestURI)) {
                return true;
            }
        }
        return false;
    }
}
