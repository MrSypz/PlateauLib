# plateau-postprocess Redesign — Pattern Notes

Research + design pattern doc. No code changed by this pass. Written so a future
session can build a VFX feature (or redesign this library for real) without
re-reading Minecraft's decompiled source from scratch.

Sources: `minecraft-merged-deobf-26.1-sources.jar` (MC 26.1, official mappings,
Fabric Loom dev environment), the current `plateau-postprocess` module (8 files),
and LekLai's 3 real consumers (`HeatDistortionRenderer`, `SwordTrailDistortionRenderer`,
`LightRenderer`).

---

## 1. How MC 26.1's render pipeline actually works

MC's rendering stack changed shape significantly from the pre-1.21 "immediate GL
calls everywhere" style. It is now three cleanly separated layers. Understand
them in this order — each is a distinct concern, and most confusion comes from
conflating them.

```
┌─────────────────────────────────────────────────────────────────┐
│ Layer 3: Frame graph (scheduling / resource lifetime)           │
│   FrameGraphBuilder, GraphicsResourceAllocator,                 │
│   CrossFrameResourcePool, ResourceHandle<T>                     │
│   "what passes run, in what order, using which pooled targets"  │
└─────────────────────────────────────────────────────────────────┘
                              │ a pass's Runnable body drops into ↓
┌─────────────────────────────────────────────────────────────────┐
│ Layer 2: Draw execution (per-draw-call GPU state + submission)  │
│   RenderPass, CommandEncoder, GpuDevice, RenderSystem           │
│   "bind this pipeline, these uniforms, these textures, draw"    │
└─────────────────────────────────────────────────────────────────┘
                              │ describes state for ↑
┌─────────────────────────────────────────────────────────────────┐
│ Layer 1: Pipeline description (declarative GPU state)           │
│   RenderPipeline, CompiledRenderPipeline / GlRenderPipeline      │
│   "the immutable record of shader + blend + depth + vertex fmt" │
└─────────────────────────────────────────────────────────────────┘
```

### 1.1 Layer 1 — `RenderPipeline`: pipelines are data, not calls

`com.mojang.blaze3d.pipeline.RenderPipeline` is a plain **immutable record**
built once via `RenderPipeline.builder(Snippet...)` and typically stored as a
`static final` field. It captures every piece of fixed-function GPU state a
draw needs, upfront:

- `location` (`Identifier`) — the pipeline's own id
- `vertexShader` / `fragmentShader` (`Identifier`s, e.g. `"core/entity"` — **not** embedded GLSL source, see §1.4)
- `shaderDefines` — preprocessor `#define`s baked per-pipeline
- `samplers` (`List<String>`), `uniforms` (`List<UniformDescription>` — name + `UniformType` + optional `TextureFormat`)
- `depthStencilState` (nullable — `null` = depth test fully disabled)
- `polygonMode`, `cull` (default `true`)
- `colorTargetState` — optional `BlendFunction` + color write mask
- `vertexFormat` + `vertexFormatMode` (e.g. `QUADS`, `LINES`)

`RenderPipeline.Snippet` lets pipelines share common state by composition —
`RenderPipelines.java` chains snippets (`MATRICES_PROJECTION_SNIPPET` →
`MATRICES_FOG_SNIPPET` → `ENTITY_SNIPPET` → ...) to build the ~100 built-in
pipelines. `Builder.build()` throws `IllegalStateException` immediately if
location/shaders/vertex format are missing — validation happens at
*pipeline-definition* time, not at draw time.

**Nothing here issues a GL call.** A `RenderPipeline` is inert data.

### 1.2 Compilation

`CompiledRenderPipeline` is a 1-method marker (`isValid()`). The OpenGL backend
implementation is `record GlRenderPipeline(RenderPipeline info, GlProgram program)`
(`com.mojang.blaze3d.opengl.GlRenderPipeline`). Compilation happens lazily and is
cached: `GlDevice.getOrCompilePipeline(pipeline)` looks up a `pipelineCache` map
keyed by the `RenderPipeline` object itself, calling `compilePipeline` (which
links the actual GL program via `GlProgram.link`) on a cache miss. Vanilla also
**eagerly precompiles** every static pipeline on resource reload
(`ShaderManager.apply` → `device.precompilePipeline(...)` for everything in
`RenderPipelines.getStaticPipelines()`), so the common case pays the compile
cost once, at load time, not on first draw.

### 1.3 Layer 2 — draw execution: `RenderPass` / `CommandEncoder` / `RenderSystem`

The actual sequence to draw something:

1. `CommandEncoder.createRenderPass(...)` opens a `RenderPass` bound to
   **explicit** color/depth `GpuTextureView`s — there is no implicit "current
   framebuffer" anymore; you always say exactly what you're rendering into.
2. `RenderPass.setPipeline(RenderPipeline)` stores the pipeline reference; the
   GL backend resolves it to a compiled `GlRenderPipeline` via
   `device.getOrCompilePipeline(pipeline)`.
3. `RenderPass.setUniform(...)`, `bindTexture(...)`, `setVertexBuffer(...)`,
   `setIndexBuffer(...)` stage the draw's inputs.
4. `RenderPass.draw(...)` / `drawIndexed(...)` → internally
   `GlCommandEncoder.applyPipelineState(RenderPipeline)` is the **single place**
   the declarative record turns into old-style `GlStateManager` calls
   (`_enableDepthTest`, `_depthFunc`, `_enableCull`, `_enableBlend`,
   `_blendFuncSeparate`, `_polygonMode`, `_colorMask`) — deduped against
   `lastPipeline` so redundant state changes are skipped. **You never call this
   yourself; it's private.**

`RenderSystem` (`com.mojang.blaze3d.systems.RenderSystem`) is *not* a grab-bag
of GL toggles anymore. Read the whole file and you'll find it holds only
cross-cutting global state: the render-thread assertion, matrix stacks, shared
fog/lighting UBO slices, shared auto-growing index buffers, and
`RenderSystem.getDevice()` (the one `GpuDevice` accessor) plus
`bindDefaultUniforms(RenderPass)`. **There is no `RenderSystem.enableBlend()`,
`disableDepthTest()`, or `setShaderColor()` anymore** — if you're hunting for
one of those pre-1.21 calls, the answer is "put that state in a
`RenderPipeline` instead."

