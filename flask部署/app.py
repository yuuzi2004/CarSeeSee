"""
Flask API服务 - YOLOv8模型部署
用于SpringBoot后端调用
"""
from flask import Flask, request, jsonify
from flask_cors import CORS
from ultralytics import YOLO
import cv2
import numpy as np
import base64
from PIL import Image
import io
import os

app = Flask(__name__)
CORS(app)  # 允许跨域请求，方便SpringBoot调用

# 全局变量：加载模型（启动时加载一次，避免重复加载）
model = None
class_names = [
    'normal_driving',        # 0: 正常驾驶
    'right_hand_messaging',  # 1: 右手发消息
    'right_hand_calling',    # 2: 右手打电话
    'left_hand_messaging',   # 3: 左手发消息
    'left_hand_calling',     # 4: 左手打电话
    'adjusting_radio',       # 5: 调整收音机
    'drinking_water',        # 6: 喝水
    'holding_objects',       # 7: 手持物品
    'adjusting_clothing',    # 8: 整理衣物
    'talking_to_passenger'  # 9: 与乘客交谈
]

def load_model():
    """加载YOLOv8模型"""
    global model
    # 优先使用训练好的模型
    model_path = "CarSeeSee\car/best.pt"
    if not os.path.exists(model_path):
        model_path = "best.pt"
        if not os.path.exists(model_path):
            model_path = "yolov8n.pt"
            print(f"⚠️  last.pt和best.pt不存在，使用默认模型: {model_path}")
    if not os.path.exists(model_path):
        raise FileNotFoundError(f"模型文件不存在: {model_path}，请先训练模型或下载预训练模型")
    model = YOLO(model_path)
    print(f"✅ 模型加载成功: {os.path.abspath(model_path)}")
    # 打印模型类别信息
    if hasattr(model, 'names'):
        print(f"📋 模型类别数量: {len(model.names)}")
        print(f"📋 模型类别: {list(model.names.values())[:10]}...")  # 只显示前10个
    return model

def calculate_iou(box1, box2):
    """计算两个边界框的IoU（交并比）"""
    x1_1, y1_1, x2_1, y2_1 = box1
    x1_2, y1_2, x2_2, y2_2 = box2
    
    # 计算交集
    x1_i = max(x1_1, x1_2)
    y1_i = max(y1_1, y1_2)
    x2_i = min(x2_1, x2_2)
    y2_i = min(y2_1, y2_2)
    
    if x2_i <= x1_i or y2_i <= y1_i:
        return 0.0
    
    inter_area = (x2_i - x1_i) * (y2_i - y1_i)
    
    # 计算并集
    box1_area = (x2_1 - x1_1) * (y2_1 - y1_1)
    box2_area = (x2_2 - x1_2) * (y2_2 - y1_2)
    union_area = box1_area + box2_area - inter_area
    
    if union_area == 0:
        return 0.0
    
    return inter_area / union_area

def base64_to_image(base64_string):
    """将base64字符串转换为OpenCV图像"""
    try:
        # 移除可能的前缀（如 data:image/jpeg;base64,）
        if ',' in base64_string:
            base64_string = base64_string.split(',')[1]
        
        # 解码base64
        image_data = base64.b64decode(base64_string)
        
        # 转换为PIL Image
        image = Image.open(io.BytesIO(image_data))
        
        # 转换为RGB（如果是RGBA或其他格式）
        if image.mode != 'RGB':
            image = image.convert('RGB')
        
        # 转换为numpy数组（OpenCV格式）
        image_array = np.array(image)
        # PIL是RGB，OpenCV需要BGR
        image_array = cv2.cvtColor(image_array, cv2.COLOR_RGB2BGR)
        
        return image_array
    except Exception as e:
        raise ValueError(f"Base64解码失败: {str(e)}")

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({
        "status": "ok",
        "message": "YOLOv8 API服务运行正常",
        "model_loaded": model is not None
    })

