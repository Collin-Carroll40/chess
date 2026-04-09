package client;

import chess.ChessMove;
import com.google.gson.Gson;
import jakarta.websocket.*;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.*;

import java.io.IOException;
import java.net.URI;

public class WebSocketFacade extends Endpoint {
    private Session session;
    private final ServerMessageHandler messageHandler;
    private final Gson gson = new Gson();

    public interface ServerMessageHandler {
        void onLoadGame(LoadGameMessage message);
        void onNotification(NotificationMessage message);
        void onError(ErrorMessage message);
    }

    public WebSocketFacade(String serverUrl, ServerMessageHandler handler) throws Exception {
        this.messageHandler = handler;
        String wsUrl = serverUrl.replace("http", "ws") + "/ws";

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        this.session = container.connectToServer(this, new URI(wsUrl));

        this.session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String msg) {
                ServerMessage base = gson.fromJson(msg, ServerMessage.class);
                switch (base.getServerMessageType()) {
                    case LOAD_GAME -> messageHandler.onLoadGame(gson.fromJson(msg, LoadGameMessage.class));
                    case NOTIFICATION -> messageHandler.onNotification(gson.fromJson(msg, NotificationMessage.class));
                    case ERROR -> messageHandler.onError(gson.fromJson(msg, ErrorMessage.class));
                }
            }
        });
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
    }

    public void connect(String authToken, int gameID) throws IOException {
        var command = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
        session.getBasicRemote().sendText(gson.toJson(command));
    }

    public void makeMove(String authToken, int gameID, ChessMove move) throws IOException {
        var command = new MakeMoveCommand(authToken, gameID, move);
        session.getBasicRemote().sendText(gson.toJson(command));
    }

    public void leave(String authToken, int gameID) throws IOException {
        var command = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameID);
        session.getBasicRemote().sendText(gson.toJson(command));
    }

    public void resign(String authToken, int gameID) throws IOException {
        var command = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID);
        session.getBasicRemote().sendText(gson.toJson(command));
    }

    public void close() throws IOException {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
}