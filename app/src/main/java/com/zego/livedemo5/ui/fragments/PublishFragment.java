package com.zego.livedemo5.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.zego.livedemo5.MainActivity;
import com.zego.livedemo5.R;
import com.zego.livedemo5.ZegoApiManager;
import com.zego.livedemo5.faceunity.EffectAndFilterSelectAdapter;
import com.zego.livedemo5.ui.activities.base.AbsBaseFragment;
import com.zego.livedemo5.ui.activities.gamelive.GameLiveActivity;
import com.zego.livedemo5.ui.activities.mixstream.MixStreamPublishActivity;
import com.zego.livedemo5.ui.activities.moreanchors.MoreAnchorsPublishActivity;
import com.zego.livedemo5.ui.activities.singleanchor.SingleAnchorPublishActivity;
import com.zego.livedemo5.ui.activities.wolvesgame.WolvesGameHostActivity;
import com.zego.livedemo5.ui.widgets.DialogSelectPublishMode;
import com.zego.livedemo5.utils.PreferenceUtil;
import com.zego.livedemo5.utils.SystemUtil;
import com.zego.livedemo5.utils.ZegoRoomUtil;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import butterknife.Bind;
import butterknife.OnClick;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * Copyright © 2016 Zego. All rights reserved.
 * des:
 */
public class PublishFragment extends AbsBaseFragment implements MainActivity.OnReInitSDKCallback, View.OnClickListener {
    private static final String TAG = "PublishFragment";

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
    protected void initViews(View rootView) {
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

        if (!ZegoApiManager.getInstance().isUseVideoCapture()) {
            mMainBottom.setVisibility(View.GONE);
            return;
        }
        mMainBottom.setVisibility(View.VISIBLE);

        mEffectRecyclerView = (RecyclerView) rootView.findViewById(R.id.effect_recycle_view);
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

        mFilterRecyclerView = (RecyclerView) rootView.findViewById(R.id.filter_recycle_view);
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

        mChooseEffectBtn = (Button) rootView.findViewById(R.id.btn_choose_effect);
        mChooseFilterBtn = (Button) rootView.findViewById(R.id.btn_choose_filter);
        mChooseBlurLevelBtn = (Button) rootView.findViewById(R.id.btn_choose_blur_level);
        mChooseColorLevelBtn = (Button) rootView.findViewById(R.id.btn_choose_color_level);
        mChooseFaceShapeBtn = (Button) rootView.findViewById(R.id.btn_choose_face_shape);
        mChooseRedLevelBtn = (Button) rootView.findViewById(R.id.btn_choose_red_level);

        mFaceShape0Nvshen = (TextView) rootView.findViewById(R.id.face_shape_0_nvshen);
        mFaceShape1Wanghong = (TextView) rootView.findViewById(R.id.face_shape_1_wanghong);
        mFaceShape2Ziran = (TextView) rootView.findViewById(R.id.face_shape_2_ziran);
        mFaceShape3Default = (TextView) rootView.findViewById(R.id.face_shape_3_default);

        mBlurLevelSelect = (LinearLayout) rootView.findViewById(R.id.blur_level_select_block);
        mColorLevelSelect = (LinearLayout) rootView.findViewById(R.id.color_level_select_block);
        mFaceShapeSelect = (LinearLayout) rootView.findViewById(R.id.lin_face_shape);
        mRedLevelSelect = (LinearLayout) rootView.findViewById(R.id.red_level_select_block);

        mBlurLevels = new TextView[BLUR_LEVEL_TV_ID.length];
        for (int i = 0; i < BLUR_LEVEL_TV_ID.length; i++) {
            final int level = i;
            mBlurLevels[i] = (TextView) rootView.findViewById(BLUR_LEVEL_TV_ID[i]);
            mBlurLevels[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setBlurLevelTextBackground(mBlurLevels[level]);
                    ZegoApiManager.getInstance().getFaceunityController().onBlurLevelSelected(level);
                }
            });
        }

        DiscreteSeekBar colorLevelSeekbar = (DiscreteSeekBar) rootView.findViewById(R.id.color_level_seekbar);
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

        DiscreteSeekBar cheekThinSeekbar = (DiscreteSeekBar) rootView.findViewById(R.id.cheekthin_level_seekbar);
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

        DiscreteSeekBar enlargeEyeSeekbar = (DiscreteSeekBar) rootView.findViewById(R.id.enlarge_eye_level_seekbar);
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

        DiscreteSeekBar faceShapeLevelSeekbar = (DiscreteSeekBar) rootView.findViewById(R.id.face_shape_seekbar);
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

        DiscreteSeekBar redLevelShapeLevelSeekbar = (DiscreteSeekBar) rootView.findViewById(R.id.red_level_seekbar);
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
                }, 1000);
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
}
