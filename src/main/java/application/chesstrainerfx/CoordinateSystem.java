package application.chesstrainerfx;

public class CoordinateSystem {

    /*een klasse met statische methoden voor het berekenen van  index naar code en andersom*/


//    public static void test() {
//        for (int row = 0; row < 8; row++) {
//            for (int col = 0; col < 8; col++) {
//                System.out.print(row + "," + col + " ");
//            }
//            System.out.println();
//        }
//    }
    public static int[] coordinateToIndex(String notation){
        int file = notation.charAt(0) - 'a'; //column
        int rank = 8 - Character.getNumericValue(notation.charAt(1));

        //int  test = Character.getNumericValue(notation.charAt(1));
        //System.out.println(test);

        //System.out.println(file + "," + rank);
        int[] coordinate = {rank, file};

        return coordinate;
    }
    public static String indexToCoordinate(int[] index){
        // index[0] is rank, index[1] is column/file
        final String[] FILES = {"a", "b", "c", "d", "e", "f", "g", "h"};    // vertical columns

        String file = FILES[index[1]];    // file or column
        //System.out.println(file);
        int rank = 8 - index[0];
        //System.out.println(file + rank);
        String output = file + rank;

        return output;
    }
}
