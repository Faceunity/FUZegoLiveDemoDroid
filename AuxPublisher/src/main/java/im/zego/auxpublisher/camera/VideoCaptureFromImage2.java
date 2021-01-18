package im.zego.auxpublisher.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import im.zego.auxpublisher.ve_gl.EglBase;
import im.zego.auxpublisher.ve_gl.GlRectDrawer;
import im.zego.auxpublisher.ve_gl.GlUtil;
import im.zego.common.util.AppLogger;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.constants.ZegoPublishChannel;

/**
 * VideoCaptureFromImage2
 * 实现将图片源作为视频数据并传给ZEGO SDK，需要继承实现ZEGO SDK的ZegoVideoCaptureDevice类
 * 采用GL_TEXTURE_2D方式传递数据，即OpenGL ES的2d贴图，通过client的onTextureCaptured传递采集数据
 */

/**
 * VideoCaptureFromImage2
 * To implement image source as video data and pass it to ZEGO SDK, you need to inherit the ZegoVideoCaptureDevice class that implements ZEGO SDK
 * Use GL_TEXTURE_2D to transfer data, that is, 2D texture of OpenGL ES, and transfer the collected data through onTextureCaptured of client
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class VideoCaptureFromImage2 extends ZegoVideoCaptureCallback
        implements Choreographer.FrameCallback, TextureView.SurfaceTextureListener, SurfaceHolder.Callback {
    private TextureView mTextureView = null;
    private SurfaceView mSurfaceView = null;
    private EglBase previewEglBase;

    private int mBitmapTextureId = 0;
    private int mPreviewTextureId = 0;
    private int mFrameBufferId = 0;

    private GlRectDrawer previewDrawer;
    // 纹理变换矩阵
    private float[] flipMatrix = new float[]{1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f};

    private float[] transformationMatrix = new float[]{1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f};

    private int mViewWidth = 0;
    private int mViewHeight = 0;
    private int mImageWidth = 0;
    private int mImageHeight = 0;

    private boolean mIsRunning = false;
    private boolean mIsCapture = false;
    private boolean mIsPreview = false;
    private Bitmap mBitmap = null;

    private HandlerThread mThread = null;
    private Handler mHandler = null;

    private Context mContext = null;

    ZegoExpressEngine mSDKEngine;
    // 图片坐标参数
    //Picture coordinate parameters
    private int mX = 0;
    private int mY = 0;
    private int mDrawCounter = 0;

    // context用于获取图片资源
    //context is used to obtain image resources
    public VideoCaptureFromImage2(Context context, ZegoExpressEngine mSDKEngine) {
        mContext = context;
        this.mSDKEngine = mSDKEngine;
    }

    // 初始化 OpenGL ES 的资源，为保证后续调用都是合法的，此处使用同步方式初始化
    //Initialize the resources of OpenGL ES, in order to ensure that subsequent calls are legal, here we use synchronous way to initialize
    public final int init() {
        mThread = new HandlerThread("VideoCaptureFromImage2" + hashCode());
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                previewEglBase = EglBase.create(null, EglBase.CONFIG_RGBA);
                previewDrawer = new GlRectDrawer();

                // 注册 Choreographer 的刷新回调，保证后续绘制图片时不会出现画面撕裂的问题
                //Register the refresh callback of Choreographer to ensure that the picture will not tear when drawing pictures
                Choreographer.getInstance().postFrameCallback(VideoCaptureFromImage2.this);
                mIsRunning = true;

                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 释放OpenGL ES 的资源
    //Free up OpenGL ES resources
    public final int uninit() {
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

        mHandler = null;
        if (Build.VERSION.SDK_INT >= 18) {
            mThread.quitSafely();
        } else {
            mThread.quit();
        }
        mThread = null;

        return 0;
    }

    // 设置位图
    public void setBitmap(final Bitmap bitmap) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBitmap = bitmap;
            }
        });
    }

    /**
     * 更新采集数据
     * <p>
     * 当 Choreographer 刷新回调触发时，绘制图像数据到屏幕和 SDK 提供的 EglSurface 上
     */
    //When Choreographer refresh callback is triggered, draw image data to the screen and EglSurface provided by SDK
    @Override
    public void doFrame(long frameTimeNanos) {
        if (!mIsRunning) {
            return;
        }
        Choreographer.getInstance().postFrameCallback(this);

        if (mBitmap == null) {
            return;
        }

        if (mIsPreview) {
            // 设置展示视图
            //Set display view
            if (mTextureView != null) {
                attachTextureView();
            } else if (mSurfaceView != null) {
                attachSurfaceView();
            }
            if (previewEglBase.hasSurface()) {
                // 绘制图片到屏幕
//                Draw a picture to the screen
                draw(mBitmap);
            }
        }

        if (mDrawCounter == 0) {
            mX = (mX + 1) % 4;
            if (mX == 0) {
                mY = (mY + 1) % 4;
            }
        }
        mDrawCounter = (mDrawCounter + 1) % 60;
    }

    // 绘制图片到屏幕
