package com.sypztep.plateau.client.impl.ui.screen.tab;

import com.sypztep.plateau.client.impl.ui.behavior.ScrollBehavior;
import com.sypztep.plateau.client.impl.ui.core.RenderHelper;
import com.sypztep.plateau.client.impl.ui.core.UIColors;
import com.sypztep.plateau.client.impl.ui.core.UIComponent;
import com.sypztep.plateau.client.impl.ui.core.UISounds;
import com.sypztep.plateau.client.impl.ui.layout.Layout;
import com.sypztep.plateau.client.impl.ui.layout.RowLayout;
import com.sypztep.plateau.client.impl.ui.screen.Tab;
import com.sypztep.plateau.client.impl.ui.screen.TabContext;
import com.sypztep.plateau.client.impl.ui.theme.UITheme;
import com.sypztep.plateau.client.impl.ui.widget.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PartyTab extends Tab {

    private final List<PartyMember> members = new ArrayList<>();
    private @Nullable String selectedMemberId = null;
    private @Nullable String statusMessage = null;
    private int statusColor = UITheme.current().textSecondary();

    public PartyTab() {
        super("party", Component.literal("Party"));
        initDemoMembers();
    }

    private void initDemoMembers() {
        members.add(new PartyMember("player_1", "MrSypz", Role.LEADER, MemberClass.WARRIOR, 95, 100, Status.ONLINE));
        members.add(new PartyMember("player_2", "Alex", Role.MEMBER, MemberClass.MAGE, 72, 80, Status.ONLINE));
        members.add(new PartyMember("player_3", "Steve", Role.MEMBER, MemberClass.RANGER, 45, 90, Status.COMBAT));
        members.add(new PartyMember("player_4", "Luna", Role.MEMBER, MemberClass.HEALER, 88, 88, Status.ONLINE));
        members.add(new PartyMember("player_5", "Kai", Role.MEMBER, MemberClass.ROGUE, 30, 70, Status.LOW_HP));
        members.add(new PartyMember("player_6", "Nova", Role.MEMBER, MemberClass.WARRIOR, 0, 85, Status.DEAD));
        members.add(new PartyMember("player_7", "Finn", Role.MEMBER, MemberClass.MAGE, 60, 60, Status.ONLINE));
        members.add(new PartyMember("player_8", "Aria", Role.MEMBER, MemberClass.HEALER, 50, 75, Status.AWAY));
    }

    @Override
    protected void buildWidgets(TabContext ctx) {
        int startY = ctx.contentStartY();

        // Two-panel layout
        int totalW = Layout.clampWidth(ctx.screenWidth() - 30, 250, 520);
        int leftStart = ctx.centerX(totalW);
        boolean wideMode = totalW > 360;

        int listW, detailW, detailX;
        if (wideMode) {
            listW = (int)(totalW * 0.55f);
            detailW = totalW - listW - 6;
            detailX = leftStart + listW + 6;
        } else {
            listW = totalW;
            detailW = 0;
            detailX = 0;
        }

        // Header
        addWidget(new UILabel(0, startY, ctx.screenWidth(),
                Component.literal("\u00a76Party \u00a77(" + members.size() + "/" + 8 + ")"))
                .setDebugLabel("PartyHeader"));

        int contentY = startY + 16;
        int availH = ctx.screenHeight() - contentY - 40;

        // Member list (scrollable)
        int listH = wideMode ? availH : Math.min(availH - 80, 180);

        MemberListPanel memberList = new MemberListPanel(leftStart, contentY, listW, listH);
        addWidget(memberList);

        // Detail panel
        int detailY = wideMode ? contentY : contentY + listH + 6;
        int detailH = wideMode ? availH : Math.max(40, availH - listH - 6);

        if (wideMode) {
            DetailPanel detail = new DetailPanel(detailX, detailY, detailW, detailH);
            addWidget(detail);
        } else if (detailY + 50 < ctx.screenHeight() - 30) {
            DetailPanel detail = new DetailPanel(leftStart, detailY, totalW, detailH);
            addWidget(detail);
        }

        // Bottom action bar
        int barY = ctx.screenHeight() - 30;
        int btnW = Layout.clampWidth(totalW / 3 - 4, 50, 100);
        int btnH = 18;

        UIButton inviteBtn = (UIButton) new UIButton(0, 0, btnW, btnH,
                Component.literal("+ Invite"), button -> setStatus("\u00a7aInvite sent!", 0xFF55FF55))
                .setGlowIntensity(0.8f).setDebugLabel("InviteBtn");

        UIButton leaveBtn = (UIButton) new UIButton(0, 0, btnW, btnH,
                Component.literal("Leave"), button -> setStatus("\u00a7cLeft the party!", 0xFFFF5555))
                .setGlowIntensity(0.5f).setShadowIntensity(0f).setDebugLabel("LeaveBtn");

        UIButton healBtn = (UIButton) new UIButton(0, 0, btnW, btnH,
                Component.literal("Heal All"), button -> {
            for (PartyMember m : members) {
                m.hp = m.maxHp;
                m.status = Status.ONLINE;
            }
            setStatus("\u00a7aAll members healed!", 0xFF55FF55);
        }).setDebugLabel("HealBtn");

        RowLayout actionRow = new RowLayout(0, barY, btnH).gap(4)
                .add(inviteBtn).add(leaveBtn).add(healBtn);
        int rowW = actionRow.apply();
        int rowOffset = ctx.centerX(rowW);
        for (var c : actionRow.getComponents()) {
            c.setPosition(c.getX() + rowOffset, c.getY());
        }

        addWidget(inviteBtn);
        addWidget(leaveBtn);
        addWidget(healBtn);

        // Status text
        if (barY - 14 > contentY + listH) {
            UILabel statusLbl = new UILabel(0, barY - 14, ctx.screenWidth(),
                    Component.literal(statusMessage != null ? statusMessage : "\u00a77Select a party member"));
            statusLbl.setCentered(true).setColor(statusColor).setDebugLabel("StatusText");
            addWidget(statusLbl);
        }
    }

    private void setStatus(String msg, int color) {
        this.statusMessage = msg;
        this.statusColor = color;
        rebuild();
    }

    private void selectMember(@Nullable String id) {
        selectedMemberId = id;
        rebuild();
    }

    private void rebuild() {
        onDeactivate();
        onActivate();
    }

    // ═══════════════════════════════════════════
    // Member list panel — scrollable
    // ═══════════════════════════════════════════

    private class MemberListPanel extends UIComponent {
        private final ScrollBehavior scroll = new ScrollBehavior();
        private static final int CARD_H = 44;
        private static final int CARD_GAP = 3;

        private final float[] cardHover;

        MemberListPanel(int x, int y, int width, int height) {
            super(x, y, width, height);
            this.cardHover = new float[members.size()];
            setDebugLabel("MemberList");
        }

        @Override
        protected void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            UITheme theme = UITheme.current();

            // Panel background + border
            RenderHelper.drawPanel(graphics, x, y, width, height, theme.panelBg(), theme.panelBorder());

            // Scroll setup
            int totalContent = members.size() * (CARD_H + CARD_GAP);
            scroll.setBounds(x + 1, y + 1, width - 2, height - 2);
            scroll.setContentHeight(totalContent);
            scroll.update(delta);

            // Scissor + render cards
            scroll.enableScissor(graphics);

            int cardX = x + 4;
            int cardW = scroll.getContentWidth() - 8;
            int cy = y + 4 - scroll.getScrollOffset();

            for (int i = 0; i < members.size(); i++) {
                PartyMember member = members.get(i);
                boolean selected = member.id.equals(selectedMemberId);
                boolean hovered = mouseX >= cardX && mouseX < cardX + cardW
                        && mouseY >= cy && mouseY < cy + CARD_H
                        && mouseY >= y && mouseY < y + height;

                cardHover[i] = stepAnimation(cardHover[i], hovered || selected, 0.06f);

                renderMemberCard(graphics, member, cardX, cy, cardW, CARD_H,
                        selected, cardHover[i], theme);

                cy += CARD_H + CARD_GAP;
            }

            scroll.disableScissor(graphics);
            scroll.renderScrollbar(graphics, mouseX, mouseY);
        }

        private void renderMemberCard(GuiGraphics graphics, PartyMember member,
                                      int cx, int cy, int cw, int ch,
                                      boolean selected, float hover, UITheme theme) {
            // Card background
            int baseBg = selected ? 0xFF2A2A3A : theme.panelBg();
            int hoverBg = selected ? 0xFF3A3A4A : UIColors.lighten(theme.panelBg(), 0.05f);
            int bg = UIColors.interpolate(baseBg, hoverBg, hover);
            graphics.fill(cx, cy, cx + cw, cy + ch, bg);

            // Left accent bar
            int accentColor = member.role == Role.LEADER ? 0xFFFFCC00 : member.memberClass.color;
            graphics.fill(cx, cy, cx + 3, cy + ch, accentColor);

            // Status dot
            int dotX = cx + 8;
            int dotY = cy + 6;
            graphics.fill(dotX, dotY, dotX + 5, dotY + 5, member.status.color);

            // Name
            int nameColor = member.status == Status.DEAD ? theme.textDisabled()
                    : UIColors.interpolate(theme.textPrimary(), 0xFFFFFFFF, hover);
            String nameStr = (member.role == Role.LEADER ? "\u00a76\u265a \u00a7f" : "") + member.name;
            graphics.drawString(font, nameStr, cx + 16, cy + 5, nameColor, true);

            // Class tag
            int classColor = UIColors.interpolate(member.memberClass.color,
                    UIColors.lighten(member.memberClass.color, 0.3f), hover);
            int classTagX = cx + cw - font.width(member.memberClass.tag) - 6;
            graphics.drawString(font, member.memberClass.tag, classTagX, cy + 5, classColor, true);

            // HP bar
            int barX = cx + 16;
            int barY = cy + 18;
            int barW = cw - 22;
            int barH = 6;

            float hpRatio = member.maxHp > 0 ? (float) member.hp / member.maxHp : 0;
            int hpColor = hpRatio > 0.5f ? 0xFF55FF55
                    : hpRatio > 0.25f ? 0xFFFFAA00 : 0xFFFF5555;
            if (member.status == Status.DEAD) hpColor = 0xFF555555;

            RenderHelper.drawProgressBar(graphics, barX, barY, barW, barH, hpRatio, hpColor, 0xFF333333);

            // HP text
            String hpStr = member.hp + "/" + member.maxHp;
            int hpTextColor = member.status == Status.DEAD ? theme.textDisabled() : 0xFFCCCCCC;
            graphics.drawString(font, hpStr, barX, barY + barH + 3, hpTextColor, true);

            // Status text
            graphics.drawString(font, member.status.label, cx + cw - font.width(member.status.label) - 6,
                    barY + barH + 3, member.status.color, true);

            // Selection border
            if (selected) {
                RenderHelper.drawBorder(graphics, cx, cy, cw, ch, 0xFF5577DD);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
            if (scroll.mouseClicked(event, false)) return true;
            if (event.button() != 0) return false;
            if (!isMouseOver(event.x(), event.y())) return false;

            int cardX = x + 4;
            int cardW = scroll.getContentWidth() - 8;
            int cy = y + 4 - scroll.getScrollOffset();

            for (PartyMember member : members) {
                if (event.x() >= cardX && event.x() < cardX + cardW
                        && event.y() >= cy && event.y() < cy + CARD_H
                        && event.y() >= y && event.y() < y + height) {

                    UISounds.playClick();

                    if (member.id.equals(selectedMemberId)) {
                        selectMember(null);
                    } else {
                        selectMember(member.id);
                    }
                    return true;
                }
                cy += CARD_H + CARD_GAP;
            }

            return false;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double h, double v) {
            return scroll.mouseScrolled(mx, my, v);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            return scroll.mouseDragged(event);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            return scroll.mouseReleased(event);
        }
    }

    // ═══════════════════════════════════════════
    // Detail panel
    // ═══════════════════════════════════════════

    private class DetailPanel extends UIComponent {
        DetailPanel(int x, int y, int width, int height) {
            super(x, y, width, height);
            setDebugLabel("DetailPanel");
            this.focusable = false;
        }

        @Override
        protected void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            updateHover(mouseX, mouseY);

            UITheme theme = UITheme.current();
            RenderHelper.drawPanelWithHover(graphics, x, y, width, height, hoverProgress, true);

            // Header
            graphics.drawCenteredString(font, "Details", x + width / 2, y + 6, theme.textAccent());

            // Find selected member
            PartyMember selected = null;
            if (selectedMemberId != null) {
                for (PartyMember m : members) {
                    if (m.id.equals(selectedMemberId)) { selected = m; break; }
                }
            }

            if (selected == null) {
                graphics.drawCenteredString(font, "\u00a77No member selected",
                        x + width / 2, y + height / 2 - font.lineHeight / 2, theme.textDisabled());
                return;
            }

            int cx = x + 8;
            int cy = y + 22;
            int cw = width - 16;

            // Name + role
            String roleStr = selected.role == Role.LEADER ? "\u00a76[Leader]" : "\u00a77[Member]";
            graphics.drawString(font, selected.name + " " + roleStr, cx, cy, theme.textPrimary(), true);
            cy += 14;

            // Class
            graphics.drawString(font, "Class: " + selected.memberClass.tag,
                    cx, cy, selected.memberClass.color, true);
            cy += 14;

            // HP bar (larger)
            graphics.drawString(font, "\u00a77HP:", cx, cy, theme.textSecondary(), true);
            int barX = cx + 20;
            int barW = cw - 24;
            int barH = 10;

            float hpRatio = selected.maxHp > 0 ? (float) selected.hp / selected.maxHp : 0;
            int hpColor = hpRatio > 0.5f ? 0xFF55FF55
                    : hpRatio > 0.25f ? 0xFFFFAA00 : 0xFFFF5555;
            if (selected.status == Status.DEAD) hpColor = 0xFF555555;

            RenderHelper.drawProgressBar(graphics, barX, cy, barW, barH, hpRatio, hpColor, 0xFF333333);

            // HP border
            RenderHelper.drawBorder(graphics, barX, cy, barW, barH, 0xFF555555);

            String hpText = selected.hp + " / " + selected.maxHp;
            graphics.drawCenteredString(font, hpText, barX + barW / 2, cy + (barH - font.lineHeight) / 2, 0xFFFFFFFF);
            cy += barH + 8;

            // Status
            graphics.drawString(font, "Status: ", cx, cy, theme.textSecondary(), true);
            graphics.drawString(font, selected.status.label,
                    cx + font.width("Status: "), cy, selected.status.color, true);
            cy += 14;

            // Stats preview
            if (cy + 40 < y + height) {
                graphics.fill(cx, cy, cx + cw, cy + 1, 0xFF333333);
                cy += 6;

                drawMiniStat(graphics, "ATK", selected.memberClass.atk, 20, cx, cy, cw, theme);
                cy += 12;
                drawMiniStat(graphics, "DEF", selected.memberClass.def, 20, cx, cy, cw, theme);
                cy += 12;
                drawMiniStat(graphics, "SPD", selected.memberClass.spd, 20, cx, cy, cw, theme);
            }

            // Action buttons at bottom
            int btnY = y + height - 24;
            if (btnY > cy + 10) {
                graphics.fill(cx, btnY - 6, cx + cw, btnY - 5, 0xFF333333);

                int actionX = cx;
                if (selected.status == Status.DEAD) {
                    drawTextAction(graphics, "\u00a7a[Revive]", actionX, btnY, mouseX, mouseY);
                } else {
                    drawTextAction(graphics, "\u00a7e[Heal]", actionX, btnY, mouseX, mouseY);
                }

                actionX += font.width("[Revive] ") + 8;
                if (selected.role != Role.LEADER) {
                    drawTextAction(graphics, "\u00a7c[Kick]", actionX, btnY, mouseX, mouseY);
                    actionX += font.width("[Kick] ") + 8;
                    drawTextAction(graphics, "\u00a7d[Promote]", actionX, btnY, mouseX, mouseY);
                }
            }
        }

        private void drawMiniStat(GuiGraphics graphics, String label, int value, int max,
                                  int x, int y, int width, UITheme theme) {
            graphics.drawString(font, "\u00a77" + label, x, y, theme.textSecondary(), true);
            int barX = x + 28;
            int barW = width - 32;
            int barH = 4;
            int barY = y + (font.lineHeight - barH) / 2;

            RenderHelper.drawProgressBar(graphics, barX, barY, barW, barH,
                    (float) value / max, 0xFF6688CC, 0xFF222222);

            graphics.drawString(font, "\u00a7f" + value, barX + barW + 4, y, theme.textPrimary(), true);
        }

        private void drawTextAction(GuiGraphics graphics, String text, int x, int y,
                                    int mouseX, int mouseY) {
            int w = font.width(text);
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + font.lineHeight;
            int color = hovered ? 0xFFFFFFFF : 0xFFAAAAAA;
            graphics.drawString(font, text, x, y, color, true);
            if (hovered) {
                graphics.fill(x, y + font.lineHeight, x + w, y + font.lineHeight + 1, color);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
            if (event.button() != 0 || selectedMemberId == null) return false;
            if (!isMouseOver(event.x(), event.y())) return false;

            PartyMember selected = null;
            for (PartyMember m : members) {
                if (m.id.equals(selectedMemberId)) { selected = m; break; }
            }
            if (selected == null) return false;

            int cx = x + 8;
            int btnY = y + height - 24;
            int actionX = cx;
            double mx = event.x();
            double my = event.y();

            String actionText = selected.status == Status.DEAD ? "[Revive]" : "[Heal]";
            int actionW = font.width(actionText);
            if (mx >= actionX && mx < actionX + actionW && my >= btnY && my < btnY + font.lineHeight) {
                selected.hp = selected.maxHp;
                selected.status = Status.ONLINE;
                setStatus("\u00a7a" + selected.name + " healed!", 0xFF55FF55);
                return true;
            }

            actionX += font.width("[Revive] ") + 8;

            if (selected.role != Role.LEADER) {
                int kickW = font.width("[Kick]");
                if (mx >= actionX && mx < actionX + kickW && my >= btnY && my < btnY + font.lineHeight) {
                    members.remove(selected);
                    selectedMemberId = null;
                    setStatus("\u00a7c" + selected.name + " was kicked!", 0xFFFF5555);
                    return true;
                }

                actionX += font.width("[Kick] ") + 8;

                int promoteW = font.width("[Promote]");
                if (mx >= actionX && mx < actionX + promoteW && my >= btnY && my < btnY + font.lineHeight) {
                    for (PartyMember m : members) {
                        if (m.role == Role.LEADER) m.role = Role.MEMBER;
                    }
                    selected.role = Role.LEADER;
                    setStatus("\u00a7d" + selected.name + " promoted to leader!", 0xFFDD66DD);
                    return true;
                }
            }

            return false;
        }
    }

    // ═══════════════════════════════════════════
    // Data models
    // ═══════════════════════════════════════════

    private static class PartyMember {
        final String id;
        String name;
        Role role;
        MemberClass memberClass;
        int hp, maxHp;
        Status status;

        PartyMember(String id, String name, Role role, MemberClass memberClass,
                    int hp, int maxHp, Status status) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.memberClass = memberClass;
            this.hp = hp;
            this.maxHp = maxHp;
            this.status = status;
        }
    }

    private enum Role {
        LEADER, MEMBER
    }

    private enum MemberClass {
        WARRIOR("Warrior", 0xFFFF6644, 15, 14, 8),
        MAGE("Mage", 0xFF6688FF, 18, 6, 10),
        RANGER("Ranger", 0xFF66DD66, 13, 9, 16),
        HEALER("Healer", 0xFFFFCC44, 7, 10, 12),
        ROGUE("Rogue", 0xFFDD66FF, 14, 7, 18);

        final String tag;
        final int color;
        final int atk, def, spd;

        MemberClass(String tag, int color, int atk, int def, int spd) {
            this.tag = tag;
            this.color = color;
            this.atk = atk;
            this.def = def;
            this.spd = spd;
        }
    }

    private enum Status {
        ONLINE("Online", 0xFF55FF55),
        COMBAT("In Combat", 0xFFFF8844),
        LOW_HP("Low HP", 0xFFFFAA00),
        DEAD("Dead", 0xFFFF3333),
        AWAY("Away", 0xFF888888);

        final String label;
        final int color;

        Status(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }
}
