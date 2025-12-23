package com.pandora.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 全局异常处理器
 * 用于捕获和处理应用中的异常,避免打印完整的堆栈信息
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 SSE 连接超时后的授权异常
     * 这是正常现象,不需要打印完整堆栈
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<String> handleAuthorizationDeniedException(
            AuthorizationDeniedException ex,
            HttpServletRequest request) {

        String uri = request.getRequestURI();

        // 如果是 SSE 相关的请求,只记录简单日志
        if (uri.contains("/notifications/stream") || uri.contains("/error")) {
            logger.debug("SSE 连接授权检查: {} - {}", uri, ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }

        // 其他授权异常正常记录
        logger.warn("访问被拒绝: {} - {}", uri, ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
    }

    /**
     * 处理异步请求超时异常
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<String> handleAsyncRequestTimeoutException(
            AsyncRequestTimeoutException ex,
            HttpServletRequest request) {

        String uri = request.getRequestURI();
        logger.debug("异步请求超时: {}", uri);
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Request Timeout");
    }

    /**
     * 处理参数类型转换异常（如日期格式错误、数字格式错误等）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String uri = request.getRequestURI();
        String paramName = ex.getName();
        String paramValue = ex.getValue() != null ? ex.getValue().toString() : "null";

        logger.warn("参数转换失败: {} - 参数: {}, 值: {}", uri, paramName, paramValue);

        return ResponseEntity.badRequest()
                .body(Map.of("error", "参数格式错误: " + paramName + "=" + paramValue));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        String uri = request.getRequestURI();
        logger.debug("静态资源不存在: {} - {}", uri, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<String> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        String uri = request.getRequestURI();
        logger.debug("路由不存在: {} - {}", uri, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
    }

    /**
     * 处理通用异常
     * 只在必要时记录堆栈信息
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {

        String uri = request.getRequestURI();
        String errorMsg = ex.getMessage();

        // 判断是否是 SSE 相关的异常
        if (isSseRelatedError(ex, uri)) {
            logger.debug("SSE 连接异常: {} - {}", uri, errorMsg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        }

        // 其他异常记录详细信息
        logger.error("请求处理异常: {} - {}", uri, errorMsg, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
    }

    /**
     * 判断是否是 SSE 相关的错误
     */
    private boolean isSseRelatedError(Exception ex, String uri) {
        if (uri.contains("/notifications/stream") || uri.contains("/error")) {
            return true;
        }

        String message = ex.getMessage();
        if (message != null) {
            return message.contains("response is already committed")
                    || message.contains("SSE")
                    || message.contains("SseEmitter");
        }

        return false;
    }
}
