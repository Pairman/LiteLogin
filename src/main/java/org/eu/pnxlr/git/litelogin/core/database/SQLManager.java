package org.eu.pnxlr.git.litelogin.core.database;

import lombok.Getter;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;
import org.eu.pnxlr.git.litelogin.core.database.table.InGameProfileTableV3;
import org.eu.pnxlr.git.litelogin.core.database.table.SkinRestoredCacheTableV2;
import org.eu.pnxlr.git.litelogin.core.database.table.UserDataTableV3;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database manager.
 */
public class SQLManager {
    private static final String DATABASE_DIRECTORY_NAME = "database";
    private static final String HSQLDB_FILE_PREFIX = "file:";
    private static final String TABLE_PREFIX = "litelogin_";
    @Getter
    private final Core core;
    private String jdbcUrl;
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
        Class.forName("org.hsqldb.jdbc.JDBCDriver");
        File databaseDirectory = new File(core.getDataFolder(), DATABASE_DIRECTORY_NAME);
        ensureDatabaseDirectory(databaseDirectory);
        File databaseFile = new File(databaseDirectory, LiteLoginConstants.DATABASE_NAME);
        jdbcUrl = "jdbc:hsqldb:" + HSQLDB_FILE_PREFIX + databaseFile.getAbsolutePath();
        final String inGameProfileTableNameV2 = TABLE_PREFIX + "in_game_profile_v2";
        final String inGameProfileTableNameV3 = TABLE_PREFIX + "in_game_profile_v3";
        final String userDataTableNameV2 = TABLE_PREFIX + "user_data_v2";
        final String userDataTableNameV3 = TABLE_PREFIX + "user_data_v3";
        final String skinRestorerCacheTableNameV2 = TABLE_PREFIX + "skin_restored_cache_v2";
        userDataTable = new UserDataTableV3(this, userDataTableNameV3, userDataTableNameV2);
        skinRestoredCacheTable = new SkinRestoredCacheTableV2(this, skinRestorerCacheTableNameV2);
        inGameProfileTable = new InGameProfileTableV3(this, inGameProfileTableNameV3, inGameProfileTableNameV2);

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            userDataTable.init(connection);
            inGameProfileTable.init(connection);
            skinRestoredCacheTable.init(connection);
            connection.commit();
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    public void close() {
        if (jdbcUrl != null) {
            try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
                connection.createStatement().execute("SHUTDOWN");
            } catch (SQLException ignored) {
            }
        }
        jdbcUrl = null;
    }

    private void ensureDatabaseDirectory(File databaseDirectory) {
        if (databaseDirectory.isDirectory()) {
            return;
        }
        if (databaseDirectory.exists()) {
            throw new RuntimeException("Database path exists but is not a directory: " + databaseDirectory.getAbsolutePath());
        }
        if (!databaseDirectory.mkdirs()) {
            throw new RuntimeException("Unable to create database directory: " + databaseDirectory.getAbsolutePath());
        }
    }
}
