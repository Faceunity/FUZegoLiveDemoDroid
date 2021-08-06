package im.zego.expresssample.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import im.zego.common.GetAppIDConfig;
import im.zego.common.util.PreferenceUtil;
import im.zego.common.widgets.CustomPopWindow;
import im.zego.expresssample.R;
import im.zego.expresssample.databinding.ActivitySettingBinding;
import im.zego.zegoexpress.ZegoExpressEngine;

import static im.zego.common.util.PreferenceUtil.KEY_APP_ID;
import static im.zego.common.util.PreferenceUtil.KEY_APP_SIGN;
import static im.zego.common.util.PreferenceUtil.KEY_SCENARIO;
import static im.zego.common.util.PreferenceUtil.KEY_TEST_ENVIRONMENT;


public class SettingActivity extends AppCompatActivity {

    private ActivitySettingBinding binding;
//    private int envSelection = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);

        // 设置界面参数，原始设置值
        initViewValue();

        binding.goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        binding.txSdkVersion.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager cmb = (ClipboardManager) SettingActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                cmb.setText(binding.txSdkVersion.getText());
                showPopWindows(getString(R.string.tx_copyed), v);
                return false;
            }
        });

        binding.txDemoVersion.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager cmb = (ClipboardManager) SettingActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                cmb.setText(binding.txDemoVersion.getText());
                showPopWindows(getString(R.string.tx_copyed), v);
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        // 保存页面设置
        finish();
    }

    public void createSaveDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.save_alert)).setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveSetting();
                dialog.dismiss();
                Toast.makeText(SettingActivity.this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(SettingActivity.this, getString(R.string.save_cancel), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void createResetDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.reset_alert)).setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                realReset();
                dialog.dismiss();
                Toast.makeText(SettingActivity.this, getString(R.string.reset_success), Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(SettingActivity.this, getString(R.string.reset_cancel), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void realReset() {
        PreferenceUtil.getInstance().setStringValue(KEY_APP_ID, String.valueOf(GetAppIDConfig.appID));
        binding.edAppId.setText(PreferenceUtil.getInstance().getStringValue(KEY_APP_ID, ""));
        PreferenceUtil.getInstance().setStringValue(KEY_APP_SIGN, GetAppIDConfig.appSign);
        binding.edAppSign.setText(PreferenceUtil.getInstance().getStringValue(KEY_APP_SIGN, ""));
        PreferenceUtil.getInstance().setBooleanValue(KEY_TEST_ENVIRONMENT, true);
        binding.spEnv.setSelection(PreferenceUtil.getInstance().getBooleanValue(KEY_TEST_ENVIRONMENT, true) ? 0 : 1);
        PreferenceUtil.getInstance().setIntValue(KEY_SCENARIO, 0);
        binding.spSc.setSelection(PreferenceUtil.getInstance().getIntValue(KEY_SCENARIO, 0));
    }

    // 还原原设置参数
    private void initViewValue() {
        binding.txSdkVersion.setText(getSdkVersion());
        binding.txDemoVersion.setText(getString(R.string.demo_version) + getLocalVersionName(this));
//        if(PreferenceUtil.getInstance().getStringValue(KEY_APP_ID, "").equals("")){
//            PreferenceUtil.getInstance().setStringValue(KEY_APP_ID, String.valueOf(GetAppIDConfig.appID));
//        }
//        binding.edAppId.setText(PreferenceUtil.getInstance().getStringValue(KEY_APP_ID, ""));
//        if(PreferenceUtil.getInstance().getStringValue(KEY_APP_SIGN,"").equals("")){
//            PreferenceUtil.getInstance().setStringValue(KEY_APP_SIGN,GetAppIDConfig.appSign);
//        }
        binding.edAppId.setText(PreferenceUtil.getInstance().getStringValue(KEY_APP_ID, ""));
        binding.edAppSign.setText(PreferenceUtil.getInstance().getStringValue(KEY_APP_SIGN, ""));
        binding.spEnv.setSelection(PreferenceUtil.getInstance().getBooleanValue(KEY_TEST_ENVIRONMENT, true) ? 0 : 1);
        binding.spSc.setSelection(PreferenceUtil.getInstance().getIntValue(KEY_SCENARIO, 0));
    }

    // 保存界面上的设置参数
    private void saveSetting() {
        // AppID
        PreferenceUtil.getInstance().setStringValue(KEY_APP_ID, binding.edAppId.getText().toString().trim());
        // AppSign
        PreferenceUtil.getInstance().setStringValue(KEY_APP_SIGN, binding.edAppSign.getText().toString().trim());
        // env
        PreferenceUtil.getInstance().setBooleanValue(KEY_TEST_ENVIRONMENT, (binding.spEnv.getSelectedItemPosition() == 0) ? true : false);
        //Scenario
        PreferenceUtil.getInstance().setIntValue(KEY_SCENARIO, binding.spSc.getSelectedItemPosition());
    }

    // 获取 SDK 版本
    public String getSdkVersion() {
        return getString(R.string.sdk_version) + ZegoExpressEngine.getVersion();//ZegoLiveRoom.version()
    }

    /**
     * 供其他Activity调用，进入当前Activity查看版本
     *
     * @param activity
     */
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, SettingActivity.class);

        activity.startActivity(intent);
    }

    public String getLocalVersionName(Context ctx) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("liveroomplayground", "not found package name" + e.getMessage());

        }

        return localVersion;
    }

    /**
     * 显示长按复制结构窗口
     *
     * @param msg  显示内容
     * @param view
     */
    private void showPopWindows(String msg, View view) {
        //创建并显示popWindow
        new CustomPopWindow.PopupWindowBuilder(this)
                .enableBackgroundDark(true) //弹出popWindow时，背景是否变暗
                .setBgDarkAlpha(0.7f) // 控制亮度
                .create()
                .setMsg(msg)
                .showAsDropDown(view, 0, 20);
    }
}