Vertex data: `VertexBuffer` no longer exists as a standalone class. Build a
`MeshData` (raw CPU-side vertex/index `ByteBuffer`s + a `DrawState` record) via
`Tesselator.getInstance().begin(mode, format)` → fill the returned
`BufferBuilder` → `.buildOrThrow()`. `RenderType.draw(MeshData)` is the
one-call path most gameplay code actually uses: uploads the mesh, resolves the
output `RenderTarget`, opens a `RenderPass`, sets the pipeline, binds
textures/uniforms, draws.

### 1.4 Shaders

Still GLSL, but the mapping moved. A `RenderPipeline` references its shaders
only by `Identifier` (e.g. `"core/entity"`) — it never embeds source or a
linked program. `ShaderManager` (a `SimplePreparableReloadListener`) scans
`assets/<ns>/shaders/**` for `.vsh`/`.fsh`/`.glsl` files at resource-reload
time and builds an `Identifier → source text` map (with `#moj_import`-style
includes). At compile time, `GlDevice.compilePipeline` resolves the
pipeline's shader `Identifier`s through that map. There is no more monolithic
per-shader `.json` (uniforms+samplers+GLSL paths bundled together) — that
role moved *into* the `RenderPipeline` builder (`.withUniform(...)`,
`.withSampler(...)`). The one place JSON still describes a rendering
construct is post-effect chains (`PostChainConfig`, §1.6) — a completely
different JSON format for a completely different purpose. Don't conflate the two.

### 1.5 Layer 3 — the frame graph: `FrameGraphBuilder`

A layer *above* everything in §1.1–1.3, concerned purely with **scheduling
and resource lifetime**, not GPU state:

- `FrameGraphBuilder.addPass(name)` registers a named `FramePass`; you declare
  its resource reads/writes and give it an `executes(Runnable)` body — the
  body is exactly where you drop back down into Layer 2 and open a real
  `RenderPass`.
- `FrameGraphBuilder.execute(GraphicsResourceAllocator, Inspector)` does, in
  order: (a) **culls** passes whose output is never consumed by anything
  externally visible (dead output = dead pass, silently dropped — a common
  "why doesn't my pass run" trap); (b) topologically sorts by read/write
  dependency, throwing on cycles; (c) computes each virtual resource's
  acquire/release window; (d) runs passes in order, acquiring/releasing pooled
  resources around each one.
- `GraphicsResourceAllocator` — 2-method interface (`acquire`/`release` a
  `ResourceDescriptor<T>`). `CrossFrameResourcePool` is the pooled
  implementation vanilla actually uses (`GameRenderer.resourcePool = new
  CrossFrameResourcePool(3)`): released resources live in a deque for N
  frames and get matched by `descriptor.canUsePhysicalResource(other)`
  (same width/height/depth-flag) before falling back to fresh allocation —
  this is what makes transient post-effect targets cheap across frames
  instead of realloc'd every tick.
- `ResourceHandle<T>` — a lazy/virtual reference (`T get()`) returned by
  `createInternal`/`importExternal`; resolved only once the graph actually
  acquires it. **Handles are versioned/single-use**: a read-modify-write
  (`readsAndWrites`) invalidates the old handle and returns a new one; holding
  a stale handle after a later write throws `IllegalStateException`.

Rule of thumb for when your VFX code needs this layer at all: **skip it** for
a single full-screen pass reading/writing one target — just open a `RenderPass`
directly against `RenderSystem.getDevice()`. **Use it** when you need pooled,
multi-pass ping-pong buffers (bloom, blur chains, anything with N intermediate
targets whose lifetimes you don't want to hand-manage) or when you need to
graft your passes into an *existing* graph (e.g. `LevelRenderer`'s own
per-frame `FrameGraphBuilder`) so culling/ordering is shared correctly.

### 1.6 `PostChain` / `PostChainConfig` — MC's built-in post-processing system

This is the system plateau-postprocess wraps. It's built entirely on top of
§1.1–1.5, nothing new under the hood.

**Config format** (`PostChainConfig`, a `record` with a `Codec`, loaded from
`assets/<ns>/post_effect/*.json`):

```
PostChainConfig
  targets: Map<Identifier, InternalTarget>   // "targets", optional, default {}
    InternalTarget(width?, height?, persistent=false, clearColor=0)
  passes: List<Pass>                          // "passes", optional, default []
    Pass(vertexShaderId, fragmentShaderId,
         inputs: List<Input>,                 // "inputs" — sampler bindings
           TargetInput(samplerName, targetId, useDepthBuffer=false, bilinear=false)
           TextureInput(samplerName, location, width, height, bilinear)  // static PNG
         outputTarget: Identifier,             // "output" — required
         uniforms: Map<String, List<UniformValue>>)  // "uniforms", optional
```

`Pass.uniforms` values are **literal constants baked from JSON at load time**
— float/int/vec/mat values written once into a `GpuBuffer` when the `PostPass`
is constructed. There is no vanilla per-frame "set this uniform from Java"
API — see §1.6.3 for what that means for dynamic effects.

**Config → runtime** (`PostChain.load(config, textureManager,
allowedExternalTargets, id, ...)`): validates every external target reference
against a caller-supplied allow-list (throws `CompilationException` if a pass
references a target outside it — the allow-list is a load-time parameter, not
inherent to the config), then builds one `PostPass` per config `Pass`.
Building a `PostPass` builds a `RenderPipeline` from
`RenderPipelines.POST_PROCESSING_SNIPPET`, adding one `withSampler` per input
and one `withUniform(groupName, UNIFORM_BUFFER)` per uniform group name —
**a pipeline only has a binding slot for uniform group names that were
present in the original config**; you cannot inject a brand-new uniform name
at runtime without the pipeline itself being built with a matching
`withUniform` call.

**Rendering a frame** — `PostChain.addToFrame(FrameGraphBuilder, screenWidth,
screenHeight, TargetBundle)`:
1. Resolves every declared target to a `ResourceHandle<RenderTarget>` —
   external targets come from the caller's `TargetBundle`; internal
   *persistent* targets are created/reused on the `PostChain` instance itself
   (self-healing across resize, see §1.7); internal *non-persistent* targets
   are pure per-frame virtual resources.
2. Each `PostPass.addToFrame(frame, targets, projectionBuffer)` registers one
   `FramePass`: declares reads for every `TargetInput`, declares
   read-and-write on its output target, and registers an `executes(Runnable)`
   body that — at actual execution time — opens a `RenderPass` against the
   (now concrete) output target, sets the pipeline, binds default uniforms,
   binds `"SamplerInfo"` (auto-populated width/height per sampler) and every
   `customUniforms` entry, binds each input texture, and calls
   `renderPass.draw(0, 3)` — **a full-screen triangle, no vertex buffer**, the
   standard post-process trick baked into the vertex shader.
3. `TargetBundle` is just a named-handle map interface
   (`replace(id, handle)` / `get(id)` / `getOrThrow`). Vanilla's concrete
   implementation, `LevelTargetBundle`, exposes fixed addressable slots:
   `main`, `translucent`, `item_entity`, `particles`, `weather`, `clouds`,
   `entity_outline` — the intermediate buffers `LevelRenderer` itself
   produces per frame, each individually readable by a post-chain if the
   allow-list permits it.

**`GameRenderer`'s actual hookup** (`GameRenderer.render`, after `renderLevel()`
and `doEntityOutline()`, before the GUI depth-clear):

