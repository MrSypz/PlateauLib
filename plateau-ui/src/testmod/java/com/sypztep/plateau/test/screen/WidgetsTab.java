package com.sypztep.plateau.test.screen;

import com.sypztep.plateau.client.PlateauUIClient;
import com.sypztep.plateau.client.v1.ui.screen.TabContext;
import com.sypztep.plateau.client.v1.ui.theme.UIThemeRegistry;
import com.sypztep.plateau.client.v2.ui.Containers;
import com.sypztep.plateau.client.v2.ui.Overlays;
import com.sypztep.plateau.client.v2.ui.Panels;
import com.sypztep.plateau.client.v2.ui.WidgetComponents;
import com.sypztep.plateau.client.v2.ui.container.PanelComponent;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import com.sypztep.plateau.client.v2.ui.core.Surface;
import com.sypztep.plateau.client.v2.ui.overlay.ContextMenuComponent;
import com.sypztep.plateau.client.v2.ui.overlay.DialogComponent;
import com.sypztep.plateau.client.v2.ui.overlay.HoverCardComponent;
import com.sypztep.plateau.client.v2.ui.screen.Tab2;
import com.sypztep.plateau.client.v2.ui.widget.LabelComponent;
import com.sypztep.plateau.client.v2.ui.widget.StringComponent;
import com.sypztep.plateau.test.UITestClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.net.URI;

@Environment(EnvType.CLIENT)
public class WidgetsTab extends Tab2 {

    private int clickCount = 0;

    public WidgetsTab() {
        super(UITestClient.id("widgets"), Component.literal("Widgets"));
    }

