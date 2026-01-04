package com.example.dangerdriving.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
//FlaskAPI配置：地址端口、预测/健康端点
@Configuration
public class FlaskApiConfig{
    @Value("${flask.api.url:http://localhost:5000}")
    private String flaskApiUrl;
    public String getPredictUrl(){
        return flaskApiUrl+"/predict_with_image";
    }
    public String getHealthUrl(){
        return flaskApiUrl+"/health";
    }
}