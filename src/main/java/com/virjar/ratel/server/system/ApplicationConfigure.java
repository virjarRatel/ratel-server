package com.virjar.ratel.server.system;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Created by tyreke.xu on 07/12/2017.
 */
@Configuration
@ImportResource("classpath:/spring/*.xml")
public class ApplicationConfigure implements WebMvcConfigurer {

    public ApplicationConfigure() {
    }

    @Resource
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor).addPathPatterns("/**");
    }
}
