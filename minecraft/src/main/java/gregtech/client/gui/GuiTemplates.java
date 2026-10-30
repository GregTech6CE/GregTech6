package gregtech.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Namespaced, reloadable GUI compositions. No registry or server initialization depends on this class. */
public final class GuiTemplates {
    private static final Map<ResourceLocation, Optional<GuiTemplate>> TEMPLATES = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> CATALOGS = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> ALIASES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> RESOLVED = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, GuiSprite> SPRITES = new ConcurrentHashMap<>();

    private GuiTemplates() {}

    public static void reload() {
        TEMPLATES.clear();
        CATALOGS.clear();
        ALIASES.clear();
        RESOLVED.clear();
        SPRITES.clear();
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.parse(name.contains(":") ? name : "gt6ce:" + name);
    }

    public static GuiSprite sprite(String name) {
        return SPRITES.computeIfAbsent(id(name), key -> {
            var catalog = CATALOGS.computeIfAbsent(key.getNamespace(), namespace -> {
                try {
                    return read(ResourceLocation.fromNamespaceAndPath(namespace, "gui/sprites.json"));
                } catch (IOException error) {
                    throw new IllegalArgumentException("Cannot load GUI sprite catalog " + namespace, error);
                }
            });
            if (!catalog.has(key.getPath())) throw new IllegalArgumentException("Unknown GUI sprite " + key);
            var entry = catalog.getAsJsonObject(key.getPath());
            return new GuiSprite(
                    id(entry.get("texture").getAsString()),
                    dimension(entry, "width", 16),
                    dimension(entry, "height", 16));
        });
    }

