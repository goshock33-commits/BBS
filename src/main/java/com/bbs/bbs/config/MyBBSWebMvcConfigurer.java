package com.bbs.bbs.config;

import com.bbs.bbs.upload.UploadStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-23 23:03:49
 * @Version 1.0
 */
@Configuration
public class MyBBSWebMvcConfigurer implements WebMvcConfigurer {
    private final UploadStorage uploadStorage;

    public MyBBSWebMvcConfigurer(UploadStorage uploadStorage) {
        this.uploadStorage = uploadStorage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 上传文件的静态资源映射（第06课会用到，这里先配好）
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(uploadStorage.resourceLocation());
    }
}
