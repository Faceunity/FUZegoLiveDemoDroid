# FUZegoLiveDemoDroid(Android)

## 概述

FUZegoLiveDemoDroid 是集成了 Faceunity 面部跟踪和虚拟道具功能 和 Zego 直播功能的 Demo。

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
try {
    //加载读取人脸数据 v3.mp3 文件
    InputStream is = getAssets().open("v3.mp3");
    byte[] v3data = new byte[is.available()];
    is.read(v3data);
    is.close();
    faceunity.fuSetup(v3data, null, authpack.A());
    //faceunity.fuSetMaxFaces(1);
    Log.e(TAG, "fuSetup");

    //加载美颜 face_beautification.mp3 文件
    is = getAssets().open("face_beautification.mp3");
    byte[] itemData = new byte[is.available()];
    is.read(itemData);
    is.close();
    mFacebeautyItem = faceunity.fuCreateItemFromPackage(itemData);
} catch (IOException e) {
    e.printStackTrace();
}
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

设置视频流宽高

```
@Override
protected int setResolution(int width, int height) {
    Log.e(TAG, "setResolution");
    mCameraWidth = width;
    mCameraHeight = height;
    restartCam();
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

#### 人脸识别状态

获取人脸识别状态，判断并修改UI以提示用户。

```
final int isTracking = faceunity.fuIsTracking();
if (isTracking != faceTrackingStatus) {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
            if (isTracking == 0) {
                Toast.makeText(mContext, "人脸识别失败。", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "人脸识别成功。", Toast.LENGTH_SHORT).show();
            }
        }
    });
    faceTrackingStatus = isTracking;
    Log.e(TAG, "isTracking " + isTracking);
}
```

#### 道具加载

判断isNeedEffectItem（是否需要加载新道具数据flag），由于加载数据比较耗时，防止画面卡顿采用异步加载形式。

##### 发送加载道具Message

```
if (isNeedEffectItem) {
    isNeedEffectItem = false;
    mCreateItemHandler.sendEmptyMessage(CreateItemHandler.HANDLE_CREATE_ITEM);
}
```

##### 自定义Handler，收到Message异步加载道具

```
class CreateItemHandler extends Handler {

        static final int HANDLE_CREATE_ITEM = 1;

        WeakReference<Context> mContext;

