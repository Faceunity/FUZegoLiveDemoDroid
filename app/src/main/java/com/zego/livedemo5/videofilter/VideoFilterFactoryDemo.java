package com.zego.livedemo5.videofilter;

import android.content.Context;

import com.zego.livedemo5.faceunity.FaceunityController;
import com.zego.zegoliveroom.videofilter.ZegoVideoFilter;
import com.zego.zegoliveroom.videofilter.ZegoVideoFilterFactory;

/**
 * Created by robotding on 16/12/3.
 */

public class VideoFilterFactoryDemo extends ZegoVideoFilterFactory {
    private int mode = 6;
    private ZegoVideoFilter mFilter = null;

    private Context mContext;

    public ZegoVideoFilter create() {
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
                mFilter = new VideoFilterFaceUnityDemo(mContext);
                break;
        }

        return mFilter;
    }

    public void destroy(ZegoVideoFilter vf) {
        mFilter = null;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public FaceunityController getFaceunityController() {
        if (mFilter instanceof FaceunityController) {
            return (FaceunityController) mFilter;
        }
        return null;
    }
}
