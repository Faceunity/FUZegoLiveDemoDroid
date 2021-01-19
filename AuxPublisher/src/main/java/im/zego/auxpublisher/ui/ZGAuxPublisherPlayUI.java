package im.zego.auxpublisher.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import org.json.JSONObject;

import java.util.Date;

import im.zego.auxpublisher.R;
import im.zego.auxpublisher.databinding.AuxPlayBinding;
import im.zego.common.util.AppLogger;
import im.zego.common.util.SettingDataUtil;
import im.zego.common.widgets.log.FloatingView;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;

import static im.zego.auxpublisher.ui.ZGAuxPublisherPublishUI.mRoomID;

public class ZGAuxPublisherPlayUI extends Activity {
    private AuxPlayBinding binding;
    private ZegoExpressEngine mSDKEngine;
    private String firstStreamId = "";
    private String secondStreamId = "";
    private boolean loginRoomFlag = false;
    private String userID;
    private String userName;
    private boolean startFirstStreamPlayFlag = false;
    private boolean startSecondStreamPlayFlag = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.aux_play);
        /** 添加悬浮日志视图 */
        /** Add floating log view */
        FloatingView.get().add();
        /** 记录SDK版本号 */
        /** Record SDK version */
        AppLogger.getInstance().i("SDK version : %s", ZegoExpressEngine.getVersion());
        initView();
        createEngine();
    }

    private void createEngine() {
        mSDKEngine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), this.getApplication(), null);
        mSDKEngine.setEventHandler(zegoEventHandler);
        AppLogger.getInstance().i(getString(R.string.create_zego_engine));
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
                Toast.makeText(ZGAuxPublisherPlayUI.this, String.format("login room fail, errorCode: %d", errorCode), Toast.LENGTH_LONG).show();
            }

        }

        @Override
        public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
            AppLogger.getInstance().i("onPlayerStateUpdate: streamID = " + streamID + ", state = " + state + ", errCode = " + errorCode);
            if (state == ZegoPlayerState.PLAY_REQUESTING) {
                if (streamID.equals(firstStreamId)) {
                    binding.firstConnectState.setText(getString(R.string.stream_request));
                    binding.startMain.setText(getString(R.string.stream_request));
                    binding.startMain.setClickable(false);
                } else if (streamID.equals(secondStreamId)) {
                    binding.secondConnectState.setText(getString(R.string.stream_request));
                    binding.startAux.setText(getString(R.string.stream_request));
                    binding.startAux.setClickable(false);
                }
            } else if (state == ZegoPlayerState.NO_PLAY) {
                if (streamID.equals(firstStreamId)) {
                    binding.firstConnectState.setText(getString(R.string.no_play));
                    binding.startMain.setText(getString(R.string.start_play_stream));
                    startFirstStreamPlayFlag = !startFirstStreamPlayFlag;
                    binding.startMain.setClickable(true);
                    firstStreamId = "";
                } else if (streamID.equals(secondStreamId)) {
                    binding.secondConnectState.setText(getString(R.string.no_play));
                    binding.startAux.setText(getString(R.string.start_play_stream));
                    startSecondStreamPlayFlag = !startSecondStreamPlayFlag;
                    binding.startAux.setClickable(true);
                    secondStreamId = "";
                }

            } else if (state == ZegoPlayerState.PLAYING) {
                if (streamID.equals(firstStreamId)) {
                    binding.firstConnectState.setText(getString(R.string.playing));
                    binding.startMain.setText(getString(R.string.stop_play_stream));
                    startFirstStreamPlayFlag = !startFirstStreamPlayFlag;
                    binding.startMain.setClickable(true);
                } else if (streamID.equals(secondStreamId)) {
                    binding.secondConnectState.setText(getString(R.string.playing));
                    binding.startAux.setText(getString(R.string.stop_play_stream));
                    startSecondStreamPlayFlag = !startSecondStreamPlayFlag;
                    binding.startAux.setClickable(true);
                }
            }
        }
    };

    private void initView() {
        binding.auxLoginRoom.setText(getString(R.string.login_room));
        binding.startMain.setText(getString(R.string.start_play_stream));
        binding.startAux.setText(getString(R.string.start_play_stream));
        binding.show.setVisibility(View.GONE);
        binding.auxRoomId.setText("roomId：" + mRoomID);
        binding.roomConnectState.setText(getString(R.string.room_unconnect));
        binding.firstConnectState.setText(getString(R.string.no_play));
        binding.secondConnectState.setText(getString(R.string.no_play));
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
        userID = "user2" + randomSuffix;
        userName = "userName2" + randomSuffix;
        ZegoRoomConfig config = new ZegoRoomConfig();
        /* 使能用户登录/登出房间通知 */
        /* Enable notification when user login or logout */
        config.isUserStatusNotify = true;
        mSDKEngine.loginRoom(mRoomID, new ZegoUser(userID, userName), config);
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, ZGAuxPublisherPlayUI.class);
        activity.startActivity(intent);
    }

    //拉流的时候，直接通过对应的streamId，调用拉流接口完成主流或辅流的开启/关闭拉取
    //When pulling a stream, directly call the stream interface through the corresponding streamId to complete the on / off pull of the mainstream or auxiliary stream
    public void startPlayOrStopFirstStream(View view) {
        if (startFirstStreamPlayFlag) {
            AppLogger.getInstance().i("stopPlayFirstStream streamId:" + firstStreamId);
            mSDKEngine.stopPlayingStream(firstStreamId);
        } else {
            if (checkStreamId(binding.startMainEt, 0)) {
                return;
            }
            AppLogger.getInstance().i("startPlayFirstStream streamId:" + firstStreamId);
            ZegoCanvas zegoCanvas = new ZegoCanvas(binding.mainView);
            zegoCanvas.viewMode = ZegoViewMode.ASPECT_FIT;
            // 开始拉流
            mSDKEngine.startPlayingStream(firstStreamId, zegoCanvas);
        }
    }

    //拉流的时候，直接通过对应的streamId，调用拉流接口完成主流或辅流的开启/关闭拉取
    //When pulling a stream, directly call the stream interface through the corresponding streamId to complete the on / off pull of the mainstream or auxiliary stream
    public void startPlayOrStopSecondStream(View view) {
        if (startSecondStreamPlayFlag) {
            AppLogger.getInstance().i("stopPlaySecondStream streamId:" + secondStreamId);
            mSDKEngine.stopPlayingStream(secondStreamId);
        } else {
            if (checkStreamId(binding.startAuxEt, 1)) {
                return;
            }
            AppLogger.getInstance().i("startPlaySecondStream streamId:" + secondStreamId);
            ZegoCanvas zegoCanvas = new ZegoCanvas(binding.auxView);
            zegoCanvas.viewMode = ZegoViewMode.ASPECT_FIT;
            // 开始拉流
            mSDKEngine.startPlayingStream(secondStreamId, zegoCanvas);
        }
    }

    public boolean checkStreamId(EditText et, int a) {
        if (et.getText().toString().trim().equals("")) {
            Toast.makeText(this, getString(R.string.streamId_null), Toast.LENGTH_SHORT).show();
            return true;
        }
        if (a == 0) {//a==0;拉第一条流
            //a == 0; pull the first stream
            if (et.getText().toString().trim().equals(secondStreamId)) {
                Toast.makeText(this, getString(R.string.play_streamId_equal), Toast.LENGTH_SHORT).show();
                return true;
            } else {
                firstStreamId = et.getText().toString().trim();
            }
        } else if (a == 1) {//a==1;拉第二条流
            //a == 1; pull the second stream
            if (et.getText().toString().trim().equals(firstStreamId)) {
                Toast.makeText(this, getString(R.string.play_streamId_equal), Toast.LENGTH_SHORT).show();
                return true;
            } else {
                secondStreamId = et.getText().toString().trim();
            }
        }
        return false;

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
