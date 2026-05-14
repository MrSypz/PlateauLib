package com.sypztep.plateau.test.screen;

import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.screen.TabContext;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.WidgetComponents;
import com.sypztep.plateau.client.v2.ui.Containers;
import com.sypztep.plateau.client.v2.ui.Overlays;
import com.sypztep.plateau.client.v2.ui.Panels;
import com.sypztep.plateau.client.v2.ui.container.ScrollContainer;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import com.sypztep.plateau.client.v2.ui.layout.FlowLayout;
import com.sypztep.plateau.client.v2.ui.screen.Tab2;
import com.sypztep.plateau.client.v2.ui.widget.StringComponent;
import com.sypztep.plateau.test.UITestClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class PlayerClassStatsTab extends Tab2 {

    private final List<AllocStat> allocStats = List.of(
            new AllocStat("STR", "Strength", 8),
            new AllocStat("AGI", "Agility", 7),
            new AllocStat("DEX", "Dexterity", 9),
            new AllocStat("INT", "Intelligence", 5),
            new AllocStat("LUK", "Luck", 3)
    );

    private int freePoints = 14;
    private ScrollContainer derivedStatsList;
    private StringComponent searchBox;

    public PlayerClassStatsTab() {
        super(UITestClient.id("player_stats"), Component.literal("Player Stats"));
    }

    @Override
    protected BaseComponent<?> build(TabContext ctx) {
        derivedStatsList = Panels.scrollBody();

        searchBox = WidgetComponents.string("Search stat")
                .onChanged(this::refreshDerivedStats)
                .sizing(Sizing.fill(), Sizing.fixed(20));

        refreshDerivedStats("");

        return Overlays.windowLayer()
                .content(Containers.horizontal(Sizing.fill(), Sizing.fill())
                        .gap(8)
                        .padding(Insets.of(8))
                        .child(leftColumn())
                        .child(derivedStatsPanel().sizing(Sizing.fill(3), Sizing.fill())));
    }

    private BaseComponent<?> leftColumn() {
        return Containers.vertical(Sizing.fill(2), Sizing.fill())
                .gap(8)
                .child(playerInfoPanel())
                .child(statAllocationPanel());
    }

    private BaseComponent<?> playerInfoPanel() {
        return Panels.fixed("Player Information")
                .sizing(Sizing.fill(), Sizing.content())
                .child(infoRow("Class", "Novice Arcanist"))
                .child(infoRow("Class Level", "27"))
                .child(infoRow("Job Level", "12"))
                .child(infoRow("Available Points", () -> String.valueOf(freePoints)))
                .child(infoRow("Next Job", "Scholar at Job Lv. 20"));
    }

    private BaseComponent<?> statAllocationPanel() {
        return Panels.detachable("Increase Stats")
                .sizing(Sizing.fill(), Sizing.fill())
                .windowSize(330, 220)
                .animationSpeed(0.45f)
                .content(statAllocationContent());
    }

    private BaseComponent<?> statAllocationContent() {
        ScrollContainer content = Panels.scrollBody();
        content.surface(Surface.NONE);
        content.child(WidgetComponents.text("Hover a value to see base + added. Points only increase in this mock.")
                .secondary()
                .sizing(Sizing.fill(), Sizing.content()));

        FlowLayout rows = Containers.vertical(Sizing.fill(), Sizing.content())
                .gap(6)
                .child(infoRow("Available Points", () -> String.valueOf(freePoints)));

        for (AllocStat stat : allocStats) {
            rows.child(new StatPointRow(stat, () -> freePoints, () -> {
                if (freePoints <= 0) return false;
                freePoints--;
                stat.addition++;
                refreshDerivedStats(searchBox.value());
                return true;
            }).sizing(Sizing.fill(), Sizing.fixed(24)));
        }

        content.child(rows);
        return content;
    }

    private BaseComponent<?> derivedStatsPanel() {
        return Panels.fixed("Derived Stats")
                .bodyPadding(Insets.of(6, 8))
                .gap(6)
                .sizing(Sizing.fill(), Sizing.fill())
                .child(searchBox)
                .child(derivedStatsList);
    }

    private BaseComponent<?> infoRow(String label, String value) {
        return infoRow(label, () -> value);
    }

    private BaseComponent<?> infoRow(String label, ValueProvider valueProvider) {
        return new DynamicTextRow(label, valueProvider)
                .sizing(Sizing.fill(), Sizing.fixed(16));
    }

    private void refreshDerivedStats(String query) {
        if (derivedStatsList == null) return;

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        derivedStatsList.getChildren().clear();

        for (DerivedStat stat : derivedStats()) {
            if (!normalizedQuery.isEmpty()
                    && !stat.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    && !stat.value().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                continue;
            }

            derivedStatsList.child(WidgetComponents.label(stat.name() + ": " + stat.value())
                    .sizing(Sizing.fill(), Sizing.fixed(14)));
        }

        if (derivedStatsList.getWidth() > 0 || derivedStatsList.getHeight() > 0) {
            derivedStatsList.mount(derivedStatsList.x(), derivedStatsList.y(), derivedStatsList.width(), derivedStatsList.height());
        }
    }

    private List<DerivedStat> derivedStats() {
        int strength = total("STR");
        int agility = total("AGI");
        int dexterity = total("DEX");
        int intelligence = total("INT");
        int luck = total("LUK");

        List<DerivedStat> stats = new ArrayList<>();
        stats.add(new DerivedStat("Health", String.valueOf(20 + strength * 3)));
        stats.add(new DerivedStat("Max Mana", String.valueOf(12 + intelligence * 4)));
        stats.add(new DerivedStat("Attack Damage", String.valueOf(1 + strength / 2)));
        stats.add(new DerivedStat("Magic Damage", String.valueOf(1 + intelligence / 2)));
        stats.add(new DerivedStat("Walk Speed", String.format(Locale.ROOT, "%.2f", 0.10 + agility * 0.002)));
        stats.add(new DerivedStat("Cast Speed", percent(5 + intelligence)));
        stats.add(new DerivedStat("Drop Chance", percent(2 + luck)));
        stats.add(new DerivedStat("Critical Chance", percent(3 + luck + dexterity / 2)));
        stats.add(new DerivedStat("Critical Damage", percent(50 + dexterity)));
        stats.add(new DerivedStat("Accuracy", String.valueOf(80 + dexterity * 2)));
        stats.add(new DerivedStat("Evasion", String.valueOf(5 + agility)));
        stats.add(new DerivedStat("Defense", String.valueOf(2 + strength / 3)));
        stats.add(new DerivedStat("Magic Defense", String.valueOf(2 + intelligence / 3)));
        stats.add(new DerivedStat("Luck", String.valueOf(luck)));
        stats.add(new DerivedStat("Mining Speed", percent(agility + strength / 2)));
        stats.add(new DerivedStat("Fishing Chance", percent(4 + luck)));
        stats.add(new DerivedStat("Potion Recovery", percent(10 + intelligence / 2)));
        stats.add(new DerivedStat("Stamina Regen", percent(6 + agility / 2)));
        stats.add(new DerivedStat("Cooldown Reduction", percent(intelligence / 2)));
        stats.add(new DerivedStat("Knockback Resist", percent(strength / 2)));
        stats.add(new DerivedStat("Crafting Quality", percent(luck + dexterity / 3)));
        stats.add(new DerivedStat("Gathering Yield", percent(2 + luck / 2)));
        stats.add(new DerivedStat("Pet Bond Gain", percent(3 + luck / 3)));
        stats.add(new DerivedStat("Experience Gain", percent(1 + intelligence / 4)));
        return stats;
    }

    private int total(String code) {
        for (AllocStat stat : allocStats) {
            if (stat.code.equals(code)) return stat.base + stat.addition;
        }
        return 0;
    }

    private static String percent(int value) {
        return value + "%";
    }

    private record DerivedStat(String name, String value) {}

    private interface ValueProvider {
        String get();
    }

    private interface StatIncreaseAction {
        boolean increase();
    }

    private static final class AllocStat {
        private final String code;
        private final String name;
        private final int base;
        private int addition;

        private AllocStat(String code, String name, int base) {
            this.code = code;
            this.name = name;
            this.base = base;
        }
    }

    private static final class DynamicTextRow extends BaseComponent<DynamicTextRow> {
        private final String label;
        private final ValueProvider valueProvider;

        private DynamicTextRow(String label, ValueProvider valueProvider) {
            this.label = label;
            this.valueProvider = valueProvider;
        }

        @Override
        public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            UITheme theme = UITheme.current();
            graphics.enableScissor(x, y, x + width, y + height);
            graphics.text(font, Component.literal(label), innerX(), innerY() + 3, theme.text().secondary(), true);

            String value = valueProvider.get();
            int valueX = innerX() + innerWidth() - font.width(value);
            graphics.text(font, Component.literal(value), valueX, innerY() + 3, theme.text().primary(), true);
            graphics.disableScissor();
        }
    }

    private static final class StatPointRow extends BaseComponent<StatPointRow> {
        private final AllocStat stat;
        private final PointsProvider pointsProvider;
        private final StatIncreaseAction increaseAction;
        private float hoverProgress;
        private float plusHoverProgress;

        private StatPointRow(AllocStat stat, PointsProvider pointsProvider, StatIncreaseAction increaseAction) {
            this.stat = stat;
            this.pointsProvider = pointsProvider;
            this.increaseAction = increaseAction;
        }

        @Override
        public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            UITheme theme = UITheme.current();
            boolean valueHovered = isValueHovered(mouseX, mouseY);
            boolean plusHovered = isPlusHovered(mouseX, mouseY) && pointsProvider.points() > 0;
            hoverProgress = stepAnimation(hoverProgress, valueHovered, 0.5f, delta);
            plusHoverProgress = stepAnimation(plusHoverProgress, plusHovered, 0.5f, delta);

            graphics.enableScissor(x, y, x + width, y + height);
            int rowBg = valueHovered ? theme.panel().bgHover() : theme.panel().bg();
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, rowBg);
            graphics.outline(x, y, width, height, valueHovered ? theme.panel().borderHover() : theme.panel().border());

            graphics.text(font, Component.literal(stat.code), innerX() + 4, innerY() + 7, theme.text().accent(), true);
            graphics.text(font, Component.literal(stat.name), innerX() + 36, innerY() + 7, theme.text().secondary(), true);

            String value = String.valueOf(stat.base + stat.addition);
            int valueX = x + width - 52;
            graphics.text(font, Component.literal(value), valueX, innerY() + 7, theme.text().primary(), true);

            if (hoverProgress > 0.01f) {
                String detail = "(" + stat.base + " + " + stat.addition + ")";
                graphics.text(font, Component.literal(detail), valueX - font.width(detail) - 8, innerY() + 7, theme.text().accent(), true);
            }

            int plusX = plusX();
            int plusColor = pointsProvider.points() > 0 ? theme.text().primary() : theme.text().disabled();
            int plusBg = plusHovered ? theme.button().bg().hover() : theme.button().bg().normal();
            graphics.fill(plusX, y + 3, plusX + 18, y + height - 3, plusBg);
            graphics.outline(plusX, y + 3, 18, height - 6, plusHovered ? theme.button().border().hover() : theme.button().border().normal());
            graphics.centeredText(font, Component.literal("+"), plusX + 9, innerY() + 7, plusColor);
            graphics.disableScissor();
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
            return click(event.button(), event.x(), event.y());
        }

        private boolean click(int button, double mouseX, double mouseY) {
            if (button != 0 || !isPlusHovered(mouseX, mouseY)) return false;
            if (increaseAction.increase()) UISounds.playClick();
            return true;
        }

        private boolean isValueHovered(double mouseX, double mouseY) {
            int valueX = x + width - 58;
            return mouseX >= valueX && mouseX < plusX() - 4 && mouseY >= y && mouseY < y + height;
        }

        private boolean isPlusHovered(double mouseX, double mouseY) {
            int plusX = plusX();
            return mouseX >= plusX && mouseX < plusX + 18 && mouseY >= y + 3 && mouseY < y + height - 3;
        }

        private int plusX() {
            return x + width - 24;
        }
    }

    private interface PointsProvider {
        int points();
    }
}
