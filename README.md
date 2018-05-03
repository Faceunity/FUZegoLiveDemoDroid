# FUZegoLiveDemoDroid(Android)

## 概述

FUZegoLiveDemoDroid 是集成了 Faceunity 面部跟踪和虚拟道具功能 和 Zego 直播功能的 Demo。

## 更新SDK

[**Nama SDK发布地址**](https://github.com/Faceunity/FULiveDemoDroid/releases),可查看Nama SDK的所有版本和发布说明。
更新方法为下载Faceunity*.zip解压后替换faceunity模块中的相应库文件。

## SDK使用介绍

 - Faceunity SDK的使用方法请参看 [**Faceunity/FULiveDemoDroid**][1]
 - ZEGO平台直播SDK开发文档

## 集成方法

首先添加相应的SDK库文件与数据文件，然后在BaseLiveActivity（直播界面基类，直播界面UI部分代码）中完成相应Faceunity SDK界面部分代码（界面不作过多的赘述）。
Zego 提供了两种画面处理的方式，分别是外部采样以及外部滤镜。
外部采样：继承ZegoVideoCaptureDevice接口实现采样器VideoCaptureFromFaceunity。需要实现camera使用以及数据采集。
外部滤镜：继承ZegoVideoFilter接口实现VideoFilterFaceUnityDemo用于处理画面。无需维护camera相关的代码，只需使用zego提供的回调即可。

### 外部采样

#### 环境初始化

##### GL环境初始化

GL线程初始化

```
mGLThread = new HandlerThread("VideoCaptureFromFaceunity" + hashCode());
mGLThread.start();
mGLHandler = new Handler(mGLThread.getLooper());
```

GL Context 初始化

```
previewEglBase = EglBase.create(null, EglBase.CONFIG_RGBA);
previewDrawer = new GlRectDrawer();
```

##### Faceunity SDK环境初始化

加载Faceunity SDK所需要的数据文件（读取人脸数据文件、美颜数据文件）：

```
InputStream v3 = context.getAssets().open(BUNDLE_v3);
byte[] v3Data = new byte[v3.available()];
v3.read(v3Data);
v3.close();
faceunity.fuSetup(v3Data, null, authpack.A());

/**
 * 加载优化表情跟踪功能所需要加载的动画数据文件anim_model.bundle；
 * 启用该功能可以使表情系数及avatar驱动表情更加自然，减少异常表情、模型缺陷的出现。该功能对性能的影响较小。
 * 启用该功能时，通过 fuLoadAnimModel 加载动画模型数据，加载成功即可启动。该功能会影响通过fuGetFaceInfo获取的expression表情系数，以及通过表情驱动的avatar模型。
 * 适用于使用Animoji和avatar功能的用户，如果不是，可不加载
 */
InputStream animModel = context.getAssets().open(BUNDLE_anim_model);
byte[] animModelData = new byte[animModel.available()];
animModel.read(animModelData);
animModel.close();
faceunity.fuLoadAnimModel(animModelData);

/**
 * 加载高精度模式的三维张量数据文件ardata_ex.bundle。
 * 适用于换脸功能，如果没用该功能可不加载；如果使用了换脸功能，必须加载，否则会报错
 */
InputStream ar = context.getAssets().open(BUNDLE_ardata_ex);
byte[] arDate = new byte[ar.available()];
ar.read(arDate);
ar.close();
faceunity.fuLoadExtendedARData(arDate);
```

#### 实现Camera.PreviewCallback接口

用于获取摄像头返回的byte[]图像数据。需要重写onPreviewFrame方法：

```
@Override
public void onPreviewFrame(byte[] data, Camera camera) {
    checkIsOnCameraThread();
    if (!isCameraRunning.get()) {
        Log.e(TAG, "onPreviewFrame: Camera is stopped");
        return;
    }
    mCameraNV21Byte = data;
    camera.addCallbackBuffer(data);
    // 主动调用绘制界面方法
    mGLHandler.post(new Runnable() {
        @Override
        public void run() {
            if (!mIsRunning) {
                return;
            }

            if (mIsPreview && previewEglBase.hasSurface()) {
                draw();
            }

        }
    });

}
```

#### 实现父类ZegoVideoCaptureDevice的虚函数

Zego 是通过 ZegoVideoCaptureDevice 类来控制外部采集器的，因此实现父类 ZegoVideoCaptureDevice 的虚函数尤为重要。

初始化方法的回调，并把Client类给传递过来。

```
@Override
protected void allocateAndStart(Client client) {
    Log.e(TAG, "allocateAndStart");
    mClient = client;

    init();
}
```

销毁方法的回调

```
@Override
protected void stopAndDeAllocate() {
    Log.e(TAG, "stopAndDeAllocate");
    uninit();

    mClient.destroy();
    mClient = null;
}
```

开始推流以及预览的回调方法

```
@Override
protected int startCapture() {
    Log.e(TAG, "startCapture");
    if (isCameraRunning.getAndSet(true)) {
        Log.e(TAG, "Camera has already been started.");
        return 0;
    }
    final boolean didPost = maybePostOnCameraThread(new Runnable() {
        @Override
        public void run() {
            // * Create and Start Cam
            createCamOnCameraThread();
            startCamOnCameraThread();
        }
    });
    mGLHandler.post(new Runnable() {
        @Override
        public void run() {
            mIsCapture = true;
        }
    });
    return 0;
}
```

停止推流与预览的回调方法

```
@Override
protected int stopCapture() {
    Log.e(TAG, "stopCapture");
    mGLHandler.post(new Runnable() {
        @Override
        public void run() {
            mIsCapture = false;
        }
    });
    final CountDownLatch barrier = new CountDownLatch(1);
    final boolean didPost = maybePostOnCameraThread(new Runnable() {
        @Override
        public void run() {
            stopCaptureOnCameraThread(true /* stopHandler */);
            releaseCam();
            barrier.countDown();
        }
    });
    if (!didPost) {
        Log.e(TAG, "Calling stopCapture() for already stopped camera.");
        return 0;
    }
    try {
        if (!barrier.await(CAMERA_STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            Log.e(TAG, "Camera stop timeout");
        }
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    Log.d(TAG, "stopCapture done");
    return 0;
}
```

推流数据的类型

```
@Override
protected int supportBufferType() {
    Log.e(TAG, "supportBufferType");
    return PIXEL_BUFFER_TYPE_GL_TEXTURE_2D;
}
```

设置视频流帧率

```
@Override
protected int setFrameRate(int framerate) {
    Log.e(TAG, "setFrameRate");
    mCameraFrameRate = framerate;
    updateRateOnCameraThread(framerate);
    return 0;
}
```

切换摄像头朝向

```
@Override
protected int setFrontCam(int bFront) {
    Log.e(TAG, "setFrontCam");
    mCameraFront = bFront;
    restartCam();
    return 0;
}
```

设置GL环境所要绑定的View（GL绘制画面就在该View上呈现）

```
@Override
protected int setView(View view) {
    Log.e(TAG, "setView");
    if (view instanceof TextureView) {
        setRendererView((TextureView) view);
    } else if (view instanceof SurfaceView) {
        setRendererView((SurfaceView) view);
    }
    return 0;
}
```

#### 道具加载

判断isNeedEffectItem（是否需要加载新道具数据flag），由于加载数据比较耗时，防止画面卡顿采用异步加载形式。

##### 发送加载道具Message

```
public void createItem(Effect item) {
    if (item == null) return;
    mFuItemHandler.removeMessages(FUItemHandler.HANDLE_CREATE_ITEM);
    mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, FUItemHandler.HANDLE_CREATE_ITEM, item));
}
```

##### 自定义Handler，收到Message异步加载道具

```
class FUItemHandler extends Handler {

    static final int HANDLE_CREATE_ITEM = 1;
    static final int HANDLE_CREATE_BEAUTY_ITEM = 2;
    static final int HANDLE_CREATE_ANIMOJI3D_ITEM = 3;

    FUItemHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            //加载道具
            case HANDLE_CREATE_ITEM:
                final Effect effect = (Effect) msg.obj;
                final int newEffectItem = loadItem(effect);
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        if (mItemsArray[ITEM_ARRAYS_EFFECT] > 0) {
                            faceunity.fuDestroyItem(mItemsArray[ITEM_ARRAYS_EFFECT]);
                        }
                        mItemsArray[ITEM_ARRAYS_EFFECT] = newEffectItem;
                        setMaxFaces(effect.maxFace());
                    }
                });
                break;
            //加载美颜bundle
            case HANDLE_CREATE_BEAUTY_ITEM:
                try {
                    InputStream beauty = mContext.getAssets().open(BUNDLE_face_beautification);
                    byte[] beautyData = new byte[beauty.available()];
                    beauty.read(beautyData);
                    beauty.close();
                    mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX] = faceunity.fuCreateItemFromPackage(beautyData);
                    isNeedUpdateFaceBeauty = true;
                    Log.e(TAG, "face beauty item handle " + mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            //加载animoji道具3D抗锯齿bundle
            case HANDLE_CREATE_ANIMOJI3D_ITEM:
                try {
                    InputStream animoji3D = mContext.getAssets().open(BUNDLE_animoji_3d);
                    byte[] animoji3DData = new byte[animoji3D.available()];
                    animoji3D.read(animoji3DData);
                    animoji3D.close();
                    mItemsArray[ITEM_ARRAYS_EFFECT_ABIMOJI_3D] = faceunity.fuCreateItemFromPackage(animoji3DData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
```

#### 美颜参数设置

```
if (isNeedUpdateFaceBeauty && mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX] != 0) {
    //filter_level 滤镜强度 范围0~1 SDK默认为 1
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "filter_level", mFaceBeautyFilterLevel);
    //filter_name 滤镜
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "filter_name", mFilterName.filterName());

    //skin_detect 精准美肤 0:关闭 1:开启 SDK默认为 0
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "skin_detect", mFaceBeautyALLBlurLevel);
    //heavy_blur 美肤类型 0:清晰美肤 1:朦胧美肤 SDK默认为 0
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "heavy_blur", mFaceBeautyType);
    //blur_level 磨皮 范围0~6 SDK默认为 6
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "blur_level", 6 * mFaceBeautyBlurLevel);
    //blur_blend_ratio 磨皮结果和原图融合率 范围0~1 SDK默认为 1
//          faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "blur_blend_ratio", 1);

    //color_level 美白 范围0~1 SDK默认为 1
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "color_level", mFaceBeautyColorLevel);
    //red_level 红润 范围0~1 SDK默认为 1
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "red_level", mFaceBeautyRedLevel);
    //eye_bright 亮眼 范围0~1 SDK默认为 0
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "eye_bright", mBrightEyesLevel);
    //tooth_whiten 美牙 范围0~1 SDK默认为 0
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "tooth_whiten", mBeautyTeethLevel);


    //face_shape_level 美型程度 范围0~1 SDK默认为1
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "face_shape_level", mFaceShapeLevel);
    //face_shape 脸型 0：女神 1：网红 2：自然 3：默认 SDK默认为 3
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "face_shape", mFaceBeautyFaceShape);
    //eye_enlarging 大眼 范围0~1 SDK默认为 0
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "eye_enlarging", mFaceBeautyEnlargeEye);
    //cheek_thinning 瘦脸 范围0~1 SDK默认为 0
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "cheek_thinning", mFaceBeautyCheekThin);
    //intensity_chin 下巴 范围0~1 SDK默认为 0.5    大于0.5变大，小于0.5变小
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_chin", mChinLevel);
    //intensity_forehead 额头 范围0~1 SDK默认为 0.5    大于0.5变大，小于0.5变小
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_forehead", mForeheadLevel);
    //intensity_nose 鼻子 范围0~1 SDK默认为 0
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_nose", mThinNoseLevel);
    //intensity_mouth 嘴型 范围0~1 SDK默认为 0.5   大于0.5变大，小于0.5变小
    faceunity.fuItemSetParam(mItemsArray[ITEM_ARRAYS_FACE_BEAUTY_INDEX], "intensity_mouth", mMouthShape);
    isNeedUpdateFaceBeauty = false;
}
```

#### 处理图像数据

使用fuDualInputToTexture后会得到新的texture，返回的texture类型为TEXTURE_2D，其中要求输入的图像分别以内存数组byte[]以及OpenGL纹理的方式。下方代码中mCameraNV21Byte为在PLCameraPreviewListener接口回调中获取的图像数据，i为PLVideoFilterListener接口的onDrawFrame方法的纹理ID参数，i2与i1为图像数据的宽高。

```
int fuTex = faceunity.fuDualInputToTexture(img, tex, flags, w, h, mFrameId++, mItemsArray);
```

#### 推流

视频流以纹理ID的形式传给mClient类
```
if (mIsCapture) {
    long now = SystemClock.elapsedRealtime();
    mClient.onTextureCaptured(mPreviewTextureId, mCameraWidth, mCameraHeight, now);
}
```

#### 图像数据画面方向错误问题解决

由于推流的纹理ID需要方向正确所以就需要通过FBO获得一个正确方向的纹理ID。
```
if (mPreviewTextureId == 0) {
    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
    mPreviewTextureId = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mCameraWidth, mCameraHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
    mFrameBufferId = GlUtil.generateFrameBuffer(mPreviewTextureId);
} else {
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
}

...

//方向转换
float[] matrix = new float[16];
Matrix.multiplyMM(matrix, 0, mtx, 0, transformationMatrix, 0);
//绘制在FBO上，并获得方向正确的纹理ID mPreviewTextureId。
previewDrawer.drawRgb(fuTex, matrix,
        mCameraWidth, mCameraHeight,
        0, 0,
        mCameraWidth, mCameraHeight);

GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
previewDrawer.drawRgb(mPreviewTextureId, transformationMatrix,
        mViewWidth, mViewHeight,
        0, 0,
        mViewWidth, mViewHeight);
```

### 外部滤镜

#### 环境初始化

##### GL环境初始化

GL线程初始化

```
mGlThread = new HandlerThread("video-filter");
mGlThread.start();
mGlHandler = new Handler(mGlThread.getLooper());
```

GL Context 初始化

```
faceunity.fuCreateEGLContext();
```

##### Faceunity SDK环境初始化

加载Faceunity SDK所需要的数据文件（读取人脸数据文件、美颜数据文件）：

```
InputStream v3 = context.getAssets().open(BUNDLE_v3);
byte[] v3Data = new byte[v3.available()];
v3.read(v3Data);
v3.close();
faceunity.fuSetup(v3Data, null, authpack.A());

/**
 * 加载优化表情跟踪功能所需要加载的动画数据文件anim_model.bundle；
 * 启用该功能可以使表情系数及avatar驱动表情更加自然，减少异常表情、模型缺陷的出现。该功能对性能的影响较小。
 * 启用该功能时，通过 fuLoadAnimModel 加载动画模型数据，加载成功即可启动。该功能会影响通过fuGetFaceInfo获取的expression表情系数，以及通过表情驱动的avatar模型。
 * 适用于使用Animoji和avatar功能的用户，如果不是，可不加载
 */
InputStream animModel = context.getAssets().open(BUNDLE_anim_model);
byte[] animModelData = new byte[animModel.available()];
animModel.read(animModelData);
animModel.close();
faceunity.fuLoadAnimModel(animModelData);

/**
 * 加载高精度模式的三维张量数据文件ardata_ex.bundle。
 * 适用于换脸功能，如果没用该功能可不加载；如果使用了换脸功能，必须加载，否则会报错
 */
InputStream ar = context.getAssets().open(BUNDLE_ardata_ex);
byte[] arDate = new byte[ar.available()];
ar.read(arDate);
ar.close();
faceunity.fuLoadExtendedARData(arDate);
```

#### 实现父类ZegoVideoFilter的虚函数

Zego 是通过 ZegoVideoFilter 类来控制外部滤镜，因此实现父类 ZegoVideoFilter 的虚函数尤为重要。用于ZEGO不支持同时提供纹理以及byte[]数据，因此只能使用faceunity的单输入方式。采用ZEGO的BUFFER_TYPE_ASYNC_I420_MEM模式。

初始化方法的回调，并把Client类给传递过来。

```
@Override
protected void allocateAndStart(Client client) {
    mClient = client;
    mWidth = mHeight = 0;
    if (mDrawer == null) {
        mDrawer = new GlRectDrawer();
    }
    mFURenderer.loadItems();
}
```

销毁方法的回调

```
@Override
protected void stopAndDeAllocate() {
    if (mTextureId != 0) {
        int[] textures = new int[]{mTextureId};
        GLES20.glDeleteTextures(1, textures, 0);
        mTextureId = 0;
    }

    if (mFrameBufferId != 0) {
        int[] frameBuffers = new int[]{mFrameBufferId};
        GLES20.glDeleteFramebuffers(1, frameBuffers, 0);
        mFrameBufferId = 0;
    }

    if (mDrawer != null) {
        mDrawer.release();
        mDrawer = null;
    }
    mFURenderer.destroyItems();
    mClient.destroy();
    mClient = null;
}
```

#### 处理图像数据

使用faceunity.fuRenderToI420Image来处理画面数据。

```
int fuTex = faceunity.fuRenderToTexture(tex, w, h, mFrameId++, mItemsArray, flags);
```

#### 推流

视频流以纹理ID的形式传给mClient类

```
mZegoClient.queueInputBuffer(index, pixelBuffer.width, pixelBuffer.height, pixelBuffer.stride, pixelBuffer.timestamp_100n);
```


  [1]: https://github.com/Faceunity/FULiveDemoDroid