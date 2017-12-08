package com.zego.livedemo5.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ToggleButton;


import com.zego.livedemo5.MainActivity;
import com.zego.livedemo5.R;
import com.zego.livedemo5.ZegoApiManager;
import com.zego.livedemo5.ui.activities.gamelive.GameLiveActivity;
import com.zego.livedemo5.ui.activities.mixstream.MixStreamPublishActivity;
import com.zego.livedemo5.ui.activities.moreanchors.MoreAnchorsPublishActivity;
import com.zego.livedemo5.ui.activities.base.AbsBaseFragment;
import com.zego.livedemo5.ui.activities.singleanchor.SingleAnchorPublishActivity;
import com.zego.livedemo5.ui.activities.wolvesgame.WolvesGameHostActivity;
import com.zego.livedemo5.ui.widgets.DialogSelectPublishMode;
import com.zego.livedemo5.utils.PreferenceUtil;
import com.zego.livedemo5.utils.SystemUtil;
import com.zego.livedemo5.utils.ZegoRoomUtil;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;


import butterknife.Bind;
import butterknife.OnClick;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * Copyright © 2016 Zego. All rights reserved.
 * des:
 */
public class PublishFragment extends AbsBaseFragment implements MainActivity.OnReInitSDKCallback {

    @Bind(R.id.tb_enable_front_cam)
    public ToggleButton tbEnableFrontCam;

    @Bind(R.id.tb_enable_torch)
    public ToggleButton tbEnableTorch;


    @Bind(R.id.sp_filters)
    public Spinner spFilters;

    @Bind(R.id.sp_beauties)
    public Spinner spBeauties;

    @Bind(R.id.et_publish_title)
    public EditText etPublishTitle;

    @Bind(R.id.textureView)
    public TextureView textureView;


    private int mSelectedBeauty = 0;

    private int mSelectedFilter = 0;

    private ZegoLiveRoom mZegoLiveRoom;

    private boolean mHasBeenCreated = false;

    private boolean mIsVisibleToUser = false;

    private boolean mSpinnerOfBeautyInitialed = false;

    private boolean mSpinnerOfFilterInitialed = false;

    public static PublishFragment newInstance() {
        return new PublishFragment();
    }

    @Override
    protected int getContentViewLayout() {
        return R.layout.fragment_publish;
    }

    @Override
    protected void initExtraData() {

    }

    @Override
    protected void initVariables() {
        mZegoLiveRoom = ZegoApiManager.getInstance().getZegoLiveRoom();
    }

    @Override
    protected void initViews() {
        ArrayAdapter<String> beautyAdapter = new ArrayAdapter<>(mParentActivity, R.layout.item_spinner, mResources.getStringArray(R.array.beauties));
        spBeauties.setAdapter(beautyAdapter);
        spBeauties.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedBeauty = position;
                if (mSpinnerOfBeautyInitialed) {
                    mZegoLiveRoom.enableBeautifying(ZegoRoomUtil.getZegoBeauty(position));
                } else {
                    mSpinnerOfBeautyInitialed = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // 默认"全屏+美白"
        spBeauties.setSelection(3);

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(mParentActivity, R.layout.item_spinner, mResources.getStringArray(R.array.filters));
        spFilters.setAdapter(filterAdapter);
        spFilters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedFilter = position;

                if (mSpinnerOfFilterInitialed) {
                    mZegoLiveRoom.setFilter(mSelectedFilter);
                } else {
                    mSpinnerOfFilterInitialed = true;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        // 开启前置摄像头时, 手电筒不可用
        if (tbEnableFrontCam.isChecked()) {
            tbEnableTorch.setEnabled(false);
        }
        tbEnableFrontCam.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mZegoLiveRoom.setFrontCam(isChecked);

                // 开启前置摄像头时, 手电筒不可用
                if (isChecked) {
                    mZegoLiveRoom.enableTorch(false);
                    tbEnableTorch.setChecked(false);
                    tbEnableTorch.setEnabled(false);
                } else {
                    tbEnableTorch.setEnabled(true);
                }
            }
        });

