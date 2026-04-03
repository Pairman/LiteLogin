package org.eu.pnxlr.git.litelogin.api.internal.util.tuple;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a triple of values.
 */
@Data
@ApiStatus.Internal
@AllArgsConstructor
public class Triple<V1, V2, V3> {
    private final V1 value1;
    private final V2 value2;
    private final V3 value3;
}
