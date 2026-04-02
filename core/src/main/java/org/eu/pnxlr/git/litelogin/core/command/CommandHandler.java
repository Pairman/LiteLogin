package org.eu.pnxlr.git.litelogin.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.command.CommandAPI;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.ISender;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Central command handler.
 */
public class CommandHandler implements CommandAPI {
    @Getter
    private static Core core;
    private final CommandDispatcher<ISender> dispatcher;

    public CommandHandler(Core core) {
        CommandHandler.core = core;
        this.dispatcher = new CommandDispatcher<>();
    }

    public void init() {
        dispatcher.register(new RootCommand(this).register(literal(LiteLoginConstants.ROOT_COMMAND_LITERAL)));
        CommandSyntaxException.BUILT_IN_EXCEPTIONS = new BuiltInExceptions();
    }

    @Override
    public void execute(ISender sender, String[] args) {
        execute(sender, String.join(" ", args));
    }

    @Override
    public void execute(ISender sender, String args) {
        core.getPlugin().getRunServer().getScheduler().runTaskAsync(() -> {
            try {
                dispatcher.execute(args, sender);
            } catch (CommandSyntaxException e) {
                sender.sendMessagePL(e.getRawMessage().getString());
                LoggerProvider.getLogger().debug(String.format("An expected exception occurs when the %s command is ", String.join(" ", args)), e);
            } catch (Exception e) {
                sender.sendMessagePL("An error occurred while processing the command. Please contact the server administrator.");
                LoggerProvider.getLogger().error(String.format("An exception occurs when the %s command is ", String.join(" ", args)), e);
            }
        });
    }

    @Override
    public List<String> tabComplete(ISender sender, String[] args) {
        if (args.length == 1) {
            return tabComplete(sender, args[0] + " ");
        }
        return tabComplete(sender, String.join(" ", args));
    }

    @Override
    public List<String> tabComplete(ISender sender, String args) {
        if (!sender.hasPermission(Permissions.COMMAND_TAB_COMPLETE)) {
            return Collections.emptyList();
        }
        CompletableFuture<Suggestions> suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(args, sender));
        List<String> ret = new ArrayList<>();
        try {
            Suggestions suggestions1 = suggestions.get();
            for (Suggestion suggestion : suggestions1.getList()) {
                ret.add(suggestion.getText());
            }
        } catch (Exception e) {
            LoggerProvider.getLogger().error(String.format("An exception occurred while executing the %s command to ", String.join(" ", args)), e);
        }
        return ret;
    }

    /**
     * Subcommand name
     */
    public final LiteralArgumentBuilder<ISender> literal(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }

    /**
     * Builds command arguments
     */
    public final <T> RequiredArgumentBuilder<ISender, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
