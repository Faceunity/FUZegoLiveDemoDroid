package im.zego.play.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import im.zego.common.entity.SDKConfigInfo;
import im.zego.common.entity.StreamQuality;
import im.zego.common.ui.BaseActivity;
import im.zego.common.util.AppLogger;
import im.zego.common.util.SettingDataUtil;
import im.zego.play.R;
import im.zego.play.databinding.ActivityPlayBinding;
import im.zego.play.databinding.PlayInputStreamIdLayoutBinding;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;

public class PlayActivityUI extends BaseActivity {


    private ActivityPlayBinding binding;
    private PlayInputStreamIdLayoutBinding layoutBinding;
    private String mStreamID;
    private StreamQuality streamQuality = new StreamQuality();
    private SDKConfigInfo sdkConfigInfo = new SDKConfigInfo();
    private ZegoExpressEngine engine;
    private String userID;
    private String userName;
    private String roomID;
    public static ZegoViewMode viewMode = ZegoViewMode.ASPECT_FILL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_play);
        layoutBinding = binding.layout;
        layoutBinding.startButton.setText(getString(R.string.tx_start_play));
        // 利用DataBinding 可以通过bean类驱动UI变化。
        // 方便快捷避免需要写一大堆 setText 等一大堆臃肿的代码。
        binding.setQuality(streamQuality);
        binding.setConfig(sdkConfigInfo);


        // 初始化SDK
        engine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), getApplication(), null);
        AppLogger.getInstance().i("createEngine");
        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "userid-" + randomSuffix;
        userName = "username-" + randomSuffix;


        engine.setEventHandler(new IZegoEventHandler() {

            @Override
            public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
                if (state == ZegoPlayerState.PLAYING) {
                    if (errorCode == 0) {
                        mStreamID = streamID;
                        AppLogger.getInstance().i("play stream success, streamID : %s", streamID);
                        Toast.makeText(PlayActivityUI.this, getString(R.string.tx_play_success), Toast.LENGTH_SHORT).show();

                        // 修改标题状态拉流成功状态
                        binding.title.setTitleName(getString(R.string.tx_playing));
                    } else {
                        // 当拉流失败 当前 mStreamID 初始化成 null 值
                        mStreamID = null;
                        // 修改标题状态拉流失败状态
                        binding.title.setTitleName(getString(R.string.tx_play_fail));
                        AppLogger.getInstance().i("play stream fail, streamID : %s, errorCode : %d", streamID, errorCode);
                        Toast.makeText(PlayActivityUI.this, getString(R.string.tx_play_fail), Toast.LENGTH_SHORT).show();
                        // 当拉流失败时需要显示布局
                        showInputStreamIDLayout();
                    }
                }
            }

            @Override
            public void onPlayerQualityUpdate(String streamID, im.zego.zegoexpress.entity.ZegoPlayStreamQuality quality) {
                /**
                 * 拉流质量更新, 回调频率默认3秒一次
                 * 可通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#setPlayQualityMonitorCycle(long)} 修改回调频率
                 */
                /**
                                  * Pull stream quality update, the callback frequency defaults once every 3 seconds
                                  * The callback frequency can be modified through {@link com.zego.zegoliveroom.ZegoLiveRoom # setPlayQualityMonitorCycle (long)}
                                  */
                streamQuality.setFps(String.format(getString(R.string.frame_rate) + " %f", quality.videoRecvFPS));
                streamQuality.setBitrate(String.format(getString(R.string.bit_rate) + " %f kbs", quality.videoKBPS));
            }

            @Override
            public void onPlayerVideoSizeChanged(String streamID, int width, int height) {
                // 视频宽高变化通知,startPlay后，如果视频宽度或者高度发生变化(首次的值也会)，则收到该通知.
                // Video width and height change notification, after startPlay, if the video width or height changes (the first value will be), you will receive this notification
                streamQuality.setResolution(String.format(getString(R.string.resolution) + " %dX%d", width, height));
            }

            @Override
            public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
                /** 房间状态回调，在登录房间后，当房间状态发生变化（例如房间断开，认证失败等），SDK会通过该接口通知 */
                /** Room status update callback: after logging into the room, when the room connection status changes
                 * (such as room disconnection, login authentication failure, etc.), the SDK will notify through the callback
                 */
                AppLogger.getInstance().i("onRoomStateUpdate: roomID = " + roomID + ", state = " + state + ", errorCode = " + errorCode);
                streamQuality.setRoomID(String.format("RoomID : %s", roomID));
                if (state == ZegoRoomState.DISCONNECTED) {
                    binding.title.setTitleName(getString(R.string.loss_connect));
                } else if (state == ZegoRoomState.CONNECTED) {
                    binding.title.setTitleName("");
                }
                if (errorCode != 0) {
                    Toast.makeText(PlayActivityUI.this, String.format("login room fail, errorCode: %d", errorCode), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList) {

            }
        });


    }


    @Override
    protected void onDestroy() {
        if (mStreamID != null) {
            engine.stopPlayingStream(mStreamID);
        }

        // 当用户退出界面时退出登录房间
        engine.logoutRoom(roomID);
        engine.setEventHandler(null);
        ZegoExpressEngine.destroyEngine(null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mStreamID != null) {
            ZegoCanvas zegoCanvas = new ZegoCanvas(binding.playView);
            ZegoExpressEngine.getEngine().startPlayingStream(mStreamID, zegoCanvas);
        }
    }

    /**
     * button 点击事件触发
     * 开始拉流
     *
     * @param view
     */
    public void onStart(View view) {
        mStreamID = layoutBinding.edStreamId.getText().toString();
        roomID = layoutBinding.edRoomId.getText().toString();

        ZegoUser user = new ZegoUser(userID, userName);
        engine.loginRoom(roomID, user, null);
        // 隐藏输入StreamID布局
        hideInputStreamIDLayout();
        // 更新界面上流名
        streamQuality.setStreamID(String.format("StreamID : %s", mStreamID));
        // 开始拉流
        ZegoCanvas zegoCanvas = new ZegoCanvas(binding.playView);
        engine.startPlayingStream(mStreamID, zegoCanvas);

        AppLogger.getInstance().i("play stream fail, streamID : %s", mStreamID);
        Toast.makeText(PlayActivityUI.this, getString(R.string.tx_play_fail), Toast.LENGTH_SHORT).show();
        // 修改标题状态拉流失败状态
        binding.title.setTitleName(getString(R.string.tx_play_fail));

    }

    /**
     * 跳转到常用界面
     *
     * @param view
     */
    public void goSetting(View view) {
        PlaySettingActivityUI.actionStart(this, mStreamID);
    }


    private void hideInputStreamIDLayout() {
        // 隐藏InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.GONE);
        binding.publishStateView.setVisibility(View.VISIBLE);
    }

    private void showInputStreamIDLayout() {
        // 显示InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.VISIBLE);
        binding.publishStateView.setVisibility(View.GONE);
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, PlayActivityUI.class);
        activity.startActivity(intent);
    }
}