```java
if (this.postEffectId != null && this.effectActive) {
    PostChain postChain = minecraft.getShaderManager()
        .getPostChain(this.postEffectId, LevelTargetBundle.MAIN_TARGETS);
    if (postChain != null) postChain.process(minecraft.getMainRenderTarget(), this.resourcePool);
}
```

Vanilla's own usage is **entirely conditional, not an always-on chain over
gameplay**: `postEffectId`/`effectActive` are set by
`checkEntityPostEffect(cameraEntity)` for creeper vision, spider vision, and
the nausea/potion "invert" effect — keyed on what entity the camera is
inside. A separate `processBlurEffect()` (menu-background blur) is called
from elsewhere entirely, not the main frame. **There is no vanilla post-chain
running over normal gameplay** by default — mods are free to run their own
anywhere in the frame.

The user's existing mixin injects right after this point (after `renderLevel`
+ `doEntityOutline` + vanilla's own conditional chain, before the GUI depth
clear) — this is the natural hook: `main` target color+depth is fully
composited (opaque + translucent + particles + weather + clouds + outline),
nothing GUI has touched it yet, and it composes correctly with vanilla's own
creeper/spider/invert/blur chains (same target, same addressability via
`LevelTargetBundle.MAIN_TARGETS`). Other viable hook points if a future effect
needs pre-composite buffers: earlier (before vanilla's own conditional
chain, to run *underneath* it) or, for anything needing `translucent`/
`particles`/`weather`/`clouds`/`entity_outline` individually before they're
blended into `main`, hooking inside `LevelRenderer.renderLevel` itself rather
than `GameRenderer.render`. There is no post-GUI hook — GUI renders via a
completely separate `guiRenderer.render(...)` call with no `PostChain`
involvement.

#### 1.6.1 `RenderTarget` / `TextureTarget` lifecycle

`RenderTarget` is abstract; `TextureTarget` is the only concrete subclass — a
thin wrapper where `resize(w, h)` **always** does `destroyBuffers()` then
`createBuffers(w, h)`. **There is no in-place GPU resize.** Any code holding a
raw `GpuTextureView` across a resize now points at a closed object and must
re-fetch. `createBuffers` asserts render-thread and validates bounds against
`device.getMaxTextureSize()`.

Dimension sourcing:
- **Main framebuffer** — resized explicitly by `GameRenderer.resize(w, h)`,
  called once per frame from `GameRenderer.render()` when the window resize
  flag is set. Polled, not event-driven.
- **Persistent internal `PostChain` targets** — `getOrCreatePersistentTarget`
  compares the cached target's dimensions against the freshly-computed
  descriptor every `addToFrame` call; on mismatch, destroys and recreates.
  Self-healing, but only checked when `addToFrame` actually runs.
- **Non-persistent internal targets** — pure per-frame virtual resources,
  nothing to go stale.

#### 1.6.2 Foot-guns (lifecycle requirements that aren't obvious from the API surface)

- `PostChain.close()` destroys all persistent targets and every `PostPass`'s
  uniform buffers. A `PostChain` fetched via `ShaderManager.getPostChain` is
  **cached and auto-closed on resource-pack reload** (`/reload`, F3+T) — a
  held reference across reload is stale and must be re-fetched, not reused.
- Non-persistent internal targets are pooled across ~3 frames
  (`CrossFrameResourcePool`) — `release` is not immediate GPU free.
- Everything here asserts render-thread. No cross-thread construction, resize,
  or destruction of targets/passes.
- `MappableRingBuffer` (backs the auto `SamplerInfo` UBO and is the natural
  pattern for any hand-rolled dynamic uniform) is triple-buffered with GPU
  fences — `currentBuffer()` blocks if the slot is still in flight, and
  `rotate()` must be called each use or CPU writes and GPU reads desync.

#### 1.6.3 Custom/dynamic uniforms — the part vanilla doesn't give you

