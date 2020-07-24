package com.zego.videofilter.faceunity;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.faceunity.wrapper.faceunity;
import com.zego.videofilter.faceunity.entity.Effect;
import com.zego.videofilter.faceunity.entity.Makeup;
import com.zego.videofilter.faceunity.param.BeautificationParam;
import com.zego.videofilter.faceunity.param.BodySlimParam;
import com.zego.videofilter.faceunity.param.MakeupParam;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 基于 Nama SDK 封装，方便集成，使用步骤：
 * <p>
 * 1. OnFaceUnityControlListener 定义了 UI 上的交互接口
 * 2. FURenderer.Builder 构造器设置相应的参数，
 * 3. SurfaceView 创建和销毁时，分别调用 onSurfaceCreated 和 onSurfaceDestroyed
 * 4. 相机朝向变化和设备方向变化时，分别调用 onCameraChanged 和 onDeviceOrientationChanged
 * 4. 处理图像时调用 onDrawFrame，针对不同数据类型，提供了纹理和 buffer 输入多种方案
 * </p>
 */
public class FURenderer implements OnFaceUnityControlListener {
    private static final String TAG = "FURenderer";
    /**
     * 输入的 texture 类型，OES 或 2D
     */
    public static final int INPUT_EXTERNAL_OES_TEXTURE = faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE;
    public static final int INPUT_2D_TEXTURE = 0;
    /**
     * 输入的 buffer 格式，NV21、I420 或 RGBA
     */
    public static final int INPUT_FORMAT_NV21 = faceunity.FU_FORMAT_NV21_BUFFER;
    public static final int INPUT_FORMAT_I420 = faceunity.FU_FORMAT_I420_BUFFER;
    public static final int INPUT_FORMAT_RGBA = faceunity.FU_FORMAT_RGBA_BUFFER;

    /**
     * 算法检测类型
     */
    public static final int TRACK_TYPE_FACE = faceunity.FUAITYPE_FACEPROCESSOR;
    public static final int TRACK_TYPE_HUMAN = faceunity.FUAITYPE_HUMAN_PROCESSOR;

    /* 更新美颜参数标记 */
    private volatile boolean mIsNeedUpdateFaceBeauty = true;
    /* 美颜和滤镜的默认参数，具体的参数定义，请看 BeautificationParam 和 FilterParam 类 */
    private int mIsBeautyOn = 1;
    private String mFilterName = BeautificationParam.ZIRAN_1;// 滤镜：自然 1
    private float mFilterLevel = 0.4f;//滤镜强度
    private float mBlurLevel = 0.7f;//磨皮程度
    private float mBlurType = 2.0f;//磨皮类型：精细磨皮
    private float mColorLevel = 0.3f;//美白
    private float mRedLevel = 0.3f;//红润
    private float mEyeBright = 0.0f;//亮眼
    private float mToothWhiten = 0.0f;//美牙
    private float mFaceShape = 4;//脸型：精细变形
    private float mFaceShapeLevel = 1.0f;//变形程度
    private float mCheekThinning = 0f;//瘦脸
    private float mCheekV = 0.5f;//V脸
    private float mCheekNarrow = 0f;//窄脸
    private float mCheekSmall = 0f;//小脸
    private float mEyeEnlarging = 0.4f;//大眼
    private float mIntensityChin = 0.3f;//下巴
    private float mIntensityForehead = 0.3f;//额头
    private float mIntensityMouth = 0.4f;//嘴形
    private float mIntensityNose = 0.5f;//瘦鼻
    private float mMicroPouch = 0f; // 去黑眼圈
    private float mMicroNasolabialFolds = 0f; // 去法令纹
    private float mMicroSmile = 0f; // 微笑嘴角
    private float mMicroCanthus = 0f; // 眼角
    private float mMicroPhiltrum = 0.5f; // 人中
    private float mMicroLongNose = 0.5f; // 鼻子长度
    private float mMicroEyeSpace = 0.5f; // 眼睛间距
    private float mMicroEyeRotate = 0.5f; // 眼睛角度

    /* 美体的默认参数，具体的参数定义，请看 BodySlimParam 类 */
    private float mBodySlimStrength = 0.0f; // 瘦身
    private float mLegSlimStrength = 0.0f; // 长腿
    private float mWaistSlimStrength = 0.0f; // 细腰
    private float mShoulderSlimStrength = 0.5f; // 美肩
    private float mHipSlimStrength = 0.0f; // 美胯
    private float mHeadSlimStrength = 0.0f; // 小头
    private float mLegThinSlimStrength = 0.0f; // 瘦腿
    private boolean mIsNeedBodySlim;

    /* 美妆组合和强度 */
    private Makeup mMakeup;
    private float mMakeupIntensity = 1f;
    // 美妆点位是否镜像
    private boolean mIsMakeupFlipPoints;

    /* 句柄数组下标，分别代表美颜、贴纸、美妆和美体 */
    private static final int ITEMS_ARRAY_FACE_BEAUTY = 0;
    private static final int ITEMS_ARRAY_EFFECT = 1;
    private static final int ITEMS_ARRAY_FACE_MAKEUP = 2;
    private static final int ITEMS_ARRAY_BODY_SLIM = 3;
    /* 句柄数组长度 4 */
    private static final int ITEMS_ARRAY_COUNT = 4;
    /* 存放美颜和贴纸句柄的数组 */
    private final int[] mItemsArray = new int[ITEMS_ARRAY_COUNT];
    private final Context mContext;
    /* IO 线程 Handler */
    private Handler mFuItemHandler;
    /* 递增的帧 ID */
    private int mFrameId = 0;
    /* 是否使用美颜，默认需要 */
    private boolean mIsNeedFaceBeauty = true;
    /* 贴纸道具 */
    private Effect mEffect;
    /* 同时识别的最大人脸数，默认 4 */
    private int mMaxFaces = 4;
    /* 同时识别的最大人体数，默认 1 */
    private int mMaxHumans = 1;
    /* 是否手动创建 EGLContext，默认不创建 */
    private boolean mIsCreateEGLContext = false;
    /* 输入图像的纹理类型，默认 OES */
    private int mInputTextureType = INPUT_EXTERNAL_OES_TEXTURE;
    /* 输入图像的 buffer 类型，此项一般不用改 */
    private int mInputImageFormat = 0;
    /* 输入图像的方向，默认前置相机 270 */
    private int mInputImageOrientation = 270;
    /* 设备方向，默认竖屏 */
    private int mDeviceOrientation = 90;
    /* 人脸识别方向，默认 1，通过 createRotationMode 方法获得 */
    private int mRotationMode = faceunity.FU_ROTATION_MODE_90;
    /* 相机前后方向，默认前置相机  */
    private int mCameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;
    /* 事件队列 */
    private final ArrayList<Runnable> mEventQueue = new ArrayList<>(16);
    /* 事件队列操作锁 */
    private final Object mLock = new Object();
    /* GL 线程 ID */
    private long mGlThreadId;
    /* 是否已经全局初始化，确保只初始化一次 */
    private static boolean sIsInited;

