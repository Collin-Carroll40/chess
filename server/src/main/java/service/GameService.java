package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.AuthData;
import model.GameData;
import request.CreateGameRequest;
import request.JoinGameRequest;
import result.CreateGameResult;
import result.ListGamesResult;

public class GameService {
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    public ListGamesResult listGames(String authToken) throws DataAccessException {
        if (authDAO.getAuth(authToken) == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        return new ListGamesResult(gameDAO.listGames());
    }

    public CreateGameResult createGame(String authToken, CreateGameRequest req) throws DataAccessException {
        if (authDAO.getAuth(authToken) == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        if (req.gameName() == null || req.gameName().isEmpty()) {
            throw new DataAccessException("Error: bad request");
        }

        int gameID = gameDAO.createGame(req.gameName());
        return new CreateGameResult(gameID);
    }

    public void joinGame(String authToken, JoinGameRequest req) throws DataAccessException {
        AuthData auth = authDAO.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("Error: unauthorized");
        }

        GameData game = gameDAO.getGame(req.gameID());
        if (game == null) {
            throw new DataAccessException("Error: bad request");
        }

        String whiteUser = game.whiteUsername();
        String blackUser = game.blackUsername();

        // Check color the user requested and avalibility
        if ("WHITE".equals(req.playerColor())) {
            if (whiteUser != null) {
                throw new DataAccessException("Error: already taken");
            }
            whiteUser = auth.username();
        } else if ("BLACK".equals(req.playerColor())) {
            if (blackUser != null) {
                throw new DataAccessException("Error: already taken");
            }
            blackUser = auth.username();
        } else {
            // non white black
            throw new DataAccessException("Error: bad request incorrect color");
        }
        // If color is null, they are joining as an observer

        // Save
        gameDAO.updateGame(new GameData(game.gameID(), whiteUser, blackUser, game.gameName(), game.game()));
    }
}