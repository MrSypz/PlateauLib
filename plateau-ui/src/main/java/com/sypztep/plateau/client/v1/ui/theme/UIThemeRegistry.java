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
        PlateauUIClient.LOGGER.info("[UITheme] Reload started");
        PlateauUIClient.LOGGER.info("[UITheme] Step 1/5: Scanning assets/*/{}/ for theme json files", DIRECTORY);

        Map<Identifier, UITheme> next = new LinkedHashMap<>();

        resourceManager.listResources(DIRECTORY, path -> path.getPath().endsWith(".json"))
                .forEach((resourceId, resource) -> {
                    PlateauUIClient.LOGGER.info("[UITheme]   Found resource: {}", resourceId);

                    try (Reader reader = resource.openAsReader()) {
                        PlateauUIClient.LOGGER.info("[UITheme]   Reading json: {}", resourceId);

                        JsonElement json = JsonParser.parseReader(reader);
                        Identifier themeId = themeIdFromResource(resourceId);

                        PlateauUIClient.LOGGER.info("[UITheme]   Decoding theme id: {} -> {}", resourceId, themeId);

                        UITheme theme = UITheme.CODEC
                                .parse(JsonOps.INSTANCE, json)
                                .getOrThrow(error -> new IllegalArgumentException(
                                        "Invalid UI theme " + resourceId + ": " + error
                                ));

                        next.put(themeId, theme);

                        PlateauUIClient.LOGGER.info("[UITheme]   Loaded theme: {}", themeId);
                    } catch (Exception e) {
                        PlateauUIClient.LOGGER.error(
                                "[UITheme]   Failed to load theme resource: {}",
                                resourceId,
                                e
                        );
                    }
                });

        PlateauUIClient.LOGGER.info("[UITheme] Step 2/5: Validating required default theme: {}", DARK_ID);

        UITheme darkTheme = next.get(DARK_ID);
        if (darkTheme == null) {
            PlateauUIClient.LOGGER.error("[UITheme] Reload failed");
            PlateauUIClient.LOGGER.error("[UITheme]   Missing required default theme id: {}", DARK_ID);
            PlateauUIClient.LOGGER.error(
                    "[UITheme]   Expected file path: assets/{}/{}/dark.json",
                    DARK_ID.getNamespace(),
                    DIRECTORY
            );
            PlateauUIClient.LOGGER.error("[UITheme]   Loaded theme ids: {}", next.keySet());

            throw new IllegalStateException(
                    "[UITheme] Missing required default theme: assets/"
                            + DARK_ID.getNamespace()
                            + "/"
                            + DIRECTORY
                            + "/dark.json"
            );
        }

        PlateauUIClient.LOGGER.info("[UITheme] Step 3/5: Applying loaded theme map");
        PlateauUIClient.LOGGER.info("[UITheme]   Previous theme count: {}", themes.size());
        PlateauUIClient.LOGGER.info("[UITheme]   New theme count: {}", next.size());

        themes.clear();
        themes.putAll(next);

        PlateauUIClient.LOGGER.info("[UITheme] Step 4/5: Resolving selected theme");
        PlateauUIClient.LOGGER.info("[UITheme]   Requested selected theme: {}", selectedTheme);

        current = themes.getOrDefault(selectedTheme, darkTheme);

        if (themes.containsKey(selectedTheme)) {
            PlateauUIClient.LOGGER.info("[UITheme]   Selected theme applied: {}", selectedTheme);
        } else {
            PlateauUIClient.LOGGER.warn(
                    "[UITheme]   Selected theme {} was not found; falling back to default theme {}",
                    selectedTheme,
                    DARK_ID
            );
        }

        PlateauUIClient.LOGGER.info("[UITheme] Step 5/5: Reload complete");
        PlateauUIClient.LOGGER.info("[UITheme]   Available themes: {}", themes.keySet());
        PlateauUIClient.LOGGER.info("[UITheme]   Active theme: {}", themes.containsKey(selectedTheme) ? selectedTheme : DARK_ID);
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