        tbEnableTorch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mZegoLiveRoom.enableTorch(isChecked);
            }
        });
    }

    @Override
    protected void loadData() {
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mHasBeenCreated) {
            if (mIsVisibleToUser) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startPreview();
                    }
                }, 500);
            }
        } else {
            mHasBeenCreated = true;
        }

    }


    @Override
    public void onStop() {
        super.onStop();

        super.onPause();
        if (SystemUtil.isAppBackground()) {
//            stopPreview();
            Log.i("Foreground", "Foreground");
        } else {
            Log.i("Foreground", "Background");
            // app进入后台, 停止预览
            stopPreview();
        }
    }

    @OnClick(R.id.btn_start_publish)
    public void startPublishing() {

        String publishTitle = etPublishTitle.getText().toString();
        if (TextUtils.isEmpty(publishTitle)) {
            publishTitle = "Hello-" + PreferenceUtil.getInstance().getUserName();
        }

        hideInputWindow();

        final String publishTitleTemp = publishTitle;
        final DialogSelectPublishMode dialog = new DialogSelectPublishMode();
        dialog.setOnSelectPublishModeListener(new DialogSelectPublishMode.OnSelectPublishModeListener() {
            @Override
            public void onSingleAnchorSelect() {
                SingleAnchorPublishActivity.actionStart(mParentActivity, publishTitleTemp, tbEnableFrontCam.isChecked(), tbEnableTorch.isChecked(), mSelectedBeauty, mSelectedFilter, mParentActivity.getWindowManager().getDefaultDisplay().getRotation());
            }

            @Override
            public void onMoreAnchorsSelect() {
                MoreAnchorsPublishActivity.actionStart(mParentActivity, publishTitleTemp, tbEnableFrontCam.isChecked(), tbEnableTorch.isChecked(), mSelectedBeauty, mSelectedFilter, mParentActivity.getWindowManager().getDefaultDisplay().getRotation());
            }

            @Override
            public void onMixStreamSelect() {
                MixStreamPublishActivity.actionStart(mParentActivity, publishTitleTemp, tbEnableFrontCam.isChecked(), tbEnableTorch.isChecked(), mSelectedBeauty, mSelectedFilter, mParentActivity.getWindowManager().getDefaultDisplay().getRotation());
            }

            @Override
            public void onGameLivingSelect() {
                GameLiveActivity.actionStart(mParentActivity, publishTitleTemp, mParentActivity.getWindowManager().getDefaultDisplay().getRotation());
            }

            @Override
            public void onWolvesGameSelect() {
                WolvesGameHostActivity.actionStart(mParentActivity, publishTitleTemp, mParentActivity.getWindowManager().getDefaultDisplay().getRotation());
            }
        });

        dialog.show(mParentActivity.getFragmentManager(), "selectPublishModeDialog");
    }

    @OnClick(R.id.main_content)
    public void hideInputWindow() {
        InputMethodManager imm = (InputMethodManager) mParentActivity.getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && imm.isActive()) {
            imm.hideSoftInputFromWindow(etPublishTitle.getWindowToken(), 0);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        mIsVisibleToUser = isVisibleToUser;
        if (mHasBeenCreated) {
            if (isVisibleToUser) {

                // 6.0及以上的系统需要在运行时申请CAMERA RECORD_AUDIO权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(mParentActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission(mParentActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(mParentActivity, new String[]{
                                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 101);
                    } else {
                        startPreview();
                    }
                } else {
                    startPreview();
                }
            } else {
                stopPreview();
            }
        }
    }

    private void startPreview() {

        // 设置app朝向
        int currentOrientation = mParentActivity.getWindowManager().getDefaultDisplay().getRotation();
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

        // 设置水印
        ZegoLiveRoom.setWaterMarkImagePath("asset:watermark.png");
        Rect rect = new Rect();
        rect.left = 30;
        rect.top = 10;
        rect.right = 180;
        rect.bottom = 160;
        ZegoLiveRoom.setPreviewWaterMarkRect(rect);

        textureView.setVisibility(View.VISIBLE);
        mZegoLiveRoom.setPreviewView(textureView);
        mZegoLiveRoom.setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill);
        mZegoLiveRoom.startPreview();

        mZegoLiveRoom.setFrontCam(tbEnableFrontCam.isChecked());
        mZegoLiveRoom.enableTorch(tbEnableTorch.isChecked());
        // 设置美颜
        mZegoLiveRoom.enableBeautifying(ZegoRoomUtil.getZegoBeauty(mSelectedBeauty));
        // 设置滤镜
        mZegoLiveRoom.setFilter(mSelectedFilter);
    }

    private void stopPreview() {
        textureView.setVisibility(View.INVISIBLE);
        mZegoLiveRoom.stopPreview();
        mZegoLiveRoom.setPreviewView(null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        stopPreview();
        startPreview();
    }

    /**
     * @see MainActivity.OnReInitSDKCallback#onReInitSDK()
     */
    @Override
    public void onReInitSDK() {
        if (!mIsVisibleToUser) return;

        // 6.0及以上的系统需要在运行时申请CAMERA RECORD_AUDIO权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(mParentActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(mParentActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mParentActivity, new String[]{
                        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 101);
            } else {
                startPreview();
            }
        } else {
            startPreview();
        }
    }
}
