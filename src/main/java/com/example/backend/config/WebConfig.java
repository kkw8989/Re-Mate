package com.example.backend.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path receiptDir = Paths.get("storage", "receipt").toAbsolutePath().normalize();
    String uploadDir = receiptDir.toUri().toString();

    registry.addResourceHandler("/images/**").addResourceLocations(uploadDir);

    System.out.println("영수증 이미지 서빙 경로: " + uploadDir);
  }
}
