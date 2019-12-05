package model;

import util.StreamUtil;

public class Level {
    private Tile[][] tiles;
    public Tile[][] getTiles() { return tiles; }
    public void setTiles(Tile[][] tiles) { this.tiles = tiles; }
    public Level() {}
    public Level(Tile[][] tiles) {
        this.tiles = tiles;
    }
    public static Level readFrom(java.io.InputStream stream) throws java.io.IOException {
        Level result = new Level();
        result.tiles = new Tile[StreamUtil.readInt(stream)][];
        for (int i = 0; i < result.tiles.length; i++) {
            result.tiles[i] = new Tile[StreamUtil.readInt(stream)];
            for (int j = 0; j < result.tiles[i].length; j++) {
                switch (StreamUtil.readInt(stream)) {
                case 0:
                    result.tiles[i][j] = Tile.EMPTY;
                    break;
                case 1:
                    result.tiles[i][j] = Tile.WALL;
                    break;
                case 2:
                    result.tiles[i][j] = Tile.PLATFORM;
                    break;
                case 3:
                    result.tiles[i][j] = Tile.LADDER;
                    break;
                case 4:
                    result.tiles[i][j] = Tile.JUMP_PAD;
                    break;
                default:
                    throw new java.io.IOException("Unexpected discriminant value");
                }
            }
        }
        return result;
    }
    public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
        StreamUtil.writeInt(stream, tiles.length);
        for (Tile[] tilesElement : tiles) {
            StreamUtil.writeInt(stream, tilesElement.length);
            for (Tile tilesElementElement : tilesElement) {
                StreamUtil.writeInt(stream, tilesElementElement.discriminant);
            }
        }
    }
}
