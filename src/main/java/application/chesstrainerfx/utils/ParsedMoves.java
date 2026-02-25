package application.chesstrainerfx.utils;

import java.util.List;

public class ParsedMoves {
        public List<String> whiteMoves;
        public List<String> blackMoves;

        ParsedMoves(List<String> w, List<String> b) {
            this.whiteMoves = w;
            this.blackMoves = b;
        }
}