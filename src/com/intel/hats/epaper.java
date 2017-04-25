package com.intel.hats;

import jssc.*;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by bayetrax on 4/19/2017.
 */
public class epaper implements SerialPortEventListener {

    private final String portName;
    String wantedPortName = "COM 4";
    SerialPort serialPort;
    public boolean isAlive = true;


    public String getImgName() {
        return imgName = (imgName == null) ? "QRAZ19.BMP" : imgName;
    }

    public void setImgName(String imgName) {
        this.imgName = imgName;
    }

    String imgName;
    private Runnable animRunnable;
    int posY;

    public epaper(String[] args) throws SerialPortException {

        if (args.length == 0) {
            System.out.println("\n\n/*************************portName is required in argument . In the device manager of PC, Check the com port epaper is connected too*************************/\n\n");
            throw new SerialPortException("No port", "constructor of epaper", "checking first argument");
        }
        portName = args[0];

        OpenConnection(portName);

        startAnim();

//        writeHandShake();
//
//        writePicture("QRAZ19.BMP",0,0);
//
//
//        writePicture("PIC3.BMP",200,50);
//
//        writePicture("PIC4.BMP",200,50);
//
//        writePicture("C39AZ19.BMP",200,50);
//        writePicture("PDFAZ19.BMP",200,50);
//
//        writePicture("DMAZ19.BMP",200,50);
//        writePicture("E817.BMP",200,50);
//
//        writePicture("PIC2.BMP",200,50);


//        closeSocket();
    }


    Thread animThread;
    int moveX = 20;

    private void startAnim() throws SerialPortException {

        Random rand = new Random();
        animRunnable = new Runnable() {
            @Override
            public void run() {
                while (isAlive) {
                    try {
//                        System.out.println("serialPort.isOpened() ="+serialPort.isOpened());
                        if (serialPort.isOpened()) {

                            writePicture(getImgName(), moveX, posY);

                            moveX = (int) (rand.nextInt(400) + 1);
                            posY = (int) (rand.nextInt(50) + 1);
                            closeSocket();
                        } else {

                            OpenConnection(portName);
                        }

                        sleep(3 * 1000);
                    } catch (SerialPortException e) {
                        e.printStackTrace();
                        System.out.println("------->>> There was a SerialPortException in Animation-"+e);
                        isAlive = false;
                    }

                }

            }
        };
        animThread = new Thread(animRunnable);
        animThread.start();
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private byte[] writeInt1Bytes(int value) {
        byte[] data = new byte[1];
        data[0] = (byte) (value & 0xFF);
        return data;
    }

    private byte[] writeInt2Bytes(int value) {
        byte[] data = new byte[2];
        data[1] = (byte) (value & 0xFF);
        data[0] = (byte) ((value >> 8) & 0xFF);
//        data[2] = (byte) ((value >> 16) & 0xFF);
//        data[3] = (byte) ((value >> 32) & 0xFF);
        return data;
    }

    private void writePicture(String imgName, int x, int y) throws SerialPortException {

        writeClearScreen();

        int FrameHeader = 1;
        int DATA = imgName.getBytes().length;
        int CommandType = 1;
        int FRAMEEND = 4;
        int PARITY = 1;
        //        0xA5 0x00 0x18 0x70 0x00 0x00 0x00 0x00 0x51 0x52 0x41 0x5A 0x31 0x39 0x2E 0x42 0x4D 0x50 0x00 0xCC 0x33 0xC3 0x3C 0xAC
        int FrameLength = FrameHeader + 2/*itself 2 byte*/ + CommandType + 2/*x pos*/ + 2/*y pos*/ + DATA + 1/* ending 0 */ + FRAMEEND + PARITY;
//        System.out.println("FrameLength0 - " + FrameLength);

        ByteBuffer byteBuffer = ByteBuffer.allocate(FrameLength);
        byteBuffer.put((byte) 0xA5);//FrameHeader
        byteBuffer.put(writeInt2Bytes(FrameLength));//FrameLength
        byteBuffer.put((byte) 0x70);//CommandType
        byteBuffer.put(writeInt2Bytes(x));//X position
        byteBuffer.put(writeInt2Bytes(y));//Y position
        byteBuffer.put(imgName.getBytes());//DATA
        byteBuffer.put((byte) 0x00);//ending “0”
        byteBuffer.put((byte) 0xCC);
        byteBuffer.put((byte) 0x33);
        byteBuffer.put((byte) 0xC3);
        byteBuffer.put((byte) 0x3C);

        int parity = (byte) 0xA5
                ^ (writeInt2Bytes(FrameLength))[0] ^ (writeInt2Bytes(FrameLength))[1]
                ^ (byte) 0x70
                ^ (writeInt2Bytes(x))[0] ^ (writeInt2Bytes(x))[1]
                ^ (writeInt2Bytes(y))[0] ^ (writeInt2Bytes(y))[1]
                ^ (byte) 0x00
                ^ (byte) 0xCC
                ^ (byte) 0x33
                ^ (byte) 0xC3
                ^ (byte) 0x3C;

        for (int i = 0; i < imgName.getBytes().length; i++) {
            parity = parity ^ imgName.getBytes()[i];
        }

        byteBuffer.put(writeInt1Bytes(parity));
//        for (byte b : byteBuffer.array()) {
//            System.out.print(String.format("%X ", b));
//        }

        serialPort.writeBytes(byteBuffer.array());
//        System.out.println();
//        System.out.println("What...a Picture");
        writeRefresh();
    }

    private void writeHandShake() throws SerialPortException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(10 * 4);
        byteBuffer.put((byte) 0xA5);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put((byte) 0x09);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put((byte) 0xCC);
        byteBuffer.put((byte) 0x33);
        byteBuffer.put((byte) 0xC3);
        byteBuffer.put((byte) 0x3C);
        byteBuffer.put((byte) 0xAC);

        serialPort.writeBytes(byteBuffer.array());

        System.out.println("What...a HandShake");
    }

