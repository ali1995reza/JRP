package jrp.impl;

import jrp.api.JRPRequest;
import jrp.api.JRPSession;
import jrp.api.ProtocolConstants;

import java.nio.ByteBuffer;

public class JRPRequestImpl implements JRPRequest {

    private final JRPSessionImpl session;
    private final ByteBuffer data;
    private final int nextToken;
    private final int requestId;
    private boolean respond = false;
    private final ThreadLocalBuffer headerBuffers;

    public JRPRequestImpl(JRPSessionImpl session, ByteBuffer data, int nextToken, int requestId) {
        this.session = session;
        this.data = data;
        this.nextToken = nextToken;
        System.out.println("NEXT TOKEN IS : " + nextToken);
        this.requestId = requestId;
        this.headerBuffers = new ThreadLocalBuffer(9);
    }


    @Override
    public int requestId() {
        return requestId;
    }

    @Override
    public ByteBuffer data() {
        return data;
    }

    private ByteBuffer getHeaderBuffer(int status) {
        ByteBuffer headerBuffer = headerBuffers.getBuffer();
        headerBuffer.clear();
        headerBuffer.put(ProtocolConstants.RESPONSE)
                .putInt(nextToken)
                .putInt(status);
        headerBuffer.flip();
        return headerBuffer;
    }

    @Override
    public synchronized void response(int status, ByteBuffer response) {
        if (respond)
            throw new IllegalStateException("already responded");
        respond = true;
        session.sendRaw(getHeaderBuffer(status), response);
    }

    @Override
    public synchronized void response(int status) {
        if (respond)
            throw new IllegalStateException("already responded");
        respond = true;
        session.sendRaw(getHeaderBuffer(status));
    }

    @Override
    public JRPSession requester() {
        return session;
    }
}
