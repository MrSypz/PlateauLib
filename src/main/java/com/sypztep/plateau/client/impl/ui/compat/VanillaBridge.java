package com.sypztep.plateau.client.impl.ui.compat;

import com.sypztep.plateau.client.impl.ui.core.UIComponent;
import com.sypztep.plateau.client.impl.ui.widget.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Bridge for migrating vanilla Screen code to PlateauLib components.
 *
 * <h3>Vanilla pattern:</h3>
 * <pre>
 *   graphics.fill(x, y, x+w, y+h, 0xFF1A1A1A);
 *   graphics.drawString(font, "Title", x+10, y+5, 0xFFFFFFFF, true);
 * </pre>
 *
 * <h3>PlateauLib replacement:</h3>
 * <pre>
 *   VanillaBridge.panel(x, y, w, h, "Title")
 * </pre>
 */
@Environment(EnvType.CLIENT)
public final class VanillaBridge {
    private VanillaBridge() {}

    /** Replaces: graphics.fill() + manual border drawing */
    public static UIPanel panel(int x, int y, int w, int h, @Nullable String title) {
        return new UIPanel(x, y, w, h, title != null ? Component.literal(title) : null);
    }

    /** Replaces: graphics.drawString() */
    public static UILabel label(int x, int y, String text) {
        return new UILabel(x, y, Component.literal(text));
    }

    /** Replaces: graphics.drawString() centered in width */
    public static UILabel labelCentered(int x, int y, int width, String text) {
        return new UILabel(x, y, width, Component.literal(text));
    }

    /** Replaces: graphics.drawString() with word wrap */
    public static UIText text(int x, int y, int width, String text) {
        return new UIText(x, y, width, Component.literal(text));
    }

    /** Replaces: manual button rendering + click detection in mouseClicked() */
    public static UIButton button(int x, int y, int w, int h, String text, Runnable onClick) {
        return new UIButton(x, y, w, h, Component.literal(text), btn -> onClick.run());
    }

    /** Button with access to the button instance in the callback */
    public static UIButton button(int x, int y, int w, int h, String text, Consumer<UIButton> onClick) {
        return new UIButton(x, y, w, h, Component.literal(text), onClick);
    }

    /**
     * Replaces: manual scroll offset tracking + graphics.enableScissor() + scroll math.
     * Returns a UIScrollPanel that renders a list of string items.
     */
    public static UIScrollPanel scrollList(int x, int y, int w, int h, @Nullable String title,
                                            List<String> items, int itemHeight) {
        return new UIScrollPanel(x, y, w, h, title != null ? Component.literal(title) : null) {
            @Override
            protected void renderScrollContent(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                                int contentX, int contentY, int contentWidth) {
                for (int i = 0; i < items.size(); i++) {
                    int itemY = contentY + i * itemHeight - getScrollOffset();
                    graphics.drawString(font, items.get(i), contentX, itemY, 0xFFFFFFFF, true);
                }
                setTotalContentHeight(items.size() * itemHeight);
            }
        };
    }

    /**
     * Wraps an entire vanilla Screen rendering block into a UIComponent.
     * This is the escape hatch for complex existing screens.
     */
    public static UIComponent custom(int x, int y, int w, int h, VanillaRenderer renderer) {
        return new UIComponent(x, y, w, h) {
            @Override
            protected void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                renderer.render(graphics, this.x, this.y, this.width, this.height, mouseX, mouseY, delta);
            }
        };
    }

    @FunctionalInterface
    public interface VanillaRenderer {
        void render(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY, float delta);
    }
}
