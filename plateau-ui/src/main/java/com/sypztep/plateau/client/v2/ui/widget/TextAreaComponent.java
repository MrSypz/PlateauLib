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

/**
 * Multi-line text input with a vanilla-style cursor/selection model.
 * <p>
 * Mirrors {@code net.minecraft.client.gui.components.MultilineTextField} (the model backing
 * {@code MultiLineEditBox}): a {@link #cursor} and a {@link #highlight} anchor define the
 * selection range. Word boundaries use {@link Character#isWhitespace(char)} like
 * {@code MultilineTextField#getPreviousWord()}/{@code getNextWord()}, and the selection is
 * painted per visible line the same way {@code MultiLineEditBox#extractContents} does.
 *
 * @see net.minecraft.client.gui.components.MultilineTextField
 * @see net.minecraft.client.gui.components.MultiLineEditBox
 */
@Environment(EnvType.CLIENT)
public class TextAreaComponent extends BaseComponent<TextAreaComponent> {
    private String value = "";
    private Component placeholder = Component.empty();
    /** Caret position. Together with {@link #highlight} this defines the selection range. */
    private int cursor = 0;
    /** Selection anchor. Equal to {@link #cursor} when there is no selection. */
    private int highlight = 0;
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

            if (hasSelection()) drawSelectionHighlight(g, lines, ix, iy, maxLines);

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

