package com.example.smartbin_2020_2;

import java.util.HashMap;
import java.util.Map;

public enum Command {

    // ENVIO Y RECIBO
    CONEXION(19),
    DESCONEXION(27),

    // SOLO ENVIO
    DETENER_RIEGO(26),


    // RECIBO
    FIN_RIEGO_ZONA_1(5);



    private int value;
    private static Map map = new HashMap<>();

    static {
        for (Command pageType : Command.values()) {
            map.put(pageType.value, pageType);
        }
    }

    public static Command valueOf(int command) {
        return (Command) map.get(command);
    }

    Command(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
