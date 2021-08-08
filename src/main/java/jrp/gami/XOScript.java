package jrp.gami;

import jrp.api.JRPRequest;
import jrp.api.ProtocolConstants;
import jrp.gami.components.Game;
import jrp.gami.components.GameScript;
import jrp.gami.components.Player;

import java.nio.ByteBuffer;

public class XOScript implements GameScript {

    @Override
    public void routine(Game game) {
        if (game.state().is(Game.State.INITIALIZE)) {
            game.run();
        }
    }

    @Override
    public void onRequestReceived(Game game, Player player, JRPRequest request) {
        request.response(ProtocolConstants.StatusCodes.OK);
    }

    @Override
    public ByteBuffer onPlayerStateChanged(Player player) {
        return ByteBuffer.allocate(0);
    }

    @Override
    public ByteBuffer onGameStateChanged(Game.State lastState, Game game, Player player) {
        return ByteBuffer.allocate(0);
    }

}
