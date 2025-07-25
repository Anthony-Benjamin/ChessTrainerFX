package application.chesstrainerfx;/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author ebenjamin
 */
public class Position {
    public int row;
    public int column;

    public Position(int row, int col) {
        this.row = row;
        this.column = col;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    @Override
    public String toString() {
        return row + "," + column;
    }


}