//    Draw a picture to the screen
    private void draw(final Bitmap bitmap) {
        try {
            // 绑定eglContext、eglDisplay、eglSurface
            //binding eglContext、eglDisplay、eglSurface
            previewEglBase.makeCurrent();

            if (mBitmapTextureId == 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                mBitmapTextureId = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            }

            if (mPreviewTextureId == 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                mPreviewTextureId = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mImageWidth, mImageHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

                // 创建帧缓冲对象，绘制纹理到帧缓冲区并返回缓冲区索引
//                Create frame buffer object, draw texture to frame buffer and return buffer index
                mFrameBufferId = GlUtil.generateFrameBuffer(mPreviewTextureId);
            } else {
                // 绑定帧缓冲区
//                Bind frame buffer
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            // 绘制rgb格式图像
            //Draw rgb format image
            previewDrawer.drawRgb(mBitmapTextureId, transformationMatrix,
                    mImageWidth, mImageHeight,
                    mImageWidth / 4 * mX, mImageHeight / 4 * mY,
                    mImageWidth / 4, mImageHeight / 4);
            // 解邦帧缓冲区
            //Unbind the frame buffer
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            // 绘制rgb格式图像
//            Draw rgb format image
            previewDrawer.drawRgb(mPreviewTextureId, flipMatrix,
                    mViewWidth, mViewHeight,
                    0, 0,
                    mViewWidth, mViewHeight);

            // 交换渲染好的buffer 去显示
            //Exchange the rendered buffer to display
            previewEglBase.swapBuffers();


            if (mIsCapture) {
                long now = SystemClock.elapsedRealtime();
                // 将图片数据传给ZEGO SDK，包括时间戳,需要注意这里参数指明了是ZegoPublishChannel.AUX。
                //Pass the image data to the ZEGO SDK, including the time stamp. Note that this parameter specifies ZegoPublishChannel.AUX.
                mSDKEngine.sendCustomVideoCaptureTextureData(mPreviewTextureId, mImageWidth, mImageHeight, now, ZegoPublishChannel.AUX);
            }

            // 分离当前eglContext
//            Detach the current eglContext
            previewEglBase.detachCurrent();
        } catch (RuntimeException e) {
            System.out.println(e.toString());
        }
    }


    @Override
    public void onStart(ZegoPublishChannel channel) {
        // demo里面没有配置主流自定义视频采集，不设则默认主流不开启自定义视频采集。所以demo推主流的时候实际不会走到这里,这需要引起注意。
        // There is no mainstream custom video capture configured in the demo. If not set, the default mainstream video capture is not enabled by default.
        // Therefore, when the demo is pushed to the mainstream, it will not actually come here, it should be paid attention to.
        AppLogger.getInstance().i(" CustomVideoCapture onStart callBack,channel:" + channel);
        // 初始化 OpenGL ES 的资源
//        Initialize OpenGL ES resources
        if (channel == ZegoPublishChannel.AUX) {
            init();
            // 设置图片的位图
//            Set the bitmap of the picture
            setBitmap(createBitmapFromAsset());
            setResolution(360, 650);
            startCapture();
            startPreview();
        }
    }


    @Override
    public void onStop(ZegoPublishChannel channel) {
        // demo里面没有配置主流自定义视频采集，不设则默认主流不开启自定义视频采集。所以demo推主流的时候实际不会走到这里,这需要引起注意。
        // There is no mainstream custom video capture configured in the demo. If not set, the default mainstream video capture is not enabled by default.
        // Therefore, when the demo is pushed to the mainstream, it will not actually come here,it should be paid attention to.
        AppLogger.getInstance().i(" CustomVideoCapture onStop callBack,channel:" + channel);
        if (channel == ZegoPublishChannel.AUX) {
            stopPreview();
            stopCapture();
            uninit();
        }
    }


    // 开始推流时,ZEGO SDK 调用 startCapture 通知外部采集设备开始工作，必须实现
//    When starting to push the stream, ZEGO SDK calls startCapture to notify the external collection device to start work, which must be implemented
    private int startCapture() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsCapture = true;
            }
        });
        return 0;
    }

    // 停止推流时，ZEGO SDK 调用 stopCapture 通知外部采集设备停止采集，必须实现
