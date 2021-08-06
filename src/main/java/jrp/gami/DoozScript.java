package jrp.gami;

import jrp.api.JRPRequest;
import jrp.gami.components.Game;
import jrp.gami.components.GameScript;
import jrp.gami.components.Player;

import java.nio.ByteBuffer;

public class DoozScript implements GameScript {

    @Override
    public void routine(Game game) {
    }

    @Override
    public void onRequestReceived(Game game, Player player, JRPRequest request) {
    }

    @Override
    public void onPlayerStateChanged(Player player) {
    }

    @Override
    public ByteBuffer onGameStateChanged(Game.State lastState, Game game, Player player) {
        return null;
    }

}
