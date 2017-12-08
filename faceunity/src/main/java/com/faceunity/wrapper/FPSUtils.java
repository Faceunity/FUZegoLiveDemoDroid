package com.faceunity.wrapper;

import android.util.Log;

/**
 * Created by tujh on 2017/12/1.
 */

public abstract class FPSUtils {

    private static long lastOneHundredFrameTimeStamp = 0;
    private static int currentFrameCnt = 0;

    public static void FPS() {
        if (++currentFrameCnt == 100) {
            currentFrameCnt = 0;
            long tmp = System.nanoTime();
            Log.e("FPSUtils", "FPS : " + (1000.0f * MiscUtil.NANO_IN_ONE_MILLI_SECOND / ((tmp - lastOneHundredFrameTimeStamp) / 100.0f)));
            lastOneHundredFrameTimeStamp = tmp;
        }
    }
}
