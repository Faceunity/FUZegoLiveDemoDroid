package im.zego.customrender.ui;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cc.customrender.R;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.ui.FaceUnityView;
import com.faceunity.nama.utils.CameraUtils;

import org.json.JSONObject;

import java.util.Date;

import im.zego.common.ui.BaseActivity;
import im.zego.common.util.AppLogger;
import im.zego.common.util.DeviceInfoManager;
import im.zego.common.util.SettingDataUtil;
import im.zego.customrender.videorender.FuVideoRenderHandler;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerMediaEvent;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoVideoMirrorMode;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;
import im.zego.zegoexpress.entity.ZegoVideoConfig;

public class FUVideoRenderUI extends BaseActivity implements SensorEventListener {
    private TextureView mPreView;
    private TextureView mPlayView;
    private TextView mErrorTxt;
    private Button mDealBtn;
    private Button mDealPlayBtn;
    private String userName;
    private String userID;

    private String mRoomID = "zgver_";
    private String mRoomName = "VideoExternalRenderDemo";
    private String mPlayStreamID = "";


    ZegoExpressEngine mSDKEngine;
    // 渲染类
    private FuVideoRenderHandler videoRenderer;
    private int chooseRenderType;
    private String mStreamID;
    public static String mainPublishChannel = "main";

