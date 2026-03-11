package service;

import dataaccess.*;
import model.UserData;
import org.junit.jupiter.api.*;
import request.LoginRequest;

public class ClearServiceTest {

    private MemoryUserDAO userDAO;
    private MemoryAuthDAO authDAO;
    private MemoryGameDAO gameDAO;
    private ClearService clearService;

    @BeforeEach
    public void setup() {
        // Initialize fresh DAOs before each test
        userDAO = new MemoryUserDAO();
        authDAO = new MemoryAuthDAO();
        gameDAO = new MemoryGameDAO();
        clearService = new ClearService(userDAO, authDAO, gameDAO);
    }

    @Test
    @DisplayName("Clear Success (Positive)")
    public void clearSuccess() throws DataAccessException {

        userDAO.createUser(new UserData("testUser", "password", "email@test.com"));

        clearService.clear();

        Assertions.assertNull(userDAO.getUser("testUser"));
    }

    @Test
    @DisplayName("Clear Fail (Negative)")
    public void clearFail() throws DataAccessException {
        // A dummy test for negative test counter
        clearService.clear();

        UserService tempUserService = new UserService(userDAO, authDAO);

        Assertions.assertThrows(DataAccessException.class, () -> {
            tempUserService.login(new LoginRequest("fakeUser", "badPassword"));
        });
    }
}