    /**
     * 初始化系统环境，加载底层数据，并进行网络鉴权。
     * 应用使用期间只需要初始化一次，无需释放数据。
     * 不需要 GL 环境，但必须在SDK其他接口前调用，否则会引起应用崩溃。
     *
     * @param context
     */
    public static void initFURenderer(Context context) {
        if (sIsInited) {
            return;
        }
        // {trace:0, debug:1, info:2, warn:3, error:4, critical:4, off:6}
        int logLevel = 6;
        faceunity.fuSetLogLevel(logLevel);
        Log.i(TAG, "initFURenderer logLevel: " + logLevel);
        // 获取 Nama SDK 版本信息
        Log.e(TAG, "fu sdk version " + faceunity.fuGetVersion());
        // v3 不再使用，第一个参数传空字节数组即可
        int isSetup = faceunity.fuSetup(new byte[0], authpack.A());
        Log.d(TAG, "fuSetup. isSetup: " + (isSetup == 0 ? "no" : "yes"));
        // 加载人脸检测算法数据模型
        loadAiModel(context, "model/ai_face_processor.bundle", faceunity.FUAITYPE_FACEPROCESSOR);
        sIsInited = isLibInit();
        Log.d(TAG, "initFURenderer finish. isLibraryInit: " + (sIsInited ? "yes" : "no"));
    }

    /**
     * 释放鉴权数据占用的内存。如需再次使用，需要调用 fuSetup
     */
    public static void destroyLibData() {
        releaseAiModel(faceunity.FUAITYPE_FACEPROCESSOR);
        if (sIsInited) {
            faceunity.fuDestroyLibData();
            sIsInited = isLibInit();
            Log.d(TAG, "destroyLibData. isLibraryInit: " + (sIsInited ? "yes" : "no"));
        }
    }

    /**
     * SDK 是否初始化
     *
     * @return
     */
    public static boolean isLibInit() {
        return faceunity.fuIsLibraryInit() == 1;
    }

    /**
     * 加载 AI 模型资源，一般在 onSurfaceCreated 方法调用，不需要 EGL Context，耗时操作，可以异步执行
     *
     * @param context
     * @param bundlePath ai_model.bundle
     * @param type       faceunity.FUAITYPE_XXX
     */
    private static void loadAiModel(Context context, String bundlePath, int type) {
        byte[] buffer = readFile(context, bundlePath);
        if (buffer != null) {
            int isLoaded = faceunity.fuLoadAIModelFromPackage(buffer, type);
            Log.d(TAG, "loadAiModel. type: " + type + ", isLoaded: " + (isLoaded == 1 ? "yes" : "no"));
        }
    }

    /**
     * 释放 AI 模型资源，一般在 onSurfaceDestroyed 方法调用，不需要 EGL Context，对应 loadAiModel 方法
     *
     * @param type
     */
    private static void releaseAiModel(int type) {
        if (faceunity.fuIsAIModelLoaded(type) == 1) {
            int isReleased = faceunity.fuReleaseAIModel(type);
            Log.d(TAG, "releaseAiModel. type: " + type + ", isReleased: " + (isReleased == 1 ? "yes" : "no"));
        }
    }

    /**
     * 加载 bundle 道具，不需要 EGL Context，耗时操作，可以异步执行
     *
     * @param bundlePath bundle 文件路径
     * @return 道具句柄，大于 0 表示加载成功
     */
    private static int loadItem(Context context, String bundlePath) {
        int handle = 0;
        if (!TextUtils.isEmpty(bundlePath)) {
            byte[] buffer = readFile(context, bundlePath);
            if (buffer != null) {
                handle = faceunity.fuCreateItemFromPackage(buffer);
            }
        }
        Log.d(TAG, "loadItem. bundlePath: " + bundlePath + ", itemHandle: " + handle);
        return handle;
    }

    /**
     * 从 assets 文件夹或者本地磁盘读文件，一般在 IO 线程调用
     *
     * @param context
     * @param path
     * @return
     */
    private static byte[] readFile(Context context, String path) {
        InputStream is = null;
        try {
            is = context.getAssets().open(path);
        } catch (IOException e1) {
            Log.w(TAG, "readFile: e1", e1);
            // open assets failed, then try sdcard
            try {
                is = new FileInputStream(path);
            } catch (IOException e2) {
                Log.w(TAG, "readFile: e2", e2);
            }
        }
        if (is != null) {
            try {
                byte[] buffer = new byte[is.available()];
                int length = is.read(buffer);
                Log.v(TAG, "readFile. path: " + path + ", length: " + length + " Byte");
                is.close();
                return buffer;
            } catch (IOException e3) {
                Log.e(TAG, "readFile: e3", e3);
            }
        }
        return null;
    }

