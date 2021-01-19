package im.zego.common.application;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.faceunity.nama.FURenderer;
import com.tencent.bugly.crashreport.CrashReport;

import im.zego.common.widgets.log.FloatingView;

/**
 * Created by zego on 2018/10/16.
 */

public class ZegoApplication extends Application {

    public static ZegoApplication zegoApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        zegoApplication = this;

        initBugly();

        // 添加悬浮日志视图
        // Add floating log view
        FloatingView.get().add();
        /**
         * 初始化FaceUnity
         */
        FURenderer.setup(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    /**
     * 初始化腾讯bug管理平台
     * Initialize the Tencent bug management platform
     */
    private void initBugly() {
        /* Bugly SDK初始化
         * 参数1：上下文对象
         * 参数2：APPID，平台注册时得到,注意替换成你的appId
         * 参数3：是否开启调试模式，调试模式下会输出'CrashReport'tag的日志
         * 注意：如果您之前使用过Bugly SDK，请将以下这句注释掉。
         */
        /* Bugly SDK initialization
         * Parameter 1: Context object
         * Parameter 2: APPID, obtained during platform registration, please replace it with your appId
         * Parameter 3: Whether to enable debug mode, the log of 'CrashReport' tag will be output in debug mode
         * Note: If you have used the Bugly SDK before, please comment out the following sentence.
         */
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(getApplicationContext());
//        strategy.setAppVersion(AppUtils.getAppVersionName());
//        strategy.setAppPackageName(AppUtils.getAppPackageName());
        strategy.setAppReportDelay(20000);                          //Bugly会在启动20s后联网同步数据
        // Bugly will synchronize data online after starting 20s

        /*  第三个参数为SDK调试模式开关，调试模式的行为特性如下：
            输出详细的Bugly SDK的Log；
            每一条Crash都会被立即上报；
            自定义日志将会在Logcat中输出。
            建议在测试阶段建议设置成true，发布时设置为false。*/
        /* The third parameter is the SDK debug mode switch. The behavior of the debug mode is as follows:
            Output detailed Bugly SDK Log;
            Every Crash will be reported immediately;
            Custom logs will be output in Logcat.
            It is recommended to set it to true during the testing phase and false when publishing. */
        CrashReport.initCrashReport(getApplicationContext(), "d53f2eacfe", false);
    }
}
