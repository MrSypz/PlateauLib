package com.sypztep.plateau.client.v2.ui.container;

import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.BaseContainerComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.overlay.WindowControls;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class PanelComponent extends BaseContainerComponent<PanelComponent> {
    private static final int DEFAULT_HEADER_HEIGHT = 20;

    private Component title;
    private int headerHeight = DEFAULT_HEADER_HEIGHT;
    private Insets bodyPadding = Insets.of(6, 8);
    private int gap = 6;
    private boolean hoverSurface = true;
    private float hoverProgress = 0f;
    private final List<PanelControl> controls = new ArrayList<>();

    public PanelComponent(Component title) {
        this.title = title;
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing = Sizing.content();
    }

    public PanelComponent(String title) {
        this(Component.literal(title));
    }

    public PanelComponent title(Component title) {
        this.title = title;
        return this;
    }

    public PanelComponent title(String title) {
        return title(Component.literal(title));
    }

    public PanelComponent headerHeight(int headerHeight) {
        this.headerHeight = Math.max(0, headerHeight);
        remount();
        return this;
    }

    public PanelComponent bodyPadding(Insets bodyPadding) {
        this.bodyPadding = bodyPadding == null ? Insets.none() : bodyPadding;
        remount();
        return this;
    }

    public PanelComponent gap(int gap) {
        this.gap = Math.max(0, gap);
        remount();
        return this;
    }

    public PanelComponent hoverSurface(boolean hoverSurface) {
        this.hoverSurface = hoverSurface;
        return this;
    }

    public PanelComponent control(WindowControls.Type type, Consumer<PanelComponent> action) {
        controls.add(new PanelControl(type, action));
        return this;
    }

    @Override
    protected void onMounted() {
        remount();
    }

    @Override
    public int determineVerticalContentSize(int availableWidth) {
        int contentWidth = Math.max(0, availableWidth - bodyPadding.horizontal());
        int total = headerHeight + bodyPadding.vertical() + gap * Math.max(0, visibleChildren().size() - 1);
        for (BaseComponent<?> child : visibleChildren()) {
            int childWidth = Math.max(0, contentWidth - child.margins().horizontal());
            total += childContentHeight(child, childWidth) + child.margins().vertical();
        }
        return total;
    }

    @Override
    public int determineHorizontalContentSize(int availableWidth) {
        int max = font.width(title) + WindowControls.titleX(0, controls.size()) + 8;
        int contentWidth = Math.max(0, availableWidth - bodyPadding.horizontal());
        for (BaseComponent<?> child : visibleChildren()) {
            int childWidth = Math.max(0, contentWidth - child.margins().horizontal());
            max = Math.max(max, childContentWidth(child, childWidth) + child.margins().horizontal() + bodyPadding.horizontal());
        }
        return max;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        UITheme theme = UITheme.current();
        if (hoverSurface) {
            hoverProgress = stepAnimation(hoverProgress, isMouseOver(mouseX, mouseY), 0.5f, delta);
        }
        int bg     = ARGB.srgbLerp(hoverProgress, theme.panel().bg(), theme.panel().bgHover());
        int border = ARGB.srgbLerp(hoverProgress, theme.panel().border(), theme.panel().borderHover());
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, bg);
        graphics.outline(x, y, width, height, border);
        graphics.fill(x + 2, y + 2, x + width - 2, y + headerHeight, theme.panel().headerBg());

        for (int index = 0; index < controls.size(); index++) {
            PanelControl control = controls.get(index);
            int controlX = WindowControls.x(x, index);
            int controlY = WindowControls.y(y);
            WindowControls.draw(graphics, control.type(), controlX, controlY, WindowControls.hit(mouseX, mouseY, controlX, controlY));
        }

        graphics.text(font, title, WindowControls.titleX(x, controls.size()), y + 6, theme.text().accent(), true);
        graphics.fill(x + 2, y + headerHeight, x + width - 2, y + headerHeight + 1, theme.panel().border());

        graphics.enableScissor(x, y + headerHeight, x + width, y + height);
        for (BaseComponent<?> child : children) {
            if (child.isVisible()) {
                boolean hoverBlocked = isMouseBlockedByTopChild(child, mouseX, mouseY);
                child.extractRenderState(graphics,
                        hoverBlocked ? hoverSuppressedMouse() : mouseX,
                        hoverBlocked ? hoverSuppressedMouse() : mouseY,
                        delta);
            }
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) return false;

        for (int index = 0; index < controls.size(); index++) {
            int controlX = WindowControls.x(x, index);
            int controlY = WindowControls.y(y);
            if (WindowControls.hit(event.x(), event.y(), controlX, controlY)) {
                Consumer<PanelComponent> action = controls.get(index).action();
                if (action != null) action.accept(this);
                UISounds.playClick();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    protected void remount() {
        if (width <= 0 || height <= headerHeight) return;

        List<BaseComponent<?>> visible = visibleChildren();
        int contentX = x + bodyPadding.left();
        int contentY = y + headerHeight + bodyPadding.top();
        int contentWidth = Math.max(0, width - bodyPadding.horizontal());
        int contentHeight = Math.max(0, height - headerHeight - bodyPadding.vertical());

        int fixedHeight = gap * Math.max(0, visible.size() - 1);
        int totalFillWeight = 0;
        int[] childHeights = new int[visible.size()];

        for (int index = 0; index < visible.size(); index++) {
            BaseComponent<?> child = visible.get(index);
            int childWidth = Math.max(0, contentWidth - child.margins().horizontal());
            switch (child.verticalSizing()) {
                case Sizing.Fixed fixed -> {
                    childHeights[index] = fixed.value();
                    fixedHeight += fixed.value() + child.margins().vertical();
                }
                case Sizing.Content ignored -> {
                    childHeights[index] = child.determineVerticalContentSize(childWidth);
                    fixedHeight += childHeights[index] + child.margins().vertical();
                }
                case Sizing.Fill fill -> totalFillWeight += Math.max(0, fill.weight());
            }
        }

        int remaining = Math.max(0, contentHeight - fixedHeight);
        float fillUnit = totalFillWeight > 0 ? (float) remaining / totalFillWeight : 0f;
        for (int index = 0; index < visible.size(); index++) {
            BaseComponent<?> child = visible.get(index);
            if (child.verticalSizing() instanceof Sizing.Fill(int weight)) {
                childHeights[index] = Math.max(0, Math.round(Math.max(0, weight) * fillUnit));
            }
        }

        int nextY = contentY;
        for (int index = 0; index < visible.size(); index++) {
            BaseComponent<?> child = visible.get(index);
            int childX = contentX + child.margins().left();
            int childWidth = switch (child.horizontalSizing()) {
                case Sizing.Fixed fixed -> fixed.value();
                case Sizing.Fill ignored -> Math.max(0, contentWidth - child.margins().horizontal());
                case Sizing.Content ignored -> child.determineHorizontalContentSize(Math.max(0, contentWidth - child.margins().horizontal()));
            };
            child.mount(childX, nextY + child.margins().top(), childWidth, childHeights[index]);
            nextY += childHeights[index] + child.margins().vertical() + (index < visible.size() - 1 ? gap : 0);
        }
    }

    private List<BaseComponent<?>> visibleChildren() {
        return children.stream().filter(BaseComponent::isVisible).toList();
    }

    private static int childContentHeight(BaseComponent<?> child, int availableWidth) {
        return switch (child.verticalSizing()) {
            case Sizing.Fixed fixed -> fixed.value();
            case Sizing.Content ignored -> child.determineVerticalContentSize(availableWidth);
            case Sizing.Fill ignored -> 0;
        };
    }

    private static int childContentWidth(BaseComponent<?> child, int availableWidth) {
        return switch (child.horizontalSizing()) {
            case Sizing.Fixed fixed -> fixed.value();
            case Sizing.Content ignored -> child.determineHorizontalContentSize(availableWidth);
            case Sizing.Fill ignored -> availableWidth;
        };
    }

    private boolean isMouseBlockedByTopChild(BaseComponent<?> target, int mouseX, int mouseY) {
        for (BaseComponent<?> child : children) {
            if (child != target
                    && child.isVisible()
                    && child.rendersAboveSiblings()
                    && child.blocksLowerInput()
                    && child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }

        for (int index = children.size() - 1; index >= 0; index--) {
            BaseComponent<?> child = children.get(index);
            if (child == target) return false;
            if (child.isVisible()
                    && !child.rendersAboveSiblings()
                    && child.blocksLowerInput()
                    && child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    // NOTE: this one is critical it not suppose to using Runnable
    private record PanelControl(WindowControls.Type type, Consumer<PanelComponent> action) {}
}
