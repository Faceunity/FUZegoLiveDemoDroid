package im.zego.videocapture.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.faceunity.nama.FURenderer;
import com.faceunity.nama.module.IMakeupModule;
import com.faceunity.nama.ui.FaceUnityView;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import im.zego.common.util.AppLogger;
import im.zego.common.util.DeviceInfoManager;
import im.zego.common.util.SettingDataUtil;
import im.zego.videocapture.FaceUnity.CameraRenderer;
import im.zego.videocapture.FaceUnity.CameraUtils;
import im.zego.videocapture.FaceUnity.gles.core.GlUtil;
import im.zego.videocapture.R;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerMediaEvent;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoVideoBufferType;
import im.zego.zegoexpress.constants.ZegoVideoFrameFormat;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoCustomVideoCaptureConfig;
import im.zego.zegoexpress.entity.ZegoEngineConfig;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;
import im.zego.zegoexpress.entity.ZegoVideoConfig;
import im.zego.zegoexpress.entity.ZegoVideoFrameParam;

/**
 * FaceUnity 接入 activity,采用自定义本地采集和渲染
 */
public class FuCaptureRenderActivity extends AppCompatActivity implements CameraRenderer.OnRendererStatusListener, SensorEventListener {
    private static final String TAG = "FuCaptureRenderActivity";
    private static final int DEFAULT_VIDEO_WIDTH = 360;
    private static final int DEFAULT_VIDEO_HEIGHT = 640;

    private GLSurfaceView mPreView;
    private TextureView mPlayView;
    private TextView mErrorTxt;
    private Button mDealBtn;
    private Button mDealPlayBtn;

    private String mRoomID = "zgvc_";
    private String mRoomName = "VideoExternalCaptureDemo";
    private String mPlayStreamID = "";

