package jrp.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;

public class JRPSessionDetailsHolder {

    private final static Random RANDOM = new SecureRandom();

    private int currentToken = 0;
    private long lastRequestTime = 0;


    public boolean fireToken(int token) {
        if(token==currentToken){
            currentToken = RANDOM.nextInt();
            return true;
        }

        return false;
    }

    public int getCurrentToken() {
        return currentToken;
    }
}
