package model;

import util.StreamUtil;

public class Game {
    private int currentTick;
    public int getCurrentTick() { return currentTick; }
    public void setCurrentTick(int currentTick) { this.currentTick = currentTick; }
    private Properties properties;
    public Properties getProperties() { return properties; }
    public void setProperties(Properties properties) { this.properties = properties; }
    private Level level;
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }
    private Player[] players;
    public Player[] getPlayers() { return players; }
    public void setPlayers(Player[] players) { this.players = players; }
    private Unit[] units;
    public Unit[] getUnits() { return units; }
    public void setUnits(Unit[] units) { this.units = units; }
    private Bullet[] bullets;
    public Bullet[] getBullets() { return bullets; }
    public void setBullets(Bullet[] bullets) { this.bullets = bullets; }
    private Mine[] mines;
    public Mine[] getMines() { return mines; }
    public void setMines(Mine[] mines) { this.mines = mines; }
    private LootBox[] lootBoxes;
    public LootBox[] getLootBoxes() { return lootBoxes; }
    public void setLootBoxes(LootBox[] lootBoxes) { this.lootBoxes = lootBoxes; }
    public Game() {}
    public Game(int currentTick, Properties properties, Level level, Player[] players, Unit[] units, Bullet[] bullets, Mine[] mines, LootBox[] lootBoxes) {
        this.currentTick = currentTick;
        this.properties = properties;
        this.level = level;
        this.players = players;
        this.units = units;
        this.bullets = bullets;
        this.mines = mines;
        this.lootBoxes = lootBoxes;
    }
    public static Game readFrom(java.io.InputStream stream) throws java.io.IOException {
        Game result = new Game();
        result.currentTick = StreamUtil.readInt(stream);
        result.properties = Properties.readFrom(stream);
        result.level = Level.readFrom(stream);
        result.players = new Player[StreamUtil.readInt(stream)];
        for (int i = 0; i < result.players.length; i++) {
            result.players[i] = Player.readFrom(stream);
        }
        result.units = new Unit[StreamUtil.readInt(stream)];
        for (int i = 0; i < result.units.length; i++) {
            result.units[i] = Unit.readFrom(stream);
        }
        result.bullets = new Bullet[StreamUtil.readInt(stream)];
        for (int i = 0; i < result.bullets.length; i++) {
            result.bullets[i] = Bullet.readFrom(stream);
        }
        result.mines = new Mine[StreamUtil.readInt(stream)];
        for (int i = 0; i < result.mines.length; i++) {
            result.mines[i] = Mine.readFrom(stream);
        }
        result.lootBoxes = new LootBox[StreamUtil.readInt(stream)];
        for (int i = 0; i < result.lootBoxes.length; i++) {
            result.lootBoxes[i] = LootBox.readFrom(stream);
        }
        return result;
    }
    public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
        StreamUtil.writeInt(stream, currentTick);
        properties.writeTo(stream);
        level.writeTo(stream);
        StreamUtil.writeInt(stream, players.length);
        for (Player playersElement : players) {
            playersElement.writeTo(stream);
        }
        StreamUtil.writeInt(stream, units.length);
        for (Unit unitsElement : units) {
            unitsElement.writeTo(stream);
        }
        StreamUtil.writeInt(stream, bullets.length);
        for (Bullet bulletsElement : bullets) {
            bulletsElement.writeTo(stream);
        }
        StreamUtil.writeInt(stream, mines.length);
        for (Mine minesElement : mines) {
            minesElement.writeTo(stream);
        }
        StreamUtil.writeInt(stream, lootBoxes.length);
        for (LootBox lootBoxesElement : lootBoxes) {
            lootBoxesElement.writeTo(stream);
        }
    }
}
