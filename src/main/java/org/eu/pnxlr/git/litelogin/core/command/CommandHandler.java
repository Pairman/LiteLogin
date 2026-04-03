package org.eu.pnxlr.git.litelogin.core.command;

import com.velocitypowered.api.command.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;
import org.eu.pnxlr.git.litelogin.core.main.Core;

/**
 * Central command handler.
 */
public class CommandHandler {
    @Getter
    private static Core core;
    private final CommandDispatcher<CommandSource> dispatcher;

    public CommandHandler(Core core) {
        CommandHandler.core = core;
        this.dispatcher = new CommandDispatcher<>();
    }

    public void init() {
        dispatcher.register(new RootCommand(this).register(
                LiteralArgumentBuilder.<CommandSource>literal(LiteLoginConstants.ROOT_COMMAND_LITERAL)
        ));
        CommandSyntaxException.BUILT_IN_EXCEPTIONS = new BuiltInExceptions();
    }

    public void execute(CommandSource sender, String args) {
        core.getScheduler().runTaskAsync(() -> {
            try {
                dispatcher.execute(args, sender);
            } catch (CommandSyntaxException e) {
                sendMessage(sender, e.getRawMessage().getString());
                LoggerProvider.getLogger().debug("An expected exception occurred while processing command: " + args, e);
            } catch (Exception e) {
                sendMessage(sender, "An error occurred while processing the command. Please contact the server administrator.");
                LoggerProvider.getLogger().error("An exception occurred while processing command: " + args, e);
            }
        });
    }

    public static void sendMessage(CommandSource sender, String message) {
        for (String line : message.split("\\r?\\n")) {
            sender.sendMessage(Component.text(line));
        }
    }

}
