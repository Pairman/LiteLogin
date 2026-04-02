package org.eu.pnxlr.git.litelogin.core.command;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Permission constants.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Permissions {
    public static final String COMMAND_TAB_COMPLETE = "command.litelogin.tab.complete";
    public static final String COMMAND_LITELOGIN_HELP = "command.litelogin.help";
    public static final String COMMAND_LITELOGIN_ADD = "command.litelogin.add";
    public static final String COMMAND_LITELOGIN_REMOVE = "command.litelogin.remove";
    public static final String COMMAND_LITELOGIN_LIST = "command.litelogin.list";
}
