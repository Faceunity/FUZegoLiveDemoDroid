package im.zego.soundlevelandspectrum.ui;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import im.zego.common.util.AppLogger;
import im.zego.common.util.SettingDataUtil;
import im.zego.common.widgets.log.FloatingView;
import im.zego.soundlevelandspectrum.R;
import im.zego.soundlevelandspectrum.widget.SoundLevelAndSpectrumItem;
import im.zego.soundlevelandspectrum.widget.SpectrumView;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;

public class SoundLevelAndSpectrumMainActivity extends Activity {

    ZegoExpressEngine mSDKEngine;

    TextView mTvSoundlevelandspectrumRoomid;
    Switch mSwSoundlevelMonitor;
    Switch mSwSpectrumMonitor;
    // 本地推流的声浪的展现，需要获取该控件来设置进度值
//    To show the sound of local push, you need to get this control to set the progress value
    public ProgressBar mPbCaptureSoundLevel;
    TextView mTvSoundlevelandspectrumUserid;
    TextView mTvSoundlevelandspectrumStreamid;
    public SpectrumView mCaptureSpectrumView;
    // 使用线性布局作为容器，以动态添加所拉的流频谱和声浪展现
//    Use a linear layout as a container to dynamically add the stream spectrum and sound wave presentation
    public LinearLayout ll_container;

    String roomID = "SoundLevelRoom-1";
    String userName;
    String userID;
    String streamID;

    // 拉多条流的时候，使用list来保存展现的频谱和声浪的视图
//    When pulling multiple streams, use list to save the displayed spectrum and sound wave view
    public ArrayList<SoundLevelAndSpectrumItem> frequencySpectrumAndSoundLevelItemList = new ArrayList<>();

    private static final String TAG = "Sound-Spectrum-Activity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soundlevelandspectrum);
        /** 添加悬浮日志视图 */
        /** Add floating log view */
        FloatingView.get().add();
        /** 记录SDK版本号 */
        /** Record SDK version */
        AppLogger.getInstance().i("SDK version : %s", ZegoExpressEngine.getVersion());

