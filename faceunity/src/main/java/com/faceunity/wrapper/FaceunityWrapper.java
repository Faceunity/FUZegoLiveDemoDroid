package com.faceunity.wrapper;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.faceunity.wrapper.gles.FullFrameRect;
import com.faceunity.wrapper.gles.Texture2dProgram;

import java.io.IOException;
import java.io.InputStream;


public class FaceunityWrapper {
    private static final String TAG = FaceunityWrapper.class.getSimpleName();

    private Context mContext;

    public static boolean isInit = false;

    private byte[] mCameraNV21Byte;
    private byte[] fuImgNV21Bytes;

    private int mFrameId = 0;

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

    private String mFilterName = EffectAndFilterSelectAdapter.FILTERS_NAME[0];

    private boolean isNeedEffectItem = true;
    private String mEffectFileName = EffectAndFilterSelectAdapter.EFFECT_ITEM_FILE_NAME[1];

    private int mCurrentCameraId;

    private HandlerThread mCreateItemThread;
    private Handler mCreateItemHandler;

    private int faceTrackingStatus = 0;

    private long lastOneHundredFrameTimeStamp = 0;
    private int currentFrameCnt = 0;
    private long oneHundredFrameFUTime = 0;

    private boolean isBenchmarkFPS = true;
    private boolean isBenchmarkTime = false;

    private FullFrameRect mFullScreenFUDisplay;

    public FaceunityWrapper(Context context, int cameraFaceId) {
        Log.e(TAG, "FaceunityWrapper = " + Thread.currentThread().getId());
        mContext = context;
        mCurrentCameraId = cameraFaceId;
    }

    public void onSurfaceCreated(Context context) {
        Log.e(TAG, "onSurfaceCreated " + Thread.currentThread().getId());
        mCreateItemThread = new HandlerThread("faceunity-efect");
        mCreateItemThread.start();
        mCreateItemHandler = new CreateItemHandler(mCreateItemThread.getLooper());

        mFullScreenFUDisplay = new FullFrameRect(new Texture2dProgram(
                Texture2dProgram.ProgramType.TEXTURE_2D));

        try {
            InputStream is = context.getAssets().open("v3.mp3");
            byte[] v3data = new byte[is.available()];
            int len = is.read(v3data);
            is.close();
            faceunity.fuSetup(v3data, null, authpack.A());
            //faceunity.fuSetMaxFaces(1);
            Log.e(TAG, "fuSetup version " + faceunity.fuGetVersion());
            Log.e(TAG, "fuSetup v3 len " + len);

            is = context.getAssets().open("face_beautification.mp3");
            byte[] itemData = new byte[is.available()];
            len = is.read(itemData);
            Log.e(TAG, "beautification len " + len);
            is.close();
            mFacebeautyItem = faceunity.fuCreateItemFromPackage(itemData);
            itemsArray[0] = mFacebeautyItem;

        } catch (IOException e) {
            e.printStackTrace();
        }
        isInit = true;
    }

    public void onSurfaceChanged(int width, int height, int previewWidth, int previewHeight) {
        Log.e(TAG, "onSurfaceChanged width " + width + " height " + height + " previewWidth " + previewWidth + " previewHeight " + previewHeight);

    }

    public void onSurfaceDestroyed() {
        Log.e(TAG, "onSurfaceDestroyed " + Thread.currentThread().getId());
        mFrameId = 0;

        if (mCreateItemHandler != null) {
            mCreateItemHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
            mCreateItemHandler = null;
            mCreateItemThread.quitSafely();
            mCreateItemThread = null;
        }

        //Note: 切忌使用一个已经destroy的item
        itemsArray[1] = mEffectItem = 0;
        itemsArray[0] = mFacebeautyItem = 0;
        faceunity.fuDestroyAllItems();
        faceunity.fuOnDeviceLost();
        isNeedEffectItem = true;

        lastOneHundredFrameTimeStamp = 0;
        oneHundredFrameFUTime = 0;

        isInit = false;
    }

    public void switchCamera(int ordinal) {
        mCurrentCameraId = ordinal;
        if (isInit) {
            Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
            Camera.getCameraInfo(mCurrentCameraId, info);
            faceunity.fuItemSetParam(mEffectItem, "rotationAngle", (360 - info.orientation));
        }
    }

