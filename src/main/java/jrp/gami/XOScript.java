package jrp.gami;

import jrp.api.JRPRequest;
import jrp.api.ProtocolConstants;
import jrp.gami.components.Game;
import jrp.gami.components.GameScript;
import jrp.gami.components.Player;

import java.nio.ByteBuffer;
import java.util.Random;

public class XOScript implements GameScript {

    private enum Piece{
        X((byte)1),O((byte)0);

        private final byte code;

        Piece(byte code) {
            this.code = code;
        }

        public byte code() {
            return code;
        }
    }

    private static class Board {
        private Piece[] board = new Piece[9];

        public boolean set(int index, Piece what) {
            if(index<0 || index>8)
                return false;
            if(board[index]!=null)
                return false;
            board[index] = what;
            return true;
        }

        public Piece getWinner() {
            if(board[0]!=null && board[0]==board[1] && board[0]==board[2]) {
                return board[0];
            }

            if(board[0]!=null && board[0]==board[3] && board[0]==board[6]) {
                return board[0];
            }

            if(board[3]!=null && board[3]==board[4] && board[3]==board[5]) {
                return board[3];
            }

            if(board[1]!=null && board[1]==board[4] && board[1]==board[7]) {
                return board[1];
            }

            if(board[6]!=null && board[6]==board[7] && board[6]==board[8]) {
                return board[6];
            }

            if(board[2]!=null && board[2]==board[5] && board[2]==board[8]) {
                return board[2];
            }

            return null;
        }

        public boolean hasEmptyPlace() {
            for(Piece piece:board) {
                if(piece==null)
                    return true;
            }

            return false;
        }

        @Override
        public String toString() {
            String str = "";
            for(int i=0;i<9;i++) {
                str+=board[i]==null?".":board[i];
                if((i+1)%3==0)
                {
                    str+="\n";
                }
            }
            return str;
        }
    }


    private final Random random = new Random();
    private final Board board = new Board();
    private Piece currentRound = Piece.X;

    @Override
    public void routine(Game game) {
        if (game.state().is(Game.State.INITIALIZE)) {
            game.run();
        }
    }

    @Override
    public void onRequestReceived(Game game, Player player, JRPRequest request) {
        if(currentRound==player.attachment(Piece.class)) {
            int index = request.data().getInt();
            if(board.set(index, currentRound)) {
                ByteBuffer buffer = game.getBuffer()
                        .putInt(index)
                        .put(currentRound.code());
                buffer.flip();
                game.sendPacketToAll(buffer);
                request.response(ProtocolConstants.StatusCodes.OK);
                currentRound = currentRound==Piece.X?Piece.O:Piece.X;
                System.out.println(board);
                if(board.getWinner()!=null || !board.hasEmptyPlace()) {
                    System.out.println("WTF");
                    game.end();
                }
            } else {
                request.response(GamiStatusCodes.GAME_BAD_STATE);
            }
        } else  {
            request.response(GamiStatusCodes.USER_BAD_STATE);
        }
    }

    @Override
    public ByteBuffer onPlayerConnected(Game game, Player player) {
        ByteBuffer packet = game.getBuffer();
        packet.put(player.attachment(Piece.class).code());
        packet.flip();
        return packet;
    }

    @Override
    public void onPlayerDisconnected(Game game, Player player) {
        game.end();
    }

    @Override
    public ByteBuffer onGameStateChanged(Game.State lastState, Game game, Player player) {
        if(game.state().is(Game.State.INITIALIZE)){
            if(player.inGameIndex()==0) {
                boolean x = random.nextBoolean();
                player.attach(x?Piece.X:Piece.O);
            } else {
                Player otherPlayer = game.players().get(0);
                Piece otherPiece = otherPlayer.attachment();
                player.attach(otherPiece==Piece.X?Piece.O:Piece.X);
            }
            return null;
        } else if(game.state().is(Game.State.END)) {
            Piece winner = board.getWinner();
            if(winner!=null) {
                return game.getBuffer().put(winner.code()).flip();
            }
            else return game.getBuffer().put((byte) -1).flip();
        } else {
            return null;
        }
    }

}
