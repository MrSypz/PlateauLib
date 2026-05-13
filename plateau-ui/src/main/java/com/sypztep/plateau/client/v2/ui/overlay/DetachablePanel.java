package com.sypztep.plateau.client.v2.ui.overlay;

import com.sypztep.plateau.client.v1.ui.core.UISounds;
import com.sypztep.plateau.client.v1.ui.theme.UITheme;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Insets;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

/**
 * Titled panel that can leave its docked layout slot and render as a floating window.
 *
 * <p>The floating copy is rendered by {@link WindowLayer}. Keep this component somewhere
 * under a WindowLayer when using the detach behavior; otherwise it behaves like a normal
 * titled panel.</p>
 */
@Environment(EnvType.CLIENT)
public class DetachablePanel extends BaseComponent<DetachablePanel> {
    private static final int HEADER_HEIGHT = 20;
    private static final int BODY_PAD = 6;
    private static final int MIN_WINDOW_WIDTH = 160;
    private static final int MIN_WINDOW_HEIGHT = 90;
    private static final int DEFAULT_RESIZE_HANDLE_SIZE = 10;

    public enum WindowOpenTrigger {
        CLICK_AND_DRAG,
        ICON_ONLY,
        DRAG_ONLY
    }

    public enum WindowInputMode {
        NON_MODAL,
        MODAL,
        PINNED
    }

    private enum State {
        DOCKED,
        OPENING,
        FLOATING,
        CLOSING
    }

    @FunctionalInterface
    public interface WindowAnimator {
        Bounds animate(Bounds current, Bounds target, float delta, float speed);
    }

    public record Bounds(float x, float y, float width, float height) {
        private Bounds clamp(int minX, int minY, int maxWidth, int maxHeight) {
            float clampedWidth = Math.max(MIN_WINDOW_WIDTH, Math.min(width, maxWidth));
            float clampedHeight = Math.max(MIN_WINDOW_HEIGHT, Math.min(height, maxHeight));
            float clampedX = Math.max(minX, Math.min(x, minX + maxWidth - clampedWidth));
            float clampedY = Math.max(minY, Math.min(y, minY + maxHeight - clampedHeight));
            return new Bounds(clampedX, clampedY, clampedWidth, clampedHeight);
        }

        private boolean near(Bounds other) {
            return Math.abs(x - other.x) < 0.5f
                    && Math.abs(y - other.y) < 0.5f
                    && Math.abs(width - other.width) < 0.5f
                    && Math.abs(height - other.height) < 0.5f;
        }
    }

    private Component title;
    private @Nullable BaseComponent<?> content;
    private @Nullable BaseComponent<?> preview;
    private WindowOpenTrigger openTrigger = WindowOpenTrigger.CLICK_AND_DRAG;
    private WindowInputMode inputMode = WindowInputMode.NON_MODAL;
    private WindowAnimator animator = DetachablePanel::defaultAnimate;
    private State state = State.DOCKED;

    private int preferredWindowWidth = 320;
    private int preferredWindowHeight = 220;
    private float animationSpeed = 0.45f;
    private boolean resizable = true;
    private int resizeHandleSize = DEFAULT_RESIZE_HANDLE_SIZE;
    private Bounds animatedBounds = new Bounds(0, 0, 0, 0);
    private Bounds targetBounds = new Bounds(0, 0, 0, 0);
    private boolean dockHeaderPressed;
    private boolean dockChildPressed;
    private boolean windowDragging;
    private boolean windowResizing;
    private float dragOffsetX;
    private float dragOffsetY;
    private float resizeOffsetX;
    private float resizeOffsetY;

    public DetachablePanel(Component title) {
        this.title = title;
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing = Sizing.content();
        this.padding = Insets.none();
    }

    public DetachablePanel(String title) {
        this(Component.literal(title));
    }

    public DetachablePanel title(Component title) {
        this.title = title;
        return this;
    }

