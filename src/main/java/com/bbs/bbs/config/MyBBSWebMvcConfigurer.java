package com.bbs.bbs.config;

import com.bbs.bbs.Interceptor.BBSLoginInterceptor;
import com.bbs.bbs.common.Constants;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
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
    @Resource
    private BBSLoginInterceptor bbsLoginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器
        registry.addInterceptor(bbsLoginInterceptor)
                .excludePathPatterns("/register")    // 注册页不拦截
                .excludePathPatterns("/login")        // 登录页不拦截
                .addPathPatterns("/logout")
                .addPathPatterns("/addPostPage")
                .addPathPatterns("/addPost")
                .addPathPatterns("/addCollect/**")
                .addPathPatterns("/editPostPage/**")
                .addPathPatterns("/editPost")
                .addPathPatterns("/detail/**")
                .addPathPatterns("/uploadFile")
                .addPathPatterns("/uploadFiles")
                .addPathPatterns("/updateUserInfo")
                .addPathPatterns("/updateHeadImg")
                .addPathPatterns("/updatePassword")
                .addPathPatterns("/userCenter")
                .addPathPatterns("/userCenter/**")
                .addPathPatterns("/myCenter")
                .addPathPatterns("/userSet");
    }
    //这行配置的含义：当浏览器请求 `/upload/xxx.jpg` 时，Spring 会去 `D:\\upload\\` 目录下查找 `xxx.jpg` 文件并返回给浏览器显示
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 上传文件的静态资源映射（第06课会用到，这里先配好）
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + Constants.FILE_UPLOAD_DIC);
    }
}
