package com.zego.livedemo5.videofilter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.faceunity.nama.FURenderer;
import com.faceunity.nama.OnFaceUnityControlListener;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created by robotding on 16/12/3.
 */

public class FUVideoFilterMemDemo extends ZegoVideoFilter {
    private static final String TAG = "FUVideoFilterMemDemo";

    private Client mClient = null;
    private HandlerThread mThread = null;
    private volatile Handler mHandler = null;

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
    private FURenderer mFURenderer;
    private byte[] mRgbaBuffer;

    public FUVideoFilterMemDemo(Context context) {
        mFURenderer = new FURenderer.Builder(context)
                .setInputTextureType(FURenderer.INPUT_2D_TEXTURE)
                .setCameraType(Camera.CameraInfo.CAMERA_FACING_FRONT)
                .setCreateEGLContext(true)
                .setInputImageOrientation(FURenderer.getCameraOrientation(Camera.CameraInfo.CAMERA_FACING_FRONT))
                .build();
    }

    @Override
    protected void allocateAndStart(Client client) {
        Log.d(TAG, "allocateAndStart: thread: " + Thread.currentThread().getId());
        mClient = client;
        mThread = new HandlerThread("video-filter");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mFURenderer.onSurfaceCreated();
            }
        });
        mIsRunning = true;

        mProduceQueue.clear();
        mConsumeQueue.clear();
        mWriteIndex = 0;
        mWriteRemain = 0;
        mMaxBufferSize = 0;
    }

    @Override
    protected void stopAndDeAllocate() {
        Log.d(TAG, "stopAndDeAllocate: thread: " + Thread.currentThread().getId());
        mIsRunning = false;

        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mFURenderer.onSurfaceDestroyed();
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

        mClient.destroy();
        mClient = null;
    }

    @Override
    protected int supportBufferType() {
        return BUFFER_TYPE_MEM;
    }

    @Override
    protected synchronized int dequeueInputBuffer(int width, int height, int stride) {
        if (stride * height > mMaxBufferSize) {
            if (mMaxBufferSize != 0) {
                mProduceQueue.clear();
            }

            mMaxBufferSize = stride * height;
            createPixelBufferPool(4);
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

    private long mSumCost;
    private int mSumFrame;

    @Override
    protected synchronized void queueInputBuffer(int bufferIndex, final int width, int height, int stride, long timestamp_100n) {
        if (bufferIndex == -1) {
            return;
        }

        PixelBuffer pixelBuffer = mProduceQueue.get(bufferIndex);
        pixelBuffer.width = width;
        pixelBuffer.height = height;
        pixelBuffer.stride = stride;
        pixelBuffer.timestamp_100n = timestamp_100n;
        pixelBuffer.buffer.limit(height * stride);
        mConsumeQueue.add(pixelBuffer);

        mWriteIndex = (mWriteIndex + 1) % mProduceQueue.size();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mIsRunning) {
                    Log.e(TAG, "already stopped");
                    return;
                }

                PixelBuffer pixelBuffer = getConsumerPixelBuffer();

                int index = mClient.dequeueInputBuffer(pixelBuffer.width, pixelBuffer.height, pixelBuffer.stride);
                if (index >= 0) {
                    ByteBuffer dst = mClient.getInputBuffer(index);
                    dst.position(0);
                    pixelBuffer.buffer.position(0);
                    if (mRgbaBuffer == null) {
                        mRgbaBuffer = new byte[pixelBuffer.buffer.limit()];
                    }
                    pixelBuffer.buffer.get(mRgbaBuffer);
                    long start = System.currentTimeMillis();
                    mFURenderer.onDrawFrameSingleInput(mRgbaBuffer, pixelBuffer.width, pixelBuffer.height, FURenderer.INPUT_FORMAT_RGBA);
                    long cost = System.currentTimeMillis() - start;
                    mSumCost += cost;
                    if (++mSumFrame % 100 == 0) {
                        int avgCost = (int) (mSumCost / mSumFrame);
                        mSumFrame = 0;
                        mSumCost = 0;
                        Log.d(TAG, "onDrawFrameSingleInput: 100 frame avg cost " + avgCost + "ms");
                    }
                    dst.put(mRgbaBuffer);

                    mClient.queueInputBuffer(index, pixelBuffer.width, pixelBuffer.height, pixelBuffer.stride, pixelBuffer.timestamp_100n);
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

    public OnFaceUnityControlListener getFaceunityController() {
        return mFURenderer;
    }
}