    /**
     * DualInputToTexture 对纹理进行特效处理
     *
     * @param texId     YUV格式纹理
     * @param texWidth  纹理宽度
     * @param texHeight 纹理高度
     * @return 特效处理后的纹理
     */
    public int onDrawFrameDualInputToTexture(int texId, int texWidth, int texHeight) {
        if (!isInit) {
            return texId;
        }
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

        if (mCameraNV21Byte == null || mCameraNV21Byte.length == 0) {
            Log.e(TAG, "camera nv21 bytes null");
            return texId;
        }

        final int isTracking = faceunity.fuIsTracking();
        if (isTracking != faceTrackingStatus) {
            faceTrackingStatus = isTracking;
            if (faceTrackingStatus == 0) {
                Toast.makeText(mContext, "人脸识别失败！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "人脸识别成功！", Toast.LENGTH_SHORT).show();
            }
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
        flags |= mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ? 0 : faceunity.FU_ADM_FLAG_FLIP_X;

        long fuStartTime = System.nanoTime();
        /**
         * 这里拿到fu处理过后的texture，可以对这个texture做后续操作，如硬编、预览。
         */
        int fuTex = faceunity.fuDualInputToTexture(fuImgNV21Bytes, texId, flags,
                texWidth, texHeight, mFrameId++, itemsArray);
        long fuEndTime = System.nanoTime();
        oneHundredFrameFUTime += fuEndTime - fuStartTime;

        return fuTex;
    }

    /**
     * RenderToTexture 对纹理进行特效处理
     *
     * @param texId     YUV格式纹理
     * @param texWidth  纹理宽度
     * @param texHeight 纹理高度
     * @return 特效处理后的纹理
     */
    public int onDrawFrameRenderToTexture(int texId, int texWidth, int texHeight) {
        if (!isInit) {
            return texId;
        }
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

        final int isTracking = faceunity.fuIsTracking();
        if (isTracking != faceTrackingStatus) {
            faceTrackingStatus = isTracking;
            if (faceTrackingStatus == 0) {
                Toast.makeText(mContext, "人脸识别失败！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "人脸识别成功！", Toast.LENGTH_SHORT).show();
            }
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

        faceunity.fuItemSetParam(mEffectItem, "rotationAngle", 0);
        //faceunity.fuItemSetParam(mFacebeautyItem, "use_old_blur", 1);

        boolean isOESTexture = false; //camera默认的是OES的
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
        flags |= mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ? 0 : faceunity.FU_ADM_FLAG_FLIP_X;

        long fuStartTime = System.nanoTime();
        /**
         * 这里拿到fu处理过后的texture，可以对这个texture做后续操作，如硬编、预览。
         */
        int fuTex = faceunity.fuRenderToTexture(texId,
                texWidth, texHeight, mFrameId++, itemsArray, flags);
        long fuEndTime = System.nanoTime();
        oneHundredFrameFUTime += fuEndTime - fuStartTime;

        return fuTex;
    }

    public boolean onPreviewFrame(byte[] data, int width, int height) {

        mCameraNV21Byte = data;

        return false;
    }

    public FaceunityControlView.OnViewEventListener initUIEventListener() {

        FaceunityControlView.OnViewEventListener eventListener = new FaceunityControlView.OnViewEventListener() {

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
            public void onCheekThinSelected(int progress, int max) {
                mFacebeautyCheeckThin = 1.0f * progress / max;
            }

            @Override
            public void onColorLevelSelected(int progress, int max) {
                mFacebeautyColorLevel = 1.0f * progress / max;
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
            public void onEnlargeEyeSelected(int progress, int max) {
                mFacebeautyEnlargeEye = 1.0f * progress / max;
            }

            @Override
            public void onFilterSelected(String filterName) {
                mFilterName = filterName;
            }

            @Override
            public void onRedLevelSelected(int progress, int max) {
                mFacebeautyRedLevel = 1.0f * progress / max;
            }

            @Override
            public void onFaceShapeLevelSelected(int progress, int max) {
                mFaceShapeLevel = (1.0f * progress) / max;
            }

            @Override
            public void onFaceShapeSelected(int faceShape) {
                mFaceShape = faceShape;
            }
        };

        return eventListener;
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
                            Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
                            Camera.getCameraInfo(mCurrentCameraId, info);
                            faceunity.fuItemSetParam(mEffectItem, "rotationAngle",
                                    (360 - info.orientation));
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

    public int getFaceTrackingStatus() {
        return faceTrackingStatus;
    }

}
