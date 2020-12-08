package com.zego.videofilter.videoFilter;

import android.content.Context;

import com.faceunity.nama.FURenderer;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilter;
import com.zego.zegoavkit2.videofilter.ZegoVideoFilterFactory;

/**
 * Created by robotding on 16/12/3.
 */

public class VideoFilterFactoryDemo extends ZegoVideoFilterFactory {
    private FilterType type = FilterType.FilterType_SurfaceTexture;
    private ZegoVideoFilter mFilter = null;

    // faceunity 美颜组件
    private FURenderer mFunRender;
    private Context mContext;

    // 前处理传递数据的类型枚举
    public enum FilterType {
        FilterType_Mem,
        FilterType_SurfaceTexture,
        FilterType_HybridMem,
        FilterType_SyncTexture,
        FilterType_ASYNCI420Mem
    }

    public VideoFilterFactoryDemo(FilterType type, FURenderer fuRenderer, Context context) {
        this.type = type;
        this.mFunRender = fuRenderer;
        this.mContext = context;
    }

    // 创建外部滤镜实例
    @Override
    public ZegoVideoFilter create() {
        switch (type) {
            case FilterType_Mem:
                mFilter = new VideoFilterMemDemo(mFunRender);
                break;
            case FilterType_SurfaceTexture:
                mFilter = new VideoFilterSurfaceTextureDemo(mContext, mFunRender);
                break;
            case FilterType_HybridMem:
                mFilter = new VideoFilterHybridDemo(mFunRender);
                break;
            case FilterType_SyncTexture:
                mFilter = new VideoFilterGlTexture2dDemo(mFunRender);
                break;
            case FilterType_ASYNCI420Mem:
                mFilter = new VideoFilterI420MemDemo(mFunRender);
                break;
            default:
        }

        return mFilter;
    }

    // 销毁外部滤镜实例
    @Override
    public void destroy(ZegoVideoFilter vf) {
        mFilter = null;
    }

    public ZegoVideoFilter getFilter() {
        return mFilter;
    }
}