    @Override
    protected BaseComponent<?> build(TabContext ctx) {
        LabelComponent clickStatus = WidgetComponents.label("Clicks: " + clickCount)
                .sizing(Sizing.fill(), Sizing.fixed(9));

        // ── Context menu ──────────────────────────────────────
        LabelComponent contextMenuStatus = WidgetComponents.label("No action yet.")
                .secondary()
                .sizing(Sizing.fill(), Sizing.fixed(9));

        // Declare menu first so trigger's onRightClick lambda can reference it.
        ContextMenuComponent contextMenu = Overlays.contextMenu()
                .label("Edit")
                .item("Undo", "Ctrl+Z", () -> contextMenuStatus.text(Component.literal("Action: Undo")))
                .item("Redo", "Ctrl+Y", false, () -> {})
                .separator()
                .item("Cut",   "Ctrl+X", () -> contextMenuStatus.text(Component.literal("Action: Cut")))
                .item("Copy",  "Ctrl+C", () -> contextMenuStatus.text(Component.literal("Action: Copy")))
                .item("Paste", "Ctrl+V", () -> contextMenuStatus.text(Component.literal("Action: Paste")))
                .separator()
                .checkItem("Word Wrap", false, checked ->
                        contextMenuStatus.text(Component.literal("Word Wrap: " + checked)));

        // sizing(fill, fixed(36)) here matters: the trigger IS the bordered box (see
        // contextMenuSection), so its hit-test bounds must cover the whole box, not just
        // a text-height strip at the top — otherwise right-click only works on a sliver
        // of what visually looks like one clickable area.
        LabelComponent contextMenuTrigger = WidgetComponents.label("Right-click anywhere in this box")
                .secondary()
                .centered(true)
                .surface(Surface.outline())
                .sizing(Sizing.fill(), Sizing.fixed(36))
                .onRightClick(e -> contextMenu.openAt(BaseComponent.currentScreenMouseX(), BaseComponent.currentScreenMouseY()));

        // ── Hover card ────────────────────────────────────────
        // HoverCard wraps the trigger inline — no separate root overlay needed. Same
        // sizing reasoning as the context menu trigger above: HoverCardComponent adopts
        // the trigger's declared sizing as its own, and hover is detected against ITS
        // bounds, so a fixed(9) trigger left most of the visible box un-hoverable.
        HoverCardComponent hoverCard = Overlays.hoverCard()
                .wrap(WidgetComponents.label("Hover over this box (500 ms delay)")
                        .secondary()
                        .centered(true)
                        .surface(Surface.outline())
                        .sizing(Sizing.fill(), Sizing.fixed(36)))
                .card(Containers.vertical(Sizing.fill(), Sizing.content())
                        .padding(Insets.of(4))
                        .gap(4)
                        .child(WidgetComponents.label("HoverCard").sizing(Sizing.fill(), Sizing.fixed(9)))
                        .child(WidgetComponents.separator())
                        .child(WidgetComponents.text("This card appears near the cursor via HoverCardOverlay, above all scissor regions.")
                                .secondary()
                                .sizing(Sizing.fill(), Sizing.content())))
                .openDelay(500)
                .closeDelay(200)
                .cardWidth(200);

        // ── Dialog ────────────────────────────────────────────
        DialogComponent dialog = new DialogComponent()
                .title("Widget Test Dialog")
                .dialogWidth(240)
                .dialogHeight(120)
                .content(WidgetComponents.text("""
                    This dialog is mounted as the last child of the widget test root.

                    Expected:
                    - backdrop covers the full screen
                    - clicks outside close the dialog
                    - buttons still work
                    - scroll content behind does not receive clicks
                    """)
                        .sizing(Sizing.fill(), Sizing.content()))
                .button("OK", d -> {
                    System.out.println("Dialog OK clicked");
                    d.close();
                })
                .button("Cancel", DialogComponent::close);

        LabelComponent inputResult = WidgetComponents.label("No input yet.")
                .secondary()
                .sizing(Sizing.fill(), Sizing.fixed(9));

        StringComponent inputField = WidgetComponents.string("Type something...")
                .sizing(Sizing.fill(), Sizing.fixed(20));

        DialogComponent inputDialog = new DialogComponent()
                .title("Input Dialog Test")
                .dialogWidth(260)
                .dialogHeight(130)
                .closeOnBackdrop(false)
                .content(Containers.vertical(Sizing.fill(), Sizing.content())
                        .gap(6)
                        .child(WidgetComponents.text("Type a word and press Submit. Output goes to console and the label below.")
                                .secondary()
                                .sizing(Sizing.fill(), Sizing.content()))
                        .child(inputField))
                .button("Submit", d -> {
                    String typed = inputField.value().trim();
                    System.out.println("[InputDialog] submitted: \"" + typed + "\"");
                    inputResult.text(Component.literal("Last input: \"" + typed + "\""));
                    d.close();
                })
                .button("Clear", d -> {
                    inputField.value("");
                    inputResult.text(Component.literal("Cleared."));
                })
                .button("Cancel", d -> {
                    inputField.value("");
                    d.close();
                });

        return Containers.stack(Sizing.fill(), Sizing.fill())
                .child(Containers.scrollable(Sizing.fill(), Sizing.fill())
                        .padding(Insets.of(8, 14))
                        .gap(8)
                        .child(infoSection())
                        .child(dialogSection(dialog))
                        .child(inputDialogSection(inputDialog, inputResult))
                        .child(contextMenuSection(contextMenuTrigger, contextMenuStatus))
                        .child(hoverCardSection(hoverCard))
                        .child(searchableDropdownSection())
                        .child(childInChildSection(clickStatus))
                        .child(rowSection())
                        .child(formSection())
                        .child(themeSection())
                        .child(v1CompareSection()))
                .child(dialog)
                .child(inputDialog)
                .child(contextMenu);
    }


    private PanelComponent section(String title) {
        return Panels.fixed(title)
                .bodyPadding(Insets.of(6, 8))
                .gap(6)
                .hoverSurface(false)
                .child(WidgetComponents.separator());
    }