`PostPass.customUniforms` (`Map<String, GpuBuffer>`, private, package-only —
this is exactly why the user's library needs `@Accessor` mixins to reach it)
is populated **once, in the `PostPass` constructor**, from the config's static
JSON `UniformValue` literals. At render time, the *only* thing vanilla does
with the map is `for (entry : customUniforms) renderPass.setUniform(entry.getKey(),
entry.getValue())` — every frame the pass runs, it rebinds whatever
`GpuBuffer` object currently sits in the map under that name.

That's the entire mechanism, and it's also the entire opportunity: because
binding is "whatever's in the map right now," a caller with access to the map
(currently: only via the accessor mixin) can either mutate an existing
buffer's bytes each frame (`device.createCommandEncoder().mapBuffer(...)`,
matching the `infoUbo` pattern) or swap in an entirely new `GpuBuffer`
instance between frames, and the next `addToFrame` pass picks it up
automatically with zero extra plumbing. This is the load-bearing mechanism
any redesign's "attach a dynamic uniform" primitive has to wrap correctly —
see §3.2.

There is a second, unrelated dynamic-uniform system in the codebase —
`DynamicUniforms`/`DynamicUniformStorage` — a per-draw-call transform/color
UBO ring buffer used by entity/chunk rendering (`writeTransform`,
`writeChunkSections`). **It is not wired into `PostPass` at all.** Don't
reach for it when the actual need is "get a value into a post-chain shader."

### 1.7 Naming collision: two unrelated things called "RenderLayer"

**Explicitly flagging this because it is a real, repeat source of confusion.**

- `net.minecraft.client.renderer.entity.layers.RenderLayer` — an
  **entity-rendering** concept. A `RenderLayerParent`-owned decorator that
  draws extra geometry on top of an entity's base model (armor, glowing eyes,
  enchantment glint, elytra, etc.), each frame, as part of normal entity
  render. Has nothing to do with post-processing, framebuffers, or passes.
- **Post-process "layering"** in this document (and in plateau-postprocess's
  own `PostEffectLayer` type) means something completely different: a
  registered unit of screen-space post-processing work (a lambda/effect that
  runs after `renderLevel`, potentially reading/writing render targets via a
  `PostChain`). No relationship to entity `RenderLayer` whatsoever beyond the
  reused English word.

If you're grepping decompiled MC source for "RenderLayer" while working on
VFX/post-process code, you almost certainly want `PostChain`/`PostPass`/
`RenderPipeline`, not `net.minecraft.client.renderer.entity.layers.RenderLayer`.

---

## 2. What's wrong with plateau-postprocess today

Concrete, cited pain points — each traces to a real file/method, not a vague
complaint. Findings from close-reading the library's mixins
(`GameRendererMixin`, `PostChainAccessor`, `PostPassAccessor`), its
convenience layer (`PostProcessResources`, `PostEffectManager`,
`PostEffectLayer`, `PostEffectHandle`), and its three real consumers in
LekLai.

**The throughline**: the convenience layer (`PostProcessResources` +
`PostEffectManager`) was built around exactly one use case — one main target,
one named mask target, one apply callback per frame, immediately after
`renderLevel`. It fits the two simple consumers (`HeatDistortionRenderer`,
`SwordTrailDistortionRenderer`) adequately, at the cost of duplicated
boilerplate between them. The moment a consumer needs anything more
(`LightRenderer` — multiple targets, an earlier draw phase, its own ordering
relative to terrain), the abstraction has **no growth path**: the consumer
falls all the way back to the raw `@Accessor` mixins and hand-rolled
`FrameGraphBuilder`/`TargetBundle` code, and in doing so silently loses the
lifecycle guarantees (disconnect cleanup, render-thread scheduling) the
manager does provide for everyone else. There is no reduced-but-still-managed
middle tier between "the convenience helper" and "raw mixin access."

1. **`PostProcessResources` hardcodes exactly one main + one mask target — doesn't generalize to N targets.**
   `PostProcessResources.java` (`maskName`, `maskTargetId`, single `TextureTarget maskTarget`
   field; `executePostChain` builds a `TargetBundle` with only `main`/`mask`
   branches). LekLai's `LightRenderer` needs `main` + a voxel-grid target + up
   to `MAX_SHADOW_SLOTS * 6` shadow-face targets — the helper structurally
   can't express that, so `LightRenderer` doesn't use `PostProcessResources`
   at all; it re-derives the same `FrameGraphBuilder`/`Map<Identifier,
   ResourceHandle<RenderTarget>>`/`TargetBundle` code, just re-typed for a
   dynamic id set. *Fix shape*: a target-set builder over an arbitrary
   `Map<Identifier, RenderTarget>`, not a fixed pair.

