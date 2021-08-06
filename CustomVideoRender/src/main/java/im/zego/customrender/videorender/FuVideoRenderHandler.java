package im.zego.customrender.videorender;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;
import android.view.Surface;
import android.view.TextureView;

import com.faceunity.nama.FURenderer;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import im.zego.zegoexpress.callback.IZegoCustomVideoRenderHandler;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoVideoFlipMode;
import im.zego.zegoexpress.entity.ZegoVideoFrameParam;

import static im.zego.customrender.ui.ZGVideoRenderUI.mainPublishChannel;


/**
 * VideoRenderHandler
 * 渲染类 Renderer 的封装层，接口更利于上层调用
 */

/**
 *  * VideoRenderHandler
 *  * Renderer encapsulation layer, the interface is more conducive to the upper layer call
 *  
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class FuVideoRenderHandler extends IZegoCustomVideoRenderHandler implements Choreographer.FrameCallback {
    private static final String TAG = "VideoRenderHandler";

    public static final Object lock = new Object();

    // opengl 颜色配置
    public static final int[] CONFIG_RGBA = {
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
    };

    private EGLContext eglContext;
    private EGLConfig eglConfig;
    private EGLDisplay eglDisplay;
    private boolean mIsRunning = false;


    private int mViewWidth = 540;
    private int mViewHeight = 960;

    private HandlerThread mThread = null;
    private Handler mHandler = null;

    private ConcurrentHashMap<String, MyVideoFrame> frameMap = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<String, MyVideoFrame> getFrameMap() {
        return frameMap;
    }

    // 流名、渲染对象的键值map
    private ConcurrentHashMap<String, Renderer> rendererMap = null;

    private FURenderer mFURenderer;
    private Handler mCapLoopHandler;

    public void setFURenderer(FURenderer FURenderer) {
        mFURenderer = FURenderer;
    }

    // 初始化，包含线程启动，视频帧回调监听，opengl相关参数的设置等
    // Initialization, including thread startup, video frame callback monitoring, opengl related parameter settings, etc.
    public final int init() {
        Log.d(TAG, "init: ");
        mThread = new HandlerThread("VideoRenderHandler" + hashCode());
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run: create egl context");
                eglDisplay = getEglDisplay();
                eglConfig = getEglConfig(eglDisplay, CONFIG_RGBA);
                eglContext = createEglContext(null, eglDisplay, eglConfig);

                Choreographer.getInstance().postFrameCallback(FuVideoRenderHandler.this);
                mIsRunning = true;

                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        rendererMap = new ConcurrentHashMap<>();

        return 0;
    }

    private void checkNotNull() {
        synchronized (lock) {
            if (rendererMap == null) {
                rendererMap = new ConcurrentHashMap<>();
            }
        }
    }


    // 根据流名添加渲染视图
    // Add rendering view based on stream name
    public void addView(final String streamID, final TextureView textureView) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkNotNull();
                if (rendererMap.get(streamID) == null) {
                    Log.i(TAG, String.format("new Renderer streamId : %s", streamID));
                    // 创建渲染类对象
                    Renderer renderer = new Renderer(eglContext, eglDisplay, eglConfig);
                    // 设置渲染view
                    renderer.setRendererView(textureView);
                    renderer.setStreamID(streamID);
                    rendererMap.put(streamID, renderer);
                } else {
                    rendererMap.get(streamID).setRendererView(textureView);
                    Log.i(TAG, String.format("setRendererView Renderer streamId : %s", streamID));
                }
            }
        });
    }

    // 删除指定流绑定的渲染视图
    // Delete the rendering view bound by the specified stream
    public void removeView(final String streamID) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkNotNull();
                if (rendererMap.get(streamID) != null) {
                    Log.i(TAG, String.format("removeView Renderer streamId : %s", streamID));
                    // 释放 EGL Surface
                    rendererMap.get(streamID).uninitEGLSurface();
                    // 释放 Render
                    rendererMap.get(streamID).uninit();
                    rendererMap.remove(streamID);
                }
                if (getFrameMap().get(streamID) != null) {
                    Log.i(TAG, String.format("removeView frameMap streamId : %s", streamID));
                    getFrameMap().remove(streamID);
                }
            }
        });
    }

    // 删除全部渲染视图
    // Delete all rendered views
    public void removeAllView() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkNotNull();
                Log.i(TAG, "removeAllView");
                for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
                    Renderer renderer = entry.getValue();
                    // 释放 EGL Surface
                    renderer.uninitEGLSurface();
                    // 释放 Render
                    renderer.uninit();
                    rendererMap.remove(entry.getKey());
                }


                for (Map.Entry<String, MyVideoFrame> entry : getFrameMap().entrySet()) {
                    getFrameMap().remove(entry.getKey());
                }
            }
        });
    }

    // 释放渲染类 Render
    private void release() {
        Log.d(TAG, "release: ");
        for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
            Renderer renderer = entry.getValue();
            renderer.uninitEGLSurface();
            renderer.uninit();
        }

        // 销毁 EGLContext 对象
        EGL14.eglDestroyContext(eglDisplay, eglContext);
        // 释放线程
        EGL14.eglReleaseThread();
        // 终止 Display 对象
        EGL14.eglTerminate(eglDisplay);

        eglContext = EGL14.EGL_NO_CONTEXT;
        eglDisplay = EGL14.EGL_NO_DISPLAY;
        eglConfig = null;

    }

    // 处理释放相关操作，线程停止、移除视频帧回调监听等
    // Handle release-related operations, thread stop, remove video frame callback monitoring, etc.
    public final int uninit() {
        Log.i(TAG, "uninit: ");
        mCapLoopHandler.post(new Runnable() {
            @Override
            public void run() {
                mFURenderer.onSurfaceDestroyed();
            }
        });
        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsRunning = false;
                release();
                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;

        if (Build.VERSION.SDK_INT >= 18) {
            mThread.quitSafely();
        } else {
            mThread.quit();
        }
        mThread = null;

        for (Map.Entry<String, Renderer> entry : rendererMap.entrySet()) {
            Renderer renderer = entry.getValue();
            renderer.uninit();
        }

        rendererMap = null;
        frameMap = null;

        // 移除视频帧回调监听
        // Remove video frame callback monitoring
        Choreographer.getInstance().removeFrameCallback(FuVideoRenderHandler.this);

        // 释放MediaCodec
        if (mAVCDecoder != null) {
            mAVCDecoder.stopAndReleaseDecoder();
            mAVCDecoder = null;
        }

        return 0;
    }

    // 获取 EGLDisplay
    private static EGLDisplay getEglDisplay() {
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException(
                    "Unable to get EGL14 display: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        int[] version = new int[2];
        // 初始化 EGL
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw new RuntimeException(
                    "Unable to initialize EGL14: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        return eglDisplay;
    }

    // 获取 EGLConfig
    private static EGLConfig getEglConfig(EGLDisplay eglDisplay, int[] configAttributes) {
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        //选择最佳的 Surface 配置
        if (!EGL14.eglChooseConfig(
                eglDisplay, configAttributes, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new RuntimeException(
                    "eglChooseConfig failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        if (numConfigs[0] <= 0) {
            throw new RuntimeException("Unable to find any matching EGL config");
        }
        final EGLConfig eglConfig = configs[0];
        if (eglConfig == null) {
            throw new RuntimeException("eglChooseConfig returned null");
        }
        return eglConfig;
    }

    // 创建 EGLContext
    private static EGLContext createEglContext(
            EGLContext sharedContext, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        if (sharedContext != null && sharedContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("Invalid sharedContext");
        }
        int[] contextAttributes = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
        EGLContext rootContext =
                sharedContext == null ? EGL14.EGL_NO_CONTEXT : sharedContext;
        final EGLContext eglContext;
        synchronized (FuVideoRenderHandler.lock) {
            // 创建记录 OpenGL ES 状态机信息的对象 EGLContext
            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, rootContext, contextAttributes, 0);
        }
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException(
                    "Failed to create EGL context: 0x" + Integer.toHexString(EGL14.eglGetError()));
        }
        return eglContext;
    }


    // 视频帧回调实现
    // Video frame callback implementation
    @Override
    public void doFrame(long frameTimeNanos) {
        if (!mIsRunning) {
            return;
        }
        Choreographer.getInstance().postFrameCallback(this);

        // 对视频帧进行绘制
        draw();
    }

    // 使用渲染类型进行绘制
    // Use the rendering type to draw
    private void draw() {

        for (Map.Entry<String, MyVideoFrame> entry : frameMap.entrySet()) {
            // 获取视频帧数据
            MyVideoFrame frameBuffer = entry.getValue();
            if (frameBuffer != null) {
                String streamID = entry.getKey();

                // 获取流名对应的渲染类对象
                Renderer renderer = rendererMap.get(streamID);
                PixelBuffer pixelBuffer = new PixelBuffer();
                pixelBuffer.buffer = frameBuffer.byteBuffers;
                pixelBuffer.strides = frameBuffer.strides;
                pixelBuffer.height = frameBuffer.height;
                pixelBuffer.width = frameBuffer.width;

                if (renderer != null) {
                    // 渲染类根据视频帧数据进行绘制
                    renderer.draw(pixelBuffer);
                }
            }
        }
    }

    private MyVideoFrame videoCaptureFrame = new MyVideoFrame();
    private MyVideoFrame videoPlayFrame = new MyVideoFrame();

    private int mFrames;
    private byte[] mInputData;
    private byte[] mReadback;
    private ByteBuffer[] mOutputBuffer;

    @Override
    public void onCapturedVideoFrameRawData(ByteBuffer[] data, int[] dataLength, ZegoVideoFrameParam param, ZegoVideoFlipMode flipMode, ZegoPublishChannel channel) {
        if (mFrames++ % 100 == 0) {
            Log.v(TAG, "onCapturedVideoFrameRawData: data " + data.length + ", dataLength " + dataLength.length
                    + ", param width " + param.width + ", param height " + param.height + ", param rotation " + param.rotation
                    + ", param format " + param.format + ", flipMode " + flipMode.name() + ", channel " + channel.name()
                    + ", egl " + EGL14.eglGetCurrentContext() + ", data0 length " + (data.length > 0 ? data[0].limit() : 0)
                    + ", data1 length " + (data.length > 1 ? data[1].limit() : 0) + ", data2 length " + (data.length > 2 ? data[2].limit() : 0)
                    + ", dataLength0 " + (dataLength.length > 0 ? dataLength[0] : 0) + ", dataLength1 " + (dataLength.length > 0 ? dataLength[1] : 0)
                    + ", dataLength2 " + (dataLength.length > 0 ? dataLength[2] : 0) + ", stride0 " + (param.strides.length > 0 ? param.strides[0] : 0)
                    + ", stride1 " + (param.strides.length > 1 ? param.strides[1] : 0) + ", stride2 " + (param.strides.length > 2 ? param.strides[2] : 0)
                    + ", looper " + Looper.myLooper());
        }
//        onCapturedVideoFrameRawData: data 3, dataLength 3, param width 360, param height 640,
//        param rotation 0, param format I420, flipMode NONE, channel MAIN, egl android.opengl.EGLContext@ca42037e,
//        data0 length 235520, data1 length 58880, data2 length 58880,
//        dataLength0 235520, dataLength1 58880, dataLength2 58880, stride0 368, stride1 184, stride2 184

        // RGBA 只需考虑 data[0]，I420 需考虑 data[0,1,2]
        // RGBA 只需考虑 dataLength[0]，I420 需考虑 dataLength[0,1,2]
        int length = dataLength[0];
        if (mCapLoopHandler == null) {
            mCapLoopHandler = new Handler(Looper.myLooper());
            Log.i(TAG, "onCapturedVideoFrameRawData: create capLoop handler");
            mFURenderer.onSurfaceCreated();

            mInputData = new byte[length];
            mReadback = new byte[length];
            mOutputBuffer = new ByteBuffer[data.length];
            mOutputBuffer[0] = ByteBuffer.allocateDirect(length);
            mOutputBuffer[1] = data[1];
            mOutputBuffer[2] = data[2];
        }

        data[0].get(mInputData);
        mFURenderer.onDrawFrameSingleInput(mInputData, param.width, param.height, FURenderer.INPUT_FORMAT_RGBA_BUFFER
                , mReadback, param.width, param.height);

        mOutputBuffer[0].put(mReadback);
        mOutputBuffer[0].flip();
        videoCaptureFrame.byteBuffers = mOutputBuffer;
        videoCaptureFrame.height = param.height;
        videoCaptureFrame.width = param.width;
        videoCaptureFrame.strides = param.strides;
        getFrameMap().put(mainPublishChannel, videoCaptureFrame);
    }

    @Override
    public void onRemoteVideoFrameRawData(ByteBuffer[] data, int[] dataLength, ZegoVideoFrameParam param, String streamID) {
        data[0].put(mReadback);
        data[0].flip();
        videoPlayFrame.byteBuffers = data;
        videoPlayFrame.height = param.height;
        videoPlayFrame.width = param.width;
        videoPlayFrame.strides = param.strides;
        getFrameMap().put(streamID, videoPlayFrame);
    }

//    @Override
//    public void onRemoteVideoFrameEncodedData(ByteBuffer data, int dataLength, ZegoVideoEncodedFrameParam param, long referenceTimeMillisecond, String streamID) {
//        Log.d(TAG, "onRemoteVideoFrameRawData: " + param.format.value());
//
//        byte[] tmpData = new byte[data.capacity()];
//        data.position(0); // 缺少此行，解码后的渲染画面会卡住
//        data.get(tmpData);
//        if (mAVCDecoder != null) {
//            mViewHeight = param.height;
//            mViewWidth = param.width;
//            // 为解码提供视频数据，时间戳
//            mAVCDecoder.inputFrameToDecoder(tmpData, (long) referenceTimeMillisecond);
//        }
//    }

    //  AVCANNEXB 模式解码器
    private AVCDecoder mAVCDecoder = null;

    // 添加解码 AVCANNEXB 格式视频帧的渲染视图
    public void addDecodView(final TextureView textureView) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mAVCDecoder == null) {
                    // 创建解码器
                    mAVCDecoder = new AVCDecoder(new Surface(textureView.getSurfaceTexture()), mViewWidth, mViewHeight);
                    // 启动解码器
                    mAVCDecoder.startDecoder();
                }
            }
        });
    }
}
