# FUZegoLiveDemoDroid 快速接入文档

FUZegoLiveDemoDroid 是集成了 FaceUnity 美颜道具贴纸功能和 **[即构直播](https://doc.zego.im/CN/693.html)** 的 Demo。

本文是 FaceUnity SDK 快速对接即构直播的导读说明，关于 `FaceUnity SDK` 的详细说明，请参看 **[FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/)**

## 快速集成方法

### 一、导入 SDK

将 faceunity  模块添加到工程中，下面是一些对文件的说明。

- jniLibs 文件夹下 libnama.so 和 libfuai.so 是人脸跟踪和道具绘制的静态库
- libs 文件夹下 nama.jar 是供应用层调用的 JNI 接口
- assets 文件夹下 AI_model/ai_face_processor.bundle 是人脸识别数据包（自 6.6.0 版本起，v3.bundle 不再使用）
- assets 文件夹下 face_beautification.bundle 是美颜功能数据包
- assets 文件夹下 normal 中的 \*.bundle 文件是特效贴纸文件，自定义特效贴纸制作的文档和工具，请联系技术支持获取。

### 二、使用 SDK

#### 1. 初始化

在 `FURenderer` 类 的  `initFURenderer` 静态方法是对 FaceUnity SDK 一些全局数据初始化的封装，可以在 Application 中调用，也可以在工作线程调用，仅需初始化一次即可。

#### 2.创建

在 `FURenderer` 类 的  `onSurfaceCreated` 方法是对 FaceUnity SDK 每次使用前数据初始化的封装，需要在 GL 线程调用。

#### 3. 图像处理

在 `FURenderer` 类 的  `onDrawFrame` 方法是对 FaceUnity SDK 图像处理方法的封装，该方法有许多重载方法适用于不同的数据类型需求。

即构提供了两种对接方式，这里分别介绍一下：

- 外部采集

开发者需要自己管理 Camera 及其数据回调，代码实现略复杂。类 FUVideoCaptureFromCamera2 是加上美颜效果的示例代码，通过双输入接口进行美颜处理。

- 外部滤镜

即构 SDK负责 Camera 管理和数据采集，开发者只要处理纹理即可，实现方式较简单。类FUVideoFilterGlTexture2dDemo 是加上美颜效果的示例代码，通过单输入接口进行美颜处理。

#### 4. 销毁

在 `FURenderer` 类 的  `onSurfaceDestroyed` 方法是对 FaceUnity SDK 数据销毁的封装，需要在 GL 线程调用。

#### 5. 切换相机

调用 `FURenderer` 类 的  `onCameraChange` 方法，用于重新为 SDK 设置参数。

### 三、切换道具及调整美颜参数

`FURenderer` 类实现了 `OnFaceUnityControlListener` 接口，而 `OnFaceUnityControlListener` 接口是对切换贴纸道具及调整美颜参数等一系列操作的封装。在 demo 中，`BeautyControlView` 用于实现用户交互，调用了 `OnFaceUnityControlListener` 的方法实现功能。

**至此快速集成完毕，关于 FaceUnity SDK 的更多详细说明，请参看 [FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/)**