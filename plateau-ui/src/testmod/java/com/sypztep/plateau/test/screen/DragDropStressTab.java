package com.sypztep.plateau.test.screen;

import com.sypztep.plateau.client.v1.ui.screen.TabContext;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.WidgetComponents;
import com.sypztep.plateau.client.v2.ui.Containers;
import com.sypztep.plateau.client.v2.ui.Overlays;
import com.sypztep.plateau.client.v2.ui.Panels;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.interaction.DragDrop;
import com.sypztep.plateau.client.v2.ui.interaction.DragSource;
import com.sypztep.plateau.client.v2.ui.interaction.DropTarget;
import com.sypztep.plateau.client.v2.ui.overlay.DetachablePanel;
import com.sypztep.plateau.client.v2.ui.screen.Tab2;
import com.sypztep.plateau.test.UITestClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class  DragDropStressTab extends Tab2 {
    private final List<DragItem> items = List.of(
            new DragItem("Ruby Core", 0xFFE25555),
            new DragItem("Verdant Seal", 0xFF5AB66A),
            new DragItem("Azure Lens", 0xFF5A8DFF),
            new DragItem("Amber Key", 0xFFFFB65A),
            new DragItem("Violet Sigil", 0xFFB45AFF),
            new DragItem("Silver Thread", 0xFFD0D0D0)
    );
    private final Map<String, DragItem> drops = new LinkedHashMap<>();

    public DragDropStressTab() {
        super(UITestClient.id("drag_drop"), Component.literal("Drag/Drop"));
    }

    @Override
    protected BaseComponent<?> build(TabContext ctx) {
        return Overlays.windowLayer().content(Containers.verticalFill()
                .padding(Insets.of(10, 20, 20, 20))
                .gap(6)
                .child(WidgetComponents.text("Drag an item from Sources, hover Target A/B tab while still dragging, then drop into a slot. Also detach the panel and repeat.")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(Panels.detachable("Tabbed Drag Window")
                        .content(tabbedWindow())
                        .windowSize(560, 300)
                        .openTrigger(DetachablePanel.WindowOpenTrigger.ICON_ONLY)
                        .sizing(Sizing.fill(), Sizing.fill())));
    }

    private BaseComponent<?> tabbedWindow() {
        return Containers.tabs()
                .padding(Insets.of(6))
                .tab("Sources", sourceTab())
                .tab("Target A", targetTab("A"))
                .tab("Target B", targetTab("B"))
                .sizing(Sizing.fill(), Sizing.fill());
    }

    private BaseComponent<?> sourceTab() {
        var list = Containers.scrollable(Sizing.fill(), Sizing.fill())
                .padding(Insets.of(8))
                .gap(6)
                .child(WidgetComponents.text("Hold and drag any item. While dragging, hover a target tab header to switch tabs.")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()));

        for (DragItem item : items) {
            list.child(new DragItemComponent(item).sizing(Sizing.fill(), Sizing.fixed(28)));
        }
        return list;
    }

    private BaseComponent<?> targetTab(String group) {
        return Containers.vertical(Sizing.fill(), Sizing.fill())
                .padding(Insets.of(8))
                .gap(8)
                .child(WidgetComponents.text("Drop targets in tab " + group + ". This tab may become active while a drag is already in progress.")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(Containers.horizontal(Sizing.fill(), Sizing.fill()).gap(6)
                        .child(new DropSlotComponent(group + "-1").sizing(Sizing.fill(), Sizing.fill()))
                        .child(new DropSlotComponent(group + "-2").sizing(Sizing.fill(), Sizing.fill()))
                        .child(new DropSlotComponent(group + "-3").sizing(Sizing.fill(), Sizing.fill())));
    }

    private record DragItem(String name, int color) {}

    private static final class DragItemComponent extends BaseComponent<DragItemComponent> {
        private final DragItem item;
        private final DragSource<DragItem> dragSource;

        private DragItemComponent(DragItem item) {
            this.item = item;
            this.dragSource = DragDrop.source(this, DragItem.class)
                    .payload(() -> item)
                    .label(value -> Component.literal(value.name()))
                    .preview(this::extractPreview)
                    .startDistance(3);
        }

        @Override
        protected boolean isFocusable() {
            return true;
        }

        @Override
        public int determineVerticalContentSize(int availableWidth) {
            return 28;
        }

        @Override
        public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            UITheme theme = UITheme.current();
            boolean hot = isMouseOver(mouseX, mouseY);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, hot ? theme.panel().bgHover() : theme.panel().bg());
            graphics.outline(x, y, width, height, hot ? theme.panel().borderHover() : theme.panel().border());
            graphics.fill(x + 6, y + 6, x + 18, y + height - 6, item.color());
            graphics.text(font, Component.literal(item.name()), x + 26, y + (height - font.lineHeight) / 2, theme.text().primary(), true);
            graphics.text(font, Component.literal("drag"), x + width - 34, y + (height - font.lineHeight) / 2, theme.text().disabled(), false);
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
            return dragSource.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
            return dragSource.mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(@NonNull MouseButtonEvent event) {
            return dragSource.mouseReleased(event);
        }

        private void extractPreview(GuiGraphicsExtractor graphics, com.sypztep.plateau.client.v2.ui.interaction.DragPayload<DragItem> payload, int mouseX, int mouseY, float delta) {
            DragItem value = payload.value();
            int previewX = mouseX + 12;
            int previewY = mouseY + 12;
            int previewWidth = Math.max(84, font.width(value.name()) + 28);
            graphics.fill(previewX + 1, previewY + 1, previewX + previewWidth + 1, previewY + 21, 0x66000000);
            graphics.fill(previewX, previewY, previewX + previewWidth, previewY + 20, 0xEE15171E);
            graphics.outline(previewX, previewY, previewWidth, 20, value.color());
            graphics.fill(previewX + 5, previewY + 5, previewX + 15, previewY + 15, value.color());
            graphics.text(font, Component.literal(value.name()), previewX + 20, previewY + 6, 0xFFFFFFFF, true);
        }
    }

    private final class DropSlotComponent extends BaseComponent<DropSlotComponent> {
        private final String id;
        private final DropTarget<DragItem> dropTarget;

        private DropSlotComponent(String id) {
            this.id = id;
            this.dropTarget = DragDrop.target(this, DragItem.class)
                    .onDrop((item, event) -> drops.put(id, item));
        }

        @Override
        public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            UITheme theme = UITheme.current();
            DragItem item = drops.get(id);
            boolean hot = isMouseOver(mouseX, mouseY);
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, hot ? theme.panel().bgHover() : theme.panel().bg());
            graphics.outline(x, y, width, height, hot ? theme.panel().borderHover() : theme.panel().border());
            graphics.text(font, Component.literal("Slot " + id), x + 8, y + 8, theme.text().accent(), true);

            if (item == null) {
                graphics.centeredText(font, Component.literal("Drop here"), x + width / 2, y + height / 2 - font.lineHeight / 2, theme.text().disabled());
            } else {
                graphics.fill(x + 12, y + 28, x + 28, y + 44, item.color());
                graphics.text(font, Component.literal(item.name()), x + 36, y + 32, theme.text().primary(), true);
            }

            dropTarget.extractHint(graphics, mouseX, mouseY, delta);
        }
    }
}
