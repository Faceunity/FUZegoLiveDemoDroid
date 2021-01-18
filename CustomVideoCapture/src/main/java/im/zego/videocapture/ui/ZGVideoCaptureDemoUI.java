package im.zego.videocapture.ui;


import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Date;

import im.zego.common.ui.BaseActivity;
import im.zego.common.util.AppLogger;
import im.zego.common.util.DeviceInfoManager;
import im.zego.common.util.SettingDataUtil;
import im.zego.videocapture.R;
import im.zego.videocapture.camera.VideoCaptureFromCamera;
import im.zego.videocapture.camera.VideoCaptureFromImage2;
import im.zego.videocapture.camera.VideoCaptureScreen;
import im.zego.videocapture.camera.ZegoVideoCaptureCallback;
import im.zego.videocapture.enums.CaptureOrigin;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerMediaEvent;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;
import im.zego.zegoexpress.entity.ZegoVideoConfig;

/**
 * ZGVideoCaptureDemoUI
 * 主要是处理推拉流并展示渲染视图
 */

/**
 *  * ZGVideoCaptureDemoUI
 *  * Mainly handle the push-pull stream and display the rendered view
 *  
 */
public class ZGVideoCaptureDemoUI extends BaseActivity {

    private TextureView mPreView;
    private TextureView mPlayView;
    private TextView mErrorTxt;
    private TextView mNumTxt;
    private Button mDealBtn;
    private Button mDealPlayBtn;

    private String mRoomID = "zgvc_";
    private String mRoomName = "VideoExternalCaptureDemo";
    private String mPlayStreamID = "";

    private boolean isLoginSuccess = false;
    // 采集源是否是录屏
    // Whether the collection source is screen recording
    private int captureOrigin = 0;
    private ZegoExpressEngine mSDKEngine;

    private int nCur = 0;
    private String userID;
    private String userName;

    private static final int DEFAULT_VIDEO_WIDTH = 360;