//    When stopping pushing, the ZEGO SDK calls stopCapture to notify the external collection device to stop collection, which must be implemented
    private int stopCapture() {
        if (mHandler == null) {
            return 0;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsCapture = false;
            }
        });
        return 0;
    }


    private int setFrameRate(int framerate) {
        return 0;
    }

    // 设置视图宽高
//    Set view width and height
    private int setResolution(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
        return 0;
    }

    // 前后摄像头的切换
//    Switching between front and back cameras
    private int setFrontCam(int bFront) {
        return 0;
    }

    // 设置展示视图
//    Set display view
    @Override
    public void setView(final View view) {
        if (view instanceof TextureView) {
            setRendererView((TextureView) view);
        } else if (view instanceof SurfaceView) {
            setRendererView((SurfaceView) view);
        }
    }


    protected int startPreview() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsPreview = true;
            }
        });
        return 0;
    }

    // 停止预览
    protected int stopPreview() {
        if (mHandler == null) {
            return 0;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsPreview = false;
            }
        });
        return 0;
    }


    // 从资源区获取图片位图
//    Get picture bitmap from resource area
    private Bitmap createBitmapFromAsset() {
        Bitmap bitmap = null;
        try {
            AssetManager assetManager = mContext.getAssets();
            InputStream is = assetManager.open("logo.png");
            bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                System.out.println("测试一:width=" + bitmap.getWidth() + " ,height=" + bitmap.getHeight());
            } else {
                System.out.println("bitmap == null");
            }
        } catch (Exception e) {
            System.out.println("异常信息:" + e.toString());
        }
        return bitmap;
    }

    // 设置渲染视图，TextureView格式
//    Set the rendering view, TextureView format
    public int setRendererView(final TextureView view) {

        if (mHandler == null) {
            doSetRendererView(view);
        } else {
            final CountDownLatch barrier = new CountDownLatch(1);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    doSetRendererView(view);
                    barrier.countDown();
                }
            });
            try {
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    // 设置SurfaceView回调监听
    // Set SurfaceView callback monitoring
    private void doSetRendererView(SurfaceView view) {
        final SurfaceView temp = view;

        if (mSurfaceView != null) {
            mSurfaceView.getHolder().removeCallback(VideoCaptureFromImage2.this);
            releasePreviewSurface();
        }

        mSurfaceView = temp;
        if (mSurfaceView != null) {
            mSurfaceView.getHolder().addCallback(VideoCaptureFromImage2.this);
        }
    }

    // 设置Texture.SurfaceTextureListener回调监听
//    Set Texture.SurfaceTextureListener callback listener
    private void doSetRendererView(TextureView view) {
        if (mTextureView != null) {
            if (mTextureView.getSurfaceTextureListener().equals(VideoCaptureFromImage2.this)) {
                mTextureView.setSurfaceTextureListener(null);
            }

            releasePreviewSurface();
        }

        mTextureView = view;
        if (mTextureView != null) {
            mTextureView.setSurfaceTextureListener(VideoCaptureFromImage2.this);
        }
    }

    // 设置渲染视图，SurfaceView格式
//    Set the rendering view, SurfaceView format
    public int setRendererView(final SurfaceView view) {
        final SurfaceView temp = view;
        if (mHandler != null) {
            final CountDownLatch barrier = new CountDownLatch(1);
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    doSetRendererView(view);
                    barrier.countDown();

                }
            });

            try {
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else {
            doSetRendererView(view);
        }

        return 0;
    }

    // 设置预览视图，TextureView格式
    //    Set preview view, TextureView format
    private void attachTextureView() {
        if (previewEglBase.hasSurface()) {
            return;
        }

        if (!mTextureView.isAvailable()) {
            return;
        }

        mViewWidth = mTextureView.getWidth();
        mViewHeight = mTextureView.getHeight();
        try {
            // 创建用于预览的EGLSurface
//            Create EGLSurface for preview
            previewEglBase.createSurface(mTextureView.getSurfaceTexture());
        } catch (RuntimeException e) {
            previewEglBase.releaseSurface();
            mViewWidth = 0;
            mViewHeight = 0;
        }
    }

    // 设置预览视图，SurfaceView格式
