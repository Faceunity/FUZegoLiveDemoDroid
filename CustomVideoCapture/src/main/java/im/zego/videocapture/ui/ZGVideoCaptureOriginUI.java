package im.zego.videocapture.ui;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import im.zego.common.ui.BaseActivity;
import im.zego.videocapture.R;
import im.zego.videocapture.camera.ZegoVideoCaptureCallback;
import im.zego.videocapture.enums.CaptureOrigin;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.constants.ZegoVideoBufferType;
import im.zego.zegoexpress.entity.ZegoCustomVideoCaptureConfig;
import im.zego.zegoexpress.entity.ZegoEngineConfig;

/**
 * ZGVideoCaptureOriginUI
 * 用于选取外部采集源
 * Used to select external acquisition source
 */

public class ZGVideoCaptureOriginUI extends BaseActivity {

    private RadioGroup mCaptureTypeGroup;

    private ZegoVideoCaptureCallback captureCallback;

    private CaptureOrigin captureOrigin;

    private ZegoEngineConfig zegoEngineConfig = new ZegoEngineConfig();

    private static final int REQUEST_CODE = 1001;

    private boolean isOpenFaceUnity = true;

    public static MediaProjection mMediaProjection;
    // 屏幕采集相关类
//    Screen capture related
    private MediaProjectionManager mMediaProjectionManager;

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, ZGVideoCaptureOriginUI.class);
        activity.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zgvideo_capture_type);

        zegoEngineConfig.customVideoCaptureMainConfig = new ZegoCustomVideoCaptureConfig();
        zegoEngineConfig.customVideoCaptureMainConfig.bufferType = ZegoVideoBufferType.RAW_DATA;

        mCaptureTypeGroup = findViewById(R.id.CaptureTypeGroup);
        // 获取采集源button id
        final int[] radioCaptureTypeBtns = {R.id.RadioImage, R.id.RadioCameraYUV, R.id.RadioScreenRecord, R.id.RadioFaceUnity};

        // 设置RadioGroup组件的事件监听
        mCaptureTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedID) {
                isOpenFaceUnity = false;
                if (radioCaptureTypeBtns[0] == radioGroup.getCheckedRadioButtonId()) {
                    // 图片作为采集源，采用的数据传递类型是Surface_Texture
                    // The picture is used as the collection source, and the data transfer type used is Surface_Texture
                    zegoEngineConfig.customVideoCaptureMainConfig.bufferType = ZegoVideoBufferType.GL_TEXTURE_2D;
                    captureOrigin = CaptureOrigin.CaptureOrigin_Image; //摄像头 码流数据
                } else if (radioCaptureTypeBtns[1] == radioGroup.getCheckedRadioButtonId()) {
                    // camera作为采集源，采用的数据传递类型是YUV格式（内存拷贝）
                    // The camera is used as the acquisition source, and the data transfer type used is YUV format (memory copy)
                    zegoEngineConfig.customVideoCaptureMainConfig.bufferType = ZegoVideoBufferType.RAW_DATA;
                    captureOrigin = CaptureOrigin.CaptureOrigin_Camera; //摄像头 码流数据
                } else if (radioCaptureTypeBtns[2] == radioGroup.getCheckedRadioButtonId()) {
                    zegoEngineConfig.customVideoCaptureMainConfig.bufferType = ZegoVideoBufferType.SURFACE_TEXTURE;
                    captureOrigin = CaptureOrigin.CaptureOrigin_Screen; //摄像头 码流数据
                    if (Build.VERSION.SDK_INT < 21) {
                        Toast.makeText(ZGVideoCaptureOriginUI.this, getString(R.string.record_request), Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        // 1. 请求录屏权限，等待用户授权
                        // 1.Request screen recording permission, waiting for user authorization
                        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
                    }
                } else {
                    isOpenFaceUnity = true;
                }
            }
        });
    }


    // 2.实现请求录屏权限结果通知接口
    // 2. Implement the notification interface for requesting screen recording permission results
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            Log.d("Zego", "获取MediaProjection成功");
            //3.获取MediaProjection
            mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void JumpPublish(View view) {
        if (isOpenFaceUnity) {
            startActivity(new Intent(ZGVideoCaptureOriginUI.this, FuCaptureRenderActivity.class));
        } else {
            ZegoExpressEngine.setEngineConfig(zegoEngineConfig);
            Intent intent = new Intent(ZGVideoCaptureOriginUI.this, ZGVideoCaptureDemoUI.class);
            intent.putExtra("captureOrigin", captureOrigin.getCode());
            ZGVideoCaptureOriginUI.this.startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 销毁activity时释放工厂对象
        captureOrigin = null;
        captureCallback = null;
        mMediaProjection = null;
    }

}
