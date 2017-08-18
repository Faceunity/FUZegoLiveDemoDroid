package com.zego.livedemo5.videocapture;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.faceunity.wrapper.faceunity;
import com.zego.livedemo5.faceunity.EffectAndFilterSelectAdapter;
import com.zego.livedemo5.faceunity.FaceunityController;
import com.zego.livedemo5.faceunity.authpack;
import com.zego.livedemo5.videocapture.ve_gl.EglBase;
import com.zego.livedemo5.videocapture.ve_gl.GlRectDrawer;
import com.zego.livedemo5.videocapture.ve_gl.GlUtil;
import com.zego.zegoliveroom.videocapture.ZegoVideoCaptureDevice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by robotding on 17/2/15.
 */

public class VideoCaptureFromFaceunity extends ZegoVideoCaptureDevice
        implements Camera.PreviewCallback, TextureView.SurfaceTextureListener, SurfaceHolder.Callback, FaceunityController {
    private static final String TAG = "VideoCaptureFromFU";
    private static final int CAMERA_STOP_TIMEOUT_MS = 7000;

    private TextureView mTextureView = null;
    private SurfaceView mSurfaceView = null;
    private EglBase previewEglBase;

    private SurfaceTexture mCameraSurfaceTexture;
    private int mCameraTextureId = 0;
    private int mPreviewTextureId = 0;
    private int mFrameBufferId = 0;

    private GlRectDrawer previewDrawer;
    private float[] transformationMatrix = new float[]{1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 1.0f};

    private int mViewWidth = 0;
    private int mViewHeight = 0;

    private boolean mIsRunning = false;
    private boolean mIsCapture = false;
    private boolean mIsPreview = false;

    private HandlerThread mGLThread = null;
    private Handler mGLHandler = null;

    private Context mContext = null;
    private Client mClient = null;

    private Camera mCam = null;
    private Camera.CameraInfo mCamInfo = null;
    int mCameraFront = 0;
    int mCameraWidth = 640;
    int mCameraHeight = 480;
    int mCameraFrameRate = 15;
    int mCameraRotation = 0;

    // Arbitrary queue depth.  Higher number means more memory allocated & held,
    // lower number means more sensitivity to processing time in the client (and
    // potentially stalling the capturer if it runs out of buffers to write to).
    private static final int NUMBER_OF_CAPTURE_BUFFERS = 3;
    private int mFrameSize = 0;

    private final AtomicBoolean isCameraRunning = new AtomicBoolean();
    private final Object pendingCameraRestartLock = new Object();
    private volatile boolean pendingCameraRestart = false;

    private int mFacebeautyItem = 0; //美颜道具
    private int mEffectItem = 0; //贴纸道具
    private int mGestureItem = 0; //手势道具
    private int[] itemsArray = {mFacebeautyItem, mEffectItem, mGestureItem};

    private float mFacebeautyColorLevel = 0.2f;
    private float mFacebeautyBlurLevel = 6.0f;
    private float mFacebeautyCheeckThin = 1.0f;
    private float mFacebeautyEnlargeEye = 0.5f;
    private float mFacebeautyRedLevel = 0.5f;
    private int mFaceShape = 3;
    private float mFaceShapeLevel = 0.5f;

    private String mFilterName = EffectAndFilterSelectAdapter.FILTERS_NAME[0];

    boolean isNeedEffectItem = true;
    private String mEffectFileName = EffectAndFilterSelectAdapter.EFFECT_ITEM_FILE_NAME[1];
    private CreateItemHandler mCreateItemHandler;

    private int faceTrackingStatus = 0;

    private byte[] mCameraNV21Byte;
    private byte[] fuImgNV21Bytes;

    private int mFrameId = 0;

    VideoCaptureFromFaceunity(Context context) {
        mContext = context;
    }

    public final int init() {
        mGLThread = new HandlerThread("VideoCaptureFromFaceunity" + hashCode());
        mGLThread.start();
        mGLHandler = new Handler(mGLThread.getLooper());

        final CountDownLatch barrier = new CountDownLatch(1);
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "EglBase.create");
                previewEglBase = EglBase.create(null, EglBase.CONFIG_RGBA);
                previewDrawer = new GlRectDrawer();

                mIsRunning = true;

                barrier.countDown();

                loadFaceunity();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mCreateItemHandler = new CreateItemHandler(mGLThread.getLooper());
        return 0;
    }

    private void loadFaceunity() {
        try {
            InputStream is = mContext.getAssets().open("v3.mp3");
            byte[] v3data = new byte[is.available()];
            int len = is.read(v3data);
            is.close();
            faceunity.fuSetup(v3data, null, authpack.A());
            //faceunity.fuSetMaxFaces(1);
            Log.e(TAG, "fuSetup v3 len " + len);

            loadFaceunityBeautification();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFaceunityBeautification() {
        try {
            InputStream is = mContext.getAssets().open("face_beautification.mp3");
            byte[] itemData = new byte[is.available()];
            int len = is.read(itemData);
            Log.e(TAG, "beautification len " + len);
            is.close();
            mFacebeautyItem = faceunity.fuCreateItemFromPackage(itemData);
            itemsArray[0] = mFacebeautyItem;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final int uninit() {
        final CountDownLatch barrier = new CountDownLatch(1);
        mGLHandler.post(new Runnable() {
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
        mGLHandler = null;

        if (Build.VERSION.SDK_INT >= 18) {
            mGLThread.quitSafely();
        } else {
            mGLThread.quit();
        }
        mGLThread = null;

        return 0;
    }

    private void draw() {

        if (mCameraNV21Byte == null || mCameraNV21Byte.length == 0) {
            Log.e(TAG, "camera nv21 bytes null");
            return;
        }

        try {
            previewEglBase.makeCurrent();
            GlUtil.checkGlError("makeCurrent");

            float[] mtx = new float[16];
            try {
                mCameraSurfaceTexture.updateTexImage();
                mCameraSurfaceTexture.getTransformMatrix(mtx);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (mPreviewTextureId == 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GlUtil.checkGlError("glActiveTexture");

                mPreviewTextureId = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
                GlUtil.checkGlError("generateTexture");

                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mCameraWidth, mCameraHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                GlUtil.checkGlError("glTexImage2D");

                mFrameBufferId = GlUtil.generateFrameBuffer(mPreviewTextureId);
                GlUtil.checkGlError("generateFrameBuffer");
            } else {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
                GlUtil.checkGlError("glBindFramebuffer");
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GlUtil.checkGlError("glClear");

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

            if (isNeedEffectItem) {
                isNeedEffectItem = false;
                mCreateItemHandler.sendEmptyMessage(CreateItemHandler.HANDLE_CREATE_ITEM);
            }

            if (mFacebeautyItem == 0) {
                loadFaceunityBeautification();
            }
            faceunity.fuItemSetParam(mFacebeautyItem, "color_level", mFacebeautyColorLevel);
            faceunity.fuItemSetParam(mFacebeautyItem, "blur_level", mFacebeautyBlurLevel);
            faceunity.fuItemSetParam(mFacebeautyItem, "filter_name", mFilterName);
            faceunity.fuItemSetParam(mFacebeautyItem, "cheek_thinning", mFacebeautyCheeckThin);
            faceunity.fuItemSetParam(mFacebeautyItem, "eye_enlarging", mFacebeautyEnlargeEye);
            faceunity.fuItemSetParam(mFacebeautyItem, "face_shape", mFaceShape);
            faceunity.fuItemSetParam(mFacebeautyItem, "face_shape_level", mFaceShapeLevel);
            faceunity.fuItemSetParam(mFacebeautyItem, "red_level", mFacebeautyRedLevel);

            //faceunity.fuItemSetParam(mFacebeautyItem, "use_old_blur", 1);

            boolean isOESTexture = true; //camera默认的是OES的
            int flags = isOESTexture ? faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE : 0;
            boolean isNeedReadBack = false; //是否需要写回，如果是，则入参的byte[]会被修改为带有fu特效的
            flags = isNeedReadBack ? flags | faceunity.FU_ADM_FLAG_ENABLE_READBACK : flags;
            if (isNeedReadBack) {
                if (fuImgNV21Bytes == null) {
                    fuImgNV21Bytes = new byte[mCameraNV21Byte.length];
                }
                System.arraycopy(mCameraNV21Byte, 0, fuImgNV21Bytes, 0, mCameraNV21Byte.length);
            } else {
                fuImgNV21Bytes = mCameraNV21Byte;
            }
            flags |= 0;
            int fuTex = faceunity.fuDualInputToTexture(fuImgNV21Bytes, mCameraTextureId, flags,
                    mCameraWidth, mCameraHeight, mFrameId++, itemsArray);

            float[] matrix = new float[16];
            Matrix.multiplyMM(matrix, 0, mtx, 0, transformationMatrix, 0);

            previewDrawer.drawRgb(fuTex, matrix,
                    mCameraWidth, mCameraHeight,
                    0, 0,
                    mCameraWidth, mCameraHeight);
            GlUtil.checkGlError("drawRgb");

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GlUtil.checkGlError("glBindFramebuffer");

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GlUtil.checkGlError("glClear");

            previewDrawer.drawRgb(mPreviewTextureId, transformationMatrix,
                    mViewWidth, mViewHeight,
                    0, 0,
                    mViewWidth, mViewHeight);
            GlUtil.checkGlError("drawRgb");

            previewEglBase.swapBuffers();
            GlUtil.checkGlError("swapBuffers");

            if (mIsCapture) {
                long now = SystemClock.elapsedRealtime();
                mClient.onTextureCaptured(mPreviewTextureId, mCameraWidth, mCameraHeight, now);
            }

            previewEglBase.detachCurrent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void allocateAndStart(Client client) {
        Log.e(TAG, "allocateAndStart");
        mClient = client;

        init();
    }

    @Override
    protected void stopAndDeAllocate() {
        Log.e(TAG, "stopAndDeAllocate");
        uninit();

        mClient.destroy();
        mClient = null;
    }

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

    @Override
    protected int supportBufferType() {
        Log.e(TAG, "supportBufferType");
        return PIXEL_BUFFER_TYPE_GL_TEXTURE_2D;
    }

    @Override
    protected int setFrameRate(int framerate) {
        Log.e(TAG, "setFrameRate");
        mCameraFrameRate = framerate;
        updateRateOnCameraThread(framerate);
        return 0;
    }

    @Override
    protected int setResolution(int width, int height) {
        Log.e(TAG, "setResolution");
        mCameraWidth = width;
        mCameraHeight = height;
        restartCam();
        return 0;
    }

    @Override
    protected int setFrontCam(int bFront) {
        Log.e(TAG, "setFrontCam");
        mCameraFront = bFront;
        restartCam();
        return 0;
    }

    @Override
    protected int setView(View view) {
        Log.e(TAG, "setView = " + view);
        if (view instanceof TextureView) {
            setRendererView((TextureView) view);
        } else if (view instanceof SurfaceView) {
            setRendererView((SurfaceView) view);
        }
        return 0;
    }

    @Override
    protected int setViewMode(int nMode) {
        Log.e(TAG, "setViewMode = " + nMode);
        return 0;
    }

    @Override
    protected int setViewRotation(int nRotation) {
        Log.e(TAG, "setViewRotation");
        return 0;
    }

    @Override
    protected int setCaptureRotation(int nRotation) {
        Log.e(TAG, "setCaptureRotation");
        mCameraRotation = nRotation;
        return 0;
    }

    @Override
    protected int startPreview() {
        Log.e(TAG, "startPreview");
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsPreview = true;
            }
        });
        return startCapture();
    }

    @Override
    protected int stopPreview() {
        Log.e(TAG, "stopPreview");
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                mIsPreview = false;
            }
        });
        return stopCapture();
    }

    @Override
    protected int enableTorch(boolean bEnable) {
        Log.e(TAG, "enableTorch");
        return 0;
    }

    @Override
    protected int takeSnapshot() {
        Log.e(TAG, "takeSnapshot");
        return 0;
    }

    @Override
    protected int setPowerlineFreq(int nFreq) {
        Log.e(TAG, "setPowerlineFreq");
        return 0;
    }

    private void doSet(TextureView textureView) {
        if (mTextureView != null) {
            if (mTextureView.getSurfaceTextureListener().equals(VideoCaptureFromFaceunity.this)) {
                mTextureView.setSurfaceTextureListener(null);
            }

            releasePreviewSurface();
        }

        mTextureView = textureView;
        if (mTextureView != null) {
            mTextureView.setSurfaceTextureListener(VideoCaptureFromFaceunity.this);
        }

    }

    public int setRendererView(TextureView view) {
        if (mGLHandler != null) {
            final TextureView temp = view;
            final CountDownLatch barrier = new CountDownLatch(1);
            mGLHandler.post(new Runnable() {
                @Override
                public void run() {
                    doSet(temp);
                    barrier.countDown();
                }
            });
            try {
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            doSet(view);
        }

        return 0;
    }

    public int setRendererView(SurfaceView view) {
        final CountDownLatch barrier = new CountDownLatch(1);
        final SurfaceView temp = view;
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSurfaceView != null) {
                    mSurfaceView.getHolder().removeCallback(VideoCaptureFromFaceunity.this);
                    releasePreviewSurface();
                }

                mSurfaceView = temp;
                if (mSurfaceView != null) {
                    mSurfaceView.getHolder().addCallback(VideoCaptureFromFaceunity.this);
                }
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

    private void attachTextureView() {
        Log.e(TAG, "attachTextureView EGL14.EGL_NO_SURFACE = " + previewEglBase.hasSurface());
        if (previewEglBase.hasSurface()) {
            return;
        }

        Log.e(TAG, "attachTextureView !isAvailable = " + !mTextureView.isAvailable());
        if (!mTextureView.isAvailable()) {
            return;
        }

        mViewWidth = mTextureView.getWidth();
        mViewHeight = mTextureView.getHeight();
        try {
            previewEglBase.createSurface(mTextureView.getSurfaceTexture());
        } catch (RuntimeException e) {
            e.printStackTrace();
            previewEglBase.releaseSurface();
            mViewWidth = 0;
            mViewHeight = 0;
        }
    }

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
            previewEglBase.createSurface(holder.getSurface());
        } catch (RuntimeException e) {
            previewEglBase.releaseSurface();
            mViewWidth = 0;
            mViewHeight = 0;
        }
    }

    private void releasePreviewSurface() {
        if (previewEglBase != null && previewEglBase.hasSurface()) {
            previewEglBase.makeCurrent();

            if (mCameraTextureId != 0) {
                int[] textures = new int[]{mCameraTextureId};
                GLES20.glDeleteTextures(1, textures, 0);
                mCameraTextureId = 0;
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

            mFrameId = 0;

            //Note: 切忌使用一个已经destroy的item
            faceunity.fuDestroyItem(mEffectItem);
            itemsArray[1] = mEffectItem = 0;
            faceunity.fuDestroyItem(mFacebeautyItem);
            itemsArray[0] = mFacebeautyItem = 0;
            faceunity.fuOnDeviceLost();
            isNeedEffectItem = true;

            mCreateItemHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
        }
    }

    private void release() {
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
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    private void releasePreviewSurfaceSafe() {
        if (mGLHandler != null) {
            final CountDownLatch barrier = new CountDownLatch(1);
            mGLHandler.post(new Runnable() {
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
        } else {
            releasePreviewSurface();
        }
    }

    private int updateRateOnCameraThread(final int framerate) {
        checkIsOnCameraThread();
        if (mCam == null) {
            return 0;
        }

        mCameraFrameRate = framerate;

        Camera.Parameters parms = mCam.getParameters();
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            if ((entry[0] == entry[1]) && entry[0] == mCameraFrameRate * 1000) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                break;
            }
        }

        int[] realRate = new int[2];
        parms.getPreviewFpsRange(realRate);
        if (realRate[0] == realRate[1]) {
            mCameraFrameRate = realRate[0] / 1000;
        } else {
            mCameraFrameRate = realRate[1] / 2 / 1000;
        }

        try {
            mCam.setParameters(parms);
        } catch (Exception ex) {
            Log.i(TAG, "vcap: update fps -- set camera parameters error with exception\n");
            ex.printStackTrace();
        }
        return 0;
    }

    private void checkIsOnCameraThread() {
        if (mGLHandler == null) {
            Log.e(TAG, "Camera is not initialized - can't check thread.");
        } else if (Thread.currentThread() != mGLHandler.getLooper().getThread()) {
            throw new IllegalStateException("Wrong thread");
        }
    }

    private boolean maybePostOnCameraThread(Runnable runnable) {
        return mGLHandler != null && isCameraRunning.get()
                && mGLHandler.postAtTime(runnable, this, SystemClock.uptimeMillis());
    }


    // Note that this actually opens the camera, and Camera callbacks run on the
    // thread that calls open(), so this is done on the CameraThread.
    private int createCamOnCameraThread() {
        checkIsOnCameraThread();
        if (!isCameraRunning.get()) {
            Log.e(TAG, "startCaptureOnCameraThread: Camera is stopped");
            return 0;
        }

        Log.i(TAG, "board: " + Build.BOARD);
        Log.i(TAG, "device: " + Build.DEVICE);
        Log.i(TAG, "manufacturer: " + Build.MANUFACTURER);
        Log.i(TAG, "brand: " + Build.BRAND);
        Log.i(TAG, "model: " + Build.MODEL);
        Log.i(TAG, "product: " + Build.PRODUCT);
        Log.i(TAG, "sdk: " + Build.VERSION.SDK_INT);

        int nFacing = (mCameraFront != 0) ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;

        if (mCam != null) {
            // * already created
            return 0;
        }

        // * find camera
        mCamInfo = new Camera.CameraInfo();
        int nCnt = Camera.getNumberOfCameras();
        for (int i = 0; i < nCnt; i++) {
            Camera.getCameraInfo(i, mCamInfo);
            if (mCamInfo.facing == nFacing) {
                mCam = Camera.open(i);
                break;
            }
        }

        // * no camera found ??
        if (mCam == null) {
            Log.i(TAG, "[WARNING] no camera found, try default\n");
            mCam = Camera.open();

            if (mCam == null) {
                Log.i(TAG, "[ERROR] no camera found\n");
                return -1;
            }
        }

        // *
        // * Now set preview size
        // *
        boolean bSizeSet = false;
        Camera.Parameters parms = mCam.getParameters();
        Camera.Size psz = parms.getPreferredPreviewSizeForVideo();

        // hardcode
        psz.width = 640;
        psz.height = 480;
        parms.setPreviewSize(psz.width, psz.height);
        mCameraWidth = psz.width;
        mCameraHeight = psz.height;

        // *
        // * Now set fps
        // *
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            if ((entry[0] == entry[1]) && entry[0] == mCameraFrameRate * 1000) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                break;
            }
        }

        int[] realRate = new int[2];
        parms.getPreviewFpsRange(realRate);
        if (realRate[0] == realRate[1]) {
            mCameraFrameRate = realRate[0] / 1000;
        } else {
            mCameraFrameRate = realRate[1] / 2 / 1000;
        }

        // *
        // * Recording hint
        // *
        parms.setRecordingHint(false);

        // *
        // * focus mode
        // *
        boolean bFocusModeSet = false;
        for (String mode : parms.getSupportedFocusModes()) {
            if (mode.compareTo(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) == 0) {
                try {
                    parms.setFocusMode(mode);
                    bFocusModeSet = true;
                    break;
                } catch (Exception ex) {
                    Log.i(TAG, "[WARNING] vcap: set focus mode error (stack trace followed)!!!\n");
                    ex.printStackTrace();
                }
            }
        }
        if (!bFocusModeSet) {
            Log.i(TAG, "[WARNING] vcap: focus mode left unset !!\n");
        }

        // *
        // * Now try to set parm
        // *
        try {
            mCam.setParameters(parms);
        } catch (Exception ex) {
            Log.i(TAG, "vcap: set camera parameters error with exception\n");
            ex.printStackTrace();
        }

        Camera.Parameters actualParm = mCam.getParameters();
        mCameraWidth = actualParm.getPreviewSize().width;
        mCameraHeight = actualParm.getPreviewSize().height;
        Log.i(TAG, "[WARNING] vcap: focus mode " + actualParm.getFocusMode());

        createPool();

        int result;
        if (mCamInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCamInfo.orientation + mCameraRotation) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (mCamInfo.orientation - mCameraRotation + 360) % 360;
        }
        mCam.setDisplayOrientation(result);

        return 0;
    }

    private void createPool() {
        mFrameSize = mCameraWidth * mCameraHeight * 3 / 2;
        for (int i = 0; i < NUMBER_OF_CAPTURE_BUFFERS; ++i) {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(mFrameSize);
            mCam.addCallbackBuffer(buffer.array());
        }
    }

    private int startCamOnCameraThread() {
        Log.e(TAG, "startCamOnCameraThread");
        checkIsOnCameraThread();
        if (!isCameraRunning.get() || mCam == null) {
            Log.e(TAG, "startPreviewOnCameraThread: Camera is stopped");
            return 0;
        }

        if (mTextureView != null) {
            attachTextureView();
        } else if (mSurfaceView != null) {
            attachSurfaceView();
        }

        if (!previewEglBase.hasSurface()) {
            Log.e(TAG, "startCamOnCameraThread ：EGL14.EGL_NO_SURFACE");
            return 0;
        }

        previewEglBase.makeCurrent();

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        mCameraTextureId = GlUtil.generateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        Log.e(TAG, "startCamOnCameraThread ：mCameraTextureId = " + mCameraTextureId);
        try {
            mCam.setPreviewTexture(mCameraSurfaceTexture = new SurfaceTexture(mCameraTextureId));
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCam.setPreviewCallback(VideoCaptureFromFaceunity.this);
        mCam.startPreview();
        return 0;
    }

    private int stopCaptureOnCameraThread(boolean stopHandler) {
        checkIsOnCameraThread();
        Log.e(TAG, "stopCaptureOnCameraThread");

        if (stopHandler) {
            // Clear the cameraThreadHandler first, in case stopPreview or
            // other driver code deadlocks. Deadlock in
            // android.hardware.Camera._stopPreview(Native Method) has
            // been observed on Nexus 5 (hammerhead), OS version LMY48I.
            // The camera might post another one or two preview frames
            // before stopped, so we have to check |isCameraRunning|.
            // Remove all pending Runnables posted from |this|.
            isCameraRunning.set(false);
            mGLHandler.removeCallbacksAndMessages(this /* token */);
        }

        if (mCam != null) {
            mCam.stopPreview();
            mCam.setPreviewCallbackWithBuffer(null);
        }
        return 0;
    }

    private int restartCam() {
        synchronized (pendingCameraRestartLock) {
            if (pendingCameraRestart) {
                // Do not handle multiple camera switch request to avoid blocking
                // camera thread by handling too many switch request from a queue.
                Log.w(TAG, "Ignoring camera switch request.");
                return 0;
            }
            pendingCameraRestart = true;
        }

        final boolean didPost = maybePostOnCameraThread(new Runnable() {
            @Override
            public void run() {
                stopCaptureOnCameraThread(false);
                releaseCam();
                createCamOnCameraThread();
                startCamOnCameraThread();
                faceunity.fuOnCameraChange();
                synchronized (pendingCameraRestartLock) {
                    pendingCameraRestart = false;
                }
            }
        });

        if (!didPost) {
            synchronized (pendingCameraRestartLock) {
                pendingCameraRestart = false;
            }
        }

        return 0;
    }

    private int releaseCam() {
        // * release cam
        if (mCam != null) {
            mCam.release();
            mCam = null;
        }

        // * release cam info
        mCamInfo = null;
        return 0;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        checkIsOnCameraThread();
        if (!isCameraRunning.get()) {
            Log.e(TAG, "onPreviewFrame: Camera is stopped");
            return;
        }

        mCameraNV21Byte = data;

        if (mIsRunning && mIsPreview && previewEglBase.hasSurface()) {
            draw();
        }

    }

    @Override
    public void onEffectItemSelected(String effectItemName) {
        if (effectItemName.equals(mEffectFileName)) {
            return;
        }
        mCreateItemHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
        mEffectFileName = effectItemName;
        isNeedEffectItem = true;
    }

    @Override
    public void onFilterSelected(String filterName) {
        mFilterName = filterName;
    }

    @Override
    public void onBlurLevelSelected(int level) {
        switch (level) {
            case 0:
                mFacebeautyBlurLevel = 0;
                break;
            case 1:
                mFacebeautyBlurLevel = 1.0f;
                break;
            case 2:
                mFacebeautyBlurLevel = 2.0f;
                break;
            case 3:
                mFacebeautyBlurLevel = 3.0f;
                break;
            case 4:
                mFacebeautyBlurLevel = 4.0f;
                break;
            case 5:
                mFacebeautyBlurLevel = 5.0f;
                break;
            case 6:
                mFacebeautyBlurLevel = 6.0f;
                break;
        }
    }

    @Override
    public void onColorLevelSelected(int progress, int max) {
        mFacebeautyColorLevel = 1.0f * progress / max;
    }

    @Override
    public void onCheekThinSelected(int progress, int max) {
        mFacebeautyCheeckThin = 1.0f * progress / max;
    }

    @Override
    public void onEnlargeEyeSelected(int progress, int max) {
        mFacebeautyEnlargeEye = 1.0f * progress / max;
    }

    @Override
    public void onFaceShapeSelected(int faceShape) {
        mFaceShape = faceShape;
    }

    @Override
    public void onFaceShapeLevelSelected(int progress, int max) {
        mFaceShapeLevel = (1.0f * progress) / max;
    }

    @Override
    public void onRedLevelSelected(int progress, int max) {
        mFacebeautyRedLevel = 1.0f * progress / max;
    }

    class CreateItemHandler extends Handler {

        static final int HANDLE_CREATE_ITEM = 1;

        CreateItemHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLE_CREATE_ITEM:
                    try {
                        if (mEffectFileName.equals("none")) {
                            itemsArray[1] = mEffectItem = 0;
                        } else {
                            InputStream is = mContext.getAssets().open(mEffectFileName);
                            byte[] itemData = new byte[is.available()];
                            int len = is.read(itemData);
                            Log.e("FU", "effect len " + len);
                            is.close();
                            int tmp = itemsArray[1];
                            itemsArray[1] = mEffectItem = faceunity.fuCreateItemFromPackage(itemData);
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
}
