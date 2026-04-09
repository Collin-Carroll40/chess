package client;

import chess.*;
import model.GameData;
import websocket.messages.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

public class ChessClient implements WebSocketFacade.ServerMessageHandler {

    private final ServerFacade facade;
    private final String serverUrl;
    private final Scanner scanner = new Scanner(System.in);
    private String authToken = null;
    private GameData[] lastListedGames = null;

    private WebSocketFacade ws = null;
    private int currentGameID = 0;
    private boolean isWhitePerspective = true;
    private boolean isObserver = false;
    private GameData currentGame = null;

    private enum State { PRELOGIN, POSTLOGIN, GAMEPLAY }
    private State state = State.PRELOGIN;

    public ChessClient(String serverUrl) {
        this.serverUrl = serverUrl;
        facade = new ServerFacade(serverUrl);
    }

    public void run() {
        System.out.println(EscapeSequences.SET_TEXT_BOLD + "♕ Welcome to 240 Chess!" + EscapeSequences.RESET_TEXT_BOLD_FAINT);
        System.out.println("Type 'help' to get started." + "\n");
        while (true) {
            String prompt = switch (state) {
                case PRELOGIN -> "[LOGGED_OUT] >>> ";
                case POSTLOGIN -> "[LOGGED_IN] >>> ";
                case GAMEPLAY -> "[IN_GAME] >>> ";
            };
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }

            try {
                String[] parts = input.split("\\s+");
                String cmd = parts[0].toLowerCase();
                String[] args = Arrays.copyOfRange(parts, 1, parts.length);

                switch (state) {
                    case PRELOGIN -> { if (!handlePrelogin(cmd, args)) { return; } }
                    case POSTLOGIN -> handlePostlogin(cmd, args);
                    case GAMEPLAY -> handleGameplay(cmd, args);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    /** @return false if user wants to quit */
    private boolean handlePrelogin(String cmd, String[] args) throws Exception {
        switch (cmd) {
            case "help" -> printPreloginHelp();
            case "quit" -> { return false; }
            case "login" -> login(args);
            case "register" -> register(args);
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
        return true;
    }

    private void handlePostlogin(String cmd, String[] args) throws Exception {
        switch (cmd) {
            case "help" -> printPostloginHelp();
            case "logout" -> logout();
            case "create" -> createGame(args);
            case "list" -> listGames();
            case "play" -> playGame(args);
            case "observe" -> observeGame(args);
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    private void handleGameplay(String cmd, String[] args) throws Exception {
        switch (cmd) {
            case "help" -> printGameplayHelp();
            case "redraw" -> redrawBoard();
            case "leave" -> leaveGame();
            case "move" -> makeMove(args);
            case "resign" -> resignGame();
            case "highlight" -> highlightMoves(args);
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    private void login(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: login <username> <password>");
            return;
        }
        var result = facade.login(args[0], args[1]);
        authToken = result.authToken();
        state = State.POSTLOGIN;
        System.out.println("Logged in as " + result.username());
    }

    private void register(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: register <username> <password> <email>");
            return;
        }
        var result = facade.register(args[0], args[1], args[2]);
        authToken = result.authToken();
        state = State.POSTLOGIN;
        System.out.println("Registered and logged in as " + result.username());
    }

    private void logout() throws Exception {
        facade.logout(authToken);
        authToken = null;
        state = State.PRELOGIN;
        System.out.println("Logged out.");
    }

    private void createGame(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: create <game name>");
            return;
        }
        String gameName = String.join(" ", args);
        facade.createGame(authToken, gameName);
        System.out.println("Created game: " + gameName);
    }

    private void listGames() throws Exception {
        lastListedGames = facade.listGames(authToken);
        if (lastListedGames.length == 0) {
            System.out.println("No games found.");
            return;
        }
        System.out.printf("%-4s %-20s %-15s %-15s%n", "#", "Game Name", "White", "Black");
        for (int i = 0; i < lastListedGames.length; i++) {
            var g = lastListedGames[i];
            System.out.printf("%-4d %-20s %-15s %-15s%n",
                    i + 1,
                    g.gameName(),
                    g.whiteUsername() != null ? g.whiteUsername() : "-",
                    g.blackUsername() != null ? g.blackUsername() : "-");
        }
    }

    private void playGame(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: play <game #> <WHITE|BLACK>");
            return;
        }
        int index = parseGameNumber(args[0]);
        if (index < 0) {
            return;
        }

        String color = args[1].toUpperCase();
        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            System.out.println("Color must be WHITE or BLACK.");
            return;
        }

        GameData game = lastListedGames[index];
        facade.joinGame(authToken, game.gameID(), color);

        isWhitePerspective = color.equals("WHITE");
        isObserver = false;
        currentGameID = game.gameID();

        ws = new WebSocketFacade(serverUrl, this);
        ws.connect(authToken, currentGameID);

        state = State.GAMEPLAY;
    }

    private void observeGame(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: observe <game #>");
            return;
        }
        int index = parseGameNumber(args[0]);
        if (index < 0) {
            return;
        }

        GameData game = lastListedGames[index];
        isWhitePerspective = true;
        isObserver = true;
        currentGameID = game.gameID();

        ws = new WebSocketFacade(serverUrl, this);
        ws.connect(authToken, currentGameID);

        state = State.GAMEPLAY;
    }

    private void redrawBoard() {
        if (currentGame != null && currentGame.game() != null) {
            BoardDrawer.drawBoard(currentGame.game().getBoard(), isWhitePerspective);
        } else {
            System.out.println("No game data yet.");
        }
    }

    private void leaveGame() throws Exception {
        ws.leave(authToken, currentGameID);
        ws.close();
        ws = null;
        currentGame = null;
        state = State.POSTLOGIN;
        System.out.println("Left the game.");
    }

    private void makeMove(String[] args) throws Exception {
        if (isObserver) {
            System.out.println("Observers cannot make moves.");
            return;
        }
        if (args.length < 2) {
            System.out.println("Usage: move <from> <to> [promotion: Q|R|B|N]");
            System.out.println("Example: move e2 e4");
            return;
        }

        ChessPosition from = parsePosition(args[0]);
        ChessPosition to = parsePosition(args[1]);
        if (from == null || to == null) {
            System.out.println("Invalid position. Use format like e2, a7, etc.");
            return;
        }

        ChessPiece.PieceType promotion = null;
        if (args.length >= 3) {
            promotion = switch (args[2].toUpperCase()) {
                case "Q" -> ChessPiece.PieceType.QUEEN;
                case "R" -> ChessPiece.PieceType.ROOK;
                case "B" -> ChessPiece.PieceType.BISHOP;
                case "N" -> ChessPiece.PieceType.KNIGHT;
                default -> null;
            };
        }

        ChessMove move = new ChessMove(from, to, promotion);
        ws.makeMove(authToken, currentGameID, move);
    }

    private void resignGame() throws Exception {
        if (isObserver) {
            System.out.println("Observers cannot resign.");
            return;
        }
        System.out.print("Are you sure you want to resign? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("yes") || confirm.equals("y")) {
            ws.resign(authToken, currentGameID);
        } else {
            System.out.println("Resign cancelled.");
        }
    }

    private void highlightMoves(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: highlight <position>");
            System.out.println("Example: highlight e2");
            return;
        }
        if (currentGame == null || currentGame.game() == null) {
            System.out.println("No game data yet.");
            return;
        }

        ChessPosition pos = parsePosition(args[0]);
        if (pos == null) {
            System.out.println("Invalid position. Use format like e2, a7, etc.");
            return;
        }

        ChessGame game = currentGame.game();
        Collection<ChessMove> moves = game.validMoves(pos);
        if (moves == null || moves.isEmpty()) {
            System.out.println("No legal moves for that piece.");
            BoardDrawer.drawBoard(game.getBoard(), isWhitePerspective);
            return;
        }

        BoardDrawer.drawBoardWithHighlights(game.getBoard(), isWhitePerspective, pos, moves);
    }

    @Override
    public void onLoadGame(LoadGameMessage message) {
        currentGame = message.getGame();
        System.out.println();
        BoardDrawer.drawBoard(currentGame.game().getBoard(), isWhitePerspective);
        System.out.print(state == State.GAMEPLAY ? "[IN_GAME] >>> " : "");
    }

    @Override
    public void onNotification(NotificationMessage message) {
        System.out.println("\n" + EscapeSequences.SET_TEXT_COLOR_YELLOW + message.getMessage()
                + EscapeSequences.RESET_TEXT_COLOR);
        System.out.print(state == State.GAMEPLAY ? "[IN_GAME] >>> " : "");
    }

    @Override
    public void onError(ErrorMessage message) {
        System.out.println("\n" + EscapeSequences.SET_TEXT_COLOR_RED + message.getErrorMessage()
                + EscapeSequences.RESET_TEXT_COLOR);
        System.out.print(state == State.GAMEPLAY ? "[IN_GAME] >>> " : "");
    }

    private ChessPosition parsePosition(String input) {
        if (input == null || input.length() != 2) {
            return null;
        }
        char colChar = input.charAt(0);
        char rowChar = input.charAt(1);
        if (colChar < 'a' || colChar > 'h' || rowChar < '1' || rowChar > '8') {
            return null;
        }
        int col = colChar - 'a' + 1;
        int row = rowChar - '0';
        return new ChessPosition(row, col);
    }

    private int parseGameNumber(String numStr) {
        if (lastListedGames == null) {
            System.out.println("Please list games first.");
            return -1;
        }
        try {
            int num = Integer.parseInt(numStr);
            if (num < 1 || num > lastListedGames.length) {
                System.out.println("Invalid game number. Use 'list' to see games.");
                return -1;
            }
            return num - 1;
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return -1;
        }
    }

    private void printPreloginHelp() {
        String b = EscapeSequences.SET_TEXT_COLOR_BLUE;
        String r = EscapeSequences.RESET_TEXT_COLOR;
        System.out.println(b + "  help" + r + "     - Show this help message");
        System.out.println(b + "  quit" + r + "     - Exit the program");
        System.out.println(b + "  login" + r + "    - Login: login <username> <password>");
        System.out.println(b + "  register" + r
                + " - Register: register <username> <password> <email>");
    }

    private void printPostloginHelp() {
        String b = EscapeSequences.SET_TEXT_COLOR_BLUE;
        String r = EscapeSequences.RESET_TEXT_COLOR;
        System.out.println(b + "  help" + r + "     - Show this help message");
        System.out.println(b + "  logout" + r + "   - Logout");
        System.out.println(b + "  create" + r
                + "   - Create a game: create <game name>");
        System.out.println(b + "  list" + r + "     - List all games");
        System.out.println(b + "  play" + r
                + "     - Join a game: play <game #> <WHITE|BLACK>");
        System.out.println(b + "  observe" + r
                + "  - Observe a game: observe <game #>");
    }

    private void printGameplayHelp() {
        String b = EscapeSequences.SET_TEXT_COLOR_BLUE;
        String r = EscapeSequences.RESET_TEXT_COLOR;
        System.out.println(b + "  help" + r + "      - Show this help message");
        System.out.println(b + "  redraw" + r + "    - Redraw the chess board");
        System.out.println(b + "  move" + r + "      - Make a move: move <from> <to> [Q|R|B|N]");
        System.out.println(b + "  highlight" + r + " - Show legal moves: highlight <position>");
        System.out.println(b + "  resign" + r + "    - Resign the game");
        System.out.println(b + "  leave" + r + "     - Leave the game");
    }
}