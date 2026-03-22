package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String uploadDir = System.getProperty("user.home") + "/remate_uploads/";
    registry.addResourceHandler("/images/**").addResourceLocations("file:" + uploadDir);
  }
}
