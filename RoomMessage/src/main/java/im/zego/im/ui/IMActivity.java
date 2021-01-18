package im.zego.im.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Date;

import im.zego.common.util.AppLogger;
import im.zego.common.util.SettingDataUtil;
import im.zego.common.widgets.log.FloatingView;
import im.zego.im.R;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.callback.IZegoIMSendBarrageMessageCallback;
import im.zego.zegoexpress.callback.IZegoIMSendBroadcastMessageCallback;
import im.zego.zegoexpress.callback.IZegoIMSendCustomCommandCallback;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.entity.ZegoBarrageMessageInfo;
import im.zego.zegoexpress.entity.ZegoBroadcastMessageInfo;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoUser;

public class IMActivity extends AppCompatActivity {
    ZegoExpressEngine engine;
    private static ArrayList<CheckBox> checkBoxList = new ArrayList<CheckBox>();
    private static LinearLayout ll_checkBoxList;
    ArrayList<String> mUserList = new ArrayList<>();
    ArrayList<String> records = new ArrayList<>();
    String userID;
    String userName;
    String roomID = "ChatRoom-1";

    @Override
    protected void onDestroy() {
        engine.logoutRoom(roomID);
        ZegoExpressEngine.destroyEngine(null);
        checkBoxList.clear();
        mUserList.clear();
        records.clear();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_im);

