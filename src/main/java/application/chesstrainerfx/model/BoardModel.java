package application.chesstrainerfx.model;

import application.chesstrainerfx.utils.*;
import application.chesstrainerfx.view.BoardChangeListener;

import java.util.ArrayList;
import java.util.List;

public class BoardModel {

    // --------------------------------------------------------------------
    // Fields
    // --------------------------------------------------------------------
    private final List<SquareModel> squares = new ArrayList<>(64);
    private final List<BoardChangeListener> listeners = new ArrayList<>();

    // En passant tracking
    private Position lastDoubleStepPawnPosition = null;

    // --------------------------------------------------------------------
    // Snapshot type
    // --------------------------------------------------------------------
    public static class BoardSnapshot {
        private final PieceModel[][] pieces; // 8x8
        private final Position lastDoubleStepPawnPosition;

        private BoardSnapshot(PieceModel[][] pieces, Position lastDoubleStepPawnPosition) {
            this.pieces = pieces;
            this.lastDoubleStepPawnPosition = lastDoubleStepPawnPosition;
        }
    }

    // --------------------------------------------------------------------
    // Constructor
    // --------------------------------------------------------------------
    public BoardModel() {
        initializeBoard();
    }

    // --------------------------------------------------------------------
    // Board initialisation
    // --------------------------------------------------------------------
    /** Maakt alle 64 velden met hun Position. */
    private void initializeBoard() {
        squares.clear();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares.add(new SquareModel(new Position(row, col)));
            }
        }
    }

    // --------------------------------------------------------------------
    // FEN parsing & initialisation
    // --------------------------------------------------------------------
    /**
     * Initialiseert het bord vanuit een FEN-string.
     * Zet alleen stukken + beurt (whiteToMove).
     */
    public void initializeFromFEN(String fen) {
        if (fen == null || fen.isEmpty()) {
            fen = "rn1qK1nR/pppppppp/3bbbb1/pppppppp/8/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1";
        }

        // 1) Bord leegmaken
        clearBoard();
        lastDoubleStepPawnPosition = null;

        String[] parts = fen.split("\\s+");
        String placement = parts[0];

        parseFenPlacement(placement);

        // 2) Beurt synchroniseren en listeners informeren
        if (parts.length > 1) {
            boolean whiteToMove = parts[1].equals("w");
            notifyListeners();
            notifyListenersTurnChanged(whiteToMove);
        } else {
            notifyListeners();
        }
    }

    /** Leegt alle stukken op het bord. */
    private void clearBoard() {
        for (SquareModel sq : squares) {
            sq.setPiece(null);
        }
    }

    /** Parse alleen het eerste FEN-veld (stuk-plaatsing). */
    private void parseFenPlacement(String placement) {
        String[] ranks = placement.split("/");
        int index = 0;

        for (String rank : ranks) {
            char[] chars = rank.toCharArray();
            for (char ch : chars) {
                if (Character.isDigit(ch)) {
                    index += Character.digit(ch, 10);
                } else {
                    SquareModel square = squares.get(index);
                    square.setPiece(pieceModelFromFenChar(ch));
                    index++;
                }
            }
        }
    }

    // --------------------------------------------------------------------
    // Piece helpers
    // --------------------------------------------------------------------
    /** Maakt een PieceModel vanuit een FEN char (bijv. 'p', 'N', 'Q'). */
    private PieceModel pieceModelFromFenChar(char c) {
        PieceColor color = Character.isLowerCase(c) ? PieceColor.BLACK : PieceColor.WHITE;

        PieceType type;
        switch (Character.toLowerCase(c)) {
            case 'k' -> type = PieceType.KING;
            case 'q' -> type = PieceType.QUEEN;
            case 'r' -> type = PieceType.ROOK;
            case 'b' -> type = PieceType.BISHOP;
            case 'n' -> type = PieceType.KNIGHT;
            case 'p' -> type = PieceType.PAWN;
            default -> {
                System.out.println("Onbekende FEN-letter: " + c);
                return null;
            }
        }
        return new PieceModel(type, color);
    }

    // --------------------------------------------------------------------
    // Accessors
    // --------------------------------------------------------------------
    public List<SquareModel> getSquares() {
        return squares;
    }

    /** Zoekt de SquareModel met gegeven Position; null als buiten bord of niet gevonden. */
    public SquareModel getSquare(Position pos) {
        if (pos == null) return null;
        int row = pos.getRow();
        int col = pos.getColumn();

        for (SquareModel sq : squares) {
            Position p = sq.getPosition();
            if (p.getRow() == row && p.getColumn() == col) {
                return sq;
            }
        }
        return null;
    }

    // --------------------------------------------------------------------
    // Mutating operations
    // --------------------------------------------------------------------
    /**
     * Verplaatst een stuk van from -> to.
     * Notificeert listeners en zet hasMoved = true op het stuk.
     */
    public void movePiece(Position from, Position to) {
        if (from == null || to == null) return;

        SquareModel source = getSquare(from);
        SquareModel target = getSquare(to);
        if (source == null || target == null) return;

        PieceModel piece = source.getPiece();
        if (piece == null) return;

        source.removePiece();
        target.setPiece(piece);

        // Markeer dat het stuk bewogen heeft (relevant voor rokade/pion-logica)
        piece.setHasMoved(true);

        notifyListeners();
    }

    // --------------------------------------------------------------------
    // En passant tracking
    // --------------------------------------------------------------------
    public void setLastDoubleStepPawnPosition(Position pos) {
        this.lastDoubleStepPawnPosition = pos;
    }

    public Position getLastDoubleStepPawnPosition() {
        return lastDoubleStepPawnPosition;
    }

    // --------------------------------------------------------------------
    // Listener API
    // --------------------------------------------------------------------
    public void addListener(BoardChangeListener/*public BoardSnapshot createSnapshot() {
        PieceModel[][] copy = new PieceModel[8][8];

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                PieceModel p = getSquare(new Position(r, c)).getPiece();
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
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                getSquare(new Position(r, c)).setPiece(snap.pieces[r][c]);
            }
        }
        this.lastDoubleStepPawnPosition = snap.lastDoubleStepPawnPosition;
        notifyListeners();
    }*/ listener) {
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
        return exportToFEN(true);
    }

    public String exportToFEN(boolean whiteToMove) {
        StringBuilder fen = new StringBuilder();

        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;
            for (int col = 0; col < 8; col++) {
                PieceModel piece = getSquare(new Position(row, col)).getPiece();
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
            if (row < 7) {
                fen.append('/');
            }
        }

        fen.append(" ");
        fen.append(whiteToMove ? "w" : "b");
        fen.append(" - - 0 1"); // TODO: echte rokade/en-passant/counters

        return fen.toString();
    }


}
