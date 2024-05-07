package com.tacz.guns.client.animation.thrid;

import com.google.common.collect.Maps;
import com.tacz.guns.api.client.animation.IThirdPersonAnimation;

import java.util.Map;

/**
 * 简单的第三人称持枪动画 Manager
 */
public final class ThirdPersonManager {
    private static final Map<String, IThirdPersonAnimation> CACHE = Maps.newHashMap();
    private static final IThirdPersonAnimation DEFAULT = new DefaultAnimation();

    public static void registerInner() {
        register("default", DEFAULT);
    }

    public static void register(String name, IThirdPersonAnimation animation) {
        CACHE.put(name, animation);
    }

    public static IThirdPersonAnimation getAnimation(String name) {
        return CACHE.getOrDefault(name, DEFAULT);
    }
}
