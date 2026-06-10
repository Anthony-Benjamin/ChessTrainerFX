package application.chesstrainerfx.utils;

/** Statische conversies tussen schaaknotatie ("e4") en bord-indexen [row, col]. */
public class CoordinateSystem {

    public static int[] coordinateToIndex(String notation){
        int file = notation.charAt(0) - 'a'; // column
        int rank = 8 - Character.getNumericValue(notation.charAt(1));

        return new int[]{rank, file};
    }

    public static String indexToCoordinate(int[] index){
        // index[0] is rank, index[1] is column/file
        final String[] FILES = {"a", "b", "c", "d", "e", "f", "g", "h"};

        String file = FILES[index[1]];
        int rank = 8 - index[0];

        return file + rank;
    }
}
