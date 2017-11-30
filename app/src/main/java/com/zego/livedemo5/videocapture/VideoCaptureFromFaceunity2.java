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
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.faceunity.wrapper.faceunity;
import com.zego.livedemo5.faceunity.CameraUtils;
import com.zego.livedemo5.faceunity.EffectAndFilterSelectAdapter;
import com.zego.livedemo5.faceunity.FaceunityController;
import com.zego.livedemo5.faceunity.MiscUtil;
import com.zego.livedemo5.faceunity.authpack;
import com.zego.livedemo5.videocapture.ve_gl.EglBase;
import com.zego.livedemo5.videocapture.ve_gl.GlRectDrawer;
import com.zego.livedemo5.videocapture.ve_gl.GlUtil;
import com.zego.zegoliveroom.videocapture.ZegoVideoCaptureDevice;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

/**
 * Created by robotding on 17/2/15.
 */

public class VideoCaptureFromFaceunity2 extends ZegoVideoCaptureDevice implements Camera.PreviewCallback, FaceunityController {

    private static final String TAG = VideoCaptureFromFaceunity2.class.getName();

    private Context mContext;

    private boolean mIsRunning = false;
    private boolean mIsPreview = false;
    private boolean mIsCapture = false;

    private boolean isNeedUpdateShowView = true;
    private View mShowView;
    private int mFrameRate;
    private int mResolutionWidth = 1280;
    private int mResolutionHeight = 720;
    private int mFrontCam = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private int mCaptureRotation;

    private Client mClient;

    private HandlerThread mGLThread = null;
    private Handler mGLHandler = null;

    private EglBase previewEglBase;
    private GlRectDrawer previewDrawer;
    private static final float[] transformationMatrix = new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 1.0f};

    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private int mViewWidth = 0;
    private int mViewHeight = 0;

    private int mPreviewTextureId;
    private int mFrameBufferId;

    private int mCameraTextureId;
    private SurfaceTexture mCameraSurfaceTexture;

    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private byte[] mCameraNV21Byte;
    private byte[] fuImgNV21Bytes;

    private int mFrameId;

    private int mFacebeautyItem = 0; //美颜道具
    private int mEffectItem = 0; //贴纸道具
    private int[] itemsArray = {mFacebeautyItem, mEffectItem};

    private float mFacebeautyColorLevel = 0.2f;
    private float mFacebeautyBlurLevel = 6.0f;
    private float mFacebeautyCheeckThin = 1.0f;
    private float mFacebeautyEnlargeEye = 0.5f;
    private float mFacebeautyRedLevel = 0.5f;
    private int mFaceShape = 3;
    private float mFaceShapeLevel = 0.5f;

    private String mFilterName = EffectAndFilterSelectAdapter.FILTERS_NAME[3];

    private boolean isNeedEffectItem = true;
    private String mEffectFileName = EffectAndFilterSelectAdapter.EFFECT_ITEM_FILE_NAME[1];

    private HandlerThread mCreateItemThread;
    private CreateItemHandler mCreateItemHandler;

    private int faceTrackingStatus = 0;

    private long lastOneHundredFrameTimeStamp = 0;
    private int currentFrameCnt = 0;
    private long oneHundredFrameFUTime = 0;

    private boolean isBenchmarkFPS = true;
    private boolean isBenchmarkTime = true;

    public VideoCaptureFromFaceunity2(Context context) {
        mContext = context;
    }

    @Override
    protected int setView(View view) {
        Log.e(TAG, "setView " + view);
        mShowView = view;
        isNeedUpdateShowView = true;
        return 0;
    }

    private void attachTextureView() {
        if (mShowView instanceof TextureView) {
            TextureView textureView = (TextureView) mShowView;
            if (!textureView.isAvailable()) {
                return;
            }
            mViewWidth = textureView.getWidth();
            mViewHeight = textureView.getHeight();
            previewEglBase.createSurface(textureView.getSurfaceTexture());
        } else if (mShowView instanceof SurfaceView) {
            SurfaceView surfaceView = (SurfaceView) mShowView;
            SurfaceHolder holder = surfaceView.getHolder();
            Surface surface = holder.getSurface();
            if (surface == null) {
                return;
            }
            Rect size = holder.getSurfaceFrame();
            mViewWidth = size.width();
            mViewHeight = size.height();
            previewEglBase.createSurface(surface);
        }
    }

    @Override
    protected void allocateAndStart(Client client) {
        Log.e(TAG, "allocateAndStart");
        mClient = client;

        mGLThread = new HandlerThread(TAG);
        mGLThread.start();
        mGLHandler = new Handler(mGLThread.getLooper());

        mCreateItemThread = new HandlerThread(TAG);
        mCreateItemThread.start();
        mCreateItemHandler = new CreateItemHandler(mCreateItemThread.getLooper());

        final CountDownLatch barrier = new CountDownLatch(1);
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "EglBase.create");
                previewEglBase = EglBase.create(null, EglBase.CONFIG_RGBA);
                previewDrawer = new GlRectDrawer();

                if (isNeedUpdateShowView) {
                    attachTextureView();
                    isNeedUpdateShowView = false;
                }

                previewEglBase.makeCurrent();
                mCameraTextureId = GlUtil.generateTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
                mCameraSurfaceTexture = new SurfaceTexture(mCameraTextureId);

                barrier.countDown();

                try {
                    InputStream is = mContext.getAssets().open("v3.mp3");
                    byte[] v3data = new byte[is.available()];
                    int len = is.read(v3data);
                    is.close();
                    faceunity.fuSetup(v3data, null, authpack.A());
                    //faceunity.fuSetMaxFaces(1);
                    Log.e(TAG, "fuSetup v3 len " + len);
                    Log.e(TAG, "fuSetup version " + faceunity.fuGetVersion());

                    is = mContext.getAssets().open("face_beautification.mp3");
                    byte[] itemData = new byte[is.available()];
                    len = is.read(itemData);
                    Log.e(TAG, "beautification len " + len);
                    is.close();
                    itemsArray[0] = mFacebeautyItem = faceunity.fuCreateItemFromPackage(itemData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mIsRunning = true;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!mIsRunning || !mIsPreview) {
            Log.e(TAG, "onPreviewFrame mIsRunning " + mIsRunning + " mIsPreview " + mIsPreview);
            return;
        }
        if (data == null || data.length == 0) {
            Log.e(TAG, "camera nv21 bytes null");
            return;
        }
        mCameraNV21Byte = data;

        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isNeedUpdateShowView) {
                        previewEglBase.releaseSurface();
                        attachTextureView();
                        isNeedUpdateShowView = false;
                    }
                    if (!previewEglBase.hasSurface()) {
                        Log.e(TAG, "onPreviewFrame No EGLSurface");
                        stopPreview();
                        return;
                    }
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

                        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mResolutionWidth, mResolutionHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                        GlUtil.checkGlError("glTexImage2D");

                        mFrameBufferId = GlUtil.generateFrameBuffer(mPreviewTextureId);
                        GlUtil.checkGlError("generateFrameBuffer");
                    }

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
                    GlUtil.checkGlError("glBindFramebuffer");

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

                    long fuStartTime = System.nanoTime();
                    int fuTex = faceunity.fuDualInputToTexture(fuImgNV21Bytes, mCameraTextureId, flags,
                            mResolutionWidth, mResolutionHeight, mFrameId++, itemsArray);
                    long fuEndTime = System.nanoTime();
                    oneHundredFrameFUTime += fuEndTime - fuStartTime;

                    GLES20.glFinish();

                    float[] matrix = new float[16];
                    Matrix.multiplyMM(matrix, 0, mtx, 0, transformationMatrix, 0);

                    float ratio = (float) mViewHeight * mResolutionHeight / (mViewWidth * mResolutionWidth);
                    Matrix.frustumM(mProjectionMatrix, 0, -1f, 1f, -ratio, ratio, 3f, 10f);
                    Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 3.01f, 0f, 0f, 0f, 0f, 1f, 0f);
                    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

                    previewDrawer.drawRgb(fuTex, matrix, mMVPMatrix,
                            mViewWidth, mViewHeight,
                            0, 0,
                            mResolutionWidth, mResolutionHeight);
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
                        mClient.onTextureCaptured(mPreviewTextureId, mResolutionWidth, mResolutionHeight, now);
                    }

                    previewEglBase.detachCurrent();

