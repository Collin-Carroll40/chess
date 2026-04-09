package client;

import chess.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static client.EscapeSequences.*;

public class BoardDrawer {

    private static final String[] COL_LABELS = {"a", "b", "c", "d", "e", "f", "g", "h"};

    public static void drawBoard(ChessBoard board, boolean whitePerspective) {
        drawBoardInternal(board, whitePerspective, null, null);
    }

    public static void drawBoardWithHighlights(ChessBoard board, boolean whitePerspective,
                                               ChessPosition selectedPos, Collection<ChessMove> moves) {
        Set<ChessPosition> highlightSquares = new HashSet<>();
        for (ChessMove move : moves) {
            highlightSquares.add(move.getEndPosition());
        }
        drawBoardInternal(board, whitePerspective, selectedPos, highlightSquares);
    }

    private static void drawBoardInternal(ChessBoard board, boolean whitePerspective,
                                          ChessPosition selectedPos, Set<ChessPosition> highlights) {
        System.out.println();

        int rowStart = whitePerspective ? 8 : 1;
        int rowEnd = whitePerspective ? 1 : 8;
        int rowStep = whitePerspective ? -1 : 1;

        int colStart = whitePerspective ? 1 : 8;
        int colEnd = whitePerspective ? 8 : 1;
        int colStep = whitePerspective ? 1 : -1;

        System.out.print("   ");
        for (int c = colStart; c != colEnd + colStep; c += colStep) {
            System.out.print(" " + COL_LABELS[c - 1] + " ");
        }
        System.out.println();

        for (int r = rowStart; r != rowEnd + rowStep; r += rowStep) {
            System.out.print(" " + r + " ");
            for (int c = colStart; c != colEnd + colStep; c += colStep) {
                ChessPosition pos = new ChessPosition(r, c);
                boolean lightSquare = (r + c) % 2 == 1;

                String bg;
                if (selectedPos != null && selectedPos.equals(pos)) {
                    bg = SET_BG_COLOR_YELLOW;
                } else if (highlights != null && highlights.contains(pos)) {
                    bg = lightSquare ? SET_BG_COLOR_GREEN : SET_BG_COLOR_DARK_GREEN;
                } else {
                    bg = lightSquare ? SET_BG_COLOR_LIGHT_GREY : SET_BG_COLOR_DARK_GREEN;
                }

                ChessPiece piece = board.getPiece(pos);
                String pieceStr = getPieceString(piece);

                System.out.print(bg + pieceStr + RESET_BG_COLOR);
            }
            System.out.println(" " + r);
        }

        System.out.print("   ");
        for (int c = colStart; c != colEnd + colStep; c += colStep) {
            System.out.print(" " + COL_LABELS[c - 1] + " ");
        }
        System.out.println();
        System.out.println();
    }

    private static String getPieceString(ChessPiece piece) {
        if (piece == null) {
            return "   ";
        }
        String color = piece.getTeamColor() == ChessGame.TeamColor.WHITE
                ? SET_TEXT_COLOR_RED : SET_TEXT_COLOR_BLUE;
        String letter = switch (piece.getPieceType()) {
            case KING -> " K ";
            case QUEEN -> " Q ";
            case BISHOP -> " B ";
            case KNIGHT -> " N ";
            case ROOK -> " R ";
            case PAWN -> " P ";
        };
        return color + letter + RESET_TEXT_COLOR;
    }
}