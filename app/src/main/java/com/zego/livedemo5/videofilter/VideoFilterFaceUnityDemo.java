package com.zego.livedemo5.videofilter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.faceunity.wrapper.faceunity;
import com.zego.livedemo5.faceunity.EffectAndFilterSelectAdapter;
import com.zego.livedemo5.faceunity.FaceunityController;
import com.zego.livedemo5.faceunity.authpack;
import com.zego.zegoliveroom.videofilter.ZegoVideoFilter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created by robotding on 17/5/3.
 */

public class VideoFilterFaceUnityDemo extends ZegoVideoFilter implements FaceunityController {
    private static final String TAG = "VideoFilterMemDemo";

    private Context mContext;

    private Client mZegoClient = null;
    private HandlerThread mGlThread = null;
    private volatile Handler mGlHandler = null;

    static class PixelBuffer {
        public int width;
        public int height;
        public int stride;
        public long timestamp_100n;
        public ByteBuffer buffer;
    }

    private ArrayList<PixelBuffer> mProduceQueue = new ArrayList<PixelBuffer>();
    private int mWriteIndex = 0;
    private int mWriteRemain = 0;
    private ConcurrentLinkedQueue<PixelBuffer> mConsumeQueue = new ConcurrentLinkedQueue<PixelBuffer>();
    private int mMaxBufferSize = 0;

    private boolean mIsRunning = false;

    private int faceTrackingStatus = 0;

    private int mFacebeautyItem = 0;
    private int mEffectItem = 0;
    private int[] itemsArray = {mFacebeautyItem, mEffectItem};

    private float mFacebeautyColorLevel = 0.2f;
    private float mFacebeautyBlurLevel = 6.0f;
    private float mFacebeautyCheeckThin = 1.0f;
    private float mFacebeautyEnlargeEye = 0.5f;
    private float mFacebeautyRedLevel = 0.5f;
    private int mFaceShape = 3;
    private float mFaceShapeLevel = 0.5f;
    private String mFilterName = EffectAndFilterSelectAdapter.FILTERS_NAME[0];

    private int mFrameId = 0;

    private boolean isNeedEffectItem = true;
    private String mEffectFileName = EffectAndFilterSelectAdapter.EFFECT_ITEM_FILE_NAME[1];
    private HandlerThread mEffectThread = null;
    private volatile Handler mEffectHandler = null;

    public VideoFilterFaceUnityDemo(Context context) {
        mContext = context;
    }

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
//                faceunity.fuSetMaxFaces(3);
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

    @Override
    protected int supportBufferType() {
        return BUFFER_TYPE_ASYNC_I420_MEM;
    }

    @Override
    protected synchronized int dequeueInputBuffer(int width, int height, int stride) {
        if (stride * height * 3 / 2 > mMaxBufferSize) {
            if (mMaxBufferSize != 0) {
                mProduceQueue.clear();
            }

            mMaxBufferSize = stride * height * 3 / 2;
            createPixelBufferPool(3);
        }

        if (mWriteRemain == 0) {
            return -1;
        }

        mWriteRemain--;
        return (mWriteIndex + 1) % mProduceQueue.size();
    }

    @Override
    protected synchronized ByteBuffer getInputBuffer(int index) {
        if (mProduceQueue.isEmpty()) {
            return null;
        }
        ByteBuffer buffer = mProduceQueue.get(index).buffer;
        buffer.position(0);
        return buffer;
    }

    @Override
    protected synchronized void queueInputBuffer(int bufferIndex, final int width, int height, int stride, long timestamp_100n) {
        Log.e(TAG, "queueInputBuffer width = " + width + " height = " + height);
        if (bufferIndex == -1) {
            return;
        }

        PixelBuffer pixelBuffer = mProduceQueue.get(bufferIndex);
        pixelBuffer.width = width;
        pixelBuffer.height = height;
        pixelBuffer.stride = stride;
        pixelBuffer.timestamp_100n = timestamp_100n;
        pixelBuffer.buffer.limit(height * stride * 3 / 2);
        mConsumeQueue.add(pixelBuffer);

        mWriteIndex = (mWriteIndex + 1) % mProduceQueue.size();

        mGlHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mIsRunning) {
                    Log.e(TAG, "already stopped");
                    return;
                }

                PixelBuffer pixelBuffer = getConsumerPixelBuffer();

                int index = mZegoClient.dequeueInputBuffer(pixelBuffer.width, pixelBuffer.height, pixelBuffer.stride);
                if (index >= 0) {
                    ByteBuffer dst = mZegoClient.getInputBuffer(index);
                    dst.position(0);
                    pixelBuffer.buffer.position(0);

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
                        mEffectHandler.sendEmptyMessage(CreateItemHandler.HANDLE_CREATE_ITEM);
                    }

                    faceunity.fuItemSetParam(mFacebeautyItem, "color_level", mFacebeautyColorLevel);
                    faceunity.fuItemSetParam(mFacebeautyItem, "blur_level", mFacebeautyBlurLevel);
                    faceunity.fuItemSetParam(mFacebeautyItem, "filter_name", mFilterName);
                    faceunity.fuItemSetParam(mFacebeautyItem, "cheek_thinning", mFacebeautyCheeckThin);
                    faceunity.fuItemSetParam(mFacebeautyItem, "eye_enlarging", mFacebeautyEnlargeEye);
                    faceunity.fuItemSetParam(mFacebeautyItem, "face_shape", mFaceShape);
                    faceunity.fuItemSetParam(mFacebeautyItem, "face_shape_level", mFaceShapeLevel);
                    faceunity.fuItemSetParam(mFacebeautyItem, "red_level", mFacebeautyRedLevel);

                    int fuTex = faceunity.fuRenderToI420Image(pixelBuffer.buffer.array(),
                            pixelBuffer.width, pixelBuffer.height, mFrameId++, itemsArray);

                    dst.put(pixelBuffer.buffer);

                    mZegoClient.queueInputBuffer(index, pixelBuffer.width, pixelBuffer.height, pixelBuffer.stride, pixelBuffer.timestamp_100n);
                }

                returnProducerPixelBuffer(pixelBuffer);
            }

        });
    }

    @Override
    protected SurfaceTexture getSurfaceTexture() {
        return null;
    }

    @Override
    protected void onProcessCallback(int textureId, int width, int height, long timestamp_100n) {

    }

    private void createPixelBufferPool(int count) {
        for (int i = 0; i < count; i++) {
            PixelBuffer pixelBuffer = new PixelBuffer();
            pixelBuffer.buffer = ByteBuffer.allocateDirect(mMaxBufferSize);
            mProduceQueue.add(pixelBuffer);
        }

        mWriteRemain = count;
        mWriteIndex = -1;
    }

    private PixelBuffer getConsumerPixelBuffer() {
        if (mConsumeQueue.isEmpty()) {
            return null;
        }
        return mConsumeQueue.poll();
    }

    private synchronized void returnProducerPixelBuffer(PixelBuffer pixelBuffer) {
        if (pixelBuffer.buffer.capacity() == mMaxBufferSize) {
            mWriteRemain++;
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
                            is.read(itemData);
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

    @Override
    public void onEffectItemSelected(String effectItemName) {
        if (effectItemName.equals(mEffectFileName)) {
            return;
        }
        mEffectHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
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
