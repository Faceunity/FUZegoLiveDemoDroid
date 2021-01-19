package im.zego.mediaplayer.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.nio.ByteBuffer;

import im.zego.common.util.AppLogger;
import im.zego.mediaplayer.R;
import im.zego.mediaplayer.tools.CommonTools;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.ZegoMediaPlayer;
import im.zego.zegoexpress.callback.IZegoMediaPlayerAudioHandler;
import im.zego.zegoexpress.callback.IZegoMediaPlayerEventHandler;
import im.zego.zegoexpress.callback.IZegoMediaPlayerLoadResourceCallback;
import im.zego.zegoexpress.callback.IZegoMediaPlayerVideoHandler;
import im.zego.zegoexpress.constants.ZegoMediaPlayerNetworkEvent;
import im.zego.zegoexpress.constants.ZegoMediaPlayerState;
import im.zego.zegoexpress.constants.ZegoVideoFrameFormat;
import im.zego.zegoexpress.entity.ZegoAudioFrameParam;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoVideoFrameParam;


public class MediaPlayerPanelFragment extends Fragment {

    private ZegoMediaPlayer mMediaplayer;
    private ZegoExpressEngine mEngine;
    private Activity mActivity;

    private Switch aSwitchMediaplayerCreate;
    private Switch aSwitchMediaplayerRepeat;
    private Switch aSwitchMediaplayerStartOrStop;
    private Switch aSwitchMediaplayerResumeOrPause;
    private Switch aSwitchMediaplayerMuteAudio;
    private Switch aSwitchMediaplayerAuxAudio;
    private Switch aSwitchMediaplayerVidoData;
    private Switch aSwitchMediaplayerAudioData;
    private Button aButtonLoadResource;
    private ProgressBar aProgressBar;

    static int index = 0;

    private static final String TAG = "MediaPlayerPanelFragmen";

    private long currentResourceTotalDuration;

    View mPanel;

    void setZegoExpressEngine(ZegoExpressEngine engine) {
        mEngine = engine;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mPanel = inflater.inflate(R.layout.mediapalyer_panel_layout, container, false);
        return mPanel;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();
        initMediaplayerPanelUI();
    }

    void setVisible(boolean visible) {
        if (visible) {
            mPanel.setVisibility(View.VISIBLE);
        } else {
            mPanel.setVisibility(View.INVISIBLE);
        }
    }

    private void initMediaplayerPanelUI() {

        if (mActivity != null) {
            aSwitchMediaplayerCreate = mPanel.findViewById(R.id.sw_mediaplayer_create);
            aSwitchMediaplayerRepeat = mPanel.findViewById(R.id.sw_mediaplayer_repeat);
            aSwitchMediaplayerStartOrStop = mPanel.findViewById(R.id.sw_mediaplayer_start_or_stop);
            aSwitchMediaplayerResumeOrPause = mPanel.findViewById(R.id.sw_mediaplayer_resume_or_pause);
            aSwitchMediaplayerMuteAudio = mPanel.findViewById(R.id.sw_mediaplayer_mute_audio);
            aSwitchMediaplayerAuxAudio = mPanel.findViewById(R.id.sw_mediaplayer_aux_audio);
            aSwitchMediaplayerVidoData = mPanel.findViewById(R.id.sw_mediaplayer_video_data);
            aSwitchMediaplayerAudioData = mPanel.findViewById(R.id.sw_mediaplayer_audio_data);
            aButtonLoadResource = mPanel.findViewById(R.id.btn_load_video_resource);
            aProgressBar = mPanel.findViewById(R.id.pb_cur_res_progress);
        }

        aSwitchMediaplayerCreate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mEngine != null) {
                    if (isChecked) {
                        mMediaplayer = ZegoMediaPlayer.createMediaPlayer();
                        mMediaplayer.setEventHandler(new IZegoMediaPlayerEventHandler() {

                            @Override
                            public void onMediaPlayerNetworkEvent(ZegoMediaPlayer mediaPlayer, ZegoMediaPlayerNetworkEvent networkEvent) {
                                AppLogger.getInstance().i("onMediaPlayerNetworkEvent: " + networkEvent);
                                Log.d(TAG, "onMediaPlayerNetworkEvent: " + networkEvent);
                                Toast.makeText(mActivity, "onMediaPlayerNetworkEvent: " + networkEvent, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onMediaPlayerPlayingProgress(ZegoMediaPlayer mediaPlayer, long millisecond) {
                                Log.d(TAG, "onMediaPlayerPlayingProgress: millisecond = " + millisecond + ", currentResourceTotalDuration = " + currentResourceTotalDuration);
                                aProgressBar.setProgress((int) (100 * (millisecond) / (double) currentResourceTotalDuration));
                            }

                            @Override
                            public void onMediaPlayerStateUpdate(ZegoMediaPlayer mediaPlayer, ZegoMediaPlayerState state, int errorCode) {
                                AppLogger.getInstance().i("onMediaPlayerStateUpdate: state = " + state + ", errorCode = " + errorCode);
                                Log.d(TAG, "onMediaPlayerStateUpdate: state = " + state + ", errorCode = " + errorCode);
                                if (errorCode != 0) {
                                    Toast.makeText(mActivity,
                                            "onMediaPlayerStateUpdate: state = " + state + ", errorCode = " + errorCode,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    } else {
                        mMediaplayer.destroyMediaPlayer();
                        mMediaplayer.setEventHandler(null);
                        mMediaplayer = null;
                    }
                }
            }
        });

        aSwitchMediaplayerRepeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mMediaplayer != null) {
                    mMediaplayer.enableRepeat(isChecked);
                }
            }
        });

        aSwitchMediaplayerStartOrStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mMediaplayer != null) {
                    if (isChecked) {
                        mMediaplayer.setPlayerCanvas(new ZegoCanvas(MediaplayerMainActivity.mediaplayerViews.get(index)));
                        mMediaplayer.start();
                    } else {
                        mMediaplayer.setPlayerCanvas(null);
                        mMediaplayer.stop();
                    }
                }
            }
        });

        aSwitchMediaplayerResumeOrPause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mMediaplayer != null) {
                    if (isChecked) {
                        mMediaplayer.pause();
                    } else {
                        mMediaplayer.resume();
                    }
                }
            }
        });

        aSwitchMediaplayerMuteAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mMediaplayer != null) {
                    mMediaplayer.muteLocal(isChecked);
                }
            }
        });

        aSwitchMediaplayerAuxAudio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mMediaplayer != null) {
                    mMediaplayer.enableAux(isChecked);
                }
            }
        });

        aSwitchMediaplayerVidoData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mMediaplayer != null) {
                    if (isChecked) {
                        mMediaplayer.setVideoHandler(new IZegoMediaPlayerVideoHandler() {
                            @Override
                            public void onVideoFrame(ZegoMediaPlayer zegoMediaplayer, ByteBuffer[] byteBuffers, int[] ints, ZegoVideoFrameParam zegoVideoFrameParam) {
                                Log.d(TAG, "App onVideoFrame:" + zegoVideoFrameParam.format.value());
                            }
                        }, ZegoVideoFrameFormat.Unknown);
                    } else {
                        mMediaplayer.setVideoHandler(null, ZegoVideoFrameFormat.Unknown);
                    }
                }
            }
        });

        aSwitchMediaplayerAudioData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mMediaplayer != null) {
                    if (isChecked) {
                        mMediaplayer.setAudioHandler(new IZegoMediaPlayerAudioHandler() {
                            @Override
                            public void onAudioFrame(ZegoMediaPlayer zegoMediaplayer, ByteBuffer byteBuffer, int i, ZegoAudioFrameParam zegoAudioFrameParam) {
                                Log.d(TAG, "App onAudioFrame:");
                            }
                        });
                    } else {
                        mMediaplayer.setAudioHandler(null);
                    }
                }
            }
        });

        aButtonLoadResource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaplayer != null) {
                    if (((RadioButton) mPanel.findViewById(R.id.rb_mediapalyer_local_resource)).isChecked()) {
                        mMediaplayer.loadResource(CommonTools.getPath(mActivity, "ad.mp4"), new IZegoMediaPlayerLoadResourceCallback() {
                            @Override
                            public void onLoadResourceCallback(int i) {
                                if (i != 0) {
                                    Log.e(TAG, "onLoadResourceCallback:" + i);
                                    AppLogger.getInstance().i("onLoadResourceCallback:" + i);
                                    Toast.makeText(mActivity, getString(R.string.local_res_error) + i, Toast.LENGTH_LONG).show();
                                }
                                // 只有在加载成功之后 getTotalDuration 才会返回正常的数值
                                //Only after the load is successful, getTotalDuration will return to the normal value
                                currentResourceTotalDuration = mMediaplayer.getTotalDuration();
                                AppLogger.getInstance().i("currentResourceTotalDuration: " + currentResourceTotalDuration);
                                Log.d(TAG, "currentResourceTotalDuration: " + currentResourceTotalDuration);
                                Toast.makeText(mActivity, "currentResourceTotalDuration: " + currentResourceTotalDuration, Toast.LENGTH_LONG).show();

                            }
                        });
                    } else if (((RadioButton) mPanel.findViewById(R.id.rb_mediapalyer_net_resource)).isChecked()) {
                        mMediaplayer.loadResource("https://storage.zego.im/demo/201808270915.mp4", new IZegoMediaPlayerLoadResourceCallback() {
                            @Override
                            public void onLoadResourceCallback(int i) {
                                // 只有在加载成功之后 getTotalDuration 才会返回正常的数值
                                //Only after the load is successful, getTotalDuration will return to the normal value
                                if (i != 0) {
                                    Log.e(TAG, "onLoadResourceCallback:" + i);
                                    AppLogger.getInstance().i("onLoadResourceCallback:" + i);
                                    Toast.makeText(mActivity, getString(R.string.net_res_error) + i, Toast.LENGTH_LONG).show();
                                }
                                currentResourceTotalDuration = mMediaplayer.getTotalDuration();
                                Log.d(TAG, "currentResourceTotalDuration: " + currentResourceTotalDuration);
                                AppLogger.getInstance().i("currentResourceTotalDuration: " + currentResourceTotalDuration);
                                Toast.makeText(mActivity, "currentResourceTotalDuration: " + currentResourceTotalDuration, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        throw new RuntimeException("选项异常");
                    }

                }
            }
        });

    }
}
