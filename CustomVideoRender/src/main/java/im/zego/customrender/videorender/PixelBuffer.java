package im.zego.customrender.videorender;

import java.nio.ByteBuffer;

/**
 * @author Richie on 2020.12.28
 */
public class PixelBuffer {
    /** 单帧视频数据
     *  包含视频画面的宽、高、数据、strides
     */
    /**
     * Single frame video data
     *      * Including the width, height, data and strides of the video screen
     *      
     */
    public int width;
    public int height;
    public ByteBuffer[] buffer;
    public int[] strides;
}
