package org.eu.pnxlr.git.litelogin.core.database.table;

import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.api.internal.util.tuple.Pair;
import org.eu.pnxlr.git.litelogin.core.database.SQLManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;

public class InGameProfileTableV3 {
    private static final String FIELD_IN_GAME_UUID = "in_game_uuid";
    private static final String FIELD_CURRENT_USERNAME_LOWER_CASE = "current_username_lower_case";
    private static final String FIELD_CURRENT_USERNAME_ORIGINAL = "current_username_original";
    private final String tableName;
    private final String tableNameV2;
    private final SQLManager sqlManager;

    public InGameProfileTableV3(SQLManager sqlManager, String tableName, String tableNameV2) {
        this.tableName = tableName;
        this.sqlManager = sqlManager;
        this.tableNameV2 = tableNameV2;
    }


    public void init(Connection connection) throws SQLException {
        String sql = MessageFormat.format(
                "CREATE TABLE IF NOT EXISTS {0} ( " +
                        "{1} VARBINARY(16) NOT NULL, " +
                        "{2} VARCHAR(64) DEFAULT NULL, " +
                        "{3} VARCHAR(64) DEFAULT NULL, " +
                        "CONSTRAINT IGPT_V3_PR PRIMARY KEY ( {1} ), " +
                        "CONSTRAINT IGPT_V3_UN UNIQUE ( {2} ))"
                , tableName, FIELD_IN_GAME_UUID, FIELD_CURRENT_USERNAME_LOWER_CASE, FIELD_CURRENT_USERNAME_ORIGINAL);
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.executeUpdate();
            // Check whether the new table contains data. If not, try migrating the data.
            try (
                    PreparedStatement prepareStatement = connection.prepareStatement("SELECT COUNT(0) FROM " + tableName);
                    ResultSet resultSet = prepareStatement.executeQuery();
            ) {
                resultSet.next();
                if (resultSet.getInt(1) != 0) {
                    // The new table already contains data, so migration is not needed
                    return;
                }
            }
            try (
                    PreparedStatement statement = connection.prepareStatement("SELECT COUNT(0) FROM " + tableNameV2);
                    ResultSet resultSet = statement.executeQuery()
            ) {
                resultSet.next();
                if (resultSet.getInt(1) == 0) {
                    // The old table contains no data, so migration is not needed
                    return;
                }
            } catch (Exception ignored) {
                // The old table does not exist, so migration is not needed
                return;
            }
        }

