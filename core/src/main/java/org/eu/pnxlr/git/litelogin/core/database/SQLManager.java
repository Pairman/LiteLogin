package org.eu.pnxlr.git.litelogin.core.database;

import org.h2.jdbcx.JdbcConnectionPool;
import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;
import org.eu.pnxlr.git.litelogin.core.database.table.InGameProfileTableV3;
import org.eu.pnxlr.git.litelogin.core.database.table.SkinRestoredCacheTableV2;
import org.eu.pnxlr.git.litelogin.core.database.table.UserDataTableV3;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database manager.
 */
public class SQLManager {
    private static final String DEFAULT_H2_JDBC_URL_TEMPLATE = "jdbc:h2:{0};TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0";
    private static final String TABLE_PREFIX = "litelogin_";
    @Getter
    private final Core core;
    @Getter
    private JdbcConnectionPool pool;
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
        Class.forName("org.h2.Driver");
        String jdbcUrl = DEFAULT_H2_JDBC_URL_TEMPLATE.replace("{0}",
                core.getPlugin().getDataFolder().getAbsolutePath() +
                java.io.File.separator + LiteLoginConstants.DATABASE_NAME);
        pool = JdbcConnectionPool.create(jdbcUrl, LiteLoginConstants.DATABASE_NAME, LiteLoginConstants.DATABASE_NAME);
        final String inGameProfileTableNameV2 = TABLE_PREFIX + "in_game_profile_v2";
        final String inGameProfileTableNameV3 = TABLE_PREFIX + "in_game_profile_v3";
        final String userDataTableNameV2 = TABLE_PREFIX + "user_data_v2";
        final String userDataTableNameV3 = TABLE_PREFIX + "user_data_v3";
        final String skinRestorerCacheTableNameV2 = TABLE_PREFIX + "skin_restored_cache_v2";
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
        if (pool != null) pool.dispose();
    }
}
