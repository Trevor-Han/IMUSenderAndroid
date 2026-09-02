# IMU Sender Android

通过 Wi‑Fi 将手机加速度计和陀螺仪数据发送到电脑端。

## GitHub 一键构建 APK

请查看 [`README_GITHUB.md`](README_GITHUB.md)。

核心入口：GitHub → Actions → **Build Android APK** → **Run workflow** → 下载 Artifacts。

## 功能

- 电脑 IP / 端口配置
- 加速度计实时数据：ax / ay / az
- 陀螺仪实时数据：gx / gy / gz
- UDP + JSON
- 默认 50 Hz
- 前台服务后台发送
- 发送包数量与传感器状态显示
- 配套 `receiver.py` 电脑接收程序
