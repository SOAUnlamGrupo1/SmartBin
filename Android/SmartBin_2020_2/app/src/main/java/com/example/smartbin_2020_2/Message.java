package com.example.smartbin_2020_2;

public class Message {

    private Command command;

    public Message(Command command) {
        this.command = command;
    }

    @Override
    public String toString() {
        StringBuilder toString = new StringBuilder(String.format("<%s", command.toString()));
        return String.format("%s>", toString);
    }

}
