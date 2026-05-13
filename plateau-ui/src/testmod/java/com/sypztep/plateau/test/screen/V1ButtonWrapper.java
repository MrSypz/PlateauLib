package com.sypztep.plateau.test.screen;

import com.sypztep.plateau.client.v1.ui.widget.UIButton;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import com.sypztep.plateau.client.v2.ui.core.Sizing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

/**
 * Wraps a v1 UIButton as a v2 BaseComponent<?> so the test tab can show both side-by-side.
 */
@Environment(EnvType.CLIENT)
public class V1ButtonWrapper extends BaseComponent<V1ButtonWrapper> {

    private final UIButton inner;

    public V1ButtonWrapper(String label, Consumer<UIButton> onClick) {
        this.inner = new UIButton(0, 0, 0, 0, Component.literal(label), onClick);
        this.horizontalSizing = Sizing.fill();
        this.verticalSizing   = Sizing.fixed(22);
    }

    public V1ButtonWrapper(String label) {
        this(label, null);
    }

    public V1ButtonWrapper enabled(boolean enabled) {
        inner.setEnabled(enabled);
        return this;
    }

    @Override
    protected void onMounted() {
        inner.setBounds(x, y, width, height);
    }

    @Override
    public void extract(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        inner.setBounds(x, y, width, height);
        inner.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        return inner.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        return inner.mouseReleased(event);
    }

    @Override
    public void updateNarration(@NonNull NarrationElementOutput output) {}

    @Override
    public int determineHorizontalContentSize(int space) { return space; }
    @Override
    public int determineVerticalContentSize(int space)   { return 22; }
}
