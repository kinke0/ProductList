package com.superpower.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.image-storage-path:./uploads/images}")
    private String imageStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/images/file/**")
                .addResourceLocations("file:" + imageStoragePath + "/");
        String reqPath = imageStoragePath.replace("images", "requirements");
        registry.addResourceHandler("/api/requirements/file/**")
                .addResourceLocations("file:" + reqPath + "/");
    }
}