    private Matrix mPlayMatrix;
    private boolean isLoginSuccess = false;
    private ZegoExpressEngine mSDKEngine;
    private String userID;
    private String userName;
    private CameraRenderer mCameraRenderer;
    private FURenderer mFURenderer;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fu_capture_render);

        mPreView = findViewById(R.id.pre_view);
        mPlayView = findViewById(R.id.play_view);
        mErrorTxt = findViewById(R.id.error_txt);
        mDealBtn = findViewById(R.id.publish_btn);
        mDealPlayBtn = findViewById(R.id.play_btn);

        //镜像翻转预览view
        mPlayView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (mPlayView.getWidth() != 0) {
                    mPlayMatrix = mPlayView.getTransform(new Matrix());
                    mPlayMatrix.postScale(-1, 1, mPlayView.getWidth() / 2, 0);
                    mPlayView.setTransform(mPlayMatrix);
                    mPlayView.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });

        mRoomID += String.valueOf((int) (Math.random() * 1000));
        // 获取设备唯一ID
        String deviceID = DeviceInfoManager.generateDeviceId(this);
        mRoomID += deviceID;
        mPlayStreamID = mRoomID;
        // 初始化SDK登录房间
        // Initialize SDK login room
        initSDK();

        // 开始推流
        // start publish stream
        doPublish();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    private void initSDK() {
        // 创建sdk
        AppLogger.getInstance().i(getString(R.string.create_zego_engine));
        ZegoEngineConfig zegoEngineConfig = new ZegoEngineConfig();
        zegoEngineConfig.customVideoCaptureMainConfig = new ZegoCustomVideoCaptureConfig();
        zegoEngineConfig.customVideoCaptureMainConfig.bufferType = ZegoVideoBufferType.RAW_DATA;
        ZegoExpressEngine.setEngineConfig(zegoEngineConfig);
        mSDKEngine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), this.getApplication(), null);
        mSDKEngine.setEventHandler(zegoEventHandler);

        mPreView.setEGLContextClientVersion(GlUtil.getSupportGLVersion(this));
        mCameraRenderer = new CameraRenderer(this, mPreView, this);
        mSDKEngine.setCustomVideoCaptureHandler(mCameraRenderer);

        ZegoRoomConfig config = new ZegoRoomConfig();
        /* 使能用户登录/登出房间通知 */
        /* Enable notification when user login or logout */
        config.isUserStatusNotify = true;
        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "user" + randomSuffix;
        userName = "userName" + randomSuffix;
        mSDKEngine.loginRoom(mRoomID, new ZegoUser(userID, userName), config);

        mFURenderer = new FURenderer.Builder(this)
                .setInputTextureType(FURenderer.INPUT_TEXTURE_EXTERNAL_OES)
                .setCameraFacing(FURenderer.CAMERA_FACING_FRONT)
                .setInputImageOrientation(CameraUtils.getCameraOrientation(FURenderer.CAMERA_FACING_FRONT))
                .setRunBenchmark(false)
                .setOnDebugListener(new FURenderer.OnDebugListener() {
                    @Override
                    public void onFpsChanged(double fps, double callTime) {
                        Log.d(TAG, "send buffer onFpsChanged FPS: " + String.format("%.2f", fps) + ", callTime: " + String.format("%.2f", callTime));
                    }
                })
                .build();
        FaceUnityView faceUnityView = findViewById(R.id.faceUnityView);
        faceUnityView.setModuleManager(mFURenderer);
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
                isLoginSuccess = true;
                mErrorTxt.setText("");
            } else if (state == ZegoRoomState.DISCONNECTED) {
                mErrorTxt.setText("login room fail, err:" + errorCode);
            }
        }

        @Override
        public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
            AppLogger.getInstance().i("onPublisherStateUpdate: streamID = " + streamID + ", state = " + state + ", errCode = " + errorCode);
            if (state == ZegoPublisherState.PUBLISH_REQUESTING) {
                mDealBtn.setText("StopPublish");
            }
        }

        @Override
        public void onPlayerMediaEvent(String streamID, ZegoPlayerMediaEvent event) {
            if (event == ZegoPlayerMediaEvent.VIDEO_BREAK_OCCUR) {
                runOnUiThread(() -> {
                    mErrorTxt.setText("play stream fail，err：" + event.value());
                });
            } else if (event == ZegoPlayerMediaEvent.VIDEO_BREAK_RESUME) {
                runOnUiThread(() -> {
                    mErrorTxt.setText("");
                    mDealPlayBtn.setText("StopPlay");
                });
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (null != mPlayMatrix) {
            mPlayView.setTransform(mPlayMatrix);
        }
        //打开加速度传感器,将设备方向传给FURenderer
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mCameraRenderer.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraRenderer.onPause();
        //注销传感器监听
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZegoExpressEngine.setEngineConfig(null);
        // 登出房间并释放ZEGO SDK
        // Log out of the room and release the ZEGO SDK
        logoutLiveRoom();
    }

    private static final int ENCODE_FRAME_WIDTH = 960;
    private static final int ENCODE_FRAME_HEIGHT = 540;
    private static final int ENCODE_FRAME_BITRATE = 1000;
    private static final int ENCODE_FRAME_FPS = 30;

    // 推流
    public void doPublish() {
        // 设置编码以及采集分辨率
        // Set encoding and acquisition resolution
        ZegoVideoConfig zegoVideoConfig = new ZegoVideoConfig();
        zegoVideoConfig.setEncodeResolution(ENCODE_FRAME_HEIGHT, ENCODE_FRAME_WIDTH);
        zegoVideoConfig.setVideoFPS(ENCODE_FRAME_FPS);
        zegoVideoConfig.setVideoBitrate(ENCODE_FRAME_BITRATE);
        mSDKEngine.setVideoConfig(zegoVideoConfig);

        ZegoCanvas zegoCanvas = new ZegoCanvas(null);
        zegoCanvas.viewMode = ZegoViewMode.SCALE_TO_FILL;
        // 设置预览视图及视图展示模式
        // Set preview view and view display mode
        mSDKEngine.startPreview(zegoCanvas);
        mSDKEngine.startPublishingStream(mRoomID);
    }

    // 登出房间，去除推拉流回调监听并释放ZEGO SDK
    // Log out of the room, remove the push-pull flow callback monitoring and release the ZEGO SDK
    public void logoutLiveRoom() {
        mSDKEngine.logoutRoom(mRoomID);
        ZegoExpressEngine.destroyEngine(null);
    }

    // 处理推流操作
    // Handling push operations
    public void DealPublishing(View view) {
        // 界面button==停止推流
        if (mDealBtn.getText().toString().equals("StopPublish")) {
            // 停止预览和推流
            mSDKEngine.stopPreview();
            mSDKEngine.stopPublishingStream();
            mDealBtn.setText("StartPublish");
        } else {
            mDealBtn.setText("StopPublish");
            // 界面button==开始推流
            doPublish();
        }
    }

    // 处理拉流操作
    // Handle pull stream operation
    public void DealPlay(View view) {
        // 界面button==开始拉流
        if (mDealPlayBtn.getText().toString().equals("StartPlay") && !mPlayStreamID.equals("")) {
            ZegoCanvas zegoCanvas = new ZegoCanvas(mPlayView);
            zegoCanvas.viewMode = ZegoViewMode.SCALE_TO_FILL;
            // 开始拉流
            mSDKEngine.startPlayingStream(mPlayStreamID, zegoCanvas);
            mDealPlayBtn.setText("StopPlay");
            mErrorTxt.setText("");
        } else {
            // 界面button==停止拉流
            if (!mPlayStreamID.equals("")) {
                //停止拉流
                mSDKEngine.stopPlayingStream(mPlayStreamID);
                mDealPlayBtn.setText("StartPlay");
            }
        }
    }

    @Override
    public void onSurfaceCreated() {
        mFURenderer.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(int viewWidth, int viewHeight) {
    }

    private byte[] readBack;
    private ByteBuffer byteBuffer;

    @Override
    public int onDrawFrame(byte[] nv21Byte, int texId, int cameraWidth, int cameraHeight, int cameraRotation, float[] mvpMatrix, float[] texMatrix, long timeStamp) {
        if (null == readBack) {
            readBack = new byte[nv21Byte.length];
        }
        int tex2D = mFURenderer.onDrawFrameDualInput(nv21Byte, texId, cameraWidth, cameraHeight, readBack, cameraWidth, cameraHeight);
        // 使用采集视频帧信息构造VideoCaptureFormat
        // Constructing VideoCaptureFormat using captured video frame information
        ZegoVideoFrameParam param = new ZegoVideoFrameParam();
        param.width = cameraWidth;
        param.height = cameraHeight;
        param.strides[0] = cameraWidth;
        param.strides[1] = cameraWidth;
        param.format = ZegoVideoFrameFormat.NV21;
        param.rotation = cameraRotation;
        long now; //部分机型存在 surfaceTexture 时间戳不准确的问题
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            now = SystemClock.elapsedRealtime();
        } else {
            now = TimeUnit.MILLISECONDS.toMillis(SystemClock.elapsedRealtime());
        }
        // 将采集的数据传给ZEGO SDK
        // Pass the collected data to ZEGO SDK
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocateDirect(readBack.length);
        }
        byteBuffer.put(readBack);
        byteBuffer.flip();
        mSDKEngine.sendCustomVideoCaptureRawData(byteBuffer, byteBuffer.limit(), param, now);
        return tex2D;
    }

    @Override
    public void onSurfaceDestroy() {
        mFURenderer.onSurfaceDestroyed();
    }

    @Override
    public void onCameraChanged(int cameraFacing, int cameraOrientation) {
        mFURenderer.onCameraChanged(cameraFacing, cameraOrientation);
        IMakeupModule makeupModule = mFURenderer.getMakeupModule();
        if (makeupModule != null) {
            boolean isBack = cameraFacing == FURenderer.CAMERA_FACING_BACK;
            makeupModule.setIsMakeupFlipPoints(isBack ? 1 : 0);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && null != mFURenderer) {
            float x = event.values[0];
            float y = event.values[1];
            if (Math.abs(x) > 3 || Math.abs(y) > 3) {
                if (Math.abs(x) > Math.abs(y)) {
                    mFURenderer.onDeviceOrientationChanged(x > 0 ? 0 : 180);
                } else {
                    mFURenderer.onDeviceOrientationChanged(y > 0 ? 90 : 270);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
