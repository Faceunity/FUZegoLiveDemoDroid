package com.zego.livedemo5.videofilter;

import android.content.Context;
import android.util.Log;

import com.faceunity.nama.OnFaceUnityControlListener;
import com.zego.livedemo5.ZegoApiManager;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilter;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilterFactory;

/**
 * Created by robotding on 16/12/3.
 */

public class VideoFilterFactoryDemo extends ZegoVideoFilterFactory {
    private static final String TAG = "VideoFilterFactoryDemo";
    private int mode = 3;
    private ZegoVideoFilter mFilter = null;

    private Context mContext;

    public VideoFilterFactoryDemo(Context context) {
        mContext = context;
    }

    @Override
    public ZegoVideoFilter create() {
        String isOpen = ZegoApiManager.getInstance().getIsOpen();
        if ("true".equals(isOpen)) {
            mode = 6;
        } else {
            mode = 2;
        }
        Log.d(TAG, "create ZegoVideoFilter, mode:" + mode);
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
                mFilter = new VideoFilterI420MemDemo(mContext);
                break;
            case 6:
//                mFilter = new FUVideoFilterHybridDemo(mContext);
                mFilter = new FUVideoFilterGlTexture2dDemo(mContext);
//                mFilter = new FUVideoFilterMemDemo(mContext);
                break;
            default:
        }
        return mFilter;
    }

    @Override
    public void destroy(ZegoVideoFilter vf) {
        mFilter = null;
    }

    public OnFaceUnityControlListener getFaceunityController() {
        if (mFilter instanceof FUVideoFilterGlTexture2dDemo) {
            return ((FUVideoFilterGlTexture2dDemo) mFilter).getFaceunityController();
        } else if (mFilter instanceof FUVideoFilterHybridDemo) {
            return ((FUVideoFilterHybridDemo) mFilter).getFaceunityController();
        } else if (mFilter instanceof VideoFilterI420MemDemo) {
            return ((VideoFilterI420MemDemo) mFilter).getFaceunityController();
        } else if (mFilter instanceof FUVideoFilterMemDemo) {
            return ((FUVideoFilterMemDemo) mFilter).getFaceunityController();
        }
        return null;
    }

    public ZegoVideoFilter getFilter() {
        return mFilter;
    }
}
