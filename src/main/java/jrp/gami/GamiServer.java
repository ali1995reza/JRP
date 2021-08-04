package jrp.gami;

import jrp.api.JRPEventListener;
import jrp.api.JRPRequest;
import jrp.api.JRPServer;
import jrp.api.JRPSession;
import jrp.impl.JRPServerImpl;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class GamiServer implements JRPEventListener {


    private final JRPServer server;
    private final List<JRPSession> finders = new ArrayList<>();

    public GamiServer(InetSocketAddress address) {
        this.server = new JRPServerImpl(address);
        this.server.registerRequestHandler(100, this::handleLogin);
        this.server.registerRequestHandler(200, this::handleFindGame);
        this.server.registerRequestHandler(300, this::stopFindGame);
    }

    private static String getAsString(ByteBuffer b) {
        byte[] data = new byte[b.remaining()];
        b.get(data);
        return new String(data);
    }


    private void handleLogin(JRPRequest request) {
        JRPSession session = request.requester();
        if (session.attachment() == null) {
            //todo handle
            //for now just get username
            session.attach(new UserDetails(id, getAsString(request.data()), session));
        } else {
            request.response(GamiStatusCodes.ALREADY_LOGGED_ID);
        }
    }


    private void handleFindGame(JRPRequest request) {
        UserDetails details = request.requester().attachment();
        if (details == null || details.state().isNot(UserDetails.State.LOGGED_ID)) {
            request.response(GamiStatusCodes.USER_BAD_STATE);
        } else {
            synchronized (finders) {
                if(!finders.isEmpty()) {
                    //todo create a game here

                }
            }
            //todo queue for finding game !
        }
    }


    private void stopFindGame(JRPRequest request) {
        UserDetails details = request.requester().attachment();
        if (details == null || details.state().isNot(UserDetails.State.FINDING_GAME)) {
            request.response(GamiStatusCodes.USER_BAD_STATE);
        } else {
            //todo queue for finding game !
        }
    }


    @Override
    public void onSessionOpened(JRPSession session) {
    }

    @Override
    public void onSessionClosed(JRPSession closedSession) {
        synchronized (finders) {
            finders.remove(closedSession);
        }
    }
}
