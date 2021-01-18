package im.zego.publish.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import im.zego.common.application.ZegoApplication;
import im.zego.common.util.AppLogger;
import im.zego.common.widgets.log.FloatingView;
import im.zego.publish.R;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.constants.ZegoVideoMirrorMode;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoVideoConfig;


/**
 * Created by zego on 2019/3/21.
 */
public class PublishSettingActivityUI extends FragmentActivity {

    public static String SHARE_PREFERENCE_NAME = "publishSetting";
    public static ZegoVideoConfig zegoVideoConfig = new ZegoVideoConfig();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_publish_setting);
    }


    public static class PrefFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private String[] stringArray;
        private ListPreference viewModeListPreference, resolutionListPreference, bitrateListPreference, fpsListPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(SHARE_PREFERENCE_NAME);
            getPreferenceManager().setSharedPreferencesMode(AppCompatActivity.MODE_PRIVATE);
            //从xml文件加载选项
            addPreferencesFromResource(R.xml.publish_setting_preference);

            stringArray = getResources().getStringArray(R.array.view_setting_describe);

            // 注册配置变化事件
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
            viewModeListPreference = (ListPreference) findPreference("publish_view_mode");
            resolutionListPreference = (ListPreference) findPreference("publish_resolution");
            bitrateListPreference = (ListPreference) findPreference("publish_bitrate");
            fpsListPreference = (ListPreference) findPreference("publish_fps");

            fpsListPreference.setSummary(sharedPreferences.getString("publish_fps", "15"));
            bitrateListPreference.setSummary(sharedPreferences.getString("publish_bitrate", "1200000"));
            resolutionListPreference.setSummary(sharedPreferences.getString("publish_resolution", "540x960"));

            // 动态修改当前描述
            String mode = sharedPreferences.getString("publish_view_mode", "1");
            viewModeListPreference.setSummary(stringArray[Integer.parseInt(mode)]);
        }

        @Override
        public void onDestroy() {

            // 取消注册监听配置事件
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onDestroy();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // 当界面配置修改时则会回调该方法
            // 方法内需要调用sdk函数来修改sdk配置参数
            // This method will be called back when the interface configuration is modified
            // The method needs to call the sdk function to modify the sdk configuration parameters
            if ("publish_view_mode".equals(key)) {
                String viewModeStr = sharedPreferences.getString(key, "1");
                int viewMode = Integer.valueOf(viewModeStr);
                PublishActivityUI.viewMode = ZegoViewMode.values()[viewMode];
                // 动态修改当前描述
                viewModeListPreference.setSummary(stringArray[viewMode]);
            } else if ("publish_hardware_encode".equals(key)) {

                boolean enable = sharedPreferences.getBoolean(key, false);

                ZegoExpressEngine.getEngine().enableHardwareEncoder(enable);

            } else if ("publish_preview_mirror".equals(key)) {

                boolean enable = sharedPreferences.getBoolean(key, true);

                // 启用预览镜像
                // Enable preview mirror
                ZegoExpressEngine.getEngine().setVideoMirrorMode(enable ? ZegoVideoMirrorMode.ONLY_PREVIEW_MIRROR : ZegoVideoMirrorMode.NO_MIRROR);
            } else if ("publish_resolution".equals(key)) {
                String resolution = sharedPreferences.getString(key, "540x960");
                // 动态修改当前描述
                resolutionListPreference.setSummary(resolution);
                String[] resolutions = resolution.split("x");

                // 设置采集分辨率
                // Set acquisition resolution
                zegoVideoConfig.setCaptureResolution(Integer.parseInt(resolutions[0]), Integer.parseInt(resolutions[1]));

                // 设置编码分辨率
                // Set encoding resolution
                zegoVideoConfig.setEncodeResolution(Integer.parseInt(resolutions[0]), Integer.parseInt(resolutions[1]));

                ZegoExpressEngine.getEngine().setVideoConfig(zegoVideoConfig);

            } else if ("publish_bitrate".equals(key)) {
                String bitrate = sharedPreferences.getString(key, "1200");
                // 动态修改当前描述
                bitrateListPreference.setSummary(bitrate);

                // 设置视频码率
                // Set video bitrate
                zegoVideoConfig.setVideoBitrate(Integer.parseInt(bitrate));
                ZegoExpressEngine.getEngine().setVideoConfig(zegoVideoConfig);

            } else if ("publish_fps".equals(key)) {
                String fps = sharedPreferences.getString(key, "15");
                // 动态修改当前描述
                fpsListPreference.setSummary(fps);
                // 设置视频推流fps
                // Set video push stream fps
                zegoVideoConfig.setVideoFPS(Integer.parseInt(fps));
                ZegoExpressEngine.getEngine().setVideoConfig(zegoVideoConfig);

            }

        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        // 在应用内实现悬浮窗，需要依附Activity生命周期
        FloatingView.get().attach(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 在应用内实现悬浮窗，需要依附Activity生命周期
        FloatingView.get().detach(this);
    }

    /**
     * 清空推流配置
     * Clear push configuration
     */
    public static void clearPublishConfig() {
        AppLogger.getInstance().i("推流常用功能, 恢复sdk默认配置");
        SharedPreferences sharedPreferences = ZegoApplication.zegoApplication.getSharedPreferences(SHARE_PREFERENCE_NAME, AppCompatActivity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();


        ZegoExpressEngine.getEngine().enableHardwareEncoder(false);
        ZegoExpressEngine.getEngine().useFrontCamera(true);
        ZegoExpressEngine.getEngine().enableAudioCaptureDevice(true);
        ZegoExpressEngine.getEngine().enableCamera(true);
        ZegoExpressEngine.getEngine().setVideoMirrorMode(ZegoVideoMirrorMode.ONLY_PREVIEW_MIRROR);

        ZegoExpressEngine.getEngine().setVideoConfig(null);

    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, PublishSettingActivityUI.class);
        activity.startActivity(intent);
    }

}