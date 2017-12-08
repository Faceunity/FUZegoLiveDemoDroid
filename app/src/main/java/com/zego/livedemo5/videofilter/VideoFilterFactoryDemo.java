package com.zego.livedemo5.videofilter;

import android.content.Context;

import com.faceunity.wrapper.FaceunityControlView;
import com.faceunity.wrapper.FaceunityController;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilter;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilterFactory;

/**
 * Created by robotding on 16/12/3.
 */

public class VideoFilterFactoryDemo extends ZegoVideoFilterFactory {
    private int mode = 6;
    private ZegoVideoFilter mFilter = null;

    private Context mContext;

    public VideoFilterFactoryDemo(Context context) {
        mContext = context;
    }

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
                mFilter = new FUVideoFilterGlTexture2dDemo(mContext);
                break;
        }

        return mFilter;
    }

    public void destroy(ZegoVideoFilter vf) {
        mFilter = null;
    }

    public FaceunityControlView.OnViewEventListener getFaceunityController() {
        if (mFilter instanceof FaceunityController) {
            return ((FaceunityController) mFilter).getFaceunityController();
        }
        return null;
    }
}