    /** Converts a legacy sheet path into a template ID, also usable without a corresponding PNG. */
    public static ResourceLocation forTexture(ResourceLocation texture) {
        String path = texture.getPath();
        if (path.startsWith("textures/gui/") && path.endsWith(".png"))
            path = path.substring("textures/gui/".length(), path.length() - 4);
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), path);
    }

    public static Optional<GuiTemplate> find(ResourceLocation template) {
        var cached = TEMPLATES.get(template);
        if (cached != null) return cached;
        Optional<GuiTemplate> result;
        try {
            var canonical = resolve(template);
            cached = TEMPLATES.get(canonical);
            if (cached != null) {
                TEMPLATES.put(template, cached);
                return cached;
            }
            var path = location(canonical);
            result = Minecraft.getInstance()
                            .getResourceManager()
                            .getResource(path)
                            .isEmpty()
                    ? Optional.empty()
                    : Optional.of(load(canonical, new HashSet<>()));
            TEMPLATES.put(canonical, result);
        } catch (IOException | RuntimeException error) {
            LogUtils.getLogger().warn("Invalid GUI template {}; using legacy background", template, error);
            result = Optional.empty();
        }
        TEMPLATES.put(template, result);
        return result;
    }

    public static void background(GuiGraphics graphics, ResourceLocation texture, int x, int y, int w, int h) {
        background(graphics, texture, x, y, w, h, java.util.List.of());
    }

    public static void background(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int w,
            int h,
            java.util.List<net.minecraft.world.inventory.Slot> slots) {
        var template = find(forTexture(texture));
        RenderSystem.enableBlend();
        graphics.setColor(1, 1, 1, 1);
        if (template.isPresent()) template.get().draw(graphics, x, y, slots);
        else graphics.blit(legacyTexture(texture), x, y, 0, 0, w, h);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        graphics.setColor(1, 1, 1, 1);
        sprite("base/background").panel(graphics, x, y, w, h);
    }

    private static GuiTemplate load(ResourceLocation key, Set<ResourceLocation> parents) throws IOException {
        key = resolve(key);
        if (parents.size() >= 16 || !parents.add(key)) throw new IllegalArgumentException("GUI parent cycle: " + key);
        var json = read(location(key));
        if (number(json, "format", 1) != 1) throw new IllegalArgumentException("Unsupported GUI format: " + key);
        GuiTemplate parent = json.has("parent") ? load(id(json.get("parent").getAsString()), parents) : null;
        var elements = new ArrayList<GuiTemplate.Element>();
        if (parent != null) elements.addAll(parent.elements());
        long total = elements.stream()
                .mapToLong(element -> (long) element.columns() * element.rows())
                .sum();
        if (json.has("elements"))
            for (var raw : json.getAsJsonArray("elements")) {
                var element = raw.getAsJsonObject();
                var sprite = sprite(element.get("sprite").getAsString());
                int columns = dimension(element, "columns", 1), rows = dimension(element, "rows", 1);
                total += (long) columns * rows;
                if (total > 4096) throw new IllegalArgumentException("Too many GUI elements");
                elements.add(new GuiTemplate.Element(
                        sprite,
                        number(element, "x", 0),
                        number(element, "y", 0),
                        columns,
                        rows,
                        number(element, "step_x", sprite.width()),
                        number(element, "step_y", sprite.height())));
            }
        var progress = parent == null ? null : parent.progress();
        if (json.has("progress")) {
            if (json.get("progress").isJsonNull()) progress = null;
            else {
                var value = json.getAsJsonObject("progress");
                var sprite = sprite(value.get("sprite").getAsString());
                if (sprite.width() % 2 != 0)
                    throw new IllegalArgumentException("Progress sprite must have equal halves");
                progress = new GuiTemplate.Progress(sprite, number(value, "x", 78), number(value, "y", 24));
            }
        }
        var background = json.has("background")
                ? sprite(json.get("background").getAsString())
                : parent == null ? sprite("base/background") : parent.background();
        if (background.width() <= 8 || background.height() <= 8)
            throw new IllegalArgumentException("Panel must exceed border size");
        int width = dimension(json, "width", parent == null ? 176 : parent.width());
        int height = dimension(json, "height", parent == null ? 166 : parent.height());
        if (width < 8 || height < 8) throw new IllegalArgumentException("GUI panel is too small");
        return new GuiTemplate(
                width,
                height,
                background,
                java.util.List.copyOf(elements),
                progress,
                json.has("brand") ? json.get("brand").getAsBoolean() : parent != null && parent.brand(),
                json.has("menu_slots") ? json.get("menu_slots").getAsBoolean() : parent != null && parent.menuSlots());
    }

    private static int number(JsonObject json, String key, int fallback) {
        int value = json.has(key) ? json.get(key).getAsInt() : fallback;
        if (value < -4096 || value > 4096) throw new IllegalArgumentException("GUI coordinate out of bounds: " + key);
        return value;
    }

    private static int dimension(JsonObject json, String key, int fallback) {
        int value = number(json, key, fallback);
        if (value <= 0) throw new IllegalArgumentException("GUI dimension must be positive: " + key);
        return value;
    }

    private static ResourceLocation location(ResourceLocation key) {
        return ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "gui/templates/" + key.getPath() + ".json");
    }

    /** Explicit legacy templates in addon/resource packs still override the shared definition. */
    private static ResourceLocation resolve(ResourceLocation original) throws IOException {
        var cached = RESOLVED.get(original);
        if (cached != null) return cached;
        var key = original;
        Set<ResourceLocation> visited = new HashSet<>();
        var resources = Minecraft.getInstance().getResourceManager();
        while (visited.size() < 16 && visited.add(key)) {
            if (resources.getResource(location(key)).isPresent()) {
                RESOLVED.put(original, key);
                return key;
            }
            var next = alias(key);
            if (next == null) {
                RESOLVED.put(original, key);
                return key;
            }
            key = next;
        }
        throw new IllegalArgumentException("GUI alias cycle/depth: " + original);
    }

    private static ResourceLocation alias(ResourceLocation key) throws IOException {
        var aliases = ALIASES.get(key.getNamespace());
        if (aliases == null) {
            var path = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "gui/template_aliases.json");
            aliases = Minecraft.getInstance()
                            .getResourceManager()
                            .getResource(path)
                            .isPresent()
                    ? read(path)
                    : new JsonObject();
            ALIASES.put(key.getNamespace(), aliases);
        }
        if (aliases.has(key.getPath())) return id(aliases.get(key.getPath()).getAsString());
        if (key.getPath().startsWith("recipes/"))
            return ResourceLocation.fromNamespaceAndPath(
                    key.getNamespace(), "machines/" + key.getPath().substring(8));
        return null;
    }

    /** Duplicate recipe sheets use the retained machine sheet only when a template cannot be rendered. */
    public static ResourceLocation legacyTexture(ResourceLocation texture) {
        var resources = Minecraft.getInstance().getResourceManager();
        if (resources.getResource(texture).isPresent()) return texture;
        try {
            var key = forTexture(texture);
            Set<ResourceLocation> visited = new HashSet<>();
            while (visited.size() < 16 && visited.add(key)) {
                key = alias(key);
                if (key == null) break;
                var candidate = ResourceLocation.fromNamespaceAndPath(
                        key.getNamespace(), "textures/gui/" + key.getPath() + ".png");
                if (resources.getResource(candidate).isPresent()) return candidate;
            }
        } catch (IOException | RuntimeException error) {
            LogUtils.getLogger().warn("Cannot resolve legacy GUI sheet {}", texture, error);
        }
        return texture;
    }

    private static JsonObject read(ResourceLocation path) throws IOException {
        var resource = Minecraft.getInstance()
                .getResourceManager()
                .getResource(path)
                .orElseThrow(() -> new IOException("Missing GUI resource " + path));
        try (var reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
