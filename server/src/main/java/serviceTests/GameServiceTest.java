package serviceTests;

import dataaccess.*;
import model.AuthData;
import org.junit.jupiter.api.*;
import request.CreateGameRequest;
import request.JoinGameRequest;
import service.GameService;

public class GameServiceTest {
    private MemoryGameDAO gameDAO;
    private MemoryAuthDAO authDAO;
    private GameService service;
    private String validToken;

    @BeforeEach
    public void setup() {
        gameDAO = new MemoryGameDAO();
        authDAO = new MemoryAuthDAO();
        service = new GameService(gameDAO, authDAO);

        AuthData auth = authDAO.createAuth("TestUser");
        validToken = auth.authToken();
    }

    @Test
    @DisplayName("Create Game Success")
    public void createSuccess() throws DataAccessException {
        var res = service.createGame(validToken, new CreateGameRequest("New Game"));
        Assertions.assertTrue(res.gameID() > 0);
    }

    @Test
    @DisplayName("Create Game Fail (Bad Token)")
    public void createFail() {
        Assertions.assertThrows(DataAccessException.class, () ->
                service.createGame("invalid_token", new CreateGameRequest("Bad Game")));
    }

    @Test
    @DisplayName("List Games Success")
    public void listSuccess() throws DataAccessException {
        gameDAO.createGame("Game 1");
        var res = service.listGames(validToken);
        Assertions.assertEquals(1, res.games().size());
    }

    @Test
    @DisplayName("Join Game Success")
    public void joinSuccess() throws DataAccessException {
        int id = gameDAO.createGame("Match");
        Assertions.assertDoesNotThrow(() ->
                service.joinGame(validToken, new JoinGameRequest("WHITE", id)));
    }

    @Test
    @DisplayName("Join Game Fail (Color Taken)")
    public void joinFail() throws DataAccessException {
        int id = gameDAO.createGame("Match");
        service.joinGame(validToken, new JoinGameRequest("WHITE", id));

        // Attempting to take an occupied spot
        Assertions.assertThrows(DataAccessException.class, () ->
                service.joinGame(validToken, new JoinGameRequest("WHITE", id)));
    }
}