    public DetachablePanel title(String title) {
        return title(Component.literal(title));
    }

    public DetachablePanel content(@Nullable BaseComponent<?> content) {
        this.content = content;
        remountDockContent();
        return this;
    }

    public DetachablePanel preview(@Nullable BaseComponent<?> preview) {
        this.preview = preview;
        remountDockContent();
        return this;
    }

    public DetachablePanel windowSize(int width, int height) {
        this.preferredWindowWidth = Math.max(MIN_WINDOW_WIDTH, width);
        this.preferredWindowHeight = Math.max(MIN_WINDOW_HEIGHT, height);
        return this;
    }

    public DetachablePanel openTrigger(WindowOpenTrigger openTrigger) {
        this.openTrigger = openTrigger == null ? WindowOpenTrigger.CLICK_AND_DRAG : openTrigger;
        return this;
    }

    public DetachablePanel inputMode(WindowInputMode inputMode) {
        this.inputMode = inputMode == null ? WindowInputMode.NON_MODAL : inputMode;
        return this;
    }

    public DetachablePanel animationSpeed(float animationSpeed) {
        this.animationSpeed = Math.max(0f, animationSpeed);
        return this;
    }

    public DetachablePanel resizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    public DetachablePanel resizeHandleSize(int resizeHandleSize) {
        this.resizeHandleSize = Math.max(0, resizeHandleSize);
        return this;
    }

    public DetachablePanel animator(WindowAnimator animator) {
        this.animator = animator == null ? DetachablePanel::defaultAnimate : animator;
        return this;
    }

    @Override
    protected void onMounted() {
        remountDockContent();
    }

    @Override
    public int determineVerticalContentSize(int availableWidth) {
        BaseComponent<?> dockChild = dockChild();
        int bodyHeight = dockChild == null ? 0 : dockChild.determineVerticalContentSize(Math.max(0, availableWidth - BODY_PAD * 2));
        return HEADER_HEIGHT + bodyHeight + BODY_PAD * 2;
    }

