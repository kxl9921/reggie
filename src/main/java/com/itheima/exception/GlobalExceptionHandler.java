package com.itheima.exception;


import com.itheima.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 异常处理程序
     *
     * @param e e
     * @return {@link R}<{@link String}>
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException e) {
        log.error(e.getMessage());
        if (e.getMessage().contains("Duplicate entry")) {
            String[] split = e.getMessage().split(" ");
            String msg = split[2] + "已存在";
            return R.error(msg);
        }
        return R.error("未知错误");
    }

    /**
     * 异常处理程序
     *
     * @param e e
     * @return {@link R}<{@link String}>
     */
    @ExceptionHandler(BusinessException.class)
    public R<String> exceptionHandler(BusinessException e) {
        log.error(e.getMessage());

        return R.error(e.getMessage());
    }


}

