package com.zego.videofilter.faceunity.entity;

import com.zego.videofilter.R;

import java.util.ArrayList;

/**
 * 道具贴纸列表
 *
 * @author Richie on 2019.12.20
 */
public enum EffectEnum {
    /**
     * 道具贴纸
     */
    Effect_none(R.drawable.ic_delete_all, "", "none"),

    Effect_sdlu(R.drawable.sdlu, "effect/sdlu.bundle", "sdlu"),
    Effect_daisypig(R.drawable.daisypig, "effect/daisypig.bundle", "daisypig"),
    Effect_fashi(R.drawable.fashi, "effect/fashi.bundle", "fashi"),
    Effect_xueqiu_lm_fu(R.drawable.xueqiu_lm_fu, "effect/xueqiu_lm_fu.bundle", "xueqiu_lm_fu"),
    Effect_wobushi(R.drawable.wobushi, "effect/wobushi.bundle", "wobushi"),
    Effect_gaoshiqing(R.drawable.gaoshiqing, "effect/gaoshiqing.bundle", "gaoshiqing");

    private int iconId;
    private String filePath;
    private String description;

    EffectEnum(int iconId, String filePath, String description) {
        this.iconId = iconId;
        this.filePath = filePath;
        this.description = description;
    }

    public Effect create() {
        return new Effect(iconId, filePath, description);
    }

    public static ArrayList<Effect> getEffects() {
        EffectEnum[] effectEnums = EffectEnum.values();
        ArrayList<Effect> effects = new ArrayList<>(effectEnums.length);
        for (EffectEnum e : effectEnums) {
            effects.add(e.create());
        }
        return effects;
    }
}
