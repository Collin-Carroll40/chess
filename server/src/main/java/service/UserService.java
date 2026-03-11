package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.UserDAO;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import request.LoginRequest;
import request.RegisterRequest;
import result.LoginResult;
import result.RegisterResult;

public class UserService {
    private final UserDAO userDAO;
    private final AuthDAO authDAO;

    public UserService(UserDAO userDAO, AuthDAO authDAO) {
        this.userDAO = userDAO;
        this.authDAO = authDAO;
    }

    public RegisterResult register(RegisterRequest req) throws DataAccessException {
        if (req.username() == null || req.password() == null || req.email() == null) {
            throw new DataAccessException("Error: bad request");
        }

        if (userDAO.getUser(req.username()) != null) {
            throw new DataAccessException("Error: already taken");
        }

        // Hash password and save user
        String hashedPassword = BCrypt.hashpw(req.password(), BCrypt.gensalt());
        userDAO.createUser(new UserData(req.username(), hashedPassword, req.email()));

        AuthData auth = authDAO.createAuth(req.username());
        return new RegisterResult(auth.username(), auth.authToken());
    }

    public LoginResult login(LoginRequest req) throws DataAccessException {
        if (req.username() == null || req.password() == null) {
            throw new DataAccessException("Error: bad request");
        }

        UserData user = userDAO.getUser(req.username());

        // Verify hash match
        if (user == null || !BCrypt.checkpw(req.password(), user.password())) {
            throw new DataAccessException("Error: unauthorized login ");
        }

        AuthData auth = authDAO.createAuth(req.username());
        return new LoginResult(auth.username(), auth.authToken());
    }

    public void logout(String authToken) throws DataAccessException {
        if (authDAO.getAuth(authToken) == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        authDAO.deleteAuth(authToken);
    }
}