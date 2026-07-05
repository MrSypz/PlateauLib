package com.sypztep.plateau.client.v2.ui.widget;

import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Single-line text input with a vanilla-style cursor/selection model.
 * <p>
 * Mirrors {@code net.minecraft.client.gui.components.EditBox}: a {@link #cursor} and a
 * {@link #highlight} anchor define the selection range (equal = no selection). Ctrl+A,
 * shift-click, shift/ctrl-arrow, double-click word select, and drag-to-select all move one
 * of the two independently, exactly like {@code EditBox#moveCursorTo(int, boolean)}.
 *
 * @see net.minecraft.client.gui.components.EditBox
 * @see net.minecraft.client.gui.components.AbstractStringWidget
 */
@Environment(EnvType.CLIENT)
public class StringComponent extends BaseComponent<StringComponent> {
    private String value = "";
    private Component placeholder = Component.empty();
    /** Caret position. Together with {@link #highlight} this defines the selection range. */
    private int cursor = 0;
    /** Selection anchor. Equal to {@link #cursor} when there is no selection. */
    private int highlight = 0;
    private int display = 0;
    private int maxLength = 256;
    private int preeditLength = 0;
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

        if (hasSelection()) {
            int relStart = relativeOffset(selectionStart(), shown.length());
            int relEnd = relativeOffset(selectionEnd(), shown.length());
            int hlStartX = tx + font.width(shown.substring(0, relStart));
            int hlEndX = tx + font.width(shown.substring(0, relEnd));
            g.textHighlight(hlStartX, ty - 1, hlEndX, ty + font.lineHeight + 1, true);
        }

        if (focused && editable && cursorVisible()) {
            int cursorX = tx + font.width(value.substring(display, Math.min(cursor, display + shown.length())));
            g.fill(cursorX, ty - 1, cursorX + 1, ty + font.lineHeight + 1, theme.text().accent());
        }
        g.disableScissor();
    }

    private int relativeOffset(int absolute, int shownLength) {
        return Math.max(0, Math.min(shownLength, absolute - display));
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y()) || event.button() != 0) return false;
        if (doubleClick) {
            selectWordAt(event.x());
        } else {
            cursor = cursorAt(event.x());
            if (!event.hasShiftDown()) highlight = cursor;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (!focused || !isMouseOver(event.x(), event.y())) return false;
        cursor = cursorAt(event.x());
        return true;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (!focused || !editable || !event.isAllowedChatCharacter()) return false;
        if (preeditLength > 0) return false; // IME is composing; charTyped fires after preedit commits
        insert(event.codepointAsString());
        return true;
    }

    @Override
    public boolean preeditUpdated(@Nullable PreeditEvent event) {
        if (!focused || !editable) return false;
        // Remove the previous preedit text from the value
        if (preeditLength > 0) {
            int start = cursor - preeditLength;
            value = value.substring(0, start) + value.substring(cursor);
            cursor = start;
            highlight = cursor;
            preeditLength = 0;
        }
        // Insert the new composition text
        String text = (event != null) ? event.fullText() : null;
        if (text != null && !text.isEmpty()) {
            insert(text);
            preeditLength = text.length();
        }
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!focused || !editable) return false;

        if (event.isSelectAll()) {
            highlight = 0;
            cursor = value.length();
            return true;
        }
        if (event.isCopy()) {
            minecraft.keyboardHandler.setClipboard(selectedText());
            return true;
        }
        if (event.isPaste()) {
            insert(minecraft.keyboardHandler.getClipboard());
            return true;
        }
        if (event.isCut()) {
            minecraft.keyboardHandler.setClipboard(selectedText());
            insert("");
            return true;
        }

        return switch (event.key()) {
            case GLFW.GLFW_KEY_BACKSPACE -> { deleteBefore(event.hasControlDownWithQuirk()); yield true; }
            case GLFW.GLFW_KEY_DELETE -> { deleteAfter(event.hasControlDownWithQuirk()); yield true; }
            case GLFW.GLFW_KEY_LEFT -> {
                int to = event.hasControlDownWithQuirk() ? wordPosition(-1, cursor) : Math.max(0, cursor - 1);
                moveCursor(to, event.hasShiftDown());
                yield true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                int to = event.hasControlDownWithQuirk() ? wordPosition(1, cursor) : Math.min(value.length(), cursor + 1);
                moveCursor(to, event.hasShiftDown());
                yield true;
            }
            case GLFW.GLFW_KEY_HOME -> { moveCursor(0, event.hasShiftDown()); yield true; }
            case GLFW.GLFW_KEY_END -> { moveCursor(value.length(), event.hasShiftDown()); yield true; }
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

    /** Moves the caret to {@code pos}; when {@code extendSelection} is false the anchor follows it (collapsing any selection). */
    private void moveCursor(int pos, boolean extendSelection) {
        cursor = Math.max(0, Math.min(value.length(), pos));
        if (!extendSelection) highlight = cursor;
    }

    private boolean hasSelection() { return cursor != highlight; }
    private int selectionStart() { return Math.min(cursor, highlight); }
    private int selectionEnd() { return Math.max(cursor, highlight); }
    private String selectedText() { return value.substring(selectionStart(), selectionEnd()); }

    /** Selects the word under the mouse, matching {@code EditBox#selectWord(MouseButtonEvent)}. */
    private void selectWordAt(double mouseX) {
        int pos = cursorAt(mouseX);
        highlight = wordPosition(-1, pos);
        cursor = wordPosition(1, pos);
    }

    /**
     * Finds the start of the previous (dir &lt; 0) or next (dir &gt; 0) word relative to {@code from}.
     * Mirrors {@code EditBox#getWordPosition(int, int, boolean)}.
     */
    private int wordPosition(int dir, int from) {
        if (dir < 0) {
            int result = from;
            while (result > 0 && value.charAt(result - 1) == ' ') result--;
            while (result > 0 && value.charAt(result - 1) != ' ') result--;
            return result;
        } else {
            int length = value.length();
            int result = value.indexOf(' ', from);
            if (result == -1) return length;
            while (result < length && value.charAt(result) == ' ') result++;
            return result;
        }
    }

    /** Inserts {@code text} in place of the current selection (or at the caret if there is none). */
    private void insert(String text) {
        if (text == null) text = "";
        String filtered = text.isEmpty() ? "" : text.replace("\n", "").replace("\r", "");
        int start = selectionStart();
        int end = selectionEnd();
        int room = Math.max(0, maxLength - (value.length() - (end - start)));
        if (filtered.length() > room) filtered = filtered.substring(0, room);
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

    public StringComponent value(String value) {
        this.value = value == null ? "" : value.substring(0, Math.min(value.length(), maxLength));
        this.cursor = Math.min(cursor, this.value.length());
        this.highlight = cursor;
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
