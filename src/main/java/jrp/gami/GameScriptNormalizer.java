package jrp.gami;

import jrp.api.JRPRequest;
import jrp.gami.components.Game;
import jrp.gami.components.GameScript;
import jrp.gami.components.Player;

import java.nio.ByteBuffer;

public final class GameScriptNormalizer implements GameScript {

    private final static ByteBuffer EMPTY = ByteBuffer.allocate(0).asReadOnlyBuffer();


    public final static GameScript wrap(GameScript script) {
        return new GameScriptNormalizer(script);
    }

    private final GameScript wrapped;

    private GameScriptNormalizer(GameScript wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void routine(Game game) {
        wrapped.routine(game);
    }

    @Override
    public void onRequestReceived(Game game, Player player, JRPRequest request) {
        wrapped.onRequestReceived(game, player, request);
    }

    @Override
    public ByteBuffer onPlayerConnected(Game game, Player player) {
        ByteBuffer buffer = wrapped.onPlayerConnected(game, player);
        return buffer==null?EMPTY:buffer;
    }

    @Override
    public void onPlayerDisconnected(Game game, Player player) {
        wrapped.onPlayerDisconnected(game, player);
    }

    @Override
    public ByteBuffer onGameStateChanged(Game.State lastState, Game game, Player player) {
        ByteBuffer buffer = wrapped.onGameStateChanged(lastState, game, player);
        return buffer==null?EMPTY:buffer;
    }

}
