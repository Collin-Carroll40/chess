package dataaccess;

import model.AuthData;

public class SqlAuthDAO implements AuthDAO {
    public SqlAuthDAO() throws DataAccessException {
        // We will configure the database here in the next step
    }

    @Override
    public AuthData createAuth(String username) throws DataAccessException {
        return null;
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        return null;
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
    }

    @Override
    public void clear() throws DataAccessException {
    }
}