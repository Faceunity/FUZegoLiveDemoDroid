# FUZegoLiveDemoDroid 快速接入文档

FUZegoLiveDemoDroid 是集成了 FaceUnity 特效功能和 **[即构直播](https://doc.zego.im/CN/693.html)** 的 Demo。

本文是 FaceUnity Nama SDK 快速对接即构直播的导读说明，SDK 版本为 **7.0.1**。关于 SDK 的详细说明，请参看 **[FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/)**。

## 快速集成方法

### 一、导入 SDK

将 faceunity  模块添加到工程中，下面是一些对文件的说明。

- jniLibs 文件夹下 libCNamaSDK.so 和 libfuai.so 是道具绘制和人脸跟踪的动态库。
- libs 文件夹下 nama.jar 提供应用层调用的 JNI 接口。
- assets/graphic 文件夹下 face_beautification.bundle 是美颜道具，body_slim.bundle 是美体道具，face_makeup.bundle 是美妆道具。
- assets/model 文件夹下 ai_face_processor.bundle 是人脸识别算法模型，ai_human_processor.bundle 是人体识别算法模型。
- assets/effect 文件夹下 \*.bundle 是特效贴纸文件，自定义特效贴纸制作的文档和工具，请联系技术支持获取。
- assets/makeup 文件夹下 \*.bundle 是美妆素材文件，自定义美妆制作的文档和工具，请联系技术支持获取。
- com/faceunity/nama/authpack.java 是证书文件，必须提供有效的证书才能运行 Demo，请联系技术支持获取。

### 二、使用 SDK

#### 1. 初始化

在 `FURenderer` 类 的  `initFURenderer` 静态方法是对 FaceUnity SDK 数据初始化的封装，可以在工作线程调用，仅需初始化一次即可。

#### 2.创建

在 `FURenderer` 类 的  `onSurfaceCreated` 方法是对 FaceUnity SDK 每次使用前数据初始化的封装，需要在 GL 线程调用。

#### 3. 图像处理

在 `FURenderer` 类 的  `onDrawFrameXXX` 方法是对 FaceUnity SDK 图像处理方法的封装，该方法有许多重载方法适用于不同的数据类型需求。

#### 4. 销毁

在 `FURenderer` 类 的  `onSurfaceDestroyed` 方法是对 FaceUnity SDK 数据销毁的封装，需要在 GL 线程调用。

#### 5. 切换相机

调用 `FURenderer` 类 的  `onCameraChanged` 方法，用于重新为 SDK 设置参数。

#### 6. 旋转手机

调用 `FURenderer` 类 的  `onDeviceOrientationChanged` 方法，用于重新为 SDK 设置参数。


上面一系列方法的使用，具体在 demo 中 videofilter 模块的 `com.zego.videofilter.videoFilter` 包中，提供了多种输入输出类型的实现，请参考代码示例接入。

### 三、切换道具及调整美颜参数

`FURenderer` 类实现了 `OnFaceUnityControlListener` 接口，而 `OnFaceUnityControlListener` 接口是对切换贴纸道具及调整美颜参数等一系列操作的封装。在 demo 中，`BeautyControlView` 用于实现用户交互，调用了 `OnFaceUnityControlListener` 的方法实现功能。

**至此快速集成完毕，关于 FaceUnity SDK 的更多详细说明，请参看 [FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/)**。