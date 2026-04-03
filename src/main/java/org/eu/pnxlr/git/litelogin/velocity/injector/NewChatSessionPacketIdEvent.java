package org.eu.pnxlr.git.litelogin.velocity.injector;

import com.velocitypowered.api.network.ProtocolVersion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
public class NewChatSessionPacketIdEvent {
    private final int packetId;
    private final ProtocolVersion version;
}
