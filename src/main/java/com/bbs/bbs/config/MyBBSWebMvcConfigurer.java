package com.bbs.bbs.config;

import com.bbs.bbs.common.Constants;
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
    //这行配置的含义：当浏览器请求 `/upload/xxx.jpg` 时，Spring 会去 `D:\\upload\\` 目录下查找 `xxx.jpg` 文件并返回给浏览器显示
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 上传文件的静态资源映射（第06课会用到，这里先配好）
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + Constants.FILE_UPLOAD_DIC);
    }
}
