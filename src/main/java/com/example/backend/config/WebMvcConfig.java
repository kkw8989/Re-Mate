package com.example.backend.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path receiptDir = Paths.get("storage", "receipt").toAbsolutePath().normalize();
    registry.addResourceHandler("/images/**").addResourceLocations(receiptDir.toUri().toString());
  }
}
