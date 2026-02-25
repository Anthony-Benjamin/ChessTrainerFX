package application.chesstrainerfx.utils;

public class ChessMainlineExtractor {

    public static String extractMainline(String pgnWithVariants) {
        if (pgnWithVariants == null || pgnWithVariants.trim().isEmpty()) {
            return "";
        }

        StringBuilder mainline = new StringBuilder();
        int depth = 0;
        boolean inNumber = false;
        StringBuilder currentToken = new StringBuilder();

        String text = pgnWithVariants.trim() + " "; // extra spatie om laatste token af te sluiten

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '(') {
                depth++;
                // begin van variant → negeren tot bijpassende )
                if (depth == 1) {
                    // token vóór ( was waarschijnlijk hoofdzet → bewaren
                    if (currentToken.length() > 0) {
                        appendToken(mainline, currentToken.toString());
                        currentToken.setLength(0);
                    }
                }
                continue;
            }

            if (c == ')') {
                depth--;
                if (depth < 0) depth = 0; // safety

                // einde variant → reset token
                currentToken.setLength(0);

                // na ) komt meestal weer hoofdlijn
                continue;
            }

            if (depth > 0) {
                // binnen variant → overslaan
                continue;
            }

            // Hoofdlijn logica
            if (Character.isWhitespace(c)) {
                if (currentToken.length() > 0) {
                    appendToken(mainline, currentToken.toString());
                    currentToken.setLength(0);
                }
            } else {
                currentToken.append(c);
            }
        }

        // laatste token nog toevoegen als die er staat
        if (currentToken.length() > 0) {
            appendToken(mainline, currentToken.toString());
        }

        String result = mainline.toString().trim();

        // optioneel: dubbele spaties opruimen en nette zetnummering behouden
        result = result.replaceAll("\\s+", " ")
                .replaceAll(" (\\d+\\.+)", " $1");

        return result;
    }

    private static void appendToken(StringBuilder sb, String token) {
        if (token.isEmpty()) return;

        // typische PGN-tokens die we willen
        if (token.matches("\\d+\\.+")          // 1.
                || token.matches("\\d+\\.\\.\\.") // 1...
                || isMove(token)                  // e4, Nf3, Qxd7#, O-O etc.
                || token.equals("+")
                || token.equals("#")
                || token.equals("!")) {

            if (sb.length() > 0 && !sb.toString().endsWith(" ")) {
                sb.append(" ");
            }
            sb.append(token);
        }
    }

    private static boolean isMove(String s) {
        // hele simpele check of het op een schaakzet lijkt
        return s.matches("[KQRBN]?[a-h]?[1-8]?[-x]?[a-h][1-8][+#!?=]{0,3}(?:\\s*[\\+\\#])?");
    }

    // Test
    public static void main(String[] args) {
        String input = "1. Rxe6+ fxe6 (1... Bxe6 2.Ba4+ Qxa4 (2... Rb5 3. Bxb5+ Qxb5 4. Rd8#) 3.Rd8#) 2. Bg6+ Rf7 3. Qxf7+ Kd8 4.Qxd7#";

        String mainline = extractMainline(input);
        System.out.println("Mainline:");
        System.out.println(mainline);
    }
}
