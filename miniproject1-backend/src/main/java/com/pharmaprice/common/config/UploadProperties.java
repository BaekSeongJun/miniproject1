package com.pharmaprice.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record UploadProperties(String uploadDir) {
}
