package com.sypztep.plateau.client.v2.ui.overlay;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Singleton overlay that renders hover-card panels above all other UI content.
 * Driven by {@link HoverCardComponent}: each frame where a card is hovered,
 * the component calls {@link #schedule} which queues the card for rendering.
 *
 * Called automatically from {@link BaseComponent#extractRenderState} at the root level
 * (same pattern as {@link TooltipOverlay}). No manual wiring needed.
 */
@Environment(EnvType.CLIENT)
public final class HoverCardOverlay {

    private static final int CARD_PAD  = 8;
    private static final int CURSOR_GAP = 12;
    private static final int SUPPRESS  = Integer.MIN_VALUE / 4;

    private static @Nullable BaseComponent<?> pendingCard;
    private static @Nullable BaseComponent<?> activeCard;
    private static int pendingX, pendingY, pendingWidth;
    private static float openProgress = 0f;

    private HoverCardOverlay() {}

    /** Called at the start of each root render frame to reset the pending card. */
    public static void beginFrame() {
        pendingCard = null;
    }

    /**
     * Schedule a card for rendering this frame near the given screen-space position.
     * Called by {@link HoverCardComponent} while it is in the open state.
     */
    public static void schedule(BaseComponent<?> card, int screenX, int screenY, int cardWidth) {
        pendingCard  = card;
        activeCard   = card;
        pendingX     = screenX;
        pendingY     = screenY;
        pendingWidth = cardWidth;
    }

    /** Called at the end of each root render frame to draw the scheduled card. */
    public static void render(GuiGraphicsExtractor g) {
        float delta = BaseComponent.lastDelta();
        openProgress = stepAnim(openProgress, pendingCard != null, 0.45f, delta);

        if (openProgress < 0.01f) { activeCard = null; return; }
        if (activeCard == null)   return;

        UITheme theme  = UITheme.current();
        Minecraft mc   = Minecraft.getInstance();
        int screenW    = mc.getWindow().getGuiScaledWidth();
        int screenH    = mc.getWindow().getGuiScaledHeight();

        int cw     = pendingWidth;
        int innerW = Math.max(1, cw - CARD_PAD * 2);
        int ch     = activeCard.determineVerticalContentSize(innerW) + CARD_PAD * 2;
        ch         = Math.max(ch, 24);

        // Position below-right of cursor, clamped inside screen
        int cx = Mth.clamp(pendingX + CURSOR_GAP, 2, screenW - cw - 2);
        int cy = Mth.clamp(pendingY + CURSOR_GAP, 2, screenH - ch - 2);

        activeCard.mount(cx + CARD_PAD, cy + CARD_PAD, innerW, Math.max(1, ch - CARD_PAD * 2));

        float scale  = Mth.lerp(easeOut(openProgress), 0.92f, 1.0f);
        int pivotX = cx + cw / 2, pivotY = cy;

        g.pose().pushMatrix();
        g.pose().translate(pivotX, pivotY);
        g.pose().scale(scale, scale);
        g.pose().translate(-pivotX, -pivotY);

        g.fill(cx + 1, cy + 1, cx + cw - 1, cy + ch - 1, theme.panel().bg());
        g.outline(cx, cy, cw, ch, theme.panel().border());
        g.outline(cx + 1, cy + 1, cw - 2, ch - 2, theme.panel().border());

        // Render card content (non-interactive); prevent re-entrant root-render logic.
        BaseComponent.enterOverlayRender();
        try {
            activeCard.extractRenderState(g, SUPPRESS, SUPPRESS, delta);
        } finally {
            BaseComponent.exitOverlayRender();
        }

        g.pose().popMatrix();
    }

    private static float stepAnim(float cur, boolean active, float speed, float dt) {
        float amt = speed * dt;
        return active ? Math.min(1f, cur + amt) : Math.max(0f, cur - amt);
    }

    private static float easeOut(float t) { float inv = 1f - t; return 1f - inv * inv * inv; }
}