        /** 添加悬浮日志视图 */
        /** Add floating log view */
        FloatingView.get().add();
        /** 记录SDK版本号 */
        /** Record SDK version */
        AppLogger.getInstance().i("SDK version : %s", ZegoExpressEngine.getVersion());
        /** 生成随机的用户ID，避免不同手机使用时用户ID冲突，相互影响 */
        /** Generate random user ID to avoid user ID conflict and mutual influence when different mobile phones are used */
        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "user" + randomSuffix;
        userName = "user" + randomSuffix;
        TextView tv_room = findViewById(R.id.tv_im_room);
        tv_room.setText(roomID);
        TextView tv_user = findViewById(R.id.tv_im_user);
        tv_user.setText(userID);
        AppLogger.getInstance().i(getString(R.string.create_zego_engine));
        engine = ZegoExpressEngine.createEngine(SettingDataUtil.getAppId(), SettingDataUtil.getAppKey(), SettingDataUtil.getEnv(), SettingDataUtil.getScenario(), this.getApplication(), null);
        if (engine != null) {
            engine.setEventHandler(new IZegoEventHandler() {
                @Override
                public void onRoomUserUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoUser> userList) {
                    AppLogger.getInstance().i("onRoomUserUpdate: roomID = " + roomID + ", state = " + updateType + ",  userList= " + userList);
                    for (int i = 0; i < userList.size(); i++) {
                        if (updateType == ZegoUpdateType.ADD) {
                            mUserList.add(userList.get(i).userID);
                        } else {
                            mUserList.remove(userList.get(i).userID);
                        }
                    }

                    ll_checkBoxList = findViewById(R.id.ll_CheckBoxList);
                    ll_checkBoxList.removeAllViews();
                    checkBoxList.clear();
                    for (String userID : mUserList) {
                        CheckBox checkBox = (CheckBox) View.inflate(IMActivity.this, R.layout.checkbox, null);
                        checkBox.setText(userID);
                        ll_checkBoxList.addView(checkBox);
                        checkBoxList.add(checkBox);
                    }
                }

                @Override
                public void onIMRecvBroadcastMessage(String roomID, ArrayList<ZegoBroadcastMessageInfo> messageList) {
                    AppLogger.getInstance().i("onIMRecvBroadcastMessage: roomID = " + roomID + ",  messageList= " + messageList);
                    for (int i = 0; i < messageList.size(); i++) {
                        ZegoBroadcastMessageInfo info = messageList.get(i);
                        records.add(info.fromUser.userID + ": " + info.message);
                    }

                    /** 在ListView中显示消息 */
                    /** Show message in the Listview */
                    ListView listView = findViewById(R.id.lv_im_message);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(IMActivity.this, R.layout.array_adapter, records);
                    listView.setAdapter(adapter);
                }

                public void onIMRecvCustomCommand(String roomID, ZegoUser fromUser, String command) {
                    AppLogger.getInstance().i("onIMRecvCustomCommand: roomID = " + roomID + "fromUser :" + fromUser + ", command= " + command);
                    records.add(fromUser.userID + ": " + command);
                    /** 在ListView中显示消息 */
                    /** Show message in the Listview */
                    ListView listView = findViewById(R.id.lv_im_message);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(IMActivity.this, R.layout.array_adapter, records);
                    listView.setAdapter(adapter);
                }

                @Override
                public void onIMRecvBarrageMessage(String roomID, ArrayList<ZegoBarrageMessageInfo> messageList) {
                    AppLogger.getInstance().i("onIMRecvBarrageMessage: roomID = " + roomID + ",  messageList= " + messageList);
                    for (int i = 0; i < messageList.size(); i++) {
                        ZegoBarrageMessageInfo info = messageList.get(i);
                        records.add(info.fromUser.userID + ": " + info.message);
                    }

                    /** 在ListView中显示消息 */
                    /** Show message in the Listview */
                    ListView listView = findViewById(R.id.lv_im_message);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(IMActivity.this, R.layout.array_adapter, records);
                    listView.setAdapter(adapter);
                }
            });
            ZegoRoomConfig config = new ZegoRoomConfig();
            /** 使能用户登录/登出房间通知 */
            /** Enable notification when user login or logout */
            config.isUserStatusNotify = true;
            engine.loginRoom(roomID, new ZegoUser(userID, userName), config);
        }
    }

    public void ClickSendBCMsg(View v) {
        EditText etMsg = findViewById(R.id.ed_bc_message);
        final String msg = etMsg.getText().toString();
        if (!msg.equals("")) {
            engine.sendBroadcastMessage(roomID, msg, new IZegoIMSendBroadcastMessageCallback() {
                /** 发送广播消息结果回调处理 */
                /** Send broadcast message result callback processing */

                @Override
                public void onIMSendBroadcastMessageResult(int errorCode, long messageID) {
                    if (errorCode == 0) {
                        AppLogger.getInstance().i("send broadcast message success");
                        Toast.makeText(IMActivity.this, getString(R.string.tx_im_send_bc_ok), Toast.LENGTH_SHORT).show();
                        records.add(userID + getString(R.string.tx_im_me) + msg);
                        ListView listView = findViewById(R.id.lv_im_message);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(IMActivity.this, R.layout.array_adapter, records);
                        listView.setAdapter(adapter);
                    } else {
                        AppLogger.getInstance().i("send broadcast message fail");
                        Toast.makeText(IMActivity.this, getString(R.string.tx_im_send_bc_fail) + errorCode, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public void ClickSendCustomMsg(View v) {
        EditText etMsg = findViewById(R.id.ed_cc_message);
        final String msg = etMsg.getText().toString();
        ArrayList<ZegoUser> userList = new ArrayList<>();
        for (int i = 0; i < checkBoxList.size(); i++) {
            if (checkBoxList.get(i).isChecked()) {
                String userID = checkBoxList.get(i).getText().toString();
                ZegoUser user = new ZegoUser(userID, userID);
                userList.add(user);
            }
        }
        if (!msg.equals("")) {
            engine.sendCustomCommand(roomID, msg, userList, new IZegoIMSendCustomCommandCallback() {
                /** 发送用户自定义消息结果回调处理 */
                /** Send custom command result callback processing */
                @Override
                public void onIMSendCustomCommandResult(int errorCode) {
                    if (errorCode == 0) {
                        AppLogger.getInstance().i("send custom message success");
                        Toast.makeText(IMActivity.this, getString(R.string.tx_im_send_cc_ok), Toast.LENGTH_SHORT).show();
                        records.add(userID + getString(R.string.tx_im_me) + msg);
                        ListView listView = findViewById(R.id.lv_im_message);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(IMActivity.this, R.layout.array_adapter, records);
                        listView.setAdapter(adapter);
                    } else {
                        AppLogger.getInstance().i("send custom message fail");
                        Toast.makeText(IMActivity.this, getString(R.string.tx_im_send_cc_fail) + errorCode, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, IMActivity.class);
        activity.startActivity(intent);
    }

    public void ClickSendBarrageMsg(View view) {
        EditText etMsg = findViewById(R.id.ed_bar_message);
        final String msg = etMsg.getText().toString();
        if (!msg.equals("")) {
            engine.sendBarrageMessage(roomID, msg, new IZegoIMSendBarrageMessageCallback() {
                @Override
                public void onIMSendBarrageMessageResult(int errorCode, String messageID) {
                    if (errorCode == 0) {
                        AppLogger.getInstance().i("send barrage message success");
                        Toast.makeText(IMActivity.this, getString(R.string.tx_im_send_bar_ok), Toast.LENGTH_SHORT).show();
                        records.add(userID + getString(R.string.tx_im_me) + msg);
                        ListView listView = findViewById(R.id.lv_im_message);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(IMActivity.this, R.layout.array_adapter, records);
                        listView.setAdapter(adapter);
                    } else {
                        AppLogger.getInstance().i("send barrage message fail");
                        Toast.makeText(IMActivity.this, getString(R.string.tx_im_send_bar_fail) + errorCode, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FloatingView.get().attach(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FloatingView.get().detach(this);
    }
}
