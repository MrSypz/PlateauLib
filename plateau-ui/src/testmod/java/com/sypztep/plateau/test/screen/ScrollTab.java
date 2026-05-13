package com.sypztep.plateau.test.screen;

import com.sypztep.plateau.client.v1.ui.screen.Tab;
import com.sypztep.plateau.client.v1.ui.screen.TabContext;
import com.sypztep.plateau.client.v1.ui.widget.UIScrollPanel;
import com.sypztep.plateau.test.UITestClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ScrollTab extends Tab {

    private static final List<String> ENTRIES = new ArrayList<>();

    static {
        for (int i = 1; i <= 40; i++) {
            ENTRIES.add("Entry #" + i + " — scrollable list item");
        }
    }

    public ScrollTab() {
        super(UITestClient.id("scroll"),
                Component.literal("Scroll"));
    }

    @Override
    protected void buildWidgets(TabContext ctx) {
        int pw = ctx.defaultPanelWidth();
        int px = ctx.defaultPanelX();

        addWidget(new UIScrollPanel(px, ctx.contentStartY() + 8, pw,
                ctx.availableHeight() - 8, Component.literal("Scroll Test")) {
            @Override
            protected void renderScrollContent(GuiGraphicsExtractor graphics,
                    int mouseX, int mouseY, float delta,
                    int contentX, int contentY, int contentWidth) {
                int lineH = font.lineHeight + 6;
                for (int i = 0; i < ENTRIES.size(); i++) {
                    int itemY = contentY + i * lineH - getScrollOffset();
                    // alternating row tint
                    if (i % 2 == 0) {
                        graphics.fill(contentX - 2, itemY - 1,
                                contentX + contentWidth + 2, itemY + font.lineHeight + 2,
                                0x11FFFFFF);
                    }
                    graphics.text(font, Component.literal(ENTRIES.get(i)), contentX, itemY, 0xFFCCCCCC, true);
                }
                setTotalContentHeight(ENTRIES.size() * lineH);
            }
        });
    }
}
