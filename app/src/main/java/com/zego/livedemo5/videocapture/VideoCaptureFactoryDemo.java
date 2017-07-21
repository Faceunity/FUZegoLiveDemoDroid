package com.zego.livedemo5.videocapture;

import android.content.Context;

import com.zego.livedemo5.faceunity.FaceunityController;
import com.zego.zegoliveroom.videocapture.ZegoVideoCaptureDevice;
import com.zego.zegoliveroom.videocapture.ZegoVideoCaptureFactory;


/**
 * Created by robotding on 16/6/5.
 */
public class VideoCaptureFactoryDemo extends ZegoVideoCaptureFactory {
    private int mode = 3;
    private ZegoVideoCaptureDevice mDevice = null;
    private Context mContext = null;

    public ZegoVideoCaptureDevice create(String device_id) {
        if (mode == 0) {
            mDevice = new VideoCaptureFromCamera();
        } else if (mode == 1) {
            mDevice = new VideoCaptureFromImage(mContext);
        } else if (mode == 2) {
            mDevice = new VideoCaptureFromImage2(mContext);
        } else if (mode == 3) {
            mDevice = new VideoCaptureFromFaceunity(mContext);
        }

        return mDevice;
    }

    public void destroy(ZegoVideoCaptureDevice vc) {
        mDevice = null;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public FaceunityController getDevice() {
        if(mDevice instanceof  FaceunityController) {
            return (FaceunityController) mDevice;
        }
        return null;
    }
}
