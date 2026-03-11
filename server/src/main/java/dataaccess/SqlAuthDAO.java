package dataaccess;

import model.AuthData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SqlAuthDAO implements AuthDAO {

    public SqlAuthDAO() throws DataAccessException {
        // Create the database if it doesn't exist
        DatabaseManager.createDatabase();

        // Create the auth table if it doesn't exist
        try (Connection conn = DatabaseManager.getConnection()) {
            var createTableStatement = """
                    CREATE TABLE IF NOT EXISTS auth (
                        authToken VARCHAR(255) NOT NULL,
                        username VARCHAR(255) NOT NULL,
                        PRIMARY KEY (authToken)
                    )""";
            try (PreparedStatement ps = conn.prepareStatement(createTableStatement)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException(String.format("Unable to configure database: %s", e.getMessage()));
        }
    }

    @Override
    public AuthData createAuth(String username) throws DataAccessException {
        // Generate a new random token
        String authToken = UUID.randomUUID().toString();

        // Insert it into the database
        try (Connection conn = DatabaseManager.getConnection()) {
            String statement = "INSERT INTO auth (authToken, username) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(statement)) {
                ps.setString(1, authToken); // Replaces the first ? with the token
                ps.setString(2, username);  // Replaces the second ? with the username
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException(String.format("Unable to create auth: %s", e.getMessage()));
        }

        return new AuthData(authToken, username);
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection()) {
            String statement = "SELECT authToken, username FROM auth WHERE authToken=?";
            try (PreparedStatement ps = conn.prepareStatement(statement)) {
                ps.setString(1, authToken);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new AuthData(rs.getString("authToken"), rs.getString("username"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(String.format("Unable to read auth: %s", e.getMessage()));
        }
        return null; // Token not found
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection()) {
            String statement = "DELETE FROM auth WHERE authToken=?";
            try (PreparedStatement ps = conn.prepareStatement(statement)) {
                ps.setString(1, authToken);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException(String.format("Unable to delete auth: %s", e.getMessage()));
        }
    }

    @Override
    public void clear() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection()) {
            String statement = "TRUNCATE TABLE auth";
            try (PreparedStatement ps = conn.prepareStatement(statement)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException(String.format("Unable to clear auth table: %s", e.getMessage()));
        }
    }
}