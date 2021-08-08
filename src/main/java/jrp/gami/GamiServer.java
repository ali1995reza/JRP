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
        this.server.registerRequestHandler(400, this::handleGameRequest);
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
            ByteBuffer b = ByteBuffer.allocate(8);
            b.putLong(username.hashCode());
            b.flip();
            request.response(ProtocolConstants.StatusCodes.OK, b);
        } else {
            request.response(GamiStatusCodes.USER_BAD_STATE);
        }
    }

    private void handleFindGame(JRPRequest request) {
        UserDetails details = request.requester().attachment();
        if (details == null || details.state().isNot(UserDetails.State.LOGGED_IN)) {
            request.response(GamiStatusCodes.USER_BAD_STATE);
        } else {
            synchronized (finders) {
                request.response(ProtocolConstants.StatusCodes.OK);
                if (!finders.isEmpty()) {
                    JRPSession otherPlayer = finders.remove(0);
                    Game game = new Game(UUID.randomUUID().toString(), Arrays.asList(otherPlayer.attachment(), request.requester().attachment()), new XOScript(), g -> {
                        routineCaller.register(g);
                        games.remove(g.id(), g);
                    });
                    games.put(game.id(), game);
                    if (game.initialize()) {
                        details.setGame(game)
                                .setState(UserDetails.State.CONNECTED_TO_GAME);
                        otherPlayer.attachment(UserDetails.class)
                                .setGame(game)
                                .setState(UserDetails.State.CONNECTED_TO_GAME);
                        game.handlePlayerConnect(otherPlayer);
                        game.handlePlayerConnect(request.requester());
                        routineCaller.register(game);
                    } else {
                        details.setState(UserDetails.State.LOGGED_IN);
                        otherPlayer.attachment(UserDetails.class)
                                .setState(UserDetails.State.LOGGED_IN);
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
                request.requester().attachment(UserDetails.class)
                        .setState(UserDetails.State.LOGGED_IN);
            }
        }
    }

    private void handleGameRequest(JRPRequest request) {
        UserDetails details = request.requester().attachment();
        if (details == null || details.state().isNot(UserDetails.State.FINDING_GAME)) {
            request.response(GamiStatusCodes.USER_BAD_STATE);
        } else {
            Game game = details.getGame();
            game.handleRequest(request);
        }
    }

    @Override
    public void onSessionOpened(JRPSession session) {
    }

    @Override
    public void onSessionClosed(JRPSession closedSession) {
        synchronized (finders) {
            finders.remove(closedSession);
            Game game = closedSession.attachment(UserDetails.class)
                    .getGame();
            if(game!=null) {
                game.handlePlayerDisconnect(closedSession);
            }
        }
    }

}
