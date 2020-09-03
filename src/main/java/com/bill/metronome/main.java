/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bill.metronome;

import ui.MainWindow;
/**
 *
 * @author Bill
 */
public class main 
{
    public static void main(String[] args) 
    {
        MainWindow mainW= new MainWindow();
        mainW.pack();
        mainW.setLocationRelativeTo(null);//Center the window
        mainW.setTitle("Metronome");
        mainW.setResizable(false);
        mainW.setVisible(true);

    }
}
