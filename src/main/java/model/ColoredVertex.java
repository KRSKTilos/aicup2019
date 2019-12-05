package model;

import util.StreamUtil;

public class ColoredVertex {
    private Vec2Float position;
    public Vec2Float getPosition() { return position; }
    public void setPosition(Vec2Float position) { this.position = position; }
    private ColorFloat color;
    public ColorFloat getColor() { return color; }
    public void setColor(ColorFloat color) { this.color = color; }
    public ColoredVertex() {}
    public ColoredVertex(Vec2Float position, ColorFloat color) {
        this.position = position;
        this.color = color;
    }
    public static ColoredVertex readFrom(java.io.InputStream stream) throws java.io.IOException {
        ColoredVertex result = new ColoredVertex();
        result.position = Vec2Float.readFrom(stream);
        result.color = ColorFloat.readFrom(stream);
        return result;
    }
    public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
        position.writeTo(stream);
        color.writeTo(stream);
    }
}
