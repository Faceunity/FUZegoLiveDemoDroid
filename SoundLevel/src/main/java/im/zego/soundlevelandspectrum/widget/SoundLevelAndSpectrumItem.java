package im.zego.soundlevelandspectrum.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import im.zego.soundlevelandspectrum.R;

public class SoundLevelAndSpectrumItem extends LinearLayout {

    private SpectrumView mSpectrumView;

    public SpectrumView getSpectrumView() {
        return mSpectrumView;
    }


    private TextView mTvUserid;

    public TextView getTvUserId() {
        return mTvUserid;
    }

    public void setTvUserId(TextView tv_userid) {
        this.mTvUserid = tv_userid;
    }


    private TextView mTvStreamId;

    public TextView getTvStreamId() {
        return mTvStreamId;
    }

    public void setTvStreamId(TextView streamId) {
        this.mTvStreamId = streamId;
    }


    private String mStreamid;

    public String getStreamid() {
        return mStreamid;
    }

    public void setStreamid(String streamid) {
        this.mStreamid = streamid;
    }


    private ProgressBar mPbSoundLevel;

    public ProgressBar getPbSoundLevel() {
        return mPbSoundLevel;
    }

    public void setPbSoundLevel(ProgressBar pb_play_sound_level) {
        this.mPbSoundLevel = pb_play_sound_level;
    }


    public SoundLevelAndSpectrumItem(Context ctx, AttributeSet attributeSet) {

        super(ctx, attributeSet);

        LayoutInflater.from(ctx).inflate(R.layout.activity_soundlevelandspectrum_layout_item, this);

        mSpectrumView = findViewById(R.id.soundlevelandspectrum_spectrum_view);

        mTvUserid = findViewById(R.id.tv_soundlevelandspectrum_userid);
        mTvStreamId = findViewById(R.id.tv_soundlevelandspectrum_streamid);

        mPbSoundLevel = findViewById(R.id.pb_sound_level);

    }


}
