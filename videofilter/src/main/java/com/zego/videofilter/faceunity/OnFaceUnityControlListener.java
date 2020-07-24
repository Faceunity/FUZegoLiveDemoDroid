package com.zego.videofilter.faceunity;

import com.zego.videofilter.faceunity.entity.Effect;
import com.zego.videofilter.faceunity.entity.Makeup;

/**
 * FURenderer与界面之间的交互接口
 *
 * @author Richie on 2019.07.18
 */
public interface OnFaceUnityControlListener {
    /**
     * 选择道具贴纸
     *
     * @param effect
     */
    void onEffectSelected(Effect effect);

    /**
     * 加载美体模块
     */
    void loadBodySlimModule();

    /**
     * 销毁美体模块
     */
    void destroyBodySlimModule();

    /**
     * 加载美妆模块
     */
    void loadMakeupModule();

    /**
     * 销毁美妆模块
     */
    void destroyMakeupModule();

    /**
     * 选择美妆
     *
     * @param makeup
     */
    void selectMakeup(Makeup makeup);

    /**
     * 设置美妆强度
     *
     * @param intensity
     */
    void setMakeupIntensity(float intensity);

    /**
     * 设置滤镜名称
     *
     * @param name 滤镜名称
     */
    void onFilterNameSelected(String name);

    /**
     * 调节滤镜强度
     *
     * @param level 滤镜程度
     */
    void onFilterLevelSelected(float level);

    /**
     * 调节磨皮
     *
     * @param level 磨皮程度
     */
    void onBlurLevelSelected(float level);

    /**
     * 调节美白
     *
     * @param level 美白程度
     */
    void onColorLevelSelected(float level);

    /**
     * 调节红润
     *
     * @param level 红润程度
     */
    void onRedLevelSelected(float level);

    /**
     * 设置瘦身程度
     *
     * @param intensity
     */
    void setBodySlimIntensity(float intensity);

    /**
     * 设置长腿程度
     *
     * @param intensity
     */
    void setLegSlimIntensity(float intensity);

    /**
     * 设置细腰程度
     *
     * @param intensity
     */
    void setWaistSlimIntensity(float intensity);

    /**
     * 设置美肩程度
     *
     * @param intensity
     */
    void setShoulderSlimIntensity(float intensity);

    /**
     * 设置美臀程度
     *
     * @param intensity
     */
    void setHipSlimIntensity(float intensity);

    /**
     * 设置小头程度
     *
     * @param intensity
     */
    void setHeadSlimIntensity(float intensity);

    /**
     * 设置瘦腿程度
     *
     * @param intensity
     */
    void setLegThinSlimIntensity(float intensity);

    /**
     * 设置去黑眼圈强度
     *
     * @param strength
     */
    void setRemovePouchStrength(float strength);

    /**
     * 设置去法令纹强度
     *
     * @param strength
     */
    void setRemoveNasolabialFoldsStrength(float strength);

    /**
     * 设置微笑嘴角强度
     *
     * @param intensity
     */
    void setSmileIntensity(float intensity);

    /**
     * 设置开眼角强度
     *
     * @param intensity
     */
    void setCanthusIntensity(float intensity);

    /**
     * 设置人中长度
     *
     * @param intensity
     */
    void setPhiltrumIntensity(float intensity);

    /**
     * 设置鼻子长度
     *
     * @param intensity
     */
    void setLongNoseIntensity(float intensity);

    /**
     * 设置眼睛间距
     *
     * @param intensity
     */
    void setEyeSpaceIntensity(float intensity);

    /**
     * 设置眼睛角度
     *
     * @param intensity
     */
    void setEyeRotateIntensity(float intensity);

    /**
     * 亮眼
     *
     * @param level
     */
    void onEyeBrightSelected(float level);

    /**
     * 美牙
     *
     * @param level
     */
    void onToothWhitenSelected(float level);

    /**
     * 大眼
     *
     * @param level
     */
    void onEyeEnlargeSelected(float level);

    /**
     * 瘦脸
     *
     * @param level
     */
    void onCheekThinningSelected(float level);

    /**
     * 下巴
     *
     * @param level
     */
    void onIntensityChinSelected(float level);

    /**
     * 额头
     *
     * @param level
     */
    void onIntensityForeheadSelected(float level);

    /**
     * 瘦鼻
     *
     * @param level
     */
    void onIntensityNoseSelected(float level);

    /**
     * 嘴形
     *
     * @param level
     */
    void onIntensityMouthSelected(float level);

    /**
     * 窄脸
     *
     * @param level
     */
    void onCheekNarrowSelected(float level);

    /**
     * 小脸
     *
     * @param level
     */
    void onCheekSmallSelected(float level);

    /**
     * V脸
     *
     * @param level
     */
    void onCheekVSelected(float level);

    /**
     * 美颜效果全局开关
     *
     * @param isOn
     */
    void setBeautificationOn(boolean isOn);

}
