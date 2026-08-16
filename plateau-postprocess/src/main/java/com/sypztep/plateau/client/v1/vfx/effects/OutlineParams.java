package com.sypztep.plateau.client.v1.vfx.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Parameters for one {@link OutlineEffect#requestFrame} call. With {@code
 * maskGroup == null} (the default), this is a whole-scene depth-discontinuity
 * edge detect. Pass a group name from {@link
 * com.sypztep.plateau.client.v1.vfx.VfxMaskGroups} via {@link #withMaskGroup}
 * to restrict outlining to just that group's drawn silhouette instead of
 * every depth edge in the scene.
 */
@Environment(EnvType.CLIENT)
public record OutlineParams(float thickness, float depthSensitivity, float colorR, float colorG, float colorB,
                             @Nullable Identifier maskGroup) {
    public static final OutlineParams DEFAULT = new OutlineParams(1.0f, 800.0f, 0f, 0f, 0f, null);

    /** Same params, restricted to the given {@link com.sypztep.plateau.client.v1.vfx.VfxMaskGroups} group. */
    public OutlineParams withMaskGroup(Identifier group) {
        return new OutlineParams(thickness, depthSensitivity, colorR, colorG, colorB, group);
    }
}
