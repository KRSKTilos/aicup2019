package model;

import util.StreamUtil;

public abstract class CustomData {
    public abstract void writeTo(java.io.OutputStream stream) throws java.io.IOException;
    public static CustomData readFrom(java.io.InputStream stream) throws java.io.IOException {
        switch (StreamUtil.readInt(stream)) {
            case Log.TAG:
                return Log.readFrom(stream);
            case Rect.TAG:
                return Rect.readFrom(stream);
            case Line.TAG:
                return Line.readFrom(stream);
            case Polygon.TAG:
                return Polygon.readFrom(stream);
            case PlacedText.TAG:
                return PlacedText.readFrom(stream);
            default:
                throw new java.io.IOException("Unexpected discriminant value");
        }
    }

    public static class Log extends CustomData {
        public static final int TAG = 0;
        private String text;
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Log() {}
        public Log(String text) {
            this.text = text;
        }
        public static Log readFrom(java.io.InputStream stream) throws java.io.IOException {
            Log result = new Log();
            result.text = StreamUtil.readString(stream);
            return result;
        }
        @Override
        public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
            StreamUtil.writeInt(stream, TAG);
            StreamUtil.writeString(stream, text);
        }
    }

    public static class Rect extends CustomData {
        public static final int TAG = 1;
        private Vec2Float pos;
        public Vec2Float getPos() { return pos; }
        public void setPos(Vec2Float pos) { this.pos = pos; }
        private Vec2Float size;
        public Vec2Float getSize() { return size; }
        public void setSize(Vec2Float size) { this.size = size; }
        private ColorFloat color;
        public ColorFloat getColor() { return color; }
        public void setColor(ColorFloat color) { this.color = color; }
        public Rect() {}
        public Rect(Vec2Float pos, Vec2Float size, ColorFloat color) {
            this.pos = pos;
            this.size = size;
            this.color = color;
        }
        public static Rect readFrom(java.io.InputStream stream) throws java.io.IOException {
            Rect result = new Rect();
            result.pos = Vec2Float.readFrom(stream);
            result.size = Vec2Float.readFrom(stream);
            result.color = ColorFloat.readFrom(stream);
            return result;
        }
        @Override
        public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
            StreamUtil.writeInt(stream, TAG);
            pos.writeTo(stream);
            size.writeTo(stream);
            color.writeTo(stream);
        }
    }

    public static class Line extends CustomData {
        public static final int TAG = 2;
        private Vec2Float p1;
        public Vec2Float getP1() { return p1; }
        public void setP1(Vec2Float p1) { this.p1 = p1; }
        private Vec2Float p2;
        public Vec2Float getP2() { return p2; }
        public void setP2(Vec2Float p2) { this.p2 = p2; }
        private float width;
        public float getWidth() { return width; }
        public void setWidth(float width) { this.width = width; }
        private ColorFloat color;
        public ColorFloat getColor() { return color; }
        public void setColor(ColorFloat color) { this.color = color; }
        public Line() {}
        public Line(Vec2Float p1, Vec2Float p2, float width, ColorFloat color) {
            this.p1 = p1;
            this.p2 = p2;
            this.width = width;
            this.color = color;
        }
        public static Line readFrom(java.io.InputStream stream) throws java.io.IOException {
            Line result = new Line();
            result.p1 = Vec2Float.readFrom(stream);
            result.p2 = Vec2Float.readFrom(stream);
            result.width = StreamUtil.readFloat(stream);
            result.color = ColorFloat.readFrom(stream);
            return result;
        }
        @Override
        public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
            StreamUtil.writeInt(stream, TAG);
            p1.writeTo(stream);
            p2.writeTo(stream);
            StreamUtil.writeFloat(stream, width);
            color.writeTo(stream);
        }
    }

    public static class Polygon extends CustomData {
        public static final int TAG = 3;
        private ColoredVertex[] vertices;
        public ColoredVertex[] getVertices() { return vertices; }
        public void setVertices(ColoredVertex[] vertices) { this.vertices = vertices; }
        public Polygon() {}
        public Polygon(ColoredVertex[] vertices) {
            this.vertices = vertices;
        }
        public static Polygon readFrom(java.io.InputStream stream) throws java.io.IOException {
            Polygon result = new Polygon();
            result.vertices = new ColoredVertex[StreamUtil.readInt(stream)];
            for (int i = 0; i < result.vertices.length; i++) {
                result.vertices[i] = ColoredVertex.readFrom(stream);
            }
            return result;
        }
        @Override
        public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
            StreamUtil.writeInt(stream, TAG);
            StreamUtil.writeInt(stream, vertices.length);
            for (ColoredVertex verticesElement : vertices) {
                verticesElement.writeTo(stream);
            }
        }
    }

    public static class PlacedText extends CustomData {
        public static final int TAG = 4;
        private String text;
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        private Vec2Float pos;
        public Vec2Float getPos() { return pos; }
        public void setPos(Vec2Float pos) { this.pos = pos; }
        private TextAlignment alignment;
        public TextAlignment getAlignment() { return alignment; }
        public void setAlignment(TextAlignment alignment) { this.alignment = alignment; }
        private float size;
        public float getSize() { return size; }
        public void setSize(float size) { this.size = size; }
        private ColorFloat color;
        public ColorFloat getColor() { return color; }
        public void setColor(ColorFloat color) { this.color = color; }
        public PlacedText() {}
        public PlacedText(String text, Vec2Float pos, TextAlignment alignment, float size, ColorFloat color) {
            this.text = text;
            this.pos = pos;
            this.alignment = alignment;
            this.size = size;
            this.color = color;
        }
        public static PlacedText readFrom(java.io.InputStream stream) throws java.io.IOException {
            PlacedText result = new PlacedText();
            result.text = StreamUtil.readString(stream);
            result.pos = Vec2Float.readFrom(stream);
            switch (StreamUtil.readInt(stream)) {
            case 0:
                result.alignment = TextAlignment.LEFT;
                break;
            case 1:
                result.alignment = TextAlignment.CENTER;
                break;
            case 2:
                result.alignment = TextAlignment.RIGHT;
                break;
            default:
                throw new java.io.IOException("Unexpected discriminant value");
            }
            result.size = StreamUtil.readFloat(stream);
            result.color = ColorFloat.readFrom(stream);
            return result;
        }
        @Override
        public void writeTo(java.io.OutputStream stream) throws java.io.IOException {
            StreamUtil.writeInt(stream, TAG);
            StreamUtil.writeString(stream, text);
            pos.writeTo(stream);
            StreamUtil.writeInt(stream, alignment.discriminant);
            StreamUtil.writeFloat(stream, size);
            color.writeTo(stream);
        }
    }
}
