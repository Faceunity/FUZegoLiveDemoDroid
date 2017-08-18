package com.zego.livedemo5.ui.activities;


import android.Manifest;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zego.livedemo5.R;
import com.zego.livedemo5.ZegoApiManager;
import com.zego.livedemo5.faceunity.EffectAndFilterSelectAdapter;
import com.zego.livedemo5.ui.activities.base.AbsBaseLiveActivity;
import com.zego.livedemo5.ui.adapters.CommentsAdapter;
import com.zego.livedemo5.ui.widgets.PublishSettingsPannel;
import com.zego.livedemo5.ui.widgets.ViewLive;
import com.zego.livedemo5.utils.LiveQualityLogger;
import com.zego.livedemo5.utils.PreferenceUtil;
import com.zego.livedemo5.utils.ZegoRoomUtil;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.im.IZegoRoomMessageCallback;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoIM;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoConversationMessage;
import com.zego.zegoliveroom.entity.ZegoRoomMessage;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;
import com.zego.zegoliveroom.entity.ZegoUserState;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;

import static com.tencent.open.utils.Global.getContext;

/**
 * des: 主页面
 */
public abstract class BaseLiveActivity extends AbsBaseLiveActivity implements View.OnClickListener {

    public static final String TAG = "BaseLiveActivity";

    protected InputStream mIsBackgroundMusic = null;

    protected LinkedList<ViewLive> mListViewLive = new LinkedList<>();

    protected TextView mTvPublisnControl = null;

    protected TextView mTvPublishSetting = null;

    protected TextView mTvSpeaker = null;

    protected EditText mEdtMessage = null;

    protected TextView mTvSendRoomMsg = null;

    protected BottomSheetBehavior mBehavior = null;

    protected RelativeLayout mRlytControlHeader = null;

    protected TextView mTvTag = null;

    protected String mPublishTitle = null;

    protected String mPublishStreamID = null;

    protected boolean mIsPublishing = false;

    protected boolean mEnableSpeaker = true;

    protected boolean mEnableCamera = true;

    protected boolean mEnableFrontCam = true;

    protected boolean mEnableMic = true;

    protected boolean mEnableTorch = false;

    protected boolean mEnableBackgroundMusic = false;

    protected boolean mEnableLoopback = false;

    protected int mSelectedBeauty = 0;

    protected int mSelectedFilter = 0;

    protected int mLiveCount = 0;

    protected boolean mHostHasBeenCalled = false;

    protected ZegoLiveRoom mZegoLiveRoom = null;

    protected String mRoomID = null;

    protected PhoneStateListener mPhoneStateListener = null;

    protected PublishSettingsPannel mSettingsPannel = null;

    protected AlertDialog mDialogHandleRequestPublish = null;

    /**
     * 推流标记, PublishFlag.JoinPublish:连麦, PublishFlag.MixStream:混流, PublishFlag.SingleAnchor:单主播
     */
    protected int mPublishFlag = ZegoConstants.PublishFlag.JoinPublish;

    /**
     * app朝向, Surface.ROTATION_0或者Surface.ROTATION_180表示竖屏推流,
     * Surface.ROTATION_90或者Surface.ROTATION_270表示横屏推流.
     */
    protected int mAppOrientation = Surface.ROTATION_0;

    protected ListView mLvComments = null;

    private CommentsAdapter mCommentsAdapter = null;

    protected List<ZegoStreamInfo> mListStreamOfRoom = new ArrayList<>();

    protected String mMixStreamID = null;

    protected List<ZegoUserState> mListRoomUser = new ArrayList<>();

    @Bind(R.id.main_bottom)
    LinearLayout mMainBottom;
    private RecyclerView mEffectRecyclerView;
    private EffectAndFilterSelectAdapter mEffectRecyclerAdapter;
    private RecyclerView mFilterRecyclerView;
    private EffectAndFilterSelectAdapter mFilterRecyclerAdapter;

    private LinearLayout mBlurLevelSelect;
    private LinearLayout mColorLevelSelect;
    private LinearLayout mFaceShapeSelect;
    private LinearLayout mRedLevelSelect;


    private Button mChooseEffectBtn;
    private Button mChooseFilterBtn;
    private Button mChooseBlurLevelBtn;
    private Button mChooseColorLevelBtn;
    private Button mChooseFaceShapeBtn;
    private Button mChooseRedLevelBtn;

    private TextView[] mBlurLevels;
    private int[] BLUR_LEVEL_TV_ID = {R.id.blur_level0, R.id.blur_level1, R.id.blur_level2,
            R.id.blur_level3, R.id.blur_level4, R.id.blur_level5, R.id.blur_level6};

    private TextView mFaceShape0Nvshen;
    private TextView mFaceShape1Wanghong;
    private TextView mFaceShape2Ziran;
    private TextView mFaceShape3Default;

    protected abstract void initPublishControlText();

    protected abstract void doPublish();

    protected abstract void hidePlayBackground();

    protected abstract void initPublishConfigs();

    protected abstract void initPlayConfigs(ViewLive viewLive, String streamID);

    protected abstract void sendRoomMessage();


    @Override
    protected void onResume() {
        super.onResume();
        startPublish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPublish();
    }
    @Override
    protected int getContentViewLayout() {
        return R.layout.activity_live;
    }


