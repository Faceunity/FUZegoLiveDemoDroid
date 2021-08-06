## 对接第三方 Demo 的 faceunity 模块

SDK 版本为 **7.4.1.0**。关于 SDK 的详细说明，请参看 **[FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/tree/master/doc)**。

--------

## 快速集成方法

### 一、添加 SDK

### 1. build.gradle配置

#### 1.1 allprojects配置
```java
allprojects {
    repositories {
        ...
        maven { url 'http://maven.faceunity.com/repository/maven-public/' }
        ...
  }
}
```

#### 1.2 dependencies导入依赖
```java
dependencies {
...
implementation 'com.faceunity:core:7.4.1.0' // 实现代码
implementation 'com.faceunity:model:7.4.1.0' // 道具以及AI bundle
...
}
```

##### 备注

集成参考文档：FULiveDemoDroid 工程 doc目录

### 2. 其他接入方式-底层库依赖

```java
dependencies {
...
implementation 'com.faceunity:nama:7.4.1.0' //底层库-标准版
implementation 'com.faceunity:nama-lite:7.4.1.0' //底层库-lite版
...
}
```

如需指定应用的 so 架构，请修改 app 模块 build.gradle：

```groovy
android {
    // ...
    defaultConfig {
        // ...
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a'
        }
    }
}
```

如需剔除不必要的 assets 文件，请修改 app 模块 build.gradle：

```groovy
android {
    // ...
    applicationVariants.all { variant ->
        variant.mergeAssetsProvider.configure {
            doLast {
                delete(fileTree(dir: outputDir, includes: ['model/ai_face_processor_lite.bundle',
                                                           'model/ai_hand_processor.bundle',
                                                           'graphics/controller.bundle',
                                                           'graphics/fuzzytoonfilter.bundle',
                                                           'graphics/fxaa.bundle',
                                                           'graphics/tongue.bundle']))
            }
        }
    }
}
```

### 二、使用 SDK

#### 1. 初始化

调用 `FURenderer` 类的  `setup` 方法初始化 SDK，可以在工作线程调用，应用启动后仅需调用一次。

在这个demo中，FURenderer.setup(getApplicationContext());方法的执行在FUBeautyActivity类中

#### 2.创建

调用 `FURenderer` 类的  `prepareRenderer` 方法在 SDK 使用前加载必要的资源。

demo中， 在package com.zego.videofilter.videoFilter;下存在4个集成ZegoVideoFilter的美颜处理类，根据不同的前处理传递数据类型，在 **allocateAndStart**方法或者**dequeueInputBuffer**方法中执行onSurfaceCreated。

**推荐使用 BUFFER_TYPE_SYNC_GL_TEXTURE_2D 格式** 

BUFFER_TYPE_SURFACE_TEXTURE 对接性能最好，但是在荣耀7上会存在**通话过程中内存不断上涨**的bug，**确认是zego bug**

#### 3. 图像处理

调用 `FURenderer` 类的  `onDrawFrameXXX` 方法进行图像处理，有许多重载方法适用于不同数据类型的需求。

与创建类似，美颜处理方法也根据不同的前处理传递数据类型，在**queueInputBuffer**或者**onProcessCallback**或者**onFrameAvailable**中执行。

#### 4. 销毁

调用 `FURenderer` 类的  `release` 方法在 SDK 结束前释放占用的资源。

与创建类似，美颜处理方法也根据不同的前处理传递数据类型，在**dequeueInputBuffer**或者**stopAndDeAllocate**中执行

#### 5. 切换相机

切换相机之后需要sdk参数，详情见 FUBeautyActivity 88~117行

#### 6. 旋转手机

调用 `FURenderer` 类 的  `setDeviceOrientation` 方法，用于重新为 SDK 设置参数。

有关于屏幕旋转的代码都在LifeCycleSensorManager类中，使用时只要注册监听器即可。

```
LifeCycleSensorManager lifeCycleSensorManager = new LifeCycleSensorManager(this, getLifecycle());
lifeCycleSensorManager.setOnAccelerometerChangedListener(new LifeCycleSensorManager.OnAccelerometerChangedListener() {
    @Override
    public void onAccelerometerChanged(float x, float y, float z) {
        if (mFURenderer != null) {
            if (Math.abs(x) > 3 || Math.abs(y) > 3) {
                if (Math.abs(x) > Math.abs(y)) {
                    mFURenderer.setDeviceOrientation(x > 0 ? 0 : 180);
                } else {
                    mFURenderer.setDeviceOrientation(y > 0 ? 90 : 270);
                }
            }
        }
    }
});
```

**注意：** 上面一系列方法的使用，可以前往对应类查看，参考该代码示例接入即可。

### 三、接口介绍

- IFURenderer 是核心接口，提供了创建、销毁、渲染等接口。
- FaceUnityDataFactory 控制四个功能模块，用于功能模块的切换，初始化
- FaceBeautyDataFactory 是美颜业务工厂，用于调整美颜参数。
- PropDataFactory 是道具业务工厂，用于加载贴纸效果。
- MakeupDataFactory 是美妆业务工厂，用于加载美妆效果。
- BodyBeautyDataFactory 是美体业务工厂，用于调整美体参数。

关于 SDK 的更多详细说明，请参看 **[FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/)**。如有对接问题，请联系技术支持。