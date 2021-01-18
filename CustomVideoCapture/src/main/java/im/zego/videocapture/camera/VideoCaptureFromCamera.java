package im.zego.videocapture.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import im.zego.common.util.AppLogger;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoVideoFrameFormat;
import im.zego.zegoexpress.entity.ZegoVideoFrameParam;

/**
 * VideoCaptureFromCamera
 * 实现从摄像头采集数据并传给ZEGO SDK，需要继承实现ZEGO SDK 的ZegoVideoCaptureDevice类
 * 采用内存拷贝方式传递数据，即YUV格式，通过client的onByteBufferFrameCaptured传递采集数据
 */

/**
 *  * VideoCaptureFromCamera
 *  * To collect data from the camera and pass it to the ZEGO SDK, you need to inherit the ZegoVideoCaptureDevice class that implements the ZEGO SDK
 *  * Use memory copy to transfer data, that is, YUV format, through client's onByteBufferFrameCaptured to transfer collected data
 *  
 */
public class VideoCaptureFromCamera extends ZegoVideoCaptureCallback implements Camera.PreviewCallback, TextureView.SurfaceTextureListener {
    private static final String TAG = "VideoCaptureFromCamera";
    private static final int CAMERA_STOP_TIMEOUT_MS = 7000;

    private Camera mCam = null;
    private Camera.CameraInfo mCamInfo = null;
    // 默认为后置摄像头
    // The default is the rear camera
    int mFront = 0;
    // 预设分辨率宽
    // Wide preset resolution
    int mWidth = 640;
    // 预设分辨率高
    // High preset resolution
    int mHeight = 480;
    // 预设采集帧率
    // Preset acquisition frame rate
    int mFrameRate = 15;
    // 默认不旋转
    // No rotation by default
    int mRotation = 0;

    // SDK 内部实现的、同样实现 ZegoVideoCaptureDevice.Client 协议的客户端，用于通知SDK采集结果
    // The client implemented inside the SDK and also implementing the ZegoVideoCaptureDevice.Client protocol is used to notify the SDK of the results
    ZegoExpressEngine mSDKEngine = null;

    private TextureView mView = null;
    private SurfaceTexture mTexture = null;

    // Arbitrary queue depth.  Higher number means more memory allocated & held,
    // lower number means more sensitivity to processing time in the client (and
    // potentially stalling the capturer if it runs out of buffers to write to).
    private static final int NUMBER_OF_CAPTURE_BUFFERS = 3;
    private final Set<byte[]> queuedBuffers = new HashSet<byte[]>();
    private int mFrameSize = 0;

    private HandlerThread mThread = null;
    private volatile Handler cameraThreadHandler = null;
    private final AtomicBoolean isCameraRunning = new AtomicBoolean();
    private final Object pendingCameraRestartLock = new Object();
    private volatile boolean pendingCameraRestart = false;

    public VideoCaptureFromCamera(ZegoExpressEngine mSDKEngine) {
        this.mSDKEngine = mSDKEngine;
    }


    /**
     * 初始化资源，必须实现
     * Initialization of resources must be achieved
     */
    @Override
    public void onStart(ZegoPublishChannel channel) {
        Log.i(TAG, "onStart");
        AppLogger.getInstance().i(" VideoCaptureFromCamera onStart callBack,channel:" + channel);
        mThread = new HandlerThread("camera-cap");
        mThread.start();
        // 创建camera异步消息处理handler
        // Create a camera asynchronous message processing handler
        cameraThreadHandler = new Handler(mThread.getLooper());

        cameraThreadHandler.post(() -> {
            setFrameRate(15);
            setResolution(360, 640);
        });

        startCapture();
    }

    // 停止推流时，ZEGO SDK 调用 stopCapture 通知外部采集设备停止采集，必须实现
    // When stopping pushing, the ZEGO SDK calls stopCapture to notify the external collection device to stop collection, which must be implemented
    @Override
    public void onStop(ZegoPublishChannel channel) {
        Log.i(TAG, "onStop");
        AppLogger.getInstance().i(" VideoCaptureFromCamera onStop callBack,channel:" + channel);
        // 停止camera采集任务
        stopCapture();
        mThread.quit();
        mThread = null;
    }


    // 设置采集帧率
    // Set the acquisition frame rate
    private int setFrameRate(int framerate) {
        mFrameRate = framerate;
        // 更新camera的采集帧率
        // Update camera frame rate
        updateRateOnCameraThread(framerate);
        return 0;
    }

    // 设置视图宽高
    // Set view width and height
    private int setResolution(int width, int height) {
        mWidth = width;
        mHeight = height;
        // 修改视图宽高后需要重启camera
        // You need to restart the camera after changing the view width and height
        restartCam();
        return 0;
    }

