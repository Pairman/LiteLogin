package org.eu.pnxlr.git.litelogin.core.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlayer;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.ISender;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.command.Permissions;
import org.eu.pnxlr.git.litelogin.core.command.argument.OnlinePlayerArgumentType;
import org.eu.pnxlr.git.litelogin.core.configuration.service.BaseServiceConfig;

import java.util.HashSet;
import java.util.Set;

public class MInfoCommand {
    private final CommandHandler handler;

    public MInfoCommand(CommandHandler handler) {
        this.handler = handler;
    }

    public LiteralArgumentBuilder<ISender> register(LiteralArgumentBuilder<ISender> literalArgumentBuilder) {
        return literalArgumentBuilder.then(handler.argument("player", OnlinePlayerArgumentType.players())
                        .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_CURRENT_OTHER))
                        .executes(this::executeInfo))
                .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_CURRENT_ONESELF))
                .executes(this::executeInfoOneself);
    }

    private int executeInfo(CommandContext<ISender> context) {
        processInfoCommand(context, OnlinePlayerArgumentType.getPlayers(context, "player"));
        return 0;
    }

    private int executeInfoOneself(CommandContext<ISender> context) throws CommandSyntaxException {
        handler.requirePlayer(context);
        IPlayer player = context.getSource().getAsPlayer();
        HashSet<IPlayer> players = new HashSet<>();
        players.add(player);
        processInfoCommand(context, players);
        return 0;
    }

    private void processInfoCommand(CommandContext<ISender> context, Set<IPlayer> players) {
        if (players.size() > 1) {
            context.getSource().sendMessagePL(
                    org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                            "§cFound §e{0}§c online players with similar names. Review the list below and prefer UUID-based commands.",
                            new Pair<>("size", players.size())
                    )
            );
        }

        for (IPlayer player : players) {
            Pair<GameProfile, Integer> profile = CommandHandler.getCore().getPlayerHandler().getPlayerOnlineProfile(player.getUniqueId());
            if (profile == null) {
                context.getSource().sendMessagePL(
                        org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                                "§aPlayer §8[§e{uuid}§8](§e{name}§8)§a bypassed LiteLogin, so no login data is available.",
                                new Pair<>("name", player.getName()),
                                new Pair<>("uuid", player.getUniqueId())
                        )
                );
                continue;
            }

            BaseServiceConfig serviceConfig = CommandHandler.getCore().getPluginConfig().getServiceIdMap().get(profile.getValue2());
            String serviceName = serviceConfig == null ? "§8Unidentified" : serviceConfig.getName();
            context.getSource().sendMessagePL(
                    org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                            "§aPlayer §8(§e{name}§8)[§e{uuid}§8]§a logged in through §e{service_name}§8(sid = §e{service_id}§8)§a with original identity §8(§e{online_name}§8)[§e{online_uuid}§8]§a.",
                            new Pair<>("name", player.getName()),
                            new Pair<>("uuid", player.getUniqueId()),
                            new Pair<>("service_name", serviceName),
                            new Pair<>("service_id", profile.getValue2()),
                            new Pair<>("online_name", profile.getValue1().getName()),
                            new Pair<>("online_uuid", profile.getValue1().getId())
                    )
            );
        }
    }
}
