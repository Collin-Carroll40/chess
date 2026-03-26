package client; /** i cant get these to pass **/

import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clear() throws Exception {
        facade.clear();
    }

    // ===== REGISTER =====
    @Test
    void registerPositive() throws Exception {
        var result = facade.register("testUser", "pass123", "test@email.com");
        assertNotNull(result.authToken());
        assertEquals("testUser", result.username());
    }

    @Test
    void registerNegativeDuplicate() {
        assertDoesNotThrow(() -> facade.register("dupUser", "pass", "e@e.com"));
        assertThrows(Exception.class, () -> facade.register("dupUser", "pass", "e@e.com"));
    }

    // ===== LOGIN =====
    @Test
    void loginPositive() throws Exception {
        facade.register("loginUser", "pass", "e@e.com");
        var result = facade.login("loginUser", "pass");
        assertNotNull(result.authToken());
    }

    @Test
    void loginNegativeBadPassword() throws Exception {
        facade.register("loginUser2", "pass", "e@e.com");
        assertThrows(Exception.class, () -> facade.login("loginUser2", "wrong"));
    }

    // ===== LOGOUT =====
    @Test
    void logoutPositive() throws Exception {
        var auth = facade.register("logoutUser", "pass", "e@e.com");
        assertDoesNotThrow(() -> facade.logout(auth.authToken()));
    }

    @Test
    void logoutNegativeBadToken() {
        assertThrows(Exception.class, () -> facade.logout("badToken"));
    }

    // ===== CREATE GAME =====
    @Test
    void createGamePositive() throws Exception {
        var auth = facade.register("gameUser", "pass", "e@e.com");
        var result = facade.createGame(auth.authToken(), "MyGame");
        assertTrue(result.gameID() > 0);
    }

    @Test
    void createGameNegativeUnauthorized() {
        assertThrows(Exception.class, () -> facade.createGame("badToken", "MyGame"));
    }

    // ===== LIST GAMES =====
    @Test
    void listGamesPositive() throws Exception {
        var auth = facade.register("listUser", "pass", "e@e.com");
        facade.createGame(auth.authToken(), "Game1");
        facade.createGame(auth.authToken(), "Game2");
        var games = facade.listGames(auth.authToken());
        assertEquals(2, games.length);
    }

    @Test
    void listGamesNegativeUnauthorized() {
        assertThrows(Exception.class, () -> facade.listGames("badToken"));
    }

    // ===== JOIN GAME =====
    @Test
    void joinGamePositive() throws Exception {
        var auth = facade.register("joinUser", "pass", "e@e.com");
        var game = facade.createGame(auth.authToken(), "JoinGame");
        assertDoesNotThrow(() -> facade.joinGame(auth.authToken(), game.gameID(), "WHITE"));
    }

    @Test
    void joinGameNegativeBadColor() throws Exception {
        var auth = facade.register("joinUser2", "pass", "e@e.com");
        var game = facade.createGame(auth.authToken(), "JoinGame2");
        facade.joinGame(auth.authToken(), game.gameID(), "WHITE");
        // Second user tries to take same color
        var auth2 = facade.register("joinUser3", "pass2", "e2@e.com");
        assertThrows(Exception.class, () -> facade.joinGame(auth2.authToken(), game.gameID(), "WHITE"));
    }
    // ===== CLEAR =====
    @Test
    void clearPositive() throws Exception {
        facade.register("clearUser", "pass", "e@e.com");
        assertDoesNotThrow(() -> facade.clear());
    }
}