    @Override
    protected void initExtraData(Bundle savedInstanceState) {
        LiveQualityLogger.open();
    }


    @Override
    protected void initVariables(final Bundle savedInstanceState) {
        mZegoLiveRoom = ZegoApiManager.getInstance().getZegoLiveRoom();
        // 初始化电话监听器
        initPhoneCallingListener();
    }

    /**
     * 初始化设置面板.
     */
    private void initSettingPannel() {

        mSettingsPannel = (PublishSettingsPannel) findViewById(R.id.publishSettingsPannel);
        mSettingsPannel.initPublishSettings(mEnableCamera, mEnableFrontCam, mEnableMic, mEnableTorch, mEnableBackgroundMusic, mEnableLoopback, mSelectedBeauty, mSelectedFilter);
        mSettingsPannel.setPublishSettingsCallback(new PublishSettingsPannel.PublishSettingsCallback() {
            @Override
            public void onEnableCamera(boolean isEnable) {
                mEnableCamera = isEnable;
                mZegoLiveRoom.enableCamera(isEnable);
            }

            @Override
            public void onEnableFrontCamera(boolean isEnable) {
                mEnableFrontCam = isEnable;
                mZegoLiveRoom.setFrontCam(isEnable);
            }

            @Override
            public void onEnableMic(boolean isEnable) {
                mEnableMic = isEnable;
                mZegoLiveRoom.enableMic(isEnable);
            }

            @Override
            public void onEnableTorch(boolean isEnable) {
                mEnableTorch = isEnable;
                mZegoLiveRoom.enableTorch(isEnable);
            }

            @Override
            public void onEnableBackgroundMusic(boolean isEnable) {
                mEnableBackgroundMusic = isEnable;
                mZegoLiveRoom.enableAux(isEnable);

                if (!isEnable) {
                    if (mIsBackgroundMusic != null) {
                        try {
                            mIsBackgroundMusic.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mIsBackgroundMusic = null;
                    }
                }
            }

            @Override
            public void onEnableLoopback(boolean isEnable) {
                mEnableLoopback = isEnable;
                mZegoLiveRoom.enableLoopback(isEnable);
            }

            @Override
            public void onSetBeauty(int beauty) {
                mSelectedBeauty = beauty;
                mZegoLiveRoom.enableBeautifying(ZegoRoomUtil.getZegoBeauty(beauty));
            }

            @Override
            public void onSetFilter(int filter) {
                mSelectedFilter = filter;
                mZegoLiveRoom.setFilter(filter);
            }
        });

        mBehavior = BottomSheetBehavior.from(mSettingsPannel);
        FrameLayout flytMainContent = (FrameLayout) findViewById(R.id.main_content);
        if (flytMainContent != null) {
            flytMainContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                }
            });
        }
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {

        mTvSpeaker = (TextView) findViewById(R.id.tv_speaker);
        mTvPublishSetting = (TextView) findViewById(R.id.tv_publish_settings);
        mTvPublisnControl = (TextView) findViewById(R.id.tv_publish_control);
        // 初始化推流控制按钮
        initPublishControlText();
        mTvPublisnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doPublish();
            }
        });

        mRlytControlHeader = (RelativeLayout) findViewById(R.id.rlyt_control_header);

        initSettingPannel();

        final ViewLive vlBigView = (ViewLive) findViewById(R.id.vl_big_view);
        if (vlBigView != null) {
            vlBigView.setActivityHost(this);
            vlBigView.setZegoLiveRoom(mZegoLiveRoom);
            vlBigView.setShareToQQCallback(new ViewLive.IShareToQQCallback() {
                @Override
                public String getRoomID() {
                    return mRoomID;
                }
            });
            mListViewLive.add(vlBigView);
        }

        initViewList(vlBigView);

        mTvSpeaker.setSelected(!mEnableSpeaker);

        mTvSendRoomMsg = (TextView) findViewById(R.id.tv_send);
        mTvSendRoomMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRoomMessage();
            }
        });

        mEdtMessage = (EditText) findViewById(R.id.et_msg);
        mEdtMessage.setSelection(mEdtMessage.getText().length());

        mLvComments = (ListView) findViewById(R.id.lv_comments);
        mCommentsAdapter = new CommentsAdapter(this, new ArrayList<ZegoRoomMessage>());

        mLvComments.setAdapter(mCommentsAdapter);

        mTvTag = (TextView) findViewById(R.id.tv_tag);

        mRlytControlHeader.bringToFront();

        if (!(ZegoApiManager.getInstance().isUseVideoCapture() || ZegoApiManager.getInstance().isUseVideoFilter()) || !isShowFaceunityUi()) {
            mMainBottom.setVisibility(View.GONE);
            return;
        }
        mMainBottom.setVisibility(View.VISIBLE);

        mEffectRecyclerView = (RecyclerView) findViewById(R.id.effect_recycle_view);
        mEffectRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mEffectRecyclerAdapter = new EffectAndFilterSelectAdapter(mEffectRecyclerView, EffectAndFilterSelectAdapter.RECYCLEVIEW_TYPE_EFFECT);
        mEffectRecyclerAdapter.setOnItemSelectedListener(new EffectAndFilterSelectAdapter.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int itemPosition) {
                Log.d(TAG, "effect item selected " + itemPosition);
                ZegoApiManager.getInstance().getFaceunityController().onEffectItemSelected(EffectAndFilterSelectAdapter.EFFECT_ITEM_FILE_NAME[itemPosition]);
            }
        });
        mEffectRecyclerView.setAdapter(mEffectRecyclerAdapter);

        mFilterRecyclerView = (RecyclerView) findViewById(R.id.filter_recycle_view);
        mFilterRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mFilterRecyclerAdapter = new EffectAndFilterSelectAdapter(mFilterRecyclerView, EffectAndFilterSelectAdapter.RECYCLEVIEW_TYPE_FILTER);
        mFilterRecyclerAdapter.setOnItemSelectedListener(new EffectAndFilterSelectAdapter.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int itemPosition) {
                Log.d(TAG, "filter item selected " + itemPosition);
                ZegoApiManager.getInstance().getFaceunityController().onFilterSelected(EffectAndFilterSelectAdapter.FILTERS_NAME[itemPosition]);
            }
        });
        mFilterRecyclerView.setAdapter(mFilterRecyclerAdapter);

        mChooseEffectBtn = (Button) findViewById(R.id.btn_choose_effect);
        mChooseFilterBtn = (Button) findViewById(R.id.btn_choose_filter);
        mChooseBlurLevelBtn = (Button) findViewById(R.id.btn_choose_blur_level);
        mChooseColorLevelBtn = (Button) findViewById(R.id.btn_choose_color_level);
        mChooseFaceShapeBtn = (Button) findViewById(R.id.btn_choose_face_shape);
        mChooseRedLevelBtn = (Button) findViewById(R.id.btn_choose_red_level);

        mFaceShape0Nvshen = (TextView) findViewById(R.id.face_shape_0_nvshen);
        mFaceShape1Wanghong = (TextView) findViewById(R.id.face_shape_1_wanghong);
        mFaceShape2Ziran = (TextView) findViewById(R.id.face_shape_2_ziran);
        mFaceShape3Default = (TextView) findViewById(R.id.face_shape_3_default);

        mBlurLevelSelect = (LinearLayout) findViewById(R.id.blur_level_select_block);
        mColorLevelSelect = (LinearLayout) findViewById(R.id.color_level_select_block);
        mFaceShapeSelect = (LinearLayout) findViewById(R.id.lin_face_shape);
        mRedLevelSelect = (LinearLayout) findViewById(R.id.red_level_select_block);

        mBlurLevels = new TextView[BLUR_LEVEL_TV_ID.length];
        for (int i = 0; i < BLUR_LEVEL_TV_ID.length; i++) {
            final int level = i;
            mBlurLevels[i] = (TextView) findViewById(BLUR_LEVEL_TV_ID[i]);
            mBlurLevels[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setBlurLevelTextBackground(mBlurLevels[level]);
                    ZegoApiManager.getInstance().getFaceunityController().onBlurLevelSelected(level);
                }
            });
        }

        DiscreteSeekBar colorLevelSeekbar = (DiscreteSeekBar) findViewById(R.id.color_level_seekbar);
        colorLevelSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                ZegoApiManager.getInstance().getFaceunityController().onColorLevelSelected(value, 100);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });

        DiscreteSeekBar cheekThinSeekbar = (DiscreteSeekBar) findViewById(R.id.cheekthin_level_seekbar);
        cheekThinSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                ZegoApiManager.getInstance().getFaceunityController().onCheekThinSelected(value, 100);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });

        DiscreteSeekBar enlargeEyeSeekbar = (DiscreteSeekBar) findViewById(R.id.enlarge_eye_level_seekbar);
        enlargeEyeSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                ZegoApiManager.getInstance().getFaceunityController().onEnlargeEyeSelected(value, 100);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });

        DiscreteSeekBar faceShapeLevelSeekbar = (DiscreteSeekBar) findViewById(R.id.face_shape_seekbar);
        faceShapeLevelSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                ZegoApiManager.getInstance().getFaceunityController().onFaceShapeLevelSelected(value, 100);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });

        DiscreteSeekBar redLevelShapeLevelSeekbar = (DiscreteSeekBar) findViewById(R.id.red_level_seekbar);
        redLevelShapeLevelSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                ZegoApiManager.getInstance().getFaceunityController().onRedLevelSelected(value, 100);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });

        mChooseEffectBtn.setOnClickListener(this);
        mChooseFilterBtn.setOnClickListener(this);
        mChooseBlurLevelBtn.setOnClickListener(this);
        mChooseColorLevelBtn.setOnClickListener(this);
        mChooseFaceShapeBtn.setOnClickListener(this);
        mChooseRedLevelBtn.setOnClickListener(this);
        mFaceShape0Nvshen.setOnClickListener(this);
        mFaceShape1Wanghong.setOnClickListener(this);
        mFaceShape2Ziran.setOnClickListener(this);
        mFaceShape3Default.setOnClickListener(this);
    }

    private void initViewList(final ViewLive vlBigView) {

        List<ViewLive> list = new ArrayList<>();

        LinearLayout llViewList = (LinearLayout) findViewById(R.id.ll_viewlist);
        for (int i = 0, llChildListSize = llViewList.getChildCount(); i < llChildListSize; i++) {
            if (llViewList.getChildAt(i) instanceof LinearLayout) {
                LinearLayout llChildList = (LinearLayout) llViewList.getChildAt(i);

                for (int j = 0, viewLiveSize = llChildList.getChildCount(); j < viewLiveSize; j++) {
                    if (llChildList.getChildAt(j) instanceof ViewLive) {
                        final ViewLive viewLive = (ViewLive) llChildList.getChildAt(j);

                        viewLive.setActivityHost(this);
                        viewLive.setZegoLiveRoom(mZegoLiveRoom);
                        viewLive.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                viewLive.toExchangeView(vlBigView);
                            }
                        });

                        list.add((ViewLive) llChildList.getChildAt(j));
                    }
                }
            }
        }

        for (int size = list.size(), i = size - 1; i >= 0; i--) {
            mListViewLive.add(list.get(i));
        }
    }

    @Override
    protected void doBusiness(Bundle savedInstanceState) {
    }

    /**
     * 电话状态监听.
     */
    protected void initPhoneCallingListener() {
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (mHostHasBeenCalled) {
                            mHostHasBeenCalled = false;
                            recordLog(TAG + ": call state idle");
                            // 登陆频道
                            for (ViewLive viewLive : mListViewLive) {
                                if (viewLive.isPublishView()) {
                                    startPublish();
                                } else if (viewLive.isPlayView()) {
                                    startPlay(viewLive.getStreamID());
                                }
                            }
                        }

                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        recordLog(TAG + ": call state ringing");
                        mHostHasBeenCalled = true;
                        // 来电停止发布与播放
                        stopAllStream();
                        break;

                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        break;
                }
            }
        };

        TelephonyManager tm = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * 获取空闲的View用于播放或者发布.
     *
     * @return
     */
    protected ViewLive getFreeViewLive() {
        ViewLive vlFreeView = null;
        for (int i = 0, size = mListViewLive.size(); i < size; i++) {
            ViewLive viewLive = mListViewLive.get(i);
            if (viewLive.isFree()) {
                vlFreeView = viewLive;
                vlFreeView.setVisibility(View.VISIBLE);
                break;
            }
        }
        return vlFreeView;
    }

    /**
     * 释放View用于再次播放.
     *
     * @param streamID
     */
    protected void releaseLiveView(String streamID) {
        if (TextUtils.isEmpty(streamID)) {
            return;
        }

        for (int i = 0, size = mListViewLive.size(); i < size; i++) {
            ViewLive currentViewLive = mListViewLive.get(i);
            if (streamID.equals(currentViewLive.getStreamID())) {
                int j = i;
                for (; j < size - 1; j++) {
                    ViewLive nextViewLive = mListViewLive.get(j + 1);
                    if (nextViewLive.isFree()) {
                        break;
                    }

                    if (nextViewLive.isPublishView()) {
                        mZegoLiveRoom.setPreviewView(currentViewLive.getTextureView());
                    } else {
                        mZegoLiveRoom.updatePlayView(nextViewLive.getStreamID(), currentViewLive.getTextureView());
                    }

                    currentViewLive.toExchangeView(nextViewLive);
                    currentViewLive = nextViewLive;
                }
                // 标记最后一个View可用
                mListViewLive.get(j).setFree();
                break;
            }
        }
    }

    /**
     * 通过streamID查找正在publish或者play的ViewLive.
     *
     * @param streamID
     * @return
     */
    protected ViewLive getViewLiveByStreamID(String streamID) {
        if (TextUtils.isEmpty(streamID)) {
            return null;
        }

        ViewLive viewLive = null;
        for (ViewLive vl : mListViewLive) {
            if (streamID.equals(vl.getStreamID())) {
                viewLive = vl;
                break;
            }
        }

        return viewLive;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            publishStream();
                        }
                    });
                } else {


                    if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, R.string.allow_camera_permission, Toast.LENGTH_LONG).show();
                    }
                    if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, R.string.open_recorder_permission, Toast.LENGTH_LONG).show();
                    }

                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
                break;
        }
    }

    protected void publishStream() {

        if (TextUtils.isEmpty(mPublishStreamID)) {
            return;
        }

        ViewLive freeViewLive = getFreeViewLive();
        if (freeViewLive == null) {
            return;
        }

        // 设置流信息
        freeViewLive.setStreamID(mPublishStreamID);
        freeViewLive.setPublishView(true);

        // 初始化配置信息, 混流模式使用
        initPublishConfigs();

        // 输出发布状态
        recordLog(TAG + ": start publishing(" + mPublishStreamID + ")");

        // 设置水印
        mZegoLiveRoom.setWaterMarkImagePath("asset:watermark.png");
        Rect rect = new Rect();
        rect.left = 50;
        rect.top = 20;
        rect.right = 200;
        rect.bottom = 170;
        mZegoLiveRoom.setPreviewWaterMarkRect(rect);
        mZegoLiveRoom.setPublishWaterMarkRect(rect);

        // 开启流量自动控制
        int properties = ZegoConstants.ZegoTrafficControlProperty.ZEGOAPI_TRAFFIC_FPS
                | ZegoConstants.ZegoTrafficControlProperty.ZEGOAPI_TRAFFIC_RESOLUTION;
        mZegoLiveRoom.enableTrafficControl(properties, true);

        // 开始播放
        mZegoLiveRoom.setPreviewView(freeViewLive.getTextureView());
        mZegoLiveRoom.startPreview();
        mZegoLiveRoom.enableMic(mEnableMic);
        mZegoLiveRoom.enableCamera(mEnableCamera);
        mZegoLiveRoom.startPublishing(mPublishStreamID, mPublishTitle, mPublishFlag);
        mZegoLiveRoom.setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
    }

    /**
     * 开始发布.
     */
    protected void startPublish() {
        // 6.0及以上的系统需要在运行时申请CAMERA RECORD_AUDIO权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 101);
            } else {
                publishStream();
            }
        } else {
            publishStream();
        }
    }

    protected void stopPublish() {
        if (mIsPublishing) {
            // 临时处理
            handlePublishStop(1, mPublishStreamID);
            initPublishControlText();

            recordLog(TAG + ": stop publishing(" + mPublishStreamID + ")");
            mZegoLiveRoom.stopPreview();
            mZegoLiveRoom.stopPublishing();
            mZegoLiveRoom.setPreviewView(null);
        }
    }

    protected void stopPlay(String streamID) {
        if (!TextUtils.isEmpty(streamID)) {
            // 临时处理
            handlePlayStop(1, streamID);

            // 输出播放状态
            recordLog(TAG + ": stop play stream(" + streamID + ")");
            mZegoLiveRoom.stopPlayingStream(streamID);
        }
    }

    private boolean isStreamExisted(String streamID) {
        if (TextUtils.isEmpty(streamID)) {
            return true;
        }

        boolean isExisted = false;

        for (ViewLive viewLive : mListViewLive) {
            if (streamID.equals(viewLive.getStreamID())) {
                isExisted = true;
                break;
            }
        }

        return isExisted;
    }

    /**
     * 开始播放流.
     */
    protected void startPlay(String streamID) {

        if (TextUtils.isEmpty(streamID)) {
            return;
        }

        if (isStreamExisted(streamID)) {
            Toast.makeText(this, "流已存在", Toast.LENGTH_SHORT).show();
            return;
        }

        ViewLive freeViewLive = getFreeViewLive();
        if (freeViewLive == null) {
            return;
        }

        // 设置流信息
        freeViewLive.setStreamID(streamID);
        freeViewLive.setPlayView(true);

        // 输出播放状态
        recordLog(TAG + ": start play stream(" + streamID + ")");

        // 初始化拉流参数, 外部渲染模式使用
        initPlayConfigs(freeViewLive, streamID);

        // 播放
        mZegoLiveRoom.startPlayingStream(streamID, freeViewLive.getTextureView());
        mZegoLiveRoom.setViewMode(ZegoVideoViewMode.ScaleAspectFill, streamID);
    }

    protected void logout() {

        mEnableLoopback = false;
        mZegoLiveRoom.enableLoopback(false);

        if (mIsPublishing) {
            AlertDialog dialog = new AlertDialog.Builder(this).setMessage(getString(R.string.do_you_really_want_to_leave)).setTitle(getString(R.string.hint)).setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    stopAllStream();
                    dialog.dismiss();
                    finish();
                }
            }).setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).create();

            dialog.show();
        } else {

            stopAllStream();
            finish();
        }

    }


    protected void stopAllStream() {
        for (ViewLive viewLive : mListViewLive) {
            if (viewLive.isPublishView()) {
                stopPublish();
            } else if (viewLive.isPlayView()) {
                stopPlay(viewLive.getStreamID());
            }
            // 释放view
            viewLive.setFree();
        }
    }

    protected void setPublishEnabled() {
        if (!mIsPublishing) {
            if (mLiveCount < ZegoLiveRoom.getMaxPlayChannelCount()) {
                mTvPublisnControl.setEnabled(true);
            } else {
                mTvPublisnControl.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                return false;
            } else {
                // 退出
                logout();
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @OnClick(R.id.tv_log_list)
    public void openLogList() {
        LogListActivity.actionStart(this);
    }

    @OnClick(R.id.tv_publish_settings)
    public void publishSettings() {
        if (mBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @OnClick(R.id.tv_speaker)
    public void doMute() {
        if (mEnableSpeaker) {
            mEnableSpeaker = false;
        } else {
            mEnableSpeaker = true;
        }

        mZegoLiveRoom.enableSpeaker(mEnableSpeaker);
        mTvSpeaker.setSelected(!mEnableSpeaker);
    }

    @OnClick(R.id.tv_close)
    public void close() {
        logout();
    }

    /**
     * 推流成功.
     */
    protected void handlePublishSucc(String streamID) {
        mIsPublishing = true;
        recordLog(TAG + ": onPublishSucc(" + streamID + ")");

        initPublishControlText();
//        mRlytControlHeader.bringToFront();
    }

    /**
     * 停止推流.
     */
    protected void handlePublishStop(int stateCode, String streamID) {
        mIsPublishing = false;
        recordLog(TAG + ": onPublishStop(" + streamID + ") --stateCode:" + stateCode);

        // 释放View
        releaseLiveView(streamID);

        initPublishControlText();
//        mRlytControlHeader.bringToFront();
    }

    /**
     * 拉流成功.
     */
    protected void handlePlaySucc(String streamID) {
        recordLog(TAG + ": onPlaySucc(" + streamID + ")");

        mLiveCount++;
        setPublishEnabled();

        mRlytControlHeader.bringToFront();
    }

    /**
     * 停止拉流.
     */
    protected void handlePlayStop(int stateCode, String streamID) {
        recordLog(TAG + ": onPlayStop(" + streamID + ") --stateCode:" + stateCode);

        // 释放View
        releaseLiveView(streamID);

        mLiveCount--;
        setPublishEnabled();

        mRlytControlHeader.bringToFront();
    }

    /**
     * 推流质量更新.
     */
    protected void handlePublishQualityUpdate(String streamID, int quality, double videoFPS, double videoBitrate) {
        ViewLive viewLive = getViewLiveByStreamID(streamID);
        if (viewLive != null) {
            viewLive.setLiveQuality(quality, videoFPS, videoBitrate);
        }

        // for espresso test, don't delete the log
        LiveQualityLogger.write("publishStreamQuality:%d, streamId: %s, videoFPS: %.2f, videoBitrate: %.2fKb/s", quality, streamID, videoFPS, videoBitrate);
    }

    protected AuxData handleAuxCallback(int dataLen) {
        // 开启伴奏后, sdk每20毫秒一次取数据
        if (!mEnableBackgroundMusic || dataLen <= 0) {
            return null;
        }

        AuxData auxData = new AuxData();
        auxData.dataBuf = new byte[dataLen];

        try {
            AssetManager am = getAssets();
            if (mIsBackgroundMusic == null) {
                mIsBackgroundMusic = am.open("a.pcm");
            }
            int len = mIsBackgroundMusic.read(auxData.dataBuf);

            if (len <= 0) {
                // 歌曲播放完毕
                mIsBackgroundMusic.close();
                mIsBackgroundMusic = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        auxData.channelCount = 2;
        auxData.sampleRate = 44100;


        return auxData;
    }

    /**
     * 拉流质量更新.
     */
    protected void handlePlayQualityUpdate(String streamID, int quality, double videoFPS, double videoBitrate) {
        ViewLive viewLive = getViewLiveByStreamID(streamID);
        if (viewLive != null) {
            viewLive.setLiveQuality(quality, videoFPS, videoBitrate);
        }

        // for espresso test, don't delete the log
        LiveQualityLogger.write("playStreamQuality: %d, streamId: %s, videoFPS: %.2f, videoBitrate: %.2fKb/s", quality, streamID, videoFPS, videoBitrate);
    }

    /**
     * 拉流分辨率更新.
     */
    protected void handleVideoSizeChanged(String streamID, int width, int height) {
        hidePlayBackground();

        if (width > height) {
            ViewLive viewLivePlay = getViewLiveByStreamID(streamID);
            if (viewLivePlay != null) {
                if (viewLivePlay.getWidth() < viewLivePlay.getHeight()) {
                    viewLivePlay.setZegoVideoViewMode(true, ZegoVideoViewMode.ScaleAspectFit);
                    mZegoLiveRoom.setViewMode(ZegoVideoViewMode.ScaleAspectFit, streamID);
                } else {
                    viewLivePlay.setZegoVideoViewMode(true, ZegoVideoViewMode.ScaleAspectFill);
                    mZegoLiveRoom.setViewMode(ZegoVideoViewMode.ScaleAspectFill, streamID);
                }
            }
        }

//        mRlytControlHeader.bringToFront();

    }

    /**
     * 房间内用户创建流.
     */
    protected void handleStreamAdded(final ZegoStreamInfo[] listStream, final String roomID) {
        if (listStream != null && listStream.length > 0) {
            for (int i = 0; i < listStream.length; i++) {
                recordLog(listStream[i].userName + ": added stream(" + listStream[i].streamID + ")");
                startPlay(listStream[i].streamID);
            }
        }
    }

    /**
     * 房间内用户删除流.
     */
    protected void handleStreamDeleted(final ZegoStreamInfo[] listStream, final String roomID) {
        if (listStream != null && listStream.length > 0) {
            for (int i = 0; i < listStream.length; i++) {
                recordLog(listStream[i].userName + ": deleted stream(" + listStream[i].streamID + ")");
                stopPlay(listStream[i].streamID);
            }
        }
    }


    /**
     * 用户掉线.
     */
    protected void handleDisconnect(int errorCode, String roomID) {
        recordLog(TAG + ": onDisconnected, roomID:" + roomID + ", errorCode:" + errorCode);
    }

    /**
     * 用户更新.
     */
    protected void handleUserUpdate(ZegoUserState[] listUser, int updateType) {
        if (listUser != null) {
            if (updateType == ZegoIM.UserUpdateType.Total) {
                mListRoomUser.clear();
            }

            if (updateType == ZegoIM.UserUpdateType.Increase) {
                for (ZegoUserState zegoUserState : listUser) {
                    if (zegoUserState.updateFlag == ZegoIM.UserUpdateFlag.Added) {
                        mListRoomUser.add(zegoUserState);
                    } else if (zegoUserState.updateFlag == ZegoIM.UserUpdateFlag.Deleted) {
                        mListRoomUser.remove(zegoUserState);
                    }
                }
            }
        }
    }

    /**
     * 房间聊天消息.
     */
    protected void handleRecvRoomMsg(String roomID, ZegoRoomMessage[] listMsg) {

        List<ZegoRoomMessage> listTextMsg = new ArrayList<>();
        for (ZegoRoomMessage message : listMsg) {

            // 文字聊天消息
            if (message.messageType == ZegoIM.MessageType.Text && message.messageCategory == ZegoIM.MessageCategory.Chat) {
                listTextMsg.add(message);
            }

        }

        if (listTextMsg.size() > 0) {
            mCommentsAdapter.addMsgList(listTextMsg);

            mLvComments.post(new Runnable() {
                @Override
                public void run() {
                    // 滚动到最后一行
                    mLvComments.setSelection(mCommentsAdapter.getListMsg().size() - 1);
                }
            });
        }
    }

    protected void doSendRoomMsg(final String msg) {
        if (TextUtils.isEmpty(msg)) {
            Toast.makeText(this, getString(R.string.message_can_not_be_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        ZegoRoomMessage roomMessage = new ZegoRoomMessage();
        roomMessage.fromUserID = PreferenceUtil.getInstance().getUserID();
        roomMessage.fromUserName = getString(R.string.me);
        roomMessage.content = msg;
        roomMessage.messageType = ZegoIM.MessageType.Text;
        roomMessage.messageCategory = ZegoIM.MessageCategory.Chat;
        roomMessage.messagePriority = ZegoIM.MessagePriority.Default;

        mCommentsAdapter.addMsg(roomMessage);
        mLvComments.post(new Runnable() {
            @Override
            public void run() {
                // 滚动到最后一行
                mLvComments.setSelection(mCommentsAdapter.getListMsg().size() - 1);
            }
        });

        mZegoLiveRoom.sendRoomMessage(ZegoIM.MessageType.Text, ZegoIM.MessageCategory.Chat, ZegoIM.MessagePriority.Default, msg, new IZegoRoomMessageCallback() {
            @Override
            public void onSendRoomMessage(int errorCode, String roomID, long messageID) {
                if (errorCode == 0) {
                    recordLog(TAG + ": 发送房间消息成功, roomID:" + roomID);
                } else {
                    recordLog(TAG + ": 发送房间消息失败, roomID:" + roomID + ", messageID:" + messageID);
                }
            }
        });
    }

    /**
     * 会话消息.
     */
    protected void handleRecvConversationMsg(String roomID, String conversationID, ZegoConversationMessage message) {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 注销电话监听
        TelephonyManager tm = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mPhoneStateListener = null;

        // 清空回调, 避免内存泄漏
        mZegoLiveRoom.setZegoLivePublisherCallback(null);
        mZegoLiveRoom.setZegoLivePlayerCallback(null);
        mZegoLiveRoom.setZegoRoomCallback(null);

        // 退出房间
        mZegoLiveRoom.logoutRoom();
        LiveQualityLogger.close();
    }


    /**
     * 设置推流朝向.
     */
    protected void setAppOrientation() {
        // 设置app朝向
        int currentOrientation = getWindowManager().getDefaultDisplay().getRotation();
        mZegoLiveRoom.setAppOrientation(currentOrientation);

        // 设置推流配置
        ZegoAvConfig currentConfig = ZegoApiManager.getInstance().getZegoAvConfig();
        int videoWidth = currentConfig.getVideoEncodeResolutionWidth();
        int videoHeight = currentConfig.getVideoEncodeResolutionHeight();
        if (((currentOrientation == Surface.ROTATION_0 || currentOrientation == Surface.ROTATION_180) && videoWidth > videoHeight) ||
                ((currentOrientation == Surface.ROTATION_90 || currentOrientation == Surface.ROTATION_270) && videoHeight > videoWidth)) {
            currentConfig.setVideoEncodeResolution(videoHeight, videoWidth);
            currentConfig.setVideoCaptureResolution(videoHeight, videoWidth);
        }
        ZegoApiManager.getInstance().setZegoConfig(currentConfig);
    }

    /**
     * 处理页面朝向变化, 目前只针对拉流.
     */
    protected void handleConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int currentOrientation = getWindowManager().getDefaultDisplay().getRotation();
        for (ViewLive viewLive : mListViewLive) {
            if (viewLive.isPlayView()) {
                if (viewLive.isNeedToSwitchFullScreen() && viewLive.getZegoVideoViewMode() == ZegoVideoViewMode.ScaleAspectFill) {
                    if (currentOrientation == Surface.ROTATION_90 || currentOrientation == Surface.ROTATION_270) {
                        mZegoLiveRoom.setViewRotation(Surface.ROTATION_0, viewLive.getStreamID());
                    } else {
                        mZegoLiveRoom.setViewRotation(Surface.ROTATION_0, viewLive.getStreamID());
                    }
                } else {
                    mZegoLiveRoom.setViewRotation(currentOrientation, viewLive.getStreamID());
                }
            }
        }
    }

    /**
     * 获取流地址.
     */
    protected List<String> getShareUrlList(HashMap<String, Object> info) {
        List<String> listUrls = new ArrayList<>();

        if (info != null) {
            String[] hlsList = (String[]) info.get(ZegoConstants.StreamKey.HLS_URL_LST);
            if (hlsList != null && hlsList.length > 0) {
                listUrls.add(hlsList[0]);
            }

            String[] rtmpList = (String[]) info.get(ZegoConstants.StreamKey.RTMP_URL_LIST);
            if (rtmpList != null && rtmpList.length > 0) {
                listUrls.add(rtmpList[0]);
            }
        }
        return listUrls;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_choose_effect:
                setEffectFilterBeautyChooseBtnTextColor(mChooseEffectBtn);
                setEffectFilterBeautyChooseBlock(mEffectRecyclerView);
                break;
            case R.id.btn_choose_filter:
                setEffectFilterBeautyChooseBtnTextColor(mChooseFilterBtn);
                setEffectFilterBeautyChooseBlock(mFilterRecyclerView);
                break;
            case R.id.btn_choose_blur_level:
                setEffectFilterBeautyChooseBtnTextColor(mChooseBlurLevelBtn);
                setEffectFilterBeautyChooseBlock(mBlurLevelSelect);
                break;
            case R.id.btn_choose_color_level:
                setEffectFilterBeautyChooseBtnTextColor(mChooseColorLevelBtn);
                setEffectFilterBeautyChooseBlock(mColorLevelSelect);
                break;
            case R.id.btn_choose_face_shape:
                setEffectFilterBeautyChooseBtnTextColor(mChooseFaceShapeBtn);
                setEffectFilterBeautyChooseBlock(mFaceShapeSelect);
                break;
            case R.id.btn_choose_red_level:
                setEffectFilterBeautyChooseBtnTextColor(mChooseRedLevelBtn);
                setEffectFilterBeautyChooseBlock(mRedLevelSelect);
                break;
            case R.id.face_shape_0_nvshen:
                setFaceShapeBackground(mFaceShape0Nvshen);
                ZegoApiManager.getInstance().getFaceunityController().onFaceShapeSelected(0);
                break;
            case R.id.face_shape_1_wanghong:
                setFaceShapeBackground(mFaceShape1Wanghong);
                ZegoApiManager.getInstance().getFaceunityController().onFaceShapeSelected(1);
                break;
            case R.id.face_shape_2_ziran:
                setFaceShapeBackground(mFaceShape2Ziran);
                ZegoApiManager.getInstance().getFaceunityController().onFaceShapeSelected(2);
                break;
            case R.id.face_shape_3_default:
                setFaceShapeBackground(mFaceShape3Default);
                ZegoApiManager.getInstance().getFaceunityController().onFaceShapeSelected(3);
                break;
        }
    }

    private void setBlurLevelTextBackground(TextView tv) {
        mBlurLevels[0].setBackground(getResources().getDrawable(R.drawable.zero_blur_level_item_unselected));
        for (int i = 1; i < BLUR_LEVEL_TV_ID.length; i++) {
            mBlurLevels[i].setBackground(getResources().getDrawable(R.drawable.blur_level_item_unselected));
        }
        if (tv == mBlurLevels[0]) {
            tv.setBackground(getResources().getDrawable(R.drawable.zero_blur_level_item_selected));
        } else {
            tv.setBackground(getResources().getDrawable(R.drawable.blur_level_item_selected));
        }
    }

    private void setFaceShapeBackground(TextView tv) {
        mFaceShape0Nvshen.setBackground(getResources().getDrawable(R.color.unselect_gray));
        mFaceShape1Wanghong.setBackground(getResources().getDrawable(R.color.unselect_gray));
        mFaceShape2Ziran.setBackground(getResources().getDrawable(R.color.unselect_gray));
        mFaceShape3Default.setBackground(getResources().getDrawable(R.color.unselect_gray));
        tv.setBackground(getResources().getDrawable(R.color.faceunityYellow));
    }

    private void setEffectFilterBeautyChooseBlock(View v) {
        mEffectRecyclerView.setVisibility(View.INVISIBLE);
        mFilterRecyclerView.setVisibility(View.INVISIBLE);
        mFaceShapeSelect.setVisibility(View.INVISIBLE);
        mBlurLevelSelect.setVisibility(View.INVISIBLE);
        mColorLevelSelect.setVisibility(View.INVISIBLE);
        mRedLevelSelect.setVisibility(View.INVISIBLE);
        v.setVisibility(View.VISIBLE);
    }

    private void setEffectFilterBeautyChooseBtnTextColor(Button selectedBtn) {
        mChooseEffectBtn.setTextColor(getResources().getColor(R.color.colorWhite));
        mChooseColorLevelBtn.setTextColor(getResources().getColor(R.color.colorWhite));
        mChooseBlurLevelBtn.setTextColor(getResources().getColor(R.color.colorWhite));
        mChooseFilterBtn.setTextColor(getResources().getColor(R.color.colorWhite));
        mChooseFaceShapeBtn.setTextColor(getResources().getColor(R.color.colorWhite));
        mChooseRedLevelBtn.setTextColor(getResources().getColor(R.color.white));
        selectedBtn.setTextColor(getResources().getColor(R.color.faceunityYellow));
    }

    protected abstract boolean isShowFaceunityUi();
}
