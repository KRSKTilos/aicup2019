package model;

import util.StreamUtil;

public class ServerMessageGame {
    private PlayerView playerView;
    public PlayerView getPlayerView() { return playerView; }
    public void setPlayerView(PlayerView playerView) { this.playerView = playerView; }
    public ServerMessageGame() {}
    public ServerMessageGame(PlayerView playerView) {
        this.playerView = playerView;
    }
    public static ServerMessageGame readFrom(java.io.InputStream stream) throws java.io.IOException {
        ServerMessageGame result = new ServerMessageGame();
        if (StreamUtil.readBoolean(stream)) {
            result.playerView = PlayerView.readFrom(stream);
        } else {
            result.playerView = null;
        }
        return result;
    }
    public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
        if (playerView == null) {
            StreamUtil.writeBoolean(stream, false);
        } else {
            StreamUtil.writeBoolean(stream, true);
            playerView.writeTo(stream);
        }
    }
}
