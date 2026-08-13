package com.bbs.bbs.web.error;

import com.bbs.bbs.web.response.ApiResult;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null
                        ? "请求参数不合法" : error.getDefaultMessage())
                .orElse("请求参数不合法");
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolation() {
        return response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "请求参数不合法");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResult<Void>> handleAuthentication() {
        return response(HttpStatus.UNAUTHORIZED,
                ErrorCode.AUTHENTICATION_REQUIRED, "请先登录");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> handleAccessDenied() {
        return response(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "无权访问");
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResult<Void>> handleNotFound() {
        return response(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "资源不存在");
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(BusinessException exception) {
        ErrorCode code = exception.getErrorCode();
        HttpStatus status = switch (code) {
            case DUPLICATE_LOGIN_NAME, DUPLICATE_COLLECTION,
                    CATEGORY_DISABLED, INVALID_STATE_TRANSITION -> HttpStatus.CONFLICT;
            case AUTHENTICATION_REQUIRED, INVALID_CREDENTIALS, ACCOUNT_DISABLED ->
                    HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED, FORBIDDEN_OWNERSHIP -> HttpStatus.FORBIDDEN;
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VALIDATION_ERROR, MODERATION_REASON_REQUIRED -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return response(status, code, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected() {
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }

    private ResponseEntity<ApiResult<Void>> response(HttpStatus status,
                                                     ErrorCode code,
                                                     String message) {
        return ResponseEntity.status(status)
                .body(ApiResult.failure(code.name(), message));
    }
}
