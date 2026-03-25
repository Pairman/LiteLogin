package org.eu.pnxlr.git.litelogin.core.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlayer;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.ISender;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.command.Permissions;
import org.eu.pnxlr.git.litelogin.core.command.argument.OnlinePlayerArgumentType;
import org.eu.pnxlr.git.litelogin.core.command.argument.StringArgumentType;
import org.eu.pnxlr.git.litelogin.core.configuration.service.BaseServiceConfig;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MLinkCommand {
    private final CommandHandler handler;
    private final Map<GameProfile, Entry> gameProfileEntryMap = new ConcurrentHashMap<>();

    public MLinkCommand(CommandHandler handler) {
        this.handler = handler;
    }

    public LiteralArgumentBuilder<ISender> register(LiteralArgumentBuilder<ISender> literal) {
        return literal
                .then(handler.literal("to")
                        .requires(sender -> sender.hasPermission(Permissions.COMMAND_LITELOGIN_LINK_TO))
                        .then(handler.argument("player", OnlinePlayerArgumentType.players()).executes(this::executeLinkTo)))
                .then(handler.literal("accept")
                        .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_LINK_ACCEPT))
                        .then(handler.argument("name", StringArgumentType.string()).executes(this::executeLinkAccept)))
                .then(handler.literal("code")
                        .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_LINK_CODE))
                        .then(handler.argument("player", OnlinePlayerArgumentType.players())
                                .then(handler.argument("code", StringArgumentType.string()).executes(this::executeLinkCode))));
    }

    @SneakyThrows
    private int executeLinkCode(CommandContext<ISender> context) {
        GameProfile self = handler.requireDataCacheArgumentSelf(context).getValue1();
        IPlayer target = OnlinePlayerArgumentType.getPlayer(context, "player");
        String code = StringArgumentType.getString(context, "code");

        gameProfileEntryMap.values().removeIf(e -> e.timeMills < System.currentTimeMillis() - 30000);
        Entry entry = gameProfileEntryMap.get(self);
        if (entry == null || !entry.receiverUserInGameUUID.equals(target.getUniqueId()) || entry.code == null) {
            context.getSource().sendMessagePL("§cThe migration request is missing, expired, or already in progress.");
            return 0;
        }
        if (!entry.code.equals(code)) {
            context.getSource().sendMessagePL("§cInvalid migration code.");
            return 0;
        }

        gameProfileEntryMap.remove(self);
        CommandHandler.getCore().getSqlManager().getUserDataTable().setInGameUUID(
                entry.requesterOnlineProfile.getValue1().getId(),
                entry.requesterOnlineProfile.getValue2(),
                entry.receiverUserInGameUUID
        );

        String done = org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "§aMigration completed. On your next login you will join as §8(§e{redirect_name}§8)[§e{redirect_uuid}§8]§a.",
                new Pair<>("redirect_name", target.getName()),
                new Pair<>("redirect_uuid", target.getUniqueId())
        );
        context.getSource().sendMessagePL(done);
        context.getSource().getAsPlayer().kickPlayer(done);
        return 0;
    }

    private int executeLinkAccept(CommandContext<ISender> context) throws CommandSyntaxException {
        handler.requireDataCacheArgumentSelf(context);
        String string = StringArgumentType.getString(context, "name");
        gameProfileEntryMap.values().removeIf(e -> e.timeMills < System.currentTimeMillis() - CommandHandler.getCore().getPluginConfig().getLinkAcceptValidTimeMills());
        Optional<Map.Entry<GameProfile, Entry>> entry = gameProfileEntryMap.entrySet().stream()
                .filter(e -> e.getKey().getName().equalsIgnoreCase(string))
                .filter(e -> e.getValue().receiverUserInGameUUID.equals(context.getSource().getAsPlayer().getUniqueId()))
                .filter(e -> e.getValue().code == null)
                .findFirst();

        if (entry.isEmpty()) {
            context.getSource().sendMessagePL("§cThe migration request is missing, expired, or already in progress.");
            return 0;
        }

        Map.Entry<GameProfile, Entry> profileEntry = entry.get();
        BaseServiceConfig serviceConfig = CommandHandler.getCore().getPluginConfig().getServiceIdMap().get(profileEntry.getValue().requesterOnlineProfile.getValue2());
        String targetServiceName = serviceConfig == null ? "§8Unidentified" : serviceConfig.getName();

        handler.getSecondaryConfirmationHandler().submit(context.getSource(), () -> {
                    profileEntry.getValue().code = ValueUtil.generateLinkCode();
                    context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                            "§6Your migration code is §e{code}§6. Now log into the target account and run §7/litelogin link code {profile_name} {code}§6 to finish the migration.",
                            new Pair<>("code", profileEntry.getValue().code),
                            new Pair<>("profile_name", context.getSource().getAsPlayer().getName())
                    ));
                },
                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "§cApprove the migration request so the player §8(§e{target_online_name}§8)[§e{target_online_uuid}§8]§c from service §e{target_service_name}§8(sid = §e{target_service_id}§8)§c may log into your current profile §8(§e{profile_name}§8)[§e{profile_uuid}§8]§c.",
                        new Pair<>("target_service_name", targetServiceName),
                        new Pair<>("target_service_id", profileEntry.getValue().requesterOnlineProfile.getValue2()),
                        new Pair<>("target_online_name", profileEntry.getKey().getName()),
                        new Pair<>("target_online_uuid", profileEntry.getKey().getId()),
                        new Pair<>("profile_name", context.getSource().getAsPlayer().getName()),
                        new Pair<>("profile_uuid", context.getSource().getAsPlayer().getUniqueId())
                ),
                "§cThis may cause irreversible data loss unless both accounts belong to you."
        );
        return 0;
    }

    private int executeLinkTo(CommandContext<ISender> context) throws CommandSyntaxException {
        Pair<GameProfile, Integer> self = handler.requireDataCacheArgumentSelf(context);
        IPlayer target = OnlinePlayerArgumentType.getPlayer(context, "player");
        handler.requirePlayerAndNoSelf(context, target);
        handler.requireDataCacheArgumentOther(target);

        handler.getSecondaryConfirmationHandler().submit(context.getSource(), () -> {
                    gameProfileEntryMap.put(self.getValue1(), new Entry(self, target.getUniqueId()));
                    context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                            "§6Migration request recorded. Log into the target account and run §7/litelogin link accept {self_online_name}§6 to receive the migration code. This request expires in §730§6 seconds.",
                            new Pair<>("self_online_name", self.getValue1().getName())
                    ));
                },
                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "§cMigrate your current login identity so it joins as §8(§e{redirect_name}§8)[§e{redirect_uuid}§8]§c.",
                        new Pair<>("redirect_name", target.getName()),
                        new Pair<>("redirect_uuid", target.getUniqueId())
                ),
                "§cAll data on your current profile will be lost and the action cannot be rolled back."
        );
        return 0;
    }

    public static class Entry {
        private final long timeMills = System.currentTimeMillis();
        private final Pair<GameProfile, Integer> requesterOnlineProfile;
        private final UUID receiverUserInGameUUID;
        private String code;

        public Entry(Pair<GameProfile, Integer> requesterOnlineProfile, UUID receiverUserInGameUUID) {
            this.requesterOnlineProfile = requesterOnlineProfile;
            this.receiverUserInGameUUID = receiverUserInGameUUID;
        }
    }
}
