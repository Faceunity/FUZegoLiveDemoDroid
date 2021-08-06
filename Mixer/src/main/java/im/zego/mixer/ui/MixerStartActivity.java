package im.zego.mixer.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.ArrayList;

import im.zego.common.util.AppLogger;
import im.zego.common.widgets.log.FloatingView;
import im.zego.mixer.R;
import im.zego.zegoexpress.callback.IZegoMixerStartCallback;
import im.zego.zegoexpress.callback.IZegoMixerStopCallback;
import im.zego.zegoexpress.constants.ZegoMixerInputContentType;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoMixerAudioConfig;
import im.zego.zegoexpress.entity.ZegoMixerInput;
import im.zego.zegoexpress.entity.ZegoMixerOutput;
import im.zego.zegoexpress.entity.ZegoMixerTask;
import im.zego.zegoexpress.entity.ZegoMixerVideoConfig;
import im.zego.zegoexpress.entity.ZegoWatermark;

public class MixerStartActivity extends AppCompatActivity implements IMixerStreamUpdateHandler {

    private static ArrayList<CheckBox> checkBoxList = new ArrayList<CheckBox>();
    private static LinearLayout ll_checkBoxList;
    private static String mixStreamID = "mixstream_output_100";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixer_start);

        ll_checkBoxList = findViewById(R.id.ll_CheckBoxList);
        TextView tv_room = findViewById(R.id.tv_room_id3);
        tv_room.setText(MixerMainActivity.roomID);

        ll_checkBoxList.removeAllViews();
        checkBoxList.clear();
        for (String streamID : MixerMainActivity.streamIDList) {
            CheckBox checkBox = (CheckBox) View.inflate(this, R.layout.checkbox, null);
            checkBox.setText(streamID);
            ll_checkBoxList.addView(checkBox);
            checkBoxList.add(checkBox);
        }

        MixerMainActivity.registerStreamUpdateNotify(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MixerMainActivity.registerStreamUpdateNotify(null);
    }

    private ZegoMixerTask task;

    public void ClickStartMix(View view) {
        int count = 0;
        String streamID_1 = "";
        String streamID_2 = "";
        for (int i = 0; i < checkBoxList.size(); i++) {
            if (checkBoxList.get(i).isChecked()) {
                count++;
                if (streamID_1.equals("")) {
                    streamID_1 = checkBoxList.get(i).getText().toString();
                } else if (streamID_2.equals("")) {
                    streamID_2 = checkBoxList.get(i).getText().toString();
                }
            }
        }
        if (count != 2) {
            Toast.makeText(this, getString(R.string.tx_mixer_hint), Toast.LENGTH_SHORT).show();
            return;
        }

        task = new ZegoMixerTask("task1");
        ArrayList<ZegoMixerInput> inputList = new ArrayList<>();
        ZegoMixerInput input_1 = new ZegoMixerInput(streamID_1, ZegoMixerInputContentType.VIDEO, new Rect(0, 0, 360, 320));
        inputList.add(input_1);

        ZegoMixerInput input_2 = new ZegoMixerInput(streamID_2, ZegoMixerInputContentType.VIDEO, new Rect(0, 320, 360, 640));
        inputList.add(input_2);

        ArrayList<ZegoMixerOutput> outputList = new ArrayList<>();
        ZegoMixerOutput output = new ZegoMixerOutput(mixStreamID);
        ZegoMixerAudioConfig audioConfig = new ZegoMixerAudioConfig();
        ZegoMixerVideoConfig videoConfig = new ZegoMixerVideoConfig();
        task.setVideoConfig(videoConfig);
        task.setAudioConfig(audioConfig);
        outputList.add(output);

        task.setInputList(inputList);
        task.setOutputList(outputList);


        ZegoWatermark watermark = new ZegoWatermark("preset-id://zegowp.png", new Rect(0, 0, 300, 200));
        task.setWatermark(watermark);

        task.setBackgroundImageURL("preset-id://zegobg.png");

        MixerMainActivity.engine.startMixerTask(task, new IZegoMixerStartCallback() {

            @Override
            public void onMixerStartResult(int errorCode, JSONObject var2) {
                AppLogger.getInstance().i("onMixerStartResult: result = " + errorCode);
                if (errorCode != 0) {
                    String msg = getString(R.string.tx_mixer_start_fail) + errorCode;
                    Toast.makeText(MixerStartActivity.this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    String msg = getString(R.string.tx_mixer_start_ok);
                    Toast.makeText(MixerStartActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextureView tv_play_mix = findViewById(R.id.tv_play_mix);
        ZegoCanvas canvas = new ZegoCanvas(tv_play_mix);
        MixerMainActivity.engine.startPlayingStream(mixStreamID, canvas);
    }

    public void onRoomStreamUpdate() {
        ll_checkBoxList.removeAllViews();
        checkBoxList.clear();
        for (String streamID : MixerMainActivity.streamIDList) {
            CheckBox checkBox = (CheckBox) View.inflate(this, R.layout.checkbox, null);
            checkBox.setText(streamID);
            ll_checkBoxList.addView(checkBox);
            checkBoxList.add(checkBox);
        }
    }

    public void ClickStopWatch(View view) {
        MixerMainActivity.engine.stopPlayingStream(mixStreamID);
    }

    public void ClickStopMix(View view) {
        MixerMainActivity.engine.stopMixerTask(task, new IZegoMixerStopCallback() {
            @Override
            public void onMixerStopResult(int i) {

            }
        });
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, MixerStartActivity.class);
        activity.startActivity(intent);
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
