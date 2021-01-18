# Zego Express Example Topics Android (Java)

[English](README.md) | [中文](README_Zego.md)

Zego Express Video Android (Java) 示例专题 Demo

## 下载 SDK

此 Repository 中可能缺少运行 Demo 工程所需的 SDK `libZegoExpressEngine.so` 与 `ZegoExpressEngine.jar`，若工程的 `ZegoExpressExample/main/libs` 文件夹中不存在对应的`libZegoExpressEngine.so` 与 `ZegoExpressEngine.jar`， 需要下载并放入 Demo 工程的 `ZegoExpressExample/main/libs` 文件夹中。

**[https://storage.zego.im/express/video/android/zh/zego-express-video-android-zh.zip](https://storage.zego.im/express/video/android/zh/zego-express-video-android-zh.zip)**

最终, `ZegoExpressExample/main/libs` 目录下的结构应如下

```tree
.
├── README.md
├── README_zh.md
└── ZegoExpressExample/main
                        └── libs
                            ├── ZegoExpressEngine.jar
                            ├── arm64-v8a
                            │   └── libZegoExpressEngine.so
                            ├── armeabi-v7a
                            │   └── libZegoExpressEngine.so
                            └── x86
                                └── libZegoExpressEngine.so
```

## 填写 SDK 所需的 appID 与 appSign

到 [ZEGO 管理控制台](https://console-express.zego.im/acount/register) 申请 appID 与 appSign , 然后将其填入 `ZegoExpressExample/common/src/main/java/im/zego/common/GetAppIDConfig.java`，否则在首次编译时会在这个文件报错。
