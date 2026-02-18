package com.sypztep.plateau.client.impl.ui.debug;

import com.sypztep.plateau.client.impl.ui.core.UIComponent;
import com.sypztep.plateau.client.impl.ui.theme.UITheme;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual inspector for UI components — like browser DevTools or Unreal UMG editor.
 *
 * <h3>Modes (cycle with F3):</h3>
 * <ul>
 *   <li><b>OFF</b> — nothing shown</li>
 *   <li><b>INSPECT</b> — hover a component to see its bounds, padding, and info panel.
 *       Only the hovered component is highlighted. Shows distance guides to screen edges.</li>
 *   <li><b>OUTLINE</b> — all components show a subtle border outline (for spotting overlaps).
 *       Hovered component still gets the full inspector panel.</li>
 *   <li><b>GRID</b> — same as OUTLINE + draws a pixel grid for alignment work.</li>
 * </ul>
 *
 * <h3>What the inspector shows when hovering:</h3>
 * <ul>
 *   <li>Red border — outer bounds</li>
 *   <li>Green border — content area (inside padding)</li>
 *   <li>Green fill — padding zones</li>
 *   <li>Blue dashed lines — distance to screen edges (margin guides)</li>
 *   <li>Info panel — class name, position, size, padding, content size, mouse offset</li>
 *   <li>Crosshair at origin</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class DebugOverlay {

    public enum Mode { OFF, INSPECT, OUTLINE, GRID }

    private static Mode mode = Mode.OFF;
    private static @Nullable UIComponent hoveredComponent = null;
    private static @Nullable UIComponent selectedComponent = null; // click to pin
    private static int screenWidth, screenHeight;

    // Grid settings
    private static final int gridSize = 10;

    // All components registered this frame (rebuilt each frame)
    private static final List<UIComponent> frameComponents = new ArrayList<>();

    private DebugOverlay() {}

    public static boolean isEnabled() { return mode != Mode.OFF; }
    public static Mode getMode() { return mode; }
    public static void setMode(Mode m) { mode = m; }

    public static void toggle() {
        Mode[] modes = Mode.values();
        mode = modes[(mode.ordinal() + 1) % modes.length];
        if (mode == Mode.OFF) {
            selectedComponent = null;
            hoveredComponent = null;
        }
    }

    /**
     * Call at the START of PlateauScreen.render() to reset per-frame tracking.
     */
    public static void beginFrame(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        frameComponents.clear();
        hoveredComponent = null;
    }

    /**
     * Called automatically from UIComponent.render() — registers and inspects.
     */
    public static void renderFor(GuiGraphics graphics, UIComponent component, int mouseX, int mouseY) {
        if (mode == Mode.OFF) return;

        frameComponents.add(component);

        boolean isHovered = component.isMouseOver(mouseX, mouseY);
        if (isHovered) {
            // Topmost hovered component wins (last rendered = on top)
            hoveredComponent = component;
        }

        // OUTLINE and GRID modes: subtle border on everything
        if (mode == Mode.OUTLINE || mode == Mode.GRID) {
            drawBorder(graphics, component.getX(), component.getY(),
                    component.getWidth(), component.getHeight(), 0x40FFFFFF);
        }
    }

    /**
     * Call at the END of PlateauScreen.render() — draws the inspector for the
     * hovered/selected component, grid, and HUD. Must be last so it's on top.
     */
    public static void renderHUD(GuiGraphics graphics, int mouseX, int mouseY) {
        if (mode == Mode.OFF) return;

        // Grid
        if (mode == Mode.GRID) {
            renderGrid(graphics);
        }

        // Determine which component to inspect
        UIComponent target = selectedComponent != null ? selectedComponent : hoveredComponent;

        if (target != null) {
            renderInspector(graphics, target, mouseX, mouseY);
        }

        // Bottom HUD bar
        renderStatusBar(graphics, mouseX, mouseY);

        // Crosshair at mouse
        graphics.fill(mouseX - 6, mouseY, mouseX + 7, mouseY + 1, 0x80FFFFFF);
        graphics.fill(mouseX, mouseY - 6, mouseX + 1, mouseY + 7, 0x80FFFFFF);
    }

    /**
     * Handle click-to-pin in inspect mode.
     * Call from PlateauScreen.mouseClicked when debug is enabled.
     * Returns true if the click was consumed by the debug overlay.
     */
    public static boolean handleClick(double mouseX, double mouseY, int button) {
        if (mode == Mode.OFF) return false;

        // Right-click clears selection
        if (button == 1) {
            selectedComponent = null;
            return true;
        }

        // Left-click pins/unpins the hovered component
        if (button == 0 && hoveredComponent != null) {
            if (selectedComponent == hoveredComponent) {
                selectedComponent = null; // toggle off
            } else {
                selectedComponent = hoveredComponent;
            }
            return true;
        }

        return false;
    }

    // ═══════════════════════════════════════════
    // Inspector rendering for a single component
    // ══════════════════════���════════════════════

    private static void renderInspector(GuiGraphics graphics, UIComponent c, int mouseX, int mouseY) {
        int x = c.getX(), y = c.getY(), w = c.getWidth(), h = c.getHeight();
        int pad = c.getPadding();

        // ── Outer bounds (red) ──
        drawBorder(graphics, x, y, w, h, UITheme.DEBUG_BOUNDS);

        // ── Padding zone (green translucent) ──
        if (pad > 0) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + pad, UITheme.DEBUG_PADDING);
            graphics.fill(x + 1, y + h - pad, x + w - 1, y + h - 1, UITheme.DEBUG_PADDING);
            graphics.fill(x + 1, y + pad, x + pad, y + h - pad, UITheme.DEBUG_PADDING);
            graphics.fill(x + w - pad, y + pad, x + w - 1, y + h - pad, UITheme.DEBUG_PADDING);

            // Content area border (green)
            int cx = c.getContentX(), cy = c.getContentY();
            int cw = c.getContentWidth(), ch = c.getContentHeight();
            drawBorder(graphics, cx, cy, cw, ch, UITheme.DEBUG_CONTENT);
        }

        // ── Origin crosshair ──
        drawBorder(graphics, x - 2, y - 2, 5, 5, UITheme.DEBUG_BOUNDS);

        // ── Distance guides to screen edges ──
        renderDistanceGuides(graphics, c);

        // ── Info panel ──
        renderInfoPanel(graphics, c, mouseX, mouseY);
    }

    /**
     * Draws dashed lines from component edges to screen edges with distance labels.
     */
    private static void renderDistanceGuides(GuiGraphics graphics, UIComponent c) {
        Font font = Minecraft.getInstance().font;
        int x = c.getX(), y = c.getY(), w = c.getWidth(), h = c.getHeight();
        int guideColor = 0x60FF6666;
        int labelColor = 0xFFFF8888;

        // Top distance (component top to screen top)
        if (y > 5) {
            drawDashedLineV(graphics, x + w / 2, 0, y, guideColor);
            String dist = String.valueOf(y);
            graphics.drawString(font, dist, x + w / 2 + 3, y / 2 - font.lineHeight / 2, labelColor, false);
        }

        // Left distance
        if (x > 5) {
            drawDashedLineH(graphics, 0, x, y + h / 2, guideColor);
            String dist = String.valueOf(x);
            graphics.drawString(font, dist, x / 2 - font.width(dist) / 2, y + h / 2 + 3, labelColor, false);
        }

        // Right distance
        int rightDist = screenWidth - (x + w);
        if (rightDist > 5) {
            drawDashedLineH(graphics, x + w, screenWidth, y + h / 2, guideColor);
            String dist = String.valueOf(rightDist);
            graphics.drawString(font, dist, x + w + rightDist / 2 - font.width(dist) / 2,
                    y + h / 2 + 3, labelColor, false);
        }

        // Bottom distance
        int bottomDist = screenHeight - (y + h);
        if (bottomDist > 5) {
            drawDashedLineV(graphics, x + w / 2, y + h, screenHeight, guideColor);
            String dist = String.valueOf(bottomDist);
            graphics.drawString(font, dist, x + w / 2 + 3,
                    y + h + bottomDist / 2 - font.lineHeight / 2, labelColor, false);
        }
    }

    /**
     * Draws the inspector info panel next to the component.
     */
    private static void renderInfoPanel(GuiGraphics graphics, UIComponent c, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;

        String name = c.getDebugLabel() != null ? c.getDebugLabel() : c.getClass().getSimpleName();
        boolean pinned = c == selectedComponent;

        // Build info lines
        List<InfoLine> lines = new ArrayList<>();
        lines.add(new InfoLine((pinned ? "§d⊙ " : "§e") + name, 0));
        lines.add(new InfoLine(String.format("§7x:§f%d  §7y:§f%d", c.getX(), c.getY()), 0));
        lines.add(new InfoLine(String.format("§7w:§f%d  §7h:§f%d", c.getWidth(), c.getHeight()), 0));

        if (c.getPadding() > 0) {
            lines.add(new InfoLine(String.format("§7pad:§a%d  §7content:§a%dx%d",
                    c.getPadding(), c.getContentWidth(), c.getContentHeight()), 0));
        }

        // Mouse offset from component origin
        int mx = mouseX - c.getX();
        int my = mouseY - c.getY();
        lines.add(new InfoLine(String.format("§7mouse offset:§f(%d, %d)", mx, my), 0));

        if (pinned) {
            lines.add(new InfoLine("§8right-click to unpin", 0));
        }

        // Calculate panel dimensions
        int lineH = font.lineHeight + 2;
        int panelPad = 6;
        int maxTextWidth = 0;
        for (InfoLine line : lines) {
            maxTextWidth = Math.max(maxTextWidth, font.width(line.text));
        }
        int panelW = maxTextWidth + panelPad * 2;
        int panelH = lines.size() * lineH + panelPad * 2;

        // Position: right side of component, fallback to left, then below
        int panelX = c.getX() + c.getWidth() + 8;
        int panelY = c.getY();

        // Clamp to screen
        if (panelX + panelW > screenWidth) {
            panelX = c.getX() - panelW - 8;
        }
        if (panelX < 0) {
            panelX = c.getX();
            panelY = c.getY() + c.getHeight() + 4;
        }
        if (panelY + panelH > screenHeight) {
            panelY = screenHeight - panelH - 2;
        }
        if (panelY < 0) panelY = 0;

        // Background with border
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xE8101010);
        drawBorder(graphics, panelX, panelY, panelW, panelH, 0xFF444444);

        // Accent line on left edge
        graphics.fill(panelX, panelY, panelX + 2, panelY + panelH,
                pinned ? 0xFFDD66DD : UITheme.DEBUG_TEXT);

        // Draw lines
        int ty = panelY + panelPad;
        for (InfoLine line : lines) {
            graphics.drawString(font, line.text, panelX + panelPad + 2, ty, UITheme.TEXT_PRIMARY, false);
            ty += lineH;
        }
    }

    // ═══════════════════════════════════════════
    // Grid
    // ═══════════════════════════════════════════

    private static void renderGrid(GuiGraphics graphics) {
        int color = 0x15FFFFFF;
        int majorColor = 0x25FFFFFF;

        for (int gx = 0; gx < screenWidth; gx += gridSize) {
            int c = (gx % (gridSize * 5) == 0) ? majorColor : color;
            graphics.fill(gx, 0, gx + 1, screenHeight, c);
        }
        for (int gy = 0; gy < screenHeight; gy += gridSize) {
            int c = (gy % (gridSize * 5) == 0) ? majorColor : color;
            graphics.fill(0, gy, screenWidth, gy + 1, c);
        }
    }

    // ═══════════════════════════════════════════
    // Status bar
    // ═══════════════════════════════════════════

    private static void renderStatusBar(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;

        String modeName = switch (mode) {
            case OFF -> "OFF";
            case INSPECT -> "INSPECT";
            case OUTLINE -> "OUTLINE";
            case GRID -> "GRID";
        };

        String left = String.format("§e[%s] §fMouse(%d, %d) §7Components: %d",
                modeName, mouseX, mouseY, frameComponents.size());
        String right = "§7F3 cycle mode §8| §7Click pin §8| §7RClick unpin";

        int barH = font.lineHeight + 6;
        int barY = screenHeight - barH;

        // Full-width bar
        graphics.fill(0, barY, screenWidth, screenHeight, 0xE0000000);
        graphics.fill(0, barY, screenWidth, barY + 1, 0xFF333333);

        graphics.drawString(font, left, 4, barY + 3, UITheme.TEXT_PRIMARY, false);
        graphics.drawString(font, right, screenWidth - font.width(right) - 4, barY + 3,
                UITheme.TEXT_PRIMARY, false);
    }

    // ═══════════════════════════════════════════
    // Drawing helpers
    // ═══════════════════════════════════════════

    private static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static void drawDashedLineH(GuiGraphics graphics, int x1, int x2, int y, int color) {
        int dashLen = 4;
        int gapLen = 3;
        for (int dx = Math.min(x1, x2); dx < Math.max(x1, x2); dx += dashLen + gapLen) {
            int end = Math.min(dx + dashLen, Math.max(x1, x2));
            graphics.fill(dx, y, end, y + 1, color);
        }
    }

    private static void drawDashedLineV(GuiGraphics graphics, int x, int y1, int y2, int color) {
        int dashLen = 4;
        int gapLen = 3;
        for (int dy = Math.min(y1, y2); dy < Math.max(y1, y2); dy += dashLen + gapLen) {
            int end = Math.min(dy + dashLen, Math.max(y1, y2));
            graphics.fill(x, dy, x + 1, end, color);
        }
    }

    private record InfoLine(String text, int color) {}
}