@app.route('/predict', methods=['POST'])
def predict():
    """
    模型预测接口
    
    请求格式（JSON）:
    {
        "image": "base64编码的图片字符串" 或
        "image_url": "图片URL"（可选）
    }
    
    或者使用multipart/form-data上传文件:
    - file: 图片文件
    """
    try:
        # 检查模型是否已加载
        if model is None:
            return jsonify({
                "success": False,
                "error": "模型未加载"
            }), 500
        
        image = None
        
        # 方式1: 接收base64编码的图片
        if request.is_json:
            data = request.get_json()
            if 'image' in data:
                base64_str = data['image']
                image = base64_to_image(base64_str)
        
        # 方式2: 接收文件上传
        elif 'file' in request.files:
            file = request.files['file']
            if file.filename == '':
                return jsonify({
                    "success": False,
                    "error": "未选择文件"
                }), 400
            
            # 读取文件
            file_bytes = file.read()
            # 转换为numpy数组
            nparr = np.frombuffer(file_bytes, np.uint8)
            image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            
            if image is None:
                return jsonify({
                    "success": False,
                    "error": "无法解析图片文件"
                }), 400
        
        else:
            return jsonify({
                "success": False,
                "error": "请提供图片（base64或文件上传）"
            }), 400
        
        if image is None:
            return jsonify({
                "success": False,
                "error": "无法获取图片"
            }), 400
        
        # 获取检测参数（从请求中获取，如果没有则使用默认值）
        conf_threshold = float(request.form.get('conf', 0.25) if request.form else request.get_json().get('conf', 0.25) if request.is_json else 0.25)
        iou_threshold = float(request.form.get('iou', 0.5) if request.form else request.get_json().get('iou', 0.5) if request.is_json else 0.5)
        
        # 限制参数范围
        conf_threshold = max(0.1, min(0.99, conf_threshold))
        iou_threshold = max(0.1, min(0.99, iou_threshold))
        
        # 执行推理（使用动态参数）
        results = model(image, conf=conf_threshold, iou=iou_threshold)
        
        # 获取模型的实际类别名称（如果模型有自定义类别）
        model_class_names = model.names if hasattr(model, 'names') else class_names
        
        # 解析结果
        raw_detections = []
        for r in results:
            for box in r.boxes:
                cls_id = int(box.cls[0])
                confidence = float(box.conf[0])
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                
                if cls_id in model_class_names:
                    class_name = model_class_names[cls_id]
                elif cls_id < len(class_names):
                    class_name = class_names[cls_id]
                else:
                    class_name = f"class_{cls_id}"
                
                raw_detections.append({
                    "class_id": cls_id,
                    "class_name": class_name,
                    "confidence": confidence,
                    "bbox": [x1, y1, x2, y2]
                })
        
        # 使用IoU进行去重：对于相同类别且IoU>iou_threshold的检测，只保留置信度最高的
        detections = []
        for i, det1 in enumerate(raw_detections):
            is_duplicate = False
            for j, det2 in enumerate(raw_detections):
                if i == j:
                    continue
                # 相同类别且IoU大于阈值，认为是重复检测
                if det1["class_name"] == det2["class_name"]:
                    iou = calculate_iou(det1["bbox"], det2["bbox"])
                    if iou > iou_threshold:
                        # 如果当前检测的置信度更低，跳过
                        if det1["confidence"] < det2["confidence"]:
                            is_duplicate = True
                            break
            
            if not is_duplicate:
                detections.append({
                    "class_id": det1["class_id"],
                    "class_name": det1["class_name"],
                    "confidence": round(det1["confidence"], 4),
                    "bbox": {
                        "x1": round(det1["bbox"][0], 2),
                        "y1": round(det1["bbox"][1], 2),
                        "x2": round(det1["bbox"][2], 2),
                        "y2": round(det1["bbox"][3], 2)
                    }
                })
        
        # 统计检测结果
        total_count = len(detections)
        danger_count = sum(1 for d in detections if d["class_name"] != "normal_driving")
        safe_count = total_count - danger_count
        
        # 返回结果
        return jsonify({
            "success": True,
            "detections": detections,
            "count": total_count,
            "danger_count": danger_count,
            "safe_count": safe_count,
            "image_shape": {
                "height": image.shape[0],
                "width": image.shape[1]
            },
            "conf_threshold": conf_threshold,
            "iou_threshold": iou_threshold
        })
    
    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

