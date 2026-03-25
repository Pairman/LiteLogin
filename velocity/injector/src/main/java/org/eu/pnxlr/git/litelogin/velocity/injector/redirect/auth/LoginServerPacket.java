package org.eu.pnxlr.git.litelogin.velocity.injector.redirect.auth;

import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import lombok.AllArgsConstructor;
import org.eu.pnxlr.git.litelogin.api.internal.main.CoreAPI;
import org.eu.pnxlr.git.litelogin.velocity.injector.proxy.IdentifiedKeyInvocationHandler;

import java.lang.reflect.Proxy;

/**
 * 擦除登录验证签名的包
 */
@AllArgsConstructor
public class LoginServerPacket extends ServerLoginPacket {
    private final CoreAPI coreApi;

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
//        if (getPlayerKey() != null) {
//            setPlayerKey((IdentifiedKey) Proxy.newProxyInstance(
//                    Thread.currentThread().getContextClassLoader(),
//                    new Class[]{IdentifiedKey.class}, new IdentifiedKeyInvocationHandler(getPlayerKey())));
//        }

        return super.handle(handler);
    }
}
