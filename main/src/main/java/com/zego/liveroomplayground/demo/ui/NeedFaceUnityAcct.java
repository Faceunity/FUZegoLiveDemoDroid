package com.zego.liveroomplayground.demo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import com.zego.liveroomplayground.R;
import com.zego.videofilter.util.PreferenceUtil;

import java.io.IOException;
import java.io.InputStream;

public class NeedFaceUnityAcct extends AppCompatActivity {

    private boolean isOn = true;//是否使用FaceUnity

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_faceunity);

        final Button button = (Button) findViewById(R.id.btn_set);
        String isOpen = PreferenceUtil.getString(this, PreferenceUtil.KEY_FACEUNITY_IS_ON);
        if (TextUtils.isEmpty(isOpen) || PreferenceUtil.VALUE_OFF.equals(isOpen)) {
            isOn = false;
        } else {
            isOn = true;
        }
        button.setText(isOn ? "On" : "Off");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isOn = !isOn;
                button.setText(isOn ? "On" : "Off");
            }
        });

        Button btnToMain = (Button) findViewById(R.id.btn_to_main);
        btnToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NeedFaceUnityAcct.this, MainActivity.class);
                PreferenceUtil.persistString(NeedFaceUnityAcct.this, PreferenceUtil.KEY_FACEUNITY_IS_ON,
                        isOn ? PreferenceUtil.VALUE_ON : PreferenceUtil.VALUE_OFF);
                startActivity(intent);
                finish();
            }
        });

        try {
            InputStream ins = getAssets().open("makeup/naicha.bundle");
            Log.e("benyq", "onCreate: ins " + ins.available());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
