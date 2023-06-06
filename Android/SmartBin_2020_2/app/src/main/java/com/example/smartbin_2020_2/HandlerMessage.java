package com.example.smartbin_2020_2;

import android.os.Handler;

import java.io.IOException;
import java.util.ArrayList;

public class  HandlerMessage extends Handler {

    private StringBuilder dataStringIN;
    private MainActivity main;

    public HandlerMessage(MainActivity main) {
        dataStringIN = new StringBuilder();
        this.main = main;
    }

    public void handleMessage(android.os.Message msg){
        if(msg.what == BTHandler.handlerState){
            String readMessage = (String) msg.obj;
            dataStringIN.append(readMessage);
            if(dataStringIN.charAt(0) == '<') {
                int endOfLineIndex = dataStringIN.indexOf(">");
                if (endOfLineIndex > 0) {
                    int firstCommaIndex = dataStringIN.indexOf(",");
                    Command command;
                    String [] values = new String[0];
                    if(firstCommaIndex == -1) {
                        command = Command.valueOf(Integer.parseInt(dataStringIN.substring(1,endOfLineIndex)));
                    } else {
                        command = Command.valueOf(Integer.parseInt(dataStringIN.substring(1,
                                firstCommaIndex)));
                        values = dataStringIN.substring(firstCommaIndex + 1, endOfLineIndex)
                                .split(",");
                    }
                    switch (command) {
                        case CONEXION:
                            // extraer los datos de tipo de riego, censo de zona1 y zona2
                            main.setArduinoStatus(ArduinoStatus.Connected);
                            break;
                        case DESCONEXION:
                            try {
                                BTHandler.getInstance().desconnect();
                            } catch (IOException ignored) {
                                return;
                            }
                            main.setArduinoStatus(ArduinoStatus.Desconnected);
                            break;

                        default:
                            break;
                    }
                }
            }
            dataStringIN.delete(0, dataStringIN.length());
        }
    }
}