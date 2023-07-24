package edu.uob.exception;

import java.io.Serial;

public class GameException extends Exception{
    public GameException(String exceptionMessage) {
        super("[ERROR]" + exceptionMessage);
    }
}
