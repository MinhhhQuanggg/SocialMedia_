package com.socialmedia.backend.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import com.socialmedia.backend.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * MVP interceptor:
 * - Lấy userId từ query param ?userId=3
 * - Gắn vào attributes để handler dùng.
 *
 * Sau này bạn thay bằng JWT:
 * - đọc header Authorization
 * - verify token
 * - set userId
 */
@Component
public class UserIdHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UserIdHandshakeInterceptor.class);

    private final JwtUtil jwtUtil;

    public UserIdHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        if (!(request instanceof ServletServerHttpRequest servletReq)) {
            return false;
        }

        var httpReq = servletReq.getServletRequest();
        String authHeader = httpReq.getHeader("Authorization");

        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            logger.debug("WS handshake: token provided in Authorization header (masked)");
        } else {
            // allow token in query param for browser clients (ws cannot set custom headers easily)
            token = httpReq.getParameter("token");
            logger.debug("WS handshake: token provided in query parameter: {}", token != null ? "yes" : "no");
        }

        if (token == null) {
            logger.warn("WS handshake rejected: no token provided (header or query param)");
            return false;
        }

        // ✅ validate token
        if (!jwtUtil.validateToken(token)) {
            logger.warn("WS handshake rejected: invalid or expired token (user may need to re-login)");
            return false;
        }

        // ✅ extract userId
        Integer userId = jwtUtil.extractUserId(token);
        logger.info("WS handshake accepted for userId={}", userId);

        attributes.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
