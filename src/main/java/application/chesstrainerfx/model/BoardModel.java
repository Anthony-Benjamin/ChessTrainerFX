package application.chesstrainerfx.model;

import application.chesstrainerfx.utils.*;
import application.chesstrainerfx.view.BoardChangeListener;

import java.util.ArrayList;
import java.util.List;

public class BoardModel {

    // Vast bord: altijd 64 velden
    private final SquareModel[] squares = new SquareModel[64];
    private final List<BoardChangeListener> listeners = new ArrayList<>();

    private Position lastDoubleStepPawnPosition = null;

    public BoardModel() {
        initializeBoard();
    }

    private int index(int row, int col) {
        return row * 8 + col;
    }

    private boolean isValidIndex(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    // Voor het aanmaken van de velden en hun positie
    public void initializeBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[index(row, col)] = new SquareModel(new Position(row, col));
            }
        }
    }

    // Zet de stukken op basis van FEN
    public void initializeFromFEN(String fen) {
        if (fen == null || fen.isEmpty()) {
            fen = "rn1qK1nR/pppppppp/3bbbb1/pppppppp/8/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1";
        }

        // Bord leegmaken
        for (SquareModel sq : squares) {
            sq.setPiece(null);
        }
        lastDoubleStepPawnPosition = null;

        String[] fenParts = fen.split(" ");
        String boardPart = fenParts[0];
        String[] ranks = boardPart.split("/");

        for (int row = 0; row < ranks.length && row < 8; row++) {
            String rank = ranks[row];
            int col = 0;

            for (char character : rank.toCharArray()) {
                if (Character.isDigit(character)) {
                    col += Character.getNumericValue(character);
                } else {
                    if (isValidIndex(row, col)) {
                        squares[index(row, col)].setPiece(pieceModelFormFENChar(character));
                    }
                    col++;
                }
            }
        }

        notifyListeners();

        if (fenParts.length > 1) {
            boolean whiteToMove = fenParts[1].equals("w");
            notifyListenersTurnChanged(whiteToMove);
        }
    }

    // Geeft een PieceModel terug op basis van FEN-char
    public PieceModel pieceModelFormFENChar(char c) {
        PieceColor color = null;
        PieceType type = null;

        if (Character.isLowerCase(c)) {
            color = PieceColor.BLACK;
        } else if (Character.isUpperCase(c)) {
            color = PieceColor.WHITE;
        }

        switch (Character.toLowerCase(c)) {
            case 'k':
                type = PieceType.KING;
                break;
            case 'q':
                type = PieceType.QUEEN;
                break;
            case 'r':
                type = PieceType.ROOK;
                break;
            case 'b':
                type = PieceType.BISHOP;
                break;
            case 'n':
                type = PieceType.KNIGHT;
                break;
            case 'p':
                type = PieceType.PAWN;
                break;
            default:
                System.out.println("geen geldige letter");
        }

        return new PieceModel(type, color);
    }

    public SquareModel[] getSquares() {
        return squares;
    }

    public SquareModel getSquare(Position pos) {
        if (pos == null) return null;

        int row = pos.getRow();
        int col = pos.getColumn();

        if (!isValidIndex(row, col)) return null;

        return squares[index(row, col)];
    }

    public void movePiece(Position from, Position to) {
        if (from == null || to == null) return;

        SquareModel source = getSquare(from);
        SquareModel target = getSquare(to);

        if (source == null || target == null) return;

        PieceModel piece = source.getPiece();
        source.removePiece();
        target.setPiece(piece);

        notifyListeners();
    }

    public void setLastDoubleStepPawnPosition(Position pos) {
        this.lastDoubleStepPawnPosition = pos;
    }

    public Position getLastDoubleStepPawnPosition() {
        return lastDoubleStepPawnPosition;
    }

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

    public String exportToFEN() {
        return exportToFEN(true);
    }

    public String exportToFEN(boolean whiteToMove) {
        StringBuilder fen = new StringBuilder();

        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;

            for (int col = 0; col < 8; col++) {
                PieceModel piece = squares[index(row, col)].getPiece();

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
        fen.append(" - - 0 1");

        return fen.toString();
    }
}