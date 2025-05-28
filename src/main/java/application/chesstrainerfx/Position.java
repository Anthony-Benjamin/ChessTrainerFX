package application.chesstrainerfx;/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author ebenjamin
 */
public class Position {
    private int row;
    private int col;
    
    public Position(int row, int col){
        this.row = row;
        this.col = col;        
    }
    
    public int getCol(){
        return col;
    }
    public int getRow(){
        return row;
    }

    @Override
    public String toString(){
        return row + "," + col;
    }
}
