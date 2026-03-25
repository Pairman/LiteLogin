package org.eu.pnxlr.git.litelogin.core.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.ISender;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.command.Permissions;
import org.eu.pnxlr.git.litelogin.core.command.argument.ProfileArgumentType;
import org.eu.pnxlr.git.litelogin.core.command.argument.StringArgumentType;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Pattern;

public class MRenameCommand {
    private final CommandHandler handler;

    public MRenameCommand(CommandHandler handler) {
        this.handler = handler;
    }

    public LiteralArgumentBuilder<ISender> register(LiteralArgumentBuilder<ISender> literalArgumentBuilder) {
        return literalArgumentBuilder
                .then(handler.argument("newname", StringArgumentType.string())
                        .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_RENAME_ONESELF))
                        .executes(this::executeRename))
                .then(handler.argument("newname", StringArgumentType.string())
                        .then(handler.argument("profile", ProfileArgumentType.profile())
                                .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_RENAME_OTHER))
                                .executes(this::executeRenameOther)));
    }

    @SneakyThrows
    private int executeRenameOther(CommandContext<ISender> context) {
        processRename(context, StringArgumentType.getString(context, "newname"), ProfileArgumentType.getProfile(context, "profile"));
        return 0;
    }

    @SneakyThrows
    private int executeRename(CommandContext<ISender> context) {
        String newName = StringArgumentType.getString(context, "newname");
        handler.requireDataCacheArgumentSelf(context);
        processRename(context, newName, new ProfileArgumentType.ProfileArgument(
                context.getSource().getAsPlayer().getUniqueId(),
                context.getSource().getAsPlayer().getName()
        ));
        return 0;
    }

    private void processRename(CommandContext<ISender> context, String newName, ProfileArgumentType.ProfileArgument argument) {
        if (newName.equals(argument.getProfileName())) {
            context.getSource().sendMessagePL("§cThe old name and the new name are identical.");
            return;
        }

        String nameAllowedRegular = CommandHandler.getCore().getPluginConfig().getNameAllowedRegular();
        if (!ValueUtil.isEmpty(nameAllowedRegular) && !Pattern.matches(nameAllowedRegular, newName)) {
            context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§cThe new name §e{name}§c does not match the required pattern §e{regular}§c.",
                    new Pair<>("name", newName),
                    new Pair<>("regular", nameAllowedRegular)
            ));
            return;
        }

        handler.getSecondaryConfirmationHandler().submit(context.getSource(), () -> {
                    try {
                        CommandHandler.getCore().getSqlManager().getInGameProfileTable().updateUsername(argument.getProfileUUID(), newName);
                        context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                                "§aRenamed profile §8(§e{profile_name}§8)[§e{profile_uuid}§8]§a from §e{profile_name}§a to §e{new_name}§a.",
                                new Pair<>("profile_name", argument.getProfileName()),
                                new Pair<>("new_name", newName),
                                new Pair<>("profile_uuid", argument.getProfileUUID())
                        ));
                        CommandHandler.getCore().getPlugin().getRunServer().getPlayerManager().kickPlayerIfOnline(
                                argument.getProfileUUID(),
                                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                                        "§aYour profile name was changed by an administrator from §e{profile_name}§a to §e{new_name}§a.",
                                        new Pair<>("profile_name", argument.getProfileName()),
                                        new Pair<>("new_name", newName),
                                        new Pair<>("profile_uuid", argument.getProfileUUID())
                                )
                        );
                    } catch (SQLIntegrityConstraintViolationException e) {
                        context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                                "§cThe new name §e{name}§c is already used by another profile.",
                                new Pair<>("name", newName)
                        ));
                    }
                },
                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "§cRename profile §8(§e{profile_name}§8)[§e{profile_uuid}§8]§c from §e{profile_name}§c to §e{new_name}§c.",
                        new Pair<>("profile_name", argument.getProfileName()),
                        new Pair<>("new_name", newName),
                        new Pair<>("profile_uuid", argument.getProfileUUID())
                ),
                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "§cThe old name §e{profile_name}§c will be released immediately and can be claimed by another profile.",
                        new Pair<>("profile_name", argument.getProfileName())
                )
        );
    }
}
