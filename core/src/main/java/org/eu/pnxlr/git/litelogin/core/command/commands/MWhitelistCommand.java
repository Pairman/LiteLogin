package org.eu.pnxlr.git.litelogin.core.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlayer;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.ISender;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.command.Permissions;
import org.eu.pnxlr.git.litelogin.core.command.argument.OnlineArgumentType;
import org.eu.pnxlr.git.litelogin.core.command.argument.StringArgumentType;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class MWhitelistCommand {
    private final CommandHandler handler;

    public MWhitelistCommand(CommandHandler handler) {
        this.handler = handler;
    }

    public LiteralArgumentBuilder<ISender> register(LiteralArgumentBuilder<ISender> literalArgumentBuilder) {
        return literalArgumentBuilder
                .then(handler.literal("add")
                        .requires(sender -> sender.hasPermission(Permissions.COMMAND_LITELOGIN_WHITELIST_ADD))
                        .then(handler.argument("username", StringArgumentType.string()).executes(this::executeAddUsername)))
                .then(handler.literal("remove")
                        .requires(sender -> sender.hasPermission(Permissions.COMMAND_LITELOGIN_WHITELIST_REMOVE))
                        .then(handler.argument("username", StringArgumentType.string()).executes(this::executeRemoveUsername)))
                .then(handler.literal("specific")
                        .then(handler.literal("add")
                                .requires(sender -> sender.hasPermission(Permissions.COMMAND_LITELOGIN_WHITELIST_SPECIFIC_ADD))
                                .then(handler.argument("online", OnlineArgumentType.online()).executes(this::executeAdd)))
                        .then(handler.literal("remove")
                                .requires(sender -> sender.hasPermission(Permissions.COMMAND_LITELOGIN_WHITELIST_SPECIFIC_REMOVE))
                                .then(handler.argument("online", OnlineArgumentType.online()).executes(this::executeRemove))))
                .then(handler.literal("list")
                        .requires(sender -> sender.hasPermission(Permissions.COMMAND_LITELOGIN_WHITELIST_LIST))
                        .executes(this::executeList)
                        .then(handler.literal("verbose")
                                .requires(sender -> sender.hasPermission(Permissions.COMMAND_LITELOGIN_WHITELIST_LIST_VERBOSE))
                                .executes(this::executeListVerbose)));
    }

    @SneakyThrows
    private int executeRemove(CommandContext<ISender> context) {
        OnlineArgumentType.OnlineArgument online = OnlineArgumentType.getOnline(context, "online");
        if (!online.isWhitelist()) {
            context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§cThe player §8(§e{online_name}§8)[§e{online_uuid}§8]§c from service §e{service_name}§8(sid = §e{service_id}§8)§c is not whitelisted.",
                    new Pair<>("online_uuid", online.getOnlineUUID()),
                    new Pair<>("online_name", online.getOnlineName()),
                    new Pair<>("service_name", online.getBaseServiceConfig().getName()),
                    new Pair<>("service_id", online.getBaseServiceConfig().getId())
            ));
            return 0;
        }

        CommandHandler.getCore().getSqlManager().getUserDataTable().setWhitelist(online.getOnlineUUID(), online.getBaseServiceConfig().getId(), false);
        context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "§aRemoved whitelist access for player §8[§e{online_uuid}§8](§e{online_name}§8)§a from service §e{service_name}§8(sid = §e{service_id}§8)§a.",
                new Pair<>("online_uuid", online.getOnlineUUID()),
                new Pair<>("online_name", online.getOnlineName()),
                new Pair<>("service_name", online.getBaseServiceConfig().getName()),
                new Pair<>("service_id", online.getBaseServiceConfig().getId())
        ));

        UUID inGameUUID = CommandHandler.getCore().getSqlManager().getUserDataTable().getInGameUUID(online.getOnlineUUID(), online.getBaseServiceConfig().getId());
        if (inGameUUID != null) {
            CommandHandler.getCore().getPlugin().getRunServer().getPlayerManager().kickPlayerIfOnline(inGameUUID, "§cYour whitelist access has been removed.");
        }
        return 0;
    }

    @SneakyThrows
    private int executeAdd(CommandContext<ISender> context) {
        OnlineArgumentType.OnlineArgument online = OnlineArgumentType.getOnline(context, "online");
        if (online.isWhitelist()) {
            context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§cThe player §8(§e{online_name}§8)[§e{online_uuid}§8]§c from service §e{service_name}§8(sid = §e{service_id}§8)§c is already whitelisted.",
                    new Pair<>("online_uuid", online.getOnlineUUID()),
                    new Pair<>("online_name", online.getOnlineName()),
                    new Pair<>("service_name", online.getBaseServiceConfig().getName()),
                    new Pair<>("service_id", online.getBaseServiceConfig().getId())
            ));
            return 0;
        }

        if (!CommandHandler.getCore().getSqlManager().getUserDataTable().dataExists(online.getOnlineUUID(), online.getBaseServiceConfig().getId())) {
            CommandHandler.getCore().getSqlManager().getUserDataTable().insertNewData(online.getOnlineUUID(), online.getBaseServiceConfig().getId(), null, null);
        }
        CommandHandler.getCore().getSqlManager().getUserDataTable().setWhitelist(online.getOnlineUUID(), online.getBaseServiceConfig().getId(), true);
        context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "§aWhitelisted player §8(§e{online_name}§8)[§e{online_uuid}§8]§a from service §e{service_name}§8(sid = §e{service_id}§8)§a.",
                new Pair<>("online_uuid", online.getOnlineUUID()),
                new Pair<>("online_name", online.getOnlineName()),
                new Pair<>("service_name", online.getBaseServiceConfig().getName()),
                new Pair<>("service_id", online.getBaseServiceConfig().getId())
        ));
        return 0;
    }

    @SneakyThrows
    private int executeRemoveUsername(CommandContext<ISender> context) {
        String username = StringArgumentType.getString(context, "username");
        int count = 0;
        if (CommandHandler.getCore().getCacheWhitelistHandler().getCachedWhitelist().remove(username)) {
            count++;
        }
        UUID inGameUUID = CommandHandler.getCore().getSqlManager().getInGameProfileTable().getInGameUUIDIgnoreCase(username);
        if (inGameUUID != null && CommandHandler.getCore().getSqlManager().getUserDataTable().hasWhitelist(inGameUUID)) {
            count++;
            CommandHandler.getCore().getSqlManager().getUserDataTable().setWhitelist(inGameUUID, false);
        }

        if (count == 0) {
            context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§e{name}§c is not in the pending whitelist.",
                    new Pair<>("name", username)
            ));
            return 0;
        }

        context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "§aRemoved §e{name}§a from the whitelist cache. Affected rows: §e{count}§a.",
                new Pair<>("name", username),
                new Pair<>("count", count)
        ));

        if (inGameUUID != null) {
            IPlayer player = CommandHandler.getCore().getPlugin().getRunServer().getPlayerManager().getPlayer(inGameUUID);
            if (player != null) {
                player.kickPlayer("§cYour whitelist access has been removed.");
            }
        }
        return 0;
    }

    @SneakyThrows
    private int executeAddUsername(CommandContext<ISender> context) {
        String username = StringArgumentType.getString(context, "username").toLowerCase(Locale.ROOT);
        boolean alreadyWhitelisted = false;
        UUID inGameUUID = CommandHandler.getCore().getSqlManager().getInGameProfileTable().getInGameUUIDIgnoreCase(username);
        if (inGameUUID != null) {
            alreadyWhitelisted = CommandHandler.getCore().getSqlManager().getUserDataTable().hasWhitelist(inGameUUID);
        }

        if (alreadyWhitelisted || !CommandHandler.getCore().getCacheWhitelistHandler().getCachedWhitelist().add(username)) {
            context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§e{name}§c is already in the pending whitelist.",
                    new Pair<>("name", username)
            ));
            return 0;
        }

        context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "§aAdded §e{name}§a to the pending whitelist. Ask them to log in soon so the entry can be resolved.",
                new Pair<>("name", username)
        ));
        return 0;
    }

    @SneakyThrows
    private int executeList(CommandContext<ISender> context, boolean verbose) {
        List<String> list = CommandHandler.getCore().getSqlManager().getUserDataTable().listWhitelist(verbose);
        context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "Whitelist table entries: {count}\n{list}",
                new Pair<>("count", list.size()),
                new Pair<>("list", String.join(verbose ? ", \n" : ", ", list))
        ));

        var cache = CommandHandler.getCore().getCacheWhitelistHandler().getCachedWhitelist();
        context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "Pending whitelist entries: {count}\n{list}",
                new Pair<>("list", cache.stream().collect(Collectors.joining(", "))),
                new Pair<>("count", cache.size())
        ));
        return 0;
    }

    @SneakyThrows
    private int executeList(CommandContext<ISender> context) {
        return executeList(context, false);
    }

    @SneakyThrows
    private int executeListVerbose(CommandContext<ISender> context) {
        return executeList(context, true);
    }
}
