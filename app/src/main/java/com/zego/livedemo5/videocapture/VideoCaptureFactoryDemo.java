package com.zego.livedemo5.videocapture;

import android.content.Context;

import com.faceunity.beautycontrolview.OnFaceUnityControlListener;
import com.zego.zegoavkit2.ZegoVideoCaptureDevice;
import com.zego.zegoavkit2.ZegoVideoCaptureFactory;


/**
 * Created by robotding on 16/6/5.
 */
public class VideoCaptureFactoryDemo extends ZegoVideoCaptureFactory {
    private int mode = 4;
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
            mDevice = new VideoCaptureFromCamera2();
        } else if (mode == 4) {
            mDevice = new FUVideoCaptureFromCamera2(mContext);
        }
        return mDevice;
    }

    public void destroy(ZegoVideoCaptureDevice vc) {
        mDevice = null;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public OnFaceUnityControlListener getFaceunityController() {
        if (mDevice instanceof FUVideoCaptureFromCamera2) {
            return ((FUVideoCaptureFromCamera2) mDevice).getFaceunityController();
        }
        return null;
    }
}
