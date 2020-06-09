package com.zego.videofilter.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Toast;

import com.zego.common.util.AppLogger;
import com.zego.common.util.ZegoUtil;
import com.zego.common.widgets.CustomDialog;
import com.zego.videofilter.R;
import com.zego.videofilter.ZGFilterHelper;
import com.zego.videofilter.databinding.ActivityFuBaseBinding;
import com.zego.videofilter.faceunity.FURenderer;
import com.zego.videofilter.faceunity.utils.LifeCycleSensorManager;
import com.zego.videofilter.videoFilter.VideoFilterFactoryDemo;
import com.zego.videofilter.view.BeautyControlView;
import com.zego.zegoavkit2.videofilter.ZegoExternalVideoFilter;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.IZegoInitSDKCompletionCallback;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;

import java.util.HashMap;


/**
 * 带美颜的推流界面
 */

public class FUBeautyActivity extends AppCompatActivity {
    public final static String TAG = FUBeautyActivity.class.getSimpleName();

    private ActivityFuBaseBinding binding;

    // faceunity 美颜相关的封装类
    protected FURenderer mFURenderer;

    // 房间 ID
    private String mRoomID = "";

    // 主播流名
    private String anchorStreamID = ZegoUtil.getPublishStreamID();

    private VideoFilterFactoryDemo.FilterType chooseFilterType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_fu_base);

        ViewStub bottomViewStub = (ViewStub) findViewById(R.id.fu_base_bottom);
        bottomViewStub.setInflatedId(R.id.fu_base_bottom);

        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        bottomViewStub.setLayoutResource(R.layout.layout_fu_beauty);
        bottomViewStub.inflate();

        mRoomID = getIntent().getStringExtra("roomID");
        chooseFilterType = (VideoFilterFactoryDemo.FilterType) getIntent().getSerializableExtra("FilterType");
        Log.d(TAG, "onCreate: roomID:" + mRoomID + ", filterType:" + chooseFilterType);
        switch (chooseFilterType) {
            case FilterType_SurfaceTexture: {
                mFURenderer = new FURenderer.Builder(this)
                        .setInputTextureType(FURenderer.INPUT_EXTERNAL_OES_TEXTURE)
                        .setCameraType(Camera.CameraInfo.CAMERA_FACING_FRONT)
                        .setInputImageOrientation(FURenderer.getCameraOrientation(Camera.CameraInfo.CAMERA_FACING_FRONT))
                        .build();
            }
            break;
            case FilterType_Mem:
            case FilterType_ASYNCI420Mem:
            case FilterType_HybridMem:
            case FilterType_SyncTexture: {
                mFURenderer = new FURenderer.Builder(this)
                        .setInputTextureType(FURenderer.INPUT_2D_TEXTURE)
                        .setCameraType(Camera.CameraInfo.CAMERA_FACING_FRONT)
                        .setInputImageOrientation(FURenderer.getCameraOrientation(Camera.CameraInfo.CAMERA_FACING_FRONT))
                        .build();
            }
            break;
            default:
        }
        BeautyControlView beautyControlView = findViewById(R.id.fu_beauty_control);
        beautyControlView.setOnFaceUnityControlListener(mFURenderer);

        // 初始化SDK
        initSDK();

        // 设置 SDK 推流回调监听
        initSDKCallback();

        LifeCycleSensorManager lifeCycleSensorManager = new LifeCycleSensorManager(this, getLifecycle());
        lifeCycleSensorManager.setOnAccelerometerChangedListener(new LifeCycleSensorManager.OnAccelerometerChangedListener() {
            @Override
            public void onAccelerometerChanged(float x, float y, float z) {
                if (Math.abs(x) > 3 || Math.abs(y) > 3) {
                    if (Math.abs(x) > Math.abs(y)) {
                        mFURenderer.onDeviceOrientationChanged(x > 0 ? 0 : 180);
                    } else {
                        mFURenderer.onDeviceOrientationChanged(y > 0 ? 90 : 270);
                    }
                }
            }
        });

        String version = ZegoLiveRoom.version();
        String version2 = ZegoLiveRoom.version2();
        Log.e(TAG, "onCreate: version:" + version + ", version2:" + version2);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void finish() {
        super.finish();

        // 在退出页面时停止推流
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().stopPublishing();
        // 停止预览
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().stopPreview();

        // 登出房间
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().logoutRoom();

        // 去除外部滤镜的设置
        ZegoExternalVideoFilter.setVideoFilterFactory(null, ZegoConstants.PublishChannelIndex.MAIN);

        // 释放 SDK
        ZGFilterHelper.sharedInstance().releaseZegoLiveRoom();
    }

    /**
     * 供其他Activity调用，进入本专题的方法
     *
     * @param activity
     */
    public static void actionStart(Activity activity, String roomID, VideoFilterFactoryDemo.FilterType filterType) {
        Intent intent = new Intent(activity, FUBeautyActivity.class);
        intent.putExtra("roomID", roomID);
        intent.putExtra("FilterType", filterType);
        activity.startActivity(intent);
    }

    /**
     * 初始化SDK逻辑
     * 初始化成功后登录房间并推流
     */
    private void initSDK() {
        AppLogger.getInstance().i(VideoFilterMainUI.class, "初始化ZEGO SDK");

        /**
         * 需要在 initSDK 之前设置 SDK 环境，此处设置为测试环境；
         * 从官网申请的 AppID 默认是测试环境，而 SDK 初始化默认是正式环境，所以需要在初始化 SDK 前设置测试环境，否则 SDK 会初始化失败；
         * 当 App 集成完成后，再向 ZEGO 申请开启正式环境，改为正式环境再初始化。
         */
        ZegoLiveRoom.setTestEnv(ZegoUtil.getIsTestEnv());

        // 设置外部滤镜---必须在初始化 ZEGO SDK 之前设置，否则不会回调   SyncTexture
        VideoFilterFactoryDemo filterFactory = new VideoFilterFactoryDemo(chooseFilterType, mFURenderer);
        ZegoExternalVideoFilter.setVideoFilterFactory(filterFactory, ZegoConstants.PublishChannelIndex.MAIN);

        // 初始化SDK
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().initSDK(ZegoUtil.getAppID(), ZegoUtil.getAppSign(), new IZegoInitSDKCompletionCallback() {
            @Override
            public void onInitSDK(int errorCode) {

                if (errorCode == 0) {
                    // 初始化成功，登录房间并推流
                    startPublish();

                    AppLogger.getInstance().i(FUBeautyActivity.class, "初始化ZEGO SDK成功");
                } else {
                    Toast.makeText(FUBeautyActivity.this, getString(com.zego.common.R.string.tx_init_failure), Toast.LENGTH_SHORT).show();
                    AppLogger.getInstance().i(FUBeautyActivity.class, "初始化ZEGO SDK失败 errorCode : %d", errorCode);
                }
            }
        });
    }

    public void startPublish() {

        // 防止用户点击，弹出加载对话框
        CustomDialog.createDialog("登录房间中...", this).show();
        AppLogger.getInstance().i(FUBeautyActivity.class, getString(R.string.tx_login_room));

        // 开始推流前需要先登录房间，此处是主播登录房间，成功登录后开始推流
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().loginRoom(mRoomID, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] zegoStreamInfos) {
                CustomDialog.createDialog(FUBeautyActivity.this).cancel();
                if (errorCode == 0) {
                    AppLogger.getInstance().i(FUBeautyActivity.class, "登录房间成功 roomId : %s", mRoomID);

                    ZegoAvConfig config = new ZegoAvConfig(ZegoAvConfig.Level.High);
                    config.setVideoFPS(30);
                    ZGFilterHelper.sharedInstance().getZegoLiveRoom().setAVConfig(config);
                    // 设置预览视图模式，此处采用 SDK 默认值--等比缩放填充整View，可能有部分被裁减。
                    ZGFilterHelper.sharedInstance().getZegoLiveRoom().setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
                    // 设置预览 view，主播自己推流采用全屏视图
                    ZGFilterHelper.sharedInstance().getZegoLiveRoom().setPreviewView(binding.preview);
                    // 启动预览
                    ZGFilterHelper.sharedInstance().getZegoLiveRoom().startPreview();

                    // 开始推流，flag 使用连麦场景，推荐场景
                    ZGFilterHelper.sharedInstance().getZegoLiveRoom().startPublishing(anchorStreamID, "anchor", ZegoConstants.PublishFlag.JoinPublish);

                } else {
                    AppLogger.getInstance().i(FUBeautyActivity.class, "登录房间失败, errorCode : %d", errorCode);
                    Toast.makeText(FUBeautyActivity.this, getString(com.zego.common.R.string.tx_login_room_failure), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 设置 SDK 推流回调监听
    private void initSDKCallback() {
        ZGFilterHelper.sharedInstance().getZegoLiveRoom().setZegoLivePublisherCallback(new IZegoLivePublisherCallback() {
            @Override
            public void onPublishStateUpdate(int errorCode, String streamID, HashMap<String, Object> hashMap) {
                // 推流状态更新，errorCode 非0 则说明推流失败
                // 推流常见错误码请看文档: <a>https://doc.zego.im/CN/308.html</a>

                if (errorCode == 0) {
                    AppLogger.getInstance().i(FUBeautyActivity.class, "推流成功, streamID : %s", streamID);
                    Toast.makeText(FUBeautyActivity.this, getString(R.string.tx_publish_success), Toast.LENGTH_SHORT).show();
                } else {
                    AppLogger.getInstance().i(FUBeautyActivity.class, "推流失败, streamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(FUBeautyActivity.this, getString(R.string.tx_publish_fail), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onJoinLiveRequest(int i, String s, String s1, String s2) {

            }

            @Override
            public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {

            }

            @Override
            public void onCaptureVideoSizeChangedTo(int i, int i1) {

            }

            @Override
            public void onCaptureVideoFirstFrame() {

            }

            @Override
            public void onCaptureAudioFirstFrame() {

            }
        });
    }
}
