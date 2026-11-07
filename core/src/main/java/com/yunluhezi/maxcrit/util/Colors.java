package com.yunluhezi.maxcrit.util;

import com.badlogic.gdx.graphics.Color;

/** 颜色工具：避免在调用点写「三元表达式 + 逗号列表」这种 Java 不允许的语法。 */
public final class Colors {

    private Colors() {}

    public static Color of(float r, float g, float b, float a) {
        return new Color(r, g, b, a);
    }

    /** 条件取色：cond 为真用 a，否则用 b。 */
    public static Color pick(boolean cond, Color a, Color b) {
        return cond ? a : b;
    }

    public static Color rgba(float r, float g, float b, float a) {
        return new Color(r, g, b, a);
    }
}
