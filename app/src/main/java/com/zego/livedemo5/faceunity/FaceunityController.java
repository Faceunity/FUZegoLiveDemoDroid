package com.zego.livedemo5.faceunity;

/**
 * Created by tujh on 2017/7/13.
 */

public interface FaceunityController {

    /**
     * 道具贴纸选择
     *
     * @param effectItemName 道具贴纸文件名
     */
    void onEffectItemSelected(String effectItemName);

    /**
     * 滤镜选择
     *
     * @param filterName 滤镜名称
     */
    void onFilterSelected(String filterName);

    /**
     * 磨皮选择
     *
     * @param level 磨皮level
     */
    void onBlurLevelSelected(int level);

    /**
     * 美白选择
     *
     * @param progress 美白滑动条进度
     * @param max      美白滑动条最大值
     */
    void onColorLevelSelected(int progress, int max);

    /**
     * 瘦脸选择
     *
     * @param progress 瘦脸滑动进度
     * @param max      瘦脸滑动条最大值
     */
    void onCheekThinSelected(int progress, int max);

    /**
     * 大眼选择
     *
     * @param progress 大眼滑动进度
     * @param max      大眼滑动条最大值
     */
    void onEnlargeEyeSelected(int progress, int max);

    /**
     * 脸型选择
     */
    void onFaceShapeSelected(int faceShape);

    /**
     * 美型程度选择
     */
    void onFaceShapeLevelSelected(int progress, int max);

    /**
     * 美白程度选择
     */
    void onRedLevelSelected(int progress, int max);
}
