package org.eu.pnxlr.git.litelogin.velocity.main;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;

/**
 * Velocity command handler.
 */
public class CommandHandler {
    private static final String USERNAME_ARGUMENT_NAME = "username";

    private final LiteLoginVelocity plugin;

    public CommandHandler(LiteLoginVelocity plugin) {
        this.plugin = plugin;
    }

    public void register() {
        BrigadierCommand command = new BrigadierCommand(
                BrigadierCommand.literalArgumentBuilder(LiteLoginConstants.ROOT_COMMAND_LITERAL)
                        .then(BrigadierCommand.literalArgumentBuilder("help")
                                .requires(source -> source.hasPermission(LiteLoginConstants.COMMAND_LITELOGIN_HELP_PERMISSION))
                                .executes(context -> execute(context.getSource(), LiteLoginConstants.ROOT_COMMAND_LITERAL + " help")))
                        .then(BrigadierCommand.literalArgumentBuilder("list")
                                .requires(source -> source.hasPermission(LiteLoginConstants.COMMAND_LITELOGIN_LIST_PERMISSION))
                                .executes(context -> execute(context.getSource(), LiteLoginConstants.ROOT_COMMAND_LITERAL + " list")))
                        .then(BrigadierCommand.literalArgumentBuilder("add")
                                .requires(source -> source.hasPermission(LiteLoginConstants.COMMAND_LITELOGIN_ADD_PERMISSION))
                                .then(BrigadierCommand.requiredArgumentBuilder(USERNAME_ARGUMENT_NAME, StringArgumentType.word())
                                        .executes(context -> execute(
                                                context.getSource(),
                                                LiteLoginConstants.ROOT_COMMAND_LITERAL + " add "
                                                        + StringArgumentType.getString(context, USERNAME_ARGUMENT_NAME)))))
                        .then(BrigadierCommand.literalArgumentBuilder("remove")
                                .requires(source -> source.hasPermission(LiteLoginConstants.COMMAND_LITELOGIN_REMOVE_PERMISSION))
                                .then(BrigadierCommand.requiredArgumentBuilder(USERNAME_ARGUMENT_NAME, StringArgumentType.word())
                                        .executes(context -> execute(
                                                context.getSource(),
                                                LiteLoginConstants.ROOT_COMMAND_LITERAL + " remove "
                                                        + StringArgumentType.getString(context, USERNAME_ARGUMENT_NAME)))))
        );

        CommandManager commandManager = plugin.getServer().getCommandManager();
        commandManager.register(commandManager.metaBuilder(command).plugin(plugin).build(), command);
    }

    private int execute(CommandSource source, String commandLine) {
        plugin.getCore().getCommandHandler().execute(source, commandLine);
        return 1;
    }
}