2. **The "PostChain got recreated by F3+T, my uniform buffer was closed under me" detection is copy-pasted, not owned by the library.**
   `PostProcessResources.patchUniforms` (iterate `passes`, compare `existing
   != uniformBuffer`, recreate on mismatch) is re-implemented near-verbatim in
   `LightRenderer.ensureBuffer` — same `needsRecreate` boolean, same accessor
   casts, same close-and-replace — because `LightRenderer` can't reuse
   `PostProcessResources` (see #1). *Fix shape*: a `ManagedUniform` handle
   that self-heals across `PostChain` recreation so no consumer touches
   `PostChainAccessor`/`PostPassAccessor` directly.

3. **`@Accessor` mixins into package-private `PostChain`/`PostPass` fields leak straight into application code.**
   `PostChainAccessor.getPasses()` and `PostPassAccessor.getCustomUniforms()`
   are imported and called directly from `LightRenderer.ensureBuffer`, not
   just from inside the library. The library boundary is porous: whenever the
   convenience layer doesn't fit, the only fallback is reaching past it into
   vanilla internals via the library's *own* mixins, with no intermediate
   API tier.

4. **The mask-draw / apply-effect split forces a hand-rolled cross-event boolean latch, duplicated verbatim in two consumers.**
   `HeatDistortionRenderer` has a static `hasMask` field set in
   `onLevelRender`, consumed defensively in `applyIfReady` (`ready = hasMask;
   hasMask = false; // always reset — prevents stale mask from a skipped
   level-render event`). `SwordTrailDistortionRenderer` has the byte-identical
   method and the byte-identical comment. The library only exposes one
   callback timing (`PostEffectLayer.apply`, fired post-`renderLevel`), so any
   effect whose mask paints during an *earlier* hook has to invent its own
   producer/consumer flag. *Fix shape*: a two-phase `prepare`/`apply` contract
   so "did this frame produce anything" is tracked by the framework.

5. **The library's own conditional-gating feature (`PostEffectHandle.when(...)`) goes unused by every real consumer, because it can't express their actual condition.**
   `when(BooleanSupplier)` exists specifically to gate a layer without
   re-registering. Yet neither `HeatDistortionRenderer` nor
   `SwordTrailDistortionRenderer` chain it — "should I run" is only knowable
   *after* the mask-draw phase, which happens in a different event than
   registration, so the registration-time `BooleanSupplier` is structurally
   unusable and both fall back to #4's hand-rolled latch instead.

6. **Lazy-init/close boilerplate for the resource holder is duplicated per consumer instead of owned by the registration handle.**
   Both simple renderers repeat: `if (resources == null) { resources = new
   PostProcessResources(...); }` plus a `close()` that nulls the static field
   back out. `PostEffectHandle` takes an `onClose` `Runnable` but has no
   concept of *owning* a resource object — three consumers, three
   hand-managed nullable statics.

7. **The most complex consumer bypasses `PostEffectManager` entirely, forfeiting all its lifecycle guarantees.**
   `LightRenderer` is wired via `LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(LightRenderer::renderSafe)`
   in `LeklaiClient`, not `PostEffectManager.register(...)` — a direct
   consequence of #1. It also means point lights live on a completely
   different scheduling axis (a raw Fabric render-event phase) than every
   other effect's `priority` int — there is no single place to reason about
   render order across all post-effects in the mod.

8. **Direct consequence of #7: `LightRenderer.close()` is dead code — never called anywhere — a real, currently-live GPU resource leak.**
   `LightRenderer.close()` releases its instance buffer and config buffer but
   is unreferenced across the whole codebase. `LeklaiClient`'s `DISCONNECT`
   handler calls `LightRenderer.clearLights()`, `VoxelShadowGrid.close()`,
   and `PointLightShadowManager.clearLevel()`, but never `LightRenderer.close()`
   — so its SSBO instance buffer and uniform buffer leak across every
   disconnect/reconnect cycle. This is exactly the bug class
   `PostEffectManager.register(layer, onClose)` exists to prevent; it only
   failed here because the consumer couldn't stay on the managed path.

9. **Render-thread scheduling for cleanup is solved once inside the library, then has to be manually re-solved outside it — and is re-solved incompletely.**
   `PlateauPostprocessClient` correctly defers `closeAll()` onto the render
   thread on disconnect (`client.execute(PostEffectManagerAccess::closeAll)`).
   Because `LightRenderer`/`VoxelShadowGrid`/`PointLightShadowManager` aren't
   registered through the manager, `LeklaiClient` reproduces the identical
   pattern by hand for them — and, per #8, misses a spot doing it.

10. **No Iris/shader-pack compatibility primitive anywhere in the library or any of the three consumers.**
    Zero references to `IrisApi` or shader-pack detection in
    `HeatDistortionRenderer`, `SwordTrailDistortionRenderer`, `LightRenderer`,
    `PostProcessResources`, or `PostEffectManager`. The only Iris touchpoint in
    the entire client is unrelated (`LeklaiClient` assigning a *particle*
    render pipeline to an Iris program, not a post-chain). None of the three
    post-process renderers ask "does a shader pack currently own the frame
    graph" before running their own `FrameGraphBuilder`/`PostChain` work —
    correctness under Iris is untested by construction, not addressed by a
    deliberate decision.

11. **Per-frame `RenderSystem.outputColorTextureOverride` push/pop for mask rendering is manual, unguarded, and inconsistently applied.**
    Both `HeatDistortionRenderer.renderMaskPass` and
    `SwordTrailDistortionRenderer.renderMaskPass` manually set
    `RenderSystem.outputColorTextureOverride`, draw, then reset — one wraps it
    in `try/finally`, the other doesn't. Two copies of "the same" pattern
    diverging is itself evidence this shouldn't be hand-written per call site.

12. **Diagnostics/error surfacing is ad hoc and inconsistent between consumers.**
    `LightRenderer` tracks its own `lastError`, throttled logging, and a pile
    of profiling fields purely so a debug overlay has something to read.
    `HeatDistortionRenderer`/`SwordTrailDistortionRenderer` have none of this
    — a null `PostChain` there just silently returns. Nothing in
    `PostEffectManager`/`PostEffectHandle` provides a shared "why didn't this
    effect run this frame" surface, so a real shader-compile failure is
    invisible in two out of three effects.

---

## 3. Proposed redesign

> **STATUS (2026-07-29): IMPLEMENTED** in `com.sypztep.plateau.client.v1.vfx`
> (the old `client.v1.postprocess` API is deleted) and all three LekLai
> consumers are migrated. §2's pain points are closed. Deltas from the sketch
> below, which is kept for rationale:
> - `VfxEffect` gained the two-phase contract as first-class API:
>   `preparePhase()` (a `VfxLevelPhase` mapping 1:1 to Fabric
>   `LevelRenderEvents`) + `prepare(VfxPrepareFrame)` returning the
>   "produced work" flag. The manager registers the Fabric listeners, so
>   `LightRenderer`'s mid-level scheduling (BEFORE_TRANSLUCENT_TERRAIN) and
>   the mask-draw phases live on the same axis as `contribute` priority.
> - `ManagedUniform` is created via `VfxPostChain.uniform(name, size)` and
>   reconciled inside `VfxPostChain.prepare(frame)` — not by the manager.
> - `VfxContext.own`/`ownTargets` is how init-allocated resources become
>   framework-owned (auto-close on disconnect, auto `ensureSized` per frame).
> - `VfxDiagnostics` reports `lastFrameCostNanos()` (long, not `Duration`);
>   effect exceptions are caught by the manager, folded into the skip reason,
>   and throttle-logged.
> - `shaderPackActive()` is a cached reflection probe of `IrisApi` (no
>   compile-time Iris dep); effects opt out via `skipUnderShaderPacks()`.
> - LekLai builds against a local PlateauLib via
>   `./gradlew publishToMavenLocal -Pversion=local` here, then
>   `-PplateauLocal` in LekLai (see its build.gradle).

Signatures and rationale below were the design sketch. The goal is a
thin, correct wrapper over the primitives from §1, not another leaky
abstraction stacked on the current leaky one. Every type below maps directly
to a concrete MC 26.1 mechanism cited above; nothing here invents new
rendering concepts MC doesn't already have.

### 3.1 Core registration — replaces `PostEffectManager` + `PostEffectHandle` + `PostEffectLayer`

```java
/** One managed post-process effect. Owns its GPU resources; the manager owns its lifecycle. */
public interface VfxEffect extends AutoCloseable {

    /** Called once, lazily, on first frame this effect actually runs. Allocate GPU resources here. */
    void init(VfxContext ctx);

    /**
     * Called every frame in priority order, whether or not this effect has
     * anything to draw. Return NONE to skip contributing to this frame with
     * zero boilerplate on the consumer's side — this replaces the hand-rolled
     * hasMask latch (pain point #4) and the unusable registration-time
     * BooleanSupplier (pain point #5): the framework asks "did you produce
     * anything" at the one moment it's actually knowable.
     */
    FrameContribution contribute(VfxFrame frame);

    /** Release GPU resources. Called on disconnect and on unregister. Idempotent. */
    @Override void close();
}

public enum FrameContribution { NONE, RAN }
```

```java
public final class VfxManager {
    /** Register once at mod init. Returns a handle for priority/gating/removal. */
    public static VfxHandle register(VfxEffect effect);
}

public final class VfxHandle {
    public VfxHandle priority(int priority);              // higher runs first — same semantics kept from today
    public VfxHandle when(BooleanSupplier gate);           // now genuinely usable: gates contribute(), not apply()
    public void unregister();                              // removes + calls effect.close()
}
```

Rationale: folding `init`/`close` into the same object the manager already
tracks removes pain points #6 (hand-managed nullable statics — the manager
calls `init` lazily and guarantees `close` exactly once) and #8/#9 (an effect
that stays on this path can never end up in the position `LightRenderer` is
in today, because there is no path that bypasses cleanup). `contribute`
returning an enum instead of the caller polling a `BooleanSupplier` fixes #5
directly — gating happens where the answer is actually known.

