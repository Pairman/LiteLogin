package org.eu.pnxlr.git.litelogin.core.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlayer;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.ISender;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.command.Permissions;
import org.eu.pnxlr.git.litelogin.core.command.argument.OnlineArgumentType;
import org.eu.pnxlr.git.litelogin.core.command.argument.ProfileArgumentType;
import org.eu.pnxlr.git.litelogin.core.command.argument.StringArgumentType;
import org.eu.pnxlr.git.litelogin.core.command.argument.UUIDArgumentType;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class MProfileCommand {
    private final CommandHandler handler;

    public MProfileCommand(CommandHandler handler) {
        this.handler = handler;
    }

    public LiteralArgumentBuilder<ISender> register(LiteralArgumentBuilder<ISender> literalArgumentBuilder) {
        return literalArgumentBuilder
                .then(handler.literal("create")
                        .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_PROFILE_CREATE))
                        .then(handler.argument("username", StringArgumentType.string())
                                .then(handler.argument("ingameuuid", UUIDArgumentType.uuid()).executes(this::executeCreate))
                                .executes(this::executeCreateRandomUUID)))
                .then(handler.literal("set")
                        .then(handler.argument("profile", ProfileArgumentType.profile())
                                .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_PROFILE_SET_ONESELF))
                                .executes(this::executeSetOneself))
                        .then(handler.argument("profile", ProfileArgumentType.profile())
                                .then(handler.argument("online", OnlineArgumentType.online())
                                        .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_PROFILE_SET_OTHER))
                                        .executes(this::executeSetOther))))
                .then(handler.literal("remove")
                        .then(handler.argument("profile", ProfileArgumentType.profile())
                                .requires(iSender -> iSender.hasPermission(Permissions.COMMAND_LITELOGIN_PROFILE_REMOVE))
                                .executes(this::executeRemove)));
    }

    @SneakyThrows
    private int executeRemove(CommandContext<ISender> context) {
        ProfileArgumentType.ProfileArgument profile = ProfileArgumentType.getProfile(context, "profile");
        String name = Optional.ofNullable(profile.getProfileName()).orElse("Unnamed");

        handler.getSecondaryConfirmationHandler().submit(context.getSource(), () -> {
                    CommandHandler.getCore().getSqlManager().getInGameProfileTable().remove(profile.getProfileUUID());
                    context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                            "§aDeleted in-game profile §8[§e{uuid}§8](§e{name}§8)§a.",
                            new Pair<>("name", name),
                            new Pair<>("uuid", profile.getProfileUUID())
                    ));
                    IPlayer player = CommandHandler.getCore().getPlugin().getRunServer().getPlayerManager().getPlayer(profile.getProfileUUID());
                    if (player != null) {
                        player.kickPlayer("§cYour profile data was forcibly released by an administrator.");
                    }
                },
                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "§cDelete in-game profile §8[§e{uuid}§8](§e{name}§8)§c.",
                        new Pair<>("name", name),
                        new Pair<>("uuid", profile.getProfileUUID())
                ),
                "§cThe profile UUID and name will be released, and the deleted data may later be assigned to another player."
        );
        return 0;
    }

    @SneakyThrows
    private int executeSetOther(CommandContext<ISender> context) {
        ProfileArgumentType.ProfileArgument profile = ProfileArgumentType.getProfile(context, "profile");
        OnlineArgumentType.OnlineArgument online = OnlineArgumentType.getOnline(context, "online");
        processSet(context, online.getOnlineUUID(), online.getOnlineName(), online.getBaseServiceConfig().getId(), profile);
        return 0;
    }

    @SneakyThrows
    private int executeSetOneself(CommandContext<ISender> context) {
        ProfileArgumentType.ProfileArgument profile = ProfileArgumentType.getProfile(context, "profile");
        Pair<GameProfile, Integer> pair = handler.requireDataCacheArgumentSelf(context);
        processSet(context, pair.getValue1().getId(), pair.getValue1().getName(), pair.getValue2(), profile);
        return 0;
    }

    private void processSet(CommandContext<ISender> context, UUID from, String fromName, int serviceId, ProfileArgumentType.ProfileArgument to) {
        handler.getSecondaryConfirmationHandler().submit(context.getSource(), () -> {
                    CommandHandler.getCore().getSqlManager().getUserDataTable().setInGameUUID(from, serviceId, to.getProfileUUID());
                    context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                            "§aUpdated the profile assignment for player §8(§e{online_name}§8)[§e{online_uuid}§8]§a. On their next login they will join as §8(§e{redirect_name}§8)[§e{redirect_uuid}§8]§a.",
                            new Pair<>("redirect_name", to.getProfileName()),
                            new Pair<>("redirect_uuid", to.getProfileUUID()),
                            new Pair<>("online_uuid", from),
                            new Pair<>("online_name", fromName)
                    ));
                    UUID inGameUUID = CommandHandler.getCore().getPlayerHandler().getInGameUUID(from, serviceId);
                    if (inGameUUID != null) {
                        CommandHandler.getCore().getPlugin().getRunServer().getPlayerManager().kickPlayerIfOnline(
                                inGameUUID,
                                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                                        "§aYour login profile was updated. On your next login you will join as §8(§e{redirect_name}§8)[§e{redirect_uuid}§8]§a.",
                                        new Pair<>("redirect_name", to.getProfileName()),
                                        new Pair<>("redirect_uuid", to.getProfileUUID())
                                )
                        );
                    }
                },
                org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "§cChange the next-login profile for player §8(§e{online_name}§8)[§e{online_uuid}§8]§c to §8(§e{redirect_name}§8)[§e{redirect_uuid}§8]§c.",
                        new Pair<>("redirect_name", to.getProfileName()),
                        new Pair<>("redirect_uuid", to.getProfileUUID()),
                        new Pair<>("online_uuid", from),
                        new Pair<>("online_name", fromName)
                ),
                "§cProfile permission differences may prevent the player from switching back easily."
        );
    }

    private void processCreate(CommandContext<ISender> context, String name, UUID uuid) throws SQLException {
        Core core = CommandHandler.getCore();
        String nameAllowedRegular = core.getPluginConfig().getNameAllowedRegular();
        if (!ValueUtil.isEmpty(nameAllowedRegular) && !Pattern.matches(nameAllowedRegular, name)) {
            context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§cThe name §e{name}§c does not match the required pattern §e{regular}§c.",
                    new Pair<>("name", name),
                    new Pair<>("regular", nameAllowedRegular)
            ));
            return;
        }
        if (uuid.version() < 2) {
            context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§cThe UUID §e{uuid}§c is not acceptable for a profile.",
                    new Pair<>("uuid", uuid)
            ));
            return;
        }
        Pair<UUID, String> pair = core.getSqlManager().getInGameProfileTable().get(uuid);
        if (pair != null) {
            context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§cA profile with UUID §8[§e{uuid}§8](§e{name}§8)§c already exists.",
                    new Pair<>("uuid", uuid),
                    new Pair<>("name", pair.getValue2())
            ));
            return;
        }
        UUID uuidIgnoreCase = core.getSqlManager().getInGameProfileTable().getInGameUUIDIgnoreCase(name);
        if (uuidIgnoreCase != null) {
            context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                    "§cA profile with name §8[§e{uuid}§8](§e{name}§8)§c already exists.",
                    new Pair<>("name", name),
                    new Pair<>("uuid", uuidIgnoreCase)
            ));
            return;
        }
        core.getSqlManager().getInGameProfileTable().insertNewData(uuid, name);
        context.getSource().sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                "§aCreated in-game profile §8(§e{name}§8[§e{uuid}§8])§a successfully.",
                new Pair<>("uuid", uuid),
                new Pair<>("name", name)
        ));
    }

    @SneakyThrows
    private int executeCreate(CommandContext<ISender> context) {
        processCreate(context, StringArgumentType.getString(context, "username"), UUIDArgumentType.getUuid(context, "ingameuuid"));
        return 0;
    }

    @SneakyThrows
    private int executeCreateRandomUUID(CommandContext<ISender> context) {
        processCreate(context, StringArgumentType.getString(context, "username"), UUID.randomUUID());
        return 0;
    }
}
