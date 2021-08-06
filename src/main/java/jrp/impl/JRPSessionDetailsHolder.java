package jrp.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;

public class JRPSessionDetailsHolder {

    private final static Random RANDOM = new SecureRandom();

    private int currentToken = 0;
    private long lastRequestTime = 0;


    public boolean fireToken(int token) {
        System.out.println("Provided token :" +token);
        System.out.println("TOken : "+currentToken);
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
