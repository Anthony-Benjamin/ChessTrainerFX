package application.chesstrainerfx.model;

import application.chesstrainerfx.utils.*;
import application.chesstrainerfx.view.BoardChangeListener;

import java.util.ArrayList;
import java.util.List;

public class BoardModel {

    // 8x8 grid is de meest logische datastructuur voor een schaakbord
    private final SquareModel[][] squares = new SquareModel[8][8];

    // listeners blijven als lijst (variabel aantal)
    private final List<BoardChangeListener> listeners = new ArrayList<>();

    // en-passant tracking
    private Position lastDoubleStepPawnPosition = null;

    public BoardModel() {
        initializeBoard();
    }

    // --------------------------------------------------------------------
    // Board init
    // --------------------------------------------------------------------
    // maakt alle velden en hun positie (1x)
    public void initializeBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col] = new SquareModel(new Position(row, col));
            }
        }
    }

    public void clearBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col].setPiece(null);
            }
        }
        notifyListeners();
    }

    // --------------------------------------------------------------------
    // FEN init
    // --------------------------------------------------------------------
    public void initializeFromFEN(String fen) {
        if (fen == null || fen.isEmpty()) {
            fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        }

        clearBoard();
        lastDoubleStepPawnPosition = null;

        String[] fenParts = fen.split("\\s+");
        String placement = fenParts[0];
        String[] ranks = placement.split("/");

        int row = 0;
        for (String rank : ranks) {
            int col = 0;
            for (char ch : rank.toCharArray()) {
                if (Character.isDigit(ch)) {
                    col += Character.digit(ch, 10);
                } else {
                    squares[row][col].setPiece(pieceModelformFENChar(ch));
                    col++;
                }
            }
            row++;
        }

        notifyListeners();

        if (fenParts.length > 1) {
            boolean whiteToMove = fenParts[1].equals("w");
            notifyListenersTurnChanged(whiteToMove);
        }
    }

    public PieceModel pieceModelformFENChar(char c) {
        PieceColor color;
        if (Character.isLowerCase(c)) {
            color = PieceColor.BLACK;
        } else if (Character.isUpperCase(c)) {
            color = PieceColor.WHITE;
        } else {
            return null;
        }

        PieceType type;
        switch (Character.toLowerCase(c)) {
            case 'k' -> type = PieceType.KING;
            case 'q' -> type = PieceType.QUEEN;
            case 'r' -> type = PieceType.ROOK;
            case 'b' -> type = PieceType.BISHOP;
            case 'n' -> type = PieceType.KNIGHT;
            case 'p' -> type = PieceType.PAWN;
            default -> {
                return null;
            }
        }
        return new PieceModel(type, color);
    }

    // --------------------------------------------------------------------
    // Access
    // --------------------------------------------------------------------
    public SquareModel getSquare(Position pos) {
        if (pos == null) return null;

        int row = pos.getRow();
        int col = pos.getColumn();

        if (row < 0 || row >= 8 || col < 0 || col >= 8) {
            return null;
        }
        return squares[row][col];
    }

    // --------------------------------------------------------------------
    // Moves
    // --------------------------------------------------------------------
    public void movePiece(Position from, Position to) {
        if (from == null || to == null) return;

        SquareModel source = getSquare(from);
        SquareModel target = getSquare(to);
        if (source == null || target == null) return;

        PieceModel piece = source.getPiece();
        if (piece == null) return;

        source.removePiece();
        target.setPiece(piece);

        // handig voor rokade/pawn logica later
        piece.setHasMoved(true);

        notifyListeners();
    }

    // --------------------------------------------------------------------
    // En-passant
    // --------------------------------------------------------------------
    public void setLastDoubleStepPawnPosition(Position pos) {
        this.lastDoubleStepPawnPosition = pos;
    }

    public Position getLastDoubleStepPawnPosition() {
        return lastDoubleStepPawnPosition;
    }

    // --------------------------------------------------------------------
    // Listeners
    // --------------------------------------------------------------------
    public void addListener(BoardChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (BoardChangeListener listener : listeners) {
            listener.onBoardUpdated();
        }
    }

    public void notifyListenersTurnChanged(boolean whiteToMove) {
        for (BoardChangeListener l : listeners) {
            l.onTurnChanged(whiteToMove);
        }
    }

    public void notifyCheck(boolean inCheck) {
        for (BoardChangeListener l : listeners) {
            l.onCheck(inCheck);
        }
    }

    // --------------------------------------------------------------------
    // FEN export
    // --------------------------------------------------------------------
    public String exportToFEN(boolean whiteToMove) {
        return exportToFEN(whiteToMove, "-");
    }

    // overload met expliciete rokaderechten ("KQkq", "Kq", "-", ...)
    public String exportToFEN(boolean whiteToMove, String castling) {
        StringBuilder fen = new StringBuilder();

        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;
            for (int col = 0; col < 8; col++) {
                PieceModel piece = squares[row][col].getPiece();
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(piece.getFENChar());
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (row < 7) fen.append('/');
        }

        fen.append(whiteToMove ? " w" : " b");
        fen.append(' ').append(castling == null || castling.isBlank() ? "-" : castling);
        fen.append(" - 0 1"); // en-passant, halfmove, fullmove (later uitbreiden)
        return fen.toString();
    }
}