package com.example.dangerdriving;

import com.example.dangerdriving.service.FlaskApiService;
import com.example.dangerdriving.util.DetectionNameUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UploadController {

    @Autowired
    private FlaskApiService flaskApiService;

    @GetMapping("/")          // 浏览器访问 http://localhost:8080 就进这个方法
    public String index() {
        return "upload";      // 对应 templates/upload.html
    }

    @PostMapping("/detect")
    public String detect(@RequestParam("file") MultipartFile file,
                         @RequestParam(value = "conf", required = false) Double confThreshold,
                         @RequestParam(value = "iou", required = false) Double iouThreshold,
                         Model model) {
        if (file.isEmpty()) {
            model.addAttribute("msg", "文件不能为空");
            return "upload";
        }

        try {
            // 调用 Flask API 进行检测（传递参数）
            Map<String, Object> result = flaskApiService.detectImage(file, confThreshold, iouThreshold);
            
            if (result != null && result.containsKey("success") && (Boolean) result.get("success")) {
                // 检测成功
                String annotatedImage = (String) result.get("annotatedImage");
                Integer count = (Integer) result.getOrDefault("count", 0);
                
                // 获取检测结果列表
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> detections = (List<Map<String, Object>>) result.getOrDefault("detections", new ArrayList<>());
                
                // 处理检测结果，添加中文名称和描述
                List<Map<String, Object>> processedDetections = new ArrayList<>();
                List<Map<String, Object>> dangerDetections = new ArrayList<>();
                
                for (Map<String, Object> detection : detections) {
                    String className = (String) detection.get("className");
                    String chineseName = DetectionNameUtil.getChineseName(className);
                    String description = DetectionNameUtil.getDescription(className);
                    boolean isDangerous = DetectionNameUtil.isDangerous(className);
                    int dangerLevel = DetectionNameUtil.getDangerLevel(className);
                    
                    detection.put("chineseName", chineseName);
                    detection.put("description", description);
                    detection.put("isDangerous", isDangerous);
                    detection.put("dangerLevel", dangerLevel);
                    
                    processedDetections.add(detection);
                    if (isDangerous) {
                        dangerDetections.add(detection);
                    }
                }
                
                // 获取统计信息
                Integer dangerCount = (Integer) result.getOrDefault("dangerCount", dangerDetections.size());
                Integer safeCount = (Integer) result.getOrDefault("safeCount", 0);
                Double resultConfThreshold = (Double) result.getOrDefault("confThreshold", confThreshold != null ? confThreshold : 0.25);
                Double resultIouThreshold = (Double) result.getOrDefault("iouThreshold", iouThreshold != null ? iouThreshold : 0.5);
                
                // 设置检测结果图片
                model.addAttribute("detectionImage", annotatedImage);
                model.addAttribute("detections", processedDetections);
                model.addAttribute("dangerDetections", dangerDetections);
                model.addAttribute("totalCount", count);
                model.addAttribute("dangerCount", dangerCount);
                model.addAttribute("safeCount", safeCount);
                model.addAttribute("confThreshold", resultConfThreshold);
                model.addAttribute("iouThreshold", resultIouThreshold);
                
                // 设置安全状态（基于实际危险行为数量判断）
                if (dangerDetections.size() > 0) {
                    model.addAttribute("safetyStatus", "danger");
                    model.addAttribute("msg", "⚠️ 检测到 " + dangerDetections.size() + " 个危险驾驶行为，请查看详细标注结果");
                } else {
                    model.addAttribute("safetyStatus", "safe");
                    model.addAttribute("msg", "✅ 检测完成，未发现危险驾驶行为");
                }
            } else {
                // 检测失败
                String error = (String) result.getOrDefault("error", "检测失败，请重试");
                model.addAttribute("msg", "❌ " + error);
                model.addAttribute("detectionImage", null);
                model.addAttribute("safetyStatus", null);
            }
        } catch (Exception e) {
            // 异常处理
            model.addAttribute("msg", "❌ 检测过程中发生错误：" + e.getMessage());
            model.addAttribute("detectionImage", null);
            model.addAttribute("safetyStatus", null);
            e.printStackTrace();
        }

        return "upload";
    }

    /**
     * AJAX检测接口，返回JSON数据，不跳转页面
     */
    @PostMapping("/detect-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detectAjax(@RequestParam("file") MultipartFile file,
                                                           @RequestParam(value = "conf", required = false) Double confThreshold,
                                                           @RequestParam(value = "iou", required = false) Double iouThreshold) {
        Map<String, Object> response = new HashMap<>();
        
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("error", "文件不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 调用 Flask API 进行检测（传递参数）
            Map<String, Object> result = flaskApiService.detectImage(file, confThreshold, iouThreshold);
            
            if (result != null && result.containsKey("success") && (Boolean) result.get("success")) {
                // 检测成功
                String annotatedImage = (String) result.get("annotatedImage");
                Integer count = (Integer) result.getOrDefault("count", 0);
                
                // 获取检测结果列表
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> detections = (List<Map<String, Object>>) result.getOrDefault("detections", new ArrayList<>());
                
                // 处理检测结果，添加中文名称和描述
                List<Map<String, Object>> processedDetections = new ArrayList<>();
                List<Map<String, Object>> dangerDetections = new ArrayList<>();
                
                for (Map<String, Object> detection : detections) {
                    String className = (String) detection.get("className");
                    String chineseName = DetectionNameUtil.getChineseName(className);
                    String description = DetectionNameUtil.getDescription(className);
                    boolean isDangerous = DetectionNameUtil.isDangerous(className);
                    int dangerLevel = DetectionNameUtil.getDangerLevel(className);
                    
                    detection.put("chineseName", chineseName);
                    detection.put("description", description);
                    detection.put("isDangerous", isDangerous);
                    detection.put("dangerLevel", dangerLevel);
                    
                    processedDetections.add(detection);
                    if (isDangerous) {
                        dangerDetections.add(detection);
                    }
                }
                
                // 获取统计信息
                Integer dangerCount = (Integer) result.getOrDefault("dangerCount", dangerDetections.size());
                Integer safeCount = (Integer) result.getOrDefault("safeCount", 0);
                Double resultConfThreshold = (Double) result.getOrDefault("confThreshold", confThreshold != null ? confThreshold : 0.25);
                Double resultIouThreshold = (Double) result.getOrDefault("iouThreshold", iouThreshold != null ? iouThreshold : 0.5);
                
                // 构建响应
                response.put("success", true);
                response.put("detectionImage", annotatedImage);  // 前端使用这个字段名
                response.put("annotatedImage", annotatedImage);   // 兼容性字段
                response.put("detections", processedDetections);
                response.put("dangerDetections", dangerDetections);
                response.put("totalCount", count);
                response.put("dangerCount", dangerCount);
                response.put("safeCount", safeCount);
                response.put("confThreshold", resultConfThreshold);
                response.put("iouThreshold", resultIouThreshold);
                
                // 设置安全状态
                if (dangerDetections.size() > 0) {
                    response.put("safetyStatus", "danger");
                    response.put("msg", "⚠️ 检测到 " + dangerDetections.size() + " 个危险驾驶行为，请查看详细标注结果");
                } else {
                    response.put("safetyStatus", "safe");
                    response.put("msg", "✅ 检测完成，未发现危险驾驶行为");
                }
                
                return ResponseEntity.ok(response);
            } else {
                // 检测失败
                String error = (String) result.getOrDefault("error", "检测失败，请重试");
                response.put("success", false);
                response.put("error", error);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            // 异常处理
            response.put("success", false);
            response.put("error", "检测过程中发生错误：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(response);
        }
    }

}