    // 前后摄像头的切换
    // Switching between front and back cameras
    private int setFrontCam(int bFront) {
        mFront = bFront;
        // 切换摄像头后需要重启camera
        // Camera needs to be restarted after switching cameras
        restartCam();
        return 0;
    }

    // 设置展示视图
    // Set display view
    @Override
    public void setView(final View view) {
        if (mView != null) {
            if (mView.getSurfaceTextureListener().equals(this)) {
                mView.setSurfaceTextureListener(null);
            }
            mView = null;
            mTexture = null;
        }
        mView = (TextureView) view;
        if (mView != null) {
            // 设置SurfaceTexture相关回调监听
            // Set SurfaceTexture related callback listener
            mView.setSurfaceTextureListener(VideoCaptureFromCamera.this);
            if (mView.isAvailable()) {
                mTexture = mView.getSurfaceTexture();
            }
        }
    }


    // 设置采集时的旋转方向
    // Set the rotation direction during acquisition
    public int setCaptureRotation(int nRotation) {
        mRotation = nRotation;
        return 0;
    }


    // 停止推流时，ZEGO SDK 调用 stopCapture 通知外部采集设备停止采集，必须实现
    // When stopping pushing, the ZEGO SDK calls stopCapture to notify the external collection device to stop collection, which must be implemented
    public int stopCapture() {
        Log.d(TAG, "stopCapture");
        final CountDownLatch barrier = new CountDownLatch(1);
        final boolean didPost = maybePostOnCameraThread(new Runnable() {
            @Override
            public void run() {
                // 停止camera
                // stop camera
                stopCaptureOnCameraThread(true /* stopHandler */);
                // 释放camera资源
                // Free camera resources
                releaseCam();
                barrier.countDown();
            }
        });
        if (!didPost) {
            Log.e(TAG, "Calling stopCapture() for already stopped camera.");
            return 0;
        }
        try {
            if (!barrier.await(CAMERA_STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "Camera stop timeout");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "stopCapture done");

        return 0;
    }

    // 开始推流时，ZEGO SDK 调用 startCapture 通知外部采集设备开始工作，必须实现
    // When starting to push the stream, ZEGO SDK calls startCapture to notify the external collection device to start work, which must be implemented
    private int startCapture() {
        if (isCameraRunning.getAndSet(true)) {
            Log.e(TAG, "Camera has already been started.");
            return 0;
        }

        final boolean didPost = maybePostOnCameraThread(new Runnable() {
            @Override
            public void run() {
                // 创建camera
                // create camera
                createCamOnCameraThread();
                // 启动camera
                //start camera
                startCamOnCameraThread();
            }
        });

        return 0;
    }


    // 更新camera的采集帧率
    // Update camera frame rate
    private int updateRateOnCameraThread(final int framerate) {
        checkIsOnCameraThread();
        if (mCam == null) {
            return 0;
        }

        mFrameRate = framerate;

        Camera.Parameters parms = mCam.getParameters();
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            if ((entry[0] == entry[1]) && entry[0] == mFrameRate * 1000) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                break;
            }
        }

        int[] realRate = new int[2];
        parms.getPreviewFpsRange(realRate);
        if (realRate[0] == realRate[1]) {
            mFrameRate = realRate[0] / 1000;
        } else {
            mFrameRate = realRate[1] / 2 / 1000;
        }

