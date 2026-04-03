package org.eu.pnxlr.git.litelogin.api.internal.main;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LiteLoginConstants {
    public static final String ROOT_COMMAND_LITERAL = "litelogin";
    public static final String COMMAND_LITELOGIN_HELP_PERMISSION = "command.litelogin.help";
    public static final String COMMAND_LITELOGIN_ADD_PERMISSION = "command.litelogin.add";
    public static final String COMMAND_LITELOGIN_REMOVE_PERMISSION = "command.litelogin.remove";
    public static final String COMMAND_LITELOGIN_LIST_PERMISSION = "command.litelogin.list";
    public static final String CHAT_SESSION_PIPELINE_KEY = "LiteLoginChatSession";
    public static final String HTTP_USER_AGENT = "LiteLogin/v1.0";
    public static final String DATABASE_NAME = "litelogin";
}
