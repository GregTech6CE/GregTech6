package gregtech.content.material;

import com.google.gson.*;
import gregtech.api.material.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.*;
import gregtech.engine.prefix.PrefixRegistry;
import gregtech.platform.Mc;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Loads the versioned vanilla item-composition table into {@link ItemMaterialRegistry}. */
public final class VanillaCompositionLoader {
    private static final String TABLE_PATH = "/data/gt6ce/materials/vanilla_compositions.json";
    private static final JsonObject TABLE = loadTable();

    private VanillaCompositionLoader() {}

    public static void register() {
        var errors = new ArrayList<String>();
        for (var entry : TABLE.entrySet()) {
            var item = Mc.item(ResourceLocation.withDefaultNamespace(entry.getKey()));
            if (item == null || item == Items.AIR) continue; // Model variants are not registered items.
            var row = entry.getValue().getAsJsonObject();
            var rowErrors = validateRow(entry.getKey(), row);
            if (!rowErrors.isEmpty()) {
                errors.addAll(rowErrors);
                continue;
            }
            var components = new ArrayList<MaterialComponent>();
            for (var part : row.getAsJsonObject("components").entrySet()) {
                var material = GTMaterialRegistry.get(part.getKey());
                components.add(MaterialComponent.of(material, part.getValue().getAsLong()));
            }
            var prefix = prefix(entry.getKey(), row, errors);
            String source = row.has("source") ? row.get("source").getAsString() : "GT6CE vanilla item data";
            ItemMaterialRegistry.register(
                    item,
                    new ItemComposition(
                            prefix,
                            components,
                            components.get(0).material(),
                            source,
                            row.has("bulkRecovery")
                                    ? row.get("bulkRecovery").getAsBoolean()
                                    : row.has("recoverable")
                                            && row.get("recoverable").getAsBoolean()),
                    ItemCompositionSource.Kind.VANILLA_JSON,
                    declarationId(entry.getKey()),
                    "vanilla_compositions.json",
                    null,
                    inheritsVanillaUnification(item)
                            ? ItemCompositionSource.MaterialProcessingAuthority.ALLOW
                            : ItemCompositionSource.MaterialProcessingAuthority.DENY);
        }
        if (!errors.isEmpty()) throw new IllegalStateException("Unresolved vanilla materials: " + errors);
    }

    /** Resolves the JSON declaration independently of whether the JSON producer has run yet. */
    public static Optional<String> declarationId(Item item) {
        ResourceLocation id = Mc.itemId(item);
        if (id == null || !id.getNamespace().equals("minecraft") || !TABLE.has(id.getPath())) return Optional.empty();
        return Optional.of(declarationId(id.getPath()));
    }

    /** Whether this JSON declaration explicitly inherits the vanilla-unification processing policy. */
    public static boolean inheritsVanillaUnification(Item item) {
        ResourceLocation id = Mc.itemId(item);
        if (id == null || !id.getNamespace().equals("minecraft") || !TABLE.has(id.getPath())) return false;
        var row = TABLE.getAsJsonObject(id.getPath());
        return row.has("inherit")
                && "vanilla-unification".equals(row.get("inherit").getAsString());
    }

    private static String declarationId(String itemPath) {
        return "gt6ce:vanilla-compositions/" + itemPath;
    }

    static List<String> validateRow(String itemPath, JsonObject row) {
        var errors = new ArrayList<String>();
        if (!row.has("components") || !row.get("components").isJsonObject()) {
            errors.add(itemPath + ": missing components object");
            return errors;
        }
        var components = row.getAsJsonObject("components");
        if (components.size() == 0) errors.add(itemPath + ": components must not be empty");
        for (var part : components.entrySet()) {
            if (!GTMaterialRegistry.get(part.getKey()).isValid())
                errors.add(itemPath + ": unknown material " + part.getKey());
            try {
                if (part.getValue().getAsLong() <= 0) errors.add(itemPath + ": component amount must be positive");
            } catch (RuntimeException failure) {
                errors.add(itemPath + ": component amount is not an integer");
            }
        }
        boolean legacyRecovery =
                row.has("recoverable") && row.get("recoverable").getAsBoolean();
        boolean bulkRecovery =
                row.has("bulkRecovery") && row.get("bulkRecovery").getAsBoolean();
        if (row.has("recoverable") && row.has("bulkRecovery") && legacyRecovery != bulkRecovery)
            errors.add(itemPath + ": recoverable and bulkRecovery disagree");
        prefix(itemPath, row, errors);
        return errors;
    }

    private static MaterialPrefix prefix(String itemPath, JsonObject row, List<String> errors) {
        if (row.has("prefix") && row.has("inherit")) {
            errors.add(itemPath + ": prefix and inherit are mutually exclusive");
            return null;
        }
        if (row.has("prefix")) {
            String name = row.get("prefix").getAsString();
            MaterialPrefix prefix = PrefixRegistry.byName(name);
            if (prefix == null) errors.add(itemPath + ": unknown prefix " + name);
            return prefix;
        }
        if (!row.has("inherit")) return null;
        if (!"vanilla-unification".equals(row.get("inherit").getAsString())) {
            errors.add(itemPath + ": unknown inheritance target "
                    + row.get("inherit").getAsString());
            return null;
        }
        var inherited = VanillaUnificationDefinitions.entries().stream()
                .filter(candidate -> candidate.itemPath().equals(itemPath))
                .findFirst();
        if (inherited.isEmpty()) {
            errors.add(itemPath + ": no vanilla-unification declaration to inherit");
            return null;
        }
        return inherited.map(VanillaUnificationDefinitions.Entry::prefix).orElse(null);
    }

    private static JsonObject loadTable() {
        try (var stream = VanillaCompositionLoader.class.getResourceAsStream(TABLE_PATH)) {
            if (stream == null) throw new IllegalStateException("Missing vanilla compositions");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
