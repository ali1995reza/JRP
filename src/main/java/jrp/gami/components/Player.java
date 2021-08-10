package jrp.gami.components;

import jrp.api.JRPSession;
import jrp.gami.GamiMessageCodes;

import java.nio.ByteBuffer;

public class Player {

    public enum State {
        CONNECTED((byte)1), DISCONNECTED((byte)0);

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

        public boolean isNot(State state) {
            return this != state;
        }
    }

    private final ByteBuffer GAME_PACKET_HEADER =
            ByteBuffer.wrap(new byte[]{GamiMessageCodes.GAME_PACKET});

    private final long id;
    private final String username;
    private State state;
    private JRPSession session;

    public Player(long id, String username, JRPSession session) {
        this.id = id;
        this.username = username;
        this.session = session;
        this.state = State.DISCONNECTED;
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

    public void sendPacket(ByteBuffer byteBuffer) {
        if(state.is(State.DISCONNECTED))
            return;
        ByteBuffer buffer = ByteBuffer.allocate(byteBuffer.remaining()+1);
        buffer.put(GamiMessageCodes.GAME_PACKET);
        buffer.put(byteBuffer);
        buffer.flip();
        send(byteBuffer);
    }

    void send(ByteBuffer ... buffers) {
        if(state.is(State.CONNECTED)) {
            session.send(buffers);
        }
    }

    ByteBuffer infoAsBuffer() {
        byte[] nameBytes = username.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(nameBytes.length+4+1+8);
        buffer.putLong(id)
                .put(state.code())
                .putInt(nameBytes.length)
                .put(nameBytes);
        buffer.flip();
        return buffer;
    }
}
