package dataaccess;

import model.GameData;
import chess.ChessGame;
import java.util.HashMap;
import java.util.Collection;

public class MemoryGameDAO implements GameDAO {
    private final HashMap<Integer, GameData> games = new HashMap<>();
    private int nextId = 1;

    @Override
    public int createGame(String gameName) {
        int gameID = nextId++;
        // Create a new game with no players yet (null for white and black)
        GameData game = new GameData(gameID, null, null, gameName, new ChessGame());
        games.put(gameID, game);
        return gameID;
    }

    @Override
    public GameData getGame(int gameID) {
        return games.get(gameID);
    }

    @Override
    public Collection<GameData> listGames() {
        return games.values();
    }

    @Override
    public void updateGame(GameData game) {
        games.put(game.gameID(), game);
    }

    @Override
    public void clear() {
        games.clear();
        nextId = 1; // Reset the ID counter when the database is cleared
    }
}