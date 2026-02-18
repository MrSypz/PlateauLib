package com.sypztep.plateau.client.impl.ui.screen.tab;

import com.sypztep.plateau.client.impl.ui.layout.Layout;
import com.sypztep.plateau.client.impl.ui.screen.Tab;
import com.sypztep.plateau.client.impl.ui.theme.UITheme;
import com.sypztep.plateau.client.impl.ui.widget.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public class InfoTab extends Tab {
    private int visitCount = 0;

    public InfoTab() {
        super("info", Component.literal("Info"));
    }

    @Override
    public void onActivate() {
        visitCount++;
        super.onActivate();
    }

    @Override
    protected void buildWidgets() {
        int sw = parentScreen.width;
        int sh = parentScreen.height;
        int startY = parentScreen.getContentStartY();

        int panelW = Layout.clampWidth(sw - 40, 200, 450);
        int panelX = Layout.centerX(sw, panelW);

        addWidget(new UILabel(0, startY, sw, Component.literal("§6About PlateauLib"))
                .setDebugLabel("InfoTitle"));

        int panelY = startY + 18;
        int panelH = Math.min(sh - panelY - 20, 200);

        UIPanel panel = (UIPanel) new UIPanel(panelX, panelY, panelW, panelH, Component.literal("Information"))
                .setDebugLabel("InfoPanel");
        panel.setFocusable(false);
        addRenderable(panel);

        int cx = panelX + 15;
        int cw = panelW - 30;
        int cy = panelY + 38;

        addWidget(new UIText(cx, cy, cw,
                Component.literal("§7Tab opened §e" + visitCount + "§7 time" + (visitCount != 1 ? "s" : "")))
                .setDebugLabel("VisitCounter"));
        cy += 16;

        addWidget(new UIText(cx, cy, cw,
                Component.literal("PlateauLib is a ")
                        .append(Component.literal("reusable UI framework")
                                .withStyle(s -> s.withColor(ChatFormatting.GOLD)
                                        .withHoverEvent(new HoverEvent.ShowText(
                                                Component.literal("§eBuilt for Fabric 1.21.11")))))
                        .append(Component.literal(" for Minecraft mods.")))
                .setLineSpacing(3).setDebugLabel("DescText"));
        cy += 28;

        if (cy + 14 < panelY + panelH - 5) {
            addWidget(new UILabel(cx, cy, Component.literal("§7§nLinks")).setDebugLabel("LinksHeader"));
            cy += 14;
        }

        if (cy + 14 < panelY + panelH - 5) {
            addWidget(new UIText(cx, cy, cw, Component.literal("§7• ")
                    .append(Component.literal("GitHub")
                            .withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true)
                                    .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create("https://github.com/MrSypz/PlateauLib")))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7Open GitHub"))))))
                    .setDebugLabel("GithubLink"));
            cy += 14;
        }

        if (cy + 14 < panelY + panelH - 5) {
            addWidget(new UIText(cx, cy, cw, Component.literal("§7• ")
                    .append(Component.literal("Copy Mod ID")
                            .withStyle(s -> s.withColor(ChatFormatting.YELLOW)
                                    .withClickEvent(new ClickEvent.CopyToClipboard("plateau"))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7Copy §eplateau"))))))
                    .setDebugLabel("CopyIdLink"));
        }

        int noteY = panelY + panelH + 6;
        if (noteY + 10 < sh - 10) {
            addWidget(new UILabel(0, noteY, sw, Component.literal("§8Visit count persists across tab switches"))
                    .setColor(UITheme.TEXT_DISABLED).setDebugLabel("BottomNote"));
        }
    }
}