package application.chesstrainerfx.model;


import application.chesstrainerfx.utils.*;
import application.chesstrainerfx.view.BoardChangeListener;

import java.util.ArrayList;
import java.util.List;

public class BoardModel {
    //een lijst van SquareModels
    private final List<SquareModel> squares;
    private final List<BoardChangeListener> listeners = new ArrayList<>();
//TODO verbeteren
    public BoardModel() {
        squares = new ArrayList<>(64);
        initializeBoard();
    }

    // voor het aanmaken van de velden en hun positie
    public void initializeBoard() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares.add(new SquareModel(new Position(row, col)));
            }
        }
    }

    // zet de stukken in de lijst squares
    public void initializeFromFEN(String fen) {
        if (fen.isEmpty()) {
            fen = "rn1qK1nR/pppppppp/3bbbb1/pppppppp/8/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1";
        }
        String[] FENString = fen.split(" ");
        String FEN = FENString[0];
        String[] ranks = FEN.split("/");
        // een teller voor de squares
        int counter = 0;
        // loop rij voor rij
        for (String rank : ranks) {
            //zet rij om in characters
            char[] chars = rank.toCharArray();
            for (char character : chars) {
                //if character is a number put a space in list instead of character
                if (Character.isDigit(character)) {
                    counter += Integer.parseInt(String.valueOf(character));
                } else {
                    squares.get(counter).setPiece(pieceModelformFENChar(character));
                    counter++;
                }
            }
        }
        if (FENString.length > 1) {
            boolean whiteToMove = FENString[1].equals("w");
            // meld dit via de listeners
            notifyListenersTurnChanged(whiteToMove);
        }
    }

    //geeft een PieceModel terug dat is opgebouwd uit PieceType en PieceColor
    public PieceModel pieceModelformFENChar(char c) {
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

    public List<SquareModel> getSquares() {
        return squares;
    }

    public SquareModel getSquare(Position pos) {
        SquareModel square = null;
        if (pos != null) {
            int row = pos.getRow();
            int col = pos.getColumn();


            for (SquareModel sq : squares) {
                if (sq.getPosition().getRow() == row && sq.getPosition().getColumn() == col) {
                    square = sq;
                }

            }

        }
            return square;
        }


    public void movePiece(Position from, Position to){

        if (from != null && to != null) {
            SquareModel source = getSquare(from);
            SquareModel  target = getSquare(to);
            PieceModel piece = source.getPiece();
            source.removePiece();
            target.setPiece(piece);
            notifyListeners();
        }

    }
    private Position lastDoubleStepPawnPosition = null;

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
    public String exportToFEN() {
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

        fen.append(" w - - 0 1"); // basis FEN metadata
        return fen.toString();
    }
    public void notifyListenersTurnChanged(boolean whiteToMove) {
        for (BoardChangeListener l : listeners) {
            l.onTurnChanged(whiteToMove);
        }
    }


    public void playCounterMove(String move, PieceColor color) {
        Position from = null;
        Position to;

        // doelveld = laatste 2 tekens (bv "e6")
        String target = move.substring(move.length() - 2);
        to = algebraicToPosition(target);

        // =========================
        // ♟️ PION-CAPTURE (fxe6)
        // =========================
        if (move.contains("x") && Character.isLowerCase(move.charAt(0))) {
            char fileChar = move.charAt(0); // f
            int file = fileChar - 'a';

            for (SquareModel sq : squares) {
                PieceModel p = sq.getPiece();
                if (p != null &&
                        p.getType() == PieceType.PAWN &&
                        p.getColor() == color &&
                        sq.getPosition().getColumn() == file) {

                    from = sq.getPosition();
                    break;
                }
            }
        }

        // =========================
        // ♟️ STUK-CAPTURE (Bxe6)
        // =========================
        else if (move.contains("x") && Character.isUpperCase(move.charAt(0))) {
            char pieceChar = move.charAt(0); // B, R, Q, N, K
            PieceType type = pieceTypeFromChar(pieceChar);

            for (SquareModel sq : squares) {
                PieceModel p = sq.getPiece();
                if (p != null &&
                        p.getType() == type &&
                        p.getColor() == color) {

                    boolean canReach = switch (type) {
                        case BISHOP -> canBishopReach(sq.getPosition(), to);
                        case ROOK   -> canRookReach(sq.getPosition(), to);
                        case QUEEN  -> canQueenReach(sq.getPosition(), to);
                        case KNIGHT -> canKnightReach(sq.getPosition(), to);
                        case KING   -> canKingReach(sq.getPosition(), to);
                        default     -> false;
                    };

                    if (canReach) {
                        from = sq.getPosition();
                        break;
                    }
                }
            }
        }

        // =========================
        // ♟️ GEWONE STUK-ZET (Be6)
        // =========================
        else if (Character.isUpperCase(move.charAt(0))) {
            char pieceChar = move.charAt(0);
            PieceType type = pieceTypeFromChar(pieceChar);

            for (SquareModel sq : squares) {
                PieceModel p = sq.getPiece();
                if (p != null &&
                        p.getType() == type &&
                        p.getColor() == color) {

                    boolean canReach = switch (type) {
                        case BISHOP -> canBishopReach(sq.getPosition(), to);
                        case ROOK   -> canRookReach(sq.getPosition(), to);
                        case QUEEN  -> canQueenReach(sq.getPosition(), to);
                        case KNIGHT -> canKnightReach(sq.getPosition(), to);
                        case KING   -> canKingReach(sq.getPosition(), to);
                        default     -> false;
                    };

                    if (canReach) {
                        from = sq.getPosition();
                        break;
                    }
                }
            }
        }

        // =========================
        // ♟️ PION VOORUIT (e6)
        // =========================
        else {
            for (SquareModel sq : squares) {
                PieceModel p = sq.getPiece();
                if (p != null &&
                        p.getType() == PieceType.PAWN &&
                        p.getColor() == color) {

                    from = sq.getPosition();
                    break;
                }
            }
        }

        // =========================
        // 🚀 UITVOEREN
        // =========================
        if (from != null && to != null) {
            movePiece(from, to);
            System.out.println("From? " + from + " to? " + to);
        } else {
            System.out.println("Geen geldige zet gevonden voor: " + move);
        }

    }




    private Position algebraicToPosition(String alg) {
        int col = alg.charAt(0) - 'a';
        int row = 8 - Character.getNumericValue(alg.charAt(1));
        return new Position(row, col);
    }

    private PieceType pieceTypeFromChar(char c) {
        switch (c) {
            case 'R': return PieceType.ROOK;
            case 'N': return PieceType.KNIGHT;
            case 'B': return PieceType.BISHOP;
            case 'Q': return PieceType.QUEEN;
            case 'K': return PieceType.KING;
            default: return PieceType.PAWN;
        }
    }



    private boolean canKnightReach(Position from, Position to) {
        int dr = Math.abs(to.getRow() - from.getRow());
        int dc = Math.abs(to.getColumn() - from.getColumn());
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }

    private boolean canRookReach(Position from, Position to) {
        if (from.getRow() != to.getRow() && from.getColumn() != to.getColumn()) {
            return false;
        }

        int rowStep = Integer.compare(to.getRow(), from.getRow());
        int colStep = Integer.compare(to.getColumn(), from.getColumn());

        int r = from.getRow() + rowStep;
        int c = from.getColumn() + colStep;

        while (r != to.getRow() || c != to.getColumn()) {
            SquareModel sq = getSquare(new Position(r, c));
            if (sq == null || sq.getPiece() != null) return false;
            r += rowStep;
            c += colStep;
        }
        return true;
    }

    private boolean canBishopReach(Position from, Position to) {
        int rowDiff = to.getRow() - from.getRow();
        int colDiff = to.getColumn() - from.getColumn();

        if (Math.abs(rowDiff) != Math.abs(colDiff)) return false;

        int rowStep = rowDiff > 0 ? 1 : -1;
        int colStep = colDiff > 0 ? 1 : -1;

        int r = from.getRow() + rowStep;
        int c = from.getColumn() + colStep;

        while (r != to.getRow() || c != to.getColumn()) {
            SquareModel sq = getSquare(new Position(r, c));
            if (sq == null || sq.getPiece() != null) return false;
            r += rowStep;
            c += colStep;
        }
        return true;
    }

    private boolean canQueenReach(Position from, Position to) {
        return canRookReach(from, to) || canBishopReach(from, to);
    }

    private boolean canKingReach(Position from, Position to) {

        int dr = Math.abs(to.getRow() - from.getRow());
        int dc = Math.abs(to.getColumn() - from.getColumn());
//        System.out.println("canKingReach? " + (dr <= 1 && dc <= 1));
        return dr <= 1 && dc <= 1;
    }

}