    private FURenderer mFURenderer;
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fu_video_render);

        mPreView = (TextureView) findViewById(R.id.pre_view);
        mPlayView = (TextureView) findViewById(R.id.play_view);
        mErrorTxt = (TextView) findViewById(R.id.error_txt);
        mDealBtn = (Button) findViewById(R.id.publish_btn);
        mDealPlayBtn = (Button) findViewById(R.id.play_btn);
        isSetDecodeCallback = getIntent().getBooleanExtra("IsUseNotDecode", false);
        // 获取已选的渲染类型
        // Get the selected rendering type
        chooseRenderType = getIntent().getIntExtra("RenderType", 0);
        initFu();

        // 获取设备唯一ID
        String deviceID = DeviceInfoManager.generateDeviceId(this);
        mRoomID += deviceID;
        mStreamID = mRoomID;
        mPlayStreamID = mStreamID;
        videoRenderer = new FuVideoRenderHandler();
        videoRenderer.setFURenderer(mFURenderer);

        videoRenderer.init();
        AppLogger.getInstance().i(getString(R.string.create_zego_engine));
        mSDKEngine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), this.getApplication(), null);
        mSDKEngine.setCustomVideoRenderHandler(videoRenderer);

        mSDKEngine.setVideoMirrorMode(ZegoVideoMirrorMode.BOTH_MIRROR);
        mSDKEngine.setEventHandler(new IZegoEventHandler() {


            @Override
            public void onDebugError(int errorCode, String funcName, String info) {

            }

            @Override
            public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
                /** 房间状态回调，在登录房间后，当房间状态发生变化（例如房间断开，认证失败等），SDK会通过该接口通知 */
                /** Room status update callback: after logging into the room, when the room connection status changes
                 * (such as room disconnection, login authentication failure, etc.), the SDK will notify through the callback
                 */
                AppLogger.getInstance().i("onRoomStateUpdate: roomID = " + roomID + ", state = " + state + ", errorCode = " + errorCode);
                if (state == ZegoRoomState.CONNECTED) {

                    mSDKEngine.enableCamera(true);
                    ZegoCanvas zegoCanvas = new ZegoCanvas(null);
                    zegoCanvas.viewMode = ZegoViewMode.SCALE_TO_FILL;
                    videoRenderer.addView(mainPublishChannel, mPreView);
                    ZegoVideoConfig zegoVideoConfig = new ZegoVideoConfig();
                    zegoVideoConfig.setCaptureResolution(360, 640);
                    zegoVideoConfig.setEncodeResolution(360, 640);

                    mSDKEngine.setVideoConfig(zegoVideoConfig);
                } else if (state == ZegoRoomState.DISCONNECTED) {
                    mErrorTxt.setText("login room fail, err:" + errorCode);
                }
            }

            @Override
            public void onPlayerMediaEvent(String streamID, ZegoPlayerMediaEvent event) {
                if (event == ZegoPlayerMediaEvent.VIDEO_BREAK_OCCUR) {

                    mErrorTxt.setText(getString(R.string.video_interrupt));

                } else if (event == ZegoPlayerMediaEvent.VIDEO_BREAK_RESUME) {

                    mErrorTxt.setText("");

                }
            }

            @Override
            public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
                Log.e("", "onPlayerStateUpdate errorCode:" + errorCode + "===" + state + "===" + streamID);
            }
        });

        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "user" + randomSuffix;
        userName = "user" + randomSuffix;

        ZegoUser zegoUser = new ZegoUser(userID, userName);

        mSDKEngine.loginRoom(mRoomID, zegoUser, new ZegoRoomConfig());

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 释放渲染类
        // Release the rendering class
        videoRenderer.uninit();
        ZegoExpressEngine.setEngineConfig(null);
        // 登出房间，去除推拉流回调监听，释放 ZEGO SDK
        // Log out of the room, remove the push-pull flow callback monitoring, and release the ZEGO SDK
        mSDKEngine.logoutRoom(mRoomID);

        ZegoExpressEngine.destroyEngine(null);
    }

    private void initFu() {
        mFURenderer = new FURenderer.Builder(this)
                .setInputTextureType(FURenderer.INPUT_TEXTURE_2D)
                .setCameraFacing(Camera.CameraInfo.CAMERA_FACING_FRONT)
                .setInputImageOrientation(CameraUtils.getCameraOrientation(Camera.CameraInfo.CAMERA_FACING_FRONT))
                .setRunBenchmark(false)
                .build();
        FaceUnityView faceUnityView = findViewById(R.id.faceUnityView);
        faceUnityView.setModuleManager(mFURenderer);
    }

    // 处理推流相关操作
    // Handling push-related operations
    public void dealPublishing(View view) {
        // 界面button==停止推流
        if (mDealBtn.getText().toString().equals("StopPublish")) {
            //停止预览，停止推流
            mSDKEngine.stopPreview();
            mSDKEngine.stopPublishingStream();

            //移除渲染视图
            videoRenderer.removeView(mainPublishChannel);

            mDealBtn.setText("StartPublish");

        } else {
            // 界面button==开始推流
            // 开启预览再开始推流

            mDealBtn.setText("StopPublish");

            // 外部渲染采用码流渲染类型时，推流时由 SDK 进行渲染。
            // When the external rendering adopts the code stream rendering type, the SDK performs rendering when pushing the stream.
            if (!isSetDecodeCallback) {
                // 添加外部渲染视图
                videoRenderer.addView(mainPublishChannel, mPreView);
                ZegoCanvas zegoCanvas = new ZegoCanvas(null);
                zegoCanvas.viewMode = ZegoViewMode.SCALE_TO_FILL;
                mSDKEngine.startPreview(zegoCanvas);
            } else {
                ZegoCanvas zegoCanvas = new ZegoCanvas(mPreView);
                zegoCanvas.viewMode = ZegoViewMode.SCALE_TO_FILL;
                mSDKEngine.startPreview(zegoCanvas);
            }

            mSDKEngine.startPublishingStream(mStreamID);
        }
    }

    private boolean isSetDecodeCallback = false;

    // 处理拉流相关操作
    // Handle pull stream related operations
    public void dealPlay(View view) {

        // 界面button==开始拉流
        if (mDealPlayBtn.getText().toString().equals("StartPlay") && !mPlayStreamID.equals("")) {
            // 设置拉流视图

            if (isSetDecodeCallback) {
                // 若选择的外部渲染类型是未解码型，设置添加解码类渲染视图
                videoRenderer.addDecodView(mPlayView);
            } else {
                // 选择的外部渲染类型不是未解码型，根据拉流流名设置渲染视图
                videoRenderer.addView(mPlayStreamID, mPlayView);
            }
            // 开始拉流，不为 SDK 设置渲染视图，使用自渲染的视图
            mSDKEngine.startPlayingStream(mPlayStreamID, new ZegoCanvas(null));

            mErrorTxt.setText("");

            mDealPlayBtn.setText("StopPlay");

        } else {
            // 界面button==停止拉流
            if (!mPlayStreamID.equals("")) {
                //停止拉流
                mSDKEngine.stopPlayingStream(mPlayStreamID);
                //移除外部渲染视图
                videoRenderer.removeView(mPlayStreamID);
                mErrorTxt.setText("");

                mDealPlayBtn.setText("StartPlay");

            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && null != mFURenderer) {
            float x = event.values[0];
            float y = event.values[1];
            if (Math.abs(x) > 3 || Math.abs(y) > 3) {
                if (Math.abs(x) > Math.abs(y)) {
                    mFURenderer.onDeviceOrientationChanged(x > 0 ? 0 : 180);
                } else {
                    mFURenderer.onDeviceOrientationChanged(y > 0 ? 90 : 270);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
