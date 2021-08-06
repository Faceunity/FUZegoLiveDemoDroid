package im.zego.mediaplayer.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Date;

import im.zego.common.util.AppLogger;
import im.zego.common.util.SettingDataUtil;
import im.zego.common.widgets.log.FloatingView;
import im.zego.mediaplayer.R;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;

public class MediaplayerMainActivity extends AppCompatActivity {

    ZegoExpressEngine mSDKEngine;

    String roomID = "Mediaplayer-1";
    String userName;
    String userID;
    String streamID;
    public static final String[] sTitle = new String[]{"Player1", "Player2", "Player3", "Player4"};


    IZegoEventHandler mIZegoEventHandler = new IZegoEventHandler() {
        // 本专题主要展示如何使用 Mediaplayer, 顾不考虑 IZegoEventHandler 的处理情况, 开发者应根据自身业务场景进行处理
        //This topic mainly shows how to use Mediaplayer, regardless of the processing situation of IZegoEventHandler, developers should handle it according to their own business scenarios
    };


    TextureView textureViewMediaplayerView1;
    TextureView textureViewMediaplayerView2;
    TextureView textureViewMediaplayerView3;
    TextureView textureViewMediaplayerView4;


    static ArrayList<TextureView> mediaplayerViews = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /** 添加悬浮日志视图 */
        /** Add floating log view */
        FloatingView.get().add();
        /** 记录SDK版本号 */
        /** Record SDK version */
        AppLogger.getInstance().i("SDK version : %s", ZegoExpressEngine.getVersion());
        /* 生成随机的用户ID，避免不同手机使用时用户ID冲突，相互影响 */
        /* Generate random user ID to avoid user ID conflict and mutual influence when different mobile phones are used */
        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "userid-" + randomSuffix;
        userName = "username-" + randomSuffix;
        streamID = "streamid-" + randomSuffix;
        AppLogger.getInstance().i(getString(R.string.create_zego_engine));
        // 创建 ZEGO 引擎对象
        mSDKEngine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), this.getApplication(), null);
        mSDKEngine.setEventHandler(mIZegoEventHandler);
        ZegoRoomConfig config = new ZegoRoomConfig();
        /* 使能用户登录/登出房间通知 */
        /* Enable notification when user login or logout */
        config.isUserStatusNotify = true;
        mSDKEngine.loginRoom(roomID, new ZegoUser(userID, userName), config);

        initUI();

    }

    private void initUI() {
        setContentView(R.layout.activity_mediaplayer_main);

        ((Switch) findViewById(R.id.sw_mediaplayer_publish)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSDKEngine.startPublishingStream(((EditText) findViewById(R.id.et_mediaplayer_publish_streamid)).getText().toString().trim());
                    mSDKEngine.startPreview(new ZegoCanvas(findViewById(R.id.tv_local_preview)));
                } else {
                    mSDKEngine.stopPublishingStream();
                    mSDKEngine.startPreview(null);
                }
            }
        });

        ((Switch) findViewById(R.id.sw_mediaplayer_play)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSDKEngine.startPlayingStream(((EditText) findViewById(R.id.et_mediaplayer_play_streamid)).getText().toString().trim(),
                            new ZegoCanvas(findViewById(R.id.tv_mediaplayer_play_stream_view)));
                } else {
                    mSDKEngine.stopPlayingStream(((EditText) findViewById(R.id.et_mediaplayer_play_streamid)).getText().toString().trim());
                }
            }
        });

        ((Switch) findViewById(R.id.sw_mediaplayer_mic)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSDKEngine.muteAudioOutput(isChecked);
            }
        });

        ((Switch) findViewById(R.id.sw_mediaplayer_cam)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSDKEngine.enableCamera(!isChecked);
            }
        });

        ((EditText) findViewById(R.id.et_mediaplayer_publish_streamid)).setText(streamID);
        ((EditText) findViewById(R.id.et_mediaplayer_play_streamid)).setText(streamID);
        textureViewMediaplayerView1 = findViewById(R.id.tv_mediaplayer_player_view_1);
        mediaplayerViews.add(textureViewMediaplayerView1);
        textureViewMediaplayerView2 = findViewById(R.id.tv_mediaplayer_player_view_2);
        mediaplayerViews.add(textureViewMediaplayerView2);
        textureViewMediaplayerView3 = findViewById(R.id.tv_mediaplayer_player_view_3);
        mediaplayerViews.add(textureViewMediaplayerView3);
        textureViewMediaplayerView4 = findViewById(R.id.tv_mediaplayer_player_view_4);
        mediaplayerViews.add(textureViewMediaplayerView4);

        final MediaPlayerPanelFragment mediaPlayerPanelFragment1 = (MediaPlayerPanelFragment) getSupportFragmentManager().findFragmentById(R.id.frg_mediaplayer_panal1);
        if (mediaPlayerPanelFragment1 != null) {
            mediaPlayerPanelFragment1.setZegoExpressEngine(mSDKEngine);
        }
        final MediaPlayerPanelFragment mediaPlayerPanelFragment2 = (MediaPlayerPanelFragment) getSupportFragmentManager().findFragmentById(R.id.frg_mediaplayer_panal2);
        if (mediaPlayerPanelFragment2 != null) {
            mediaPlayerPanelFragment2.setZegoExpressEngine(mSDKEngine);
        }
        final MediaPlayerPanelFragment mediaPlayerPanelFragment3 = (MediaPlayerPanelFragment) getSupportFragmentManager().findFragmentById(R.id.frg_mediaplayer_panal3);
        if (mediaPlayerPanelFragment3 != null) {
            mediaPlayerPanelFragment3.setZegoExpressEngine(mSDKEngine);
        }
        final MediaPlayerPanelFragment mediaPlayerPanelFragment4 = (MediaPlayerPanelFragment) getSupportFragmentManager().findFragmentById(R.id.frg_mediaplayer_panal4);
        if (mediaPlayerPanelFragment4 != null) {
            mediaPlayerPanelFragment4.setZegoExpressEngine(mSDKEngine);
        }
        TabLayout mTabLayout = findViewById(R.id.tab_mediaplayer_layout);

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int index = tab.getPosition();
                MediaPlayerPanelFragment.index = index;
                mediaPlayerPanelFragment1.setVisible(false);
                mediaPlayerPanelFragment2.setVisible(false);
                mediaPlayerPanelFragment3.setVisible(false);
                mediaPlayerPanelFragment4.setVisible(false);

                switch (index) {
                    case 0:
                        mediaPlayerPanelFragment1.setVisible(true);
                        break;
                    case 1:
                        mediaPlayerPanelFragment2.setVisible(true);
                        break;
                    case 2:
                        mediaPlayerPanelFragment3.setVisible(true);
                        break;
                    case 3:
                        mediaPlayerPanelFragment4.setVisible(true);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mTabLayout.addTab(mTabLayout.newTab().setText(sTitle[0]));
        mTabLayout.addTab(mTabLayout.newTab().setText(sTitle[1]));
        mTabLayout.addTab(mTabLayout.newTab().setText(sTitle[2]));
        mTabLayout.addTab(mTabLayout.newTab().setText(sTitle[3]));
    }

    @Override
    protected void onDestroy() {
        mediaplayerViews.clear();
        mSDKEngine.stopPlayingStream(streamID);
        mSDKEngine.stopPublishingStream();
        mSDKEngine.logoutRoom(roomID);
        mSDKEngine.setEventHandler(null);
        ZegoExpressEngine.destroyEngine(null);
        mIZegoEventHandler = null;
        mSDKEngine = null;
        super.onDestroy();
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, MediaplayerMainActivity.class);
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
