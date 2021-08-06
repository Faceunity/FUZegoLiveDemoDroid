package im.zego.play.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import im.zego.common.widgets.log.FloatingView;
import im.zego.play.R;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.constants.ZegoViewMode;

/**
 * Created by zego on 2019/3/21.
 */
public class PlaySettingActivityUI extends FragmentActivity {


    public static String SHARE_PREFERENCE_NAME = "playSetting";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play_setting);

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

    public static class PrefFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private ListPreference viewModeListPreference;
        private String streamID;
        private String[] stringArray;

        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(SHARE_PREFERENCE_NAME);
            getPreferenceManager().setSharedPreferencesMode(AppCompatActivity.MODE_PRIVATE);
            //从xml文件加载选项
            addPreferencesFromResource(R.xml.play_setting_preference);

            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
            viewModeListPreference = (ListPreference) findPreference("play_view_mode");

            streamID = getActivity().getIntent().getStringExtra("streamID");

            stringArray = getResources().getStringArray(R.array.view_setting_describe);

            String mode = sharedPreferences.getString("play_view_mode", "1");
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
            //This method will be called back when the interface configuration is modified
            // The method needs to call the sdk function to modify the sdk configuration parameters
            if ("play_view_mode".equals(key)) {
                if (streamID != null && !"".equals(streamID)) {
                    String viewModeStr = sharedPreferences.getString(key, "1");
                    int viewMode = Integer.valueOf(viewModeStr);
                    setPlayViewMode(viewMode);
                    // 动态修改当前描述
                    String[] stringArray = getResources().getStringArray(R.array.view_setting_describe);
                    viewModeListPreference.setSummary(stringArray[viewMode]);
                } else {
                    Toast.makeText(PrefFragment.this.getActivity(), R.string.tx_set_play_view_mode_failure, Toast.LENGTH_LONG).show();
                }
            } else if ("play_hardware_decode".equals(key)) {
                boolean enable = sharedPreferences.getBoolean(key, false);
                enableHardwareDecode(enable);
            } else if ("play_volume".equals(key)) {
                int volume = sharedPreferences.getInt(key, 100);
                setPlayVolume(volume);
            }

        }

        /**
         * 设置SDK拉流视图
         * Set SDK pull stream view
         */
        private void setPlayViewMode(int viewMode) {
            PlayActivityUI.viewMode = ZegoViewMode.values()[viewMode];
        }

        /**
         * 启用SDK硬编
         * Enable SDK hard coding
         */
        private void enableHardwareDecode(boolean enable) {
            ZegoExpressEngine.getEngine().enableHardwareDecoder(enable);
        }

        /**
         * 设置拉流音量
         * Set pull stream volume
         *
         * @param volume
         */
        private void setPlayVolume(int volume) {
            ZegoExpressEngine.getEngine().setPlayVolume(streamID, volume);

        }

    }

    public static void actionStart(Activity activity, String mStreamID) {
        Intent intent = new Intent(activity, PlaySettingActivityUI.class);
        intent.putExtra("streamID", mStreamID);
        activity.startActivity(intent);
    }
}
