package application.chesstrainerfx;

import java.util.ArrayList;
import java.util.Arrays;

public class Test {
    public static void main(String[] args) {
        String FEN = "bqr1krnb/pppppppp/4n3/8/8/8/PPPPPPPP/BQRNKRNB";
//        String FEN = "rbqnbkrn/pppppppp/8/8/3P4/2N1P3/PPP2PPP/RBQ1BKRN";
        String[] rijen = FEN.split("/");
        ArrayList<String> array2 = new ArrayList<>();
        for (int k = 0; k < rijen.length; k++) {
            String rij = rijen[k];    // row

                char[] chars = rij.toCharArray();

                for (char character : chars) {
                    // System.out.println(character);

                    if (isNumeric(String.valueOf(character))) {
                        for (int j = 0; j < Integer.parseInt(String.valueOf(character)); j++) {
                            array2.add(".");
                        }
                    } else {
                        array2.add(String.valueOf(character));
                    }
                    System.out.println();
//                    System.out.print();
                }
            }
           System.out.println(array2);
        }


    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }

        try {
            int number = Integer.parseInt(strNum);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
