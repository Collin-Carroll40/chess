package dataaccess;

import model.UserData;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

class SqlUserDAOTest {

    private SqlUserDAO userDAO;

    @BeforeEach
    void setUp() throws DataAccessException {
        userDAO = new SqlUserDAO();
        userDAO.clear();
    }

    @Test
    @DisplayName("Positive: Create User")
    void createUserPositive() throws DataAccessException {
        // Manually hash the password so the DAO receives a valid hash format
        String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw("password123", org.mindrot.jbcrypt.BCrypt.gensalt());
        UserData testUser = new UserData("Collin", hashedPassword, "collin@test.com");

        userDAO.createUser(testUser);
        UserData fetchedUser = userDAO.getUser("Collin");

        assertNotNull(fetchedUser);
        assertEquals("Collin", fetchedUser.username());
        // Verification will now succeed because the stored password is a valid hash
        assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw("password123", fetchedUser.password()));
    }

    @Test
    @DisplayName("Negative: Create Duplicate User")
    void createUserNegative() throws DataAccessException {
        UserData testUser = new UserData("Collin", "password123", "collin@test.com");
        userDAO.createUser(testUser);

        // Trying to create the exact same user again should crash the database
        assertThrows(DataAccessException.class, () -> userDAO.createUser(testUser));
    }

    @Test
    @DisplayName("Positive: Get User")
    void getUserPositive() throws DataAccessException {
        userDAO.createUser(new UserData("Abbey", "securePass", "abbey@test.com"));
        UserData result = userDAO.getUser("Abbey");
        assertNotNull(result);
        assertEquals("Abbey", result.username());
    }

    @Test
    @DisplayName("Negative: Get Fake User")
    void getUserNegative() throws DataAccessException {
        // Getting a user that doesn't exist should just return null
        UserData result = userDAO.getUser("GhostUser");
        assertNull(result);
    }

    @Test
    @DisplayName("Positive: Clear Users")
    void clearUsers() throws DataAccessException {
        userDAO.createUser(new UserData("Collin", "password123", "collin@test.com"));
        userDAO.clear();
        assertNull(userDAO.getUser("Collin"));
    }
}