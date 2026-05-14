package com.sypztep.plateau.test.screen;

import com.sypztep.plateau.client.PlateauUIClient;
import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.screen.TabContext;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v1.ui.theme.UIThemeRegistry;
import com.sypztep.plateau.client.v2.ui.Containers;
import com.sypztep.plateau.client.v2.ui.Overlays;
import com.sypztep.plateau.client.v2.ui.Panels;
import com.sypztep.plateau.client.v2.ui.WidgetComponents;
import com.sypztep.plateau.client.v2.ui.container.ScrollablePanel;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import com.sypztep.plateau.client.v2.ui.overlay.DetachablePanel;
import com.sypztep.plateau.client.v2.ui.overlay.DialogComponent;
import com.sypztep.plateau.client.v2.ui.screen.Tab2;
import com.sypztep.plateau.test.UITestClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ComponentDocsTab extends Tab2 {
    private int clickCount;

    public ComponentDocsTab() {
        super(UITestClient.id("docs"), Component.literal("Docs"));
    }

    @Override
    protected BaseComponent<?> build(TabContext ctx) {
        DialogComponent dialog = Overlays.dialog("DialogComponent")
                .dialogWidth(240)
                .dialogHeight(120)
                .content(WidgetComponents.text("Dialog content is mounted above normal content and blocks lower input.")
                        .sizing(Sizing.fill(), Sizing.content()))
                .button("OK", DialogComponent::close);

        return Overlays.windowLayer().content(Containers.stackFill()
                .child(Containers.tabs()
                        .padding(Insets.of(8, 14))
                        .contentSlideAnimation(0.35f)
                        .tab("Label", labelsPage())
                        .tab("Text", textPage())
                        .tab("Button", buttonsPage())
                        .tab("Input", inputsPage())
                        .tab("Drop", dropdownPage())
                        .tab("Panel", panelsPage())
                        .tab("Scroll", scrollPage())
                        .tab("Layout", layoutPage())
                        .tab("Tabs", tabsPage())
                        .tab("Overlay", overlayPage(dialog))
                        .tab("Theme", themePage()))
                .child(dialog));
    }

    private BaseComponent<?> labelsPage() {
        return docPage("LabelComponent")
                .child(WidgetComponents.label("Primary label").sizing(Sizing.fill(), Sizing.fixed(10)))
                .child(WidgetComponents.label("Accent label", 0xFFEEDDAA).sizing(Sizing.fill(), Sizing.fixed(10)))
                .child(WidgetComponents.label(Component.literal("Component label").withColor(0xFF5AB66A))
                        .sizing(Sizing.fill(), Sizing.fixed(10)))
                .child(snippet("""
                        WidgetComponents.label("Primary label")
                            .sizing(Sizing.fill(), Sizing.fixed(10))
                        """));
    }

    private BaseComponent<?> textPage() {
        return docPage("TextComponent")
                .child(WidgetComponents.text("TextComponent wraps by width and computes content height from wrapped lines. Resize the screen to test wrapping.")
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(WidgetComponents.text(Component.literal("Rich text: ")
                        .append(Component.literal("hover link")
                                .withStyle(style -> style
                                        .withBold(true)
                                        .withUnderlined(true)
                                        .withColor(0xFF5AB66A)
                                        .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://example.com")))
                                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Tooltip from vanilla Style hover event."))))))
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(WidgetComponents.text(Component.literal("Command: ")
                        .append(Component.literal("/plateau-ui")
                                .withStyle(style -> style
                                        .withUnderlined(true)
                                        .withClickEvent(new ClickEvent.SuggestCommand("/plateau-ui"))
                                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("SuggestCommand keeps full text hit range."))))))
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(snippet("""
                        WidgetComponents.text(Component.literal("Web Link")
                            .withStyle(style -> style
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.OpenUrl(uri))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("tip")))))
                        """));
    }

    private BaseComponent<?> buttonsPage() {
        var status = WidgetComponents.label("Clicks: " + clickCount).sizing(Sizing.fill(), Sizing.fixed(10));
        return docPage("ButtonComponent")
                .child(status)
                .child(Containers.horizontal(Sizing.fill(), Sizing.fixed(22)).gap(4)
                        .child(WidgetComponents.button("Click", button -> {
                            clickCount++;
                            status.text(Component.literal("Clicks: " + clickCount));
                        }).sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(WidgetComponents.button("Disabled").enabled(false).sizing(Sizing.fill(), Sizing.fixed(22))))
                .child(snippet("""
                        WidgetComponents.button("Click", button -> {
                            // mutate state
                        }).sizing(Sizing.fill(), Sizing.fixed(22))
                        """));
    }

    private BaseComponent<?> inputsPage() {
        return docPage("StringComponent / TextAreaComponent / CheckBoxComponent / SliderButtonComponent")
                .child(WidgetComponents.string("Single line string").value("Edit me")
                        .sizing(Sizing.fill(), Sizing.fixed(20)))
                .child(WidgetComponents.textArea("Text area").value("Line one\nLine two")
                        .sizing(Sizing.fill(), Sizing.fixed(58)))
                .child(WidgetComponents.checkbox("Enable option", true)
                        .sizing(Sizing.fill(), Sizing.fixed(18)))
                .child(WidgetComponents.slider("Volume", 0.0, 1.0, 0.65)
                        .sizing(Sizing.fill(), Sizing.fixed(20)))
                .child(snippet("""
                        WidgetComponents.string("Search...")
                        WidgetComponents.textArea("Notes")
                        WidgetComponents.checkbox("Enabled", true)
                        WidgetComponents.slider("Volume", 0.0, 1.0, 0.65)
                        """));
    }

    private BaseComponent<?> dropdownPage() {
        return docPage("DropdownComponent")
                .child(WidgetComponents.dropdown("Small", "Medium", "Large", "Huge")
                        .sizing(Sizing.fill(), Sizing.fixed(20)))
                .child(WidgetComponents.text("Open the dropdown inside this scroll body. Its menu renders above siblings and blocks lower input while hovered.")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(snippet("""
                        WidgetComponents.dropdown("Small", "Medium", "Large")
                            .sizing(Sizing.fill(), Sizing.fixed(20))
                        """));
    }

    private BaseComponent<?> panelsPage() {
        return docPage("PanelComponent / ScrollablePanel / DetachablePanel")
                .child(Panels.fixed("Fixed Panel")
                        .bodyPadding(Insets.of(6))
                        .child(WidgetComponents.text("Use PanelComponent for titled boxed content.")
                                .sizing(Sizing.fill(), Sizing.content()))
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(Panels.scroll("Scrollable Panel")
                        .scrollPadding(Insets.of(6))
                        .child(WidgetComponents.text("ScrollablePanel combines a title and a scroll body. Add enough content and the body scrolls.").sizing(Sizing.fill(), Sizing.content()))
                        .child(WidgetComponents.text("Line 01\nLine 02\nLine 03\nLine 04\nLine 05\nLine 06").secondary().sizing(Sizing.fill(), Sizing.content()))
                        .sizing(Sizing.fill(), Sizing.fixed(90)))
                .child(Panels.detachable("Detachable Panel")
                        .content(WidgetComponents.text("Open this panel into the WindowLayer. Close returns it to the tile.")
                                .sizing(Sizing.fill(), Sizing.content()))
                        .windowSize(260, 140)
                        .openTrigger(DetachablePanel.WindowOpenTrigger.ICON_ONLY)
                        .sizing(Sizing.fill(), Sizing.fixed(86)))
                .child(snippet("""
                        Panels.fixed("Title").child(content)
                        Panels.scroll("Title").child(content)
                        Panels.detachable("Title").content(content)
                        """));
    }

    private BaseComponent<?> scrollPage() {
        var scroll = Panels.scroll("ScrollContainer")
                .scrollPadding(Insets.of(6))
                .gap(4)
                .sizing(Sizing.fill(), Sizing.fixed(170));
        for (int i = 1; i <= 16; i++) {
            scroll.child(WidgetComponents.label("Row " + i).sizing(Sizing.fill(), Sizing.fixed(10)));
        }
        return docPage("ScrollContainer")
                .child(scroll)
                .child(snippet("""
                        Containers.scrollable(Sizing.fill(), Sizing.fill())
                            .padding(Insets.of(4))
                            .gap(3)
                            .child(row)
                        """));
    }

    private BaseComponent<?> layoutPage() {
        return docPage("FlowLayout / StackLayout / SplitLayout")
                .child(Containers.horizontal(Sizing.fill(), Sizing.fixed(22)).gap(4)
                        .child(WidgetComponents.button("A").sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(WidgetComponents.button("B").sizing(Sizing.fill(2), Sizing.fixed(22)))
                        .child(WidgetComponents.button("C").sizing(Sizing.fill(), Sizing.fixed(22))))
                .child(Containers.split(com.sypztep.plateau.client.v2.ui.layout.SplitLayout.Axis.HORIZONTAL, 0.35f)
                        .child(Panels.fixed("Left").child(WidgetComponents.label("35%").sizing(Sizing.fill(), Sizing.fixed(10))))
                        .child(Panels.fixed("Right").child(WidgetComponents.label("65%").sizing(Sizing.fill(), Sizing.fixed(10))))
                        .sizing(Sizing.fill(), Sizing.fixed(74)))
                .child(Containers.stack(Sizing.fill(), Sizing.fixed(44))
                        .surface(Surface.outline())
                        .child(WidgetComponents.label("Stack child A").sizing(Sizing.fill(), Sizing.fixed(10)))
                        .child(WidgetComponents.label("Stack child B", 0xFFEEDDAA).margins(Insets.of(16, 8, 0, 0)).sizing(Sizing.fill(), Sizing.fixed(10))))
                .child(snippet("""
                        Containers.vertical(...).gap(4).child(a).child(b)
                        Containers.horizontal(...).child(a.sizing(Sizing.fill(), ...))
                        Containers.split(Axis.HORIZONTAL, 0.35f).child(left).child(right)
                        Containers.stack(...).child(base).child(overlay)
                        """));
    }

    private BaseComponent<?> tabsPage() {
        return docPage("TabComponent")
                .child(Containers.tabs()
                        .padding(Insets.of(6))
                        .surface(Surface.outline())
                        .contentSlideAnimation(0.35f)
                        .tab("One", WidgetComponents.text("First tab body").sizing(Sizing.fill(), Sizing.content()))
                        .tab("Two", WidgetComponents.text("Second tab body").sizing(Sizing.fill(), Sizing.content()))
                        .tab("Three", WidgetComponents.text("Third tab body").sizing(Sizing.fill(), Sizing.content()))
                        .sizing(Sizing.fill(), Sizing.fixed(98)))
                .child(snippet("""
                        Containers.tabs()
                            .tab("One", firstContent)
                            .tab("Two", secondContent)
                            .contentSlideAnimation(0.35f)
                        """));
    }

    private BaseComponent<?> overlayPage(DialogComponent dialog) {
        return docPage("DialogComponent / WindowLayer")
                .child(WidgetComponents.text("WindowLayer is the root host for detached panels and drag previews. DialogComponent should be stacked at the root so it is not clipped.")
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(WidgetComponents.button("Open Dialog", button -> dialog.open())
                        .sizing(Sizing.fill(), Sizing.fixed(22)))
                .child(snippet("""
                        Overlays.windowLayer().content(root)

                        DialogComponent dialog = Overlays.dialog("Title")
                            .content(body)
                            .button("OK", DialogComponent::close)
                        """));
    }

    private BaseComponent<?> themePage() {
        return docPage("Theme")
                .child(Containers.horizontal(Sizing.fill(), Sizing.fixed(22)).gap(4)
                        .child(WidgetComponents.button("Dark", button -> UIThemeRegistry.INSTANCE.apply(PlateauUIClient.id("dark")))
                                .sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(WidgetComponents.button("Legacy", button -> UIThemeRegistry.INSTANCE.apply(PlateauUIClient.id("legacy")))
                                .sizing(Sizing.fill(), Sizing.fixed(22))))
                .child(snippet("""
                        UIThemeRegistry.INSTANCE.apply(PlateauUIClient.id("dark"))
                        UIThemeRegistry.INSTANCE.apply(PlateauUIClient.id("legacy"))
                        """));
    }

    private ScrollablePanel docPage(String title) {
        ScrollablePanel panel = Panels.scroll(title)
                .scrollPadding(Insets.of(4))
                .gap(8)
                .child(WidgetComponents.separator());
        panel.bodyPadding(Insets.of(8));
        panel.sizing(Sizing.fill(), Sizing.fill());
        return panel;
    }

    private BaseComponent<?> snippet(String code) {
        return new CodeBlockComponent("java", code.strip())
                .sizing(Sizing.fill(), Sizing.content());
    }

    private static final class CodeBlockComponent extends BaseComponent<CodeBlockComponent> {
        private static final int HEADER_HEIGHT = 18;
        private static final int LINE_HEIGHT = 11;
        private static final int PAD_X = 8;
        private static final int PAD_Y = 6;
        private static final int COPY_WIDTH = 44;
        private static final int COPY_HEIGHT = 14;
        private static final Set<String> KEYWORDS = Set.of(
                "new", "return", "if", "else", "for", "while", "var", "true", "false", "null"
        );
        private static final Set<String> TYPES = Set.of(
                "Component", "Sizing", "Insets", "DialogComponent", "ClickEvent", "HoverEvent", "URI"
        );
        private static final Set<String> FACTORIES = Set.of(
                "WidgetComponents", "Containers", "Panels", "Overlays", "UIThemeRegistry", "PlateauUIClient"
        );

        private final String language;
        private final String code;
        private final String[] lines;
        private boolean wasCopyHovered;
        private long copiedAt;

        private CodeBlockComponent(String language, String code) {
            this.language = language;
            this.code = code;
            this.lines = code.isEmpty() ? new String[]{""} : code.split("\\R", -1);
            this.horizontalSizing = Sizing.fill();
            this.verticalSizing = Sizing.content();
        }

        @Override
        public int determineVerticalContentSize(int availableWidth) {
            return HEADER_HEIGHT + PAD_Y * 2 + Math.max(1, lines.length) * LINE_HEIGHT;
        }

        @Override
        public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            UITheme theme = UITheme.current();
            int bodyTop = y + HEADER_HEIGHT;
            int bg = ARGB.color(0xEE, ARGB.transparent(theme.panel().bg()));
            int header = ARGB.color(0xF4, ARGB.transparent(theme.panel().headerBg()));
            int border = isMouseOver(mouseX, mouseY) ? theme.panel().borderHover() : theme.panel().border();

            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, bg);
            graphics.fill(x + 1, y + 1, x + width - 1, bodyTop, header);
            graphics.outline(x, y, width, height, border);

            graphics.text(font, Component.literal(language), x + PAD_X, y + 5, theme.text().secondary(), false);
            drawCopyButton(graphics, mouseX, mouseY);

            graphics.enableScissor(x + 1, bodyTop, x + width - 1, y + height - 1);
            int lineY = bodyTop + PAD_Y;
            for (String line : lines) {
                drawHighlightedLine(graphics, line, x + PAD_X, lineY);
                lineY += LINE_HEIGHT;
            }
            graphics.disableScissor();
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 0 || !copyHovered(event.x(), event.y())) return false;
            minecraft.keyboardHandler.setClipboard(code);
            copiedAt = Util.getMillis();
            UISounds.playClick();
            return true;
        }

        private void drawCopyButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            UITheme theme = UITheme.current();
            boolean hovered = copyHovered(mouseX, mouseY);
            if (hovered && !wasCopyHovered) UISounds.playHover();
            wasCopyHovered = hovered;

            int buttonX = x + width - COPY_WIDTH - 5;
            int buttonY = y + 2;
            int fill = hovered ? theme.panel().bgHover() : ARGB.color(0x99, ARGB.transparent(theme.panel().bg()));
            int border = hovered ? theme.panel().borderHover() : theme.panel().border();
            String label = Util.getMillis() - copiedAt < 1200L ? "Copied" : "Copy";
            int labelX = buttonX + (COPY_WIDTH - font.width(label)) / 2;

            graphics.fill(buttonX, buttonY, buttonX + COPY_WIDTH, buttonY + COPY_HEIGHT, fill);
            graphics.outline(buttonX, buttonY, COPY_WIDTH, COPY_HEIGHT, border);
            graphics.text(font, Component.literal(label), labelX, buttonY + 3, hovered ? theme.text().primary() : theme.text().secondary(), false);
        }

        private void drawHighlightedLine(GuiGraphicsExtractor graphics, String line, int lineX, int lineY) {
            UITheme theme = UITheme.current();
            if (line.stripLeading().startsWith("//")) {
                drawSegment(graphics, line, lineX, lineY, ARGB.color(0xCC, theme.text().disabled()));
                return;
            }

            int nextX = lineX;
            int index = 0;
            while (index < line.length()) {
                char current = line.charAt(index);
                if (current == '"') {
                    int end = stringEnd(line, index + 1);
                    nextX = drawSegment(graphics, line.substring(index, end), nextX, lineY, 0xFFCE9178);
                    index = end;
                } else if (Character.isJavaIdentifierStart(current)) {
                    int end = index + 1;
                    while (end < line.length() && Character.isJavaIdentifierPart(line.charAt(end))) end++;
                    String token = line.substring(index, end);
                    nextX = drawSegment(graphics, token, nextX, lineY, tokenColor(token, theme));
                    index = end;
                } else if (Character.isDigit(current)) {
                    int end = index + 1;
                    while (end < line.length() && (Character.isDigit(line.charAt(end)) || line.charAt(end) == '.')) end++;
                    nextX = drawSegment(graphics, line.substring(index, end), nextX, lineY, 0xFFB5CEA8);
                    index = end;
                } else {
                    nextX = drawSegment(graphics, String.valueOf(current), nextX, lineY, ARGB.color(0xDD, theme.text().secondary()));
                    index++;
                }
            }
        }

        private int tokenColor(String token, UITheme theme) {
            if (KEYWORDS.contains(token)) return 0xFFC586C0;
            if (TYPES.contains(token)) return 0xFF4EC9B0;
            if (FACTORIES.contains(token)) return theme.text().accent();
            return theme.text().primary();
        }

        private int drawSegment(GuiGraphicsExtractor graphics, String text, int textX, int textY, int color) {
            graphics.text(font, Component.literal(text), textX, textY, color, false);
            return textX + font.width(text);
        }

        private int stringEnd(String line, int from) {
            for (int index = from; index < line.length(); index++) {
                if (line.charAt(index) == '"' && line.charAt(index - 1) != '\\') return index + 1;
            }
            return line.length();
        }

        private boolean copyHovered(double mouseX, double mouseY) {
            int buttonX = x + width - COPY_WIDTH - 5;
            int buttonY = y + 2;
            return mouseX >= buttonX && mouseX < buttonX + COPY_WIDTH
                    && mouseY >= buttonY && mouseY < buttonY + COPY_HEIGHT;
        }
    }
}
