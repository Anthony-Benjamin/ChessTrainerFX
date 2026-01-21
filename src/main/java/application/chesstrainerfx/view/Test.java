package application.chesstrainerfx.view;

public class Test {

    public static void main(String[] args){
        String pgn = "1. Rxe6+ fxe6 (1... Bxe6 2.Ba4+ Qxa4 (2... Rb5 3. Bxb5+ Qxb5 4. Rd8#) 3." +
                "Rd8#) 2. Bg6+ Rf7 3. Qxf7+ Kd8 4.Qxd7#";

        String result = removeVariations(pgn);
        System.out.println(result);
    }

    static String removeVariations(String pgn){
        StringBuilder result = new StringBuilder();
        int depth = 0;

        System.out.println(pgn.toCharArray());

        for(char c : pgn.toCharArray()){
            if(c == '('){
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0) {
                result.append(c);
            }
//            System.out.println("depth: " + depth);
        }

        return result.toString();
    }
}
