package com.sypztep.plateau.client.v1.vfx.mesh;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * EnKreta — plateau-vfx's non-procedural mesh particle system: a
 * vanilla-schema block model JSON (the same {@code elements}/{@code faces}/
 * {@code textures} format used by blocks/items — author it in Blockbench
 * with the "Minecraft: Java Block/Item" project type and export as-is)
 * turned into a flat {@code List<BakedQuad>} a caller can draw with an
 * arbitrary per-instance {@link PoseStack.Pose}, the same way
 * {@link PrimitiveMeshes} draws procedural shapes.
 *
 * <p><b>Deliberately bypasses vanilla's blockstate/item model-discovery
 * graph — and its file location, not just its loading mechanism.</b>
 * {@code fabric-model-loading-api-v1}'s {@code ExtraModelKey} mechanism was
 * tried first and doesn't fit: it resolves through the same {@code
 * ModelDiscovery} vanilla uses for actual blockstates, which only knows
 * about models reachable from a blockstate/item root. A model with no such
 * root (this class's whole use case) hits {@code ModelDiscovery}'s "not
 * discovered previously" fallback — silently baking to an empty/missing
 * model instead of failing loudly. This class instead reads and bakes the
 * JSON itself: {@link CuboidModel#fromStream} for parsing (pure, no registry
 * involved) and {@link FaceBakery#bakeQuad} (a static method needing only a
 * {@link ModelBaker.Interner}, not a full {@code ModelBaker}) for baking —
 * both already used internally by vanilla's own pipeline, just called
 * directly instead of through the blockstate-shaped API wrapped around them.
 * Assets also live under their own {@code vfx_mesh/} root, not vanilla's
 * {@code models/} — reusing that folder would still work mechanically
 * (this class never touches vanilla's own bulk model scan of it either
 * way), but keeping a fully separate path makes the "not part of the
 * blockstate system" property visible at the file layout, not just in the
 * loading code.
 *
 * <p><b>Textures</b> live under {@code textures/vfx_mesh/...} and bake
 * against {@link VfxAtlas}, plateau-vfx's own dedicated GPU atlas — not
 * vanilla's {@code blocks.png}. Sprite stitching itself is still vanilla's
 * {@link SpriteLoader} machinery ({@link VfxAtlas} just points it at a
 * separate atlas/definition); only the model-file lookup and the choice of
 * atlas are sidestepped from vanilla's block/item system.
 *
 * <p><b>Element size limit — inherited from vanilla, not our own choice.</b>
 * {@link CuboidModel#fromStream} parses each element with vanilla's own
 * {@code CuboidModelElement.Deserializer}, unmodified — which hard-rejects
 * any {@code from}/{@code to} coordinate outside {@code [-16, 32]} (raw
 * Blockbench units, thrown as a {@code JsonParseException}, caught by
 * {@link #bake} and logged as a parse failure rather than propagated).
 * That's a per-axis span of 48 raw units, i.e. **3×3×3 real blocks** after
 * {@code FaceBakery}'s implicit {@code /16} — a single element can't be
 * larger than that no matter how this class's own code changes, only
 * vanilla's parser could lift it. Flagging this now since it hasn't caused
 * a problem yet: a VFX mesh wanting to span more than ~3 blocks per element
 * needs multiple elements (still one model/one {@code EnKreta}) or multiple
 * {@code EnKreta} instances composed by the caller — revisit if that ever
 * becomes a real constraint (would mean writing a from-scratch element
 * parser instead of reusing {@link CuboidModel}, a real scope increase).
 *
 * <p><b>Reload safety, no extra listener registration</b>: {@link #quads()}
 * compares {@link Minecraft#getResourceManager()}'s current instance against
 * the one last baked with (MC swaps in a new instance only once a full
 * reload — including texture-atlas stitching — has completed, so by the
 * time this comparison can observe a new instance, the atlas is already
 * safe to sample). Re-bakes only on that change, self-healing across F3+T
 * without a dedicated {@code PreparableReloadListener} or the ordering
 * concerns one would need against the vanilla texture-stitch listener.
 *
 * <p><b>Memory</b>: a bake's {@code Vector3fc} corner points and {@code
 * BakedQuad.MaterialInfo} records are interned within that one bake call
 * (small elements share exact corner coordinates and, usually, one
 * material) so the cached quad list doesn't carry duplicate small objects;
 * the interning maps themselves are call-scoped, not kept as fields.
 * {@link #draw} reuses one {@link QuadInstance} per {@code EnKreta}
 * instead of allocating one per particle per frame.
 */
@Environment(EnvType.CLIENT)
public final class EnKreta {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlateauVfx/EnKreta");
    private static final FileToIdConverter MODEL_FILES = FileToIdConverter.json("vfx_mesh");

    private final Identifier modelId;
    private final QuadInstance scratchInstance = new QuadInstance();
    private ResourceManager lastResourceManager;
    private List<BakedQuad> quads = List.of();

    private EnKreta(Identifier modelId) {
        this.modelId = modelId;
    }

    /**
     * {@code modelId} points at a model file the same way vanilla points at
     * a block model — {@code plateau-vfx:block/my_shape} for an asset at
     * {@code assets/plateau-vfx/vfx_mesh/block/my_shape.json} — but under
     * this module's own {@code vfx_mesh/} root, and read directly from the
     * resource pack, never through the blockstate graph.
     */
    public static EnKreta of(Identifier modelId) {
        return new EnKreta(modelId);
    }

    /**
     * Draws every quad of this mesh with one uniform {@code color}/{@code
     * light} for all vertices, at whatever transform {@code pose} encodes
     * (translate/rotate/scale the {@link PoseStack} before reading {@code
     * pose} — same call pattern as {@link PrimitiveMeshes}). No-op if the
     * model failed to bake (logged once per resource reload, not spammed).
     */
    public void draw(PoseStack.Pose pose, VertexConsumer buffer, int color, int light) {
        List<BakedQuad> current = quads();
        if (current.isEmpty()) return;

        scratchInstance.setColor(color);
        scratchInstance.setLightCoords(light);
        for (BakedQuad quad : current) {
            buffer.putBakedQuad(pose, quad, scratchInstance);
        }
    }

    /** Diagnostics: how many quads this mesh currently has baked. */
    public int quadCount() {
        return quads().size();
    }

    private List<BakedQuad> quads() {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (resourceManager != lastResourceManager) {
            lastResourceManager = resourceManager;
            quads = bake(resourceManager);
        }
        return quads;
    }

    private List<BakedQuad> bake(ResourceManager resourceManager) {
        Identifier fileLocation = MODEL_FILES.idToFile(modelId);
        Optional<Resource> resource = resourceManager.getResource(fileLocation);
        if (resource.isEmpty()) {
            LOGGER.warn("[EnKreta] {} not found (looked for {})", modelId, fileLocation);
            return List.of();
        }

        CuboidModel model;
        try (BufferedReader reader = resource.get().openAsReader()) {
            // NOTE: this is where vanilla's [-16, 32]-per-axis element size
            // limit gets enforced (see class doc "Element size limit") — a
            // JsonParseException from an oversized element lands in this
            // catch, not as a crash.
            model = CuboidModel.fromStream(reader);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("[EnKreta] failed to parse {}", fileLocation, e);
            return List.of();
        }

        UnbakedGeometry geometry = model.geometry();
        if (!(geometry instanceof UnbakedCuboidGeometry cuboidGeometry)) {
            LOGGER.warn("[EnKreta] {} has no 'elements'", modelId);
            return List.of();
        }

        TextureAtlas blockAtlas = VfxAtlas.atlas();
        if (blockAtlas == null) {
            // VfxAtlas.register() hasn't been called yet, or this is running
            // before its first reload has stitched anything.
            LOGGER.warn("[EnKreta] {}: VfxAtlas not ready yet", modelId);
            return List.of();
        }
        TextureSlots textures = new TextureSlots.Resolver().addLast(model.textureSlots()).resolve(modelId::toString);
        Map<Vector3fc, Vector3fc> vectorCache = new HashMap<>();
        Map<BakedQuad.MaterialInfo, BakedQuad.MaterialInfo> materialCache = new HashMap<>();
        ModelBaker.Interner interner = new ModelBaker.Interner() {
            @Override
            public Vector3fc vector(Vector3fc vector) {
                return vectorCache.computeIfAbsent(vector, v -> v);
            }

            @Override
            public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material) {
                return materialCache.computeIfAbsent(material, m -> m);
            }
        };

        List<BakedQuad> result = new ArrayList<>();
        boolean loggedSprite = false;
        for (CuboidModelElement element : cuboidGeometry.elements()) {
            for (Map.Entry<Direction, CuboidFace> entry : element.faces().entrySet()) {
                Direction facing = entry.getKey();
                CuboidFace face = entry.getValue();

                // Two distinct ways a texture can end up missing, both handled the
                // same way (fall back to vanilla's own purple/black missing-texture
                // sprite, still draw the geometry, log why):
                //  1. The slot has no mapping at all in "textures" (typo, or
                //     genuinely unassigned) — textures.getMaterial() returns null.
                //  2. The slot resolves to a real Identifier, but no texture file
                //     was ever stitched at that path — TextureAtlas.getSprite()
                //     *silently* substitutes its own missingSprite with no log of
                //     its own, so this case is otherwise invisible without checking
                //     the returned sprite's identity ourselves.
                Material material = textures.getMaterial(face.texture());
                TextureAtlasSprite sprite = material != null
                        ? blockAtlas.getSprite(material.sprite())
                        : blockAtlas.missingSprite();
                if (material == null) {
                    LOGGER.warn("[EnKreta] {}: unresolved texture reference '{}', using missing-texture sprite",
                            modelId, face.texture());
                } else if (sprite == blockAtlas.missingSprite()) {
                    LOGGER.warn("[EnKreta] {}: texture '{}' not found (looked for {}), using missing-texture sprite",
                            modelId, face.texture(), material.sprite());
                }
                if (!loggedSprite) {
                    loggedSprite = true;
                    LOGGER.info("[EnKreta] {}: face texture '{}' -> sprite {}",
                            modelId, face.texture(), sprite.contents().name());
                }

                boolean forceTranslucent = material != null && material.forceTranslucent();
                Material.Baked bakedMaterial = new Material.Baked(sprite, forceTranslucent);
                Transparency transparency = forceTranslucent ? Transparency.TRANSLUCENT : Transparency.NONE;
                BakedQuad.MaterialInfo materialInfo = BakedQuad.MaterialInfo.of(
                        bakedMaterial, transparency, face.tintIndex(), element.shade(), element.lightEmission());

                CuboidFace.UVs uvs = face.uvs() != null ? face.uvs() : defaultFaceUV(element.from(), element.to(), facing);
                result.add(FaceBakery.bakeQuad(interner, element.from(), element.to(), uvs, face.rotation(),
                        materialInfo, facing, BlockModelRotation.IDENTITY, element.rotation()));
            }
        }

        LOGGER.info("[EnKreta] {} baked: {} quad(s)", modelId, result.size());
        return List.copyOf(result);
    }

    /** {@code FaceBakery.defaultFaceUV} is package-private — same formula, copied. */
    private static CuboidFace.UVs defaultFaceUV(Vector3fc from, Vector3fc to, Direction facing) {
        return switch (facing) {
            case DOWN -> new CuboidFace.UVs(from.x(), 16.0f - to.z(), to.x(), 16.0f - from.z());
            case UP -> new CuboidFace.UVs(from.x(), from.z(), to.x(), to.z());
            case NORTH -> new CuboidFace.UVs(16.0f - to.x(), 16.0f - to.y(), 16.0f - from.x(), 16.0f - from.y());
            case SOUTH -> new CuboidFace.UVs(from.x(), 16.0f - to.y(), to.x(), 16.0f - from.y());
            case WEST -> new CuboidFace.UVs(from.z(), 16.0f - to.y(), to.z(), 16.0f - from.y());
            case EAST -> new CuboidFace.UVs(16.0f - to.z(), 16.0f - to.y(), 16.0f - from.z(), 16.0f - from.y());
        };
    }
}
