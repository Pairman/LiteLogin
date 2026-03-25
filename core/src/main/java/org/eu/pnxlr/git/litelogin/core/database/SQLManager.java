package org.eu.pnxlr.git.litelogin.core.database;

import lombok.Getter;
import org.eu.pnxlr.git.litelogin.core.configuration.SqlConfig;
import org.eu.pnxlr.git.litelogin.core.database.pool.H2ConnectionPool;
import org.eu.pnxlr.git.litelogin.core.database.pool.ISQLConnectionPool;
import org.eu.pnxlr.git.litelogin.core.database.table.InGameProfileTableV3;
import org.eu.pnxlr.git.litelogin.core.database.table.SkinRestoredCacheTableV2;
import org.eu.pnxlr.git.litelogin.core.database.table.UserDataTableV3;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库管理程序
 */
public class SQLManager {
    @Getter
    private final Core core;
    @Getter
    private ISQLConnectionPool pool;
    @Getter
    private InGameProfileTableV3 inGameProfileTable;
    @Getter
    private UserDataTableV3 userDataTable;
    @Getter
    private SkinRestoredCacheTableV2 skinRestoredCacheTable;


    public SQLManager(Core core) {
        this.core = core;
    }

    public void init() throws SQLException, ClassNotFoundException {
        SqlConfig sqlConfig = core.getPluginConfig().getSqlConfig();
        pool = new H2ConnectionPool(
                core.getPlugin().getDataFolder(),
                sqlConfig.getUsername(),
                sqlConfig.getPassword(),
                sqlConfig.getConnectUrl().isEmpty() ? H2ConnectionPool.defaultUrl : sqlConfig.getConnectUrl()
        );
        String tablePrefix = sqlConfig.getTablePrefix() + '_';

        final String inGameProfileTableNameV2 = tablePrefix + "in_game_profile_v2";
        final String inGameProfileTableNameV3 = tablePrefix + "in_game_profile_v3";
        final String userDataTableNameV2 = tablePrefix + "user_data_v2";
        final String userDataTableNameV3 = tablePrefix + "user_data_v3";
        final String skinRestorerCacheTableNameV2 = tablePrefix + "skin_restored_cache_v2";
        userDataTable = new UserDataTableV3(this, userDataTableNameV3, userDataTableNameV2);
        skinRestoredCacheTable = new SkinRestoredCacheTableV2(this, skinRestorerCacheTableNameV2);
        inGameProfileTable = new InGameProfileTableV3(this, inGameProfileTableNameV3, inGameProfileTableNameV2);

        try (Connection connection = getPool().getConnection()){
            connection.setAutoCommit(false);
            userDataTable.init(connection);
            inGameProfileTable.init(connection);
            skinRestoredCacheTable.init(connection);
            connection.commit();
        }
    }

    public void close() {
        if (pool != null) pool.close();
    }
}
