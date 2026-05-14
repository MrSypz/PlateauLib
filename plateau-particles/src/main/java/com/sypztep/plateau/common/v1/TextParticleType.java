package com.sypztep.plateau.common.v1;

import net.minecraft.resources.Identifier;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public final class TextParticleType {
    private final Identifier id;
    private final String translationKey;
    private final float maxSize;
    private final float yPos;
    private final IntSupplier colorSupplier;
    private final BooleanSupplier enabledCheck;

    private TextParticleType(Identifier id, String translationKey, float maxSize, float yPos,
                             IntSupplier colorSupplier, BooleanSupplier enabledCheck) {
        this.id = id;
        this.translationKey = translationKey;
        this.maxSize = maxSize;
        this.yPos = yPos;
        this.colorSupplier = colorSupplier;
        this.enabledCheck = enabledCheck;
    }

    public Identifier id()                 { return id; }
    public String translationKey()         { return translationKey; }
    public float maxSize()                 { return maxSize; }
    public float yPos()                    { return yPos; }
    public int color()                     { return colorSupplier.getAsInt(); }
    public boolean isEnabled()             { return enabledCheck.getAsBoolean(); }

    public static Builder builder(Identifier id, String translationKey) {
        return new Builder(id, translationKey);
    }

    public static final class Builder {
        private final Identifier id;
        private final String translationKey;
        private float maxSize     = -0.045f;
        private float yPos        = 0f;
        private IntSupplier     colorSupplier = () -> 0xFFFFFFFF;
        private BooleanSupplier enabledCheck  = () -> true;

        private Builder(Identifier id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        public Builder maxSize(float v)          { this.maxSize = v; return this; }
        public Builder yPos(float v)             { this.yPos = v; return this; }
        public Builder color(IntSupplier s)      { this.colorSupplier = s; return this; }
        public Builder enabled(BooleanSupplier s){ this.enabledCheck = s; return this; }

        public TextParticleType build() {
            return new TextParticleType(id, translationKey, maxSize, yPos, colorSupplier, enabledCheck);
        }
    }
}
