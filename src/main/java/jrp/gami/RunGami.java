package jrp.gami;

import jrp.gami.components.Game;

import java.net.InetSocketAddress;

public class RunGami {

    public static void main(String[] args) {
        GamiServer server = new GamiServer(new InetSocketAddress(5566));
    }

}
