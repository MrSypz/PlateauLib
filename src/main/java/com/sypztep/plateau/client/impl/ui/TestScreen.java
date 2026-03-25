package com.sypztep.plateau.client.impl.ui;

import com.sypztep.plateau.client.impl.ui.core.RenderHelper;
import com.sypztep.plateau.client.impl.ui.layout.Layout;
import com.sypztep.plateau.client.impl.ui.screen.PlateauScreen;
import com.sypztep.plateau.client.impl.ui.screen.Tab;
import com.sypztep.plateau.client.impl.ui.screen.TabContext;
import com.sypztep.plateau.client.impl.ui.screen.TabManager;
import com.sypztep.plateau.client.impl.ui.theme.UITheme;
import com.sypztep.plateau.client.impl.ui.widget.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TestScreen extends PlateauScreen {
    public TestScreen() {
        super(Component.literal("UI Test"));
        tabManager = new TabManager(this);
        tabManager.registerTab(new WidgetTab());
        tabManager.registerTab(new LayoutTab());
        tabManager.registerTab(new ScrollTab());
    }

    @Override
    protected void initComponents() {}

    // ═══════════════════════════════════════════
    // Tab 1: Widgets showcase
    // ═══════════════════════════════════════════
    static class WidgetTab extends Tab {
        WidgetTab() { super("widgets", Component.literal("Widgets")); }

        @Override
        protected void buildWidgets(TabContext ctx) {
            int panelW = Layout.clampWidth(ctx.screenWidth() - 40, 200, 400);
            int panelX = ctx.centerX(panelW);
            int y = ctx.contentStartY();

            // Panel
            UIPanel panel = new UIPanel(panelX, y, panelW, 140, Component.literal("Widget Demo"));
            panel.setFocusable(false);
            addRenderable(panel);

            int btnW = panelW - 30;
            int btnX = panelX + 15;

            // Buttons — Tab key cycles focus, Enter/Space activates
            UIButton btn1 = new UIButton(btnX, y + 40, btnW, 20, Component.literal("Focusable Button 1"), b -> {});
            btn1.setGlowIntensity(1.2f).setRoundedCorners(true, 4);
            addWidget(btn1);

            UIButton btn2 = new UIButton(btnX, y + 65, btnW, 20, Component.literal("Focusable Button 2"), b -> {});
            btn2.setBounceIntensity(1.5f);
            addWidget(btn2);

            UIButton btn3 = new UIButton(btnX, y + 90, btnW, 20, Component.literal("Disabled"), null);
            btn3.setEnabled(false);
            addWidget(btn3);

            // Label
            addWidget(new UILabel(panelX, y + 150, panelW,
                    Component.literal("\u00a77Tab = cycle focus \u00a7e| \u00a77Enter = activate")));
        }
    }

    // ═══════════════════════════════════════════
    // Tab 2: UIRow + UIColumn layout
    // ═══════════════════════════════════════════
    static class LayoutTab extends Tab {
        LayoutTab() { super("layout", Component.literal("Layout")); }

        @Override
        protected void buildWidgets(TabContext ctx) {
            int totalW = Layout.clampWidth(ctx.screenWidth() - 40, 300, 500);
            int startX = ctx.centerX(totalW);
            int y = ctx.contentStartY();
            int availH = ctx.availableHeight() - 30;

            // Row with two panels (40% / 60% split)
            UIRow row = new UIRow(startX, y, totalW, availH).gap(6);

            UIScrollPanel leftPanel = new UIScrollPanel(0, 0, 0, 0, Component.literal("Left (40%)")) {
                @Override
                protected void renderScrollContent(GuiGraphics g, int mx, int my, float d,
                                                   int cx, int cy, int cw) {
                    UITheme theme = UITheme.current();
                    for (int i = 0; i < 20; i++) {
                        int itemY = cy + i * 22 - getScrollOffset();
                        int color = (i % 2 == 0) ? theme.textPrimary() : theme.textSecondary();
                        g.drawString(font, "Item " + (i + 1), cx + 4, itemY + 4, color, false);
                    }
                    setTotalContentHeight(20 * 22);
                }
            };

            UIScrollPanel rightPanel = new UIScrollPanel(0, 0, 0, 0, Component.literal("Right (60%)")) {
                @Override
                protected void renderScrollContent(GuiGraphics g, int mx, int my, float d,
                                                   int cx, int cy, int cw) {
                    UITheme theme = UITheme.current();
                    g.drawString(font, "Arrow keys scroll when focused", cx + 4, cy + 4 - getScrollOffset(),
                            theme.textAccent(), false);
                    g.drawString(font, "Page Up/Down for fast scroll", cx + 4, cy + 20 - getScrollOffset(),
                            theme.textSecondary(), false);
                    g.drawString(font, "Home/End to jump", cx + 4, cy + 36 - getScrollOffset(),
                            theme.textSecondary(), false);

                    for (int i = 0; i < 30; i++) {
                        int itemY = cy + 60 + i * 18 - getScrollOffset();
                        float ratio = i / 29f;
                        RenderHelper.drawProgressBar(g, cx + 4, itemY, cw - 8, 12,
                                ratio, theme.progressFill(), theme.progressBg());
                    }
                    setTotalContentHeight(60 + 30 * 18);
                }
            };

            row.add(leftPanel, 0.4f);
            row.add(rightPanel, 0.6f);

            addWidget(row);
        }
    }

    // ═══════════════════════════════════════════
    // Tab 3: Scroll + keyboard nav
    // ═══════════════════════════════════════════
    static class ScrollTab extends Tab {
        ScrollTab() { super("scroll", Component.literal("Scroll")); }

        @Override
        protected void buildWidgets(TabContext ctx) {
            int panelW = Layout.clampWidth(ctx.screenWidth() - 40, 200, 350);
            int panelX = ctx.centerX(panelW);
            int y = ctx.contentStartY();
            int panelH = ctx.availableHeight() - 30;

            UIScrollPanel scrollPanel = new UIScrollPanel(panelX, y, panelW, panelH,
                    Component.literal("Keyboard Scrollable")) {
                @Override
                protected void renderScrollContent(GuiGraphics g, int mx, int my, float d,
                                                   int cx, int cy, int cw) {
                    UITheme theme = UITheme.current();
                    for (int i = 0; i < 50; i++) {
                        int itemY = cy + i * 20 - getScrollOffset();
                        boolean hovered = mx >= cx && mx < cx + cw
                                && my >= itemY && my < itemY + 18;

                        if (hovered) {
                            g.fill(cx, itemY, cx + cw, itemY + 18, theme.panelBgHover());
                        }

                        int color = hovered ? theme.textAccent() : theme.textPrimary();
                        g.drawString(font, "Row " + (i + 1) + " — focus me with Tab, scroll with arrows",
                                cx + 6, itemY + 4, color, false);
                    }
                    setTotalContentHeight(50 * 20);
                }
            };

            addWidget(scrollPanel);

            addWidget(new UILabel(panelX, y + panelH + 5, panelW,
                    Component.literal("\u00a78Focus panel with Tab, then use Arrow/Page/Home/End")));
        }
    }
}
