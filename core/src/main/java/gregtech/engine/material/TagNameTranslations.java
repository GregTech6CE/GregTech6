package gregtech.engine.material;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Compose generated tag names from the active language without retaining a name per material/form pair. */
public final class TagNameTranslations {
    private final Map<String, String> language;
    private final Map<String, String> materials = new HashMap<>();
    private final Map<String, String> forms = new HashMap<>();
    private final Map<String, String> terms = new HashMap<>();

    public TagNameTranslations(Map<String, String> language) {
        this.language = language;
        language.forEach((key, value) -> {
            if (key.startsWith("material.gt6ce.")) materials.put(normalize(key.substring(15)), value);
            if (key.startsWith("item.gt6ce.") || key.startsWith("block.gt6ce.")) {
                int start = key.startsWith("item.") ? 11 : 12;
                if (value.contains("%s") && value.indexOf('%') == value.lastIndexOf('%'))
                    forms.put(normalize(key.substring(start)), value);
            }
            for (String prefix :
                    new String[] {"itemGroup.gt6ce.", "emi.group.gt6ce.", "tool.gt6ce.", "gt6ce.tag.part."})
                if (key.startsWith(prefix)) terms.put(normalize(key.substring(prefix.length())), value);
        });
        // Explicit tag vocabulary takes precedence over creative-group and tool labels.
        language.forEach((key, value) -> {
            if (key.startsWith("gt6ce.tag.part.")) terms.put(normalize(key.substring(15)), value);
        });
    }

    public String resolve(String key) {
        if (!key.startsWith("tag.")) return null;
        String path = key.substring(4);
        if (path.startsWith("item.")) path = path.substring(5);
        else if (path.startsWith("fluid.")) path = path.substring(6);
        int dot = path.indexOf('.');
        if (dot < 0) return null;
        String namespace = path.substring(0, dot);
        if (!namespace.equals("gt6ce") && !namespace.equals("forge") && !namespace.equals("c")) return null;
        String exact = language.get("tag." + path);
        if (exact != null) return exact;
        path = path.substring(dot + 1);
        String[] parts = path.split("\\.");
        if (parts.length == 2) {
            String template = forms.get(normalize(alias(parts[0])));
            String material = material(parts[1]);
            if (template != null && material != null) return template.replace("%s", material);
        }
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            String translated = part(part);
            // Unknown addon/data-pack tags remain untranslated instead of masking missing entries.
            if (translated == null) return null;
            if (!result.isEmpty()) result.append(" · ");
            result.append(translated);
        }
        return result.toString();
    }

    private String material(String part) {
        return materials.get(normalize(alias(part)));
    }

    private String part(String part) {
        String normalized = normalize(part);
        String value = terms.get(normalized);
        if (value != null) return value;
        value = material(part);
        if (value != null) return value;
        String aliased = alias(part);
        value = forms.get(normalize(aliased));
        if (value != null) return value.replace("%s", "").trim().replaceAll(" +", " ");
        for (String prefix : new String[] {"item.gt6ce.", "block.gt6ce.", "fluid_type.gt6ce."}) {
            value = language.get(prefix + aliased);
            if (value != null && !value.contains("%")) return value;
        }
        if (part.startsWith("raw_")) {
            value = material(part.substring(4));
            String template = language.get("block.gt6ce.block_raw");
            if (value != null && template != null) return template.replace("%s", value);
        }
        if (part.chars().allMatch(Character::isDigit)) return part;
        return null;
    }

    private static String alias(String part) {
        return switch (part) {
            case "aluminum" -> "aluminium";
            case "sulphur" -> "sulfur";
            case "elementium" -> "elven_elementium";
            case "osmium" -> "osmium_elemental";
            case "bolts" -> "bolt";
            case "dusts" -> "dust";
            case "small_dusts" -> "dust_small";
            case "tiny_dusts" -> "dust_tiny";
            case "foils" -> "foil";
            case "gears" -> "gear_gt";
            case "gems" -> "gem";
            case "ingots" -> "ingot";
            case "nuggets" -> "nugget";
            case "plates" -> "plate";
            case "raw_materials" -> "ore_raw";
            case "rings" -> "ring";
            case "rods" -> "stick";
            case "long_rods" -> "stick_long";
            case "screws" -> "screw";
            case "ores" -> "ore";
            case "storage_blocks" -> "block_solid";
            case "casingSmall", "casingsmall" -> "item_casing";
            default -> part;
        };
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replace("_", "");
    }
}
