package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class StringComponent extends BaseComponent<StringComponent> {
    private String value = "";
    private Component placeholder = Component.empty();
    private int cursor = 0;
    private int display = 0;
    private int maxLength = 256;
    private boolean editable = true;
    private boolean shadow = true;
    private Consumer<String> onChanged = ignored -> {};
    private long focusedTime = Util.getMillis();

    public StringComponent() {
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing = Sizing.fixed(20);
    }

    public StringComponent(Component placeholder) {
        this();
        this.placeholder = placeholder;
    }

    @Override
    public int determineHorizontalContentSize(int space) { return Math.max(80 + padding.horizontal(), Math.max(0, space)); }

    @Override
    public int determineVerticalContentSize(int space) { return 20 + padding.vertical(); }

    @Override
    protected boolean isFocusable() { return true; }

    @Override
    public void setFocused(boolean focused) {
        if (focused && !this.focused) focusedTime = Util.getMillis();
        super.setFocused(focused);
    }

    @Override
    public void extract(net.minecraft.client.gui.GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0) return;

        UITheme theme = UITheme.current();
        boolean hot = focused || isMouseOver(mouseX, mouseY);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, hot ? theme.panel().bgHover() : theme.panel().bg());
        g.outline(x, y, width, height, hot ? theme.panel().borderHover() : theme.panel().border());

        int tx = innerX() + 5;
        int ty = innerY() + (innerHeight() - font.lineHeight) / 2;
        int textW = Math.max(1, innerWidth() - 10);
        updateDisplay(textW);
        String shown = font.plainSubstrByWidth(value.substring(display), textW);

        g.enableScissor(tx, y, innerX() + innerWidth() - 5, y + height);
        if (shown.isEmpty() && value.isEmpty() && !focused) {
            g.text(font, placeholder, tx, ty, theme.text().secondary(), shadow);
        } else {
            g.text(font, shown, tx, ty, editable ? theme.text().primary() : theme.text().disabled(), shadow);
        }

        if (focused && editable && cursorVisible()) {
            int cursorX = tx + font.width(value.substring(display, Math.min(cursor, display + shown.length())));
            g.fill(cursorX, ty - 1, cursorX + 1, ty + font.lineHeight + 1, theme.text().accent());
        }
        g.disableScissor();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y()) || event.button() != 0) return false;
        cursor = cursorAt(event.x());
        return true;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (!focused || !editable || !event.isAllowedChatCharacter()) return false;
        insert(event.codepointAsString());
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!focused || !editable) return false;

        if (event.isSelectAll()) {
            cursor = value.length();
            return true;
        }
        if (event.isCopy()) {
            minecraft.keyboardHandler.setClipboard(value);
            return true;
        }
        if (event.isPaste()) {
            insert(minecraft.keyboardHandler.getClipboard());
            return true;
        }
        if (event.isCut()) {
            minecraft.keyboardHandler.setClipboard(value);
            value("");
            return true;
        }

        return switch (event.key()) {
            case GLFW.GLFW_KEY_BACKSPACE -> { deleteBefore(); yield true; }
            case GLFW.GLFW_KEY_DELETE -> { deleteAfter(); yield true; }
            case GLFW.GLFW_KEY_LEFT -> { cursor = event.hasControlDownWithQuirk() ? previousWord(cursor) : Math.max(0, cursor - 1); yield true; }
            case GLFW.GLFW_KEY_RIGHT -> { cursor = event.hasControlDownWithQuirk() ? nextWord(cursor) : Math.min(value.length(), cursor + 1); yield true; }
            case GLFW.GLFW_KEY_HOME -> { cursor = 0; yield true; }
            case GLFW.GLFW_KEY_END -> { cursor = value.length(); yield true; }
            default -> false;
        };
    }

    private boolean cursorVisible() {
        return ((Util.getMillis() - focusedTime) / 300L) % 2L == 0L;
    }

    private int cursorAt(double mouseX) {
        int textX = innerX() + 5;
        int relative = Math.max(0, (int) (mouseX - textX));
        String tail = value.substring(display);
        int local = font.plainSubstrByWidth(tail, relative).length();
        return Math.min(value.length(), display + local);
    }

    private void updateDisplay(int innerWidth) {
        display = Math.min(display, cursor);
        while (display > 0 && font.width(value.substring(display, cursor)) < innerWidth / 2) {
            display--;
        }
        while (display < cursor && font.width(value.substring(display, cursor)) > innerWidth) {
            display++;
        }
    }

    private void insert(String text) {
        if (text == null || text.isEmpty()) return;
        String filtered = text.replace("\n", "").replace("\r", "");
        int room = Math.max(0, maxLength - value.length());
        if (filtered.length() > room) filtered = filtered.substring(0, room);
        if (filtered.isEmpty()) return;
        value = value.substring(0, cursor) + filtered + value.substring(cursor);
        cursor += filtered.length();
        onChanged.accept(value);
    }

    private void deleteBefore() {
        if (cursor <= 0) return;
        value = value.substring(0, cursor - 1) + value.substring(cursor);
        cursor--;
        onChanged.accept(value);
    }

    private void deleteAfter() {
        if (cursor >= value.length()) return;
        value = value.substring(0, cursor) + value.substring(cursor + 1);
        onChanged.accept(value);
    }

    private int previousWord(int from) {
        int i = Math.max(0, from - 1);
        while (i > 0 && value.charAt(i) == ' ') i--;
        while (i > 0 && value.charAt(i - 1) != ' ') i--;
        return i;
    }

    private int nextWord(int from) {
        int i = Math.min(value.length(), from);
        while (i < value.length() && value.charAt(i) != ' ') i++;
        while (i < value.length() && value.charAt(i) == ' ') i++;
        return i;
    }

    public StringComponent value(String value) {
        this.value = value == null ? "" : value.substring(0, Math.min(value.length(), maxLength));
        this.cursor = Math.min(cursor, this.value.length());
        onChanged.accept(this.value);
        return this;
    }

    public String value() { return value; }
    public StringComponent placeholder(Component placeholder) { this.placeholder = placeholder; return this; }
    public StringComponent maxLength(int maxLength) { this.maxLength = Math.max(0, maxLength); return value(value); }
    public StringComponent editable(boolean editable) { this.editable = editable; return this; }
    public StringComponent shadow(boolean shadow) { this.shadow = shadow; return this; }
    public StringComponent onChanged(Consumer<String> onChanged) { this.onChanged = onChanged != null ? onChanged : ignored -> {}; return this; }
}
