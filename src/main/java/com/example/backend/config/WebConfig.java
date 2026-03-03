package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {

    String userHome = System.getProperty("user.home");

    String uploadDir = "file:///" + userHome.replace("\\", "/") + "/remate_uploads/";

    registry.addResourceHandler("/images/**").addResourceLocations(uploadDir);

    System.out.println("이미지 서빙 경로: " + uploadDir);
  }
}