        /** 生成随机的用户ID，避免不同手机使用时用户ID冲突，相互影响 */
        /** Generate random user ID to avoid user ID conflict and mutual influence when different mobile phones are used */
        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));

        mTvSoundlevelandspectrumRoomid = findViewById(R.id.tv_soundlevelandspectrum_roomid);
        mSwSoundlevelMonitor = findViewById(R.id.sw_soundlevelandspectrum_soundlevel_monitor);
        mSwSpectrumMonitor = findViewById(R.id.sw_soundlevelandspectrum_spectrum_monitor);
        mPbCaptureSoundLevel = findViewById(R.id.pb_sound_level);
        mTvSoundlevelandspectrumUserid = findViewById(R.id.tv_soundlevelandspectrum_userid);
        mTvSoundlevelandspectrumStreamid = findViewById(R.id.tv_soundlevelandspectrum_streamid);
        mCaptureSpectrumView = findViewById(R.id.soundlevelandspectrum_spectrum_view);
        ll_container = findViewById(R.id.ll_container);

        mTvSoundlevelandspectrumRoomid.setText(roomID);
        mSwSoundlevelMonitor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSDKEngine.startSoundLevelMonitor();
                } else {
                    mSDKEngine.stopSoundLevelMonitor();
                }
            }
        });
        mSwSpectrumMonitor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSDKEngine.startAudioSpectrumMonitor();
                } else {
                    mSDKEngine.stopAudioSpectrumMonitor();
                }
            }
        });
        AppLogger.getInstance().i(getString(R.string.create_zego_engine));
        // 创建引擎
        //create Engine
        mSDKEngine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), this.getApplication(), null);
        // 增加本专题所用的回调
        //Increase the callback used in this topic
        mSDKEngine.setEventHandler(new IZegoEventHandler() {

            // 由于本专题中声浪需要做动画效果，这里使用两个实例变量来保存上一次SDK声浪回调中抛出的值，以实现过度动画的效果
            // 上一次本地采集的进度值
//            Since the sound waves in this topic need to be animated, two instance variables are used here to save the value thrown in the previous SDK sound wave callback to achieve the effect of excessive animation
//            The progress value of the last local collection
            private double last_progress_captured = 0.0;
            // 默认情况SDK默认支持最多拉12路流，这里使用一个12长度的int数值来保存所拉的流监控周期
//            By default, the SDK supports up to 12 streams by default. Here, a 12-length int value is used to save the stream monitoring period.
            private HashMap<String, Float> last_stream_to_progress_value = new HashMap();

            @Override
            public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList) {
                super.onRoomStreamUpdate(roomID, updateType, streamList);
                Log.v(TAG, "onRoomStreamUpdate: roomID" + roomID + ", updateType:" + updateType.value() + ", streamList: " + streamList);
                AppLogger.getInstance().i("onRoomStreamUpdate: roomID" + roomID + ", updateType:" + updateType.value() + ", streamList: " + streamList);
                // 这里拉流之后动态添加渲染的View
//                Add the rendered view dynamically after pulling the stream here
                if (updateType == ZegoUpdateType.ADD) {
                    for (ZegoStream zegoStream : streamList) {
                        mSDKEngine.startPlayingStream(zegoStream.streamID, new ZegoCanvas(null));
                        SoundLevelAndSpectrumItem soundLevelAndSpectrumItem = new SoundLevelAndSpectrumItem(SoundLevelAndSpectrumMainActivity.this, null);
                        ll_container.addView(soundLevelAndSpectrumItem);
                        soundLevelAndSpectrumItem.getTvStreamId().setText(zegoStream.streamID);
                        soundLevelAndSpectrumItem.getTvUserId().setText(zegoStream.user.userID);
                        soundLevelAndSpectrumItem.setStreamid(zegoStream.streamID);
                        last_stream_to_progress_value.put(zegoStream.streamID, 0.0f);
                        frequencySpectrumAndSoundLevelItemList.add(soundLevelAndSpectrumItem);

                    }
                } else if (updateType == ZegoUpdateType.DELETE) {
                    for (ZegoStream zegoStream : streamList) {
                        mSDKEngine.stopPlayingStream(zegoStream.streamID);
                        Iterator<SoundLevelAndSpectrumItem> it = frequencySpectrumAndSoundLevelItemList.iterator();
                        while (it.hasNext()) {
                            SoundLevelAndSpectrumItem soundLevelAndSpectrumItemTmp = it.next();
                            if (soundLevelAndSpectrumItemTmp.getStreamid().equals(zegoStream.streamID)) {
                                it.remove();
                                ll_container.removeView(soundLevelAndSpectrumItemTmp);
                                last_stream_to_progress_value.remove(zegoStream.streamID);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCapturedSoundLevelUpdate(float soundLevel) {
                super.onCapturedSoundLevelUpdate(soundLevel);
                Log.v(TAG, "onCapturedSoundLevelUpdate:" + soundLevel);
                ValueAnimator animator = ValueAnimator.ofFloat((float) last_progress_captured, (float) soundLevel).setDuration(100);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        mPbCaptureSoundLevel.setProgress((int) ((Float) valueAnimator.getAnimatedValue()).floatValue());
                    }
                });
                animator.start();
                last_progress_captured = soundLevel;
            }

            @Override
            public void onRemoteSoundLevelUpdate(HashMap<String, Float> soundLevels) {
                super.onRemoteSoundLevelUpdate(soundLevels);
                Log.v(TAG, "onRemoteSoundLevelUpdate:" + soundLevels.size());
                Iterator<HashMap.Entry<String, Float>> it = soundLevels.entrySet().iterator();
                while (it.hasNext()) {
                    HashMap.Entry<String, Float> entry = it.next();
                    String streamid = entry.getKey();
                    Float value = entry.getValue();
                    for (final SoundLevelAndSpectrumItem soundLevelAndSpectrumItem : frequencySpectrumAndSoundLevelItemList) {
                        if (streamid.equals(soundLevelAndSpectrumItem.getStreamid())) {
                            ValueAnimator animator = ValueAnimator.ofFloat(value.floatValue(), soundLevels.get(streamid).floatValue()).setDuration(100);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                    soundLevelAndSpectrumItem.getPbSoundLevel().setProgress(((Float) (valueAnimator.getAnimatedValue())).intValue());
                                }
                            });
                            animator.start();
                            last_stream_to_progress_value.put(streamid, value);
                        }
                    }
                }
            }

            @Override
            public void onCapturedAudioSpectrumUpdate(float[] frequencySpectrum) {
                super.onCapturedAudioSpectrumUpdate(frequencySpectrum);
                Log.v(TAG, "call back onCapturedAudioSpectrumUpdate");
                mCaptureSpectrumView.updateFrequencySpectrum(frequencySpectrum);
            }

            @Override
            public void onRemoteAudioSpectrumUpdate(HashMap<String, float[]> frequencySpectrums) {
                super.onRemoteAudioSpectrumUpdate(frequencySpectrums);
                Log.v(TAG, "call back onRemoteAudioSpectrumUpdate:" + frequencySpectrums);
                Iterator<HashMap.Entry<String, float[]>> it = frequencySpectrums.entrySet().iterator();
                while (it.hasNext()) {
                    HashMap.Entry<String, float[]> entry = it.next();
                    String streamid = entry.getKey();
                    float[] values = entry.getValue();

                    for (SoundLevelAndSpectrumItem soundLevelAndSpectrumItem : frequencySpectrumAndSoundLevelItemList) {
                        if (streamid.equals(soundLevelAndSpectrumItem.getStreamid())) {
                            soundLevelAndSpectrumItem.getSpectrumView().updateFrequencySpectrum(values);
                        }
                    }
                }
            }

            @Override
            public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
                /** 房间状态回调，在登录房间后，当房间状态发生变化（例如房间断开，认证失败等），SDK会通过该接口通知 */
                /** Room status update callback: after logging into the room, when the room connection status changes
                 * (such as room disconnection, login authentication failure, etc.), the SDK will notify through the callback
                 */
                AppLogger.getInstance().i("onRoomStateUpdate: roomID = " + roomID + ", state = " + state + ", errorCode = " + errorCode);
                Log.v(TAG, "onRoomStateUpdate: errorcode:" + errorCode + ", roomID: " + roomID + ", state:" + state.value());
                if (errorCode != 0) {
                    Toast.makeText(SoundLevelAndSpectrumMainActivity.this, String.format("登陆房间失败, 错误码: %d", errorCode), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
                AppLogger.getInstance().i("onPublisherStateUpdate: errorcode:" + errorCode + ", streamID:" + streamID + ", state:" + state.value());
                Log.v(TAG, "onPublisherStateUpdate: errorcode:" + errorCode + ", streamID:" + streamID + ", state:" + state.value());

            }
        });


        userID = "userid-" + randomSuffix;
        userName = "username-" + randomSuffix;
        streamID = "streamid-" + randomSuffix;

        mTvSoundlevelandspectrumUserid.setText(userID);
        mTvSoundlevelandspectrumStreamid.setText(streamID);

        ZegoRoomConfig config = new ZegoRoomConfig();
        /* 使能用户登录/登出房间通知 */
        /* Enable notification when user login or logout */
        config.isUserStatusNotify = true;
        mSDKEngine.loginRoom(roomID, new ZegoUser(userID, userName), config);
        // 本专题展示声浪与频谱，无需推视频流
        //This topic shows sound waves and spectrum without pushing video streams
        mSDKEngine.enableCamera(false);
        mSDKEngine.startPublishingStream(streamID);

    }

    @Override
    protected void onDestroy() {

        mSDKEngine.stopAudioSpectrumMonitor();
        mSDKEngine.stopSoundLevelMonitor();


        mSDKEngine.stopPublishingStream();
        mSDKEngine.logoutRoom(roomID);
        ZegoExpressEngine.destroyEngine(null);

        super.onDestroy();
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, SoundLevelAndSpectrumMainActivity.class);
        activity.startActivity(intent);
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
