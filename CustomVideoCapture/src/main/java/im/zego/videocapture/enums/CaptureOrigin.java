package im.zego.videocapture.enums;

// 外部采集来源
public enum CaptureOrigin {
    // 图片源，当前采集设备使用的数据传递类型是Surface_Texture
    // Image source, the data transfer type used by the current collection device is Surface_Texture
    CaptureOrigin_Image(0),
    // 图片源，当前采集设备使用的数据传递类型是GL_Texture_2D
    // Image source, the data transfer type used by the current collection device is GL_Texture_2D
    CaptureOrigin_ImageV2(1),
    // 屏幕源
    // screen source
    CaptureOrigin_Screen(2),
    // 摄像头源，当前采集设备使用的数据传递类型是YUV格式（内存拷贝）
    // Camera source, the data transfer type used by the current collection device is YUV format (memory copy)
    CaptureOrigin_Camera(3),
    // 摄像头源，当前采集设备使用的数据传递类型是Surface_Texture
    // Camera source, the data transfer type used by the current collection device is Surface_Texture
    CaptureOrigin_CameraV2(4),
    // 摄像头源，当前采集设备使用的数据传递类型是ENCODED_FRAME（码流）
    // Camera source, the data transfer type used by the current acquisition device is ENCODED_FRAME (code stream)
    CaptureOrigin_CameraV3(5);

    private int code = 0;

    CaptureOrigin(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}