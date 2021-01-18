package im.zego.video.talk.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.gridlayout.widget.GridLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.zego.common.util.AppLogger;
import im.zego.common.util.SettingDataUtil;
import im.zego.common.widgets.log.FloatingView;
import im.zego.video.talk.R;
import im.zego.video.talk.databinding.VideoTalkBinding;
import im.zego.video.talk.utils.ScreenHelper;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;

public class ZGVideoTalkUI extends Activity {
    private VideoTalkBinding binding;
    public static final String mRoomID = "VideoTalkRoom-1";
    private ZegoExpressEngine mSDKEngine;
    private String userID;
    private String userName;
    private String mainStreamId;
    private TextureView textureView;
    private Map<String, TextureView> viewMap;
    private List<String> streamIdList;

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, ZGVideoTalkUI.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.video_talk);
        binding.setActivity(this);
        /** 添加悬浮日志视图 */
        /** Add floating log view */
        FloatingView.get().add();
        /** 记录SDK版本号 */
        /** Record SDK version */
        AppLogger.getInstance().i("SDK version : %s", ZegoExpressEngine.getVersion());
        initView();
        createEngine();
        loginRoomAndPublishStream();
    }

    private void loginRoomAndPublishStream() {
        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "user" + randomSuffix;
        userName = "userName" + randomSuffix;
        mainStreamId = "streamId" + randomSuffix;
        streamIdList.add(mainStreamId);
        viewMap.put(mainStreamId, textureView);
        ZegoRoomConfig config = new ZegoRoomConfig();
        /* 使能用户登录/登出房间通知 */
        /* Enable notification when user login or logout */
        config.isUserStatusNotify = true;
        mSDKEngine.loginRoom(mRoomID, new ZegoUser(userID, userName), config);
        AppLogger.getInstance().i("startPublishStream streamId:" + mainStreamId);
        ZegoCanvas zegoCanvas = new ZegoCanvas(viewMap.get(mainStreamId));
        zegoCanvas.viewMode = ZegoViewMode.ASPECT_FIT;
        // 设置预览视图及视图展示模式
        mSDKEngine.startPreview(zegoCanvas);
        mSDKEngine.startPublishingStream(mainStreamId);
    }

    private void createEngine() {
        mSDKEngine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), this.getApplication(), null);
        mSDKEngine.setEventHandler(zegoEventHandler);
        mSDKEngine.enableCamera(true);//打开摄像头
        mSDKEngine.muteMicrophone(false);//打开麦克风
        mSDKEngine.muteSpeaker(false);//开启音频输出
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

            } else if (state == ZegoRoomState.DISCONNECTED) {
                binding.roomConnectState.setText(getString(R.string.room_unconnect));
            }
            if (errorCode != 0) {
                Toast.makeText(ZGVideoTalkUI.this, String.format("login room fail, errorCode: %d", errorCode), Toast.LENGTH_LONG).show();
            }

        }

        @Override
        public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
            AppLogger.getInstance().i("onPlayerStateUpdate: streamID = " + streamID + ", state = " + state + ", errCode = " + errorCode);
        }

        @Override
        public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
            AppLogger.getInstance().i("onPublisherStateUpdate: streamID = " + streamID + ", state = " + state + ", errCode = " + errorCode);
        }

        @Override
        public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList) {
            super.onRoomStreamUpdate(roomID, updateType, streamList);
            AppLogger.getInstance().i("onRoomStreamUpdate: roomID" + roomID + ", updateType:" + updateType.value() + ", streamList: " + streamList);
            // 这里拉流之后动态添加渲染的View
            if (updateType == ZegoUpdateType.ADD) {
                for (ZegoStream zegoStream : streamList) {
                    AppLogger.getInstance().i("onRoomStreamUpdate: ZegoUpdateType.ADD streamId:" + zegoStream.streamID);
                    TextureView addTextureView = new TextureView(ZGVideoTalkUI.this);
                    int row = streamIdList.size() / 2;
                    int column = streamIdList.size() % 2;
                    addToGridLayout(row, column, addTextureView);
                    viewMap.put(zegoStream.streamID, addTextureView);
                    streamIdList.add(zegoStream.streamID);
                    mSDKEngine.startPlayingStream(zegoStream.streamID, new ZegoCanvas(addTextureView));
                }
            } else if (updateType == ZegoUpdateType.DELETE) {// callback in UIThread
                for (ZegoStream zegoStream : streamList) {
                    AppLogger.getInstance().i("onRoomStreamUpdate:  ZegoUpdateType.DELETE streamId:" + zegoStream.streamID);
                    mSDKEngine.stopPlayingStream(zegoStream.streamID);
                    streamIdList.remove(zegoStream.streamID);
                    notifyGridLayout();
                    viewMap.remove(zegoStream.streamID);
                }
            }
        }
    };

    private void notifyGridLayout() {
        int j = 0;
        binding.gridLayout.removeAllViews();
        for (String streamId : streamIdList) {
            int row = j / 2;
            int column = j % 2;
            addToGridLayout(row, column, viewMap.get(streamId));
            j++;
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
        binding.gridLayout.removeView(textureView);
        viewMap.remove(mainStreamId);
        streamIdList.clear();
    }

    private void initView() {
        viewMap = new HashMap<>();
        streamIdList = new ArrayList<>();
        binding.roomId.setText("roomId:" + mRoomID);
        binding.roomConnectState.setText(getString(R.string.room_unconnect));
        initGridLayout();
    }

    private void initGridLayout() {
        binding.gridLayout.setRowCount(30);//默认最大是30行,一共30*2共60个窗口
        binding.gridLayout.setColumnCount(2);
        textureView = new TextureView(this);
        addToGridLayout(0, 0, textureView);
    }

    public void addToGridLayout(int row, int column, TextureView textureView) {
        //设置它的行 和 权重 有了权重才能水平均匀分布
        //由于方法重载，注意这个地方的1.0f 必须是float，
        GridLayout.Spec rowSpec = GridLayout.spec(row, 1.0f);//行
        GridLayout.Spec columnSpec = GridLayout.spec(column, 1.0f);//列
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, columnSpec);
        params.setGravity(Gravity.CENTER);
        params.setMargins(10, 10, 10, 10);//px
        params.height = (int) ((ScreenHelper.getSingleton(this.getApplication()).getScreenWidthPixels() / 2 - 20) * 1.6);//px
        params.width = ScreenHelper.getSingleton(this.getApplication()).getScreenWidthPixels() / 2 - 20;
        binding.gridLayout.addView(textureView, params);
    }

    public void operateCamera(Boolean isChecked) {
        if (mSDKEngine != null) {
            mSDKEngine.enableCamera(isChecked);
        }
    }

    public void operateMic(Boolean isChecked) {
        if (mSDKEngine != null) {
            mSDKEngine.muteMicrophone(!isChecked);
        }
    }

    public void operateSpeaker(Boolean isChecked) {
        if (mSDKEngine != null) {
            mSDKEngine.muteSpeaker(!isChecked);
        }
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
