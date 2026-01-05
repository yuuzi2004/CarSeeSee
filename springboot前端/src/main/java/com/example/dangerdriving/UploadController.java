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
@Controller //SpringMVC组件，接收Web请求
public class UploadController {
    @Autowired //注入FlaskApiService
    private FlaskApiService flaskApiService;
    @GetMapping("/") //upload.html
    public String index() {
        return "upload";
    }
    @PostMapping("/detect") //同步检测，跳转页面
    public String detect(@RequestParam("file") MultipartFile file,
                         @RequestParam(value="conf",required=false) Double confThreshold,
                         @RequestParam(value="iou",required=false) Double iouThreshold,
                         Model model) { //传数据到视图
        if (file.isEmpty()) { //空文件校验
            model.addAttribute("msg","文件不能为空");
            return "upload";
        }
        try {
            Map<String,Object> result = flaskApiService.detectImage(file,confThreshold,iouThreshold);
            if (result!=null && result.containsKey("success") && (Boolean) result.get("success")) {
                String annotatedImage = (String) result.get("annotatedImage");
                Integer count = (Integer) result.getOrDefault("count",0);
                @SuppressWarnings("unchecked")
                List<Map<String,Object>> detections =
                        (List<Map<String,Object>>) result.getOrDefault("detections",new ArrayList<>());
                List<Map<String,Object>> processedDetections = new ArrayList<>();
                List<Map<String,Object>> dangerDetections = new ArrayList<>();
                for (Map<String,Object> detection : detections) { //逐框翻译
                    String className = (String) detection.get("className");
                    detection.put("chineseName", DetectionNameUtil.getChineseName(className));
                    detection.put("description", DetectionNameUtil.getDescription(className));
                    detection.put("isDangerous", DetectionNameUtil.isDangerous(className));
                    detection.put("dangerLevel", DetectionNameUtil.getDangerLevel(className));
                    processedDetections.add(detection);
                    if ((Boolean)detection.get("isDangerous")) dangerDetections.add(detection);
                }
                //统计回填
                model.addAttribute("detectionImage",annotatedImage);
                model.addAttribute("detections",processedDetections);
                model.addAttribute("dangerDetections",dangerDetections);
                model.addAttribute("totalCount",count);
                model.addAttribute("dangerCount",result.getOrDefault("dangerCount",dangerDetections.size()));
                model.addAttribute("safeCount",result.getOrDefault("safeCount",0));
                model.addAttribute("confThreshold",result.getOrDefault("confThreshold",confThreshold!=null?confThreshold:0.25));
                model.addAttribute("iouThreshold",result.getOrDefault("iouThreshold",iouThreshold!=null?iouThreshold:0.5));
                //安全状态
                if (dangerDetections.size()>0) {
                    model.addAttribute("safetyStatus","danger");
                    model.addAttribute("msg","⚠️检测到"+dangerDetections.size()+"个危险行为");
                } else {
                    model.addAttribute("safetyStatus","safe");
                    model.addAttribute("msg","✅未发现危险行为");
                }
                return "upload";
            } else {//返回错误
                model.addAttribute("msg","❌"+result.getOrDefault("error","检测失败"));
                return "upload";
            }
        } catch (Exception e) {//网络解析异常
            model.addAttribute("msg","❌检测错误："+e.getMessage());
            return "upload";
        }
    }
    @PostMapping("/detect-ajax")//Ajax接口，返回JSON
    @ResponseBody
    public ResponseEntity<Map<String,Object>> detectAjax(@RequestParam("file") MultipartFile file,
                                                         @RequestParam(value="conf",required=false) Double confThreshold,
                                                         @RequestParam(value="iou",required=false) Double iouThreshold) {
        Map<String,Object> response = new HashMap<>();
        if (file.isEmpty()) {//空文件快速400
            response.put("success",false);
            response.put("error","文件不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        try {
            Map<String,Object> result = flaskApiService.detectImage(file,confThreshold,iouThreshold);
            if (result!=null && result.containsKey("success") && (Boolean) result.get("success")) {
                response.put("success",true);
                response.put("detectionImage",result.get("annotatedImage"));//前端img.src
                response.put("annotatedImage",result.get("annotatedImage"));
                //中文处理同同步接口
                @SuppressWarnings("unchecked")
                List<Map<String,Object>> detections = (List<Map<String,Object>>) result.getOrDefault("detections",new ArrayList<>());
                List<Map<String,Object>> processedDetections = new ArrayList<>();
                List<Map<String,Object>> dangerDetections = new ArrayList<>();
                for (Map<String,Object> detection : detections) {
                    String className = (String) detection.get("className");
                    detection.put("chineseName",DetectionNameUtil.getChineseName(className));
                    detection.put("description",DetectionNameUtil.getDescription(className));
                    detection.put("isDangerous",DetectionNameUtil.isDangerous(className));
                    detection.put("dangerLevel",DetectionNameUtil.getDangerLevel(className));
                    processedDetections.add(detection);
                    if ((Boolean)detection.get("isDangerous")) dangerDetections.add(detection);
                }
                response.put("detections",processedDetections);
                response.put("dangerDetections",dangerDetections);
                response.put("totalCount",result.getOrDefault("count",0));
                response.put("dangerCount",result.getOrDefault("dangerCount",dangerDetections.size()));
                response.put("safeCount",result.getOrDefault("safeCount",0));
                response.put("confThreshold",result.getOrDefault("confThreshold",confThreshold!=null?confThreshold:0.25));
                response.put("iouThreshold",result.getOrDefault("iouThreshold",iouThreshold!=null?iouThreshold:0.5));
                if (dangerDetections.size()>0) {
                    response.put("safetyStatus","danger");
                    response.put("msg","⚠️检测到"+dangerDetections.size()+"个危险行为");
                } else {
                    response.put("safetyStatus","safe");
                    response.put("msg","✅未发现危险行为");
                }
                return ResponseEntity.ok(response);//200+JSON
            } else {//失败
                response.put("success",false);
                response.put("error",result.getOrDefault("error","检测失败"));
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {//异常转500
            response.put("success",false);
            response.put("error","检测错误："+e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}