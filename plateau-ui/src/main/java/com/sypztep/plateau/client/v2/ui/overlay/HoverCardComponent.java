package com.sypztep.plateau.client.v2.ui.overlay;

import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.BaseContainerComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Hover-triggered card overlay — wrapper edition.
 *
 * Place this component where the trigger would normally go in your layout tree.
 * It wraps the trigger child, inheriting its sizing, and forwards all input and
 * rendering to it. When the user hovers over the trigger for {@code openDelay} ms,
 * the card content is rendered above all other UI via {@link HoverCardOverlay}
 * (no scissor conflicts, no coordinate-space issues).
 *
 * <pre>{@code
 *   someContainer.child(
 *       Overlays.hoverCard()
 *           .wrap(WidgetComponents.label("Hover me").sizing(Sizing.fill(), Sizing.fixed(20)))
 *           .card(Containers.vertical(Sizing.fill(), Sizing.content())
 *               .padding(Insets.of(6))
 *               .child(WidgetComponents.label("Card title"))
 *               .child(WidgetComponents.text("Some description.")))
 *           .openDelay(500)
 *           .closeDelay(200)
 *           .cardWidth(180)
 *   );
 * }</pre>
 *
 * {@link HoverCardOverlay} is driven automatically by {@link BaseComponent#extractRenderState};
 * no separate overlay component needs to be added to the screen root.
 *
 * <p><b>Dense lists:</b> the default {@code closeDelay} keeps a card open for a grace period after
 * the mouse leaves the trigger, which is what you want for one isolated trigger with open space
 * around it. But {@link #extract} re-schedules the card every frame it's still open, anchored at
 * the CURRENT mouse position rather than the trigger — so for many {@code HoverCardComponent}s
 * wrapping small rows packed tightly in a list, the lingering card visibly follows the cursor into
 * whatever row or gap it moves to next, reading as a ghost tooltip. If wrapping many small,
 * densely-packed triggers (e.g. rows in a list), use {@code openDelay(0).closeDelay(0)}, or drive
 * {@link HoverCardOverlay#schedule} directly once per frame from the row's own current-frame hover
 * state instead of one {@code HoverCardComponent} per row.
 */
@Environment(EnvType.CLIENT)
public class HoverCardComponent extends BaseContainerComponent<HoverCardComponent> {

    private @Nullable BaseComponent<?> triggerChild;
    private @Nullable BaseComponent<?> cardContent;
    private int openDelay  = 500;
    private int closeDelay = 200;
    private int cardWidth  = 180;

    private boolean open       = false;
    private long    hoverStart = -1L;
    private long    leaveTime  = -1L;

    public HoverCardComponent() {}

    // ── Fluent API ────────────────────────────────────────────

    /**
     * Wrap a trigger component. The HoverCard inherits the trigger's sizing and
     * forwards all input and rendering to it.
     */
    public HoverCardComponent wrap(BaseComponent<?> trigger) {
        if (triggerChild != null) children.remove(triggerChild);
        triggerChild          = trigger;
        this.horizontalSizing = trigger.horizontalSizing();
        this.verticalSizing   = trigger.verticalSizing();
        children.add(0, trigger);
        return this;
    }

    /** Card content to display in the hover overlay (non-interactive). */
    public HoverCardComponent card(BaseComponent<?> content) {
        this.cardContent = content;
        return this;
    }

    /** Milliseconds of continuous hover before the card opens (default 500). */
    public HoverCardComponent openDelay(int ms)  { this.openDelay  = Math.max(0, ms); return this; }

    /** Milliseconds after the mouse leaves before the card closes (default 200). */
    public HoverCardComponent closeDelay(int ms) { this.closeDelay = Math.max(0, ms); return this; }

    /** Width of the card panel in pixels; height is derived from content (default 180). */
    public HoverCardComponent cardWidth(int w)   { this.cardWidth  = Math.max(60, w); return this; }

    // ── Content size (delegates to trigger) ───────────────────

    @Override
    public int determineHorizontalContentSize(int space) {
        return triggerChild != null ? triggerChild.determineHorizontalContentSize(space) : 0;
    }

    @Override
    public int determineVerticalContentSize(int space) {
        return triggerChild != null ? triggerChild.determineVerticalContentSize(space) : 0;
    }

    // ── Layout ────────────────────────────────────────────────

    @Override
    protected void onMounted() {
        if (triggerChild != null) {
            triggerChild.mount(
                x + triggerChild.margins().left(),
                y + triggerChild.margins().top(),
                Math.max(0, width  - triggerChild.margins().horizontal()),
                Math.max(0, height - triggerChild.margins().vertical())
            );
        }
    }

    // ── Rendering ─────────────────────────────────────────────

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        extractChildrenInLayerOrder(g, mouseX, mouseY, delta);

        // mouseX/mouseY are content-space here (scroll offset already applied by parent),
        // so isMouseOver() correctly detects hover regardless of scroll position.
        updateHoverState(mouseX, mouseY);

        if (open && cardContent != null) {
            // lastScreenMouseX/Y are screen-space from the current root render —
            // correct for positioning the card panel above all content.
            HoverCardOverlay.schedule(
                cardContent,
                BaseComponent.lastScreenMouseX(),
                BaseComponent.lastScreenMouseY(),
                cardWidth
            );
        }
    }

    // ── Hover tracking ────────────────────────────────────────

    private void updateHoverState(int mouseX, int mouseY) {
        boolean over = isMouseOver(mouseX, mouseY);
        long    now  = Util.getMillis();

        if (over) {
            leaveTime = -1L;
            if (hoverStart < 0L) hoverStart = now;
            if (!open && now - hoverStart >= openDelay) open = true;
        } else {
            hoverStart = -1L;
            if (open) {
                if (leaveTime < 0L) leaveTime = now;
                if (now - leaveTime >= closeDelay) { open = false; leaveTime = -1L; }
            }
        }
    }
}
