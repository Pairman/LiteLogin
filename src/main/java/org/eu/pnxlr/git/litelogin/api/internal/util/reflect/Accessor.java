package org.eu.pnxlr.git.litelogin.api.internal.util.reflect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Object accessor.
 */
@ApiStatus.Internal
@AllArgsConstructor
public class Accessor {
    @Getter
    private final Class<?> classHandle;

    /**
     * Finds the first method that matches the given predicate.
     */
    public Method findFirstMethod(boolean declared, Function<Method, Boolean> function, String exceptionMessage) throws NoSuchMethodException {
        for (Method method : declared ? classHandle.getDeclaredMethods() : classHandle.getMethods()) {
            if (function.apply(method)) {
                return method;
            }
        }
        throw new NoSuchMethodException(exceptionMessage);
    }

    /**
     * Finds the first field that matches the given predicate.
     */
    public Field findFirstField(boolean declared, Function<Field, Boolean> function, String exceptionMessage) throws NoSuchFieldException {
        for (Field field : declared ? classHandle.getDeclaredFields() : classHandle.getFields()) {
            if (function.apply(field)) {
                return field;
            }
        }
        throw new NoSuchFieldException(exceptionMessage);
    }

    /**
     * Finds the first method with the given name.
     */
    public Method findFirstMethodByName(boolean declared, String name) throws NoSuchMethodException {
        return findFirstMethod(declared, m -> m.getName().equals(name), String.format("%s(dedicated = %b) -> %s", classHandle.getName(), declared, name));
    }

    /**
     * Finds the first field with the given type.
     */
    public Field findFirstFieldByType(boolean declared, Type fieldType) throws NoSuchFieldException {
        return findFirstField(declared, f -> f.getType().equals(fieldType), String.format("%s(dedicated = %b) -> %s", classHandle.getName(), declared, fieldType));
    }
}
