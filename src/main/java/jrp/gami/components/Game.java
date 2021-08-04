package jrp.gami.components;

import jrp.api.JRPRequest;
import jrp.gami.GamiStatusCodes;
import jrp.gami.UserDetails;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Game {


    public enum State {
        UNKNOWN, INITIALIZE, RUNNING, PAUSE, END;

        public boolean is(State other) {
            return this == other;
        }

        public boolean isNot(State other) {
            return this != other;
        }

        public boolean isNotOneOf(State... others) {
            for (State other : others) {
                if (this == other)
                    return true;
            }

            return false;
        }

    }


    private final List<Player> players;
    private final GameScript script;
    private final Object _lock = new Object();
    private State state = State.UNKNOWN;


    public Game(List<UserDetails> pl, GameScript script) {
        this.script = script;
        List<Player> players = new ArrayList<>();
        for (UserDetails player : pl) {
            players.add(new Player(
                    player.id(),
                    player.getUsername(),
                    player.getSession()
            ));
        }
        this.players = Collections.unmodifiableList(players);
    }

    public State state() {
        return state;
    }

    public void initialize() {
        synchronized (_lock) {

        }
    }

    public Player findPlayerBy(long id) {
        for(Player player:players) {
            if(player.getId() == id)
                return player;
        }

        return null;
    }

    public Player findPlayerByUsername(String username) {
        for(Player player:players) {
            if(player.getUsername().equals(username))
                return player;
        }

        return null;
    }

    private void doHandleRequest(JRPRequest request) {
        if(state.isNot(State.RUNNING))
            request.response(GamiStatusCodes.GAME_BAD_STATE);

        UserDetails userDetails = request.requester().attachment();

        script.onRequestReceived(this, findPlayerBy(userDetails.id()), request);
    }

    private ByteBuffer doInitialize() {
        if (state.isNot(State.UNKNOWN))
            throw new IllegalStateException("bad state !");

        State lastState = state;
        state = State.INITIALIZE;

        return script.onGameStateChanged(lastState, this);
    }

    private ByteBuffer doRunGame() {
        if (state.isNotOneOf(State.INITIALIZE, State.PAUSE))
            throw new IllegalStateException("invalid state");

        State lastState = state;
        state = State.RUNNING;

        return script.onGameStateChanged(lastState, this);
    }

    private ByteBuffer doPauseGame() {
        if (state.isNot(State.RUNNING))
            throw new IllegalStateException("invalid state");

        State lastState = state;
        state = State.PAUSE;

        return script.onGameStateChanged(lastState, this);
    }

    private ByteBuffer doEndGame() {

        State lastState = state;
        state = State.END;

        return script.onGameStateChanged(lastState, this);
    }
}
