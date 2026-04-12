package com.sypztep.plateau.client.v1.ui.widget;

import com.sypztep.plateau.client.v1.ui.core.UIComponent;
import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Navigation bar with animated selection indicator and hover effects.
 * Supports horizontal and vertical orientation.
 */
public class UINavBar extends UIComponent {
    private final List<NavItem> items = new ArrayList<>();
    private int selectedIndex = 0;
    private int itemPadding = 10;
    private int itemSpacing = 5;
    private boolean horizontal = true;

    // Selection indicator animation
    private float selectionAnimPos = 0;
    private float selectionAnimSize = 0;
    private float selectionTargetPos = 0;
    private float selectionTargetSize = 0;
    private boolean selectionInitialized = false;

    // Per-item hover animation
    private final Map<Integer, Float> hoverAnimations = new HashMap<>();

    public UINavBar(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public UINavBar addItem(Identifier id, Component label, @Nullable Identifier icon, Consumer<Identifier> onSelect) {
        items.add(new NavItem(id, label, icon, onSelect));
        return this;
    }

    public UINavBar addItem(Identifier id, Component label, Consumer<Identifier> onSelect) {
        return addItem(id, label, null, onSelect);
    }

    public void setActive(Identifier id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(id)) {
                selectedIndex = i;
                updateSelectionTarget();
                break;
            }
        }
    }

    public Identifier getActiveId() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            return items.get(selectedIndex).id;
        }
        return null;
    }

    private void updateSelectionTarget() {
        if (items.isEmpty()) return;

        if (horizontal) {
            int ix = x + itemPadding;
            for (int i = 0; i < items.size(); i++) {
                int w = getItemWidth(items.get(i));
                if (i == selectedIndex) {
                    selectionTargetPos = ix;
                    selectionTargetSize = w;
                    if (!selectionInitialized) {
                        selectionAnimPos = selectionTargetPos;
                        selectionAnimSize = selectionTargetSize;
                        selectionInitialized = true;
                    }
                    break;
                }
                ix += w + itemSpacing;
            }
        } else {
            int iy = y + itemPadding;
            for (int i = 0; i < items.size(); i++) {
                int h = getItemHeight(items.get(i));
                if (i == selectedIndex) {
                    selectionTargetPos = iy;
                    selectionTargetSize = h;
                    if (!selectionInitialized) {
                        selectionAnimPos = selectionTargetPos;
                        selectionAnimSize = selectionTargetSize;
                        selectionInitialized = true;
                    }
                    break;
                }
                iy += h + itemSpacing;
            }
        }
    }

    @Override
    protected void renderComponent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        selectionAnimPos += (selectionTargetPos - selectionAnimPos) * 0.15f;
        selectionAnimSize += (selectionTargetSize - selectionAnimSize) * 0.15f;

        updateHoverAnimations(mouseX, mouseY);

        UITheme theme = UITheme.current();
        graphics.fill(x, y, x + width, y + height, theme.navBg());

        if (horizontal) {
            renderHorizontal(graphics, mouseX, mouseY, theme);
        } else {
            renderVertical(graphics, mouseX, mouseY, theme);
        }

        renderFocusRing(graphics);
    }

    private void updateHoverAnimations(int mouseX, int mouseY) {
        if (horizontal) {
            int ix = x + itemPadding;
            for (int i = 0; i < items.size(); i++) {
                int w = getItemWidth(items.get(i));
                boolean hovered = mouseX >= ix && mouseX < ix + w
                        && mouseY >= y && mouseY < y + height;
                updateItemHover(i, hovered);
                ix += w + itemSpacing;
            }
        } else {
            int iy = y + itemPadding;
            for (int i = 0; i < items.size(); i++) {
                int h = getItemHeight(items.get(i));
                boolean hovered = mouseX >= x && mouseX < x + width
                        && mouseY >= iy && mouseY < iy + h;
                updateItemHover(i, hovered);
                iy += h + itemSpacing;
            }
        }
    }

    private void updateItemHover(int index, boolean hovered) {
        float current = hoverAnimations.getOrDefault(index, 0f);
        current = stepAnimation(current, hovered, 0.08f);
        if (current < 0.01f) {
            hoverAnimations.remove(index);
        } else {
            hoverAnimations.put(index, current);
        }
    }

    private void renderHorizontal(GuiGraphicsExtractor graphics, int mouseX, int mouseY, UITheme theme) {
        int ix = x + itemPadding;
        int ih = height - itemPadding * 2;

        graphics.fill((int) selectionAnimPos, y + height - 3,
                (int) (selectionAnimPos + selectionAnimSize), y + height,
                theme.navIndicator());

        for (int i = 0; i < items.size(); i++) {
            NavItem item = items.get(i);
            int iw = getItemWidth(item);
            boolean selected = i == selectedIndex;
            float hover = hoverAnimations.getOrDefault(i, 0f);

            renderItem(graphics, item, ix, y + itemPadding, iw, ih, selected, hover, theme);
            ix += iw + itemSpacing;
        }
    }

    private void renderVertical(GuiGraphicsExtractor graphics, int mouseX, int mouseY, UITheme theme) {
        int iy = y + itemPadding;
        int iw = width - itemPadding * 2;

        graphics.fill(x, (int) selectionAnimPos, x + 3,
                (int) (selectionAnimPos + selectionAnimSize),
                theme.navIndicator());

        for (int i = 0; i < items.size(); i++) {
            NavItem item = items.get(i);
            int ih = getItemHeight(item);
            boolean selected = i == selectedIndex;
            float hover = hoverAnimations.getOrDefault(i, 0f);

            renderItemVertical(graphics, item, x + itemPadding, iy, iw, ih, selected, hover, theme);
            iy += ih + itemSpacing;
        }
    }

    private void renderItem(GuiGraphicsExtractor graphics, NavItem item, int ix, int iy, int iw, int ih,
                            boolean selected, float hover, UITheme theme) {
        int baseColor = selected ? 0xFFFFFFFF : theme.textSecondary();
        int hoverTarget = selected ? 0xFFFFFFFF : 0xFFE0E0E0;
        int textColor = ARGB.srgbLerp(hover, baseColor, hoverTarget );

        int textY = iy + (ih - font.lineHeight) / 2;
        int textX = ix;
        int iconSize = 16;

        float scale = 1.0f + 0.05f * hover;
        graphics.pose().pushMatrix();
        float cx = textX + iw / 2f;
        float cy = iy + ih / 2f;
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-cx, -cy);

        if (item.icon != null) {
            int iconY = iy + (ih - iconSize) / 2;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, item.icon, textX, iconY, iconSize, iconSize);
            textX += iconSize + 5;
        }

        graphics.text(font, item.label, textX, textY, textColor, true);
        graphics.pose().popMatrix();
    }

    private void renderItemVertical(GuiGraphicsExtractor graphics, NavItem item, int ix, int iy, int iw, int ih,
                                    boolean selected, float hover, UITheme theme) {
        int baseColor = selected ? 0xFFFFFFFF : theme.textSecondary();
        int hoverTarget = selected ? 0xFFFFFFFF : 0xFFE0E0E0;
        int textColor = ARGB.srgbLerp(hover, baseColor, hoverTarget);

        int textY = iy + (ih - font.lineHeight) / 2;
        int iconSize = 16;

        float scale = 1.0f + 0.05f * hover;
        graphics.pose().pushMatrix();
        float cx = ix + iw / 2f;
        float cy = iy + ih / 2f;
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-cx, -cy);

        int contentW = font.width(item.label);
        if (item.icon != null) contentW += iconSize + 5;
        int curX = ix + (iw - contentW) / 2;

        if (item.icon != null) {
            int iconY = iy + (ih - iconSize) / 2;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, item.icon, curX, iconY, iconSize, iconSize);
            curX += iconSize + 5;
        }

        graphics.text(font, item.label, curX, textY, textColor, true);
        graphics.pose().popMatrix();
    }

    // ═══════════════════════════════════════════
    // Input
    // ═══════════════════════════════════════════

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        double mx = event.x(), my = event.y();

        if (horizontal) {
            int ix = x + itemPadding;
            int iy = y + itemPadding;
            int ih = height - itemPadding * 2;

            for (int i = 0; i < items.size(); i++) {
                NavItem item = items.get(i);
                int iw = getItemWidth(item);

                if (mx >= ix && mx < ix + iw && my >= iy && my < iy + ih) {
                    selectItem(i);
                    return true;
                }
                ix += iw + itemSpacing;
            }
        } else {
            int ix = x + itemPadding;
            int iy = y + itemPadding;
            int iw = width - itemPadding * 2;

            for (int i = 0; i < items.size(); i++) {
                NavItem item = items.get(i);
                int ih = getItemHeight(item);

                if (mx >= ix && mx < ix + iw && my >= iy && my < iy + ih) {
                    selectItem(i);
                    return true;
                }
                iy += ih + itemSpacing;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (!focused) return false;

        int key = keyEvent.key();
        // Arrow keys (horizontal: left/right, vertical: up/down)
        boolean prev = (horizontal && key == 263) || (!horizontal && key == 265);
        boolean next = (horizontal && key == 262) || (!horizontal && key == 264);

        if (prev && selectedIndex > 0) {
            selectItem(selectedIndex - 1);
            return true;
        }
        if (next && selectedIndex < items.size() - 1) {
            selectItem(selectedIndex + 1);
            return true;
        }

        // Enter/Space to confirm
        if (key == 257 || key == 32) {
            NavItem item = items.get(selectedIndex);
            if (item.onSelect != null) item.onSelect.accept(item.id);
            return true;
        }

        return false;
    }

    /**
     * Navigate to next/previous tab programmatically.
     * Useful for controller bumpers (LB/RB) mapped from the screen level.
     */
    public boolean selectNext() {
        if (selectedIndex < items.size() - 1) {
            selectItem(selectedIndex + 1);
            return true;
        }
        return false;
    }

    public boolean selectPrevious() {
        if (selectedIndex > 0) {
            selectItem(selectedIndex - 1);
            return true;
        }
        return false;
    }

    private void selectItem(int index) {
        if (index == selectedIndex) return;
        selectedIndex = index;
        updateSelectionTarget();

        UISounds.playTabSwitch();

        NavItem item = items.get(index);
        if (item.onSelect != null) {
            item.onSelect.accept(item.id);
        }
    }

    // ═══════════════════════════════════════════
    // Sizing helpers
    // ═══════════════════════════════════════════

    private int getItemWidth(NavItem item) {
        int w = font.width(item.label);
        if (item.icon != null) w += 16 + 5;
        return w;
    }

    private int getItemHeight(NavItem item) {
        return Math.max(font.lineHeight, 16);
    }

    // ═══════════════════════════════════════════
    // Narration
    // ═══════════════════════════════════════════

    @Override
    public void updateNarration(NarrationElementOutput output) {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            output.add(NarratedElementType.TITLE,
                    Component.translatable("narration.tab", items.get(selectedIndex).label));
            output.add(NarratedElementType.USAGE,
                    Component.literal("Use arrow keys to switch tabs"));
        }
    }

    // Fluent setters
    public UINavBar setOrientation(boolean horizontal) { this.horizontal = horizontal; return this; }
    public UINavBar setItemPadding(int padding) { this.itemPadding = padding; return this; }
    public UINavBar setItemSpacing(int spacing) { this.itemSpacing = spacing; return this; }

    private record NavItem(Identifier id, Component label, @Nullable Identifier icon, Consumer<Identifier> onSelect) {}
}
