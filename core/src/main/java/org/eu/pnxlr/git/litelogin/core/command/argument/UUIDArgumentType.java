package org.eu.pnxlr.git.litelogin.core.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.command.UniversalCommandExceptionType;

import java.util.UUID;

/**
 * UUID 参数阅读程序
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UUIDArgumentType implements ArgumentType<UUID> {

    public static UUIDArgumentType uuid() {
        return new UUIDArgumentType();
    }

    public static UUID getUuid(final CommandContext<?> context, final String name) {
        return context.getArgument(name, UUID.class);
    }

    @Override
    public UUID parse(StringReader reader) throws CommandSyntaxException {
        int argBeginning = reader.getCursor();

        String uuidString = StringArgumentType.readString(reader);
        UUID ret = ValueUtil.getUuidOrNull(uuidString);
        if (ret == null) {
            reader.setCursor(argBeginning);
            throw UniversalCommandExceptionType.create(
                    ValueUtil.transPapi("§cInvalid UUID §e{value}§c.", new Pair<>("value", uuidString)),
                    reader
            );
        }
        return ret;
    }
}