    private void drawSelectionHighlight(GuiGraphicsExtractor g, List<String> lines, int ix, int iy, int maxLines) {
        int selStart = selectionStart();
        int selEnd = selectionEnd();
        for (int i = 0; i < maxLines && scrollLine + i < lines.size(); i++) {
            int line = scrollLine + i;
            String text = lines.get(line);
            int lineBegin = indexAt(line, 0, lines);
            int lineEndIdx = lineBegin + text.length();
            if (selStart > lineEndIdx || selEnd < lineBegin) continue;

            int from = Math.max(selStart, lineBegin) - lineBegin;
            int to = Math.min(selEnd, lineEndIdx) - lineBegin;
            int hx1 = ix + font.width(text.substring(0, from));
            int hx2 = ix + font.width(text.substring(0, to));
            int hy = iy + i * font.lineHeight;
            g.textHighlight(hx1, hy, hx2, hy + font.lineHeight, true);
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y()) || event.button() != 0) return false;
        if (doubleClick) {
            selectWordAt(event.x(), event.y());
        } else {
            cursor = cursorAt(event.x(), event.y());
            if (!event.hasShiftDown()) highlight = cursor;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (!focused || !isMouseOver(event.x(), event.y())) return false;
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
            highlight = cursor;
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
        if (event.isSelectAll()) { highlight = 0; cursor = value.length(); return true; }
        if (event.isCopy()) { minecraft.keyboardHandler.setClipboard(selectedText()); return true; }
        if (event.isPaste()) { insert(minecraft.keyboardHandler.getClipboard()); return true; }
        if (event.isCut()) { minecraft.keyboardHandler.setClipboard(selectedText()); insert(""); return true; }

        boolean shift = event.hasShiftDown();
        return switch (event.key()) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> { insert("\n"); yield true; }
            case GLFW.GLFW_KEY_BACKSPACE -> { deleteBefore(event.hasControlDownWithQuirk()); yield true; }
            case GLFW.GLFW_KEY_DELETE -> { deleteAfter(event.hasControlDownWithQuirk()); yield true; }
            case GLFW.GLFW_KEY_LEFT -> {
                int to = event.hasControlDownWithQuirk() ? wordPosition(-1, cursor) : Math.max(0, cursor - 1);
                moveCursor(to, shift);
                yield true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                int to = event.hasControlDownWithQuirk() ? wordPosition(1, cursor) : Math.min(value.length(), cursor + 1);
                moveCursor(to, shift);
                yield true;
            }
            case GLFW.GLFW_KEY_UP -> { moveVertical(-1, shift); yield true; }
            case GLFW.GLFW_KEY_DOWN -> { moveVertical(1, shift); yield true; }
            case GLFW.GLFW_KEY_HOME -> { moveCursor(lineStart(cursor), shift); yield true; }
            case GLFW.GLFW_KEY_END -> { moveCursor(lineEnd(cursor), shift); yield true; }
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

    /** Moves the caret to {@code pos}; when {@code extendSelection} is false the anchor follows it (collapsing any selection). */
    private void moveCursor(int pos, boolean extendSelection) {
        cursor = Math.max(0, Math.min(value.length(), pos));
        if (!extendSelection) highlight = cursor;
    }

    private boolean hasSelection() { return cursor != highlight; }
    private int selectionStart() { return Math.min(cursor, highlight); }
    private int selectionEnd() { return Math.max(cursor, highlight); }
    private String selectedText() { return value.substring(selectionStart(), selectionEnd()); }

    /** Inserts {@code text} in place of the current selection (or at the caret if there is none). */
    private void insert(String text) {
        if (text == null) text = "";
        int start = selectionStart();
        int end = selectionEnd();
        int room = Math.max(0, maxLength - (value.length() - (end - start)));
        String filtered = text.length() > room ? text.substring(0, room) : text;
        if (filtered.isEmpty() && start == end) return;
        value = value.substring(0, start) + filtered + value.substring(end);
        cursor = start + filtered.length();
        highlight = cursor;
        onChanged.accept(value);
    }

    private void deleteBefore(boolean wholeWord) {
        if (hasSelection()) { insert(""); return; }
        int to = wholeWord ? wordPosition(-1, cursor) : Math.max(0, cursor - 1);
        if (to == cursor) return;
        value = value.substring(0, to) + value.substring(cursor);
        cursor = to;
        highlight = cursor;
        onChanged.accept(value);
    }

    private void deleteAfter(boolean wholeWord) {
        if (hasSelection()) { insert(""); return; }
        int to = wholeWord ? wordPosition(1, cursor) : Math.min(value.length(), cursor + 1);
        if (to == cursor) return;
        value = value.substring(0, cursor) + value.substring(to);
        onChanged.accept(value);
    }

    private void moveVertical(int delta, boolean extendSelection) {
        List<String> lines = lines();
        int[] lc = lineColumnAt(cursor, lines);
        int line = Math.max(0, Math.min(lines.size() - 1, lc[0] + delta));
        int pos = indexAt(line, Math.min(lc[1], lines.get(line).length()), lines);
        moveCursor(pos, extendSelection);
    }

    private int cursorAt(double mouseX, double mouseY) {
        List<String> lines = lines();
        int line = Math.max(0, Math.min(lines.size() - 1, scrollLine + (int) ((mouseY - (innerY() + 5)) / font.lineHeight)));
        int col = font.plainSubstrByWidth(lines.get(line), Math.max(0, (int) (mouseX - (innerX() + 5)))).length();
        return indexAt(line, col, lines);
    }

    /** Selects the word under the mouse, matching {@code MultilineTextField#selectWordAtCursor()}. */
    private void selectWordAt(double mouseX, double mouseY) {
        int pos = cursorAt(mouseX, mouseY);
        int[] bounds = wordBoundsAt(pos);
        highlight = bounds[0];
        cursor = bounds[1];
    }

    /** Word start/end around {@code pos}, mirroring {@code MultilineTextField#getPreviousWord()}. */
    private int[] wordBoundsAt(int pos) {
        if (value.isEmpty()) return new int[]{0, 0};
        int start = Math.max(0, Math.min(pos, value.length() - 1));
        while (start > 0 && Character.isWhitespace(value.charAt(start - 1))) start--;
        while (start > 0 && !Character.isWhitespace(value.charAt(start - 1))) start--;
        int end = start;
        while (end < value.length() && !Character.isWhitespace(value.charAt(end))) end++;
        return new int[]{start, end};
    }

    /**
     * Finds the start of the previous (dir &lt; 0) or next (dir &gt; 0) word relative to {@code from}.
     * Mirrors {@code MultilineTextField#getPreviousWord()}/{@code getNextWord()}.
     */
    private int wordPosition(int dir, int from) {
        int length = value.length();
        if (dir < 0) {
            int result = from;
            while (result > 0 && Character.isWhitespace(value.charAt(result - 1))) result--;
            while (result > 0 && !Character.isWhitespace(value.charAt(result - 1))) result--;
            return result;
        } else {
            int result = from;
            while (result < length && !Character.isWhitespace(value.charAt(result))) result++;
            while (result < length && Character.isWhitespace(value.charAt(result))) result++;
            return result;
        }
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
        this.highlight = cursor;
        onChanged.accept(this.value);
        return this;
    }

    public String value() { return value; }
    public TextAreaComponent placeholder(Component placeholder) { this.placeholder = placeholder; return this; }
    public TextAreaComponent maxLength(int maxLength) { this.maxLength = Math.max(0, maxLength); return value(value); }
    public TextAreaComponent onChanged(Consumer<String> onChanged) { this.onChanged = onChanged != null ? onChanged : ignored -> {}; return this; }
}
