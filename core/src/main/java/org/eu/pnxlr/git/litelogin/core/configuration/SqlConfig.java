package org.eu.pnxlr.git.litelogin.core.configuration;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * 表示数据库配置
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
public class SqlConfig {
    private final String username;
    private final String password;
    private final String tablePrefix;
    private final String connectUrl;

    public static SqlConfig read(CommentedConfigurationNode node) throws SerializationException {
        String username = node.node("username").getString("root");
        String password = node.node("password").getString("root");
        String tablePrefix = node.node("tablePrefix").getString("litelogin");
        String connectUrl = node.node("connectUrl").getString("");

        return new SqlConfig(username, password, tablePrefix, connectUrl);
    }
}
