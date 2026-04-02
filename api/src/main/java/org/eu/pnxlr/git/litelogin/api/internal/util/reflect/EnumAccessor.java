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
    public Enum<?>[] getValues() {
        return (Enum<?>[]) enumClass.getEnumConstants();
    }

    /**
     * Returns the enum constant at the given index.
     */
    public Enum<?> indexOf(int index) {
        return getValues()[index];
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
