package com.example.dangerdriving.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 检测类别名称工具类
 * 用于将英文类别名称转换为中文显示名称和描述
 */
public class DetectionNameUtil {
    
    private static final Map<String, String> CHINESE_NAMES = new HashMap<>();
    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();
    
    static {
        // 类别名称映射（根据pre_data0.3数据集）
        CHINESE_NAMES.put("normal_driving", "正常驾驶");
        CHINESE_NAMES.put("right_hand_messaging", "右手发消息");
        CHINESE_NAMES.put("right_hand_calling", "右手打电话");
        CHINESE_NAMES.put("left_hand_messaging", "左手发消息");
        CHINESE_NAMES.put("left_hand_calling", "左手打电话");
        CHINESE_NAMES.put("adjusting_radio", "调整收音机");
        CHINESE_NAMES.put("drinking_water", "喝水");
        CHINESE_NAMES.put("holding_objects", "手持物品");
        CHINESE_NAMES.put("adjusting_clothing", "整理衣物");
        CHINESE_NAMES.put("talking_to_passenger", "与乘客交谈");
        
        // 危险行为描述
        DESCRIPTIONS.put("normal_driving", "✅ 正常驾驶状态，无危险行为");
        DESCRIPTIONS.put("right_hand_messaging", "⚠️ 严重危险：右手使用手机发消息，会严重分散注意力，极易引发交通事故");
        DESCRIPTIONS.put("right_hand_calling", "⚠️ 严重危险：右手使用手机打电话，会严重分散注意力，严重影响行车安全");
        DESCRIPTIONS.put("left_hand_messaging", "⚠️ 严重危险：左手使用手机发消息，会严重分散注意力，极易引发交通事故");
        DESCRIPTIONS.put("left_hand_calling", "⚠️ 严重危险：左手使用手机打电话，会严重分散注意力，严重影响行车安全");
        DESCRIPTIONS.put("adjusting_radio", "⚠️ 危险：调整收音机，会短暂分散注意力，影响对路况的观察");
        DESCRIPTIONS.put("drinking_water", "⚠️ 危险：驾驶时喝水，会分散注意力，影响对车辆的控制");
        DESCRIPTIONS.put("holding_objects", "⚠️ 危险：手持物品，可能影响对方向盘的控制，存在安全隐患");
        DESCRIPTIONS.put("adjusting_clothing", "⚠️ 危险：整理衣物，会分散注意力，影响对路况的观察");
        DESCRIPTIONS.put("talking_to_passenger", "⚠️ 危险：与乘客交谈，会分散注意力，影响对路况的判断");
    }
    
    /**
     * 获取中文类别名称
     */
    public static String getChineseName(String englishName) {
        return CHINESE_NAMES.getOrDefault(englishName, englishName);
    }
    
    /**
     * 获取危险行为描述
     */
    public static String getDescription(String englishName) {
        return DESCRIPTIONS.getOrDefault(englishName, "未知行为类型");
    }
    
    /**
     * 判断是否为危险行为
     */
    public static boolean isDangerous(String englishName) {
        return !"normal_driving".equals(englishName);
    }
    
    /**
     * 获取危险等级（1-3，3最危险）
     */
    public static int getDangerLevel(String englishName) {
        switch (englishName) {
            // 最危险（3级）：使用手机相关行为
            case "right_hand_messaging":
            case "right_hand_calling":
            case "left_hand_messaging":
            case "left_hand_calling":
                return 3; // 最危险
            // 较危险（2级）：影响驾驶控制的行为
            case "holding_objects":
            case "adjusting_clothing":
                return 2; // 较危险
            // 一般危险（1级）：分散注意力的行为
            case "adjusting_radio":
            case "drinking_water":
            case "talking_to_passenger":
                return 1; // 一般危险
            default:
                return 0; // 无危险（normal_driving）
        }
    }
}

