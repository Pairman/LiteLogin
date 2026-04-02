package org.eu.pnxlr.git.litelogin.velocity.main;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import org.eu.pnxlr.git.litelogin.velocity.impl.VelocitySender;

import java.util.List;

/**
 * Velocity command handler.
 */
public class CommandHandler {
    private final LiteLoginVelocity plugin;

    private final SimpleCommand simpleCommand = new SimpleCommand() {
        @Override
        public void execute(Invocation invocation) {
            String[] arguments = invocation.arguments();
            String[] ns = new String[arguments.length + 1];
            System.arraycopy(arguments, 0, ns, 1, arguments.length);
            ns[0] = invocation.alias();
            plugin.getCoreApi().getCommandHandler().execute(new VelocitySender(invocation.source()), ns);
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            String[] arguments = invocation.arguments();
            String[] ns = new String[arguments.length + 1];
            System.arraycopy(arguments, 0, ns, 1, arguments.length);
            ns[0] = invocation.alias();
            return plugin.getCoreApi().getCommandHandler().tabComplete(new VelocitySender(invocation.source()), ns);
        }
    };

    public CommandHandler(LiteLoginVelocity plugin) {
        this.plugin = plugin;
    }

    public void register(String cmdName) {
        CommandManager commandManager = plugin.getServer().getCommandManager();
        commandManager.register(commandManager.metaBuilder(cmdName).build(), simpleCommand);
    }
}
