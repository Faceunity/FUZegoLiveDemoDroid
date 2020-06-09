package com.zego.videofilter.videoFilter;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.zego.videofilter.faceunity.FURenderer;
import com.zego.videofilter.videoFilter.ve_gl.EglBase;
import com.zego.videofilter.videoFilter.ve_gl.EglBase14;
import com.zego.videofilter.videoFilter.ve_gl.GlRectDrawer;
import com.zego.videofilter.videoFilter.ve_gl.GlUtil;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilter;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

/**
 * 外部滤镜采用 BUFFER_TYPE_SURFACE_TEXTURE（传递 SurfaceTexture）方式传递数据给 SDK。
 * <p>
 * Created by robotding on 17/3/28.
 */

public class VideoFilterSurfaceTextureDemo extends ZegoVideoFilter implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "VideoFilterSurfaceTextu";
    // SDK 内部实现 ZegoVideoFilter.Client 协议的对象
    private ZegoVideoFilter.Client mClient = null;

    // faceunity 美颜处理类
    private FURenderer mFuRender;

    private HandlerThread mThread = null;
    private volatile Handler mHandler = null;

    private EglBase mDummyContext = null;
    private EglBase mEglContext = null;
    private int mInputWidth = 0;
    private int mInputHeight = 0;
    private int mOutputWidth = 0;
    private int mOutputHeight = 0;
    private SurfaceTexture mInputSurfaceTexture = null;
    private int mInputTextureId = 0;
    private Surface mOutputSurface = null;
    private boolean mIsEgl14 = false;

    private GlRectDrawer mDrawer = null;
    private float[] transformationMatrix = new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    public VideoFilterSurfaceTextureDemo(FURenderer fuRenderer) {
        this.mFuRender = fuRenderer;
    }

    /**
     * 初始化资源，比如图像绘制（openGL）、美颜组件等
     *
     * @param client SDK 内部实现 ZegoVideoFilter.Client 协议的对象
     *               <p>
     *               注意：client 必须保存为强引用对象，在 stopAndDeAllocate 被调用前必须一直被保存。
     *               SDK 不负责管理 client 的生命周期。
     */
    @Override
    protected void allocateAndStart(ZegoVideoFilter.Client client) {
        Log.d(TAG, "allocateAndStart: ");
        mClient = client;
        mThread = new HandlerThread("video-filter");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        mInputWidth = 0;
        mInputHeight = 0;

        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDummyContext = EglBase.create(null, EglBase.CONFIG_PIXEL_BUFFER);

                try {
                    mDummyContext.createDummyPbufferSurface();
                    mDummyContext.makeCurrent();
                } catch (RuntimeException e) {
                    // Clean up before rethrowing the exception.
                    mDummyContext.releaseSurface();
                    throw e;
                }

                mInputTextureId = GlUtil.generateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
                // 滤镜 SurfaceTexture
                mInputSurfaceTexture = new SurfaceTexture(mInputTextureId);
                mInputSurfaceTexture.setOnFrameAvailableListener(VideoFilterSurfaceTextureDemo.this);

                mEglContext = EglBase.create(mDummyContext.getEglBaseContext(), EglBase.CONFIG_RECORDABLE);
                mIsEgl14 = EglBase14.isEGL14Supported();

                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放资源
     * 注意：必须调用 client 的 destroy 方法，否则会造成内存泄漏。
     */
    @Override
    protected void stopAndDeAllocate() {
        Log.d(TAG, "stopAndDeAllocate: ");
        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
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

        mThread.quit();
        mThread = null;

        // 建议在同步停止滤镜任务后再清理 client 对象，保证 SDK 调用 stopAndDeAllocate 后，没有残留的异步任务导致野指针 crash。
        mClient.destroy();
        mClient = null;
    }

    /**
     * 指定滤镜的传递数据类型，传递 SurfaceTexture。
     * SDK 需要根据 supportBufferType 返回的类型值创建不同的 client 对象。
     *
     * @return
     */
    @Override
    protected int supportBufferType() {
        return BUFFER_TYPE_SURFACE_TEXTURE;
    }

    /**
     * SDK 通知外部滤镜当前采集图像的宽高并获取 SurfaceTexture
     *
     * @param width  采集图像宽
     * @param height 采集图像高
     * @param stride
     * @return
     */
    @Override
    protected int dequeueInputBuffer(final int width, final int height, int stride) {
        if (stride != width * 4) {
            return -1;
        }

        if (mInputWidth != width || mInputHeight != height) {
            if (mClient.dequeueInputBuffer(width, height, stride) < 0) {
                return -1;
            }

            mInputWidth = width;
            mInputHeight = height;

            // 获取 SDK 给出的 SurfaceTexture
            final SurfaceTexture surfaceTexture = mClient.getSurfaceTexture();
            final CountDownLatch barrier = new CountDownLatch(1);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // 设置 Surface
                    setOutputSurface(surfaceTexture, width, height);
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

    @Override
    protected ByteBuffer getInputBuffer(int index) {
        return null;
    }

    @Override
    protected void queueInputBuffer(int bufferIndex, int width, int height, int stride, long timestamp_100n) {
    }

    // SDK 会调用此滤镜的 getSurfaceTexture 获取 SurfaceTexture 对象
    @Override
    protected SurfaceTexture getSurfaceTexture() {
        return mInputSurfaceTexture;
    }

    @Override
    protected void onProcessCallback(int textureId, int width, int height, long timestamp_100n) {

    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mDummyContext.makeCurrent();
        if (mDrawer == null) {
            mDrawer = new GlRectDrawer();
        }

        surfaceTexture.updateTexImage();
        long timestampNs = surfaceTexture.getTimestamp();
        mDummyContext.detachCurrent();

        mEglContext.makeCurrent();

        // 调用 faceunity 进行美颜，美颜后返回纹理 ID
        int textureID = mFuRender.onDrawFrameSingleInput(mInputTextureId, mOutputWidth, mInputHeight);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        // 绘制美颜数据
        mDrawer.drawRgb(textureID, transformationMatrix,
                mOutputWidth, mOutputHeight, 0, 0, mOutputWidth, mOutputHeight);


        if (mIsEgl14) {
            ((EglBase14) mEglContext).swapBuffers(timestampNs);
        } else {
            mEglContext.swapBuffers();
        }

        mEglContext.detachCurrent();
    }

    // 设置 Surface
    private void setOutputSurface(SurfaceTexture surfaceTexture, int width, int height) {
        if (mEglContext.hasSurface()) {
            mEglContext.makeCurrent();
            if (mDrawer != null) {
                mDrawer.release();
                mDrawer = null;
            }

            // 销毁 faceunity 相关的资源
            mFuRender.onSurfaceDestroyed();

            mEglContext.releaseSurface();
        }

        if (mOutputSurface != null) {
            mOutputSurface.release();
            mOutputSurface = null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            surfaceTexture.setDefaultBufferSize(width, height);
        }
        mOutputSurface = new Surface(surfaceTexture);
        mOutputWidth = width;
        mOutputHeight = height;

        mEglContext.createSurface(mOutputSurface);

        mEglContext.makeCurrent();

        // 创建及初始化 faceunity 相应的资源
        mFuRender.onSurfaceCreated();
    }

    // 释放 openGL 相关资源
    private void release() {
        mInputSurfaceTexture.release();
        mInputSurfaceTexture = null;

        mDummyContext.makeCurrent();
        if (mInputTextureId != 0) {
            int[] textures = new int[]{mInputTextureId};
            GLES20.glDeleteTextures(1, textures, 0);
            mInputTextureId = 0;
        }
        mDummyContext.release();
        mDummyContext = null;

        if (mEglContext.hasSurface()) {
            mEglContext.makeCurrent();
            if (mDrawer != null) {
                mDrawer.release();
                mDrawer = null;
            }

            // 销毁 faceunity 相关的资源
            mFuRender.onSurfaceDestroyed();
        }
        mEglContext.release();
        mEglContext = null;

        if (mOutputSurface != null) {
            mOutputSurface.release();
            mOutputSurface = null;
        }
    }
}
