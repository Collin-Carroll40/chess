package service;

import dataaccess.MemoryAuthDAO;
import dataaccess.MemoryUserDAO;
import dataaccess.DataAccessException;
import org.junit.jupiter.api.*;
import request.LoginRequest;
import request.RegisterRequest;
import result.LoginResult;
import result.RegisterResult;
import service.UserService;

public class UserServiceTest {
    private MemoryUserDAO userDAO;
    private MemoryAuthDAO authDAO;
    private UserService service;

    @BeforeEach
    public void setup() {
        // Initialize the in-memory DAOs and the service for a fresh start before each test
        userDAO = new MemoryUserDAO();
        authDAO = new MemoryAuthDAO();
        service = new UserService(userDAO, authDAO);
    }

    @Test
    @DisplayName("Register Success (Happy Path)")
    public void registerSuccess() throws DataAccessException {
        RegisterRequest req = new RegisterRequest("Collin", "securePass123", "collin@byu.edu");
        RegisterResult res = service.register(req);

        // Assertions verify the response contains a valid token and the correct username
        Assertions.assertNotNull(res.authToken());
        Assertions.assertEquals("Collin", res.username());
    }

    @Test
    @DisplayName("Register Fail (Already Taken - Error Path)")
    public void registerFail() throws DataAccessException {
        RegisterRequest req = new RegisterRequest("Collin", "pass1", "email1");
        service.register(req); // First time succeeds

        // Second attempt with same username must throw an exception to prevent duplicates
        Assertions.assertThrows(DataAccessException.class, () -> service.register(req));
    }

    @Test
    @DisplayName("Login Success (Happy Path)")
    public void loginSuccess() throws DataAccessException {
        // Must register the user before they can log in
        service.register(new RegisterRequest("Collin", "myPassword", "c@c.com"));

        LoginResult res = service.login(new LoginRequest("Collin", "myPassword"));
        Assertions.assertNotNull(res.authToken());
        Assertions.assertEquals("Collin", res.username());
    }

    @Test
    @DisplayName("Login Fail (Wrong Password - Error Path)")
    public void loginFail() throws DataAccessException {
        service.register(new RegisterRequest("Collin", "correctPass", "c@c.com"));

        // Use an incorrect password to verify the service denies access
        LoginRequest badReq = new LoginRequest("Collin", "wrongPassword");
        Assertions.assertThrows(DataAccessException.class, () -> service.login(badReq));
    }

    @Test
    @DisplayName("Logout Success (Happy Path)")
    public void logoutSuccess() throws DataAccessException {
        RegisterResult regRes = service.register(new RegisterRequest("Collin", "p", "e"));

        // Verify logout does not throw an error for a valid token
        Assertions.assertDoesNotThrow(() -> service.logout(regRes.authToken()));

        // Verify the token is actually gone
        Assertions.assertNull(authDAO.getAuth(regRes.authToken()));
    }

    @Test
    @DisplayName("Logout Fail (Invalid Token - Error Path)")
    public void logoutFail() {
        // Attempting to logout with a non-existent token should fail
        Assertions.assertThrows(DataAccessException.class, () -> service.logout("fakeToken123"));
    }
}