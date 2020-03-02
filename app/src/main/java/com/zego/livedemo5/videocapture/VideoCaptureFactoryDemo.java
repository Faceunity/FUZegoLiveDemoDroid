package com.zego.livedemo5.videocapture;

import android.content.Context;

import com.zego.livedemo5.ZegoApiManager;
import com.zego.zegoavkit2.ZegoVideoCaptureDevice;
import com.zego.zegoavkit2.ZegoVideoCaptureFactory;


/**
 * Created by robotding on 16/6/5.
 */
public class VideoCaptureFactoryDemo extends ZegoVideoCaptureFactory {
    private int mode = 4;//默认1
    private ZegoVideoCaptureDevice mDevice = null;
    private Context mContext = null;
    public ZegoApiManager.OnFURendererCreatedListener fuRendererCompleteListener;

    @Override
    public ZegoVideoCaptureDevice create(String device_id) {
        String isOpen = ZegoApiManager.getInstance().getIsOpen();
        if (isOpen.equals("true")) {
            mode = 4;
        } else {
            mode = 1;
        }
        if (mode == 0) {
            mDevice = new VideoCaptureFromCamera();
        } else if (mode == 1) {
            mDevice = new VideoCaptureFromImage(mContext);
        } else if (mode == 2) {
            mDevice = new VideoCaptureFromImage2(mContext);
        } else if (mode == 3) {
            mDevice = new VideoCaptureFromCamera2();
        } else if (mode == 4) {
            mDevice = new FUVideoCaptureFromCamera2(mContext, fuRendererCompleteListener);
        }
        return mDevice;
    }

    @Override
    public void destroy(ZegoVideoCaptureDevice vc) {
        mDevice = null;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setFuRendererCompleteListener(ZegoApiManager.OnFURendererCreatedListener listener) {
        this.fuRendererCompleteListener = listener;
    }
}
