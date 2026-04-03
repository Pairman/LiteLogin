package org.eu.pnxlr.git.litelogin.core.configuration;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
/**
 * Represents a skin restoration configuration.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class SkinRestorerConfig {
    private static final int RESTORER_TIMEOUT_MILLIS = 30000;
    private static final int RESTORER_RETRY_COUNT = 3;
    private static final long RESTORER_RETRY_DELAY_MILLIS = 1000L;

    private final boolean enabled;
    private final int timeout = RESTORER_TIMEOUT_MILLIS;
    private final int retry = RESTORER_RETRY_COUNT;
    private final long retryDelay = RESTORER_RETRY_DELAY_MILLIS;

    public static SkinRestorerConfig fromBoolean(boolean enabled) {
        return new SkinRestorerConfig(enabled);
    }
}
