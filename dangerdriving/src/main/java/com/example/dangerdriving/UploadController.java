package com.example.dangerdriving;

import org.springframework.stereotype.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
@Controller
public class UploadController {

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

        // 1 保存文件
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String folder = "D:/dangerdriving/upload/";
        File target = new File(folder + fileName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            model.addAttribute("msg", "保存失败：" + e.getMessage());
            return "upload";
        }

        /* ======= 以下都是预留，接模型时只改这里 ======= */
        // 结果图（目前空）
        model.addAttribute("detectionImage", null);
        // 安全状态（safe / danger）
        model.addAttribute("safetyStatus", null);
        // 提示语
        model.addAttribute("msg", "文件已保存：" + fileName + "，等待模型分析...");
        /* =========================================== */

        return "upload";
    }

}