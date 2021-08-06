package jrp.gami;

import jrp.api.JRPSession;
import jrp.gami.components.Game;
import jrp.gami.components.Player;

public class UserDetails {

    public enum State {
        LOGGED_ID, FINDING_GAME, CONNECTED_TO_GAME;

        public boolean is(State other) {
            return this == other;
        }

        public boolean isNot(State other) {
            return this != other;
        }
    }


    private final long id;
    private final String username;
    private final JRPSession session;
    private State state = State.LOGGED_ID;
    private Game game;

    public UserDetails(long id, String username, JRPSession session) {
        this.id = id;
        this.username = username;
        this.session = session;
    }

    public long id() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public State state() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public JRPSession getSession() {
        return session;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

}
