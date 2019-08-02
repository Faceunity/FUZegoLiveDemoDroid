package com.zego.livedemo5.videofilter;

import android.content.Context;

import com.faceunity.beautycontrolview.OnFaceUnityControlListener;
import com.zego.livedemo5.ZegoApiManager;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilter;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilterFactory;

/**
 * Created by robotding on 16/12/3.
 */

public class VideoFilterFactoryDemo extends ZegoVideoFilterFactory {
    private int mode = 6;//默认1
    private ZegoVideoFilter mFilter = null;

    private Context mContext;

    public VideoFilterFactoryDemo(Context context) {
        mContext = context;
    }

    public ZegoVideoFilter create() {
        String isOpen = ZegoApiManager.getInstance().getIsOpen();
        if (isOpen.equals("true")) {
            mode = 6;
        } else {
            mode = 1;
        }
        switch (mode) {
            case 0:
                mFilter = new VideoFilterMemDemo();
                break;
            case 1:
                mFilter = new VideoFilterSurfaceTextureDemo();
                break;
            case 2:
                mFilter = new VideoFilterHybridDemo();
                break;
            case 3:
                mFilter = new VideoFilterGlTexture2dDemo();
                break;
            case 4:
                mFilter = new VideoFilterSurfaceTextureDemo2();
                break;
            case 5:
                mFilter = new VideoFilterI420MemDemo();
                break;
            case 6:
                mFilter = new FUVideoFilterGlTexture2dDemo(mContext);
                break;
        }

        return mFilter;
    }

    public void destroy(ZegoVideoFilter vf) {
        mFilter = null;
    }

    public OnFaceUnityControlListener getFaceunityController() {
        if (mFilter instanceof FUVideoFilterGlTexture2dDemo) {
            return ((FUVideoFilterGlTexture2dDemo) mFilter).getFaceunityController();
        }
        return null;
    }
}