    /**
     * 获取相机方向
     *
     * @param cameraFacing
     * @return
     */
    public static int getCameraOrientation(int cameraFacing) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int cameraId = -1;
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == cameraFacing) {
                cameraId = i;
                break;
            }
        }
        if (cameraId < 0) {
            // no front camera, regard it as back camera
            return 90;
        } else {
            return info.orientation;
        }
    }

    /**
     * 获取 Nama SDK 完整版本号，例如 6.7.0_tf_phy-f1e36a93-b9e3359-b5f220d
     *
     * @return full version
     */
    public static String getVersion() {
        return faceunity.fuGetVersion();
    }

    private FURenderer(Context context) {
        mContext = context;
    }

    /**
     * 创建及初始化 SDK 相关资源，必须在 GL 线程调用。如果没有 GL 环境，请把 mIsCreateEGLContext 设置为 true。
     */
    public void onSurfaceCreated() {
        Log.e(TAG, "onSurfaceCreated");
        mGlThreadId = Thread.currentThread().getId();
        HandlerThread handlerThread = new HandlerThread("FUItemHandlerThread");
        handlerThread.start();
        Handler fuItemHandler = new FUItemHandler(handlerThread.getLooper());
        mFuItemHandler = fuItemHandler;
        mFrameId = 0;
        synchronized (mLock) {
            mEventQueue.clear();
        }
        /*
         * 创建OpenGL环境，适用于没有 OpenGL 环境时。
         * 如果调用了fuCreateEGLContext，销毁时需要调用fuReleaseEGLContext
         */
        if (mIsCreateEGLContext) {
            faceunity.fuCreateEGLContext();
        }
        // 设置人脸识别的方向，能够提高首次识别速度
        setRotationMode();
        // 设置同时识别的人脸数量
        setMaxFaces(mMaxFaces);
        // 异步加载美颜道具
        if (mIsNeedFaceBeauty) {
            fuItemHandler.sendEmptyMessage(ITEMS_ARRAY_FACE_BEAUTY);
        }
        // 异步加载贴纸道具
        if (mEffect != null) {
            Message.obtain(fuItemHandler, ITEMS_ARRAY_EFFECT, mEffect).sendToTarget();
        }
        if (mIsNeedBodySlim) {
            loadBodySlimModule();
        }
        if (mMakeup != null) {
            loadMakeupModule();
            selectMakeup(new Makeup(mMakeup));
        }
    }

    /**
     * 单 texture 输入接口，必须在具有 GL 环境的线程调用
     *
     * @param tex
     * @param w
     * @param h
     * @return
     */
    public int onDrawFrameSingleInput(int tex, int w, int h) {
        if (tex <= 0 || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        int fuTex = faceunity.fuRenderToTexture(tex, w, h, mFrameId++, mItemsArray, flags);
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 单 texture 输入接口，支持数据回写，必须在具有 GL 环境的线程调用
     *
     * @param tex
     * @param w
     * @param h
     * @param readBackImg    数据回写到的 buffer
     * @param readBackW
     * @param readBackH
     * @param readBackFormat buffer 格式: nv21, i420, rgba
     * @return
     */
    public int onDrawFrameSingleInput(int tex, int w, int h, byte[] readBackImg, int readBackW, int readBackH, int readBackFormat) {
        if (tex <= 0 || w <= 0 || h <= 0 || readBackImg == null || readBackW <= 0 || readBackH <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        switch (readBackFormat) {
            case INPUT_FORMAT_I420:
                flags |= faceunity.FU_ADM_FLAG_I420_TEXTURE;
                break;
            case INPUT_FORMAT_RGBA:
                flags |= faceunity.FU_ADM_FLAG_RGBA_BUFFER;
                break;
            case INPUT_FORMAT_NV21:
            default:
                flags |= faceunity.FU_ADM_FLAG_NV21_TEXTURE;
                break;
        }

        int fuTex = faceunity.fuRenderToTexture(tex, w, h, mFrameId++, mItemsArray, flags, readBackImg, readBackW, readBackH);
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 单 buffer 输入接口，必须在具有 GL 环境的线程调用
     *
     * @param img
     * @param w
     * @param h
     * @param format buffer 格式: nv21, i420, rgba
     * @return
     */
    public int onDrawFrameSingleInput(byte[] img, int w, int h, int format) {
        if (img == null || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        flags ^= mInputTextureType;
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        int fuTex;
        switch (format) {
            case INPUT_FORMAT_I420:
                fuTex = faceunity.fuRenderToI420Image(img, w, h, mFrameId++, mItemsArray, flags);
                break;
            case INPUT_FORMAT_RGBA:
                fuTex = faceunity.fuRenderToRgbaImage(img, w, h, mFrameId++, mItemsArray, flags);
                break;
            case INPUT_FORMAT_NV21:
            default:
                fuTex = faceunity.fuRenderToNV21Image(img, w, h, mFrameId++, mItemsArray, flags);
                break;
        }
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 单 buffer 输入接口，支持数据回写，必须在具有 GL 环境的线程调用
     *
     * @param img
     * @param w
     * @param h
     * @param readBackImg 数据回写到的 buffer
     * @param readBackW
     * @param readBackH
     * @param format      buffer 格式: nv21, i420, rgba
     * @return
     */
    public int onDrawFrameSingleInput(byte[] img, int w, int h, byte[] readBackImg, int readBackW, int readBackH, int format) {
        if (img == null || w <= 0 || h <= 0 || readBackImg == null || readBackW <= 0 || readBackH <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        flags ^= mInputTextureType;
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        int fuTex;
        switch (format) {
            case INPUT_FORMAT_I420:
                fuTex = faceunity.fuRenderToI420Image(img, w, h, mFrameId++, mItemsArray, flags,
                        readBackW, readBackH, readBackImg);
                break;
            case INPUT_FORMAT_RGBA:
                fuTex = faceunity.fuRenderToRgbaImage(img, w, h, mFrameId++, mItemsArray, flags,
                        readBackW, readBackH, readBackImg);
                break;
            case INPUT_FORMAT_NV21:
            default:
                fuTex = faceunity.fuRenderToNV21Image(img, w, h, mFrameId++, mItemsArray, flags,
                        readBackW, readBackH, readBackImg);
                break;
        }
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 双输入接口，输入 buffer 和 texture，必须在具有 GL 环境的线程调用
     * 由于省去数据拷贝，性能相对最优，优先推荐使用。
     *
     * @param img NV21 数据
     * @param tex 纹理 ID
     * @param w
     * @param h
     * @return
     */
    public int onDrawFrameDualInput(byte[] img, int tex, int w, int h) {
        if (img == null || tex <= 0 || w <= 0 || h <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        int fuTex = faceunity.fuDualInputToTexture(img, tex, flags, w, h, mFrameId++, mItemsArray);
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 双输入接口，输入 buffer 和 texture，支持数据回写到 buffer，必须在具有 GL 环境的线程调用
     *
     * @param img         NV21数据
     * @param tex         纹理 ID
     * @param w
     * @param h
     * @param readBackImg 数据回写到的 buffer
     * @param readBackW
     * @param readBackH
     * @return
     */
    public int onDrawFrameDualInput(byte[] img, int tex, int w, int h, byte[] readBackImg, int readBackW, int readBackH) {
        if (img == null || tex <= 0 || w <= 0 || h <= 0 || readBackImg == null || readBackW <= 0 || readBackH <= 0) {
            Log.e(TAG, "onDrawFrame data is invalid");
            return 0;
        }
        prepareDrawFrame();
        int flags = createFlags();
        if (mIsRunBenchmark) {
            mCallStartTime = System.nanoTime();
        }
        int fuTex = faceunity.fuDualInputToTexture(img, tex, flags, w, h, mFrameId++, mItemsArray,
                readBackW, readBackH, readBackImg);
        if (mIsRunBenchmark) {
            mSumRenderTime += System.nanoTime() - mCallStartTime;
        }
        return fuTex;
    }

    /**
     * 销毁 SDK 相关资源，必须在 GL 线程调用。如果没有 GL 环境，请把 mIsCreateEGLContext 设置为 true。
     */
    public void onSurfaceDestroyed() {
        Log.e(TAG, "onSurfaceDestroyed");
        if (mFuItemHandler != null) {
            mFuItemHandler.removeCallbacksAndMessages(null);
            mFuItemHandler.getLooper().quit();
        }
        mFrameId = 0;
        synchronized (mLock) {
            mEventQueue.clear();
        }
        if (mMakeup != null) {
            int itemHandle = mMakeup.getItemHandle();
            if (itemHandle > 0) {
                faceunity.fuUnBindItems(mItemsArray[ITEMS_ARRAY_FACE_MAKEUP], new int[]{itemHandle});
                faceunity.fuDestroyItem(itemHandle);
                mMakeup.setItemHandle(0);
            }
        }
        for (int item : mItemsArray) {
            if (item > 0) {
                faceunity.fuDestroyItem(item);
            }
        }
        resetTrackStatus();
        releaseAiModel(faceunity.FUAITYPE_HUMAN_PROCESSOR);
        Arrays.fill(mItemsArray, 0);
        faceunity.fuDestroyAllItems();
        faceunity.fuOnDeviceLost();
        faceunity.fuDone();
        if (mIsCreateEGLContext) {
            faceunity.fuReleaseEGLContext();
        }
    }

    /**
     * 每帧处理画面时被调用
     */
    private void prepareDrawFrame() {
        // 计算 FPS 和渲染时长
        benchmarkFPS();
        // 获取人脸是否识别
        int trackFace = faceunity.fuIsTracking();
        // 获取人体是否识别
        int trackHumans = faceunity.fuHumanProcessorGetNumResults();
        if (mOnTrackStatusChangedListener != null) {
            if (mItemsArray[ITEMS_ARRAY_BODY_SLIM] > 0) {
                if (mTrackHumanStatus != trackHumans) {
                    mTrackHumanStatus = trackHumans;
                    mOnTrackStatusChangedListener.onTrackStatusChanged(TRACK_TYPE_HUMAN, trackHumans);
                }
            } else {
                if (mTrackFaceStatus != trackFace) {
                    mTrackFaceStatus = trackFace;
                    mOnTrackStatusChangedListener.onTrackStatusChanged(TRACK_TYPE_FACE, trackFace);
                }
            }
        }
        // 获取内部错误信息，并调用回调接口
        int error = faceunity.fuGetSystemError();
        if (error != 0) {
            String errorMessage = faceunity.fuGetSystemErrorString(error);
            if (mOnSystemErrorListener != null) {
                mOnSystemErrorListener.onSystemError(errorMessage);
            }
        }

        // 更新美颜参数
        if (mIsNeedUpdateFaceBeauty && mItemsArray[ITEMS_ARRAY_FACE_BEAUTY] > 0) {
            int itemFaceBeauty = mItemsArray[ITEMS_ARRAY_FACE_BEAUTY];

            //            Log.d(TAG, "prepareDrawFrame: face beauty params: isBeautyOn:" + sIsBeautyOn + ", filterName:"
//                    + sFilterName + ", filterLevel:" + mFilterLevel + ", blurType:" + mBlurType
//                    + ", blurLevel:" + mBlurLevel + ", colorLevel:" + mColorLevel + ", redLevel:" + mRedLevel
//                    + ", eyeBright:" + mEyeBright + ", toothWhiten:" + mToothWhiten + ", faceShapeLevel:"
//                    + mFaceShapeLevel + ", faceShape:" + mFaceShape + ", eyeEnlarging:" + mEyeEnlarging
//                    + ", cheekThinning:" + mCheekThinning + ", cheekNarrow:" + mCheekNarrow + ", cheekSmall:"
//                    + mCheekSmall + ", cheekV:" + mCheekV + ", intensityNose:" + mIntensityNose + ", intensityChin:"
//                    + mIntensityChin + ", intensityForehead:" + mIntensityForehead + ", intensityMouth:"
//                    + mIntensityMouth + ", microPouch:" + sMicroPouch + ", microNasolabialFolds:"
//                    + sMicroNasolabialFolds + ", microSmile:" + sMicroSmile + ", microCanthus:"
//                    + sMicroCanthus + ", microPhiltrum:" + sMicroPhiltrum + ", microLongNose:"
//                    + sMicroLongNose + ", microEyeSpace:" + sMicroEyeSpace + ", eyeRotate:" + sMicroEyeRotate);

            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.IS_BEAUTY_ON, mIsBeautyOn);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.FILTER_NAME, mFilterName);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.FILTER_LEVEL, mFilterLevel);

            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.HEAVY_BLUR, 0.0);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.BLUR_TYPE, mBlurType);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.BLUR_LEVEL, 6.0 * mBlurLevel);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.COLOR_LEVEL, mColorLevel);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.RED_LEVEL, mRedLevel);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.EYE_BRIGHT, mEyeBright);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.TOOTH_WHITEN, mToothWhiten);

            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.FACE_SHAPE_LEVEL, mFaceShapeLevel);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.FACE_SHAPE, mFaceShape);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.EYE_ENLARGING, mEyeEnlarging);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.CHEEK_THINNING, mCheekThinning);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.CHEEK_NARROW, mCheekNarrow);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.CHEEK_SMALL, mCheekSmall);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.CHEEK_V, mCheekV);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.INTENSITY_NOSE, mIntensityNose);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.INTENSITY_CHIN, mIntensityChin);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.INTENSITY_FOREHEAD, mIntensityForehead);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.INTENSITY_MOUTH, mIntensityMouth);

            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.REMOVE_POUCH_STRENGTH, mMicroPouch);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.REMOVE_NASOLABIAL_FOLDS_STRENGTH, mMicroNasolabialFolds);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.INTENSITY_SMILE, mMicroSmile);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.INTENSITY_CANTHUS, mMicroCanthus);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.INTENSITY_PHILTRUM, mMicroPhiltrum);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.INTENSITY_LONG_NOSE, mMicroLongNose);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.INTENSITY_EYE_SPACE, mMicroEyeSpace);
            faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.INTENSITY_EYE_ROTATE, mMicroEyeRotate);
            mIsNeedUpdateFaceBeauty = false;
        }

        synchronized (mLock) {
            while (!mEventQueue.isEmpty()) {
                mEventQueue.remove(0).run();
            }
        }
    }

    /**
     * 类似 GLSurfaceView 的 queueEvent 机制
     *
     * @param r
     */
    public void queueEvent(Runnable r) {
        if (r != null) {
            if (mGlThreadId == Thread.currentThread().getId()) {
                r.run();
            } else {
                synchronized (mLock) {
                    mEventQueue.add(r);
                }
            }
        }
    }

    /**
     * 设置需要识别的人脸个数
     *
     * @param maxFaces
     */
    public void setMaxFaces(final int maxFaces) {
        if (maxFaces > 0) {
            mMaxFaces = maxFaces;
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "setMaxFaces() called with: maxFaces = [" + maxFaces + "]");
                    faceunity.fuSetMaxFaces(maxFaces);
                }
            });
        }
    }

    /**
     * 设置需要识别的人体个数
     *
     * @param maxHumans
     */
    public void setMaxHumans(final int maxHumans) {
        if (maxHumans > 0) {
            mMaxHumans = maxHumans;
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "setMaxHumans() called with: maxHumans = [" + maxHumans + "]");
                    faceunity.fuHumanProcessorSetMaxHumans(maxHumans);
                }
            });
        }
    }

    @Override
    public void loadBodySlimModule() {
        Log.d(TAG, "loadBodySlimModule: ");
        if (mItemsArray[ITEMS_ARRAY_BODY_SLIM] <= 0) {
            mIsNeedBodySlim = true;
            mFuItemHandler.sendEmptyMessage(ITEMS_ARRAY_BODY_SLIM);
        }
    }

    @Override
    public void destroyBodySlimModule() {
        Log.d(TAG, "destroyBodySlimModule: ");
        mIsNeedBodySlim = false;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                int itemBodySlim = mItemsArray[ITEMS_ARRAY_BODY_SLIM];
                if (itemBodySlim > 0) {
                    faceunity.fuDestroyItem(itemBodySlim);
                    mItemsArray[ITEMS_ARRAY_BODY_SLIM] = 0;
                }
                resetTrackStatus();
            }
        });
        mFuItemHandler.post(new Runnable() {
            @Override
            public void run() {
                releaseAiModel(faceunity.FUAITYPE_HUMAN_PROCESSOR);
            }
        });
    }

    @Override
    public void loadMakeupModule() {
        Log.d(TAG, "loadMakeupModule: ");
        if (mItemsArray[ITEMS_ARRAY_FACE_MAKEUP] <= 0) {
            mFuItemHandler.post(new Runnable() {
                @Override
                public void run() {
                    int itemMakeup = loadItem(mContext, "graphics/face_makeup.bundle");
                    if (itemMakeup <= 0) {
                        Log.w(TAG, "create face makeup item failed: " + itemMakeup);
                        return;
                    }
                    faceunity.fuItemSetParam(itemMakeup, MakeupParam.MAKEUP_INTENSITY, mMakeupIntensity);
                    mItemsArray[ITEMS_ARRAY_FACE_MAKEUP] = itemMakeup;
                }
            });
        }
    }

    @Override
    public void destroyMakeupModule() {
        Log.d(TAG, "destroyMakeupModule: ");
        queueEvent(new Runnable() {
            @Override
            public void run() {
                int itemMakeup = mItemsArray[ITEMS_ARRAY_FACE_MAKEUP];
                if (itemMakeup > 0) {
                    if (mMakeup != null) {
                        int itemHandle = mMakeup.getItemHandle();
                        if (itemHandle > 0) {
                            faceunity.fuUnBindItems(itemMakeup, new int[]{itemHandle});
                            faceunity.fuDestroyItem(itemHandle);
                            mMakeup.setItemHandle(0);
                        }
                        mMakeup = null;
                    }
                    faceunity.fuDestroyItem(itemMakeup);
                    mItemsArray[ITEMS_ARRAY_FACE_MAKEUP] = 0;
                    resetTrackStatus();
                }
            }
        });
    }

    @Override
    public void selectMakeup(Makeup makeup) {
        Message.obtain(mFuItemHandler, ITEMS_ARRAY_FACE_MAKEUP, makeup).sendToTarget();
    }

    @Override
    public void setMakeupIntensity(final float intensity) {
        mMakeupIntensity = intensity;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                int itemMakeup = mItemsArray[ITEMS_ARRAY_FACE_MAKEUP];
                if (itemMakeup > 0) {
                    faceunity.fuItemSetParam(itemMakeup, MakeupParam.MAKEUP_INTENSITY, intensity);
                }
            }
        });
    }


    /**
     * 美妆功能点位镜像，0为关闭，1为开启
     *
     * @param isFlipPoints 是否镜像点位
     */
    public void setIsMakeupFlipPoints(final boolean isFlipPoints) {
        mIsMakeupFlipPoints = isFlipPoints;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mMakeup != null) {
                    int item = mItemsArray[ITEMS_ARRAY_FACE_MAKEUP];
                    if (item > 0 && mMakeup.isNeedFlipPoints()) {
                        Log.i(TAG, "setIsMakeupFlipPoints() called with: isFlipPoints = [" + isFlipPoints + "]");
                        faceunity.fuItemSetParam(item, MakeupParam.IS_FLIP_POINTS, isFlipPoints ? 1.0 : 0.0);
                    }
                }
            }
        });
    }

    /**
     * 设备方向发生变化时调用
     *
     * @param deviceOrientation home 下 90，home 右 0，home 上 270，home 左 180
     */
    public void onDeviceOrientationChanged(final int deviceOrientation) {
        if (mDeviceOrientation == deviceOrientation) {
            return;
        }
        Log.d(TAG, "onDeviceOrientationChanged() called with: deviceOrientation = [" + deviceOrientation + "]");
        mDeviceOrientation = deviceOrientation;
        callWhenDeviceChanged();
    }

    /**
     * 相机切换时需要调用
     *
     * @param cameraType            前后置相机 ID
     * @param inputImageOrientation 相机方向
     */
    public void onCameraChanged(final int cameraType, final int inputImageOrientation) {
        if (mCameraType == cameraType && mInputImageOrientation == inputImageOrientation) {
            return;
        }
        Log.d(TAG, "onCameraChanged() called with: cameraType = [" + cameraType
                + "], inputImageOrientation = [" + inputImageOrientation + "]");
        mCameraType = cameraType;
        mInputImageOrientation = inputImageOrientation;
        callWhenDeviceChanged();
    }

    private void callWhenDeviceChanged() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                setRotationMode();
                setEffectItemParams(mItemsArray[ITEMS_ARRAY_EFFECT]);
                setBeautyBodyOrientation();
                faceunity.fuOnCameraChange();
            }
        });
    }

    private void setRotationMode() {
        int rotationMode = createRotationMode();
        faceunity.fuSetDefaultRotationMode(rotationMode);
        mRotationMode = rotationMode;
    }

    private int createRotationMode() {
        if (mInputTextureType == FURenderer.INPUT_2D_TEXTURE) {
            if (mDeviceOrientation == 90) {
                return faceunity.FU_ROTATION_MODE_0;
            } else if (mDeviceOrientation == 0) {
                return faceunity.FU_ROTATION_MODE_270;
            } else if (mDeviceOrientation == 180) {
                return faceunity.FU_ROTATION_MODE_90;
            } else if (mDeviceOrientation == 270) {
                return faceunity.FU_ROTATION_MODE_180;
            }
        }
        int rotMode = faceunity.FU_ROTATION_MODE_0;
        if (mInputImageOrientation == 270) {
            if (mCameraType == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                rotMode = mDeviceOrientation / 90;
            } else {
                if (mDeviceOrientation == 90) {
                    rotMode = faceunity.FU_ROTATION_MODE_270;
                } else if (mDeviceOrientation == 270) {
                    rotMode = faceunity.FU_ROTATION_MODE_90;
                } else {
                    rotMode = mDeviceOrientation / 90;
                }
            }
        } else if (mInputImageOrientation == 90) {
            if (mCameraType == Camera.CameraInfo.CAMERA_FACING_BACK) {
                if (mDeviceOrientation == 90) {
                    rotMode = faceunity.FU_ROTATION_MODE_270;
                } else if (mDeviceOrientation == 270) {
                    rotMode = faceunity.FU_ROTATION_MODE_90;
                } else {
                    rotMode = mDeviceOrientation / 90;
                }
            } else {
                if (mDeviceOrientation == 0) {
                    rotMode = faceunity.FU_ROTATION_MODE_180;
                } else if (mDeviceOrientation == 90) {
                    rotMode = faceunity.FU_ROTATION_MODE_270;
                } else if (mDeviceOrientation == 180) {
                    rotMode = faceunity.FU_ROTATION_MODE_0;
                } else {
                    rotMode = faceunity.FU_ROTATION_MODE_90;
                }
            }
        }
        return rotMode;
    }

    private int createFlags() {
        int flags = mInputTextureType | mInputImageFormat;
        if (mInputTextureType == INPUT_2D_TEXTURE && mCameraType != Camera.CameraInfo.CAMERA_FACING_FRONT) {
            flags |= faceunity.FU_ADM_FLAG_FLIP_X;
        }
        return flags;
    }

    /**
     * 对道具贴纸设置相应的参数
     *
     * @param itemHandle
     */
    private void setEffectItemParams(final int itemHandle) {
        if (itemHandle <= 0) {
            return;
        }
        double isAndroid;
        if (mInputTextureType == INPUT_EXTERNAL_OES_TEXTURE) {
            isAndroid = 1.0;
        } else {
            isAndroid = 0.0;
        }
        // 历史遗留参数，和具体道具有关
        faceunity.fuItemSetParam(itemHandle, "isAndroid", isAndroid);
        // rotationAngle 参数是用于旋转普通道具
        faceunity.fuItemSetParam(itemHandle, "rotationAngle", mRotationMode * 90);
        Log.d(TAG, "setEffectItemParams. rotationMode: " + mRotationMode + ", isAndroid: " + isAndroid);
    }

    //--------------------------------------美颜参数与道具回调----------------------------------------

    @Override
    public void onEffectSelected(Effect effect) {
        if (effect == null) {
            return;
        }
        mEffect = effect;
        mFuItemHandler.sendMessage(Message.obtain(mFuItemHandler, ITEMS_ARRAY_EFFECT, effect));
    }

    @Override
    public void onFilterNameSelected(String name) {
        mFilterName = name;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onFilterLevelSelected(float level) {
        mFilterLevel = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onBlurLevelSelected(float level) {
        mBlurLevel = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onColorLevelSelected(float level) {
        mColorLevel = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onRedLevelSelected(float level) {
        mRedLevel = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void setBodySlimIntensity(final float intensity) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mBodySlimStrength = intensity;
                int itemBody = mItemsArray[ITEMS_ARRAY_BODY_SLIM];
                if (itemBody > 0) {
                    faceunity.fuItemSetParam(itemBody, BodySlimParam.BODY_SLIM_STRENGTH, intensity);
                }
            }
        });
    }

    @Override
    public void setLegSlimIntensity(final float intensity) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mLegSlimStrength = intensity;
                int itemBody = mItemsArray[ITEMS_ARRAY_BODY_SLIM];
                if (itemBody > 0) {
                    faceunity.fuItemSetParam(itemBody, BodySlimParam.LEG_SLIM_STRENGTH, intensity);
                }
            }
        });
    }

    @Override
    public void setWaistSlimIntensity(final float intensity) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mWaistSlimStrength = intensity;
                int itemBody = mItemsArray[ITEMS_ARRAY_BODY_SLIM];
                if (itemBody > 0) {
                    faceunity.fuItemSetParam(itemBody, BodySlimParam.WAIST_SLIM_STRENGTH, intensity);
                }
            }
        });
    }

    @Override
    public void setShoulderSlimIntensity(final float intensity) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mShoulderSlimStrength = intensity;
                int itemBody = mItemsArray[ITEMS_ARRAY_BODY_SLIM];
                if (itemBody > 0) {
                    faceunity.fuItemSetParam(itemBody, BodySlimParam.SHOULDER_SLIM_STRENGTH, intensity);
                }
            }
        });
    }

    @Override
    public void setHipSlimIntensity(final float intensity) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mHipSlimStrength = intensity;
                int itemBody = mItemsArray[ITEMS_ARRAY_BODY_SLIM];
                if (itemBody > 0) {
                    faceunity.fuItemSetParam(itemBody, BodySlimParam.HIP_SLIM_STRENGTH, intensity);
                }
            }
        });
    }

    @Override
    public void setHeadSlimIntensity(final float intensity) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mHeadSlimStrength = intensity;
                int itemBody = mItemsArray[ITEMS_ARRAY_BODY_SLIM];
                if (itemBody > 0) {
                    faceunity.fuItemSetParam(itemBody, BodySlimParam.HEAD_SLIM, intensity);
                }
            }
        });
    }

    @Override
    public void setLegThinSlimIntensity(final float intensity) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mLegThinSlimStrength = intensity;
                int itemBody = mItemsArray[ITEMS_ARRAY_BODY_SLIM];
                if (itemBody > 0) {
                    faceunity.fuItemSetParam(itemBody, BodySlimParam.LEG_SLIM, intensity);
                }
            }
        });
    }

    @Override
    public void setRemovePouchStrength(float strength) {
        mMicroPouch = strength;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void setRemoveNasolabialFoldsStrength(float strength) {
        mMicroNasolabialFolds = strength;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void setSmileIntensity(float intensity) {
        mMicroSmile = intensity;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void setCanthusIntensity(float intensity) {
        mMicroCanthus = intensity;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void setPhiltrumIntensity(float intensity) {
        mMicroPhiltrum = intensity;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void setLongNoseIntensity(float intensity) {
        mMicroLongNose = intensity;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void setEyeSpaceIntensity(float intensity) {
        mMicroEyeSpace = intensity;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void setEyeRotateIntensity(float intensity) {
        mMicroEyeRotate = intensity;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onEyeBrightSelected(float level) {
        mEyeBright = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onToothWhitenSelected(float level) {
        mToothWhiten = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onEyeEnlargeSelected(float level) {
        mEyeEnlarging = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onCheekThinningSelected(float level) {
        mCheekThinning = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onIntensityChinSelected(float level) {
        mIntensityChin = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onIntensityForeheadSelected(float level) {
        mIntensityForehead = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onIntensityNoseSelected(float level) {
        mIntensityNose = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onIntensityMouthSelected(float level) {
        mIntensityMouth = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onCheekNarrowSelected(float level) {
        mCheekNarrow = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onCheekSmallSelected(float level) {
        mCheekSmall = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void onCheekVSelected(float level) {
        mCheekV = level;
        mIsNeedUpdateFaceBeauty = true;
    }

    @Override
    public void setBeautificationOn(boolean isOn) {
        int isBeautyOn = isOn ? 1 : 0;
        if (mIsBeautyOn == isBeautyOn) {
            return;
        }
        mIsBeautyOn = isBeautyOn;
        queueEvent(new Runnable() {
            @Override
            public void run() {
                int itemFaceBeauty = mItemsArray[ITEMS_ARRAY_FACE_BEAUTY];
                if (itemFaceBeauty > 0) {
                    faceunity.fuItemSetParam(itemFaceBeauty, BeautificationParam.IS_BEAUTY_ON, mIsBeautyOn);
                }
            }
        });
    }

    private void setBeautyBodyOrientation() {
        int itemBody = mItemsArray[ITEMS_ARRAY_BODY_SLIM];
        if (itemBody > 0) {
            int bodyOrientation = createRotationMode();
            faceunity.fuItemSetParam(itemBody, BodySlimParam.ORIENTATION, bodyOrientation);
        }
    }

    //-----------------------------人脸识别回调相关定义-----------------------------------

    private int mTrackFaceStatus = -1;
    private int mTrackHumanStatus = -1;

    public interface OnTrackStatusChangedListener {
        /**
         * 识别到的人脸或人体数量发生变化
         *
         * @param type   类型
         * @param status 状态
         */
        void onTrackStatusChanged(int type, int status);
    }

    private OnTrackStatusChangedListener mOnTrackStatusChangedListener;

    public void resetTrackStatus() {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mTrackFaceStatus = -1;
                mTrackHumanStatus = -1;
            }
        });
    }

    //-------------------------错误信息回调相关定义---------------------------------

    public interface OnSystemErrorListener {
        /**
         * SDK 发生错误时调用
         *
         * @param error 错误消息
         */
        void onSystemError(String error);
    }

    private OnSystemErrorListener mOnSystemErrorListener;

    //------------------------------FPS 渲染时长回调相关定义------------------------------------

    private static final int NANO_IN_ONE_MILLI_SECOND = 1_000_000;
    private static final int NANO_IN_ONE_SECOND = 1_000_000_000;
    private static final int FRAME_COUNT = 10;
    private boolean mIsRunBenchmark = false;
    private int mCurrentFrameCount;
    private long mLastFrameTimestamp;
    private long mSumRenderTime;
    private long mCallStartTime;
    private OnDebugListener mOnDebugListener;

    public interface OnDebugListener {
        /**
         * 统计每 10 帧的平均值，FPS 和渲染时间
         *
         * @param fps        FPS
         * @param renderTime 渲染时间
         */
        void onFpsChanged(double fps, double renderTime);
    }

    private void benchmarkFPS() {
        if (!mIsRunBenchmark) {
            return;
        }
        if (++mCurrentFrameCount == FRAME_COUNT) {
            long tmp = System.nanoTime();
            double fps = (double) NANO_IN_ONE_SECOND / ((double) (tmp - mLastFrameTimestamp) / FRAME_COUNT);
            double renderTime = (double) mSumRenderTime / FRAME_COUNT / NANO_IN_ONE_MILLI_SECOND;
            mLastFrameTimestamp = tmp;
            mSumRenderTime = 0;
            mCurrentFrameCount = 0;

            if (mOnDebugListener != null) {
                mOnDebugListener.onFpsChanged(fps, renderTime);
            }
        }
    }

    //--------------------------------------IO handler 线程异步加载道具-------------------------------------

    private class FUItemHandler extends Handler {

        FUItemHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                // 加载贴纸道具
                case ITEMS_ARRAY_EFFECT: {
                    if (msg.obj == null) {
                        return;
                    }
                    final Effect effect = (Effect) msg.obj;
                    final int itemEffect = loadItem(mContext, effect.getFilePath());
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            if (mItemsArray[ITEMS_ARRAY_EFFECT] > 0) {
                                faceunity.fuDestroyItem(mItemsArray[ITEMS_ARRAY_EFFECT]);
                                mItemsArray[ITEMS_ARRAY_EFFECT] = 0;
                            }
                            if (itemEffect > 0) {
                                setEffectItemParams(itemEffect);
                                mItemsArray[ITEMS_ARRAY_EFFECT] = itemEffect;
                            }
                        }
                    });
                }
                break;
                // 加载美颜道具
                case ITEMS_ARRAY_FACE_BEAUTY: {
                    final int itemFaceBeauty = loadItem(mContext, "graphics/face_beautification.bundle");
                    if (itemFaceBeauty <= 0) {
                        Log.w(TAG, "load face beauty item failed");
                        break;
                    }
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            mItemsArray[ITEMS_ARRAY_FACE_BEAUTY] = itemFaceBeauty;
                            mIsNeedUpdateFaceBeauty = true;
                        }
                    });
                }
                break;
                // 加载美妆道具
                case ITEMS_ARRAY_FACE_MAKEUP: {
                    if (msg.obj == null) {
                        return;
                    }
                    final Makeup makeup = (Makeup) msg.obj;
                    final int itemHandle = loadItem(mContext, makeup.getFilePath());
                    if (itemHandle <= 0) {
                        Log.w(TAG, "create makeup item failed: " + makeup);
                    }
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            int itemMakeup = mItemsArray[ITEMS_ARRAY_FACE_MAKEUP];
                            if (mMakeup != null) {
                                int oldItemHandle = mMakeup.getItemHandle();
                                if (oldItemHandle > 0) {
                                    faceunity.fuUnBindItems(itemMakeup, new int[]{oldItemHandle});
                                    faceunity.fuDestroyItem(oldItemHandle);
                                    mMakeup.setItemHandle(0);
                                    Log.d(TAG, "makeup unbind " + mMakeup);
                                }
                            }
                            mMakeup = makeup;
                            if (itemHandle > 0) {
//                                faceunity.fuItemSetParam(itemMakeup, MakeupParam.IS_FLIP_POINTS, makeup.isNeedFlipPoints() ? 1.0 : 0.0);
                                faceunity.fuBindItems(itemMakeup, new int[]{itemHandle});
                                makeup.setItemHandle(itemHandle);
                                Log.d(TAG, "makeup bind " + makeup);
                            }
                            resetTrackStatus();
                        }
                    });
                }
                break;
                // 加载美体 bundle
                case ITEMS_ARRAY_BODY_SLIM: {
                    loadAiModel(mContext, "model/ai_human_processor.bundle", faceunity.FUAITYPE_HUMAN_PROCESSOR);
                    final int itemBodySlim = loadItem(mContext, "graphics/body_slim.bundle");
                    if (itemBodySlim <= 0) {
                        Log.w(TAG, "create body slim item failed: " + itemBodySlim);
                        return;
                    }
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            if (mItemsArray[ITEMS_ARRAY_BODY_SLIM] > 0) {
                                faceunity.fuDestroyItem(mItemsArray[ITEMS_ARRAY_BODY_SLIM]);
                                mItemsArray[ITEMS_ARRAY_BODY_SLIM] = 0;
                            }
                            faceunity.fuItemSetParam(itemBodySlim, BodySlimParam.BODY_SLIM_STRENGTH, mBodySlimStrength);
                            faceunity.fuItemSetParam(itemBodySlim, BodySlimParam.LEG_SLIM_STRENGTH, mLegSlimStrength);
                            faceunity.fuItemSetParam(itemBodySlim, BodySlimParam.WAIST_SLIM_STRENGTH, mWaistSlimStrength);
                            faceunity.fuItemSetParam(itemBodySlim, BodySlimParam.SHOULDER_SLIM_STRENGTH, mShoulderSlimStrength);
                            faceunity.fuItemSetParam(itemBodySlim, BodySlimParam.HIP_SLIM_STRENGTH, mHipSlimStrength);
                            faceunity.fuItemSetParam(itemBodySlim, BodySlimParam.HEAD_SLIM, mHeadSlimStrength);
                            faceunity.fuItemSetParam(itemBodySlim, BodySlimParam.LEG_SLIM, mLegThinSlimStrength);
                            faceunity.fuItemSetParam(itemBodySlim, BodySlimParam.DEBUG, 0.0);
                            mItemsArray[ITEMS_ARRAY_BODY_SLIM] = itemBodySlim;
                            setBeautyBodyOrientation();
                            setMaxHumans(mMaxHumans);
                            resetTrackStatus();
                        }
                    });
                }
                break;
                default:
            }
        }
    }

    //--------------------------------------Builder----------------------------------------

    /**
     * FURenderer Builder
     */
    public static class Builder {
        private Context context;
        private boolean isCreateEGLContext = false;
        private Effect effect;
        private int maxFaces = 4;
        private int maxHumans = 1;
        private int deviceOrientation = 90;
        private int inputTextureType = INPUT_EXTERNAL_OES_TEXTURE;
        private int inputImageFormat = 0;
        private int inputImageOrientation = 270;
        private int cameraType = Camera.CameraInfo.CAMERA_FACING_FRONT;
        private boolean isNeedFaceBeauty = true;
        private boolean isRunBenchmark = false;
        private OnDebugListener onDebugListener;
        private OnTrackStatusChangedListener onTrackStatusChangedListener;
        private OnSystemErrorListener onSystemErrorListener;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        /**
         * 是否手动创建 EGLContext
         *
         * @param isCreateEGLContext
         * @return
         */

        public Builder setCreateEGLContext(boolean isCreateEGLContext) {
            this.isCreateEGLContext = isCreateEGLContext;
            return this;
        }

        /**
         * 默认贴纸道具
         *
         * @param effect
         * @return
         */
        public Builder setEffect(Effect effect) {
            this.effect = effect;
            return this;
        }

        /**
         * 同时识别的最大人脸数
         *
         * @param maxFaces
         * @return
         */
        public Builder setMaxFaces(int maxFaces) {
            this.maxFaces = maxFaces;
            return this;
        }

        /**
         * 同时识别的最大人体数
         *
         * @param maxHumans
         * @return
         */
        public Builder setMaxHumans(int maxHumans) {
            this.maxHumans = maxHumans;
            return this;
        }

        /**
         * 设备方向
         *
         * @param deviceOrientation
         * @return
         */
        public Builder setDeviceOrientation(int deviceOrientation) {
            this.deviceOrientation = deviceOrientation;
            return this;
        }

        /**
         * 输入图像的纹理类型
         *
         * @param inputTextureType OES 或者 2D
         * @return
         */
        public Builder setInputTextureType(int inputTextureType) {
            this.inputTextureType = inputTextureType;
            return this;
        }

        /**
         * 输入图像的 buffer 类型，一般不用修改此项
         *
         * @param inputImageFormat
         * @return
         */
        public Builder setInputImageFormat(int inputImageFormat) {
            this.inputImageFormat = inputImageFormat;
            return this;
        }

        /**
         * 输入图像的方向
         *
         * @param inputImageOrientation
         * @return
         */
        public Builder setInputImageOrientation(int inputImageOrientation) {
            this.inputImageOrientation = inputImageOrientation;
            return this;
        }

        /**
         * 相机前后方向
         *
         * @param cameraType
         * @return
         */
        public Builder setCameraType(int cameraType) {
            this.cameraType = cameraType;
            return this;
        }

        /**
         * 是否使用美颜
         *
         * @param isNeedFaceBeauty
         * @return
         */
        public Builder setNeedFaceBeauty(boolean isNeedFaceBeauty) {
            this.isNeedFaceBeauty = isNeedFaceBeauty;
            return this;
        }

        /**
         * 是否需要 benchmark 统计数据
         *
         * @param isRunBenchmark
         * @return
         */
        public Builder setRunBenchmark(boolean isRunBenchmark) {
            this.isRunBenchmark = isRunBenchmark;
            return this;
        }

        /**
         * FPS 和渲染时长数据回调
         *
         * @param onDebugListener
         * @return
         */
        public Builder setOnDebugListener(OnDebugListener onDebugListener) {
            this.onDebugListener = onDebugListener;
            return this;
        }

        /**
         * 人脸识别状态改变回调
         *
         * @param onTrackStatusChangedListener
         * @return
         */
        public Builder setOnTrackStatusChangedListener(OnTrackStatusChangedListener onTrackStatusChangedListener) {
            this.onTrackStatusChangedListener = onTrackStatusChangedListener;
            return this;
        }

        /**
         * SDK 错误信息回调
         *
         * @param onSystemErrorListener
         * @return
         */
        public Builder setOnSystemErrorListener(OnSystemErrorListener onSystemErrorListener) {
            this.onSystemErrorListener = onSystemErrorListener;
            return this;
        }

        public FURenderer build() {
            FURenderer fuRenderer = new FURenderer(context);
            fuRenderer.mIsCreateEGLContext = isCreateEGLContext;
            fuRenderer.mMaxFaces = maxFaces;
            fuRenderer.mMaxHumans = maxHumans;
            fuRenderer.mDeviceOrientation = deviceOrientation;
            fuRenderer.mInputTextureType = inputTextureType;
            fuRenderer.mInputImageFormat = inputImageFormat;
            fuRenderer.mInputImageOrientation = inputImageOrientation;
            fuRenderer.mCameraType = cameraType;
            fuRenderer.mEffect = effect;
            fuRenderer.mIsNeedFaceBeauty = isNeedFaceBeauty;
            fuRenderer.mIsRunBenchmark = isRunBenchmark;
            fuRenderer.mOnDebugListener = onDebugListener;
            fuRenderer.mOnTrackStatusChangedListener = onTrackStatusChangedListener;
            fuRenderer.mOnSystemErrorListener = onSystemErrorListener;

            Log.d(TAG, "FURenderer fields. isCreateEGLContext: " + isCreateEGLContext + ", maxFaces: "
                    + maxFaces + ", maxHumans: " + maxHumans + ", inputTextureType: " + inputTextureType + ", inputImageFormat: "
                    + inputImageFormat + ", inputImageOrientation: " + inputImageOrientation
                    + ", deviceOrientation: " + deviceOrientation + ", cameraType: " + cameraType
                    + ", isRunBenchmark: " + isRunBenchmark + ", isNeedFaceBeauty: " + isNeedFaceBeauty
                    + ", effect: " + effect);
            return fuRenderer;
        }
    }

}