        try {
            mCam.setParameters(parms);
        } catch (Exception ex) {
            Log.i(TAG, "vcap: update fps -- set camera parameters error with exception\n");
            ex.printStackTrace();
        }
        return 0;
    }

    // 检查CameraThread是否正常运行
    // Check if CameraThread is running normally
    private void checkIsOnCameraThread() {
        if (cameraThreadHandler == null) {
            Log.e(TAG, "Camera is not initialized - can't check thread.");
        } else if (Thread.currentThread() != cameraThreadHandler.getLooper().getThread()) {
            throw new IllegalStateException("Wrong thread");
        }
    }

    // 控制UI刷新
    // Control UI refresh
    private boolean maybePostOnCameraThread(Runnable runnable) {
        return cameraThreadHandler != null && isCameraRunning.get()
                && cameraThreadHandler.postAtTime(runnable, this, SystemClock.uptimeMillis());
    }

    // 创建camera
    //create camera
    private int createCamOnCameraThread() {
        checkIsOnCameraThread();
        if (!isCameraRunning.get()) {
            Log.e(TAG, "startCaptureOnCameraThread: Camera is stopped");
            return 0;
        }

        Log.i(TAG, "board: " + Build.BOARD);
        Log.i(TAG, "device: " + Build.DEVICE);
        Log.i(TAG, "manufacturer: " + Build.MANUFACTURER);
        Log.i(TAG, "brand: " + Build.BRAND);
        Log.i(TAG, "model: " + Build.MODEL);
        Log.i(TAG, "product: " + Build.PRODUCT);
        Log.i(TAG, "sdk: " + Build.VERSION.SDK_INT);

        // 获取欲设置camera的索引号
        //Get the index number of the camera to be set
        int nFacing = (mFront != 0) ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;

        if (mCam != null) {
            // 已打开camera
            // Camera opened
            return 0;
        }

        mCamInfo = new Camera.CameraInfo();
        // 获取设备上camera的数目
        // Get the number of cameras on the device
        int nCnt = Camera.getNumberOfCameras();
        // 得到欲设置camera的索引号并打开camera
        // Get the index number of the camera you want to set and open the camera
        for (int i = 0; i < nCnt; i++) {
            Camera.getCameraInfo(i, mCamInfo);
            if (mCamInfo.facing == nFacing) {
                mCam = Camera.open(i);
                break;
            }
        }

        // 没找到欲设置的camera
        // Did not find the camera to be set
        if (mCam == null) {
            Log.i(TAG, "[WARNING] no camera found, try default\n");
            // 先试图打开默认camera
            // First try to open the default camera
            mCam = Camera.open();

            if (mCam == null) {
                Log.i(TAG, "[ERROR] no camera found\n");
                return -1;
            }
        }


        boolean bSizeSet = false;
        Camera.Parameters parms = mCam.getParameters();
        // 获取camera首选的size
        // Get the camera's preferred size
        Camera.Size psz = parms.getPreferredPreviewSizeForVideo();

        mWidth = 640;
        mHeight = 480;

        parms.setPreviewSize(640, 480);

        // 获取camera支持的帧率范围，并设置预览帧率范围
        // Get the frame rate range supported by the camera and set the preview frame rate range
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            if ((entry[0] == entry[1]) && entry[0] == mFrameRate * 1000) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                break;
            }
        }

        // 获取camera的实际帧率
        // Get the actual frame rate of the camera
        int[] realRate = new int[2];
        parms.getPreviewFpsRange(realRate);
        if (realRate[0] == realRate[1]) {
            mFrameRate = realRate[0] / 1000;
        } else {
            mFrameRate = realRate[1] / 2 / 1000;
        }

        // 不启用提高MediaRecorder录制摄像头视频性能的功能，可能会导致在某些手机上预览界面变形的问题
        // Failure to enable the function that improves the performance of the MediaRecorder to record camera video may cause distortions in the preview interface on some phones
        parms.setRecordingHint(false);

        // 设置camera的对焦模式
        // Set the camera's focus mode
        boolean bFocusModeSet = false;
        for (String mode : parms.getSupportedFocusModes()) {
            if (mode.compareTo(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) == 0) {
                try {
                    parms.setFocusMode(mode);
                    bFocusModeSet = true;
                    break;
                } catch (Exception ex) {
                    Log.i(TAG, "[WARNING] vcap: set focus mode error (stack trace followed)!!!\n");
                    ex.printStackTrace();
                }
            }
        }
        if (!bFocusModeSet) {
            Log.i(TAG, "[WARNING] vcap: focus mode left unset !!\n");
        }

        // 设置camera的参数
        // Set camera parameters
        try {
            mCam.setParameters(parms);
        } catch (Exception ex) {
            Log.i(TAG, "vcap: set camera parameters error with exception\n");
            ex.printStackTrace();
        }

        Camera.Parameters actualParm = mCam.getParameters();

        mWidth = actualParm.getPreviewSize().width;
        mHeight = actualParm.getPreviewSize().height;
        Log.i(TAG, "[WARNING] vcap: focus mode " + actualParm.getFocusMode());

        createPool();

        int result;
        if (mCamInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCamInfo.orientation + mRotation) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (mCamInfo.orientation - mRotation + 360) % 360;
        }
        // 设置预览图像的转方向
        // Set the rotation direction of the preview image
        mCam.setDisplayOrientation(result);

        return 0;
    }

    // 为camera分配内存存放采集数据
    // Allocate memory for camera to store collected data
    private void createPool() {
        queuedBuffers.clear();
        mFrameSize = mWidth * mHeight * 3 / 2;
        for (int i = 0; i < NUMBER_OF_CAPTURE_BUFFERS; ++i) {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(mFrameSize);
            queuedBuffers.add(buffer.array());
            // 减少camera预览时的内存占用
            // Reduce memory usage during camera preview
            mCam.addCallbackBuffer(buffer.array());
        }
    }

    // 启动camera
    //start camera
    private int startCamOnCameraThread() {
        checkIsOnCameraThread();
        if (!isCameraRunning.get() || mCam == null) {
            Log.e(TAG, "startPreviewOnCameraThread: Camera is stopped");
            return 0;
        }

        if (mTexture == null) {
            return -1;
        }

        try {

            mCam.setPreviewTexture(mTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 在打开摄像头预览前先分配一个buffer地址，目的是为了后面内存复用
        // Before opening the camera preview, first allocate a buffer address, the purpose is to reuse memory later
        mCam.setPreviewCallbackWithBuffer(this);
        // 启动camera预览
        // Start camera preview
        mCam.startPreview();
        return 0;
    }

    // 停止camera采集
    // Stop camera collection
    private int stopCaptureOnCameraThread(boolean stopHandler) {
        checkIsOnCameraThread();
        Log.d(TAG, "stopCaptureOnCameraThread");

        if (stopHandler) {
            // Clear the cameraThreadHandler first, in case stopPreview or
            // other driver code deadlocks. Deadlock in
            // android.hardware.Camera._stopPreview(Native Method) has
            // been observed on Nexus 5 (hammerhead), OS version LMY48I.
            // The camera might post another one or two preview frames
            // before stopped, so we have to check |isCameraRunning|.
            // Remove all pending Runnables posted from |this|.
            isCameraRunning.set(false);
            cameraThreadHandler.removeCallbacksAndMessages(this /* token */);
        }

        if (mCam != null) {
            // 停止camera预览
            // stop camera preview
            mCam.stopPreview();
            mCam.setPreviewCallbackWithBuffer(null);
        }
        queuedBuffers.clear();
        return 0;
    }

    // 重启camera
    // restart camera
    private int restartCam() {
        synchronized (pendingCameraRestartLock) {
            if (pendingCameraRestart) {
                // Do not handle multiple camera switch request to avoid blocking
                // camera thread by handling too many switch request from a queue.
                Log.w(TAG, "Ignoring camera switch request.");
                return 0;
            }
            pendingCameraRestart = true;
        }

        final boolean didPost = maybePostOnCameraThread(new Runnable() {
            @Override
            public void run() {
                stopCaptureOnCameraThread(false);
                releaseCam();
                createCamOnCameraThread();
                startCamOnCameraThread();
                synchronized (pendingCameraRestartLock) {
                    pendingCameraRestart = false;
                }
            }
        });

        if (!didPost) {
            synchronized (pendingCameraRestartLock) {
                pendingCameraRestart = false;
            }
        }

        return 0;
    }

    // 释放camera
    // release camera
    private int releaseCam() {
        // * release cam
        if (mCam != null) {
            mCam.release();
            mCam = null;
        }

        // * release cam info
        mCamInfo = null;
        return 0;
    }

    ByteBuffer byteBuffer;

    // 预览视频帧回调
    // Preview video frame callback
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        checkIsOnCameraThread();
        if (!isCameraRunning.get()) {
            Log.e(TAG, "onPreviewFrame: Camera is stopped");
            return;
        }

        if (!queuedBuffers.contains(data)) {
            // |data| is an old invalid buffer.
            return;
        }

        if (mSDKEngine == null) {
            return;
        }

        // 使用采集视频帧信息构造VideoCaptureFormat
        // Constructing VideoCaptureFormat using captured video frame information
        ZegoVideoFrameParam param = new ZegoVideoFrameParam();
        param.width = mWidth;
        param.height = mHeight;
        param.strides[0] = mWidth;
        param.strides[1] = mWidth;
        param.format = ZegoVideoFrameFormat.NV21;
        param.rotation = mCamInfo.orientation;

        long now;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            now = SystemClock.elapsedRealtime();
        } else {
            now = TimeUnit.MILLISECONDS.toMillis(SystemClock.elapsedRealtime());
        }
        // 将采集的数据传给ZEGO SDK
        // Pass the collected data to ZEGO SDK
        if (byteBuffer == null) {
            byteBuffer = ByteBuffer.allocateDirect(data.length);
        }
        byteBuffer.put(data);
        byteBuffer.flip();

        mSDKEngine.sendCustomVideoCaptureRawData(byteBuffer, data.length, param, now);

        // 实现camera预览时的内存复用
        // Memory reuse during camera preview
        camera.addCallbackBuffer(data);
    }

    // TextureView.SurfaceTextureListener 回调
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mTexture = surface;
        // 启动采集
        // start collect
        startCapture();
        // 不能使用 restartCam ，因为切后台时再切回时，isCameraRunning 已经被置为 false
        //restartCam();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mTexture = surface;
        // 视图size变化时重启camera
        // Restart the camera when the view size changes
        restartCam();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mTexture = null;
        // 停止采集
        // stop collect
        stopCapture();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
