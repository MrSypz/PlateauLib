package com.sypztep.plateau.client.v2.ui.screen;

import com.sypztep.plateau.client.v1.ui.screen.Tab;
import com.sypztep.plateau.client.v1.ui.screen.TabContext;
import com.sypztep.plateau.client.v2.ui.core.BaseComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * A v2 tab that replaces the imperative {@code buildWidgets(TabContext)} with a declarative
 * component tree. Implement {@link #build(TabContext)} and return a root {@link BaseComponent}
 * (typically a {@code FlowLayout}) that describes the tab's full content.
 *
 * <p>The framework mounts the root to fill the content area automatically. No manual
 * x/y/width/height arithmetic needed.
 *
 * <pre>{@code
 * class MyTab extends Tab2 {
 *     MyTab(Identifier id) { super(id, Component.literal("My Tab")); }
 *
 *     @Override
 *     protected BaseComponent<?> build(TabContext ctx) {
 *         return Containers.vertical(Sizing.fill(), Sizing.fill())
 *             .padding(Insets.of(8))
 *             .gap(6)
 *             .child(Components.label("Hello World"))
 *             .child(Components.button("Click me", button -> {}));
 *     }
 * }
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public abstract class Tab2 extends Tab {

    protected Tab2(Identifier id, Component label, @Nullable Identifier icon) {
        super(id, label, icon);
    }

    protected Tab2(Identifier id, Component label) {
        super(id, label);
    }

    /**
     * Build and return the component tree for this tab's content area.
     * Called once each time the tab is activated. The root component will be
     * automatically mounted to fill the available screen space below the nav bar.
     */
    protected abstract BaseComponent<?> build(TabContext ctx);

    @Override
    protected final void buildWidgets(TabContext ctx) {
        BaseComponent<?> root = build(ctx);
        int contentY = ctx.contentStartY();
        int screenW  = ctx.screenWidth();
        int screenH  = ctx.screenHeight();
        root.mount(0, contentY, screenW, screenH - contentY);
        addWidget(root);
    }
}
