package com.mmy.nxsh.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final String ELDERLY_TOKEN_PREFIX = "elderly:token:";
    private static final String FAMILY_TOKEN_PREFIX = "family:token:";

    private final StringRedisTemplate stringRedisTemplate;

    public TokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或token已过期\"}");
            return false;
        }

        // 移除可能的 "Bearer " 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 检查 elderly 或 family token 是否存在于 Redis
        String elderlyId = stringRedisTemplate.opsForValue().get(ELDERLY_TOKEN_PREFIX + token);
        String familyId = stringRedisTemplate.opsForValue().get(FAMILY_TOKEN_PREFIX + token);

        if (elderlyId == null && familyId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"token无效或已过期，请重新登录\"}");
            return false;
        }

        // 将用户信息存入 request attribute，供后续 Controller 使用
        if (elderlyId != null) {
            request.setAttribute("currentUserId", Long.parseLong(elderlyId));
            request.setAttribute("currentUserType", "elderly");
        } else {
            request.setAttribute("currentUserId", Long.parseLong(familyId));
            request.setAttribute("currentUserType", "family");
        }

        return true;
    }
}
