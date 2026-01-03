package com.example.dangerdriving;

import com.example.dangerdriving.service.FlaskApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
                         Model model) {
        if (file.isEmpty()) {
            model.addAttribute("msg", "文件不能为空");
            return "upload";
        }

        try {
            // 调用 Flask API 进行检测
            Map<String, Object> result = flaskApiService.detectImage(file);
            
            if (result != null && result.containsKey("success") && (Boolean) result.get("success")) {
                // 检测成功
                String annotatedImage = (String) result.get("annotatedImage");
                Boolean hasDanger = (Boolean) result.getOrDefault("hasDanger", false);
                Integer count = (Integer) result.getOrDefault("count", 0);
                
                // 设置检测结果图片
                model.addAttribute("detectionImage", annotatedImage);
                
                // 设置安全状态
                if (hasDanger != null && hasDanger) {
                    model.addAttribute("safetyStatus", "danger");
                    model.addAttribute("msg", "⚠️ 检测到 " + count + " 个危险驾驶行为，请查看标注结果");
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

}