package org.eu.pnxlr.git.litelogin.core.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.ISender;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.internal.util.There;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.command.Permissions;
import org.eu.pnxlr.git.litelogin.core.command.argument.OnlineArgumentType;
import org.eu.pnxlr.git.litelogin.core.command.argument.ProfileArgumentType;
import org.eu.pnxlr.git.litelogin.core.configuration.service.BaseServiceConfig;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MFindCommand {
    private final CommandHandler handler;

    public MFindCommand(CommandHandler handler) {
        this.handler = handler;
    }

    public LiteralArgumentBuilder<ISender> register(LiteralArgumentBuilder<ISender> literalArgumentBuilder) {
        return literalArgumentBuilder.then(
                        handler.literal("profile")
                                .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_FIND_PROFILE))
                                .then(handler.argument("profile", ProfileArgumentType.profile())
                                        .executes(this::executeProfile)))
                .then(handler.literal("online")
                        .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_FIND_ONLINE))
                        .then(handler.argument("online", OnlineArgumentType.online())
                                .executes(this::executeOnline)));
    }

    @SneakyThrows
    private int executeOnline(CommandContext<ISender> context) {
        OnlineArgumentType.OnlineArgument online = OnlineArgumentType.getOnline(context, "online");
        String whitelist = online.isWhitelist() ? "enabled" : "disabled";
        UUID profileUUID = online.getProfileUUID();
        String profileInfo;
        if (profileUUID == null) {
            profileInfo = "No profile";
        } else {
            String profileName = CommandHandler.getCore().getSqlManager().getInGameProfileTable().getUsername(profileUUID);
            if (profileName == null) {
                profileName = "Unnamed";
            }
            profileInfo = org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§8(§e{profile_name}§8)[§e{profile_uuid}§8]",
                    new Pair<>("profile_uuid", profileUUID),
                    new Pair<>("profile_name", profileName)
            );
        }

        context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "§aFound online identity §8(§e{online_name}§8)[§e{online_uuid}§8]§a from service §e{service_name}§8(sid = §e{service_id}§8)§a, whitelist: §e{whitelist}§a, in-game profile: §e{profile}",
                new Pair<>("service_name", online.getBaseServiceConfig().getName()),
                new Pair<>("service_id", online.getBaseServiceConfig().getId()),
                new Pair<>("online_uuid", online.getOnlineUUID()),
                new Pair<>("online_name", online.getOnlineName()),
                new Pair<>("whitelist", whitelist),
                new Pair<>("profile", profileInfo)
        ));
        return 0;
    }

    @SneakyThrows
    private int executeProfile(CommandContext<ISender> context) {
        ProfileArgumentType.ProfileArgument profile = ProfileArgumentType.getProfile(context, "profile");
        UUID profileUUID = profile.getProfileUUID();
        Set<There<UUID, String, Integer>> onlineProfiles = CommandHandler.getCore().getSqlManager().getUserDataTable().getOnlineProfiles(profileUUID);
        String profileName = CommandHandler.getCore().getSqlManager().getInGameProfileTable().getUsername(profileUUID);
        if (profileName == null) {
            profileName = "§8Unnamed profile";
        }

        String list = onlineProfiles.stream().map(p -> {
            BaseServiceConfig serviceConfig = CommandHandler.getCore().getPluginConfig().getServiceIdMap().get(p.getValue3());
            String serviceName = serviceConfig == null ? "§8Service missing" : serviceConfig.getName();
            return org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§aPlayer §8(§e{online_name}§8)[§e{online_uuid}§8]§a through service §e{service_name}§8(sid = §e{service_id}§8)§a",
                    new Pair<>("service_name", serviceName),
                    new Pair<>("service_id", p.getValue3()),
                    new Pair<>("online_uuid", p.getValue1()),
                    new Pair<>("online_name", Optional.ofNullable(p.getValue2()).orElse("§8Name not recorded"))
            );
        }).collect(Collectors.joining(", §r"));

        context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "§aFound profile §8(§e{profile_name}§8[§e{profile_uuid}§8])§a. It currently has §e{count}§a login identities:\n {list}",
                new Pair<>("profile_uuid", profileUUID),
                new Pair<>("profile_name", profileName),
                new Pair<>("count", onlineProfiles.size()),
                new Pair<>("list", list)
        ));
        return 0;
    }
}
