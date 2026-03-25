package org.eu.pnxlr.git.litelogin.core.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlayer;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.ISender;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.command.Permissions;
import org.eu.pnxlr.git.litelogin.core.command.argument.StringArgumentType;
import org.eu.pnxlr.git.litelogin.core.configuration.service.BaseServiceConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RootCommand {
    private final CommandHandler handler;

    public RootCommand(CommandHandler handler) {
        this.handler = handler;
    }

    public LiteralArgumentBuilder<ISender> register(LiteralArgumentBuilder<ISender> literalArgumentBuilder) {
        return literalArgumentBuilder.then(handler.literal("reload")
                        .requires(sender -> sender.hasPermission(Permissions.COMMAND_LITELOGIN_RELOAD))
                        .executes(this::executeReload))
                .then(handler.literal("eraseUsername")
                        .requires(sender -> sender.hasPermission(Permissions.COMMAND_LITELOGIN_ERASE_USERNAME))
                        .then(handler.argument("username", StringArgumentType.string())
                                .executes(this::executeEraseUsername)))
                .then(handler.literal("eraseAllUsernames")
                        .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_ERASE_ALL_USERNAMES))
                        .executes(this::executeEraseAllUsernames))
                .then(handler.literal("confirm")
                        .requires(sender -> sender.hasPermission(Permissions.COMMAND_LITELOGIN_CONFIRM))
                        .executes(this::executeConfirm))
                .then(handler.literal("list")
                        .requires(sender -> sender.hasPermission(Permissions.COMMAND_LITELOGIN_LIST))
                        .executes(this::executeList))
                .then(new MWhitelistCommand(handler).register(handler.literal("whitelist")))
                .then(new MProfileCommand(handler).register(handler.literal("profile")))
                .then(new MRenameCommand(handler).register(handler.literal("rename")))
                .then(new MFindCommand(handler).register(handler.literal("find")))
                .then(new MInfoCommand(handler).register(handler.literal("info")))
                .then(new MLinkCommand(handler).register(handler.literal("link")));
    }

    private int executeList(CommandContext<ISender> context) {
        Set<IPlayer> onlinePlayers = CommandHandler.getCore().getPlugin().getRunServer().getPlayerManager().getOnlinePlayers();
        Map<Integer, List<IPlayer>> identifiedPlayerMap = new HashMap<>();
        for (IPlayer player : onlinePlayers) {
            Pair<GameProfile, Integer> profile = CommandHandler.getCore().getPlayerHandler().getPlayerOnlineProfile(player.getUniqueId());
            int sid = profile == null ? -1 : profile.getValue2();
            identifiedPlayerMap.computeIfAbsent(sid, ignored -> new ArrayList<>()).add(player);
        }

        CommandHandler.getCore().getPluginConfig().getServiceIdMap().forEach((key, value) ->
                identifiedPlayerMap.computeIfAbsent(key, ignored -> new ArrayList<>()));

        String list = identifiedPlayerMap.entrySet().stream().map(entry -> {
            String serviceName;
            if (entry.getKey() == -1) {
                serviceName = "§8Unidentified";
            } else {
                BaseServiceConfig service = CommandHandler.getCore().getPluginConfig().getServiceIdMap().get(entry.getKey());
                serviceName = service == null ? "§8Unknown" : service.getName();
            }
            String playerList = entry.getValue().stream().map(IPlayer::getName).collect(Collectors.joining(", §r"));
            return org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§a[{service_name}§8(sid = {service_id})§a] §e({count}): {list}",
                    new Pair<>("service_name", serviceName),
                    new Pair<>("service_id", entry.getKey()),
                    new Pair<>("count", entry.getValue().size()),
                    new Pair<>("list", playerList)
            );
        }).collect(Collectors.joining("\n§r"));

        context.getSource().sendMessagePL(
                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "{list}\nTotal online players: {count}",
                        new Pair<>("list", list),
                        new Pair<>("count", onlinePlayers.size())
                )
        );
        return 0;
    }

    private int executeEraseAllUsernames(CommandContext<ISender> context) {
        handler.getSecondaryConfirmationHandler().submit(context.getSource(), () -> {
                    int count = CommandHandler.getCore().getSqlManager().getInGameProfileTable().eraseAllUsername();
                    CommandHandler.getCore().getPlugin().getRunServer().getPlayerManager().kickAll("§cAll in-game profile names have been reclaimed by an administrator.");
                    context.getSource().sendMessagePL(
                            org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                                    "§aReclaimed all profile names. Affected rows: §e{count}§a.",
                                    new Pair<>("count", count)
                            )
                    );
                },
                "§cReclaim all profile names.",
                "§cAll online players will be disconnected."
        );
        return 0;
    }

    @SneakyThrows
    private int executeConfirm(CommandContext<ISender> context) {
        handler.getSecondaryConfirmationHandler().confirm(context.getSource());
        return 0;
    }

    @SneakyThrows
    private int executeEraseUsername(CommandContext<ISender> context) {
        String username = StringArgumentType.getString(context, "username").toLowerCase(Locale.ROOT);
        UUID profileUuid = CommandHandler.getCore().getSqlManager().getInGameProfileTable().getInGameUUIDIgnoreCase(username);
        if (profileUuid == null) {
            context.getSource().sendMessagePL(
                    org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                            "§cThe profile name §e{name}§c is not used by any profile.",
                            new Pair<>("name", username)
                    )
            );
            return 0;
        }

        handler.getSecondaryConfirmationHandler().submit(context.getSource(), () -> {
                    int count = CommandHandler.getCore().getSqlManager().getInGameProfileTable().eraseUsername(username);
                    String kickMsg = org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                            "§cYour in-game profile name §e{name}§c has been reclaimed by the system. Please contact the server administrator if this is unexpected.",
                            new Pair<>("name", username)
                    );
                    CommandHandler.getCore().getPlugin().getRunServer().getPlayerManager().kickPlayerIfOnline(username, kickMsg);
                    if (count == 0) {
                        context.getSource().sendMessagePL(
                                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                                        "§cThe profile name §e{name}§c is not used by any profile.",
                                        new Pair<>("name", username)
                                )
                        );
                    } else {
                        context.getSource().sendMessagePL(
                                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                                        "§aReclaimed profile name §e{name}§a.",
                                        new Pair<>("name", username)
                                )
                        );
                    }
                },
                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "§cReclaim profile name §e{name}§c so another profile can use it.",
                        new Pair<>("name", username)
                ),
                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "§cThe profile name §e{name}§c will be released immediately, and any online player using it will be disconnected.",
                        new Pair<>("name", username)
                )
        );
        return 0;
    }

    @SneakyThrows
    private int executeReload(CommandContext<ISender> context) {
        CommandHandler.getCore().reload();
        context.getSource().sendMessagePL("§aAll LiteLogin files have been reloaded.");
        return 0;
    }
}
