package org.eu.pnxlr.git.litelogin.core.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlayer;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.command.UniversalCommandExceptionType;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OnlinePlayerArgumentType implements ArgumentType<Set<IPlayer>> {

    public static OnlinePlayerArgumentType players() {
        return new OnlinePlayerArgumentType();
    }

    @SuppressWarnings("unchecked")
    public static Set<IPlayer> getPlayers(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Set.class);
    }

    public static IPlayer getPlayer(final CommandContext<?> context, final String name) throws CommandSyntaxException {
        Set<IPlayer> players = getPlayers(context, name);
        if(players.size() == 1){
            return players.iterator().next();
        }
        throw UniversalCommandExceptionType.create("§cMore than one online player matches the input. Please search again with a UUID.");
    }

    @SneakyThrows
    @Override
    public Set<IPlayer> parse(StringReader reader) {
        int i = reader.getCursor();
        String string = StringArgumentType.readString(reader);

        UUID uuidOrNull = ValueUtil.getUuidOrNull(string);
        if (uuidOrNull != null) {
            IPlayer player = CommandHandler.getCore().getPlugin().getRunServer().getPlayerManager().getPlayer(uuidOrNull);
            if (player == null) {
                reader.setCursor(i);
                throw UniversalCommandExceptionType.create(
                        org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi("§cCould not find an online player with UUID §e{uuid}§",
                                new Pair<>("uuid", string)
                        ), reader);
            }
            HashSet<IPlayer> players = new HashSet<>();
            players.add(player);
            return players;
        }
        Set<IPlayer> players = CommandHandler.getCore().getPlugin().getRunServer().getPlayerManager().getPlayers(string);
        if (players.isEmpty()) {
            reader.setCursor(i);
            throw UniversalCommandExceptionType.create(
                    org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi("§cCould not find an online player named §e{name}§",
                            new Pair<>("name", string)
                    ), reader);
        }
        return players;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (IPlayer key : CommandHandler.getCore().getPlugin().getRunServer().getPlayerManager().getOnlinePlayers()) {
            if (key.getName().toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase()))
                builder.suggest(key.getName());
        }
        return builder.buildFuture();
    }
}
