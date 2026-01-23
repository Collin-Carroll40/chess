package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
//    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
//        ChessPiece piece = board.getPiece(myPosition);
//        if (piece.getPieceType() == PieceType.BISHOP) {
//            return List.of(new ChessMove(new ChessPosition(5,2), new ChessPosition(1,8),null));
//        }
//        return List.of();


    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
            Collection<ChessMove> moves = new ArrayList<>();

            //Bishop
            if (type == PieceType.BISHOP) {
                int[][] diagonals = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
                moves.addAll(getSlidingMoves(board, myPosition, diagonals));
            }

            // Rook
            if (type == PieceType.ROOK) {
                int[][] straight = {{1,0},{-1,0},{0,1},{0,-1}};
                moves.addAll(getSlidingMoves(board, myPosition, straight));
            }

            //Queen
            if(type == PieceType.QUEEN) {
                int[][] allDirs = {{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}};
                moves.addAll(getSlidingMoves(board,myPosition, allDirs));
            }


            // king
            if (type == PieceType.KING) {
                int[][] dirs = {{1,1},{1,0},{1,-1},{0,-1},{0,1},{-1,-1},{-1,-0},{-1,1}};
                for (int[] d : dirs) {
                    int r = myPosition.getRow() + d[0];
                    int c = myPosition.getColumn() + d[1];
                    if (r >= 1 && r <= 8 && c>= 1 && c <= 8) {
                        ChessPosition p = new ChessPosition(r,c);
                        if (board.getPiece(p)==null || board.getPiece(p).getTeamColor() !=pieceColor){
                            moves.add(new ChessMove(myPosition, p, null));
                        }
                    }
                }
            }

            // exact same as king but knight
            if (type == PieceType.KNIGHT) {
                int[][] dirs = {{2,1},{2,-1},{1,2},{1,-2},{-2,1},{-2,-1},{-1,2},{-1,-2}};
                for (int[] d : dirs) {
                    int r = myPosition.getRow() + d[0];
                    int c = myPosition.getColumn() + d[1];
                    if (r >= 1 && r <= 8 && c>= 1 && c <= 8) {
                        ChessPosition p = new ChessPosition(r,c);
                        if (board.getPiece(p)==null || board.getPiece(p).getTeamColor() !=pieceColor){
                            moves.add(new ChessMove(myPosition, p, null));
                        }
                    }
                }
            }

            //Pawn
            if (type == PieceType.PAWN) {
                int direction = (pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
                int startRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
                int promotionRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 8 : 1;

                int r = myPosition.getRow();
                int c = myPosition.getColumn();

                // Forward
                if (r + direction >= 1 && r + direction <= 8) {
                    ChessPosition nextPos = new ChessPosition(r + direction, c);
                    if (board.getPiece(nextPos) == null) {
                        addPawnMove(moves, myPosition, nextPos, (r + direction) == promotionRow);

                        // B. Double Move at start
                        if (r == startRow) {
                            ChessPosition doublePos = new ChessPosition(r + (direction * 2), c);
                            if (board.getPiece(doublePos) == null) {
                                moves.add(new ChessMove(myPosition, doublePos, null));
                            }
                        }
                    }
                }

                // side capture
                int[] caps = {c - 1, c + 1};
                for (int capCol : caps) {
                    if (capCol >= 1 && capCol <= 8 && r + direction >= 1 && r + direction <= 8) {
                        ChessPosition capPos = new ChessPosition(r + direction, capCol);
                        ChessPiece target = board.getPiece(capPos);
                        if (target != null && target.getTeamColor() != pieceColor) {
                            addPawnMove(moves, myPosition, capPos, (r + direction) == promotionRow);
                        }
                    }
                }
            }


           return moves;
    }
    // I will implement Sliding Moves here
    private Collection<ChessMove> getSlidingMoves(ChessBoard board, ChessPosition myPosition, int[][] directions) {
        Collection<ChessMove> moves = new ArrayList<>();
        for(int[] direction : directions) {
            int r = myPosition.getRow();
            int c = myPosition.getColumn();
            while (true) {
                r += direction[0];
                c +=direction[1];
                if(r < 1 || r > 8 || c < 1 || c > 8) break; //off board
                ChessPosition newPos = new ChessPosition(r,c);
                ChessPiece piece = board.getPiece(newPos);

                if(piece == null) {
                    moves.add(new ChessMove(myPosition, newPos,null));
                } else {
                    if (piece.getTeamColor()!=this.pieceColor) {
                        moves.add(new ChessMove(myPosition, newPos, null)); //capture
                    }
                    break;
                }


            }
        }
        return moves;
    }

// Pawn Promotion
    private void addPawnMove(Collection<ChessMove> moves, ChessPosition start, ChessPosition end, boolean promote) {
        if (promote) {
            moves.add(new ChessMove(start, end, PieceType.QUEEN));
            moves.add(new ChessMove(start, end, PieceType.BISHOP));
            moves.add(new ChessMove(start, end, PieceType.ROOK));
            moves.add(new ChessMove(start, end, PieceType.KNIGHT));
        } else {
            moves.add(new ChessMove(start, end, null));
        }
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ChessPiece piece = (ChessPiece) o;
        return pieceColor == piece.pieceColor && type == piece.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }
}
