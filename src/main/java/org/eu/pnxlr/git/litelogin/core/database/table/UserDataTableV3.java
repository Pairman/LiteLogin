package org.eu.pnxlr.git.litelogin.core.database.table;

import lombok.AllArgsConstructor;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.api.internal.util.tuple.Triple;
import org.eu.pnxlr.git.litelogin.core.command.CommandHandler;
import org.eu.pnxlr.git.litelogin.core.configuration.BaseServiceConfig;
import org.eu.pnxlr.git.litelogin.core.database.SQLManager;

import java.sql.*;
import java.text.MessageFormat;
import java.util.*;

/**
 * Player data table.
 */
public class UserDataTableV3 {
    private static final String FIELD_ONLINE_UUID = "online_uuid";
    private static final String FIELD_ONLINE_NAME = "online_name";
    private static final String FIELD_SERVICE_ID = "service_id";
    private static final String FIELD_IN_GAME_PROFILE_UUID = "in_game_profile_uuid";
    private static final String FIELD_WHITELIST = "whitelist";
    private final SQLManager sqlManager;
    private final String tableName;
    private final String tableNameV2;

    public UserDataTableV3(SQLManager sqlManager, String tableName, String tableNameV2) {
        this.sqlManager = sqlManager;
        this.tableName = tableName;
        this.tableNameV2 = tableNameV2;
    }