    @Override
    public void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        UITheme theme = UITheme.current();
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, theme.panel().bg());
        graphics.outline(x, y, width, height, theme.panel().border());
        graphics.fill(x + 2, y + 2, x + width - 2, y + HEADER_HEIGHT, theme.panel().headerBg());

        if (canClickOpen()) {
            WindowControls.draw(graphics, WindowControls.Type.OPEN, openControlX(), openControlY(), isDockHeaderHovered(mouseX, mouseY));
        }

        graphics.text(font, title, WindowControls.titleX(x, canClickOpen() ? 1 : 0), y + 6, theme.text().accent(), true);
        graphics.fill(x + 2, y + HEADER_HEIGHT, x + width - 2, y + HEADER_HEIGHT + 1, theme.panel().border());

        if (isFloatingActive()) {
            return;
        }

        BaseComponent<?> dockChild = dockChild();
        if (dockChild != null) {
            dockChild.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    @Override
    protected boolean isFocusable() {
        return true;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (clickDockHeader(event.button(), event.x(), event.y())) return true;

        BaseComponent<?> dockChild = dockChild();
        if (!isFloatingActive() && dockChild != null && dockChild.mouseClicked(event, doubleClick)) {
            dockChildPressed = event.button() == 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (dockHeaderPressed && event.button() == 0 && openTrigger != WindowOpenTrigger.ICON_ONLY) {
            if (!isFloatingActive()) open();
            startWindowDrag(event.x(), event.y());
            dragWindow(event.x(), event.y());
            dockHeaderPressed = false;
            return true;
        }

        BaseComponent<?> dockChild = dockChild();
        return !isFloatingActive() && dockChildPressed && dockChild != null && dockChild.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        boolean wasDockChildPressed = dockChildPressed;
        if (event.button() == 0) {
            dockHeaderPressed = false;
            dockChildPressed = false;
            windowDragging = false;
        }

        BaseComponent<?> dockChild = dockChild();
        return !isFloatingActive() && dockChild != null && (wasDockChildPressed || dockChild.isMouseOver(event.x(), event.y())) && dockChild.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        BaseComponent<?> dockChild = dockChild();
        return !isFloatingActive()
                && dockChild != null
                && isMouseOver(mouseX, mouseY)
                && dockChild.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        BaseComponent<?> dockChild = dockChild();
        return !isFloatingActive() && dockChild != null && dockChild.keyPressed(event);
    }

    public void open() {
        if (isFloatingActive()) return;
        animatedBounds = dockBounds();
        targetBounds = preferredWindowBounds();
        state = State.OPENING;
    }

    public void close() {
        if (!isFloatingActive()) return;
        state = State.CLOSING;
        targetBounds = dockBounds();
        windowDragging = false;
        windowResizing = false;
    }

    public boolean isFloatingActive() {
        return state == State.OPENING || state == State.FLOATING || state == State.CLOSING;
    }

    public WindowInputMode inputMode() {
        return inputMode;
    }

    public void tickFloating(int hostX, int hostY, int hostWidth, int hostHeight, float delta) {
        if (!isFloatingActive()) return;

        Bounds target = state == State.CLOSING ? dockBounds() : targetBounds;
        target = target.clamp(hostX + 8, hostY + 8, Math.max(MIN_WINDOW_WIDTH, hostWidth - 16), Math.max(MIN_WINDOW_HEIGHT, hostHeight - 16));
        targetBounds = target;
        animatedBounds = animator.animate(animatedBounds, target, delta, animationSpeed);

        if (state == State.OPENING && animatedBounds.near(target)) {
            animatedBounds = target;
            state = State.FLOATING;
        } else if (state == State.CLOSING && animatedBounds.near(target)) {
            animatedBounds = target;
            state = State.DOCKED;
            remountDockContent();
        }
    }

    public boolean isFloatingMouseOver(double mouseX, double mouseY) {
        if (!isFloatingActive()) return false;
        return mouseX >= animatedBounds.x()
                && mouseX < animatedBounds.x() + animatedBounds.width()
                && mouseY >= animatedBounds.y()
                && mouseY < animatedBounds.y() + animatedBounds.height();
    }

    public void extractFloating(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!isFloatingActive()) return;

        int windowX = Math.round(animatedBounds.x());
        int windowY = Math.round(animatedBounds.y());
        int windowWidth = Math.round(animatedBounds.width());
        int windowHeight = Math.round(animatedBounds.height());

        UITheme theme = UITheme.current();
        graphics.fill(windowX + 2, windowY + 2, windowX + windowWidth, windowY + windowHeight, 0x66000000);
        graphics.fill(windowX + 1, windowY + 1, windowX + windowWidth - 1, windowY + windowHeight - 1, theme.panel().bg());
        graphics.outline(windowX, windowY, windowWidth, windowHeight, theme.panel().borderHover());
        graphics.outline(windowX + 1, windowY + 1, windowWidth - 2, windowHeight - 2, theme.panel().border());
        graphics.fill(windowX + 2, windowY + 2, windowX + windowWidth - 2, windowY + HEADER_HEIGHT, theme.panel().headerBg());
        graphics.text(font, title, WindowControls.titleX(windowX, 1), windowY + 6, theme.text().accent(), true);

        int closeX = closeX();
        int closeY = closeY();
        WindowControls.draw(graphics, WindowControls.Type.CLOSE, closeX, closeY, isCloseHovered(mouseX, mouseY));

        graphics.fill(windowX + 2, windowY + HEADER_HEIGHT, windowX + windowWidth - 2, windowY + HEADER_HEIGHT + 1, theme.panel().border());

        if (content != null) {
            mountFloatingContent(windowX, windowY, windowWidth, windowHeight);
            content.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        if (resizable && resizeHandleSize > 0) {
            int handleX = windowX + windowWidth - resizeHandleSize - 1;
            int handleY = windowY + windowHeight - resizeHandleSize - 1;
            int color = isResizeHovered(mouseX, mouseY) ? theme.panel().borderHover() : theme.panel().border();
            graphics.fill(handleX + resizeHandleSize - 2, handleY, handleX + resizeHandleSize, handleY + resizeHandleSize, color);
            graphics.fill(handleX, handleY + resizeHandleSize - 2, handleX + resizeHandleSize, handleY + resizeHandleSize, color);
        }
    }

    public boolean mouseClickedFloating(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (!isFloatingActive() || event.button() != 0 || !isFloatingMouseOver(event.x(), event.y())) return false;
        mountFloatingContent();

        if (isCloseHovered(event.x(), event.y())) {
            close();
            UISounds.play(SoundEvents.UI_LOOM_TAKE_RESULT, 1,1);
            return true;
        }

        if (isResizeHovered(event.x(), event.y())) {
            startWindowResize(event.x(), event.y());
            UISounds.play(SoundEvents.UI_LOOM_TAKE_RESULT, 1,1);
            return true;
        }

        if (isFloatingHeaderHovered(event.x(), event.y())) {
            startWindowDrag(event.x(), event.y());
            UISounds.playHover();
            return true;
        }

        return content != null && content.mouseClicked(event, doubleClick);
    }

    public boolean mouseReleasedFloating(@NonNull MouseButtonEvent event) {
        if (!isFloatingActive()) return false;
        mountFloatingContent();
        if (event.button() == 0) {
            windowDragging = false;
            windowResizing = false;
        }
        if (content != null) content.mouseReleased(event);
        return isFloatingMouseOver(event.x(), event.y());
    }

    public boolean mouseDraggedFloating(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (!isFloatingActive()) return false;
        mountFloatingContent();
        if (windowDragging && event.button() == 0) {
            dragWindow(event.x(), event.y());
            return true;
        }
        if (windowResizing && event.button() == 0) {
            resizeWindow(event.x(), event.y());
            return true;
        }
        return content != null && content.mouseDragged(event, dragX, dragY);
    }

    public boolean mouseScrolledFloating(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!isFloatingMouseOver(mouseX, mouseY)) return false;
        mountFloatingContent();
        return content != null && content.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    public boolean keyPressedFloating(@NonNull KeyEvent event) {
        if (!isFloatingActive()) return false;
        mountFloatingContent();
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return content != null && content.keyPressed(event);
    }

    private void remountDockContent() {
        BaseComponent<?> dockChild = dockChild();
        if (dockChild == null || width <= 0 || height <= HEADER_HEIGHT) return;
        dockChild.mount(x + BODY_PAD, y + HEADER_HEIGHT + BODY_PAD, Math.max(0, width - BODY_PAD * 2), Math.max(0, height - HEADER_HEIGHT - BODY_PAD * 2));
    }

    private void mountFloatingContent(int windowX, int windowY, int windowWidth, int windowHeight) {
        if (content == null) return;
        content.mount(windowX + BODY_PAD, windowY + HEADER_HEIGHT + BODY_PAD, Math.max(0, windowWidth - BODY_PAD * 2), Math.max(0, windowHeight - HEADER_HEIGHT - BODY_PAD * 2));
    }

    private void mountFloatingContent() {
        mountFloatingContent(
                Math.round(animatedBounds.x()),
                Math.round(animatedBounds.y()),
                Math.round(animatedBounds.width()),
                Math.round(animatedBounds.height())
        );
    }

    private BaseComponent<?> dockChild() {
        return preview != null ? preview : content;
    }

    private boolean canClickOpen() {
        return openTrigger != WindowOpenTrigger.DRAG_ONLY;
    }

    private boolean clickDockHeader(int button, double mouseX, double mouseY) {
        if (button != 0 || !isDockHeaderHovered(mouseX, mouseY)) return false;
        dockHeaderPressed = true;
        if (openTrigger != WindowOpenTrigger.DRAG_ONLY) {
            open();
        }
        UISounds.play(SoundEvents.UI_LOOM_SELECT_PATTERN, 1,1);
        return true;
    }

    private boolean isDockHeaderHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEADER_HEIGHT;
    }

    private boolean isFloatingHeaderHovered(double mouseX, double mouseY) {
        return mouseX >= animatedBounds.x()
                && mouseX < animatedBounds.x() + animatedBounds.width()
                && mouseY >= animatedBounds.y()
                && mouseY < animatedBounds.y() + HEADER_HEIGHT;
    }

    private boolean isCloseHovered(double mouseX, double mouseY) {
        return WindowControls.hit(mouseX, mouseY, closeX(), closeY());
    }

    private int closeX() {
        return WindowControls.x(Math.round(animatedBounds.x()), 0);
    }

    private int closeY() {
        return WindowControls.y(Math.round(animatedBounds.y()));
    }

    private int openControlX() {
        return WindowControls.x(x, 0);
    }

    private int openControlY() {
        return WindowControls.y(y);
    }

    private void startWindowDrag(double mouseX, double mouseY) {
        windowDragging = true;
        dragOffsetX = (float) (mouseX - animatedBounds.x());
        dragOffsetY = (float) (mouseY - animatedBounds.y());
    }

    private void dragWindow(double mouseX, double mouseY) {
        targetBounds = new Bounds((float) mouseX - dragOffsetX, (float) mouseY - dragOffsetY, targetBounds.width(), targetBounds.height());
        animatedBounds = targetBounds;
    }

    private boolean isResizeHovered(double mouseX, double mouseY) {
        if (!resizable || resizeHandleSize <= 0 || !isFloatingActive()) return false;
        return mouseX >= animatedBounds.x() + animatedBounds.width() - resizeHandleSize - 1
                && mouseX < animatedBounds.x() + animatedBounds.width()
                && mouseY >= animatedBounds.y() + animatedBounds.height() - resizeHandleSize - 1
                && mouseY < animatedBounds.y() + animatedBounds.height();
    }

    private void startWindowResize(double mouseX, double mouseY) {
        windowResizing = true;
        resizeOffsetX = (float) (animatedBounds.x() + animatedBounds.width() - mouseX);
        resizeOffsetY = (float) (animatedBounds.y() + animatedBounds.height() - mouseY);
    }

    private void resizeWindow(double mouseX, double mouseY) {
        float windowWidth = Math.max(MIN_WINDOW_WIDTH, (float) mouseX - animatedBounds.x() + resizeOffsetX);
        float windowHeight = Math.max(MIN_WINDOW_HEIGHT, (float) mouseY - animatedBounds.y() + resizeOffsetY);
        targetBounds = new Bounds(animatedBounds.x(), animatedBounds.y(), windowWidth, windowHeight);
        animatedBounds = targetBounds;
    }

    private Bounds dockBounds() {
        return new Bounds(x, y, Math.max(MIN_WINDOW_WIDTH, width), Math.max(MIN_WINDOW_HEIGHT, height));
    }

    private Bounds preferredWindowBounds() {
        int windowWidth = Math.max(preferredWindowWidth, width);
        int windowHeight = Math.max(preferredWindowHeight, height);
        int windowX = x + Math.max(0, (width - windowWidth) / 2);
        int windowY = Math.max(0, y - Math.max(24, (windowHeight - height) / 2));
        return new Bounds(windowX, windowY, windowWidth, windowHeight);
    }

    private static Bounds defaultAnimate(Bounds current, Bounds target, float delta, float speed) {
        float amount = Math.min(1f, Math.max(0f, speed) * Math.max(0f, delta));
        return new Bounds(
                Mth.lerp(amount, current.x(), target.x()),
                Mth.lerp(amount, current.y(), target.y()),
                Mth.lerp(amount, current.width(), target.width()),
                Mth.lerp(amount, current.height(), target.height())
        );
    }
}
