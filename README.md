# Android Sensors Project-Video-branch

## 视频推流
支持rtmp视频推流。在`VideoActivity.java`中设置`String url = "rtmp://192.168.0.101:1935/live/home";`推流地址。

目前采用camera，和`setPreviewCallback`，可能由此导致编码无关键帧。在编码处设置了2秒强行触发关键帧解决该问题。

(视频的输入源，如果是通过Camera的PreviewCallback的方式来获取视频数据再喂给MediaCodec的方式是无法控制输出关键帧的数量的。想要控制输出输出关键帧数量就必须通过调用MediaCodec.createInputSurface()方法获取输入Surface，再通过Opengl渲染后喂给MediaCodec才能真正控制关键帧的数量(至于为什么会这样我也没搞明白，希望有明白的能指教一下)。)[来源]https://www.jianshu.com/p/175d1e4ffaad

尝试使用Camerax和createInputSurface来优化该问题。

## 传感器
利用旧的安卓智能机上的传感器及剩余算力为其他项目提供支持。

目前使用TCP进行传输，可页面配置ip地址及端口，保存于SP中。
|  数据显示 | TCP设置  |
|---|---|
| ![data_layout](/img/data_layout.png)  | ![data_layout](/img/TCP_setting.png)  |

| TCP服务端接收的JSON |
| :---: |
|  ![data_layout](/img/TCP_server.png)   |
## 传感器列表
- 陀螺仪
- 加速度计
- 地磁传感器
- GPS