### 3.2 Target/uniform lifecycle — replaces `PostProcessResources`

The generalization pain points (#1, #2, #3) all come from one root cause: the
current type hardcodes "one mask target" and "one uniform group" into its
field layout. Fix: model both as open collections, keyed by the same
`Identifier`s MC's own `TargetBundle`/`PostChainConfig` already use.

```java
/** Owns N named render targets sized to track the main framebuffer (or fixed size). Handles resize. */
public final class VfxTargetSet implements AutoCloseable {
    public static VfxTargetSet.Builder builder();
    // Builder: .target(Identifier id, boolean useDepth)  — screen-sized, tracks main target each frame
    //          .fixedTarget(Identifier id, int w, int h, boolean useDepth)
    public RenderTarget get(Identifier id);
    public void ensureSized(Minecraft mc);        // called once per frame by VfxManager, not by consumers
    @Override public void close();                 // destroyBuffers() every target — pain points #1 solved generically
}

/** A self-healing uniform slot: survives PostChain recreation (F3+T reload) transparently. */
public final class ManagedUniform {
    public static ManagedUniform of(String groupName, int byteSize);
    /** Write bytes for this frame. Internally: map the buffer, or recreate + rebind into
     *  every PostPass's customUniforms map if MC discarded the old one (pain point #2). */
    public void write(Consumer<ByteBuffer> writer);
    /** Internal — called by VfxManager once per registered effect per frame, wraps the
     *  PostChainAccessor/PostPassAccessor reach so no consumer ever imports those types (#3). */
    void reconcile(PostChain chain);
}
```

A `VfxTargetSet` replaces the hardcoded `main`+`mask` pair with an arbitrary
`Map<Identifier, RenderTarget>` — this is the fix pain point #1 asked for
directly: `LightRenderer`'s shadow-face targets and `HeatDistortionRenderer`'s
single mask target become the same code path, just different builder calls.
`ManagedUniform` is `PostProcessResources.patchUniforms` promoted to a
reusable type, so #2's duplicated detection logic collapses to one
implementation, and #3's accessor imports never need to leave this package —
consumers only ever see `ManagedUniform.write(...)`.

### 3.3 Running a PostChain against a target set

```java
public final class VfxPostChain {
    public static VfxPostChain load(Identifier postChainId, VfxTargetSet targets, /* allow-list derived from targets' ids */ Identifier... externalIds);

    /** Runs this frame's pass(es) via a fresh or caller-supplied FrameGraphBuilder.
     *  Overload taking an existing FrameGraphBuilder lets an effect graft into
     *  another pass (e.g. LevelRenderer's own graph) instead of always building
     *  a standalone one-shot graph — closes the gap that pushed LightRenderer
     *  to hand-roll this itself. */
    public void run(GraphicsResourceAllocator pool);
    public void run(FrameGraphBuilder existingGraph, GraphicsResourceAllocator pool);
}
```

This is a direct, thin wrapper over `PostChain.load`/`addToFrame`/`process`
from §1.6 — it does not reimplement frame-graph scheduling, it just removes
the need for a consumer to hand-build a `TargetBundle` anonymous class every
time (the exact duplicated code in `PostProcessResources.executePostChain`
vs. `LightRenderer`'s equivalent).

### 3.4 Mask/scoped-output helper — replaces the manual `outputColorTextureOverride` push/pop

```java
public final class VfxScope {
    /** try-with-resources: sets RenderSystem.outputColorTextureOverride on open,
     *  guarantees reset on close even on exception — fixes pain point #11's
     *  inconsistent try/finally usage by making the unsafe pattern impossible
     *  to write incorrectly. */
    public static VfxScope overrideOutput(GpuTextureView target);
    @Override public void close(); // AutoCloseable
}
// usage: try (var scope = VfxScope.overrideOutput(mask.getColorTextureView())) { drawMask(); }
```

### 3.5 Iris awareness — closes pain point #10

```java
public final class VfxManager {
    // ...
    /** True when a shader pack is active and expected to own frame-graph composition
     *  (e.g. deferred rendering rewrites target contents outside vanilla's control).
     *  VfxManager checks this once per frame; effects that opt in to
     *  VfxEffect#skipUnderShaderPacks() are automatically skipped rather than
     *  every consumer needing its own IrisApi check. */
    public static boolean shaderPackActive();
}
```

Rationale: this doesn't attempt real Iris pipeline integration (out of scope,
and MC 26.1's Iris compat story is itself still evolving) — it gives every
effect a single, correct place to opt out of running when a shader pack is
likely to conflict, instead of the current state (nobody checks, silently).

### 3.6 Diagnostics — closes pain point #12

```java
public interface VfxDiagnostics {
    Optional<String> lastSkipReason();   // "chain not loaded", "gated by when()", "shader pack active", ...
    Duration lastFrameCost();
}
// VfxHandle.diagnostics() returns one of these, populated by VfxManager itself —
// consumers get this for free; LightRenderer's hand-rolled lastError/lastLogTimeMs
// fields become unnecessary.
```

### What stays exactly as-is

- The core insight of the current design — a flat, priority-ordered,
  frame-graph-agnostic registry the consumer never has to think about frame
  graphs to use — is correct and should not be thrown out. `PostEffectManager`'s
  `register`/`priority`/`unregister` shape survives into `VfxManager` almost
  unchanged; the redesign only widens what a registered effect is *allowed to
  own* (multiple targets, its own resource lifecycle, a two-phase contribute
  contract) rather than replacing the registration model itself.
- `PlateauPostprocessClient`'s disconnect-hooked `closeAll()` pattern is
  correct and stays — it just needs to be the *only* cleanup path, which
  requires closing the gap that let `LightRenderer` opt out of it (§3.1–3.2).

---

## 4. Quick-reference cheat sheet

*"I'm about to build a VFX feature — which primitive do I actually want?"*

| I want to... | Use | Not |
|---|---|---|
| Describe fixed-function GPU state (shader, blend, depth, vertex format) for a custom draw | `RenderPipeline.builder(...)` (§1.1) — build once, `static final` | Ad hoc `GlStateManager` calls — that API is gone |
| Batch/draw a category of world geometry (blocks, entities, particles) with an existing pipeline | `RenderType` (§1.3) — wraps a `RenderPipeline` + output target + textures | Building your own `RenderPipeline` from scratch if an existing `RenderType` already fits |
| Draw a one-off custom mesh (a quad, a line strip) | `Tesselator` → `BufferBuilder` → `MeshData` → `RenderType.draw(mesh)` or a manual `RenderPass` | `VertexBuffer` — doesn't exist anymore |
| Run a full-screen post-process effect (distortion, glow, color grade) reading the composited frame | `PostChain`/`PostChainConfig` (§1.6), or this library's `VfxPostChain` wrapper (§3.3) once it exists | Hand-opening a `RenderPass` per effect — reinvents pass ordering/target management MC already solved |
| Push a per-frame dynamic value into a post-chain shader (not a load-time JSON constant) | `PostPass.customUniforms` map via `ManagedUniform` (§3.2) once it exists, or the raw `@Accessor` mixins today | `DynamicUniforms`/`DynamicUniformStorage` — unrelated system, not wired to `PostPass` |
| Manage N intermediate render targets with pooled allocation across frames (bloom, blur, multi-target effects like point-light shadow faces) | `FrameGraphBuilder` + `GraphicsResourceAllocator`/`CrossFrameResourcePool` (§1.5), or `VfxTargetSet` (§3.2) once it exists | Manually new/destroy-ing `TextureTarget`s per frame yourself |
| Get/resize/destroy a screen-sized (or fixed-size) offscreen buffer | `RenderTarget`/`TextureTarget` (§1.6.1) — remember: resize = full destroy+recreate, never in-place | Assuming a resize preserves the old `GpuTextureView` |
| Find where in the frame to inject a post-effect | After `renderLevel()`+`doEntityOutline()`, before GUI, in `GameRenderer.render` (§1.6) — the existing `GameRendererMixin` hook is correct, keep using it | Any hook inside `LevelRenderer` unless you specifically need a pre-composite buffer (`translucent`/`particles`/`weather`/`clouds`/`entity_outline` individually) |
| Decorate an entity's rendered model with extra geometry (glow, armor, elytra) | `net.minecraft.client.renderer.entity.layers.RenderLayer` (§1.7) | **This is not post-processing** — don't go here looking for screen-space effects |
| Register a mod's own persistent post-effect with correct disconnect/reload cleanup | This library — `PostEffectManager`/`VfxManager` (§2, §3.1) | Rolling your own Fabric event registration + manual cleanup, which is how `LightRenderer`'s leak (pain point #8) happened |

**One-paragraph mental model, if you remember nothing else**: `RenderPipeline`
is *what* to draw with (immutable data, built once). `RenderPass`/
`CommandEncoder`/`RenderSystem` are *how* you actually submit a draw this
frame (session objects, opened against explicit targets). `FrameGraphBuilder`
is *when/in what order, with which pooled scratch buffers* a set of passes
run. `PostChain` is MC's own built-in tool for wiring #1–#3 together
specifically for full-screen post-processing, driven by a JSON config. This
library's job is to be a thin, correctly-lifecycle-managed front door onto
`PostChain` — not to reimplement any of the four layers above it.

---

## 5. 26.2 outlook — migration impact map

Written 2026-07-29 from ChampionAsh5357's 26.1.x→26.2 migration primer
(non-exhaustive, code-level only — the NeoForged primer repo). Start the
actual migration from this section instead of re-deriving it — but re-verify
each row against the real 26.2 decompiled sources; the primer is high-level
and some of our touch points (noted below) simply aren't mentioned in it.

### 5.1 What survives

- **The `PostChain` layer itself appears untouched.** The primer has zero
  entries for `PostChain`, `PostPass`, or the `post_effect` JSON format. The
  Vfx architecture (manager/effect lifecycle, `VfxPostChain`, `ManagedUniform`,
  `VfxTargetSet`) survives conceptually intact — the breakage concentrates in
  the plumbing these types were built to hide.
- **The rumored "post-process via command"** is a user-facing pack feature
  (the primer delegates those to Misode's changelog), i.e. a vanilla trigger
  for the same `post_effect` chains — the `GameRenderer.postEffectId`
  mechanism made data-drivable. It can toggle a static whole-screen chain; it
  cannot do per-frame dynamic uniforms, mask targets, SSBOs, or mid-frame
  ordering. Complement to this library, not a replacement.

### 5.2 Library-side breaks — small, one file each, fixed once for all consumers

| 26.2 change (primer ref) | Breaks | Fix shape |
|---|---|---|
| `CommandEncoder.mapBuffer` / `GpuBuffer$MappedView` → `GpuBuffer#map` / `GpuBufferSlice$MappedView` | `ManagedUniform.write` | mechanical |
| `TextureTarget`/`RenderTarget` now take a `GpuFormat` (`RGBA8` → `RGBA8_UNORM`, not 1:1) | `VfxTargetSet` target creation | add format to the builder spec, default `RGBA8_UNORM` |
| `createRenderPass` clear color is `Optional<Vector4fc>`, not `OptionalInt` | `VfxTargetSet.clear` | mechanical |
| `Minecraft.getMainRenderTarget` → `GameRenderer#mainRenderTarget` | `VfxTargetSet.ensureSized`, `VfxPostChain.addToFrame` | mechanical |
| `RenderSystem.getDevice().isZZeroToOne()` → `DeviceInfo#isZZeroToOne` | `LightRenderer.writeUniforms` (consumer, but same class of fix) | mechanical |
| `GameRenderer` accessor renames (`getMainCamera` → `mainCamera`, etc.) | consumers' prepare passes | mechanical |

Not mentioned in the primer, must be source-verified at migration time:
`ShaderManager.getPostChain` signature, `PostPass.customUniforms` field (our
accessor mixin target), `RenderSystem.outputColorTextureOverride` (see 5.3),
`GameRenderer.render`'s `renderLevel` call site (our mixin injection point —
note `LevelRenderer.renderLevel` → `render` and the `LevelRenderer`/
`LevelExtractor` split happened, so the surrounding code moved).

### 5.3 Consumer-side breaks — the real migration work

1. **`MultiBufferSource` is removed entirely** ("Feature Rendering: The
   Takeover"). The mask-draw pattern — `bufferSource.getBuffer(RenderType)` +
   `endBatch` inside `VfxScope.overrideOutput` — is dead. The 26.2-idiomatic
   replacement is cleaner: a custom `FeatureRenderer` (or
   `RenderTypeFeatureRenderer`) whose execute stage opens a `RenderPass`
   against the mask target *directly* — no output-override hack at all.
   Consequences: `VfxScope` likely becomes obsolete; `VfxEffect.prepare`'s
   meaning shifts from "draw the mask now" to "submit mask nodes" (or the
   mask drawing moves into a feature renderer the effect owns). This is a
   *design* change to the prepare contract, not just a rename.
2. **Reverse-Z depth**: vanilla pipelines inverted their depth state
   (`GREATER_THAN_OR_EQUAL`, depth values are additive inverses). The point
   light shader **reconstructs world position from the depth buffer** — that
   math must flip, and the shadow-face perspective matrices with it. The
   sneakiest breakage in this list: it compiles fine and renders garbage.
3. **Fabric `LevelRenderEvents` will be reworked** — `LevelRenderContext.
   bufferSource()` can't survive `MultiBufferSource`'s removal, and
   `LevelRenderer` split into `LevelRenderer`/`LevelExtractor`.
   `VfxLevelPhase`/`VfxPrepareFrame` must track whatever Fabric ships;
   phase names may not map 1:1.
4. **`LightInstanceBuffer`'s persistent-mapped SSBO**: `GlBuffer` mapping
   restructured (`mappedBuffer`, `DeviceFeatures#persistentMapping`,
   `mapBuffer` → `GpuBuffer#map`). Needs rework, and persistent-mapping
   availability is now a queried device feature, not an assumption.
5. **`RenderType.draw` → `PreparedRenderType#drawFromBuffer`** (not 1:1) —
   any consumer code drawing meshes directly through a `RenderType`.
6. **`Tesselator` is removed** — any remaining direct-tessellation code paths
   (none in the three current consumers' post-process code, but check
   `LeklaiRenderTypes` users).
7. **Vulkan backend exists** (`com.mojang.blaze3d.vulkan`, GLSL compiled to
   SPIR-V via `GlslCompiler`). Two follow-ups: post-chain GLSL must survive
   stricter SPIR-V compilation, and `VfxManager.shaderPackActive()` gets a
   sibling question — "which backend is active" (`DeviceInfo#backendName`)
   for any GL-assuming code path.
8. **`RenderPipeline` builder changes** (`withUniform`/`withSampler` →
   `BindGroupLayout` via `withBindGroupLayout`, `withVertexFormat` →
   `withVertexBinding` + `withPrimitiveTopology`) — any custom pipelines in
   consumers (`LeklaiRenderTypes`, `LekLaiRenderPipelines`).

### 5.4 Migration order that minimizes pain

1. Fix the library's mechanical breaks (5.2) — gets `VfxPostChain`/
   `ManagedUniform`/`VfxTargetSet` compiling.
2. Re-verify the unlisted internals (`customUniforms` accessor, `getPostChain`,
   mixin injection point) against 26.2 sources; adjust `ManagedUniform`/mixins.
3. Redesign mask drawing on the feature-renderer system; decide `VfxScope`'s
   fate and whether `prepare` becomes submission-based.
4. Migrate consumers' pipelines/render types (reverse-Z audit included).
5. Last: `LightRenderer` (SSBO mapping + depth reconstruction + shadow
   matrices together, since they must stay consistent).
