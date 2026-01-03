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
    'normal_driving',
    'talking_on_phone',
    'sleeping',
    'chatting',
    'no_seatbelt',
    'eating',
    'smoking',
    'sudden_acceleration',
    'sudden_brake',
    'sharp_turn'
]

def load_model():
    """加载YOLOv8模型"""
    global model
    model_path = "car\yolov8n.pt"
    if not os.path.exists(model_path):
        raise FileNotFoundError(f"模型文件不存在: {model_path}")
    model = YOLO(model_path)
    print(f"✅ 模型加载成功: {model_path}")
    return model

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
        
        # 执行推理
        results = model(image)
        
        # 解析结果
        detections = []
        for r in results:
            for box in r.boxes:
                # 获取类别ID和置信度
                cls_id = int(box.cls[0])
                confidence = float(box.conf[0])
                
                # 获取边界框坐标（xyxy格式）
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                
                # 获取类别名称
                class_name = class_names[cls_id] if cls_id < len(class_names) else f"class_{cls_id}"
                
                detections.append({
                    "class_id": cls_id,
                    "class_name": class_name,
                    "confidence": round(confidence, 4),
                    "bbox": {
                        "x1": round(x1, 2),
                        "y1": round(y1, 2),
                        "x2": round(x2, 2),
                        "y2": round(y2, 2)
                    }
                })
        
        # 返回结果
        return jsonify({
            "success": True,
            "detections": detections,
            "count": len(detections),
            "image_shape": {
                "height": image.shape[0],
                "width": image.shape[1]
            }
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
        
        # 执行推理
        results = model(image)
        
        # 获取带标注的图片
        annotated_image = results[0].plot()
        
        # 转换为base64
        _, buffer = cv2.imencode('.jpg', annotated_image)
        image_base64 = base64.b64encode(buffer).decode('utf-8')
        
        # 解析检测结果
        detections = []
        for r in results:
            for box in r.boxes:
                cls_id = int(box.cls[0])
                confidence = float(box.conf[0])
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                class_name = class_names[cls_id] if cls_id < len(class_names) else f"class_{cls_id}"
                
                detections.append({
                    "class_id": cls_id,
                    "class_name": class_name,
                    "confidence": round(confidence, 4),
                    "bbox": {
                        "x1": round(x1, 2),
                        "y1": round(y1, 2),
                        "x2": round(x2, 2),
                        "y2": round(y2, 2)
                    }
                })
        
        return jsonify({
            "success": True,
            "annotated_image": image_base64,
            "detections": detections,
            "count": len(detections)
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

