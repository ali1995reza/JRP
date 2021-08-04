package jrp.gami.components;

import jrp.api.JRPSession;

public class Player {

    public enum State {
        CONNECTED, DISCONNECTED;

        public boolean is(State other) {
            return this == other;
        }

        public boolean isNot(State state) {
            return this != state;
        }
    }


    private final long id;
    private final String username;
    private State state;
    private JRPSession session;

    public Player(long id, String username, JRPSession session) {
        this.id = id;
        this.username = username;
        this.session = session;
        this.state = State.CONNECTED;
    }


    final void setState(State state) {
        if(state.is(State.DISCONNECTED))
            this.session = null;
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public void setSession(JRPSession session) {
        this.session = session;
    }

    public JRPSession getSession() {
        return session;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
