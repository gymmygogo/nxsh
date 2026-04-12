package com.mmy.nxsh.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;

    public CorsConfig(TokenInterceptor tokenInterceptor) {
        this.tokenInterceptor = tokenInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 登录、注册、发送验证码
                        "/elderly/login", "/elderly/register", "/elderly/sendCode",
                        "/family/login", "/family/register", "/family/sendCode",
                        "/family/sendVerify",
                        // WebSocket 端点
                        "/ws/**",
                        // 静态资源
                        "/error", "/favicon.ico"
                );
    }
}
