package org.eu.pnxlr.git.litelogin.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;
import org.eu.pnxlr.git.litelogin.api.internal.util.tuple.Pair;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.core.configuration.BaseServiceConfig;

import java.util.ArrayList;
import java.util.Collection;
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

    public LiteralArgumentBuilder<CommandSource> register(LiteralArgumentBuilder<CommandSource> literalArgumentBuilder) {
        return literalArgumentBuilder.then(LiteralArgumentBuilder.<CommandSource>literal("help")
                        .requires(source -> source.hasPermission(LiteLoginConstants.COMMAND_LITELOGIN_HELP_PERMISSION))
                        .executes(this::executeHelp))
                .then(LiteralArgumentBuilder.<CommandSource>literal("list")
                        .requires(source -> source.hasPermission(LiteLoginConstants.COMMAND_LITELOGIN_LIST_PERMISSION))
                        .executes(this::executeList))
                .then(LiteralArgumentBuilder.<CommandSource>literal("add")
                        .requires(source -> source.hasPermission(LiteLoginConstants.COMMAND_LITELOGIN_ADD_PERMISSION))
                        .then(usernameArgument().executes(this::executeAddUsername)))
                .then(LiteralArgumentBuilder.<CommandSource>literal("remove")
                        .requires(source -> source.hasPermission(LiteLoginConstants.COMMAND_LITELOGIN_REMOVE_PERMISSION))
                        .then(usernameArgument().executes(this::executeRemoveUsername)));
    }

    private RequiredArgumentBuilder<CommandSource, String> usernameArgument() {
        return RequiredArgumentBuilder.<CommandSource, String>argument("username", StringArgumentType.word());
    }

    private int executeHelp(CommandContext<CommandSource> context) {
        CommandHandler.sendMessage(context.getSource(), String.join("\n",
                "LiteLogin commands:",
                "/litelogin help - Show this help message.",
                "/litelogin list - List online players and whitelist entries.",
                "/litelogin add <username> - Add a pending whitelist entry.",
                "/litelogin remove <username> - Remove a whitelist entry."
        ));
        return 0;
    }

    @SneakyThrows
    private int executeList(CommandContext<CommandSource> context) {
        Collection<Player> onlinePlayers = CommandHandler.getCore().getProxyServer().getAllPlayers();
        Map<Integer, List<Player>> identifiedPlayerMap = new HashMap<>();
        for (Player player : onlinePlayers) {
            Pair<GameProfile, Integer> profile = CommandHandler.getCore().getPlayerHandler().getPlayerOnlineProfile(player.getUniqueId());
            int sid = profile == null ? -1 : profile.getValue2();
            identifiedPlayerMap.computeIfAbsent(sid, ignored -> new ArrayList<>()).add(player);
        }

        CommandHandler.getCore().getPluginConfig().getServiceIdMap().forEach((key, value) ->
                identifiedPlayerMap.computeIfAbsent(key, ignored -> new ArrayList<>()));

        String list = identifiedPlayerMap.entrySet().stream().map(entry -> {
            String serviceName;
            if (entry.getKey() == -1) {
                serviceName = "Unidentified";
            } else {
                BaseServiceConfig service = CommandHandler.getCore().getPluginConfig().getServiceIdMap().get(entry.getKey());
                serviceName = service == null ? "Unknown" : service.getName();
            }
            String playerList = entry.getValue().stream().map(Player::getUsername).collect(Collectors.joining(", "));
            return org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "[{service_name} (sid = {service_id})] ({count}): {list}",
                    new Pair<>("service_name", serviceName),
                    new Pair<>("service_id", entry.getKey()),
                    new Pair<>("count", entry.getValue().size()),
                    new Pair<>("list", playerList)
            );
        }).collect(Collectors.joining("\n"));

        CommandHandler.sendMessage(context.getSource(),
                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(String.join("\n",
                                "{list}",
                                "Total online players: {count}",
                                "",
                                "Whitelist table entries: {whitelist_count}",
                                "{whitelist_list}",
                                "",
                                "Pending whitelist entries: {pending_count}",
                                "{pending_list}"),
                        new Pair<>("list", list),
                        new Pair<>("count", onlinePlayers.size()),
                        new Pair<>("whitelist_count", CommandHandler.getCore().getSqlManager().getUserDataTable().listWhitelist(false).size()),
                        new Pair<>("whitelist_list", String.join(", ", CommandHandler.getCore().getSqlManager().getUserDataTable().listWhitelist(true))),
                        new Pair<>("pending_count", CommandHandler.getCore().getCachedWhitelist().size()),
                        new Pair<>("pending_list", CommandHandler.getCore().getCachedWhitelist().stream().collect(Collectors.joining(", ")))
                )
        );
        return 0;
    }

    @SneakyThrows
    private int executeAddUsername(CommandContext<CommandSource> context) {
        String username = StringArgumentType.getString(context, "username").toLowerCase(Locale.ROOT);
        boolean alreadyWhitelisted = false;
        UUID inGameUUID = CommandHandler.getCore().getSqlManager().getInGameProfileTable().getInGameUUIDIgnoreCase(username);
        if (inGameUUID != null) {
            alreadyWhitelisted = CommandHandler.getCore().getSqlManager().getUserDataTable().hasWhitelist(inGameUUID);
        }

        if (alreadyWhitelisted || !CommandHandler.getCore().getCachedWhitelist().add(username)) {
            CommandHandler.sendMessage(context.getSource(), org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "{name} is already in the pending whitelist.",
                    new Pair<>("name", username)
            ));
            return 0;
        }

        CommandHandler.sendMessage(context.getSource(), org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "Added {name} to the pending whitelist. Ask them to log in soon so the entry can be resolved.",
                new Pair<>("name", username)
        ));
        return 0;
    }

    @SneakyThrows
    private int executeRemoveUsername(CommandContext<CommandSource> context) {
        String username = StringArgumentType.getString(context, "username");
        int count = 0;
        if (CommandHandler.getCore().getCachedWhitelist().remove(username)) {
            count++;
        }
        UUID inGameUUID = CommandHandler.getCore().getSqlManager().getInGameProfileTable().getInGameUUIDIgnoreCase(username);
        if (inGameUUID != null && CommandHandler.getCore().getSqlManager().getUserDataTable().hasWhitelist(inGameUUID)) {
            count++;
            CommandHandler.getCore().getSqlManager().getUserDataTable().setWhitelist(inGameUUID, false);
        }

        if (count == 0) {
            CommandHandler.sendMessage(context.getSource(), org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "{name} is not in the pending whitelist.",
                    new Pair<>("name", username)
            ));
            return 0;
        }

        CommandHandler.sendMessage(context.getSource(), org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "Removed {name} from the whitelist cache. Affected rows: {count}.",
                new Pair<>("name", username),
                new Pair<>("count", count)
        ));

        if (inGameUUID != null) {
            CommandHandler.getCore().getProxyServer().getPlayer(inGameUUID)
                    .ifPresent(player -> player.disconnect(net.kyori.adventure.text.Component.text("Your whitelist access has been removed.")));
        }
        return 0;
    }
}
