package jrp.gami;

import jrp.api.*;
import jrp.gami.components.Game;
import jrp.impl.JRPServerImpl;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GamiServer implements JRPEventListener {

    private final JRPServer server;
    private final List<JRPSession> finders = new ArrayList<>();
    private final GameRoutineCaller routineCaller;
    private final ConcurrentHashMap<String, Game> games = new ConcurrentHashMap<>();

    public GamiServer(InetSocketAddress address) {
        this.server = new JRPServerImpl(address);
        this.server.start();
        this.routineCaller = new GameRoutineCaller(5);
        this.server.registerRequestHandler(88,this::echo);
        this.server.registerRequestHandler(100, this::handleLogin);
        this.server.registerRequestHandler(200, this::handleFindGame);
        this.server.registerRequestHandler(300, this::stopFindGame);
    }

    private static String getAsString(ByteBuffer b) {
        byte[] data = new byte[b.remaining()];
        b.get(data);
        return new String(data);
    }

    private void echo(JRPRequest request) {
        System.out.println("ECHO");
        request.response(ProtocolConstants.StatusCodes.OK,
                request.data());
    }

    private void handleLogin(JRPRequest request) {
        JRPSession session = request.requester();
        if (session.attachment() == null) {
            //todo handle
            //for now just get username
            String username = getAsString(request.data());
            session.attach(new UserDetails(username.hashCode(), username, session));
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
                request.response(ProtocolConstants.StatusCodes.OK);
                if (!finders.isEmpty()) {
                    JRPSession otherPlayer = finders.remove(0);
                    Game game = new Game(UUID.randomUUID().toString(), Arrays.asList(otherPlayer.attachment(), request.requester().attachment()), new DoozScript(), g -> {
                        routineCaller.register(g);
                        games.remove(g.id(), g);
                    });
                    games.put(game.id(), game);
                    if (game.initialize()) {
                        routineCaller.register(game);
                    }
                } else {
                    details.setState(UserDetails.State.FINDING_GAME);
                    finders.add(request.requester());
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
            synchronized (finders) {
                request.response(ProtocolConstants.StatusCodes.OK);
                finders.remove(request.requester());
            }
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
