package jrp.gami.components;

import jrp.api.JRPRequest;
import jrp.api.JRPSession;
import jrp.gami.GamiMessageCodes;
import jrp.gami.GamiStatusCodes;
import jrp.gami.UserDetails;

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

    private final List<Player> players;
    private final GameScript script;
    private final Object _lock = new Object();
    private State state = State.UNKNOWN;
    private final String id;
    private final Consumer<Game> endNotifier;

    public Game(String id, List<UserDetails> pl, GameScript script, Consumer<Game> endNotifier) {
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
        ByteBuffer dataToSend = ByteBuffer.allocate(1 + 8);
        dataToSend.put(GamiMessageCodes.PLAYER_DISCONNECT);
        dataToSend.putLong(player.getId());
        dataToSend.flip();
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
        ByteBuffer gameInfo = infoAsBuffer();
        ByteBuffer dataToSend = ByteBuffer.allocate(gameInfo.remaining() + buffer.remaining() + 1 + 4);
        dataToSend.put(GamiMessageCodes.CONNECT_TO_GAME);
        dataToSend.putInt(gameInfo.remaining());
        dataToSend.put(gameInfo);
        dataToSend.put(buffer);
        dataToSend.flip();
        player.send(dataToSend);
        dataToSend = ByteBuffer.allocate(1 + 8);
        dataToSend.put(GamiMessageCodes.PLAYER_CONNECT);
        dataToSend.putLong(player.getId());
        dataToSend.flip();
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
            ByteBuffer dataToSend = ByteBuffer.allocate(buffer.remaining() + 1);
            dataToSend.put(GamiMessageCodes.GAME_INITIALIZED);
            dataToSend.put(buffer);
            dataToSend.flip();
            player.send(dataToSend);
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
            ByteBuffer dataToSend = ByteBuffer.allocate(buffer.remaining() + 1);
            dataToSend.put(GamiMessageCodes.GAME_RUN);
            dataToSend.put(buffer);
            dataToSend.flip();
            player.send(dataToSend);
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
            ByteBuffer dataToSend = ByteBuffer.allocate(buffer.remaining() + 1);
            dataToSend.put(GamiMessageCodes.GAME_PAUSED);
            dataToSend.put(buffer);
            dataToSend.flip();
            player.send(dataToSend);
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
            ByteBuffer dataToSend = ByteBuffer.allocate(buffer.remaining() + 1);
            dataToSend.put(GamiMessageCodes.GAME_END);
            dataToSend.put(buffer);
            dataToSend.flip();
            player.send(dataToSend);
        }
    }

    private ByteBuffer infoAsBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        byte[] idAsBytes = id.getBytes();
        buffer.putInt(idAsBytes.length).put(idAsBytes);
        buffer.put(state.code());
        for (Player player : players) {
            buffer.put(player.infoAsBuffer());
        }
        buffer.flip();
        return buffer;
    }

}
