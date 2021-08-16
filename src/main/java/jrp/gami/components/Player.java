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
    private final int inGameIndex;
    private final String username;
    private State state;
    private JRPSession session;
    private Object attachment;

    public Player(long id, int inGameIndex, String username, JRPSession session) {
        this.id = id;
        this.inGameIndex = inGameIndex;
        this.username = username;
        this.session = session;
        this.state = State.DISCONNECTED;
    }

    public int inGameIndex() {
        return inGameIndex;
    }

    final void setState(State state) {
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
        send(GAME_PACKET_HEADER, byteBuffer);
    }

    void send(ByteBuffer ... buffers) {
        if(state.is(State.CONNECTED)) {
            session.send(buffers);
        }
    }

    public void attach(Object o) {
        this.attachment = o;
    }

    public <T> T attachment() {
        return (T) attachment;
    }

    public <T> T attachment(Class<T> clazz) {
        return attachment();
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
