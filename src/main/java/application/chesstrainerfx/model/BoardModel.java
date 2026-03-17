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

    /**
     * Constructs a new BoardModel and initializes the chess board.
     */
    public BoardModel() {
        initializeBoard();
    }

    /**
     * Calculates the linear array index for a given row and column.
     *
     * @param row The row (0-7).
     * @param col The column (0-7).
     * @return The corresponding index in the `squares` array.
     */
    private int index(int row, int col) {
        return row * 8 + col;
    }

    /**
     * Checks if a given row and column are within the valid bounds of the chess board.
     *
     * @param row The row to check.
     * @param col The column to check.
     * @return True if the row and column are valid, false otherwise.
     */
    private boolean isValidIndex(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    /**
     * Initializes all 64 squares of the chess board, assigning each a Position object.
     */
    public void initializeBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[index(row, col)] = new SquareModel(new Position(row, col));
            }
        }
    }

    /**
     * Initializes the chess board's piece setup and game state from a FEN string.
     * If the FEN string is null or empty, a default FEN is used.
     *
     * @param fen The FEN (Forsyth-Edwards Notation) string representing the board state.
     */
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

    /**
     * Creates a PieceModel object based on a FEN character.
     * The character determines the piece type and color (uppercase for White, lowercase for Black).
     *
     * @param c The FEN character representing a piece (e.g., 'p', 'R', 'k').
     * @return A new PieceModel instance, or null if the character is invalid.
     */
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
                System.out.println("geen geldige letter"); // Consider logging or throwing an exception
                return null; // Return null for invalid characters
        }

        return new PieceModel(type, color);
    }

    /**
     * Returns the array of all 64 SquareModel objects representing the board.
     *
     * @return An array of SquareModel objects.
     */
    public SquareModel[] getSquares() {
        return squares;
    }

    /**
     * Retrieves the SquareModel at a specific position on the board.
     *
     * @param pos The Position object (row, col) of the desired square.
     * @return The SquareModel at the given position, or null if the position is invalid.
     */
    public SquareModel getSquare(Position pos) {
        if (pos == null) return null;

        int row = pos.getRow();
        int col = pos.getColumn();

        if (!isValidIndex(row, col)) return null;

        return squares[index(row, col)];
    }

    /**
     * Moves a piece from a source position to a target position.
     * This method assumes the move is valid according to game rules; it only handles the physical transfer.
     *
     * @param from The starting Position of the piece.
     * @param to The destination Position for the piece.
     */
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

    /**
     * Sets the position of a pawn that just performed a double-step move.
     * This is crucial for determining potential en passant captures.
     *
     * @param pos The Position of the pawn that just moved two squares.
     */
    public void setLastDoubleStepPawnPosition(Position pos) {
        this.lastDoubleStepPawnPosition = pos;
    }

    /**
     * Retrieves the position of the pawn that most recently performed a double-step move.
     *
     * @return The Position of the double-stepped pawn, or null if no such move occurred in the last turn.
     */
    public Position getLastDoubleStepPawnPosition() {
        return lastDoubleStepPawnPosition;
    }

    /**
     * Adds a BoardChangeListener to receive notifications about board updates and turn changes.
     *
     * @param listener The BoardChangeListener to add.
     */
    public void addListener(BoardChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Notifies all registered listeners that the board state has been updated.
     */
    private void notifyListeners() {
        for (BoardChangeListener listener : listeners) {
            listener.onBoardUpdated();
        }
    }

    /**
     * Notifies all registered listeners that the turn has changed.
     *
     * @param whiteToMove True if it's White's turn, false if it's Black's turn.
     */
    public void notifyListenersTurnChanged(boolean whiteToMove) {
        for (BoardChangeListener l : listeners) {
            l.onTurnChanged(whiteToMove);
        }
    }

    /**
     * Exports the current board state to a FEN (Forsyth-Edwards Notation) string,
     * assuming it's White's turn to move.
     *
     * @return A FEN string representing the current board state.
     */
    public String exportToFEN() {
        return exportToFEN(true);
    }

    /**
     * Exports the current board state to a FEN (Forsyth-Edwards Notation) string.
     *
     * @param whiteToMove True if it's White's turn, false if it's Black's turn.
     * @return A FEN string representing the current board state and whose turn it is.
     */
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
        fen.append(" - - 0 1"); // Simplistic castling, en passant, halfmove, and fullmove counters
        return fen.toString();
    }
}