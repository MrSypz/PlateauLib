package com.sypztep.plateau.client.v1.ui.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.sypztep.plateau.client.PlateauUIClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class UIThemeRegistry implements ResourceManagerReloadListener {
    public static final String DIRECTORY = "ui_themes";
    public static final Identifier DARK_ID = PlateauUIClient.id("dark");
    public static final UIThemeRegistry INSTANCE = new UIThemeRegistry();

    private final Map<Identifier, UITheme> themes = new ConcurrentHashMap<>();

    private Identifier selectedTheme = DARK_ID;
    private volatile UITheme current;

    private UIThemeRegistry() {
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<Identifier, UITheme> next = new LinkedHashMap<>();

        resourceManager.listResources(DIRECTORY, path -> path.getPath().endsWith(".json"))
                .forEach((resourceId, resource) -> {
                    try (Reader reader = resource.openAsReader()) {
                        JsonElement json = JsonParser.parseReader(reader);
                        Identifier themeId = themeIdFromResource(resourceId);
                        UITheme theme = UITheme.CODEC
                                .parse(JsonOps.INSTANCE, json)
                                .getOrThrow(error -> new IllegalArgumentException(
                                        "Invalid UI theme " + resourceId + ": " + error
                                ));
                        next.put(themeId, theme);
                    } catch (Exception e) {
                        PlateauUIClient.LOGGER.error("[UITheme] Failed to load {}", resourceId, e);
                    }
                });

        UITheme darkTheme = next.get(DARK_ID);
        if (darkTheme == null) {
            throw new IllegalStateException(
                    "[UITheme] Missing required default theme: assets/"
                            + DARK_ID.getNamespace() + "/" + DIRECTORY + "/dark.json"
            );
        }

        themes.clear();
        themes.putAll(next);

        if (!themes.containsKey(selectedTheme)) {
            PlateauUIClient.LOGGER.warn("[UITheme] Theme {} not found, falling back to {}", selectedTheme, DARK_ID);
        }

        current = themes.getOrDefault(selectedTheme, darkTheme);
    }

    public UITheme current() {
        UITheme theme = current;
        if (theme == null) {
            throw new IllegalStateException(
                    "[UITheme] Theme requested before client resources were loaded"
            );
        }
        return theme;
    }

    public UITheme get(Identifier id) {
        UITheme theme = themes.get(id);
        if (theme == null) {
            throw new IllegalArgumentException("[UITheme] Unknown theme: " + id);
        }
        return theme;
    }

    public void apply(Identifier id) {
        UITheme theme = get(id);
        selectedTheme = id;
        current = theme;
    }

    public Identifier selectedTheme() {
        return selectedTheme;
    }

    public Set<Identifier> availableThemes() {
        return Set.copyOf(themes.keySet());
    }

    private static Identifier themeIdFromResource(Identifier resourceId) {
        String path = resourceId.getPath();
        String relative = path.substring(DIRECTORY.length() + 1, path.length() - ".json".length());
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), relative);
    }
}