package com.socialmedia.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String projectRoot = System.getProperty("user.dir");
        
        // Serve uploads folder (images, videos, files) from project root
        String projectUploads = "file:" + projectRoot + "/uploads/";
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(projectUploads)
            .setCachePeriod(3600)
            .resourceChain(true);
        
        // Serve static images folder from frontend
        String imagesFolder = "file:" + projectRoot + "/frontend/images/";
        registry.addResourceHandler("/images/**")
            .addResourceLocations(imagesFolder)
            .setCachePeriod(3600)
            .resourceChain(true);
        
        System.out.println("Static resources configured:");
        System.out.println("  /uploads/** -> " + projectUploads);
        System.out.println("  /images/** -> " + imagesFolder);
    }
}