    public void init(Connection connection) throws SQLException {
        String sql = MessageFormat.format(
                "CREATE TABLE IF NOT EXISTS {0} ( " +
                        "{1} VARBINARY(16) NOT NULL, " +
                        "{2} INTEGER NOT NULL, " +
                        "{3} VARCHAR(64) DEFAULT NULL, " +
                        "{4} VARBINARY(16) DEFAULT NULL, " +
                        "{5} BOOLEAN DEFAULT FALSE, " +
                        "PRIMARY KEY ( {1}, {2} ))"
                , tableName, FIELD_ONLINE_UUID, FIELD_SERVICE_ID, FIELD_ONLINE_NAME, FIELD_IN_GAME_PROFILE_UUID, FIELD_WHITELIST);
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


        LoggerProvider.getLogger().info("Migrating user data from " + tableNameV2 + " to " + tableName + '.');
        @AllArgsConstructor
        class V2Entry {
            private final byte[] onlineUUID;
            private final int serviceId;
            private final byte[] inGameProfileUUID;
            private final boolean whitelist;
        }
        // Read the old table
        List<V2Entry> oldData = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT online_uuid, yggdrasil_id, in_game_profile_uuid, whitelist FROM " + tableNameV2);
             ResultSet resultSet = statement.executeQuery();) {
            while (resultSet.next()) {
                oldData.add(new V2Entry(resultSet.getBytes(1),
                        resultSet.getBytes(2)[0],
                        resultSet.getBytes(3),
                        resultSet.getBoolean(4))
                );
            }
        }
        for (V2Entry datum : oldData) {
            try (PreparedStatement statement = connection.prepareStatement(
                    String.format(
                            "INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?)", tableName, FIELD_ONLINE_UUID, FIELD_SERVICE_ID, FIELD_IN_GAME_PROFILE_UUID, FIELD_WHITELIST
                    )
            )) {
                statement.setBytes(1, datum.onlineUUID);
                statement.setInt(2, datum.serviceId);
                statement.setBytes(3, datum.inGameProfileUUID);
                statement.setBoolean(4, datum.whitelist);
                statement.executeUpdate();
            }
        }
        LoggerProvider.getLogger().info("Updated user data, total " + oldData.size() + ".");
    }

    public Triple<String, UUID, Boolean> get(UUID onlineUUID, int serviceId) throws SQLException {
        String sql = String.format(
                "SELECT %s, %s, %s FROM %s WHERE %s = ? AND %s = ? LIMIT 1"
                , FIELD_ONLINE_NAME, FIELD_IN_GAME_PROFILE_UUID, FIELD_WHITELIST, tableName, FIELD_ONLINE_UUID, FIELD_SERVICE_ID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(onlineUUID));
            statement.setInt(2, serviceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Triple<>(
                            resultSet.getString(1),
                            Optional.ofNullable(resultSet.getBytes(2)).map(ValueUtil::bytesToUuid).orElse(null),
                            resultSet.getBoolean(3));
                }
            }
        }
        return null;
    }

    public UUID getOnlineUUID(String username, int serviceId) throws SQLException {
        String sql = String.format(
                "SELECT %s FROM %s WHERE lower(%s) = ? AND %s = ? LIMIT 1"
                , FIELD_ONLINE_UUID, tableName, FIELD_ONLINE_NAME, FIELD_SERVICE_ID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username.toLowerCase(Locale.ROOT));
            statement.setInt(2, serviceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getBytes(1)).map(ValueUtil::bytesToUuid).orElse(null);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the user's in-game UUID from the database.
     *
     * @param onlineUUID user's online UUID
     * @param serviceId  authentication service ID associated with the online UUID
     * @return retrieved in-game UUID
     */
    public UUID getInGameUUID(UUID onlineUUID, int serviceId) throws SQLException {
        String sql = String.format(
                "SELECT %s FROM %s WHERE %s = ? AND %s = ? LIMIT 1"
                , FIELD_IN_GAME_PROFILE_UUID, tableName, FIELD_ONLINE_UUID, FIELD_SERVICE_ID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(onlineUUID));
            statement.setInt(2, serviceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getBytes(1)).map(ValueUtil::bytesToUuid).orElse(null);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the authentication service IDs used by the player at login.
     *
     * @param inGameUUID user's in-game UUID
     * @return retrieved online information
     */
    public Set<Integer> getOnlineServiceIds(UUID inGameUUID) throws SQLException {
        Set<Integer> result = new HashSet<>();
        String sql = String.format(
                "SELECT %s FROM %s WHERE %s = ?"
                , FIELD_SERVICE_ID, tableName, FIELD_IN_GAME_PROFILE_UUID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(inGameUUID));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(resultSet.getInt(1));
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }


    /**
     * Returns the profile set.
     *
     * @param inGameUUID in-game UUID
     */
    public Set<Triple<UUID, String, Integer>> getOnlineProfiles(UUID inGameUUID) throws SQLException {
        Set<Triple<UUID, String, Integer>> result = new HashSet<>();
        String sql = String.format(
                "SELECT %s, %s, %s FROM %s WHERE %s = ?"
                , FIELD_ONLINE_UUID, FIELD_ONLINE_NAME, FIELD_SERVICE_ID, tableName, FIELD_IN_GAME_PROFILE_UUID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(inGameUUID));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new Triple<>(
                            Optional.ofNullable(resultSet.getBytes(1)).map(ValueUtil::bytesToUuid).orElse(null),
                            resultSet.getString(2),
                            resultSet.getInt(3)
                    ));
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Sets the in-game UUID.
     *
     * @param onlineUUID    online UUID
     * @param serviceId     service ID
     * @param newInGameUUID new in-game UUID
     */
    public int setInGameUUID(UUID onlineUUID, int serviceId, UUID newInGameUUID) throws SQLException {
        String sql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ? AND %s = ?"
                , tableName, FIELD_IN_GAME_PROFILE_UUID, FIELD_ONLINE_UUID, FIELD_SERVICE_ID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(newInGameUUID));
            statement.setBytes(2, ValueUtil.uuidToBytes(onlineUUID));
            statement.setInt(3, serviceId);
            return statement.executeUpdate();
        }
    }

    /**
     * Checks whether the data exists.
     *
     * @param onlineUUID  online UUID
     * @param serviceId service Id
     */
    public boolean dataExists(UUID onlineUUID, int serviceId) throws SQLException {
        String sql = String.format(
                "SELECT 1 FROM %s WHERE %s = ? AND %s = ? LIMIT 1"
                , tableName, FIELD_ONLINE_UUID, FIELD_SERVICE_ID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(onlineUUID));
            statement.setInt(2, serviceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Inserts a user record.
     *
     * @param onlineUUID  user's online UUID
     * @param serviceId authentication service ID associated with the online UUID
     * @param inGameUUID  new in-game UUID for the user
     * @return number of affected rows
     */
    public int insertNewData(UUID onlineUUID, int serviceId, String onlineName, UUID inGameUUID) throws SQLException {
        String sql = String.format(
                "INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?) "
                , tableName, FIELD_ONLINE_UUID, FIELD_SERVICE_ID, FIELD_ONLINE_NAME, FIELD_IN_GAME_PROFILE_UUID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(onlineUUID));
            statement.setInt(2, serviceId);
            statement.setString(3, onlineName);
            if (inGameUUID == null) {
                statement.setNull(4, Types.VARBINARY);
            } else {
                statement.setBytes(4, ValueUtil.uuidToBytes(inGameUUID));
            }
            return statement.executeUpdate();
        }
    }

    /**
     * Sets whitelist status.
     *
     * @param onlineUUID  online UUID
     * @param serviceId service Id
     * @param whitelist   new whitelist status
     */
    public void setWhitelist(UUID onlineUUID, int serviceId, boolean whitelist) throws SQLException {
        String sql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ? AND %s = ?"
                , tableName, FIELD_WHITELIST, FIELD_ONLINE_UUID, FIELD_SERVICE_ID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBoolean(1, whitelist);
            statement.setBytes(2, ValueUtil.uuidToBytes(onlineUUID));
            statement.setInt(3, serviceId);
            statement.executeUpdate();
        }
    }

    /**
     * Queries whitelist status.
     */
    public boolean hasWhitelist(UUID onlineUUID, int serviceId) throws SQLException {
        String sql = String.format(
                "SELECT %s FROM %s WHERE %s = ? AND %s = ? LIMIT 1"
                , FIELD_WHITELIST, tableName, FIELD_ONLINE_UUID, FIELD_SERVICE_ID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(onlineUUID));
            statement.setInt(2, serviceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
            }
        }
        return false;
    }

    /**
     * Queries whitelist status.
     */
    public boolean hasWhitelist(UUID inGameUUID) throws SQLException {
        String sql = String.format(
                "SELECT %s FROM %s WHERE %s = ? LIMIT 1"
                , FIELD_WHITELIST, tableName, FIELD_IN_GAME_PROFILE_UUID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(inGameUUID));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
            }
        }
        return false;
    }

    /**
     * Sets whitelist status.
     */
    public void setWhitelist(UUID inGameUUID, boolean whitelist) throws SQLException {
        String sql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ?"
                , tableName, FIELD_WHITELIST, FIELD_IN_GAME_PROFILE_UUID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBoolean(1, whitelist);
            statement.setBytes(2, ValueUtil.uuidToBytes(inGameUUID));
            statement.executeUpdate();
        }
    }

    /**
     * Lists the whitelist.
     */
    public List<String> listWhitelist(boolean verbose) throws SQLException {
        String sql = verbose ? String.format(
            "SELECT %s, %s, %s, %s FROM %s WHERE %s = true",
            FIELD_ONLINE_NAME, FIELD_SERVICE_ID, FIELD_ONLINE_UUID, FIELD_IN_GAME_PROFILE_UUID,
            tableName, FIELD_WHITELIST
        ) : String.format(
            "SELECT %s FROM %s WHERE %s = true",
            FIELD_ONLINE_NAME, tableName, FIELD_WHITELIST
        );
        List<String> result = new ArrayList<>();
        try (
            Connection connection = sqlManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {
            if (verbose)
                while (resultSet.next()) {
                    int serviceId = resultSet.getInt(2);
                    BaseServiceConfig serviceConfig = CommandHandler.getCore()
                            .getPluginConfig().getServiceIdMap().get(serviceId);
                    String serviceName = serviceConfig == null ? "Service missing" : serviceConfig.getName();
                    result.add(String.format(
                        "%s (%s=%d(%s), %s=%s, %s=%s)",
                        resultSet.getString(1),
                        FIELD_SERVICE_ID, serviceId, serviceName,
                        FIELD_ONLINE_UUID, ValueUtil.bytesToUuid(resultSet.getBytes(3)),
                        FIELD_IN_GAME_PROFILE_UUID, Optional.ofNullable(resultSet.getBytes(4)).map(ValueUtil::bytesToUuid).orElse(null)
                    ));
                }
            else
                while (resultSet.next())
                    result.add(String.format("%s", resultSet.getString(1)));
        }
        return result;
    }

    public void setOnlineName(UUID onlineUUID, int serviceId, String onlineName) throws SQLException {
        String sql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ? AND %s = ?"
                , tableName, FIELD_ONLINE_NAME, FIELD_ONLINE_UUID, FIELD_SERVICE_ID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, onlineName);
            statement.setBytes(2, ValueUtil.uuidToBytes(onlineUUID));
            statement.setInt(3, serviceId);
            statement.executeUpdate();
        }
    }

    public String getOnlineName(UUID onlineUUID, int serviceId) throws SQLException {
        String sql = String.format(
                "SELECT %s FROM %s WHERE %s = ? AND %s = ? LIMIT 1"
                , FIELD_ONLINE_NAME, tableName, FIELD_ONLINE_UUID, FIELD_SERVICE_ID
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, ValueUtil.uuidToBytes(onlineUUID));
            statement.setInt(2, serviceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
            return null;
        }
    }
}
