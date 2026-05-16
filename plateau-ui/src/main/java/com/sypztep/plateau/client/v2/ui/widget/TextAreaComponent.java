package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class TextAreaComponent extends BaseComponent<TextAreaComponent> {
    private String value = "";
    private Component placeholder = Component.empty();
    private int cursor = 0;
    private int scrollLine = 0;
    private int maxLength = 4096;
    private int preeditLength = 0;
    private Consumer<String> onChanged = ignored -> {};
    private long focusedTime = Util.getMillis();

    public TextAreaComponent() {
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing = Sizing.fixed(64);
    }

    public TextAreaComponent(Component placeholder) {
        this();
        this.placeholder = placeholder;
    }

    @Override
    public int determineHorizontalContentSize(int space) { return Math.max(120 + padding.horizontal(), Math.max(0, space)); }

    @Override
    public int determineVerticalContentSize(int space) { return 64 + padding.vertical(); }

    @Override
    protected boolean isFocusable() { return true; }

    @Override
    public void setFocused(boolean focused) {
        if (focused && !this.focused) focusedTime = Util.getMillis();
        super.setFocused(focused);
    }

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (width <= 0 || height <= 0) return;

        UITheme theme = UITheme.current();
        boolean hot = focused || isMouseOver(mouseX, mouseY);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, hot ? theme.panel().bgHover() : theme.panel().bg());
        g.outline(x, y, width, height, hot ? theme.panel().borderHover() : theme.panel().border());

        int ix = innerX() + 5;
        int iy = innerY() + 5;
        int iw = Math.max(1, innerWidth() - 10);
        int ih = Math.max(1, innerHeight() - 10);
        List<String> lines = lines();
        ensureCursorVisible(lines, ih);

        g.enableScissor(ix, iy, ix + iw, iy + ih);
        if (value.isEmpty() && !focused) {
            g.text(font, placeholder, ix, iy, theme.text().secondary(), true);
        } else {
            int maxLines = Math.max(1, ih / font.lineHeight);
            for (int i = 0; i < maxLines && scrollLine + i < lines.size(); i++) {
                g.text(font, lines.get(scrollLine + i), ix, iy + i * font.lineHeight, theme.text().primary(), true);
            }

            if (focused && cursorVisible()) {
                int[] lc = lineColumnAt(cursor, lines);
                int cursorLine = lc[0] - scrollLine;
                if (cursorLine >= 0 && cursorLine < maxLines) {
                    String before = lines.get(lc[0]).substring(0, Math.min(lc[1], lines.get(lc[0]).length()));
                    int cx = ix + font.width(before);
                    int cy = iy + cursorLine * font.lineHeight;
                    g.fill(cx, cy - 1, cx + 1, cy + font.lineHeight + 1, theme.text().accent());
                }
            }
        }
        g.disableScissor();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y()) || event.button() != 0) return false;
        cursor = cursorAt(event.x(), event.y());
        return true;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (!focused || !event.isAllowedChatCharacter()) return false;
        if (preeditLength > 0) return false; // IME is composing; charTyped fires after preedit commits
        insert(event.codepointAsString());
        return true;
    }

    @Override
    public boolean preeditUpdated(@Nullable PreeditEvent event) {
        if (!focused) return false;
        if (preeditLength > 0) {
            int start = cursor - preeditLength;
            value = value.substring(0, start) + value.substring(cursor);
            cursor = start;
            preeditLength = 0;
        }
        String text = (event != null) ? event.fullText() : null;
        if (text != null && !text.isEmpty()) {
            insert(text);
            preeditLength = text.length();
        }
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!focused) return false;
        if (event.isSelectAll()) { cursor = value.length(); return true; }
        if (event.isCopy()) { minecraft.keyboardHandler.setClipboard(value); return true; }
        if (event.isPaste()) { insert(minecraft.keyboardHandler.getClipboard()); return true; }
        if (event.isCut()) { minecraft.keyboardHandler.setClipboard(value); value(""); return true; }

        return switch (event.key()) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { insert("\n"); yield true; }
            case GLFW.GLFW_KEY_BACKSPACE -> { deleteBefore(); yield true; }
            case GLFW.GLFW_KEY_DELETE -> { deleteAfter(); yield true; }
            case GLFW.GLFW_KEY_LEFT -> { cursor = Math.max(0, cursor - 1); yield true; }
            case GLFW.GLFW_KEY_RIGHT -> { cursor = Math.min(value.length(), cursor + 1); yield true; }
            case GLFW.GLFW_KEY_UP -> { moveVertical(-1); yield true; }
            case GLFW.GLFW_KEY_DOWN -> { moveVertical(1); yield true; }
            case GLFW.GLFW_KEY_HOME -> { cursor = lineStart(cursor); yield true; }
            case GLFW.GLFW_KEY_END -> { cursor = lineEnd(cursor); yield true; }
            default -> false;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        int max = Math.max(0, lines().size() - Math.max(1, (height - 10) / font.lineHeight));
        scrollLine = Math.max(0, Math.min(max, scrollLine + (vAmount < 0 ? 1 : -1)));
        return true;
    }

    private void insert(String text) {
        if (text == null || text.isEmpty()) return;
        int room = Math.max(0, maxLength - value.length());
        if (text.length() > room) text = text.substring(0, room);
        if (text.isEmpty()) return;
        value = value.substring(0, cursor) + text + value.substring(cursor);
        cursor += text.length();
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

    private void moveVertical(int delta) {
        List<String> lines = lines();
        int[] lc = lineColumnAt(cursor, lines);
        int line = Math.max(0, Math.min(lines.size() - 1, lc[0] + delta));
        cursor = indexAt(line, Math.min(lc[1], lines.get(line).length()), lines);
    }

    private int cursorAt(double mouseX, double mouseY) {
        List<String> lines = lines();
        int line = Math.max(0, Math.min(lines.size() - 1, scrollLine + (int) ((mouseY - (innerY() + 5)) / font.lineHeight)));
        int col = font.plainSubstrByWidth(lines.get(line), Math.max(0, (int) (mouseX - (innerX() + 5)))).length();
        return indexAt(line, col, lines);
    }

    private int[] lineColumnAt(int index, List<String> lines) {
        int pos = 0;
        for (int i = 0; i < lines.size(); i++) {
            int len = lines.get(i).length();
            if (index <= pos + len) return new int[]{i, index - pos};
            pos += len + 1;
        }
        return new int[]{Math.max(0, lines.size() - 1), lines.isEmpty() ? 0 : lines.getLast().length()};
    }

    private int indexAt(int line, int col, List<String> lines) {
        int pos = 0;
        for (int i = 0; i < line; i++) pos += lines.get(i).length() + 1;
        return Math.min(value.length(), pos + col);
    }

    private int lineStart(int index) { return value.lastIndexOf('\n', Math.max(0, index - 1)) + 1; }
    private int lineEnd(int index) { int end = value.indexOf('\n', index); return end < 0 ? value.length() : end; }

    private void ensureCursorVisible(List<String> lines, int innerHeight) {
        int cursorLine = lineColumnAt(cursor, lines)[0];
        int visibleLines = Math.max(1, innerHeight / font.lineHeight);
        if (cursorLine < scrollLine) scrollLine = cursorLine;
        if (cursorLine >= scrollLine + visibleLines) scrollLine = cursorLine - visibleLines + 1;
    }

    private boolean cursorVisible() {
        return ((Util.getMillis() - focusedTime) / 300L) % 2L == 0L;
    }

    private List<String> lines() {
        String[] split = value.split("\n", -1);
        List<String> lines = new ArrayList<>(split.length);
        Collections.addAll(lines, split);
        return lines.isEmpty() ? List.of("") : lines;
    }

    public TextAreaComponent value(String value) {
        this.value = value == null ? "" : value.substring(0, Math.min(value.length(), maxLength));
        this.cursor = Math.min(cursor, this.value.length());
        onChanged.accept(this.value);
        return this;
    }

    public String value() { return value; }
    public TextAreaComponent placeholder(Component placeholder) { this.placeholder = placeholder; return this; }
    public TextAreaComponent maxLength(int maxLength) { this.maxLength = Math.max(0, maxLength); return value(value); }
    public TextAreaComponent onChanged(Consumer<String> onChanged) { this.onChanged = onChanged != null ? onChanged : ignored -> {}; return this; }
}
