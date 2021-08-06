package im.zego.video.talk.utils;

import android.app.Application;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class ScreenHelper {//获取屏幕数据工具类
    private volatile static ScreenHelper singleton;
    WindowManager windowManager;
    DisplayMetrics outMetrics;

    private ScreenHelper(Application application) {
        windowManager = (WindowManager) application.getSystemService(application.WINDOW_SERVICE);
        outMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(outMetrics);
    }

    public static ScreenHelper getSingleton(Application application) {
        if (singleton == null) {
            synchronized (ScreenHelper.class) {
                if (singleton == null) {
                    singleton = new ScreenHelper(application);
                }
            }
        }
        return singleton;
    }

    public int getScreenWidthPixels() {
        return outMetrics.widthPixels;
    }

    public int getScreenHeightPixels() {
        return outMetrics.heightPixels;
    }
}
