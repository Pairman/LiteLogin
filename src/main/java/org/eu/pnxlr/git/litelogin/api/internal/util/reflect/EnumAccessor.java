package org.eu.pnxlr.git.litelogin.api.internal.util.reflect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

/**
 * Enum constant accessor.
 */
@ApiStatus.Internal
@AllArgsConstructor
public class EnumAccessor {
    @Getter
    private final Class<?> enumClass;

    /**
     * Returns all enum constants.
     */
    private Enum<?>[] getValues() {
        return (Enum<?>[]) enumClass.getEnumConstants();
    }

    public Enum<?> findByName(String name) throws ReflectiveOperationException {
        for (Enum<?> value : getValues()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        throw new ReflectiveOperationException(String.format("%s -> %s", enumClass.getName(), name));
    }
}
