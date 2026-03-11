package dataaccess;

import chess.ChessGame;
import model.GameData;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class SqlGameDAOTest {

    private SqlGameDAO gameDAO;

    @BeforeEach
    void setUp() throws DataAccessException {
        gameDAO = new SqlGameDAO();
        gameDAO.clear();
    }

    @Test
    @DisplayName("Positive: Create Game")
    void createGamePositive() throws DataAccessException {
        int gameId = gameDAO.createGame("My Chess Game");
        assertTrue(gameId > 0);
    }

    @Test
    @DisplayName("Negative: Create Game Null Name")
    void createGameNegative() {
        // SQL requires a game name, so null should throw an exception
        assertThrows(DataAccessException.class, () -> gameDAO.createGame(null));
    }

    @Test
    @DisplayName("Positive: Get Game")
    void getGamePositive() throws DataAccessException {
        int gameId = gameDAO.createGame("Test Game");
        GameData game = gameDAO.getGame(gameId);
        assertNotNull(game);
        assertEquals("Test Game", game.gameName());
    }

    @Test
    @DisplayName("Negative: Get Fake Game")
    void getGameNegative() throws DataAccessException {
        GameData game = gameDAO.getGame(9999);
        assertNull(game);
    }

    @Test
    @DisplayName("Positive: List Games")
    void listGamesPositive() throws DataAccessException {
        gameDAO.createGame("Game 1");
        gameDAO.createGame("Game 2");
        Collection<GameData> games = gameDAO.listGames();
        assertEquals(2, games.size());
    }

    @Test
    @DisplayName("Negative: List Games Empty")
    void listGamesNegative() throws DataAccessException {
        Collection<GameData> games = gameDAO.listGames();
        assertTrue(games.isEmpty());
    }

    @Test
    @DisplayName("Positive: Update Game")
    void updateGamePositive() throws DataAccessException {
        int gameId = gameDAO.createGame("Update Me");
        GameData original = gameDAO.getGame(gameId);

        // Add a white player
        GameData updated = new GameData(gameId, "Collin", null, "Update Me", original.game());
        gameDAO.updateGame(updated);

        GameData retrieved = gameDAO.getGame(gameId);
        assertEquals("Collin", retrieved.whiteUsername());
    }

    @Test
    @DisplayName("Negative: Update Fake Game")
    void updateGameNegative() {
        // Trying to update a game with a null game object should throw an error
        GameData badUpdate = new GameData(999, "Collin", null, "Bad Game", null);
        assertThrows(DataAccessException.class, () -> gameDAO.updateGame(badUpdate));
    }

    @Test
    @DisplayName("Positive: Clear Games")
    void clearGames() throws DataAccessException {
        gameDAO.createGame("Game 1");
        gameDAO.clear();
        Collection<GameData> games = gameDAO.listGames();
        assertTrue(games.isEmpty());
    }
}