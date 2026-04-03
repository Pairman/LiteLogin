package org.eu.pnxlr.git.litelogin.api.internal.util.reflect;

import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.*;

/**
 * Reflection utilities.
 */
@ApiStatus.Internal
public class ReflectUtil {
    private ReflectUtil() {
    }

    /**
     * Updates the Accessible flag.
     */
    public static Method handleAccessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    /**
     * Updates the Accessible flag.
     */
    public static<T> Constructor<T> handleAccessible(Constructor<T> constructor) {
        constructor.setAccessible(true);
        return constructor;
    }

    /**
     * Updates the Accessible flag.
     */
    public static Field handleAccessible(Field field) {
        field.setAccessible(true);
        return field;
    }
}
