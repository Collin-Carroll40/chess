package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.UserDAO;
import model.AuthData;
import model.UserData;
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
        // Check for bad requests (missing info)
        if (req.username() == null || req.password() == null || req.email() == null) {
            throw new DataAccessException("Error: bad request");
        }
        // Check if username is taken
        if (userDAO.getUser(req.username()) != null) {
            throw new DataAccessException("Error: already taken");
        }

        // Create the user and generate an auth token
        userDAO.createUser(new UserData(req.username(), req.password(), req.email()));
        AuthData auth = authDAO.createAuth(req.username());

        return new RegisterResult(auth.username(), auth.authToken());
    }

    public LoginResult login(LoginRequest req) throws DataAccessException {
        UserData user = userDAO.getUser(req.username());

        // Verify user exists + password match
        if (user == null || !user.password().equals(req.password())) {
            throw new DataAccessException("Error: unauthorized");
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