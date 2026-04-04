package com.socialmedia.backend.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NgrokHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Bypass ngrok browser warning by sending expected header back and allowing custom request header
        httpResponse.setHeader("ngrok-skip-browser-warning", "true");

        // Robust CORS headers for all responses, including static resources
        String origin = httpRequest.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Vary", "Origin");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");

            // Echo requested headers from preflight, else allow common headers
            String reqHeaders = httpRequest.getHeader("Access-Control-Request-Headers");
            if (reqHeaders == null || reqHeaders.isBlank()) {
                reqHeaders = "Authorization, Content-Type, ngrok-skip-browser-warning";
            }
            httpResponse.setHeader("Access-Control-Allow-Headers", reqHeaders);
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            httpResponse.setHeader("Access-Control-Max-Age", "3600");
        }

        // Short-circuit preflight requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}
