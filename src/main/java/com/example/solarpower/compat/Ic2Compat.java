package com.example.solarpower.compat;

import net.minecraftforge.fml.common.Loader;

/**
 * IC2 软依赖判定。
 * <p>本类刻意不引用任何 {@code ic2.*} 类型，因此 IC2 缺席时也能安全加载——
 * 各处调用点先用 {@link #LOADED} 判断，为 false 时对应的 IC2 方法体永远不会被 JVM 解析。
 */
public final class Ic2Compat {

    /** 服务器/客户端是否装了 IC2。 */
    public static final boolean LOADED = Loader.isModLoaded("ic2");

    private Ic2Compat() {
    }
}