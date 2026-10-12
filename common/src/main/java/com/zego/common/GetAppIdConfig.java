package com.zego.common;

/**
 * Created by zego on 2018/11/15.
 */

public class GetAppIdConfig {

    /**
     * 请提前在即构管理控制台获取 appID 与 appSign
     * AppID 填写样式示例：
     * public static final long appId = 123456789L;
     * appSign 填写样式示例：
     * public static final byte[] appSign = {
     * (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     * (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     * (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     * (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     * (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     * (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     * (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
     * (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
     * }
     */

    public static final long appId = 735890592;

    public static final byte[] appSign = new byte[]{(byte) 0x10, (byte) 0xf6, (byte) 0x45, (byte) 0xe7,
            (byte) 0xe1, (byte) 0xfb, (byte) 0xf5, (byte) 0x27, (byte) 0xe2, (byte) 0x09, (byte) 0xc3,
            (byte) 0x8f, (byte) 0x04, (byte) 0xb3, (byte) 0x9c, (byte) 0x32, (byte) 0x9e, (byte) 0xa5,
            (byte) 0x56, (byte) 0xb6, (byte) 0x55, (byte) 0xf8, (byte) 0x59, (byte) 0xd5, (byte) 0x29,
            (byte) 0x10, (byte) 0xb3, (byte) 0xc0, (byte) 0x9d, (byte) 0x3f, (byte) 0x1e, (byte) 0x99};
}
