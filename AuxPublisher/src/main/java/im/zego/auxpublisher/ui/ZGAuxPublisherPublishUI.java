package im.zego.auxpublisher.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import org.json.JSONObject;

import java.util.Date;

import im.zego.auxpublisher.R;
import im.zego.auxpublisher.camera.VideoCaptureFromImage2;
import im.zego.auxpublisher.camera.ZegoVideoCaptureCallback;
import im.zego.auxpublisher.databinding.AuxPublishBinding;
import im.zego.common.util.AppLogger;
import im.zego.common.util.SettingDataUtil;
import im.zego.common.widgets.log.FloatingView;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoVideoBufferType;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoCustomVideoCaptureConfig;
import im.zego.zegoexpress.entity.ZegoEngineConfig;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;
import im.zego.zegoexpress.entity.ZegoVideoConfig;

import static im.zego.zegoexpress.constants.ZegoVideoConfigPreset.PRESET_1080P;

public class ZGAuxPublisherPublishUI extends Activity {
    private AuxPublishBinding binding;
    private ZegoCustomVideoCaptureConfig captureConfig;
    private ZegoExpressEngine mSDKEngine;
    private boolean loginRoomFlag = false;
    private boolean startMainPublishFlag = false;
    private boolean startAuxPublishFlag = false;
    private String userID;
    private String userName;
    private String mainStreamId = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
    private String auxStreamId = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000)) + "123";
    public static final String mRoomID = "AuxPublisherRoom-1";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.aux_publish);
        /** 添加悬浮日志视图 */
        /** Add floating log view */
        FloatingView.get().add();
        /** 记录SDK版本号 */
        /** Record SDK version */
        AppLogger.getInstance().i("SDK version : %s", ZegoExpressEngine.getVersion());
        initView();
        createEngine();
    }

    private void initView() {
        binding.auxLoginRoom.setText(getString(R.string.login_room));
        binding.startMainEt.setText(mainStreamId);
        binding.startAuxEt.setText(auxStreamId);
        binding.startMain.setText(getString(R.string.start_publish_main));
        binding.startAux.setText(getString(R.string.start_publish_aux));
        binding.show.setVisibility(View.GONE);
        binding.auxRoomId.setText("roomId：" + mRoomID);
        binding.roomConnectState.setText(getString(R.string.room_unconnect));
        binding.mainConnectState.setText(getString(R.string.no_publish));
        binding.auxConnectState.setText(getString(R.string.no_publish));
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
                binding.roomConnectState.setText(getString(R.string.room_connect));
                binding.auxLoginRoom.setText(getString(R.string.logout_room));
                loginRoomFlag = !loginRoomFlag;
                binding.show.setVisibility(View.VISIBLE);

            } else if (state == ZegoRoomState.DISCONNECTED) {
                binding.roomConnectState.setText(getString(R.string.room_unconnect));
                binding.auxLoginRoom.setText(getString(R.string.login_room));
                loginRoomFlag = !loginRoomFlag;
                binding.show.setVisibility(View.GONE);
            }
            if (errorCode != 0) {
                Toast.makeText(ZGAuxPublisherPublishUI.this, String.format("login room fail, errorCode: %d", errorCode), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
            AppLogger.getInstance().i("onPublisherStateUpdate: streamID = " + streamID + ", state = " + state + ", errCode = " + errorCode);
            if (state == ZegoPublisherState.PUBLISH_REQUESTING) {
                if (streamID.equals(mainStreamId)) {
                    binding.mainConnectState.setText(getString(R.string.stream_request));
                    binding.startMain.setText(getString(R.string.stream_request));
                    binding.startMain.setClickable(false);
                } else if (streamID.equals(auxStreamId)) {
                    binding.auxConnectState.setText(getString(R.string.stream_request));
                    binding.startAux.setText(getString(R.string.stream_request));
                    binding.startAux.setClickable(false);
                }
            } else if (state == ZegoPublisherState.NO_PUBLISH) {
                if (streamID.equals(mainStreamId)) {
                    binding.mainConnectState.setText(getString(R.string.no_publish));
                    binding.startMain.setText(getString(R.string.start_publish_main));
                    startMainPublishFlag = !startMainPublishFlag;
                    binding.startMain.setClickable(true);
                } else if (streamID.equals(auxStreamId)) {
                    binding.auxConnectState.setText(getString(R.string.no_publish));
                    binding.startAux.setText(getString(R.string.start_publish_aux));
                    startAuxPublishFlag = !startAuxPublishFlag;
                    binding.startAux.setClickable(true);
                }

            } else if (state == ZegoPublisherState.PUBLISHING) {
                if (streamID.equals(mainStreamId)) {
                    binding.mainConnectState.setText(getString(R.string.publishing));
                    binding.startMain.setText(getString(R.string.stop_publish_main));
                    startMainPublishFlag = !startMainPublishFlag;
                    binding.startMain.setClickable(true);
                } else if (streamID.equals(auxStreamId)) {
                    binding.auxConnectState.setText(getString(R.string.publishing));
                    binding.startAux.setText(getString(R.string.stop_publish_aux));
                    startAuxPublishFlag = !startAuxPublishFlag;
                    binding.startAux.setClickable(true);
                }
            }
        }


    };

    private void createEngine() {
        captureConfig = new ZegoCustomVideoCaptureConfig();
        captureConfig.bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
        ZegoEngineConfig engineConfig = new ZegoEngineConfig();
        /** 辅流自定义视频采集配置，不设则默认辅流不开启自定义视频采集 */
        //Custom video capture configuration for auxiliary stream, if not set, the default auxiliary stream will not enable custom video capture
        engineConfig.customVideoCaptureAuxConfig = captureConfig;
        ZegoExpressEngine.setEngineConfig(engineConfig);
        mSDKEngine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), this.getApplication(), null);
        AppLogger.getInstance().i(getString(R.string.create_zego_engine));
        mSDKEngine.setEventHandler(zegoEventHandler);
        ZegoVideoCaptureCallback videoCapture = null;
        videoCapture = new VideoCaptureFromImage2(this.getApplicationContext(), mSDKEngine);
        videoCapture.setView(binding.auxView);
        mSDKEngine.setCustomVideoCaptureHandler(videoCapture);
        ZegoVideoConfig videoConfig = new ZegoVideoConfig(PRESET_1080P);
        mSDKEngine.setVideoConfig(videoConfig, ZegoPublishChannel.MAIN);
        mSDKEngine.setVideoConfig(videoConfig, ZegoPublishChannel.AUX);
    }

    public void loginRoom(View view) {
        if (loginRoomFlag) {
            logOutRoom();
        } else {
            loginRoom();
        }

    }

    private void logOutRoom() {
        mSDKEngine.logoutRoom(mRoomID);
    }

    private void loginRoom() {
        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "user" + randomSuffix;
        userName = "userName" + randomSuffix;
        ZegoRoomConfig config = new ZegoRoomConfig();
        /* 使能用户登录/登出房间通知 */
        /* Enable notification when user login or logout */
        config.isUserStatusNotify = true;
        mSDKEngine.loginRoom(mRoomID, new ZegoUser(userID, userName), config);
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, ZGAuxPublisherPublishUI.class);
        activity.startActivity(intent);
    }

    public void startOrStopPushMain(View view) {
        if (startMainPublishFlag) {
            AppLogger.getInstance().i("stopPublishMainStream streamId:" + mainStreamId);
            //通过ZegoPublishChannel.MAIN参数表明操作的是主流
            //ZegoPublishChannel.MAIN parameter indicates that the operation is mainstream
            mSDKEngine.stopPublishingStream(ZegoPublishChannel.MAIN);
            mSDKEngine.stopPreview();
        } else {
            AppLogger.getInstance().i("startPublishMainStream streamId:" + mainStreamId);
            ZegoCanvas zegoCanvas = new ZegoCanvas(binding.mainView);
            zegoCanvas.viewMode = ZegoViewMode.ASPECT_FIT;
            // 设置预览视图及视图展示模式
            mSDKEngine.startPreview(zegoCanvas);
            //通过ZegoPublishChannel.MAIN参数表明操作的是主流
            //ZegoPublishChannel.MAIN parameter indicates that the operation is mainstream
            mSDKEngine.startPublishingStream(mainStreamId, ZegoPublishChannel.MAIN);
        }
    }

    public void startOrStopPushAux(View view) {
        if (startAuxPublishFlag) {
            AppLogger.getInstance().i("stopPublishAuxStream streamId:" + auxStreamId);
            //通过ZegoPublishChannel.AUX参数表明操作的是辅流
            //The ZegoPublishChannel.AUX parameter indicates that the auxiliary stream is operated
            mSDKEngine.stopPublishingStream(ZegoPublishChannel.AUX);
        } else {
            //通过ZegoPublishChannel.AUX参数表明操作的是辅流
            //The ZegoPublishChannel.AUX parameter indicates that the auxiliary stream is operated
            AppLogger.getInstance().i("startPublishAuxStream streamId:" + auxStreamId);
            mSDKEngine.startPublishingStream(auxStreamId, ZegoPublishChannel.AUX);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZegoExpressEngine.setEngineConfig(null);
        // 登出房间并释放ZEGO SDK
        //Log out of the room and release the ZEGO SDK
        logoutLiveRoom();
    }

    // 登出房间，去除推拉流回调监听并释放ZEGO SDK
    //Log out of the room, remove the push-pull stream callback listener and release the ZEGO SDK
    public void logoutLiveRoom() {
        mSDKEngine.logoutRoom(mRoomID);
        ZegoExpressEngine.destroyEngine(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FloatingView.get().attach(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FloatingView.get().detach(this);
    }
}
