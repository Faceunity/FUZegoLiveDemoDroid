package com.zego.livedemo5.videofilter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.util.Log;

import com.faceunity.nama.FURenderer;
import com.faceunity.nama.OnFaceUnityControlListener;
import com.zego.livedemo5.videocapture.ve_gl.GlRectDrawer;
import com.zego.livedemo5.videocapture.ve_gl.GlUtil;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilter;

import java.nio.ByteBuffer;

/**
 * Created by robotding on 17/2/23.
 */
public class FUVideoFilterGlTexture2dDemo extends ZegoVideoFilter {
    private static final String TAG = "FUVideoFilterGlTexture2";

    private Client mClient = null;
    private GlRectDrawer mDrawer;
    private int mTextureId = 0;
    private int mFrameBufferId = 0;
    private float[] transformationMatrix = new float[]{1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f};

    private int mWidth = 0;
    private int mHeight = 0;

    private FURenderer mFURenderer;

    public FUVideoFilterGlTexture2dDemo(Context context) {
        mFURenderer = new FURenderer.Builder(context)
                .setInputTextureType(FURenderer.INPUT_2D_TEXTURE)
                .setCameraType(Camera.CameraInfo.CAMERA_FACING_FRONT)
                .setInputImageOrientation(FURenderer.getCameraOrientation(Camera.CameraInfo.CAMERA_FACING_FRONT))
                .build();
    }

    @Override
    protected void allocateAndStart(Client client) {
        Log.d(TAG, "allocateAndStart: thread:" + Thread.currentThread().getName());
        mClient = client;
        mWidth = mHeight = 0;
        if (mDrawer == null) {
            mDrawer = new GlRectDrawer();
        }
        mFURenderer.onSurfaceCreated();
    }

    @Override
    protected void stopAndDeAllocate() {
        Log.d(TAG, "stopAndDeAllocate: thread:" + Thread.currentThread().getName());
        if (mTextureId != 0) {
            int[] textures = new int[]{mTextureId};
            GLES20.glDeleteTextures(1, textures, 0);
            mTextureId = 0;
        }

        if (mFrameBufferId != 0) {
            int[] frameBuffers = new int[]{mFrameBufferId};
            GLES20.glDeleteFramebuffers(1, frameBuffers, 0);
            mFrameBufferId = 0;
        }

        if (mDrawer != null) {
            mDrawer.release();
            mDrawer = null;
        }
        mFURenderer.onSurfaceDestroyed();
        mClient.destroy();
        mClient = null;
    }

    @Override
    protected int supportBufferType() {
        return BUFFER_TYPE_SYNC_GL_TEXTURE_2D;
    }

    @Override
    protected int dequeueInputBuffer(int width, int height, int stride) {
        return 0;
    }

    @Override
    protected ByteBuffer getInputBuffer(int index) {
        return null;
    }

    @Override
    protected void queueInputBuffer(int bufferIndex, int width, int height, int stride, long timestamp_100n) {

    }

    @Override
    protected SurfaceTexture getSurfaceTexture() {
        return null;
    }

    private long mSumCost;
    private int mSumFrame;

    @Override
    protected void onProcessCallback(int textureId, int width, int height, long timestamp_100n) {
        if (mWidth != width || mHeight != height) {
            if (mTextureId != 0) {
                int[] textures = new int[]{mTextureId};
                GLES20.glDeleteTextures(1, textures, 0);
                mTextureId = 0;
            }

            if (mFrameBufferId != 0) {
                int[] frameBuffers = new int[]{mFrameBufferId};
                GLES20.glDeleteFramebuffers(1, frameBuffers, 0);
                mFrameBufferId = 0;
            }

            mWidth = width;
            mHeight = height;
        }

        if (mTextureId == 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            mTextureId = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            mFrameBufferId = GlUtil.generateFrameBuffer(mTextureId);
        } else {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);
        }
        long start = System.currentTimeMillis();
        int texture = mFURenderer.onDrawFrameSingleInput(textureId, width, height);
        long cost = System.currentTimeMillis() - start;
        mSumCost += cost;
        if (++mSumFrame % 100 == 0) {
            int avgCost = (int) (mSumCost / mSumFrame);
            mSumFrame = 0;
            mSumCost = 0;
            Log.d(TAG, "onDrawFrameSingleInput: 100 frame avg cost " + avgCost + "ms");
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mDrawer.drawRgb(texture, transformationMatrix,
                width, height, 0, 0, width, height);

        mClient.onProcessCallback(mTextureId, width, height, timestamp_100n);
    }

    public OnFaceUnityControlListener getFaceunityController() {
        return mFURenderer;
    }
}
