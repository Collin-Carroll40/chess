package chess;

import java.util.Collection;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {


    private TeamColor teamTurn;
    private ChessBoard board;


    public ChessGame() {
        this.teamTurn = TeamColor.WHITE;
        this.board = new ChessBoard();
        this.board.resetBoard();
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);

        if (piece == null) {
            return null;
        }

        Collection<ChessMove> validMoves = new java.util.ArrayList<>();
        Collection<ChessMove> possibleMoves = piece.pieceMoves(board, startPosition);

        // Simulation Loop I dont know if this is the fastest or will slow it down too much?
        for (ChessMove move : possibleMoves) {
            // Record the state before moving
            ChessPiece pieceAtDest = board.getPiece(move.getEndPosition());

            // Do the move (temporarily)
            board.addPiece(move.getEndPosition(), piece);
            board.addPiece(move.getStartPosition(), null);

            // Check if King is safe
            if (!isInCheck(piece.getTeamColor())) {
                validMoves.add(move);
            }

            // Undo the move (restore state)
            board.addPiece(move.getStartPosition(), piece);
            board.addPiece(move.getEndPosition(), pieceAtDest);
        }

        return validMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPiece piece = board.getPiece(move.getStartPosition());

        // piece and color
        if (piece == null) {
            throw new InvalidMoveException("No piece at start position");
        }
        if (piece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("Not your turn");
        }

        // Check if the move is in the list of valid moves
        Collection<ChessMove> valid = validMoves(move.getStartPosition());
        if (valid == null || !valid.contains(move)) {
            throw new InvalidMoveException("Invalid move");
        }

        // 3. Execute the Move
        // Check for promotion
        if (move.getPromotionPiece() != null) {
            ChessPiece promotedPiece = new ChessPiece(teamTurn, move.getPromotionPiece());
            board.addPiece(move.getEndPosition(), promotedPiece);
        } else {
            // Normal move
            board.addPiece(move.getEndPosition(), piece);
        }

        // Remove from old spot
        board.addPiece(move.getStartPosition(), null);

        // 4. Switch Turns
        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }


    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = findKing(teamColor);

        if (kingPosition == null) {
            return false;
        }

        for (int row = 1 ; row <= 8; row++){
            for (int col = 1; col <= 8; col++){

                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);

                if (piece != null && piece.getTeamColor() != teamColor) {

                    // Get all moves this enemy can make
                    Collection<ChessMove> enemyMoves = piece.pieceMoves(board, pos);

                    // Check if any of those moves land on the King
                    for (ChessMove move : enemyMoves) {
                        if (move.getEndPosition().equals(kingPosition)) {
                            return true; // The King is under attack!
                        }
                    }
                }

            }
        }
        return false;
    }

    private ChessPosition findKing(TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);

                // Check if this is correct color
                if (piece != null &&
                        piece.getPieceType() == ChessPiece.PieceType.KING &&
                        piece.getTeamColor() == teamColor) {
                    return pos;
                }
            }
        }
        return null; // shouldnt happen
    }


    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        return isInCheck(teamColor) && !hasLegalMoves(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        return !isInCheck(teamColor) && !hasLegalMoves(teamColor);
    }

    // helper haslegalmove this is important for chack and stale

    private boolean hasLegalMoves(TeamColor teamColor) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPosition pos = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(pos);

                // If it's my right color, check if it has any valid moves
                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(pos);
                    if (moves != null && !moves.isEmpty()) {
                        return true; // We found at least one move!
                    }
                }
            }
        }
        return false; // No moves found anywhere shouldn't happen
    }


// this helper helped haha
    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return teamTurn == chessGame.teamTurn && Objects.equals(board, chessGame.board);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamTurn, board);
    }
}
