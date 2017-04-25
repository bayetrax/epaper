package com.intel.hats;

import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        epaper epaperObj = null;
        epaperObj = new epaper(args);
//        System.out.println("Exception in create EPAPER OBJ");

        Scanner scanner = new Scanner(System.in);
        System.out.println("/************************Let's display new Image on e-Paper !*************************/");
        System.out.println("/*\n" +
                "The name of the bitmap file should be in uppercase English character(s). And\n" +
                "the string length of the bitmap name should be less than 11 characters, in\n" +
                "which the ending “0” is included. For example, PIC7.BMP and PIC789.BMP\n" +
                "are correct bitmap names, while PIC7890.BMP is a wrong bitmap namem.\n" +
                " */");
        System.out.println("Feed me image File name Ex: PIC3.BMP \t  type :kill  - to stop ");

        String imageName;
        while (true) {
            imageName = scanner.next();
            if(imageName.equalsIgnoreCase("kill")){
                epaperObj.isAlive = false;
                break;
            }
            epaperObj.setImgName(imageName);
            System.out.println("Keep Going! Feed me image \t -OR- \t type :kill  - to stop ");
        }

        System.out.println("/*************************ask for modification & improvements - bipin.srinivasx.ayetra@intel.com*************************/");
        epaperObj.closeSocket();

    }
}
