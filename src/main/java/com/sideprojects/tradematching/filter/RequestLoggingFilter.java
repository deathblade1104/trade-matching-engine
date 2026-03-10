package com.sideprojects.tradematching.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final int SLOW_REQUEST_MS = 1000;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = res.getStatus();
            String method = req.getMethod();
            String uri = req.getRequestURI();
            String ip = req.getRemoteAddr();
            String userAgent = req.getHeader("User-Agent");
            if (userAgent == null) userAgent = "";

            log.info("{} {} {} {}ms - {} {}",
                    method, uri, status, duration, ip, userAgent);

            if (duration > SLOW_REQUEST_MS) {
                log.warn("SLOW REQUEST: {} {} took {}ms", method, uri, duration);
            }
            if (uri.contains("/orders") && "POST".equalsIgnoreCase(method)) {
                log.info("ORDER_PLACED: {} - {}ms", uri, duration);
            }
            if (uri.contains("/trades")) {
                log.info("TRADE_QUERY: {} - {}ms", uri, duration);
            }
        }
    }
}