        LoggerProvider.getLogger().info("Migrating in-game profile data from " + tableNameV2 + " to " + tableName + '.');
        // Read the old table
        List<Pair<byte[], String>> oldData = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT in_game_uuid, current_username FROM " + tableNameV2);
             ResultSet resultSet = statement.executeQuery();) {
            while (resultSet.next()) {
                oldData.add(new Pair<>(resultSet.getBytes(1), resultSet.getString(2)));
            }
        }
        for (Pair<byte[], String> datum : oldData) {
            try (PreparedStatement statement = connection.prepareStatement(
                    String.format(
                            "INSERT INTO %s (%s, %s) VALUES (?, ?)", tableName, FIELD_IN_GAME_UUID, FIELD_CURRENT_USERNAME_LOWER_CASE
                    )
            )) {
                statement.setBytes(1, datum.getValue1());
                statement.setString(2, Optional.ofNullable(datum.getValue2()).map(String::toLowerCase).orElse(null));
                statement.executeUpdate();
            }
        }
        LoggerProvider.getLogger().info("Updated in game profile data, total " + oldData.size() + ".");
    }

    public Pair<UUID, String> get(UUID inGameUUID) throws SQLException {
        String sql = String.format(
                "SELECT %s FROM %s WHERE %s = ? LIMIT 1"
                , FIELD_CURRENT_USERNAME_ORIGINAL, tableName, FIELD_IN_GAME_UUID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(inGameUUID));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String string = resultSet.getString(1);
                    return new Pair<>(inGameUUID, string);
                }
            }
        }
        return null;
    }

    /**
     * Returns the in-game UUID.
     *
     * @param currentUsername username
     * @return in-game UUID
     */
    public UUID getInGameUUIDIgnoreCase(String currentUsername) throws SQLException {
        String sql = String.format(
                "SELECT %s FROM %s WHERE LOWER(%s) = ? LIMIT 1"
                , FIELD_IN_GAME_UUID, tableName, FIELD_CURRENT_USERNAME_LOWER_CASE
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, currentUsername.toLowerCase(Locale.ROOT));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return ValueUtil.bytesToUuid(resultSet.getBytes(1));
                }
            }
        }
        return null;
    }

    /**
     * Checks whether the data exists.
     *
     * @param inGameUUID in-game UUID
     * @return whether the data exists
     */
    public boolean dataExists(UUID inGameUUID) throws SQLException {
        String sql = String.format(
                "SELECT 1 FROM %s WHERE %s = ? LIMIT 1"
                , tableName, FIELD_IN_GAME_UUID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(inGameUUID));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Returns the in-game name.
     *
     * @param inGameUUID in-game UUID
     */
    public String getUsername(UUID inGameUUID) throws SQLException {
        String sql = String.format(
                "SELECT %s FROM %s WHERE %s = ? LIMIT 1"
                , FIELD_CURRENT_USERNAME_ORIGINAL, tableName, FIELD_IN_GAME_UUID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(inGameUUID));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return null;
    }

    /**
     * Updates the username.
     *
     * @param inGameUUID      in-game UUID
     * @param currentUsername new username
     * @throws SQLException
     */
    public void updateUsername(UUID inGameUUID, String currentUsername) throws SQLException {
        String sql = String.format(
                "UPDATE %s SET %s = ?, %s = ? WHERE %s = ?"
                , tableName, FIELD_CURRENT_USERNAME_LOWER_CASE, FIELD_CURRENT_USERNAME_ORIGINAL, FIELD_IN_GAME_UUID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, currentUsername.toLowerCase(Locale.ROOT));
            statement.setString(2, currentUsername);
            statement.setBytes(3, ValueUtil.uuidToBytes(inGameUUID));
            statement.executeUpdate();
        }
    }

    /**
     * Inserts a new record.
     *
     * @param inGameUUID in-game UUID
     */
    public void insertNewData(UUID inGameUUID, String currentUsername) throws SQLException {
        String sql = String.format(
                "INSERT INTO %s (%s, %s, %s) VALUES (?, ?, ?)"
                , tableName, FIELD_IN_GAME_UUID, FIELD_CURRENT_USERNAME_LOWER_CASE, FIELD_CURRENT_USERNAME_ORIGINAL
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            connection.setAutoCommit(false);
            statement.setBytes(1, ValueUtil.uuidToBytes(inGameUUID));
            statement.setString(2, currentUsername.toLowerCase());
            statement.setString(3, currentUsername);
            statement.executeUpdate();
            connection.commit();
        }
    }

    public boolean remove(UUID uuid) throws SQLException {
        String sql = String.format(
                "DELETE FROM %s WHERE %s = ?"
                , tableName, FIELD_IN_GAME_UUID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(uuid));
            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Clears the username usage record.
     *
     * @param currentUsername username
     */
    public int eraseUsername(String currentUsername) throws SQLException {
        String sql = String.format(
                "UPDATE %s SET %s = ?, %s = ? WHERE LOWER(%s) = ?"
                , tableName, FIELD_CURRENT_USERNAME_LOWER_CASE, FIELD_CURRENT_USERNAME_ORIGINAL, FIELD_CURRENT_USERNAME_LOWER_CASE
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, null);
            statement.setString(2, null);
            statement.setString(3, currentUsername.toLowerCase(Locale.ROOT));
            return statement.executeUpdate();
        }
    }

    public int eraseAllUsername() throws SQLException {
        String sql = String.format(
                "UPDATE %s SET %s = ?, %s = ?"
                , tableName, FIELD_CURRENT_USERNAME_LOWER_CASE, FIELD_CURRENT_USERNAME_ORIGINAL
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, null);
            statement.setString(2, null);
            return statement.executeUpdate();
        }
    }
}
