package org.eu.pnxlr.git.litelogin.core.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.command.UniversalCommandExceptionType;
import org.eu.pnxlr.git.litelogin.core.configuration.service.BaseServiceConfig;

import java.util.concurrent.CompletableFuture;

/**
 * Service 参数阅读程序
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceIdArgumentType implements ArgumentType<BaseServiceConfig> {

    public static ServiceIdArgumentType service() {
        return new ServiceIdArgumentType();
    }

    public static BaseServiceConfig getService(final CommandContext<?> context, final String name) {
        return context.getArgument(name, BaseServiceConfig.class);
    }

    protected static BaseServiceConfig readServiceConfig(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        final int result = reader.readInt();
        BaseServiceConfig config = CommandHandler.getCore().getPluginConfig().getServiceIdMap().get(result);
        if (config == null) {
            reader.setCursor(start);
            throw UniversalCommandExceptionType.create(
                    org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                            "§cCould not find an authentication service with id §e{service_id}§c.",
                            new Pair<>("service_id", result)
                    ),
                    reader
            );
        }
        return config;
    }

    public static <S> CompletableFuture<Suggestions> getSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        CommandHandler.getCore().getPluginConfig().getServiceIdMap().forEach((key, value) -> {
            if ((key + "").startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(key);
            }
        });
        return builder.buildFuture();
    }

    @Override
    public BaseServiceConfig parse(StringReader reader) throws CommandSyntaxException {
        return readServiceConfig(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return getSuggestions(context, builder);
    }
}
