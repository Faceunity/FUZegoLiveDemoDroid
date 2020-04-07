package com.zego.livedemo5.videofilter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.faceunity.nama.FURenderer;
import com.faceunity.nama.OnFaceUnityControlListener;
import com.zego.livedemo5.videocapture.ve_gl.EglBase;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * @author Richie on 2020.03.23
 */
public class FUVideoFilterHybridDemo extends ZegoVideoFilter {
    private static final String TAG = "FUVideoFilterHybridDemo";
    private ZegoVideoFilter.Client mClient = null;
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
    private EglBase captureEglBase;

    private FURenderer mFURenderer;
    private byte[] mRgbaBuffer;

    public FUVideoFilterHybridDemo(Context context) {
        mFURenderer = new FURenderer.Builder(context)
                .setInputTextureType(FURenderer.INPUT_2D_TEXTURE)
                .setCameraType(Camera.CameraInfo.CAMERA_FACING_FRONT)
                .setInputImageOrientation(FURenderer.getCameraOrientation(Camera.CameraInfo.CAMERA_FACING_FRONT))
                .build();
    }

    @Override
    protected void allocateAndStart(ZegoVideoFilter.Client client) {
        Log.d(TAG, "allocateAndStart: thread:" + Thread.currentThread().getName());
        mClient = client;
        mThread = new HandlerThread("video-filter");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                captureEglBase = EglBase.create(null, EglBase.CONFIG_PIXEL_BUFFER);
                try {
                    captureEglBase.createDummyPbufferSurface();
                    captureEglBase.makeCurrent();
                } catch (RuntimeException e) {
                    // Clean up before rethrowing the exception.
                    captureEglBase.releaseSurface();
                    throw e;
                }

                // call on GLThread
                mFURenderer.onSurfaceCreated();

                barrier.countDown();
            }
        });
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mProduceQueue.clear();
        mConsumeQueue.clear();
        mWriteIndex = 0;
        mWriteRemain = 0;
        mMaxBufferSize = 0;
    }

    @Override
    protected void stopAndDeAllocate() {
        Log.d(TAG, "stopAndDeAllocate: thread:" + Thread.currentThread().getName());
        final CountDownLatch barrier = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mClient.destroy();
                mClient = null;
                // call on GLThread
                mFURenderer.onSurfaceDestroyed();
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
    }

    @Override
    protected int supportBufferType() {
        return BUFFER_TYPE_HYBRID_MEM_GL_TEXTURE_2D;
    }

    @Override
    protected synchronized int dequeueInputBuffer(int width, int height, int stride) {
        if (stride * height > mMaxBufferSize) {
            if (mMaxBufferSize != 0) {
                mProduceQueue.clear();
            }

            mMaxBufferSize = stride * height;
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

    private long mSumCost;
    private int mSumFrame;

    @Override
    protected synchronized void queueInputBuffer(int bufferIndex, final int width, final int height, int stride, long timestamp_100n) {
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
        mWriteIndex++;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                PixelBuffer pixelBuffer = getConsumerPixelBuffer();

                if (pixelBuffer == null) {
                    return;
                }

                pixelBuffer.buffer.position(0);
                if (mRgbaBuffer == null) {
                    mRgbaBuffer = new byte[pixelBuffer.buffer.limit()];
                }
                pixelBuffer.buffer.get(mRgbaBuffer);

                captureEglBase.makeCurrent();

                long start = System.currentTimeMillis();
                int fuTexId = mFURenderer.onDrawFrameSingleInput(mRgbaBuffer, pixelBuffer.width, pixelBuffer.height, FURenderer.INPUT_FORMAT_RGBA);
                long cost = System.currentTimeMillis() - start;
                mSumCost += cost;
                if (++mSumFrame % 100 == 0) {
                    int avgCost = (int) (mSumCost / mSumFrame);
                    mSumFrame = 0;
                    mSumCost = 0;
                    Log.d(TAG, "onDrawFrameSingleInput: 100 frame avg cost " + avgCost + "ms");
                }

                mClient.onProcessCallback(fuTexId, pixelBuffer.width, pixelBuffer.height, pixelBuffer.timestamp_100n);
                captureEglBase.detachCurrent();

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

    private void release() {
        if (captureEglBase.hasSurface()) {
            captureEglBase.makeCurrent();
        }

        captureEglBase.release();
        captureEglBase = null;
    }

    public OnFaceUnityControlListener getFaceunityController() {
        return mFURenderer;
    }

}