        CreateItemHandler(Looper looper, Context context) {
            super(looper);
            mContext = new WeakReference<Context>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLE_CREATE_ITEM:
                    Log.e(TAG, "HANDLE_CREATE_ITEM = " + mEffectFileName);
                    try {
                        if (mEffectFileName.equals("none")) {
                            mEffectItem = 0;
                        } else {
                            InputStream is = mContext.get().getAssets().open(mEffectFileName);
                            byte[] itemData = new byte[is.available()];
                            is.read(itemData);
                            is.close();
                            int tmp = mEffectItem;
                            mEffectItem = faceunity.fuCreateItemFromPackage(itemData);
                            faceunity.fuItemSetParam(mEffectItem, "isAndroid", 1.0);
                            if (tmp != 0) {
                                faceunity.fuDestroyItem(tmp);
                            }
                        }
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
faceunity.fuItemSetParam(mFacebeautyItem, "color_level", mFacebeautyColorLevel);
faceunity.fuItemSetParam(mFacebeautyItem, "blur_level", mFacebeautyBlurLevel);
faceunity.fuItemSetParam(mFacebeautyItem, "filter_name", mFilterName);
faceunity.fuItemSetParam(mFacebeautyItem, "cheek_thinning", mFacebeautyCheeckThin);
faceunity.fuItemSetParam(mFacebeautyItem, "eye_enlarging", mFacebeautyEnlargeEye);
faceunity.fuItemSetParam(mFacebeautyItem, "face_shape", mFaceShape);
faceunity.fuItemSetParam(mFacebeautyItem, "face_shape_level", mFaceShapeLevel);
faceunity.fuItemSetParam(mFacebeautyItem, "red_level", mFacebeautyRedLevel);
```

#### 处理图像数据

使用fuDualInputToTexture后会得到新的texture，返回的texture类型为TEXTURE_2D，其中要求输入的图像分别以内存数组byte[]以及OpenGL纹理的方式。下方代码中mCameraNV21Byte为在PLCameraPreviewListener接口回调中获取的图像数据，i为PLVideoFilterListener接口的onDrawFrame方法的纹理ID参数，i2与i1为图像数据的宽高。

```
int fuTex = faceunity.fuDualInputToTexture(mCameraNV21Byte, i, 1,
        i2, i1, mFrameId++, new int[]{mEffectItem, mFacebeautyItem});
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
try {
    //加载读取人脸数据 v3.mp3 文件
    InputStream is = getAssets().open("v3.mp3");
    byte[] v3data = new byte[is.available()];
    is.read(v3data);
    is.close();
    faceunity.fuSetup(v3data, null, authpack.A());
    //faceunity.fuSetMaxFaces(1);
    Log.e(TAG, "fuSetup");

    //加载美颜 face_beautification.mp3 文件
    is = getAssets().open("face_beautification.mp3");
    byte[] itemData = new byte[is.available()];
    is.read(itemData);
    is.close();
    mFacebeautyItem = faceunity.fuCreateItemFromPackage(itemData);
} catch (IOException e) {
    e.printStackTrace();
}
```

#### 实现父类ZegoVideoFilter的虚函数

Zego 是通过 ZegoVideoFilter 类来控制外部滤镜，因此实现父类 ZegoVideoFilter 的虚函数尤为重要。用于ZEGO不支持同时提供纹理以及byte[]数据，因此只能使用faceunity的单输入方式。采用ZEGO的BUFFER_TYPE_ASYNC_I420_MEM模式。

初始化方法的回调，并把Client类给传递过来。

```
@Override
protected void allocateAndStart(Client client) {
    mZegoClient = client;
    mGlThread = new HandlerThread("video-filter");
    mGlThread.start();
    mGlHandler = new Handler(mGlThread.getLooper());

    final CountDownLatch barrier = new CountDownLatch(1);
    mGlHandler.post(new Runnable() {
        @Override
        public void run() {
            faceunity.fuCreateEGLContext();

            try {
                InputStream is = mContext.getAssets().open("v3.mp3");
                byte[] v3data = new byte[is.available()];
                int len = is.read(v3data);
                is.close();
                faceunity.fuSetup(v3data, null, authpack.A());
//              faceunity.fuSetMaxFaces(3);
                Log.e(TAG, "fuGetVersion " + faceunity.fuGetVersion() + " fuSetup v3 len " + len);

                is = mContext.getAssets().open("face_beautification.mp3");
                byte[] itemData = new byte[is.available()];
                len = is.read(itemData);
                Log.e(TAG, "beautification len " + len);
                is.close();
                mFacebeautyItem = faceunity.fuCreateItemFromPackage(itemData);
                itemsArray[0] = mFacebeautyItem;

                isNeedEffectItem = true;

            } catch (IOException e) {
                e.printStackTrace();
            }

            barrier.countDown();
        }
    });

    mEffectThread = new HandlerThread("video-filter-Effect");
    mEffectThread.start();
    mEffectHandler = new CreateItemHandler(mEffectThread.getLooper());

    mProduceQueue.clear();
    mConsumeQueue.clear();
    mWriteIndex = 0;
    mWriteRemain = 0;
    mMaxBufferSize = 0;

    try {
        barrier.await();
    } catch (InterruptedException e) {
        e.printStackTrace();
    }

    mIsRunning = true;
}
```

销毁方法的回调

```
@Override
protected void stopAndDeAllocate() {
    mIsRunning = false;

    final CountDownLatch barrier = new CountDownLatch(1);
    mGlHandler.post(new Runnable() {
        @Override
        public void run() {
            //Note: 切忌使用一个已经destroy的item
            faceunity.fuDestroyItem(mEffectItem);
            itemsArray[1] = mEffectItem = 0;
            faceunity.fuDestroyItem(mFacebeautyItem);
            itemsArray[0] = mFacebeautyItem = 0;
            faceunity.fuOnDeviceLost();
            isNeedEffectItem = true;

            faceunity.fuReleaseEGLContext();
            mFrameId = 0;

            barrier.countDown();
        }
    });
    try {
        barrier.await();
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    mGlHandler = null;
    mEffectHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
    mEffectHandler = null;

    if (Build.VERSION.SDK_INT >= 18) {
        mGlThread.quitSafely();
        mEffectThread.quitSafely();
    } else {
        mGlThread.quit();
        mEffectThread.quit();
    }
    mGlThread = null;
    mEffectThread = null;

    mZegoClient.destroy();
    mZegoClient = null;
}
```

#### 人脸识别状态

获取人脸识别状态，判断并修改UI以提示用户。

```
final int isTracking = faceunity.fuIsTracking();
if (isTracking != faceTrackingStatus) {
    new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
            if (isTracking == 0) {
                Toast.makeText(mContext, "人脸识别失败。", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "人脸识别成功。", Toast.LENGTH_SHORT).show();
            }
        }
    });
    faceTrackingStatus = isTracking;
    Log.e(TAG, "isTracking " + isTracking);
}
```

#### 道具加载

判断isNeedEffectItem（是否需要加载新道具数据flag），由于加载数据比较耗时，防止画面卡顿采用异步加载形式。

##### 发送加载道具Message

```
if (isNeedEffectItem) {
    isNeedEffectItem = false;
    mCreateItemHandler.sendEmptyMessage(CreateItemHandler.HANDLE_CREATE_ITEM);
}
```

##### 自定义Handler，收到Message异步加载道具

```
class CreateItemHandler extends Handler {