    private void writeClearScreen() throws SerialPortException {
//        0xA5 0x00 0x09 0x2E 0xCC 0x33 0xC3 0x3C 0x82
        ByteBuffer byteBuffer = ByteBuffer.allocate(10 * 4);
        byteBuffer.put((byte) 0xA5);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put((byte) 0x09);
        byteBuffer.put((byte) 0x2E);
        byteBuffer.put((byte) 0xCC);
        byteBuffer.put((byte) 0x33);
        byteBuffer.put((byte) 0xC3);
        byteBuffer.put((byte) 0x3C);
        byteBuffer.put((byte) 0x82);

        serialPort.writeBytes(byteBuffer.array());

//        System.out.println("What...a ClearScreen");
    }

    private void writeRefresh() throws SerialPortException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(9 * 4);
        byteBuffer.put((byte) 0xA5);
        byteBuffer.put((byte) 0x00);
        byteBuffer.put((byte) 0x09);
        byteBuffer.put((byte) 0x0A);
        byteBuffer.put((byte) 0xCC);
        byteBuffer.put((byte) 0x33);
        byteBuffer.put((byte) 0xC3);
        byteBuffer.put((byte) 0x3C);
        byteBuffer.put((byte) 0xA6);

        serialPort.writeBytes(byteBuffer.array());

//        System.out.println("What...a Refresh");
    }


    private void OpenConnection(String arg0) throws SerialPortException {
        String[] portNames = SerialPortList.getPortNames();
        boolean found = false;
        for (int i = 0; i < portNames.length; i++) {
//            System.out.println(portNames[i]);
            if (arg0.equalsIgnoreCase(portNames[i])) {
                found = true;
            }
        }
        if (found == false) {
            throw new SerialPortException(arg0, "OpenConnection", "Not found; hardware might be in different port");
        }

        serialPort = new SerialPort(arg0);
        serialPort.openPort();//Open serial port
        serialPort.setParams(SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE, false, true);//Set params. Also you can set params by this string: serialPort.setParams(9600, 8, 1, 0);

        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
//        System.out.println("What...a connection");
        serialPort.addEventListener(this::serialEvent);
//            serialPort.closePort();
    }


    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
//        System.out.println("serialPortEvent getPortName-" + serialPortEvent.getPortName());
//        System.out.println("serialPortEvent getEventType-" + serialPortEvent.getEventType());
//        System.out.println("serialPortEvent -getEventValue-" + serialPortEvent.getEventValue());

        if (serialPortEvent.isRXCHAR()) { // if we receive data
            if (serialPortEvent.getEventValue() > 0) { // if there is some existent data
                try {
                    byte[] bytes = this.serialPort.readBytes(); // reading the bytes received on serial port
                    if (bytes != null) {
//                        for (byte b : bytes) {
////                            this.serialInput.add(b); // adding the bytes to the linked list
//
//                            // *** DEBUGGING *** //
//                            System.out.println(b);
//                            System.out.print(String.format("%x ", b));
//                        }
                        convertHexToString(bytes);
                    }
                } catch (SerialPortException e) {
                    System.out.println(e);
                    e.printStackTrace();
                }
            }
        } else {
            try {
                System.out.println("-------------------" + convertHexToString(this.serialPort.readBytes()));
            } catch (SerialPortException e) {
                e.printStackTrace();
            }
        }

//        System.out.println("");
    }

    public void closeSocket() throws SerialPortException {
        serialPort.closePort();
    }

    public String convertHexToString(byte[] hexbytes) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < hexbytes.length; i += 1) {
            hex.append(String.format("%x", hexbytes[i]));
        }
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
//        System.out.println(output);
        return output.toString();
    }
}
