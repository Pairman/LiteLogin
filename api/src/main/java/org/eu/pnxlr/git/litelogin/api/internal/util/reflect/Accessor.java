package org.eu.pnxlr.git.litelogin.api.internal.util.reflect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Object accessor.
 */
@ApiStatus.Internal
@AllArgsConstructor
public class Accessor {
    @Getter
    private final Class<?> classHandle;

    private <V> List<V> getElements(V[] vs, Function<V, Boolean> function) {
        return Arrays.stream(vs).filter(function::apply).collect(Collectors.toList());
    }

    /**
     * Finds all methods that match the given predicate.
     */
    public List<Method> findAllMethods(boolean declared, Function<Method, Boolean> function) {
        return getElements(declared ? classHandle.getDeclaredMethods() : classHandle.getMethods(), function);
    }

    /**
     * Finds all fields that match the given predicate.
     */
    public List<Field> findAllFields(boolean declared, Function<Field, Boolean> function) {
        return getElements(declared ? classHandle.getDeclaredFields() : classHandle.getFields(), function);
    }

    /**
     * Finds all constructors that match the given predicate.
     */
    public List<Constructor<?>> findAllConstructors(boolean declared, Function<Constructor<?>, Boolean> function) {
        return getElements(declared ? classHandle.getDeclaredConstructors() : classHandle.getConstructors(), function);
    }

    /**
     * Finds the first method that matches the given predicate.
     */
    public Method findFirstMethod(boolean declared, Function<Method, Boolean> function, String exceptionMessage) throws NoSuchMethodException {
        List<Method> elements = getElements(declared ? classHandle.getDeclaredMethods() : classHandle.getMethods(), function);
        if (elements.size() == 0) throw new NoSuchMethodException(exceptionMessage);
        return elements.get(0);
    }

    /**
     * Finds the first field that matches the given predicate.
     */
    public Field findFirstField(boolean declared, Function<Field, Boolean> function, String exceptionMessage) throws NoSuchFieldException {
        List<Field> elements = getElements(declared ? classHandle.getDeclaredFields() : classHandle.getFields(), function);
        if (elements.size() == 0) throw new NoSuchFieldException(exceptionMessage);
        return elements.get(0);
    }

    /**
     * Finds the first constructor that matches the given predicate.
     */
    public Constructor<?> findFirstConstructors(boolean declared, Function<Constructor<?>, Boolean> function, String exceptionMessage) throws ReflectiveOperationException {
        List<Constructor<?>> elements = getElements(declared ? classHandle.getDeclaredConstructors() : classHandle.getConstructors(), function);
        if (elements.size() == 0) throw new ReflectiveOperationException(exceptionMessage);
        return elements.get(0);
    }

    /**
     * Finds the first method with the given name.
     */
    public Method findFirstMethodByName(boolean declared, String name) throws NoSuchMethodException {
        return findFirstMethod(declared, m -> m.getName().equals(name), String.format("%s(dedicated = %b) -> %s", classHandle.getName(), declared, name));
    }

    /**
     * Finds the first method with the given parameter types.
     */
    public Method findFirstMethodByParameterTypes(boolean declared, Type[] types) throws NoSuchMethodException {
        return findFirstMethod(declared, m -> Arrays.equals(types, m.getParameterTypes()), String.format("%s(dedicated = %b) -> %s", classHandle.getName(), declared, Arrays.toString(types)));
    }

    /**
     * Finds the first method with the given return type.
     */
    public Method findFirstMethodByReturnType(boolean declared, Type returnType) throws NoSuchMethodException {
        return findFirstMethod(declared, m -> m.getReturnType().equals(returnType), String.format("%s(dedicated = %b) -> returnType = %s", classHandle.getName(), declared, returnType));
    }

    /**
     * Finds the first field with the given name.
     */
    public Field findFirstFieldByName(boolean declared, String name) throws NoSuchFieldException {
        return findFirstField(declared, f -> f.getName().equals(name), String.format("%s(dedicated = %b) -> %s", classHandle.getName(), declared, name));
    }

    /**
     * Finds the first field with the given type.
     */
    public Field findFirstFieldByType(boolean declared, Type fieldType) throws NoSuchFieldException {
        return findFirstField(declared, f -> f.getType().equals(fieldType), String.format("%s(dedicated = %b) -> %s", classHandle.getName(), declared, fieldType));
    }

    /**
     * Finds the first field with the given generic type.
     */
    public Constructor<?> findFirstConstructorByParameterTypes(boolean declared, Type[] types) throws ReflectiveOperationException {
        return findFirstConstructors(declared, c -> Arrays.equals(c.getParameterTypes(), types), String.format("%s(dedicated = %b) -> %s", classHandle.getName(), declared, types));
    }
}
