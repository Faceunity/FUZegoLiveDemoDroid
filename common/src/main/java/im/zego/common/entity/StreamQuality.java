package im.zego.common.entity;


import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import im.zego.common.BR;

/**
 * Created by zego on 2019/3/20.
 */

public class StreamQuality extends BaseObservable {
    private String roomID = "";
    private String streamID = "";
    private String resolution = "";
    private String bitrate = "";
    private String fps = "";
    private String networkQuality = "";
    private String hardwareEncode = "";

    @Bindable
    public String getNetworkQuality() {
        return networkQuality;
    }

    public void setNetworkQuality(String networkQuality) {
        this.networkQuality = networkQuality;
        notifyPropertyChanged(BR.networkQuality);
    }


    @Bindable
    public String getHardwareEncode() {
        return hardwareEncode;
    }

    public void setHardwareEncode(String hardwareEncode) {
        this.hardwareEncode = hardwareEncode;
        notifyPropertyChanged(BR.hardwareEncode);
    }


    @Bindable
    public String getRoomID() {
        return roomID;
    }

    public void setRoomID(String roomID) {
        this.roomID = roomID;
        notifyPropertyChanged(BR.roomID);
    }

    @Bindable
    public String getStreamID() {
        return streamID;
    }

    public void setStreamID(String streamID) {
        this.streamID = streamID;
        notifyPropertyChanged(BR.streamID);
    }

    @Bindable
    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
        notifyPropertyChanged(BR.resolution);
    }

    @Bindable
    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
        notifyPropertyChanged(BR.bitrate);
    }

    @Bindable
    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
        notifyPropertyChanged(BR.fps);
    }
}
