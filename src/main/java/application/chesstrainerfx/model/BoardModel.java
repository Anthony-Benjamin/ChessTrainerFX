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

    // --------------------------------------------------------------------
    // Snapshot helper (voor Undo/Redo)
    // --------------------------------------------------------------------
    public static class BoardSnapshot {
        private final PieceModel[][] pieces; // 8x8
        private final Position lastDoubleStepPawnPosition;

        private BoardSnapshot(PieceModel[][] pieces, Position lastDoubleStepPawnPosition) {
            this.pieces = pieces;
            this.lastDoubleStepPawnPosition = lastDoubleStepPawnPosition;
        }
    }

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

    private void clearBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col].setPiece(null);
            }
        }
    }

    // --------------------------------------------------------------------
    // FEN init
    // --------------------------------------------------------------------
    public void initializeFromFEN(String fen) {
        if (fen == null || fen.isEmpty()) {
            fen = "rn1qK1nR/pppppppp/3bbbb1/pppppppp/8/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1";
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

    // blijft dezelfde naam/signature houden om niks te breken
    public PieceModel pieceModelformFENChar(char c) {
        PieceColor color;
        if (Character.isLowerCase(c)) {
            color = PieceColor.BLACK;
        } else if (Character.isUpperCase(c)) {
            color = PieceColor.WHITE;
        } else {
            System.out.println("geen geldige letter");
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
                System.out.println("geen geldige letter");
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

    // Optioneel/legacy: als je ooit nog een flat list nodig hebt.
    // In jouw huidige codebase lijkt dit niet gebruikt te worden.
    public List<SquareModel> getSquares() {
        List<SquareModel> flat = new ArrayList<>(64);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                flat.add(squares[r][c]);
            }
        }
        return flat;
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

    // --------------------------------------------------------------------
    // FEN export
    // --------------------------------------------------------------------
    public String exportToFEN() {
        // jij gebruikte altijd " w - - 0 1" als basis metadata
        return exportToFEN(true);
    }

    // extra overload (handig voor jou/Controller)
    public String exportToFEN(boolean whiteToMove) {
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
        fen.append(" - - 0 1"); // basis metadata (later uitbreiden)
        return fen.toString();
    }

    // --------------------------------------------------------------------
    // Snapshots (Undo/Redo)
    // --------------------------------------------------------------------
    public BoardSnapshot createSnapshot() {
        PieceModel[][] copy = new PieceModel[8][8];

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                PieceModel p = squares[r][c].getPiece();
                if (p == null) {
                    copy[r][c] = null;
                } else {
                    PieceModel p2 = new PieceModel(p.getType(), p.getColor());
                    p2.setHasMoved(p.hasMoved());
                    copy[r][c] = p2;
                }
            }
        }

        return new BoardSnapshot(copy, lastDoubleStepPawnPosition);
    }

    public void restoreSnapshot(BoardSnapshot snap) {
        if (snap == null) return;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                squares[r][c].setPiece(snap.pieces[r][c]);
            }
        }
        this.lastDoubleStepPawnPosition = snap.lastDoubleStepPawnPosition;
        notifyListeners();
    }
}