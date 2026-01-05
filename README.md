项目简介
本项目基于YOLOv8深度学习模型，旨在实现对驾驶过程中多种安全相关行为的自动检测与识别。通过对驾驶员的行为进行分类，如正常驾驶、打电话、发信息、调节收音机等，提升驾驶安全性，预防交通事故。

数据集使用：https://download.csdn.net/download/m0_51381592/42419781
功能特点
使用优化版YOLOv8模型进行目标检测
支持多类驾驶行为识别
利用GPU加速训练，提升训练效率
包含完整的数据集预处理流程
训练过程支持自动早停，防止过拟合
提供详细的训练日志和模型评估结果

环境要求
Python 3.11
PyTorch 2.7.1
CUDA 11.8
ultralytics 8.3.245
其他依赖：numpy, opencv-python, matplotlib, tqdm

数据集预处理及模型训练/  # 包含数据预处理和模型训练的Jupyter Notebook
dangerdriving/           # Java相关项目
flask部署/               # 模型部署


<img width="471" height="373" alt="image" src="https://github.com/user-attachments/assets/a7ad5857-6a17-4dd5-b06a-c0e5e1e4c8e5" />