//                    Log.e(TAG, "onPreviewFrame mPreviewTextureId " + mPreviewTextureId + " mCameraTextureId " + mCameraTextureId + " fuTex " + fuTex);
//                    Log.e(TAG, "onPreviewFrame mViewWidth " + mViewWidth + " mViewHeight " + mViewHeight + " mResolutionWidth " + mResolutionWidth + " mResolutionHeight " + mResolutionHeight);

                    if (++currentFrameCnt == 100) {
                        currentFrameCnt = 0;
                        long tmp = System.nanoTime();
                        if (isBenchmarkFPS)
                            Log.e(TAG, "dualInput FPS : " + (1000.0f * MiscUtil.NANO_IN_ONE_MILLI_SECOND / ((tmp - lastOneHundredFrameTimeStamp) / 100.0f)));
                        lastOneHundredFrameTimeStamp = tmp;
                        if (isBenchmarkTime)
                            Log.e(TAG, "dualInput cost time avg : " + oneHundredFrameFUTime / 100.f / MiscUtil.NANO_IN_ONE_MILLI_SECOND);
                        oneHundredFrameFUTime = 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void stopAndDeAllocate() {
        Log.e(TAG, "stopAndDeAllocate");
        mIsRunning = false;

        final CountDownLatch barrier = new CountDownLatch(1);
        mGLHandler.post(new Runnable() {
            @Override
            public void run() {
                if (previewEglBase != null && previewEglBase.hasSurface()) {
//                    previewEglBase.makeCurrent();

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

                    isNeedUpdateShowView = true;
                    previewEglBase.releaseSurface();
                    previewEglBase.detachCurrent();
                    previewEglBase = null;
                    mShowView = null;

                    mFrameId = 0;

                    //Note: 切忌使用一个已经destroy的item
                    itemsArray[1] = mEffectItem = 0;
                    itemsArray[0] = mFacebeautyItem = 0;
                    faceunity.fuDestroyAllItems();
                    faceunity.fuOnDeviceLost();
                    isNeedEffectItem = true;
                }
                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mGLHandler = null;
        mCreateItemHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
        mCreateItemHandler = null;
        if (Build.VERSION.SDK_INT >= 18) {
            mGLThread.quitSafely();
            mCreateItemThread.quitSafely();
        } else {
            mGLThread.quit();
            mCreateItemThread.quit();
        }
        mGLThread = null;
        mCreateItemThread = null;

        mClient.destroy();
        mClient = null;
    }


    @Override
    protected int setFrameRate(int i) {
        Log.e(TAG, "setFrameRate");

        if (mCamera != null) {
            mCamera.stopPreview();
            mFrameRate = i;
            setCameraParameters();
            startCamera();
        } else {
            mFrameRate = i;
        }
        return 0;
    }

    @Override
    protected int setResolution(int i, int i1) {
        Log.e(TAG, "setResolution mResolutionWidth " + i + " mResolutionHeight " + i1);

        if (mCamera != null) {
            mCamera.stopPreview();
            setCameraParameters();
            startCamera();
        } else {
            mResolutionWidth = i;
            mResolutionHeight = i1;
        }
        return 0;
    }

    @Override
    protected int setFrontCam(int i) {
        Log.e(TAG, "setFrontCam " + i);
        mFrontCam = i;

        if (mCamera != null) {
            releaseCamera();
            openCamera();
            mGLHandler.post(new Runnable() {
                @Override
                public void run() {
                    faceunity.fuOnCameraChange();
                    mFrameId = 0;
                }
            });
        }
        return 0;
    }

    @Override
    protected int setCaptureRotation(int i) {
        Log.e(TAG, "setCaptureRotation");

        if (mCamera != null) {
            mCamera.stopPreview();
            mCaptureRotation = i;
            setCameraParameters();
            startCamera();
        } else {
            mCaptureRotation = i;
        }
        return 0;
    }

    @Override
    protected int supportBufferType() {
        return PIXEL_BUFFER_TYPE_GL_TEXTURE_2D;
    }

    @Override
    protected int startCapture() {
        Log.e(TAG, "startCapture");
        mIsCapture = true;
        return startPreview();
    }

    @Override
    protected int stopCapture() {
        Log.e(TAG, "stopCapture");
        mIsCapture = false;
        return stopPreview();
    }

    @Override
    protected int startPreview() {
        Log.e(TAG, "startPreview");
        mIsPreview = true;
        if (mCamera == null) {
            openCamera();
        }
        return 0;
    }

    @Override
    protected int stopPreview() {
        Log.e(TAG, "stopPreview");
        mIsPreview = false;
        if (mCamera != null) {
            releaseCamera();
            mFrameId = 0;
        }
        return 0;
    }

    @Override
    protected int setViewMode(int i) {
        return 0;
    }

    @Override
    protected int setViewRotation(int i) {
        return 0;
    }

    @Override
    protected int enableTorch(boolean b) {
        return 0;
    }

    @Override
    protected int takeSnapshot() {
        return 0;
    }

    @Override
    protected int setPowerlineFreq(int i) {
        return 0;
    }

    private void openCamera() {
        Log.e(TAG, "openCamera");

        Camera.CameraInfo info = new Camera.CameraInfo();
        int cameraId = 0;
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == mFrontCam) {
                cameraId = i;
                mCameraInfo = info;
                mCamera = Camera.open(i);
                break;
            }
        }

        setCameraParameters();

        startCamera();
    }

    private void setCameraParameters() {
        setCameraParameters(mResolutionWidth, mResolutionHeight);
    }

    private void setCameraParameters(int w, int h) {
        Log.e(TAG, "setCameraParameters");
        if (mCamera == null) {
            return;
        }

        int result;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + mCaptureRotation) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (mCameraInfo.orientation - mCaptureRotation + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);

        Camera.Parameters parameters = mCamera.getParameters();

        CameraUtils.setFocusModes(parameters);

        CameraUtils.chooseFramerate(parameters, mFrameRate);

        int size[] = CameraUtils.choosePreviewSize(parameters, w, h);
        mResolutionWidth = size[0];
        mResolutionHeight = size[1];

        mCamera.setParameters(parameters);
    }

    private void startCamera() {
        Log.e(TAG, "startCamera");
        if (mCamera == null) {
            return;
        }
        try {
            mCamera.setPreviewTexture(mCameraSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.setPreviewCallback(VideoCaptureFromFaceunity2.this);
        mCamera.startPreview();
    }

    private void releaseCamera() {
        Log.e(TAG, "release camera");
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewTexture(null);
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
                            faceunity.fuItemSetParam(mEffectItem, "rotationAngle",
                                    mFrontCam == Camera.CameraInfo.CAMERA_FACING_FRONT ? 90 : 270);
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

}
