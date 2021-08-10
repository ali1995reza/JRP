package jrp.gami.components;

import jrp.api.JRPRequest;
import jrp.api.JRPSession;
import jrp.gami.GamiMessageCodes;
import jrp.gami.GamiStatusCodes;
import jrp.gami.UserDetails;
import jrp.impl.ThreadLocalBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class Game {

    public enum State {
        UNKNOWN((byte) 0), INITIALIZE((byte) 1), RUNNING((byte) 2), PAUSE((byte) 3), END((byte) 4);

        private final byte code;

        State(byte code) {
            this.code = code;
        }

        public byte code() {
            return code;
        }

        public boolean is(State other) {
            return this == other;
        }

        public boolean isNot(State other) {
            return this != other;
        }

        public boolean isNotOneOf(State... others) {
            for (State other : others) {
                if (this == other) {
                    return false;
                }
            }
            return true;
        }

        public boolean isOneOf(State... others) {
            for (State other : others) {
                if (this == other) {
                    return true;
                }
            }
            return false;
        }
    }

    private final static ByteBuffer CONNECT_TO_GAME_HEADER = ByteBuffer.wrap(new byte[]{GamiMessageCodes.CONNECT_TO_GAME}).asReadOnlyBuffer();

    private final List<Player> players;
    private final GameScript script;
    private final Object _lock = new Object();
    private State state = State.UNKNOWN;
    private final String id;
    private final Consumer<Game> endNotifier;
    private final ThreadLocalBuffer headerBuffers;
    private final ThreadLocalBuffer gameBuffers;

    public Game(String id, List<UserDetails> pl, GameScript script, Consumer<Game> endNotifier, ThreadLocalBuffer headerBuffers, ThreadLocalBuffer gameBuffers) {
        this.headerBuffers = headerBuffers;
        this.gameBuffers = gameBuffers;
        System.out.println("GAME ID IS : "+id);
        this.id = id;
        this.script = script;
        this.endNotifier = endNotifier;
        List<Player> players = new ArrayList<>();
        for (UserDetails player : pl) {
            players.add(new Player(player.id(), player.getUsername(), player.getSession()));
        }
        this.players = Collections.unmodifiableList(players);
    }

    public State state() {
        return state;
    }

    public String id() {
        return id;
    }

    public boolean initialize() {
        synchronized (_lock) {
            try {
                doInitialize();
                return true;
            } catch (Throwable e) {
                doEndGame();
            }
            return false;
        }
    }

    public ByteBuffer getBuffer() {
        return gameBuffers.getBuffer();
    }

    public void run() {
        synchronized (_lock) {
            try {
                doRunGame();
            } catch (Throwable e) {
                doEndGame();
            }
        }
    }

    public void pause() {
        synchronized (_lock) {
            try {
                doPauseGame();
            } catch (Throwable e) {
                doEndGame();
            }
        }
    }

    public void handleRequest(JRPRequest request) {
        synchronized (_lock) {
            try {
                doHandleRequest(request);
            } catch (Throwable e) {
                doEndGame();
            }
        }
    }

    public void handlePlayerDisconnect(JRPSession session) {
        synchronized (_lock) {
            try {
                doHandlePlayerDisconnect(session);
            } catch (Throwable e) {
                doEndGame();
            }
        }
    }

    public void handlePlayerConnect(JRPSession session) {
        synchronized (_lock) {
            try {
                doHandlePlayerConnect(session);
            } catch (Throwable e) {
                doEndGame();
            }
        }
    }

    public void routine() {
        synchronized (_lock) {
            if (state.isOneOf(State.END, State.UNKNOWN)) {
                return;
            }
            try {
                script.routine(this);
            } catch (Throwable e) {
                doEndGame();
            }
        }
    }

    public void end() {
        synchronized (_lock) {
            try {
                doEndGame();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            endNotifier.accept(this);
        }
    }

    public Player findPlayerById(long id) {
        for (Player player : players) {
            if (player.getId() == id) {
                return player;
            }
        }
        return null;
    }

    public Player findPlayerByUsername(String username) {
        for (Player player : players) {
            if (player.getUsername().equals(username)) {
                return player;
            }
        }
        return null;
    }


    private ByteBuffer getPlayerStateChangedHeaderBuffer(Player player) {
        ByteBuffer buffer = headerBuffers.getBuffer();
        buffer.put(GamiMessageCodes.PLAYER_STATE_CHANGED);
        buffer.put(player.getState().code());
        buffer.putLong(player.getId());
        return buffer;
    }

    private boolean doHandlePlayerDisconnect(JRPSession session) {
        UserDetails userDetails = session.attachment();
        if (userDetails == null) {
            return false;
        }
        Player player = findPlayerById(userDetails.id());
        if (player == null || player.getState().is(Player.State.DISCONNECTED)) {
            return false;
        }
        player.setState(Player.State.DISCONNECTED);
        player.setSession(null);
        System.out.println("Player "+player.getId()+" is disconnected");
        ByteBuffer dataToSend = getPlayerStateChangedHeaderBuffer(player);
        for (Player p : players) {
            if (p == player) {
                continue;
            }
            p.send(dataToSend.duplicate());
        }
        script.onPlayerStateChanged(player);
        return true;
    }

    private boolean doHandlePlayerConnect(JRPSession session) {
        UserDetails userDetails = session.attachment();
        if (userDetails == null) {
            return false;
        }
        Player player = findPlayerById(userDetails.id());
        if (player.getState().is(Player.State.CONNECTED)) {
            return false;
        }
        player.setState(Player.State.CONNECTED);
        player.setSession(session);
        ByteBuffer buffer = script.onPlayerStateChanged(player);
        player.send(CONNECT_TO_GAME_HEADER, infoAsBuffer(), buffer);
        ByteBuffer dataToSend = getPlayerStateChangedHeaderBuffer(player);
        for (Player p : players) {
            if (p == player) {
                continue;
            }
            p.send(dataToSend.duplicate());
        }
        return true;
    }

    public void sendPacketToAll(ByteBuffer byteBuffer) {
        byteBuffer = byteBuffer.duplicate();
        for (Player player : players) {
            player.sendPacket(byteBuffer.duplicate());
        }
    }

    private void doHandleRequest(JRPRequest request) {
        if (state.isNot(State.RUNNING)) {
            request.response(GamiStatusCodes.GAME_BAD_STATE);
        }
        UserDetails userDetails = request.requester().attachment();
        script.onRequestReceived(this, findPlayerById(userDetails.id()), request);
    }


    private ByteBuffer getGameStateChangedHeader() {
        ByteBuffer dataToSend = headerBuffers.getBuffer();
        dataToSend.put(GamiMessageCodes.GAME_STATE_CHANGED);
        dataToSend.put(state.code());
        dataToSend.flip();
        return dataToSend;
    }

    private void doInitialize() {
        if (state.isNot(State.UNKNOWN)) {
            throw new IllegalStateException("bad state !");
        }
        State lastState = state;
        state = State.INITIALIZE;
        for (Player player : players) {
            if (player.getState().is(Player.State.DISCONNECTED)) {
                return;
            }
            ByteBuffer buffer = script.onGameStateChanged(lastState, this, player);
            player.send(getGameStateChangedHeader(), buffer);
        }
    }

    private void doRunGame() {
        if (state.isNotOneOf(State.INITIALIZE, State.PAUSE)) {
            throw new IllegalStateException("invalid state");
        }
        State lastState = state;
        state = State.RUNNING;
        for (Player player : players) {
            if (player.getState().is(Player.State.DISCONNECTED)) {
                return;
            }
            ByteBuffer buffer = script.onGameStateChanged(lastState, this, player);
            player.send(getGameStateChangedHeader(), buffer);
        }
    }

    private void doPauseGame() {
        if (state.isNot(State.RUNNING)) {
            throw new IllegalStateException("invalid state");
        }
        State lastState = state;
        state = State.PAUSE;
        for (Player player : players) {
            if (player.getState().is(Player.State.DISCONNECTED)) {
                return;
            }
            ByteBuffer buffer = script.onGameStateChanged(lastState, this, player);
            player.send(getGameStateChangedHeader(), buffer);
        }
    }

    private void doEndGame() {
        State lastState = state;
        state = State.END;
        for (Player player : players) {
            if (player.getState().is(Player.State.DISCONNECTED)) {
                return;
            }
            ByteBuffer buffer = script.onGameStateChanged(lastState, this, player);
            player.send(getGameStateChangedHeader(), buffer);
        }
    }

    private ByteBuffer infoAsBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        byte[] idAsBytes = id.getBytes();
        buffer.putInt(idAsBytes.length).put(idAsBytes);
        buffer.put(state.code());
        buffer.putInt(players.size());
        for (Player player : players) {
            buffer.put(player.infoAsBuffer());
        }
        buffer.flip();
        return buffer;
    }

}
