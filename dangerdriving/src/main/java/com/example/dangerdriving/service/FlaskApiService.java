package com.example.dangerdriving.service;

import com.example.dangerdriving.config.FlaskApiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flask API 服务类
 * 负责与 Flask AI 服务进行通信
 */
@Service
public class FlaskApiService {

    @Autowired
    private FlaskApiConfig flaskApiConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用 Flask API 进行图片检测
     * 
     * @param file 上传的图片文件
     * @param confThreshold 置信度阈值（可选，默认0.25）
     * @param iouThreshold IoU阈值（可选，默认0.5）
     * @return 检测结果，包含标注后的图片（base64）和检测信息
     */
    public Map<String, Object> detectImage(MultipartFile file, Double confThreshold, Double iouThreshold) throws IOException {
        String url = flaskApiConfig.getPredictUrl();
        
        // 使用默认值
        if (confThreshold == null) confThreshold = 0.25;
        if (iouThreshold == null) iouThreshold = 0.5;
        
        // 创建 HTTP 客户端
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            
            // 构建 multipart/form-data 请求
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", 
                file.getBytes(),
                org.apache.http.entity.ContentType.parse(file.getContentType()),
                file.getOriginalFilename()
            );
            // 添加检测参数
            builder.addTextBody("conf", String.valueOf(confThreshold));
            builder.addTextBody("iou", String.valueOf(iouThreshold));
            
            httpPost.setEntity(builder.build());
            
            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity);
                
                // 解析 JSON 响应
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                Map<String, Object> result = new HashMap<>();
                
                if (jsonNode.has("success") && jsonNode.get("success").asBoolean()) {
                    // 获取标注后的图片（base64）
                    if (jsonNode.has("annotated_image")) {
                        String annotatedImageBase64 = jsonNode.get("annotated_image").asText();
                        result.put("annotatedImage", "data:image/jpeg;base64," + annotatedImageBase64);
                    }
                    
                    // 获取检测结果
                    List<Map<String, Object>> detections = new ArrayList<>();
                    if (jsonNode.has("detections")) {
                        JsonNode detectionsNode = jsonNode.get("detections");
                        for (JsonNode detection : detectionsNode) {
                            Map<String, Object> detectionMap = new HashMap<>();
                            detectionMap.put("classId", detection.has("class_id") ? detection.get("class_id").asInt() : -1);
                            detectionMap.put("className", detection.has("class_name") ? detection.get("class_name").asText() : "");
                            detectionMap.put("confidence", detection.has("confidence") ? detection.get("confidence").asDouble() : 0.0);
                            if (detection.has("bbox")) {
                                Map<String, Double> bbox = new HashMap<>();
                                JsonNode bboxNode = detection.get("bbox");
                                bbox.put("x1", bboxNode.has("x1") ? bboxNode.get("x1").asDouble() : 0.0);
                                bbox.put("y1", bboxNode.has("y1") ? bboxNode.get("y1").asDouble() : 0.0);
                                bbox.put("x2", bboxNode.has("x2") ? bboxNode.get("x2").asDouble() : 0.0);
                                bbox.put("y2", bboxNode.has("y2") ? bboxNode.get("y2").asDouble() : 0.0);
                                detectionMap.put("bbox", bbox);
                            }
                            detections.add(detectionMap);
                        }
                    }
                    result.put("detections", detections);
                    result.put("count", jsonNode.has("count") ? jsonNode.get("count").asInt() : 0);
                    result.put("dangerCount", jsonNode.has("danger_count") ? jsonNode.get("danger_count").asInt() : 0);
                    result.put("safeCount", jsonNode.has("safe_count") ? jsonNode.get("safe_count").asInt() : 0);
                    result.put("confThreshold", jsonNode.has("conf_threshold") ? jsonNode.get("conf_threshold").asDouble() : 0.25);
                    result.put("iouThreshold", jsonNode.has("iou_threshold") ? jsonNode.get("iou_threshold").asDouble() : 0.5);
                    result.put("success", true);
                    
                    // 判断是否有危险行为（除了 normal_driving 都是危险行为）
                    boolean hasDanger = false;
                    for (Map<String, Object> detection : detections) {
                        String className = (String) detection.get("className");
                        if (className != null && !className.equals("normal_driving")) {
                            hasDanger = true;
                            break;
                        }
                    }
                    result.put("hasDanger", hasDanger);
                    
                } else {
                    // API 返回错误
                    result.put("success", false);
                    result.put("error", jsonNode.has("error") ? jsonNode.get("error").asText() : "未知错误");
                }
                
                return result;
            }
        }
    }

    public boolean healthCheck() {
        try {
            String url = flaskApiConfig.getHealthUrl();
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                org.apache.http.client.methods.HttpGet httpGet = new org.apache.http.client.methods.HttpGet(url);
                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    return response.getStatusLine().getStatusCode() == 200;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }
}

