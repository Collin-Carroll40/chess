package client;

import chess.ChessBoard;
import chess.ChessGame;
import model.GameData;

import java.util.Arrays;
import java.util.Scanner;

public class ChessClient {

    private final ServerFacade facade;
    private final Scanner scanner = new Scanner(System.in);
    private String authToken = null;
    private GameData[] lastListedGames = null;

    private enum State { PRELOGIN, POSTLOGIN }
    private State state = State.PRELOGIN;

    public ChessClient(String serverUrl) {
        facade = new ServerFacade(serverUrl);
    }

    public void run() {
        System.out.println(EscapeSequences.SET_TEXT_BOLD + "♕ Welcome to 240 Chess!" + EscapeSequences.RESET_TEXT_BOLD_FAINT);
        System.out.println("Type 'help' to get started." + "\n");
        while (true) {
            System.out.print(state == State.PRELOGIN ? "[LOGGED_OUT] >>> " : "[LOGGED_IN] >>> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }

            try {
                String[] parts = input.split("\\s+");
                String cmd = parts[0].toLowerCase();
                String[] args = Arrays.copyOfRange(parts, 1, parts.length);

                if (state == State.PRELOGIN) {
                    if (!handlePrelogin(cmd, args)) {
                        return;
                    }
                } else {
                    handlePostlogin(cmd, args);
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
        System.out.println("Joined game as " + color);

        ChessGame chessGame = new ChessGame();
        chessGame.getBoard().resetBoard();
        BoardDrawer.drawBoard(chessGame.getBoard(), color.equals("WHITE"));
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

        System.out.println("Observing game: " + lastListedGames[index].gameName());

        ChessGame chessGame = new ChessGame();
        chessGame.getBoard().resetBoard();
        BoardDrawer.drawBoard(chessGame.getBoard(), true);
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
}