@app.route('/predict_with_image', methods=['POST'])
def predict_with_image():
    """
    返回带标注的图片（base64编码）
    方便前端直接显示结果
    支持动态调整置信度和IoU阈值
    """
    try:
        if model is None:
            return jsonify({
                "success": False,
                "error": "模型未加载"
            }), 500
        
        image = None
        
        # 接收图片
        if request.is_json and 'image' in request.get_json():
            base64_str = request.get_json()['image']
            image = base64_to_image(base64_str)
        elif 'file' in request.files:
            file = request.files['file']
            file_bytes = file.read()
            nparr = np.frombuffer(file_bytes, np.uint8)
            image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        else:
            return jsonify({
                "success": False,
                "error": "请提供图片"
            }), 400
        
        if image is None:
            return jsonify({
                "success": False,
                "error": "无法获取图片"
            }), 400
        
        # 获取检测参数（从请求中获取，如果没有则使用默认值）
        conf_threshold = float(request.form.get('conf', 0.25))  # 置信度阈值，默认0.25
        iou_threshold = float(request.form.get('iou', 0.5))     # IoU阈值，默认0.5
        
        # 限制参数范围
        conf_threshold = max(0.1, min(0.99, conf_threshold))
        iou_threshold = max(0.1, min(0.99, iou_threshold))
        
        # 执行推理（使用动态参数）
        results = model(image, conf=conf_threshold, iou=iou_threshold)
        
        # 获取模型的实际类别名称（如果模型有自定义类别）
        model_class_names = model.names if hasattr(model, 'names') else class_names
        
        # 解析结果
        raw_detections = []
        for r in results:
            for box in r.boxes:
                cls_id = int(box.cls[0])
                confidence = float(box.conf[0])
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                
                if cls_id in model_class_names:
                    class_name = model_class_names[cls_id]
                elif cls_id < len(class_names):
                    class_name = class_names[cls_id]
                else:
                    class_name = f"class_{cls_id}"
                
                raw_detections.append({
                    "class_id": cls_id,
                    "class_name": class_name,
                    "confidence": confidence,
                    "bbox": [x1, y1, x2, y2]
                })
        
        # 使用IoU进行去重：对于相同类别且IoU>0.5的检测，只保留置信度最高的
        detections = []
        for i, det1 in enumerate(raw_detections):
            is_duplicate = False
            for j, det2 in enumerate(raw_detections):
                if i == j:
                    continue
                # 相同类别且IoU大于0.5，认为是重复检测
                if det1["class_name"] == det2["class_name"]:
                    iou = calculate_iou(det1["bbox"], det2["bbox"])
                    if iou > 0.5:
                        # 如果当前检测的置信度更低，跳过
                        if det1["confidence"] < det2["confidence"]:
                            is_duplicate = True
                            break
            
            if not is_duplicate:
                detections.append({
                    "class_id": det1["class_id"],
                    "class_name": det1["class_name"],
                    "confidence": round(det1["confidence"], 4),
                    "bbox": {
                        "x1": round(det1["bbox"][0], 2),
                        "y1": round(det1["bbox"][1], 2),
                        "x2": round(det1["bbox"][2], 2),
                        "y2": round(det1["bbox"][3], 2)
                    }
                })
        
        # 获取带标注的图片（使用模型自己的类别名称进行标注）
        annotated_image = results[0].plot()
        
        # 转换为base64
        _, buffer = cv2.imencode('.jpg', annotated_image)
        image_base64 = base64.b64encode(buffer).decode('utf-8')
        
        # 统计检测结果
        total_count = len(detections)
        danger_count = sum(1 for d in detections if d["class_name"] != "normal_driving")
        safe_count = total_count - danger_count
        
        return jsonify({
            "success": True,
            "annotated_image": image_base64,
            "detections": detections,
            "count": total_count,
            "danger_count": danger_count,
            "safe_count": safe_count,
            "conf_threshold": conf_threshold,
            "iou_threshold": iou_threshold
        })
    
    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500

if __name__ == '__main__':
    # 启动时加载模型
    print("🚀 正在启动Flask API服务...")
    load_model()
    
    # 启动Flask服务
    # host='0.0.0.0' 允许外部访问
    # port=5000 默认端口，可根据需要修改
    app.run(host='0.0.0.0', port=5000, debug=False)

