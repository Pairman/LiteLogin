package org.eu.pnxlr.git.litelogin.api.internal.util.reflect;

import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.function.Function;

/**
 * Reflection utilities.
 */
@ApiStatus.Internal
public class ReflectUtil {

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

    /**
     * Finds the first instance field declared in the target class.
     */
    public static Field findNoStaticField(Class<?> target, Type fieldType) throws NoSuchFieldException {
        for (Field field : target.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (field.getType() == fieldType) {
                return field;
            }
        }
        for (Field field : target.getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (field.getType() == fieldType) {
                return field;
            }
        }
        throw new NoSuchFieldException("Type: " + fieldType.getTypeName());
    }

    /**
     * Finds the first instance method declared in the target class.
     */
    public static Method findNoStaticMethodByParameters(Class<?> target, Type... fieldTypes) throws NoSuchMethodException {
        for (Method method : target.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Arrays.equals(method.getParameterTypes(), fieldTypes)) {
                return method;
            }
        }
        throw new NoSuchMethodException(target.getName() + " Types: " + Arrays.toString(fieldTypes));
    }

    /**
     * Finds the first static method declared in the target class.
     */
    public static Method findStaticMethodByParameters(Class<?> target, Type... fieldTypes) throws NoSuchMethodException {
        for (Method method : target.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) continue;
            if (Arrays.equals(method.getParameterTypes(), fieldTypes)) {
                return method;
            }
        }
        throw new NoSuchMethodException(target.getName() + " Types: " + Arrays.toString(fieldTypes));
    }

    public static Method findStaticMethodByReturnTypeAndParameters(Class<?> target, Type returnType, Type... fieldTypes) throws NoSuchMethodException {
        for (Method method : target.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) continue;
            if (Arrays.equals(method.getParameterTypes(), fieldTypes)) {
                if (returnType.equals(method.getReturnType())) {
                    return method;
                }
            }
        }
        throw new NoSuchMethodException(target.getName() + " Types: " + Arrays.toString(fieldTypes));
    }

    public static Method findNoStaticMethodByReturnType(Class<?> target, Type returnType) throws NoSuchMethodException {
        for (Method method : target.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) continue;
            if (method.getReturnType().equals(returnType)) {
                return method;
            }
        }
        throw new NoSuchMethodException(target.getName() + " Types: " + returnType);
    }

    /**
     * Replaces one field in a record object.
     *
     * @param source   record object
     * @param match    object matching function
     * @param redirect redirect object function
     * @return modified record, leaving the source record unchanged
     */
    public static Object redirectRecordObject(Object source, Function<Object, Boolean> match, Function<Object, Object> redirect)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        LinkedHashMap<Field, Object> fieldObjectMap = new LinkedHashMap<>();
        for (Field field : source.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object value = ReflectUtil.handleAccessible(field).get(source);
            if (match.apply(value)) {
                value = redirect.apply(value);
            }
            fieldObjectMap.put(field, value);
        }

        final Constructor<?> declaredConstructor = source.getClass().getDeclaredConstructor(
                fieldObjectMap.keySet().stream().map(Field::getType).toArray(Class[]::new)
        );

        return declaredConstructor.newInstance(fieldObjectMap.values().toArray());
    }
}
