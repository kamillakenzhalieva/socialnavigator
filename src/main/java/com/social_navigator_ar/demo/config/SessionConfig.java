package com.social_navigator_ar.demo.config;

import jakarta.servlet.ServletContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SessionConfig implements WebMvcConfigurer {
    public void configureServletContext(ServletContext servletContext) {
        servletContext.getSessionCookieConfig().setMaxAge(3600); // 1 час
    }
}