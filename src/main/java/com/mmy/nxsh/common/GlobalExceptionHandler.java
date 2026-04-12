package com.mmy.nxsh.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 处理业务逻辑抛出的 IllegalArgumentException (比如"已绑定")
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 让 HTTP 状态码保持 400，匹配你的测试预期
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return ApiResponse.failure(400, e.getMessage());
    }

    // 2. 顺便统一处理 @Valid 参数校验失败的异常 (比如"手机号不能为空")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        // 提取具体的校验报错信息
        String errorMsg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.failure(400, errorMsg);
    }

    // 3. 兜底处理其他未知异常
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("未捕获异常: {}", e.getMessage(), e);
        return ApiResponse.failure(500, "服务器内部错误: " + e.getMessage());
    }
    /**
     * 处理缺少请求参数或文件的情况
     */
    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 强制返回 400 状态码
    public ApiResponse<?> handleMissingParamException(Exception e) {
        // 返回你统一的 Result 格式，但状态码给 400，并且提示具体缺啥
        return ApiResponse.failure(400, "缺少必填参数: " + e.getMessage());
    }
}