package jrp.gami.components;

import jrp.api.JRPRequest;

import java.nio.ByteBuffer;

public interface GameScript {

    void routine(Game game); //will call every one seconds !

    void onRequestReceived(Game game, Player player, JRPRequest request);

    void onPlayerStateChanged(Player player);

    ByteBuffer onGameStateChanged(Game.State lastState, Game game);




}