    private static final int DEFAULT_VIDEO_HEIGHT = 640;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zgvideo_capture_demo);

        mPreView = (TextureView) findViewById(R.id.pre_view);
        mPlayView = (TextureView) findViewById(R.id.play_view);
        mErrorTxt = (TextView) findViewById(R.id.error_txt);
        mNumTxt = (TextView) findViewById(R.id.num_txt);
        mDealBtn = (Button) findViewById(R.id.publish_btn);
        mDealPlayBtn = (Button) findViewById(R.id.play_btn);

        // 获取设备唯一ID
        String deviceID = DeviceInfoManager.generateDeviceId(this);
        mRoomID += deviceID;
        mPlayStreamID = mRoomID;
        // 采集源是否是录屏
        // Whether the collection source is screen recording
        captureOrigin = getIntent().getIntExtra("captureOrigin", 0);
        if (captureOrigin == CaptureOrigin.CaptureOrigin_Screen.getCode()) {
            // 采集源为录屏时，启动一个线程在界面上显示动态数字
            // When the collection source is screen recording, start a thread to display dynamic numbers on the interface
            new Thread(new ThreadShow()).start();
        }
        // 初始化SDK登录房间
        // Initialize SDK login room
        initSDK();

        // 开始推流
        // start publish stream
        doPublish();
    }

    private void initSDK() {
        ZegoVideoCaptureCallback videoCapture = null;
        // 创建sdk
        AppLogger.getInstance().i(getString(R.string.create_zego_engine));
        mSDKEngine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), this.getApplication(), null);
        mSDKEngine.setEventHandler(zegoEventHandler);
        if (captureOrigin == CaptureOrigin.CaptureOrigin_Camera.getCode()) {
            videoCapture = new VideoCaptureFromCamera(mSDKEngine);
        } else if (captureOrigin == CaptureOrigin.CaptureOrigin_Image.getCode()) {
            videoCapture = new VideoCaptureFromImage2(this.getApplicationContext(), mSDKEngine);
        } else if (captureOrigin == CaptureOrigin.CaptureOrigin_Screen.getCode()) {
            videoCapture = new VideoCaptureScreen(ZGVideoCaptureOriginUI.mMediaProjection, DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT, mSDKEngine);
        }
        videoCapture.setView(mPreView);
        mSDKEngine.setCustomVideoCaptureHandler(videoCapture);


        ZegoRoomConfig config = new ZegoRoomConfig();
        /* 使能用户登录/登出房间通知 */
        /* Enable notification when user login or logout */
        config.isUserStatusNotify = true;
        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "user" + randomSuffix;
        userName = "userName" + randomSuffix;
        mSDKEngine.loginRoom(mRoomID, new ZegoUser(userID, userName), config);
    }

    IZegoEventHandler zegoEventHandler = new IZegoEventHandler() {


        @Override
        public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
            /** 房间状态回调，在登录房间后，当房间状态发生变化（例如房间断开，认证失败等），SDK会通过该接口通知 */
            /** Room status update callback: after logging into the room, when the room connection status changes
             * (such as room disconnection, login authentication failure, etc.), the SDK will notify through the callback
             */
            AppLogger.getInstance().i("onRoomStateUpdate: roomID = " + roomID + ", state = " + state + ", errorCode = " + errorCode);
            if (state == ZegoRoomState.CONNECTED) {
                isLoginSuccess = true;
                mErrorTxt.setText("");

            } else if (state == ZegoRoomState.DISCONNECTED) {
                mErrorTxt.setText("login room fail, err:" + errorCode);
            }
        }

        @Override
        public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
            AppLogger.getInstance().i("onPublisherStateUpdate: streamID = " + streamID + ", state = " + state + ", errCode = " + errorCode);
            if (state == ZegoPublisherState.PUBLISH_REQUESTING) {
                mDealBtn.setText("StopPublish");
            }
        }

        @Override
        public void onPlayerMediaEvent(String streamID, ZegoPlayerMediaEvent event) {
            if (event == ZegoPlayerMediaEvent.VIDEO_BREAK_OCCUR) {
                runOnUiThread(() -> {
                    mErrorTxt.setText("play stream fail，err：");
                });
            } else if (event == ZegoPlayerMediaEvent.VIDEO_BREAK_RESUME) {
                runOnUiThread(() -> {
                    mErrorTxt.setText("");
                    mDealPlayBtn.setText("StopPlay");
                });
            }

        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZegoExpressEngine.setEngineConfig(null);
        // 登出房间并释放ZEGO SDK
        // Log out of the room and release the ZEGO SDK
        logoutLiveRoom();
    }


    // 推流
    public void doPublish() {
        // 设置编码以及采集分辨率
        // Set encoding and acquisition resolution
        ZegoVideoConfig zegoVideoConfig = new ZegoVideoConfig();
        zegoVideoConfig.setCaptureResolution(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT);
        zegoVideoConfig.setEncodeResolution(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT);
        mSDKEngine.setVideoConfig(zegoVideoConfig);

        ZegoCanvas zegoCanvas = new ZegoCanvas(null);
        zegoCanvas.viewMode = ZegoViewMode.SCALE_TO_FILL;
        // 设置预览视图及视图展示模式
        // Set preview view and view display mode
        mSDKEngine.startPreview(zegoCanvas);
        mSDKEngine.startPublishingStream(mRoomID);
    }

    // 登出房间，去除推拉流回调监听并释放ZEGO SDK
    // Log out of the room, remove the push-pull flow callback monitoring and release the ZEGO SDK
    public void logoutLiveRoom() {
        mSDKEngine.logoutRoom(mRoomID);
        ZegoExpressEngine.destroyEngine(null);
    }

    // 处理推流操作
    // Handling push operations
    public void DealPublishing(View view) {
        // 界面button==停止推流
        if (mDealBtn.getText().toString().equals("StopPublish")) {
            // 停止预览和推流
            mSDKEngine.stopPreview();
            mSDKEngine.stopPublishingStream();
            mDealBtn.setText("StartPublish");
        } else {
            mDealBtn.setText("StopPublish");
            // 界面button==开始推流
            doPublish();
        }
    }

    // 处理拉流操作
    // Handle pull stream operation
    public void DealPlay(View view) {
        // 界面button==开始拉流
        if (mDealPlayBtn.getText().toString().equals("StartPlay") && !mPlayStreamID.equals("")) {
            ZegoCanvas zegoCanvas = new ZegoCanvas(mPlayView);
            zegoCanvas.viewMode = ZegoViewMode.SCALE_TO_FILL;
            // 开始拉流
            mSDKEngine.startPlayingStream(mPlayStreamID, zegoCanvas);
            mDealPlayBtn.setText("StopPlay");
            mErrorTxt.setText("");

        } else {
            // 界面button==停止拉流
            if (!mPlayStreamID.equals("")) {
                //停止拉流
                mSDKEngine.stopPlayingStream(mPlayStreamID);

                mDealPlayBtn.setText("StartPlay");

            }
        }
    }

    // 界面动画数字线程类
    // Interface animation digital thread class
    class ThreadShow implements Runnable {

        @Override
        public void run() {
            // TODO Auto-generated method stub

            while (true) {
                try {
                    Thread.sleep(1000);

                    runOnUiThread(() -> {
                        mNumTxt.setText(String.valueOf(nCur));
                    });

                    // 实现界面上的数字递增
                    // Realize number increment on the interface
                    nCur++;
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    System.out.println("thread error...");
                }
            }
        }
    }
}
