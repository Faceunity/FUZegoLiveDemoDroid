package im.zego.customrender.videorender;

import java.nio.ByteBuffer;

public class MyVideoFrame {
    public ByteBuffer[] byteBuffers = new ByteBuffer[4];
    public int width;
    public int height;
    public int[] strides = new int[4];

    public MyVideoFrame() {
    }
}
