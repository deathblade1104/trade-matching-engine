package com.sideprojects.tradematching.advice;

import com.sideprojects.tradematching.exception.BadRequestException;
import com.sideprojects.tradematching.exception.ForbiddenException;
import com.sideprojects.tradematching.exception.ResourceConflictException;
import com.sideprojects.tradematching.exception.ResourceNotFoundException;
import com.sideprojects.tradematching.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        logError(request, HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getStatus());
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceConflict(
            ResourceConflictException ex, HttpServletRequest request) {
        logError(request, HttpStatus.CONFLICT, ex.getMessage(), ex);
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.CONFLICT)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getStatus());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        logError(request, HttpStatus.UNAUTHORIZED, ex.getMessage(), ex);
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.UNAUTHORIZED)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getStatus());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<?>> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        logError(request, HttpStatus.FORBIDDEN, ex.getMessage(), ex);
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.FORBIDDEN)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getStatus());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        logError(request, HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleInternalServerError(
            Exception ex, HttpServletRequest request) {
        logError(request, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        ApiError apiError = ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getStatus());
    }

    private void logError(HttpServletRequest request, HttpStatus status, String message, Throwable ex) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null && auth.getPrincipal() != null
                ? auth.getPrincipal().toString()
                : null;
        log.error("statusCode={} path={} user_id={} error.message={}",
                status.value(), request.getRequestURI(), userId, message, ex);
    }
}
