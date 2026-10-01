package com.gregtech.gregtech.content.book;

import com.google.gson.JsonParser;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** GT6's 300 plank-index shelves and 60 metalset shelves, limited to planks obtainable in this port. */
public final class BookShelfVariants {
    public record Variant(String path, int originalId, String kind, String materialName,
                          String displayMaterialName, String texture, float hardness,
                          float resistance, int flammability) {
        public boolean metal() { return kind.equals("metal"); }

        public GTMaterial material() {
            GTMaterial material = GTMaterialRegistry.get(materialName);
            if (!material.isValid()) throw new IllegalStateException("Unknown bookshelf material: " + materialName);
            return material;
        }

        public MutableComponent displayName() {
            if (path.equals("bookshelf")) return Component.translatable("block.gregtech.bookshelf");
            return Component.translatable("block.gregtech.bookshelf_variant",
                    Component.translatable("material.gregtech."
                            + displayMaterialName.toLowerCase(java.util.Locale.ROOT)));
        }
    }

    private static final List<Variant> ALL = load();
    private static final Map<String, Variant> BY_PATH;
    private static final Map<Integer, Variant> BY_ORIGINAL_ID;

    static {
        Map<String, Variant> byPath = new HashMap<>();
        Map<Integer, Variant> byOriginalId = new HashMap<>();
        for (Variant variant : ALL) {
            if (byPath.putIfAbsent(variant.path(), variant) != null
                    || byOriginalId.putIfAbsent(variant.originalId(), variant) != null)
                throw new IllegalStateException("Duplicate bookshelf variant " + variant.path());
        }
        BY_PATH = Collections.unmodifiableMap(byPath);
        BY_ORIGINAL_ID = Collections.unmodifiableMap(byOriginalId);
    }

    private BookShelfVariants() {}

    public static List<Variant> all() { return ALL; }
    public static Variant defaultVariant() { return BY_PATH.get("bookshelf"); }
    public static Variant byPath(String path) { return BY_PATH.get(path); }
    public static Variant byOriginalId(int id) { return BY_ORIGINAL_ID.get(id); }

    private static List<Variant> load() {
        try (InputStream stream = BookShelfVariants.class.getClassLoader()
                .getResourceAsStream("data/gregtech/bookshelf_variants.json")) {
            if (stream == null) throw new IllegalStateException("Missing bookshelf_variants.json");
            var array = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("variants");
            List<Variant> variants = new ArrayList<>();
            for (var entry : array) {
                var value = entry.getAsJsonObject();
                variants.add(new Variant(value.get("path").getAsString(), value.get("gt6_id").getAsInt(),
                        value.get("kind").getAsString(), value.get("material").getAsString(),
                        value.get("display").getAsString(), value.get("texture").getAsString(),
                        value.get("hardness").getAsFloat(), value.get("resistance").getAsFloat(),
                        value.get("flammability").getAsInt()));
            }
            if (variants.isEmpty()) throw new IllegalStateException("No GT6 bookshelf variants");
            return List.copyOf(variants);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to load GT6 bookshelf variants", exception);
        }
    }
}
