package dataaccess;

import model.UserData;

public class SqlUserDAO implements UserDAO {
    public SqlUserDAO() throws DataAccessException {
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        return null;
    }

    @Override
    public void clear() throws DataAccessException {
    }
}