    private BaseComponent<?> infoSection() {
        // Vertical flow: wrapped text inside a panel.
        return section("Info")
                .child(WidgetComponents.text("plateau-ui v2 - no x/y math, no getContentY(). This is TextComponent, not LabelComponent, so it should wrap instead of overflowing.")
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(WidgetComponents.text("Sizing.fill() distributes space. PanelComponent draws the box. This second line should wrap correctly when the panel width gets smaller.")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(WidgetComponents.text(Component.literal("Web Link: ")
                        .append(Component.literal("Test")
                                .withStyle(style -> style
                                        .withBold(true)
                                        .withUnderlined(true)
                                        .withColor(0x5AB66A)
                                        .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://www.youtube.com/watch?v=dQw4w9WgXcQ")))
                                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("fully doc to see how this lib work."))))))
                        .sizing(Sizing.fill(), Sizing.content())
                )
                .child(WidgetComponents.text(Component.literal("Rich text: ")
                        .append(Component.literal("bold underlined command link")
                                .withStyle(style -> style
                                        .withBold(true)
                                        .withUnderlined(true)
                                        .withColor(0x5AB66A)
                                        .withClickEvent(new ClickEvent.SuggestCommand("/plateau-ui"))
                                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Hover sound, tooltip, underline highlight, and click handling come from TextComponent."))))))
                        .sizing(Sizing.fill(), Sizing.content()));
    }


    private BaseComponent<?> childInChildSection(LabelComponent clickStatus) {
        // Tabs: outer tabs mounted inside a vertical section.
        return section("Child-in-child Layout")
                .child(WidgetComponents.text("Outer TabComponent lives inside this section. Its children include ScrollContainer, nested TabComponent, TextComponent, FlowLayout and buttons.")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(Containers.tabs()
                        .sizing(Sizing.fill(), Sizing.fixed(190))
                        .padding(Insets.of(6))
                        .surface(Surface.outline())
                        .tab("Text + Buttons", textButtonsTab(clickStatus))
                        .tab("Nested Tabs", nestedTabsTab())
                        .tab("Mixed Layout", mixedLayoutTab()));
    }

    private BaseComponent<?> textButtonsTab(LabelComponent clickStatus) {
        // Scroll container: wrapped text and buttons share one clipped area.
        return Panels.scroll("Scrollable Tab Content")
                .scrollPadding(Insets.of(8))
                .gap(8)
                .child(WidgetComponents.text("""
                        This is TextComponent inside ScrollContainer inside TabComponent.

                        Expected:
                        - text wraps instead of overflowing
                        - scroll wheel works only when mouse is over this panel
                        - button clicks still fire inside the scroll content
                        - switching tabs should not break layout
                        """)
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(clickStatus)
                .child(WidgetComponents.button("Click inside tab scroll", button -> {
                    clickCount++;
                    clickStatus.text(Component.literal("Clicks: " + clickCount));
                }).sizing(Sizing.fill(), Sizing.fixed(22)))
                .child(WidgetComponents.button("Disabled inside scroll").enabled(false)
                        .sizing(Sizing.fill(), Sizing.fixed(22)))
                .child(WidgetComponents.separator())
                .child(WidgetComponents.text("""
                        Filler text for scroll testing.
                        Line 01 - the scroll container should clip this content.
                        Line 02 - mouse coordinates should still hit the button correctly.
                        Line 03 - nested content-space dispatch should stay correct.
                        Line 04 - no manual x/y math in the tab content.
                        Line 05 - TextComponent height comes from wrapped lines.
                        Line 06 - this should not overflow outside the tab body.
                        """)
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .sizing(Sizing.fill(), Sizing.fill());
    }

    private BaseComponent<?> nestedTabsTab() {
        // Nested content: tabs inside tabs with active scroll bodies.
        return Containers.tabs()
                .sizing(Sizing.fill(), Sizing.fill())
                .padding(Insets.of(6))
                .surface(Surface.outline())
                .tab("Inner A", Panels.scroll("Inner A Scroll")
                        .scrollPadding(Insets.of(8))
                        .gap(8)
                        .child(WidgetComponents.text("""
                                Inner A:
                                This is a ScrollContainer inside an inner TabComponent,
                                inside an outer TabComponent.

                                Test:
                                - outer tab header click
                                - inner tab header click
                                - scroll inside active inner tab
                                - button inside inner scroll
                                """)
                                .sizing(Sizing.fill(), Sizing.content()))
                        .child(WidgetComponents.button("Inner A Button", b -> {
                            System.out.println("Inner A clicked");
                        }).sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(WidgetComponents.text("""
                                More lines for inner scroll.
                                More lines for inner scroll.
                                More lines for inner scroll.
                                More lines for inner scroll.
                                More lines for inner scroll.
                                """)
                                .secondary()
                                .sizing(Sizing.fill(), Sizing.content()))
                        .sizing(Sizing.fill(), Sizing.fill()))
                .tab("Inner B", Containers.vertical(Sizing.fill(), Sizing.fill())
                        .padding(Insets.of(8))
                        .gap(8)
                        .child(WidgetComponents.text("Inner B uses FlowLayout directly, no scroll.")
                                .sizing(Sizing.fill(), Sizing.content()))
                        .child(Containers.horizontal(Sizing.fill(), Sizing.fixed(22)).gap(4)
                                .child(WidgetComponents.button("Accept", b -> System.out.println("Accept")).sizing(Sizing.fill(), Sizing.fixed(22)))
                                .child(WidgetComponents.button("Cancel", b -> System.out.println("Cancel")).sizing(Sizing.fill(), Sizing.fixed(22))))
                        .child(WidgetComponents.text("If this panel renders correctly, nested mount/layout is working.")
                                .secondary()
                                .sizing(Sizing.fill(), Sizing.content())));
    }

    private BaseComponent<?> mixedLayoutTab() {
        // Stack-free content: fixed-height horizontal row in vertical flow.
        return Containers.vertical(Sizing.fill(), Sizing.fill())
                .padding(Insets.of(8))
                .gap(8)
                .surface(Surface.outline())
                .child(WidgetComponents.text("Mixed tab content: text, separator, horizontal row, and a fill spacer.")
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(WidgetComponents.separator())
                .child(Containers.horizontal(Sizing.fill(), Sizing.fixed(22)).gap(4)
                        .child(WidgetComponents.button("Left").sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(WidgetComponents.button("Middle").sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(WidgetComponents.button("Right").sizing(Sizing.fill(), Sizing.fixed(22))))
                .child(WidgetComponents.text("""
                        This tab intentionally does not scroll.
                        If text becomes too large here, it should be clipped by parent height,
                        which makes the difference between FlowLayout content and ScrollContainer obvious.
                        """)
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()));
    }


    private BaseComponent<?> rowSection() {
        // Horizontal flow: fill-sized buttons divide the row evenly.
        return section("Row Layout")
                .child(WidgetComponents.text("Each button uses Sizing.fill() - equal share automatically.")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(Containers.horizontal(Sizing.fill(), Sizing.fixed(22)).gap(4)
                        .child(WidgetComponents.button("A").sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(WidgetComponents.button("B").sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(WidgetComponents.button("C").sizing(Sizing.fill(), Sizing.fixed(22))));
    }

    private BaseComponent<?> formSection() {
        return section("Form Components")
                .child(WidgetComponents.text("Slider, string input, text area, checkbox, and dropdown are v2-native components.")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(WidgetComponents.slider("Volume", 0.0, 1.0, 0.65)
                        .sizing(Sizing.fill(), Sizing.fixed(20)))
                .child(WidgetComponents.string("Single line string")
                        .value("Edit me")
                        .sizing(Sizing.fill(), Sizing.fixed(20)))
                .child(WidgetComponents.textArea("Text area")
                        .value("Line one\nLine two")
                        .sizing(Sizing.fill(), Sizing.fixed(58)))
                .child(WidgetComponents.checkbox("Enable particles", true)
                        .sizing(Sizing.fill(), Sizing.fixed(18)))
                .child(WidgetComponents.dropdown("Small", "Medium", "Large", "Huge")
                        .sizing(Sizing.fill(), Sizing.fixed(20)));
    }


    private BaseComponent<?> themeSection() {
        return section("Theme")
                .child(Containers.horizontal(Sizing.fill(), Sizing.fixed(22)).gap(6)
                        .child(WidgetComponents.button("Dark", b -> UIThemeRegistry.INSTANCE.apply(PlateauUIClient.id("dark")))
                                .sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(WidgetComponents.button("Legacy", b -> UIThemeRegistry.INSTANCE.apply(PlateauUIClient.id("legacy")))
                                .sizing(Sizing.fill(), Sizing.fixed(22))));
    }


    private BaseComponent<?> v1CompareSection() {
        return section("v1 vs v2 Button (hover + press comparison)")
                .child(WidgetComponents.text("v1 - bounce + shadow + glow").secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(Containers.horizontal(Sizing.fill(), Sizing.fixed(22)).gap(4)
                        .child(new V1ButtonWrapper("v1 Normal").sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(new V1ButtonWrapper("v1 Disabled").enabled(false).sizing(Sizing.fill(), Sizing.fixed(22))))
                .child(WidgetComponents.text("v2 - flat dim + ease-out").secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(Containers.horizontal(Sizing.fill(), Sizing.fixed(22)).gap(4)
                        .child(WidgetComponents.button("v2 Normal").sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(WidgetComponents.button("v2 Disabled").enabled(false).sizing(Sizing.fill(), Sizing.fixed(22))));
    }

    private BaseComponent<?> dialogSection(DialogComponent dialog) {
        // Stack overlay: dialog is mounted as the last root child.
        return section("Dialog")
                .child(WidgetComponents.text("""
                    DialogComponent must be mounted outside the ScrollContainer and added as the last child
                    of an overlay/root container. Otherwise the backdrop can be clipped by scroll scissor.
                    """)
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(Containers.horizontal(Sizing.fill(), Sizing.fixed(22)).gap(4)
                        .child(WidgetComponents.button("Open Dialog", b -> dialog.open())
                                .sizing(Sizing.fill(), Sizing.fixed(22)))
                        .child(WidgetComponents.button("Toggle Dialog", b -> dialog.toggle())
                                .sizing(Sizing.fill(), Sizing.fixed(22))));
    }

    private BaseComponent<?> inputDialogSection(DialogComponent inputDialog, LabelComponent inputResult) {
        return section("Input Dialog (type + submit)")
                .child(WidgetComponents.text("Opens a dialog with a StringComponent. Submit prints the typed value to console and updates the label. Clear resets without closing. Cancel discards and closes.")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(WidgetComponents.button("Open Input Dialog", b -> inputDialog.open())
                        .sizing(Sizing.fill(), Sizing.fixed(22)))
                .child(inputResult);
    }

    private BaseComponent<?> searchableDropdownSection() {
        LabelComponent selectionLabel = WidgetComponents.label("Selected: (none)")
                .secondary()
                .sizing(Sizing.fill(), Sizing.fixed(9));

        var dropdown = WidgetComponents.searchableDropdown(
                        "Apple", "Banana", "Cherry", "Date", "Elderberry",
                        "Fig", "Grape", "Honeydew", "Kiwi", "Lemon",
                        "Mango", "Nectarine", "Orange", "Papaya", "Quince")
                .onChanged(v -> selectionLabel.text(Component.literal("Selected: " + v)));

        return section("Searchable Dropdown")
                .child(WidgetComponents.text("Type to filter, click a suggestion to select. List floats above siblings — no scissor issues.")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(dropdown)
                .child(selectionLabel);
    }

    private BaseComponent<?> contextMenuSection(LabelComponent trigger, LabelComponent statusLabel) {
        // trigger IS the bordered box (see its construction above) — no separate wrapping
        // container, so there is no dead padding area that looks clickable but isn't.
        return section("Context Menu")
                .child(WidgetComponents.text("Right-click the box below to open a context menu.")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(trigger)
                .child(statusLabel);
    }

    private BaseComponent<?> hoverCardSection(HoverCardComponent hoverCard) {
        // hoverCard wraps a trigger that IS the bordered box — see its construction above.
        return section("Hover Card")
                .child(WidgetComponents.text("Hover over the box below for 500 ms to open a card. Card renders above all content via HoverCardOverlay (no scissor conflicts).")
                        .secondary()
                        .sizing(Sizing.fill(), Sizing.content()))
                .child(hoverCard);
    }
}
