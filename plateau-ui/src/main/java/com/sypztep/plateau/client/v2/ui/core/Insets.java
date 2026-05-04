package com.sypztep.plateau.client.v2.ui.core;

public record Insets(int top, int right, int bottom, int left) {

    public static Insets none()                                     { return new Insets(0, 0, 0, 0); }
    public static Insets of(int all)                                { return new Insets(all, all, all, all); }
    public static Insets of(int vertical, int horizontal)           { return new Insets(vertical, horizontal, vertical, horizontal); }
    public static Insets of(int top, int right, int bottom, int left) { return new Insets(top, right, bottom, left); }

    public static Insets top(int v)    { return new Insets(v, 0, 0, 0); }
    public static Insets bottom(int v) { return new Insets(0, 0, v, 0); }
    public static Insets left(int v)   { return new Insets(0, 0, 0, v); }
    public static Insets right(int v)  { return new Insets(0, v, 0, 0); }

    public int horizontal() { return left + right; }
    public int vertical()   { return top + bottom; }

    public Insets add(Insets other) {
        return new Insets(top + other.top, right + other.right, bottom + other.bottom, left + other.left);
    }
}
