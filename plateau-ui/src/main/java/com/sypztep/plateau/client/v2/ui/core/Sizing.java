package com.sypztep.plateau.client.v2.ui.core;

public sealed interface Sizing permits Sizing.Fixed, Sizing.Fill, Sizing.Content {

    record Fixed(int value) implements Sizing {}
    record Fill(int weight) implements Sizing {}
    record Content() implements Sizing {}

    static Sizing fixed(int px) { return new Fixed(Math.max(0, px)); }
    static Sizing fill() { return new Fill(1); }
    static Sizing fill(int weight) { return new Fill(Math.max(0, weight)); }
    static Sizing content() { return new Content(); }

    default boolean isFill()    { return this instanceof Fill; }
    default boolean isFixed()   { return this instanceof Fixed; }
    default boolean isContent() { return this instanceof Content; }
}
