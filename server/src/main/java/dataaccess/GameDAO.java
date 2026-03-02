package dataaccess;

import model.GameData;
import java.util.Collection;

public interface GameDAO {
    int createGame(String gameName);
    GameData getGame(int gameID);
    Collection<GameData> listGames();
    void updateGame(GameData game);
    void clear();
}