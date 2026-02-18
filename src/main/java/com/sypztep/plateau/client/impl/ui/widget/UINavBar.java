package com.sypztep.plateau.client.impl.ui.widget;

import com.sypztep.plateau.client.impl.ui.core.UIColors;
import com.sypztep.plateau.client.impl.ui.core.UIComponent;
import com.sypztep.plateau.client.impl.ui.theme.UITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Add a navigation item with icon.
     */
    public UINavBar addItem(String id, Component label, @Nullable Identifier icon, Consumer<String> onSelect) {
        items.add(new NavItem(id, label, icon, onSelect));
        return this;
    }

    /**
     * Add a navigation item without icon.
     */
    public UINavBar addItem(String id, Component label, Consumer<String> onSelect) {
        return addItem(id, label, null, onSelect);
    }

    /**
     * Set active item by ID.
     */
    public void setActive(String id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(id)) {
                selectedIndex = i;
                updateSelectionTarget();
                break;
            }
        }
    }

    public String getActiveId() {
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
    protected void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Animate selection indicator — fixed speed, no delta
        selectionAnimPos += (selectionTargetPos - selectionAnimPos) * 0.15f;
        selectionAnimSize += (selectionTargetSize - selectionAnimSize) * 0.15f;

        // Update per-item hover
        updateHoverAnimations(mouseX, mouseY);

        // Background
        graphics.fill(x, y, x + width, y + height, UITheme.NAV_BG);

        if (horizontal) {
            renderHorizontal(graphics, mouseX, mouseY);
        } else {
            renderVertical(graphics, mouseX, mouseY);
        }
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

    // ═══════════════════════════════════════════
    // Horizontal rendering
    // ═══════════════════════════════════════════

    private void renderHorizontal(GuiGraphics graphics, int mouseX, int mouseY) {
        int ix = x + itemPadding;
        int ih = height - itemPadding * 2;

        // Selection indicator — bottom bar
        graphics.fill((int) selectionAnimPos, y + height - 3,
                (int) (selectionAnimPos + selectionAnimSize), y + height,
                UITheme.NAV_INDICATOR);

        for (int i = 0; i < items.size(); i++) {
            NavItem item = items.get(i);
            int iw = getItemWidth(item);
            boolean selected = i == selectedIndex;
            float hover = hoverAnimations.getOrDefault(i, 0f);

            renderItem(graphics, item, ix, y + itemPadding, iw, ih, selected, hover);

            ix += iw + itemSpacing;
        }
    }

    // ═══════════════════════════════════════════
    // Vertical rendering
    // ═══════════════════════════════════════════

    private void renderVertical(GuiGraphics graphics, int mouseX, int mouseY) {
        int iy = y + itemPadding;
        int iw = width - itemPadding * 2;

        // Selection indicator — left bar
        graphics.fill(x, (int) selectionAnimPos, x + 3,
                (int) (selectionAnimPos + selectionAnimSize),
                UITheme.NAV_INDICATOR);

        for (int i = 0; i < items.size(); i++) {
            NavItem item = items.get(i);
            int ih = getItemHeight(item);
            boolean selected = i == selectedIndex;
            float hover = hoverAnimations.getOrDefault(i, 0f);

            renderItemVertical(graphics, item, x + itemPadding, iy, iw, ih, selected, hover);

            iy += ih + itemSpacing;
        }
    }

    // ═══════════════════════════════════════════
    // Item rendering
    // ═══════════════════════════════════════════

    private void renderItem(GuiGraphics graphics, NavItem item, int ix, int iy, int iw, int ih,
                            boolean selected, float hover) {
        int baseColor = selected ? 0xFFFFFFFF : UITheme.TEXT_SECONDARY;
        int hoverTarget = selected ? 0xFFFFFFFF : 0xFFE0E0E0;
        int textColor = UIColors.interpolate(baseColor, hoverTarget, hover);

        int textY = iy + (ih - font.lineHeight) / 2;
        int textX = ix;
        int iconSize = 16;

        // Scale on hover
        float scale = 1.0f + 0.05f * hover;
        graphics.pose().pushMatrix();
        float cx = textX + iw / 2f;
        float cy = iy + ih / 2f;
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-cx, -cy);

        if (item.icon != null) {
            int iconY = iy + (ih - iconSize) / 2;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED ,item.icon, textX, iconY, iconSize, iconSize);
            textX += iconSize + 5;
        }

        graphics.drawString(font, item.label, textX, textY, textColor, true);

        graphics.pose().popMatrix();
    }

    private void renderItemVertical(GuiGraphics graphics, NavItem item, int ix, int iy, int iw, int ih,
                                    boolean selected, float hover) {
        int baseColor = selected ? 0xFFFFFFFF : UITheme.TEXT_SECONDARY;
        int hoverTarget = selected ? 0xFFFFFFFF : 0xFFE0E0E0;
        int textColor = UIColors.interpolate(baseColor, hoverTarget, hover);

        int textY = iy + (ih - font.lineHeight) / 2;
        int iconSize = 16;

        float scale = 1.0f + 0.05f * hover;
        graphics.pose().pushMatrix();
        float cx = ix + iw / 2f;
        float cy = iy + ih / 2f;
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-cx, -cy);

        // Center icon + text
        int contentW = font.width(item.label);
        if (item.icon != null) contentW += iconSize + 5;
        int curX = ix + (iw - contentW) / 2;

        if (item.icon != null) {
            int iconY = iy + (ih - iconSize) / 2;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED ,item.icon, curX, iconY, iconSize, iconSize);
            curX += iconSize + 5;
        }

        graphics.drawString(font, item.label, curX, textY, textColor, true);

        graphics.pose().popMatrix();
    }

    // ═══════════════════════════════════════════
    // Input
    // ═══════════════════════════════════════════

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
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

    /**
     * Arrow keys navigate between items when focused.
     */
    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!focused) return false;

        int key = keyEvent.key();
        // Left/Up = previous, Right/Down = next
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

        // Enter/Space activates current
        if (key == 257 || key == 32) {
            NavItem item = items.get(selectedIndex);
            if (item.onSelect != null) item.onSelect.accept(item.id);
            return true;
        }

        return false;
    }

    private void selectItem(int index) {
        if (index == selectedIndex) return;
        selectedIndex = index;
        updateSelectionTarget();

        minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.5f));

        NavItem item = items.get(index);
        if (item.onSelect != null) {
            item.onSelect.accept(item.id);
        }
    }

    // ═══════════════════════════════════════════
    // Focus visual
    // ═══════════════════════════════════════════

    // NavBar renders its own focus ring around the whole bar
    // since it handles internal navigation via arrow keys

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

    // ═══════════════════════════════════════════
    // Fluent setters
    // ═══════════════════════════════════════════

    public UINavBar setOrientation(boolean horizontal) { this.horizontal = horizontal; return this; }
    public UINavBar setItemPadding(int padding) { this.itemPadding = padding; return this; }
    public UINavBar setItemSpacing(int spacing) { this.itemSpacing = spacing; return this; }

    private record NavItem(String id, Component label, @Nullable Identifier icon, Consumer<String> onSelect) {}
}