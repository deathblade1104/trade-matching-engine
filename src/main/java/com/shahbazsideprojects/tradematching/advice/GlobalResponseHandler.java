package com.shahbazsideprojects.tradematching.advice;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    private static final List<String> SKIP_PATHS = List.of(
            "/v3/api-docs", "/actuator", "/swagger-ui", "/health"
    );

    @Override
    public boolean supports(MethodParameter returnType,
                           Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                 MediaType selectedContentType,
                                 Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                 ServerHttpRequest request, ServerHttpResponse response) {
        String path = request.getURI().getPath();
        boolean skip = SKIP_PATHS.stream().anyMatch(path::contains);
        if (body instanceof ApiResponse<?> || skip) {
            return body;
        }
        return new ApiResponse<>(body);
    }
}
