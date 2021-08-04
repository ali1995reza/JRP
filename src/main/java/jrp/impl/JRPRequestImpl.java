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

    public JRPRequestImpl(JRPSessionImpl session, ByteBuffer data, int nextToken, int requestId) {
        this.session = session;
        this.data = data;
        this.nextToken = nextToken;
        this.requestId = requestId;
    }


    @Override
    public int requestId() {
        return requestId;
    }

    @Override
    public ByteBuffer data() {
        return data;
    }

    @Override
    public synchronized void response(int status, ByteBuffer response) {
        if(respond)
            throw new IllegalStateException("already responded");
        respond = true;
        response = response.duplicate();
        ByteBuffer buffer = ByteBuffer.allocate(response.remaining()+4+1);
        buffer.put(ProtocolConstants.RESPONSE)
                .putInt(status)
                .put(response);
        buffer.flip();
        session.sendRaw(buffer);
    }

    @Override
    public synchronized void response(int status) {
        if(respond)
            throw new IllegalStateException("already responded");
        respond = true;
        ByteBuffer buffer = ByteBuffer.allocate(4+1);
        buffer.put(ProtocolConstants.RESPONSE)
                .putInt(status);
        buffer.flip();
        session.sendRaw(buffer);
    }

    @Override
    public JRPSession requester() {
        return session;
    }
}
