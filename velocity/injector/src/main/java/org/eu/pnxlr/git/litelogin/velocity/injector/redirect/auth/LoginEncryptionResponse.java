package org.eu.pnxlr.git.litelogin.velocity.injector.redirect.auth;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket;
import lombok.AllArgsConstructor;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.main.CoreAPI;
import org.eu.pnxlr.git.litelogin.velocity.injector.handler.InitialLoginSessionHandlerBridge;
import net.kyori.adventure.text.Component;

/**
 * EncryptionResponse 数据包处理
 */
@AllArgsConstructor
public class LoginEncryptionResponse extends EncryptionResponsePacket {
    private final CoreAPI coreApi;

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        if (!(handler instanceof InitialLoginSessionHandler)) {
            return super.handle(handler);
        }
        InitialLoginSessionHandlerBridge loginSessionHandler = new InitialLoginSessionHandlerBridge(((InitialLoginSessionHandler) handler), coreApi);
        try {
            loginSessionHandler.handle(this);
        } catch (Throwable e) {
            if (loginSessionHandler.isEncrypted()) {
                loginSessionHandler.getInbound().disconnect(Component.text("§cAn exception occurred while handling the login request. Please contact the server administrator."));
            }
            loginSessionHandler.getMcConnection().close(true);
            LoggerProvider.getLogger().error("An exception occurred while processing a login request.", e);
        }
        return true;
    }
}
