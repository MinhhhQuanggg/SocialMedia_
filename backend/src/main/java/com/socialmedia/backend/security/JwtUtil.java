package com.socialmedia.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expMinutes:120}")
    private long expMinutes;

    public String generateToken(Integer userId, String userName, String roleName) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expMinutes * 60_000);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userName", userName)
                .claim("role", roleName)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(
                        Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token); // nếu sai sẽ throw exception
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Integer extractUserId(String token) {
        Claims claims = parseClaims(token);
        return getUserId(claims);
    }


    public Integer getUserId(Claims claims) {
        return Integer.valueOf(claims.getSubject());
    }
    
    public String getRole(Claims claims) {
        Object role = claims.get("role");
        return role == null ? "USER" : role.toString();
    }
}
