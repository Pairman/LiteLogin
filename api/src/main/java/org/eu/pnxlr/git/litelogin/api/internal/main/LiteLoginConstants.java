package org.eu.pnxlr.git.litelogin.api.internal.main;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LiteLoginConstants {
    public static final String ROOT_COMMAND_LITERAL = "litelogin";
    public static final String CORE_NESTED_JAR_RESOURCE = "LiteLogin-Core.JarFile";
    public static final String CORE_CLASS_NAME = "org.eu.pnxlr.git.litelogin.core.main.Core";
    public static final String VELOCITY_INJECTOR_NESTED_JAR_RESOURCE = "LiteLogin-Velocity-Injector.JarFile";
    public static final String VELOCITY_INJECTOR_CLASS_NAME = "org.eu.pnxlr.git.litelogin.velocity.injector.VelocityInjector";
    public static final String CHAT_SESSION_PIPELINE_KEY = "LiteLoginChatSession";
    public static final String HTTP_USER_AGENT = "LiteLogin/v1.0";
    public static final String DATABASE_NAME = "litelogin";
}
