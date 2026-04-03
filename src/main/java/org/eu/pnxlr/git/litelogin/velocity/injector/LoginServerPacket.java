package org.eu.pnxlr.git.litelogin.velocity.injector;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import lombok.AllArgsConstructor;

/**
 * 擦除登录验证签名的包
 */
@AllArgsConstructor
public class LoginServerPacket extends ServerLoginPacket {

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return super.handle(handler);
    }
}
