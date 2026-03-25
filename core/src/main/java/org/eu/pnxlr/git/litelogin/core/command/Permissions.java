package org.eu.pnxlr.git.litelogin.core.command;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 储存权限的地方
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Permissions {
    public static final String COMMAND_TAB_COMPLETE = "command.litelogin.tab.complete";
    public static final String COMMAND_LITELOGIN_RELOAD = "command.litelogin.reload";
    public static final String COMMAND_LITELOGIN_CONFIRM = "command.litelogin.confirm";
    public static final String COMMAND_LITELOGIN_ERASE_USERNAME = "command.litelogin.eraseusername";
    public static final String COMMAND_LITELOGIN_WHITELIST_ADD = "command.litelogin.whitelist.add";
    public static final String COMMAND_LITELOGIN_WHITELIST_REMOVE = "command.litelogin.whitelist.remove";
    public static final String COMMAND_LITELOGIN_WHITELIST_LIST = "command.litelogin.whitelist.list";
    public static final String COMMAND_LITELOGIN_WHITELIST_LIST_VERBOSE = "command.litelogin.whitelist.list.verbose";
    public static final String COMMAND_LITELOGIN_WHITELIST_SPECIFIC_ADD = "command.litelogin.whitelist.specific.add";
    public static final String COMMAND_LITELOGIN_WHITELIST_SPECIFIC_REMOVE = "command.litelogin.whitelist.specific.remove";
    public static final String COMMAND_LITELOGIN_RENAME_ONESELF = "command.litelogin.rename.oneself";
    public static final String COMMAND_LITELOGIN_RENAME_OTHER = "command.litelogin.rename.other";
    public static final String COMMAND_LITELOGIN_ERASE_ALL_USERNAMES = "command.litelogin.eraseallusernames";
    public static final String COMMAND_LITELOGIN_CURRENT_ONESELF = "command.litelogin.current.oneself";
    public static final String COMMAND_LITELOGIN_CURRENT_OTHER = "command.litelogin.current.other";
    public static final String COMMAND_LITELOGIN_PROFILE_CREATE = "command.litelogin.profile.create";
    public static final String COMMAND_LITELOGIN_PROFILE_SET_ONESELF = "command.litelogin.profile.set.oneself";
    public static final String COMMAND_LITELOGIN_PROFILE_SET_OTHER = "command.litelogin.profile.set.other";
    public static final String COMMAND_LITELOGIN_PROFILE_REMOVE = "command.litelogin.profile.remove";
    public static final String COMMAND_LITELOGIN_LIST = "command.litelogin.list";
    public static final String COMMAND_LITELOGIN_FIND_ONLINE = "command.litelogin.find.online";
    public static final String COMMAND_LITELOGIN_FIND_PROFILE = "command.litelogin.find.profile";
    public static final String COMMAND_LITELOGIN_LINK_TO = "command.litelogin.link.to";
    public static final String COMMAND_LITELOGIN_LINK_ACCEPT = "command.litelogin.link.accept";
    public static final String COMMAND_LITELOGIN_LINK_CODE = "command.litelogin.link.code";
}
