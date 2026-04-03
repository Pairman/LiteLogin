package org.eu.pnxlr.git.litelogin.core.database.table;

import org.eu.pnxlr.git.litelogin.api.internal.util.tuple.Pair;
import org.eu.pnxlr.git.litelogin.core.database.SQLManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;

/**
 * Skin restoration cache table.
 */
public class SkinRestoredCacheTableV2 {
    private static final String FIELD_CURRENT_SKIN_URL_SHA256 = "current_skin_url_sha256";
    private static final String FIELD_CURRENT_SKIN_MODEL = "current_skin_model";
    private static final String FIELD_RESTORER_VALUE = "restorer_value";
    private static final String FIELD_RESTORER_SIGNATURE = "restorer_signature";
    private final SQLManager sqlManager;
    private final String tableName;

    public SkinRestoredCacheTableV2(SQLManager sqlManager, String tableName) {
        this.sqlManager = sqlManager;
        this.tableName = tableName;
    }

    public void init(Connection connection) throws SQLException {
        String sql = MessageFormat.format(
                "CREATE TABLE IF NOT EXISTS {0} ( " +
                        "{1} VARBINARY(32) NOT NULL, " +
                        "{2} VARCHAR(16) NOT NULL, " +
                        "{3} LONGVARCHAR NOT NULL, " +
                        "{4} LONGVARCHAR NOT NULL, " +
                        "PRIMARY KEY ( {1}, {2} ))"
                , tableName, FIELD_CURRENT_SKIN_URL_SHA256, FIELD_CURRENT_SKIN_MODEL, FIELD_RESTORER_VALUE, FIELD_RESTORER_SIGNATURE);
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Returns the cached data object.
     *
     * @param urlSha256 skin URL
     * @param model     skin model
     * @return cached object
     */
    public Pair<String, String> getCacheRestored(byte[] urlSha256, String model) throws SQLException {
        String sql = String.format(
                "SELECT %s, %s FROM %s WHERE %s = ? AND %s = ? LIMIT 1"
                , FIELD_RESTORER_VALUE, FIELD_RESTORER_SIGNATURE, tableName, FIELD_CURRENT_SKIN_URL_SHA256, FIELD_CURRENT_SKIN_MODEL
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, urlSha256);
            statement.setString(2, model);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Pair<>(resultSet.getString(1), resultSet.getString(2));
                }
            }
        }
        return null;
    }

    /**
     * Inserts a new cached object.
     *
     * @param urlSha256 skin URL
     * @param model     skin model
     * @param value     value
     * @param signature signature
     */
    public void insertNew(byte[] urlSha256, String model, String value, String signature) throws SQLException {
        String sql = String.format(
                "INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?, ?) "
                , tableName, FIELD_CURRENT_SKIN_URL_SHA256, FIELD_CURRENT_SKIN_MODEL, FIELD_RESTORER_VALUE, FIELD_RESTORER_SIGNATURE
        );
        try (Connection connection = sqlManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setBytes(1, urlSha256);
            statement.setString(2, model);
            statement.setString(3, value);
            statement.setString(4, signature);
            statement.executeUpdate();
        }
    }
}
