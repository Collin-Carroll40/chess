package dataaccess;

import java.sql.SQLException;

public class DataAccessException extends Exception {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String failedToGetConnection, SQLException ex) {
    }
}