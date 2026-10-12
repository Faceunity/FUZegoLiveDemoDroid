package com.faceunity.nama;

import com.faceunity.nama.utils.FuDeviceUtils;

import java.io.File;

public class FUConfig {
    public static final String BLACK_LIST = "config" + File.separator + "blackList.json";;
    // 设备等级动态获取
    public static int DEVICE_LEVEL = FuDeviceUtils.judgeDeviceLevel();
}
