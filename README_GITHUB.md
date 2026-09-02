# IMU Sender — GitHub 一键构建 APK

这是一个原生 Android IMU 数据发送器：通过 Wi‑Fi 将手机加速度计和陀螺仪数据以 UDP/JSON 方式发送到电脑。

## 最简单的构建方法

### 1. 上传到 GitHub

将整个 `IMUSenderAndroid` 文件夹上传到自己的 GitHub 仓库。

### 2. 打开 Actions

进入 GitHub 仓库 → **Actions** → **Build Android APK**。

点击 **Run workflow**。

- `debug`：推荐，构建速度快，生成可直接安装的测试 APK。
- `release`：当前工程没有配置签名密钥，因此生成的是未签名 release APK，不建议直接安装。后续可以再配置 GitHub Secrets 做正式签名。

### 3. 下载 APK

构建完成后打开本次 workflow → 页面底部 **Artifacts** → 下载：

`IMUSender-debug-APK`

解压后得到：

`IMUSender-debug.apk`

把 APK 发到 Android 手机上即可安装。

## 电脑端接收

电脑与手机连接同一个 Wi‑Fi，在电脑运行：

```bash
python receiver.py --port 5005
```

然后在 App 中填写电脑局域网 IP，例如 `192.168.1.100`，端口填写 `5005`，点击“开始发送”。

## 数据格式

每个 UDP 数据包是一行 JSON：

```json
{"timestamp": 1788300000000, "ax": -0.351000, "ay": 7.381100, "az": 6.912000, "gx": -0.031600, "gy": 0.667600, "gz": -0.187500}
```

默认发送频率为 50 Hz。

## 注意

1. Android 13+ 第一次启动可能会请求通知权限，这是为了前台服务后台运行提示。
2. 手机和电脑必须处于同一个局域网，并确保电脑防火墙允许 UDP 5005 入站。
3. 加速度单位是 `m/s²`；陀螺仪单位是 `rad/s`。
