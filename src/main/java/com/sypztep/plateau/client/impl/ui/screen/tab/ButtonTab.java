package com.sypztep.plateau.client.impl.ui.screen.tab;

import com.sypztep.plateau.client.impl.ui.debug.DebugOverlay;
import com.sypztep.plateau.client.impl.ui.layout.ColumnLayout;
import com.sypztep.plateau.client.impl.ui.layout.Layout;
import com.sypztep.plateau.client.impl.ui.layout.RowLayout;
import com.sypztep.plateau.client.impl.ui.screen.Tab;
import com.sypztep.plateau.client.impl.ui.screen.TabContext;
import com.sypztep.plateau.client.impl.ui.theme.UITheme;
import com.sypztep.plateau.client.impl.ui.widget.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public class ButtonTab extends Tab {
    private float xp = 0.35f;
    private UILabel statusLabel;

    public ButtonTab() {
        super("buttons", Component.literal("Buttons"));
    }

    @Override
    protected void buildWidgets(TabContext ctx) {
        int startY = ctx.contentStartY();

        int marginX = Math.max(10, Layout.percent(ctx.screenWidth(), 0.05f));
        int availW = ctx.screenWidth() - marginX * 2;

        // Header
        addWidget(new UILabel(0, startY, ctx.screenWidth(), Component.literal("\u00a7eButton Examples"))
                .setDebugLabel("SectionHeader"));

        // Panel sizing
        int panelW = Layout.clampWidth(availW, 160, 400);
        int panelX = ctx.centerX(panelW);
        int panelY = startY + 18;
        int btnPad = 10;
        int btnW = panelW - btnPad * 2;
        int btnH = 20;

        // Buttons
        UIButton fancyBtn = (UIButton) new UIButton(0, 0, btnW, btnH, Component.literal("Click Me!"), button -> {
            xp = Math.min(1f, xp + 0.1f);
            updateStatus();
        })
                .setGlowIntensity(1.5f)
                .setBounceIntensity(1.2f)
                .setRoundedCorners(true, 4)
                .setDebugLabel("FancyButton");

        UIButton disabledBtn = (UIButton) new UIButton(0, 0, btnW, btnH, Component.literal("Disabled"), null)
                .setEnabled(false)
                .setDebugLabel("DisabledButton");

        UIButton resetBtn = (UIButton) new UIButton(0, 0, btnW, btnH, Component.literal("Reset"), button -> {
            xp = 0f;
            updateStatus();
        })
                .setShadowIntensity(0f)
                .setGlowIntensity(0.5f)
                .setDebugLabel("ResetButton");

        int halfBtnW = (btnW - 5) / 2;
        UIButton debugBtn = (UIButton) new UIButton(0, 0, halfBtnW, btnH, Component.literal("Toggle Debug"),
                button -> DebugOverlay.toggle())
                .setRoundedCorners(false, 0)
                .setDebugLabel("DebugToggle");

        UIButton silentBtn = (UIButton) new UIButton(0, 0, halfBtnW, btnH, Component.literal("No Sound"), button -> {})
                .setPlaySounds(false, false)
                .setDebugLabel("SilentButton");

        ColumnLayout col = new ColumnLayout(panelX + btnPad, panelY + 35, btnW)
                .gap(6).align(ColumnLayout.Align.CENTER)
                .add(fancyBtn).add(disabledBtn).add(resetBtn);
        int colH = col.apply();

        RowLayout row = new RowLayout(panelX + btnPad, panelY + 35 + colH + 6, btnH)
                .gap(5).add(debugBtn).add(silentBtn);
        row.apply();

        int panelH = 35 + colH + 6 + btnH + 10;

        UIPanel panel = (UIPanel) new UIPanel(panelX, panelY, panelW, panelH, Component.literal("Controls"))
                .setDebugLabel("ControlsPanel");
        panel.setFocusable(false);
        addRenderable(panel);

        addWidget(fancyBtn);
        addWidget(disabledBtn);
        addWidget(resetBtn);
        addWidget(debugBtn);
        addWidget(silentBtn);

        // Status
        int statusY = panelY + panelH + 8;
        if (statusY + 10 > ctx.screenHeight() - 20) statusY = ctx.screenHeight() - 20;
        statusLabel = (UILabel) new UILabel(0, statusY, ctx.screenWidth(), Component.literal("\u00a77Press buttons above"))
                .setColor(UITheme.current().textSecondary()).setDebugLabel("StatusText");
        addWidget(statusLabel);
        updateStatus();

        // Rich text
        int textY = statusY + 18;
        int textX = panelX;

        if (textY + 60 < ctx.screenHeight() - 20) {
            addWidget(new UIText(textX, textY, panelW, Component.literal("Hover over ")
                    .append(Component.literal("this text")
                            .withStyle(s -> s
                                    .withHoverEvent(new HoverEvent.ShowText(
                                            Component.literal("\u00a7eTooltip works!\n\u00a77Multiline too.")))
                                    .withColor(ChatFormatting.YELLOW).withUnderlined(true)))
                    .append(Component.literal(" to see a tooltip.")))
                    .setDebugLabel("TooltipText"));

            textY += 20;
            addWidget(new UIText(textX, textY, panelW, Component.literal("Open ")
                    .append(Component.literal("[Modrinth]")
                            .withStyle(s -> s
                                    .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create("https://modrinth.com")))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("\u00a77Click to open \u00a7amodrinth.com")))
                                    .withColor(ChatFormatting.AQUA).withUnderlined(true)))
                    .append(Component.literal(" or "))
                    .append(Component.literal("[Copy my name]")
                            .withStyle(s -> s
                                    .withClickEvent(new ClickEvent.CopyToClipboard("MrSypz"))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("\u00a77Copy \u00a7eMrSypz")))
                                    .withColor(ChatFormatting.GREEN))))
                    .setDebugLabel("LinkText"));
        }
    }

    private void updateStatus() {
        if (statusLabel != null) {
            statusLabel.setText(xp <= 0f
                    ? Component.literal("\u00a7cXP reset!")
                    : Component.literal("\u00a7aXP: " + (int)(xp * 100) + "%"));
        }
    }
}
