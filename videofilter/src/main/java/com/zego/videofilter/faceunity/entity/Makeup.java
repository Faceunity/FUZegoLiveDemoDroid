package com.zego.videofilter.faceunity.entity;

/**
 * 美妆组合妆容
 *
 * @author Richie on 2019.11.11
 */
public class Makeup {
    private int iconId;
    private String name;
    private String filePath;
    private boolean isNeedFlipPoints;
    private int itemHandle;

    public Makeup(Makeup makeup) {
        this(makeup.iconId, makeup.name, makeup.filePath, makeup.isNeedFlipPoints);
    }

    public Makeup(int iconId, String name, String filePath, boolean isNeedFlipPoints) {
        this.iconId = iconId;
        this.name = name;
        this.filePath = filePath;
        this.isNeedFlipPoints = isNeedFlipPoints;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isNeedFlipPoints() {
        return isNeedFlipPoints;
    }

    public void setNeedFlipPoints(boolean needFlipPoints) {
        isNeedFlipPoints = needFlipPoints;
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getItemHandle() {
        return itemHandle;
    }

    public void setItemHandle(int itemHandle) {
        this.itemHandle = itemHandle;
    }

    @Override
    public String toString() {
        return "Makeup{" +
                "iconId=" + iconId +
                ", name='" + name + '\'' +
                ", filePath='" + filePath + '\'' +
                ", isNeedFlipPoints=" + isNeedFlipPoints +
                ", itemHandle=" + itemHandle +
                '}';
    }

}
