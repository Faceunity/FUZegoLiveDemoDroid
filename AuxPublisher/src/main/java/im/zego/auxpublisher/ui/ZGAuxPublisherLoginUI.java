package im.zego.auxpublisher.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import im.zego.auxpublisher.R;
import im.zego.auxpublisher.databinding.AuxLoginBinding;


public class ZGAuxPublisherLoginUI extends Activity {
    private AuxLoginBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.aux_login);
        binding.explanation.setText(getString(R.string.explanation));
    }

    public void jumpPublish(View view) {
        ZGAuxPublisherPublishUI.actionStart(this);
    }

    public void jumpStart(View view) {
        ZGAuxPublisherPlayUI.actionStart(this);
    }

    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, ZGAuxPublisherLoginUI.class);
        activity.startActivity(intent);
    }
}
