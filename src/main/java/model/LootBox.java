package model;

import util.StreamUtil;

public class LootBox {
    private Vec2Double position;
    public Vec2Double getPosition() { return position; }
    public void setPosition(Vec2Double position) { this.position = position; }
    private Vec2Double size;
    public Vec2Double getSize() { return size; }
    public void setSize(Vec2Double size) { this.size = size; }
    private Item item;
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public LootBox() {}
    public LootBox(Vec2Double position, Vec2Double size, Item item) {
        this.position = position;
        this.size = size;
        this.item = item;
    }
    public static LootBox readFrom(java.io.InputStream stream) throws java.io.IOException {
        LootBox result = new LootBox();
        result.position = Vec2Double.readFrom(stream);
        result.size = Vec2Double.readFrom(stream);
        result.item = Item.readFrom(stream);
        return result;
    }
    public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
        position.writeTo(stream);
        size.writeTo(stream);
        item.writeTo(stream);
    }
}
