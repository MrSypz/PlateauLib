package com.sypztep.plateau.client.v1.vfx.mesh;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A dedicated GPU texture atlas for {@link Anicca} textures, entirely
 * separate from vanilla's {@code minecraft:textures/atlas/blocks.png} atlas
 * — mesh-particle textures never compete for space in (or bloat) the atlas
 * every actual block/item in the game shares. Same mechanism vanilla itself
 * uses for its own named atlases (`blocks`, `items`, `particles`, ...): one
 * {@link TextureAtlas} plus a {@link SpriteLoader}-driven reload that
 * stitches whatever {@code textures/vfx_mesh/**} files exist across every
 * loaded namespace — vanilla's own atlas-owning class, {@code AtlasManager},
 * has a hardcoded, closed list of atlases with no extension point for mods,
 * so this replicates its `AtlasEntry.scheduleLoad`/`upload` pattern directly
 * rather than trying to register into it.
 *
 * <p>Sprite source: {@code assets/<ns>/atlases/vfx_mesh.json} — same
 * `sources`-array schema as vanilla's own {@code atlases/blocks.json};
 * point a {@code "minecraft:directory"} source at {@code "vfx_mesh"} with
 * prefix {@code "vfx_mesh/"} so texture files under
 * {@code assets/<ns>/textures/vfx_mesh/*.png} become sprites named
 * {@code <ns>:vfx_mesh/<name>} — the identifier an {@code Anicca}'s model
 * JSON should reference in its {@code "textures"} map.
 *
 * <p>No mipmaps (0 levels) — VFX meshes are typically close to the camera
 * and change often; the extra stitch time/memory a full mip chain costs
 * isn't worth it for this atlas the way it is for `blocks.png`.
 */
@Environment(EnvType.CLIENT)
public final class VfxAtlas {
    public static final Identifier LOCATION = Identifier.fromNamespaceAndPath("plateau-vfx", "textures/atlas/vfx_mesh.png");
    private static final Identifier DEFINITION = Identifier.fromNamespaceAndPath("plateau-vfx", "vfx_mesh");
    private static final Identifier RELOAD_LISTENER_ID = Identifier.fromNamespaceAndPath("plateau-vfx", "vfx_mesh_atlas");

    private static TextureAtlas atlas;
    private static boolean registered;

    private VfxAtlas() {}

    /** Call once at client init. Idempotent. */
    public static void register() {
        if (registered) return;
        registered = true;

        atlas = new TextureAtlas(LOCATION);
        Minecraft.getInstance().getTextureManager().register(LOCATION, atlas);
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(RELOAD_LISTENER_ID, new AtlasReloadListener());
    }

    /** The render type {@link Anicca} draws through — same cutout/cull entity pipeline vanilla block items use, bound to this atlas instead of `blocks.png`. */
    public static RenderType renderType() {
        return RenderTypes.entityCutoutCull(LOCATION);
    }

    static TextureAtlas atlas() {
        return atlas;
    }

    private static final class AtlasReloadListener implements PreparableReloadListener {
        @Override
        public CompletableFuture<Void> reload(SharedState currentReload, Executor taskExecutor,
                                               PreparationBarrier preparationBarrier, Executor reloadExecutor) {
            return SpriteLoader.create(atlas)
                    .loadAndStitch(currentReload.resourceManager(), DEFINITION, 0, taskExecutor, Set.of())
                    .thenCompose(preparations -> preparations.readyForUpload().thenApply(unused -> preparations))
                    .thenCompose(preparationBarrier::wait)
                    .thenAcceptAsync(atlas::upload, reloadExecutor);
        }
    }
}
