package server;

import chess.*;
import com.google.gson.Gson;
import dataaccess.*;
import model.*;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.*;

import java.io.IOException;

@WebSocket
public class WebSocketHandler {
    private final ConnectionManager connections = new ConnectionManager();
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;
    private final Gson gson = new Gson();

    public WebSocketHandler(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String msg) throws IOException {
        try {
            UserGameCommand command = gson.fromJson(msg, UserGameCommand.class);
            String authToken = command.getAuthToken();
            int gameID = command.getGameID();

            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(session, authToken, gameID);
                case MAKE_MOVE -> {
                    MakeMoveCommand moveCmd = gson.fromJson(msg, MakeMoveCommand.class);
                    handleMakeMove(session, authToken, gameID, moveCmd.getMove());
                }
                case LEAVE -> handleLeave(session, authToken, gameID);
                case RESIGN -> handleResign(session, authToken, gameID);
            }
        } catch (Exception e) {
            System.err.println("WebSocket error: " + e.getMessage());
            e.printStackTrace();
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleConnect(Session session, String authToken, int gameID) throws IOException {
        try {
            AuthData auth = authDAO.getAuth(authToken);
            if (auth == null) {
                sendError(session, "Error: unauthorized");
                return;
            }
            GameData game = gameDAO.getGame(gameID);
            if (game == null) {
                sendError(session, "Error: bad request - game not found");
                return;
            }

            connections.add(gameID, authToken, auth.username(), session);
            connections.sendToUser(authToken, gameID, new LoadGameMessage(game));

            String role;
            if (auth.username().equals(game.whiteUsername())) {
                role = "white";
            } else if (auth.username().equals(game.blackUsername())) {
                role = "black";
            } else {
                role = "an observer";
            }

            connections.broadcast(gameID, authToken,
                    new NotificationMessage(auth.username() + " joined the game as " + role));
        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleMakeMove(Session session, String authToken, int gameID, ChessMove move) throws IOException {
        try {
            AuthData auth = authDAO.getAuth(authToken);
            if (auth == null) {
                sendError(session, "Error: unauthorized");
                return;
            }
            GameData game = gameDAO.getGame(gameID);
            if (game == null) {
                sendError(session, "Error: bad request");
                return;
            }

            ChessGame chessGame = game.game();
            if (chessGame.isOver()) {
                sendError(session, "Error: game is already over");
                return;
            }

            String username = auth.username();
            ChessGame.TeamColor playerColor = getPlayerColor(username, game);

            if (playerColor == null) {
                sendError(session, "Error: observers cannot make moves");
                return;
            }
            if (chessGame.getTeamTurn() != playerColor) {
                sendError(session, "Error: it is not your turn");
                return;
            }

            chessGame.makeMove(move);

            GameData updated = new GameData(game.gameID(), game.whiteUsername(),
                    game.blackUsername(), game.gameName(), chessGame);
            gameDAO.updateGame(updated);

            connections.broadcast(gameID, null, new LoadGameMessage(updated));
            connections.broadcast(gameID, authToken,
                    new NotificationMessage(username + " made a move: " + move));

            ChessGame.TeamColor opponent = (playerColor == ChessGame.TeamColor.WHITE)
                    ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
            checkGameStatus(gameID, game, chessGame, opponent);

        } catch (InvalidMoveException e) {
            sendError(session, "Error: invalid move - " + e.getMessage());
        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleLeave(Session session, String authToken, int gameID) throws IOException {
        try {
            AuthData auth = authDAO.getAuth(authToken);
            if (auth == null) {
                sendError(session, "Error: unauthorized");
                return;
            }
            GameData game = gameDAO.getGame(gameID);
            if (game == null) {
                sendError(session, "Error: bad request");
                return;
            }

            String white = game.whiteUsername();
            String black = game.blackUsername();
            if (auth.username().equals(white)) {
                white = null;
            } else if (auth.username().equals(black)) {
                black = null;
            }
            gameDAO.updateGame(new GameData(game.gameID(), white, black, game.gameName(), game.game()));

            connections.remove(gameID, authToken);

            connections.broadcast(gameID, authToken,
                    new NotificationMessage(auth.username() + " left the game"));
        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleResign(Session session, String authToken, int gameID) throws IOException {
        try {
            AuthData auth = authDAO.getAuth(authToken);
            if (auth == null) {
                sendError(session, "Error: unauthorized");
                return;
            }
            GameData game = gameDAO.getGame(gameID);
            if (game == null) {
                sendError(session, "Error: bad request");
                return;
            }

            ChessGame chessGame = game.game();
            if (chessGame.isOver()) {
                sendError(session, "Error: game is already over");
                return;
            }

            String username = auth.username();
            if (!username.equals(game.whiteUsername()) && !username.equals(game.blackUsername())) {
                sendError(session, "Error: observers cannot resign");
                return;
            }

            chessGame.setOver(true);
            gameDAO.updateGame(new GameData(game.gameID(), game.whiteUsername(),
                    game.blackUsername(), game.gameName(), chessGame));

            connections.broadcast(gameID, null,
                    new NotificationMessage(username + " resigned the game"));
        } catch (DataAccessException e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private ChessGame.TeamColor getPlayerColor(String username, GameData game) {
        if (username.equals(game.whiteUsername())) {
            return ChessGame.TeamColor.WHITE;
        } else if (username.equals(game.blackUsername())) {
            return ChessGame.TeamColor.BLACK;
        }
        return null;
    }

    private void checkGameStatus(int gameID, GameData game, ChessGame chessGame,
                                 ChessGame.TeamColor opponent) throws IOException, DataAccessException {
        if (chessGame.isInCheckmate(opponent)) {
            endGame(game, chessGame, gameID, opponent + " is in checkmate");
        } else if (chessGame.isInStalemate(opponent)) {
            endGame(game, chessGame, gameID, "Game ended in stalemate");
        } else if (chessGame.isInCheck(opponent)) {
            connections.broadcast(gameID, null,
                    new NotificationMessage(opponent + " is in check"));
        }
    }

    private void endGame(GameData game, ChessGame chessGame, int gameID,
                         String message) throws IOException, DataAccessException {
        chessGame.setOver(true);
        gameDAO.updateGame(new GameData(game.gameID(), game.whiteUsername(),
                game.blackUsername(), game.gameName(), chessGame));
        connections.broadcast(gameID, null, new NotificationMessage(message));
    }

    private void sendError(Session session, String message) throws IOException {
        if (session.isOpen()) {
            session.getRemote().sendString(gson.toJson(new ErrorMessage(message)));
        }
    }
}