//    Set preview view, SurfaceView format
    private void attachSurfaceView() {
        if (previewEglBase.hasSurface()) {
            return;
        }

        SurfaceHolder holder = mSurfaceView.getHolder();
        if (holder.isCreating() || null == holder.getSurface()) {
            return;
        }

        Rect size = holder.getSurfaceFrame();
        mViewWidth = size.width();
        mViewHeight = size.height();
        try {
            // 创建用于预览的EGLSurface
            //            Create EGLSurface for preview
            previewEglBase.createSurface(holder.getSurface());
        } catch (RuntimeException e) {
            previewEglBase.releaseSurface();
            mViewWidth = 0;
            mViewHeight = 0;
        }
    }

    // 销毁用于预览的surface
//    Destroy the surface for preview
    private void releasePreviewSurface() {
        if (previewEglBase.hasSurface()) {
            // 绑定eglContext、eglDisplay、eglSurface
//            Bind eglContext, eglDisplay, eglSurface
            previewEglBase.makeCurrent();

            if (mBitmapTextureId != 0) {
                int[] textures = new int[]{mBitmapTextureId};
                GLES20.glDeleteTextures(1, textures, 0);
                mBitmapTextureId = 0;
            }

            if (mPreviewTextureId != 0) {
                int[] textures = new int[]{mPreviewTextureId};
                GLES20.glDeleteTextures(1, textures, 0);
                mPreviewTextureId = 0;
            }

            if (mFrameBufferId != 0) {
                int[] frameBuffers = new int[]{mFrameBufferId};
                GLES20.glDeleteFramebuffers(1, frameBuffers, 0);
                mFrameBufferId = 0;
            }

            previewEglBase.releaseSurface();
            previewEglBase.detachCurrent();
        }
    }

    // 释放绘制相关类
//    Release drawing related classes
    private void release() {
        // 销毁用于预览的surface
//        Destroy the surface for preview
        releasePreviewSurface();
        if (previewDrawer != null) {
            previewDrawer.release();
            previewDrawer = null;
        }

        if (previewEglBase != null) {
            previewEglBase.release();
            previewEglBase = null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // 安全销毁用于预览的surface
//        Safely destroy the surface used for preview
        releasePreviewSurfaceSafe();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // 安全销毁用于预览的surface
        //        Safely destroy the surface used for preview
        releasePreviewSurfaceSafe();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 安全销毁用于预览的surface
        //        Safely destroy the surface used for preview
        releasePreviewSurfaceSafe();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // 安全销毁用于预览的surface
        //        Safely destroy the surface used for preview
        releasePreviewSurfaceSafe();
    }

    // 安全销毁用于预览的surface
    //        Safely destroy the surface used for preview
    private void releasePreviewSurfaceSafe() {
        final CountDownLatch barrier = new CountDownLatch(1);
        if (mHandler == null) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                releasePreviewSurface();
                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
