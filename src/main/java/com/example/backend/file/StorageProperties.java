package com.example.backend.file;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String rootDir, long maxBytes, List<String> allowedContentTypes) {}
