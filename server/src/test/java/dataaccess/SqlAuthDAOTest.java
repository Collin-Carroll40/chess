package dataaccess;

import model.AuthData;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class SqlAuthDAOTest {

    private SqlAuthDAO authDAO;

    @BeforeEach
    void setUp() throws DataAccessException {
        authDAO = new SqlAuthDAO();
        authDAO.clear();
    }

    @Test
    @DisplayName("Positive: Create Auth")
    void createAuthPositive() throws DataAccessException {
        AuthData auth = authDAO.createAuth("Collin");
        assertNotNull(auth);
        assertNotNull(auth.authToken());
        assertEquals("Collin", auth.username());
    }

    @Test
    @DisplayName("Negative: Create Auth with Null Username")
    void createAuthNegative() {
        // SQL should block a null username because of the NOT NULL constraint
        assertThrows(DataAccessException.class, () -> authDAO.createAuth(null));
    }

    @Test
    @DisplayName("Positive: Get Auth")
    void getAuthPositive() throws DataAccessException {
        AuthData created = authDAO.createAuth("Abbey");
        AuthData retrieved = authDAO.getAuth(created.authToken());
        assertNotNull(retrieved);
        assertEquals(created.authToken(), retrieved.authToken());
    }

    @Test
    @DisplayName("Negative: Get Fake Auth")
    void getAuthNegative() throws DataAccessException {
        AuthData retrieved = authDAO.getAuth("fake-bad-token");
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Positive: Delete Auth")
    void deleteAuthPositive() throws DataAccessException {
        AuthData created = authDAO.createAuth("Collin");
        authDAO.deleteAuth(created.authToken());
        assertNull(authDAO.getAuth(created.authToken()));
    }

    @Test
    @DisplayName("Negative: Delete Fake Auth")
    void deleteAuthNegative() throws DataAccessException {
        // Deleting a fake token shouldn't crash, but it shouldn't delete real ones either
        AuthData realAuth = authDAO.createAuth("Collin");
        authDAO.deleteAuth("fake-token");
        assertNotNull(authDAO.getAuth(realAuth.authToken()));
    }

    @Test
    @DisplayName("Positive: Clear Auth")
    void clearAuth() throws DataAccessException {
        authDAO.createAuth("Collin");
        authDAO.clear();
        // Since we don't know the token, we just ensure the DB doesn't crash on clear
        assertDoesNotThrow(() -> authDAO.clear());
    }
}