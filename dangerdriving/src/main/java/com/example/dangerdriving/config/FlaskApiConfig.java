package com.example.dangerdriving.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Flask API 配置类
 * 用于管理 Flask AI 服务的地址和端口
 */
@Configuration
public class FlaskApiConfig {
    
    /**
     * Flask API 服务地址
     * 默认: http://localhost:5000
     * 可在 application.properties 中配置: flask.api.url=http://localhost:5000
     */
    @Value("${flask.api.url:http://localhost:5000}")
    private String flaskApiUrl;
    
    public String getFlaskApiUrl() {
        return flaskApiUrl;
    }
    
    public String getPredictUrl() {
        return flaskApiUrl + "/predict_with_image";
    }
    
    public String getHealthUrl() {
        return flaskApiUrl + "/health";
    }
}