        static final int HANDLE_CREATE_ITEM = 1;

        WeakReference<Context> mContext;

        CreateItemHandler(Looper looper, Context context) {
            super(looper);
            mContext = new WeakReference<Context>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLE_CREATE_ITEM:
                    Log.e(TAG, "HANDLE_CREATE_ITEM = " + mEffectFileName);
                    try {
                        if (mEffectFileName.equals("none")) {
                            mEffectItem = 0;
                        } else {
                            InputStream is = mContext.get().getAssets().open(mEffectFileName);
                            byte[] itemData = new byte[is.available()];
                            is.read(itemData);
                            is.close();
                            int tmp = mEffectItem;
                            mEffectItem = faceunity.fuCreateItemFromPackage(itemData);
                            faceunity.fuItemSetParam(mEffectItem, "isAndroid", 1.0);
                            if (tmp != 0) {
                                faceunity.fuDestroyItem(tmp);
                            }
                        }
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
faceunity.fuItemSetParam(mFacebeautyItem, "color_level", mFacebeautyColorLevel);
faceunity.fuItemSetParam(mFacebeautyItem, "blur_level", mFacebeautyBlurLevel);
faceunity.fuItemSetParam(mFacebeautyItem, "filter_name", mFilterName);
faceunity.fuItemSetParam(mFacebeautyItem, "cheek_thinning", mFacebeautyCheeckThin);
faceunity.fuItemSetParam(mFacebeautyItem, "eye_enlarging", mFacebeautyEnlargeEye);
faceunity.fuItemSetParam(mFacebeautyItem, "face_shape", mFaceShape);
faceunity.fuItemSetParam(mFacebeautyItem, "face_shape_level", mFaceShapeLevel);
faceunity.fuItemSetParam(mFacebeautyItem, "red_level", mFacebeautyRedLevel);
```

#### 处理图像数据

使用faceunity.fuRenderToI420Image来处理画面数据。

```
int fuTex = faceunity.fuRenderToI420Image(pixelBuffer.buffer.array(),
                        pixelBuffer.width, pixelBuffer.height, mFrameId++, itemsArray);
```

#### 推流

视频流以纹理ID的形式传给mClient类

```
mZegoClient.queueInputBuffer(index, pixelBuffer.width, pixelBuffer.height, pixelBuffer.stride, pixelBuffer.timestamp_100n);
```


  [1]: https://github.com/Faceunity/FULiveDemoDroid