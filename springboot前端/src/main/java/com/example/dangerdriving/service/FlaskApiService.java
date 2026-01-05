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
@Service
public class FlaskApiService{
    @Autowired
    private FlaskApiConfig flaskApiConfig;
    private final ObjectMapper objectMapper=new ObjectMapper();
    public Map<String,Object>detectImage(MultipartFile file,Double confThreshold,Double iouThreshold)throws IOException{
        String url=flaskApiConfig.getPredictUrl();
        if(confThreshold==null)confThreshold=0.25;//置信度
        if(iouThreshold==null)iouThreshold=0.5;//重叠面积比
        try(CloseableHttpClient httpClient=HttpClients.createDefault()){//自动关闭连接池
            HttpPost httpPost=new HttpPost(url);
            MultipartEntityBuilder builder=MultipartEntityBuilder.create();
            builder.addBinaryBody("file",file.getBytes(),org.apache.http.entity.ContentType.parse(file.getContentType()),file.getOriginalFilename());
            builder.addTextBody("conf",String.valueOf(confThreshold));//置信度参数
            builder.addTextBody("iou",String.valueOf(iouThreshold)); //IoU参数
            httpPost.setEntity(builder.build());
            try(CloseableHttpResponse response=httpClient.execute(httpPost)){
                HttpEntity entity=response.getEntity();
                String responseBody=EntityUtils.toString(entity);
                JsonNode jsonNode=objectMapper.readTree(responseBody);//转树形结构
                Map<String,Object> result=new HashMap<>();
                if(jsonNode.has("success")&&jsonNode.get("success").asBoolean()){//判断后端成功标志
                    if(jsonNode.has("annotated_image")){//有画框图
                        String annotatedImageBase64=jsonNode.get("annotated_image").asText();
                        result.put("annotatedImage","data:image/jpeg;base64,"+annotatedImageBase64);
                    }
                    List<Map<String,Object>>detections=new ArrayList<>();
                    if(jsonNode.has("detections")){//遍历检测框
                        JsonNode detectionsNode=jsonNode.get("detections");
                        for(JsonNode detection:detectionsNode){
                            Map<String,Object> detectionMap=new HashMap<>();
                            detectionMap.put("classId",detection.has("class_id")?detection.get("class_id").asInt():-1);
                            detectionMap.put("className",detection.has("class_name")?detection.get("class_name").asText():"");
                            detectionMap.put("confidence",detection.has("confidence")?detection.get("confidence").asDouble():0.0);
                            if(detection.has("bbox")){//拆坐标
                                Map<String,Double> bbox=new HashMap<>();
                                JsonNode bboxNode=detection.get("bbox");
                                bbox.put("x1",bboxNode.has("x1")?bboxNode.get("x1").asDouble():0.0);
                                bbox.put("y1",bboxNode.has("y1")?bboxNode.get("y1").asDouble():0.0);
                                bbox.put("x2",bboxNode.has("x2")?bboxNode.get("x2").asDouble():0.0);
                                bbox.put("y2",bboxNode.has("y2")?bboxNode.get("y2").asDouble():0.0);
                                detectionMap.put("bbox",bbox);
                            }
                            detections.add(detectionMap);
                        }
                    }
                    result.put("detections",detections);
                    result.put("count",jsonNode.has("count")?jsonNode.get("count").asInt():0);
                    result.put("dangerCount",jsonNode.has("danger_count")?jsonNode.get("danger_count").asInt():0);
                    result.put("safeCount",jsonNode.has("safe_count")?jsonNode.get("safe_count").asInt():0);
                    result.put("confThreshold",jsonNode.has("conf_threshold")?jsonNode.get("conf_threshold").asDouble():0.25);
                    result.put("iouThreshold",jsonNode.has("iou_threshold")?jsonNode.get("iou_threshold").asDouble():0.5);
                    result.put("success",true);//成功标志
                    boolean hasDanger=false;
                    for(Map<String,Object> detection:detections){
                        String className=(String)detection.get("className");
                        if(className!=null&&!className.equals("normal_driving")){
                            hasDanger=true;
                            break;
                        }
                    }
                    result.put("hasDanger",hasDanger);
                }else{//Flask返回错误
                    result.put("success",false);
                    result.put("error",jsonNode.has("error")?jsonNode.get("error").asText():"未知错误");
                }
                return result;
            }
        }
    }
}