package client;

import chess.*;

import static client.EscapeSequences.*;

public class BoardDrawer {

    private static final String[] COL_LABELS = {"a", "b", "c", "d", "e", "f", "g", "h"};

    public static void drawBoard(ChessBoard board, boolean whitePerspective) {
        System.out.println();

        int rowStart = whitePerspective ? 8 : 1;
        int rowEnd = whitePerspective ? 1 : 8;
        int rowStep = whitePerspective ? -1 : 1;

        int colStart = whitePerspective ? 1 : 8;
        int colEnd = whitePerspective ? 8 : 1;
        int colStep = whitePerspective ? 1 : -1;

        // Column headers
        System.out.print("   ");
        for (int c = colStart; c != colEnd + colStep; c += colStep) {
            System.out.print(" " + COL_LABELS[c - 1] + " ");
        }
        System.out.println();

        for (int r = rowStart; r != rowEnd + rowStep; r += rowStep) {
            System.out.print(" " + r + " ");
            for (int c = colStart; c != colEnd + colStep; c += colStep) {
                boolean lightSquare = (r + c) % 2 == 1;
                String bg = lightSquare ? SET_BG_COLOR_LIGHT_GREY : SET_BG_COLOR_DARK_GREEN;

                ChessPiece piece = board.getPiece(new ChessPosition(r, c));
                String pieceStr = getPieceString(piece);

                System.out.print(bg + pieceStr + RESET_BG_COLOR);
            }
            System.out.println(" " + r);
        }

        // Column footers
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