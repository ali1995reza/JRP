package jrp.impl;

import jrp.api.JRPEventListener;
import jrp.api.JRPServer;
import jrp.api.ProtocolConstants;
import jrp.api.RequestHandler;
import jrp.utils.transport.nio.implement.NIOProcessor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class JRPServerImpl implements JRPServer {

    private final NIOProcessor processor;
    private final ConcurrentHashMap<Integer, RequestHandler> handlers;
    private final JRPEventListenerRef eventListener = new JRPEventListenerRef();
    private final JRPSocketServerIoHandler serverIoHandler;


    public JRPServerImpl(InetSocketAddress address) {
        this.processor = new NIOProcessor();
        this.handlers = new ConcurrentHashMap<>();
        serverIoHandler = new JRPSocketServerIoHandler(address, eventListener, this::handlePacket);
    }

    @Override
    public void start() {
        processor.start();
        try {
            processor.registerIoHandler(serverIoHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        processor.stop();
    }

    @Override
    public void registerRequestHandler(int requestId, RequestHandler handler) {
        handlers.put(requestId, handler);
    }

    @Override
    public void addEventListener(JRPEventListener eventListener) {
        this.eventListener.setRef(eventListener);
    }

    private void handlePacket(JRPSessionImpl session, ByteBuffer packet) {
        int token = packet.getInt();
        int requestId = packet.getInt();
        if (!session.sessionDetails().fireToken(token))
            throw new IllegalStateException("invalid token");

        JRPRequestImpl request = new JRPRequestImpl(
                session, packet,
                token, requestId
        );

        RequestHandler handler = handlers.get(requestId);

        if(handler==null) {
            request.response(ProtocolConstants.StatusCodes.REQUEST_ID_NOT_FOUNT);
        } else {
            handler.handle(request);
        }
    }
}
