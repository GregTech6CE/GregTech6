package gregtech.engine.resource;

import gregtech.content.block.stone.StoneVariant;
import gregtech.content.energy.ChemicalBatterySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * Model and blockstate documents derived from a block family's parameters instead of shipped JSON.
 *
 * <p>Stones, decoration piles, logistics extenders and filters, reactor cores, chemical batteries,
 * compasses and scaffolds repeat one fixed geometry per family, so only their textures and the
 * hand-written templates they reference stay on disk. Every document is rendered from the family
 * tables below.
 *
 * <p>Model paths are pack paths without the {@code models/} prefix and the {@code .json} suffix;
 * blockstate keys are bare registry paths. Nothing here touches Minecraft types.
 */
public final class ProceduralModels {
    private static final String BLOCK_PARENT = "minecraft:block/block";
    private static final String ITEM_PARENT = "minecraft:item/generated";
    private static final String CUTOUT = "minecraft:cutout";

    /** The six block faces in the order vanilla serialises them. */
    private static final List<String> ALL_FACINGS = List.of("down", "up", "north", "south", "west", "east");

    private static final List<String> HORIZONTAL_FACINGS = List.of("north", "east", "south", "west");

    /** Facing order used where a definition table lists one model per direction. */
    private static final List<String> MODEL_ORDER = List.of("down", "east", "north", "south", "up", "west");

    private static final List<String> PILE_KINDS = List.of("ingot", "plate", "gem_plate");
    private static final int PILE_MAX = 64;

    private static final List<String> LOGISTICS_FAMILIES = List.of(
            "filter_prefix",
            "filter_slots",
            "inventory_extender",
            "inventory_tank_extender",
            "tank_extender",
            "universal_extender");

    /** Filter blockstates and the logistics family whose models they select. */
    private static final Map<String, String> FILTER_BLOCKSTATES = Map.of(
            "filter_items", "filter_slots",
            "filter_fluids", "filter_slots",
            "filter_items_fluids", "filter_slots",
            "filter_oredict", "filter_prefix");

    /** Extender blockstates, each addressed by its own family name. */
    private static final Set<String> EXTENDER_BLOCKSTATES =
            Set.of("inventory_extender", "inventory_tank_extender", "tank_extender", "universal_extender");

    private static final List<String> COMPASS_MODES = List.of("center", "death", "face", "north", "spawn");

    /** Needle sprite suffixes for compass frames 1 through 15. */
    private static final List<String> COMPASS_DIRECTIONS =
            List.of("nne", "ne", "nee", "e", "see", "se", "sse", "s", "ssw", "sw", "sww", "w", "nww", "nw", "nnw");

    /** Filter covers whose inventory icons switch to their blacklist overlay when {@code gt.cover.inverted} is set. */
    private static final Map<String, String> FILTER_COVER_FOLDERS = Map.of(
            "item_filter", "filteritem",
            "fluid_filter", "filterfluid",
            "item_retriever_cover", "retrieveritem");

    private static final int BATTERY_MAX_SCALE = 9;

    /** Reactor core sizes, each a separate registry block with its own top sprite. */
    private static final List<String> REACTOR_SIZES = List.of("1x1", "2x2");

    private static final String CASING_HULL = "gt6ce:block/machines/casing/hull";
    private static final String HAMMER_COLORED = "gt6ce:block/machines/casing/hammer_colored_side";
    private static final String TANK_COLORED = "gt6ce:block/machines/casing/tank_colored_in";
    private static final String BRIDGE_OVERLAY = "gt6ce:block/machines/shared_overlay/bridge_inv_overlay_side";
    private static final String FILTER_OVERLAY_OUT = "gt6ce:block/machines/shared_overlay/filter_prefix_overlay_out";

    private static final String REACTOR_PIPE = "gt6ce:block/machines/casing/kinetic_steam_colored_pipe";
    private static final String REACTOR_SIDE = "gt6ce:block/machines/casing/reactor_core_1x1_colored_side1";
    private static final String REACTOR_OVERLAY_FACE =
            "gt6ce:block/machines/shared_overlay/reactor_core_1x1_overlay_face1";
    private static final String REACTOR_OVERLAY_FACE_2 =
            "gt6ce:block/machines/shared_overlay/reactor_core_1x1_overlay_face2";
    private static final String REACTOR_OVERLAY_SIDE =
            "gt6ce:block/machines/shared_overlay/reactor_core_1x1_overlay_side1";

    private static final String SCAFFOLD_PLATE = "gt6ce:block/iconsets/plate";
    private static final String SCAFFOLD_HATCH = "gt6ce:block/iconsets/hatch";

    /** Solid block sprite that every scaffold design 1 model uses for its rods. */
    private static final String SCAFFOLD_ROD = "gt6ce:block/material_icons/dull/blocksolid";

    /** Scaffold design 1 ships for each of these materials plus the uncoloured {@code scaffold_1}. */
    private static final List<String> SCAFFOLD_MATERIALS = List.of(
            "adamantium",
            "aluminium",
            "antimony",
            "arsenic_bronze",
            "arsenic_copper",
            "bismuth",
            "bismuth_bronze",
            "brass",
            "bronze",
            "chromium",
            "cobalt",
            "constantan",
            "duranium",
            "efrine",
            "electrum",
            "elven_elementium",
            "germanium",
            "gold",
            "invar",
            "iridium",
            "knightmetal",
            "lead",
            "lumium",
            "manasteel",
            "manganese",
            "molybdenum",
            "netherite",
            "nickel",
            "osmium",
            "palladium",
            "platinum",
            "silver",
            "stainless_steel",
            "steel_galvanized",
            "terrasteel",
            "thaumium",
            "tin_alloy",
            "titanium",
            "tritanium",
            "tungsten",
            "tungsten_carbide",
            "tungstensteel",
            "ultimet");

    /** Every scaffold design 1 model path, derived from {@link #SCAFFOLD_MATERIALS}. */
    private static final Set<String> SCAFFOLD_PATHS = scaffoldPaths();

    private static final List<String> VERTICAL_FACES = List.of("down", "up");

    private static volatile @Nullable Set<String> cachedModelPaths;

    private static volatile @Nullable Set<String> cachedBlockstateStems;

    private ProceduralModels() {}

    /**
     * Emits every procedural model to {@code sink}, keyed by model path and encoded as UTF-8.
     *
     * <p>The path table is the single source of truth, so a path that has no renderer fails the build
     * instead of silently dropping a model. Identical documents share one byte array.
     */
    public static void emitModels(String compositeLoader, BiConsumer<String, byte[]> sink) {
        Map<String, byte[]> encoded = new HashMap<>();
        for (String path : modelPaths()) {
            String json = renderModelJson(path, compositeLoader);
            if (json == null) {
                throw new IllegalStateException("Procedural model has no renderer: " + path);
            }
            sink.accept(path, encoded.computeIfAbsent(json, text -> text.getBytes(StandardCharsets.UTF_8)));
        }
    }

    /**
     * Emits every procedural blockstate to {@code sink}, keyed by blockstate stem and encoded as UTF-8.
     *
     * <p>The stem table is the single source of truth, so a stem that has no renderer fails the build
     * instead of silently dropping a blockstate.
     */
    public static void emitBlockstates(BiConsumer<String, byte[]> sink) {
        for (String stem : blockstateStems()) {
            String json = renderBlockstateJson(stem);
            if (json == null) {
                throw new IllegalStateException("Procedural blockstate has no renderer: " + stem);
            }
            sink.accept(stem, json.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Reports whether {@code modelPath} is one of the procedural models. */
    public static boolean containsModel(String modelPath) {
        return modelPath != null && modelPaths().contains(modelPath);
    }

    /** Renders {@code modelPath}, or returns {@code null} when it is not a procedural model. */
    public static @Nullable String renderModelJson(String modelPath, String compositeLoader) {
        if (modelPath == null) return null;
        if (modelPath.startsWith("block/stones/") || modelPath.startsWith("item/stone_")) {
            return renderStoneModel(modelPath);
        }
        if (modelPath.startsWith("block/decoration/pile/")) return renderPileModel(modelPath);
        if (modelPath.startsWith("block/logistics/")) return renderLogisticsModel(modelPath, compositeLoader);
        if (modelPath.startsWith("block/machine/energy/reactor_core")) {
            return renderReactorCoreModel(modelPath, compositeLoader);
        }
        if (modelPath.startsWith("block/chemical_battery/")) return renderBatteryBlockModel(modelPath);
        if (modelPath.startsWith("item/battery_")) return renderBatteryItemModel(modelPath);
        if (modelPath.startsWith("item/compass_") || modelPath.startsWith("item/compass/")) {
            return renderCompassModel(modelPath);
        }
        String filterCover = renderFilterCoverModel(modelPath);
        if (filterCover != null) return filterCover;
        if (SCAFFOLD_PATHS.contains(modelPath)) return scaffoldDesign1Json(compositeLoader, SCAFFOLD_ROD);
        return null;
    }

    /** Reports whether {@code stem} is one of the procedural blockstates. */
    public static boolean containsBlockstate(String stem) {
        return stem != null && blockstateStems().contains(stem);
    }

    /** Renders the blockstate named {@code stem}, or returns {@code null} when it is not procedural. */
    public static @Nullable String renderBlockstateJson(String stem) {
        if (stem == null) return null;
        if (stem.startsWith("stone_")) return renderStoneBlockstate(stem);
        if (stem.endsWith("_pile")) return renderPileBlockstate(stem);
        if (stem.startsWith("battery_")) return renderBatteryBlockstate(stem);
        if (stem.startsWith("filter_")) return renderFilterBlockstate(stem);
        if (stem.startsWith("reactor_core")) return renderReactorCoreBlockstate(stem);
        if (EXTENDER_BLOCKSTATES.contains(stem)) return extenderBlockstateJson(stem);
        return null;
    }

    /** Every procedural model path, collected once from the tables that also drive rendering. */
    private static Set<String> modelPaths() {
        Set<String> current = cachedModelPaths;
        if (current != null) return current;
        synchronized (ProceduralModels.class) {
            current = cachedModelPaths;
            if (current == null) {
                Set<String> paths = new LinkedHashSet<>(4096);
                forEachStoneModelPath(paths::add);
                forEachPileModelPath(paths::add);
                forEachLogisticsModelPath(paths::add);
                forEachReactorCoreModelPath(paths::add);
                forEachBatteryModelPath(paths::add);
                forEachCompassModelPath(paths::add);
                forEachFilterCoverModelPath(paths::add);
                paths.addAll(SCAFFOLD_PATHS);
                current = Collections.unmodifiableSet(paths);
                cachedModelPaths = current;
            }
            return current;
        }
    }

    /** Every procedural blockstate stem, collected once from the tables that also drive rendering. */
    private static Set<String> blockstateStems() {
        Set<String> current = cachedBlockstateStems;
        if (current != null) return current;
        synchronized (ProceduralModels.class) {
            current = cachedBlockstateStems;
            if (current == null) {
                Set<String> stems = new LinkedHashSet<>(4096);
                Consumer<String> collector = stem -> {
                    if (!stems.add(stem)) {
                        throw new IllegalStateException("Duplicate procedural blockstate: " + stem);
                    }
                };
                forEachStoneBlockstateStem(collector);
                forEachPileBlockstateStem(collector);
                forEachFilterBlockstateStem(collector);
                forEachExtenderBlockstateStem(collector);
                forEachReactorCoreBlockstateStem(collector);
                forEachBatteryBlockstateStem(collector);
                current = Collections.unmodifiableSet(stems);
                cachedBlockstateStems = current;
            }
            return current;
        }
    }

    // ---------------------------------------------------------------- stones

    /** Stone registry ID and texture folder pair, decoupled from material bootstrap. */
    public record StoneSpec(String registryId, String textureFolder) {}

    private static final List<StoneSpec> STONE_SPECS = List.of(
            new StoneSpec("stone_granite_black", "gt.stone.granite.black"),
            new StoneSpec("stone_granite_red", "gt.stone.granite.red"),
            new StoneSpec("stone_basalt", "gt.stone.basalt"),
            new StoneSpec("stone_marble", "gt.stone.marble"),
            new StoneSpec("stone_limestone", "gt.stone.limestone"),
            new StoneSpec("stone_granite", "gt.stone.granite"),
            new StoneSpec("stone_diorite", "gt.stone.diorite"),
            new StoneSpec("stone_andesite", "gt.stone.andesite"),
            new StoneSpec("stone_komatiite", "gt.stone.komatiite"),
            new StoneSpec("stone_greenschist", "gt.stone.greenschist"),
            new StoneSpec("stone_blueschist", "gt.stone.blueschist"),
            new StoneSpec("stone_kimberlite", "gt.stone.kimberlite"),
            new StoneSpec("stone_quartzite", "gt.stone.quartzite"),
            new StoneSpec("stone_prismarine_light", "gt.stone.prismarine.light"),
            new StoneSpec("stone_prismarine_dark", "gt.stone.prismarine.dark"),
            new StoneSpec("stone_slate", "gt.stone.slate"),
            new StoneSpec("stone_shale", "gt.stone.shale"),
            new StoneSpec("stone_gypsum", "gt.stone.gypsum"),
            new StoneSpec("stone_salt", "gt.stone.salt"));

    public static List<StoneSpec> stoneSpecs() {
        return STONE_SPECS;
    }

    private static String stoneTexture(StoneSpec type, StoneVariant variant) {
        return "gt6ce:block/stones/" + type.textureFolder() + "/" + variant.textureName();
    }

    private static String stoneBlockJson(String texture) {
        return toJson(object("parent", "minecraft:block/cube_all", "textures", object("all", texture)));
    }

    private static String stoneSlabJson(String texture) {
        return toJson(object("parent", "gt6ce:block/stones/slab/down", "textures", object("all", texture)));
    }

    private static String stoneSlabSideJson(String texture) {
        return toJson(object("parent", "gt6ce:block/stones/slab/side", "textures", object("all", texture)));
    }

    private static String stoneItemJson(String base) {
        return toJson(object("parent", "gt6ce:block/stones/" + base));
    }

    private static String stoneSlabItemJson(String texture) {
        return toJson(object(
                "parent",
                "minecraft:block/slab",
                "textures",
                object("bottom", texture, "top", texture, "side", texture)));
    }

    private static void forEachStoneModelPath(Consumer<String> sink) {
        for (StoneSpec type : STONE_SPECS) {
            for (StoneVariant variant : StoneVariant.values()) {
                String base = type.registryId() + "_" + variant.registrySuffix();
                sink.accept("block/stones/" + base);
                sink.accept("block/stones/" + base + "_slab");
                sink.accept("block/stones/" + base + "_slab_side");
                sink.accept("item/" + base);
                sink.accept("item/" + base + "_slab");
            }
        }
    }

    private static @Nullable String renderStoneModel(String modelPath) {
        boolean itemModel = modelPath.startsWith("item/");
        String rest = modelPath.substring(itemModel ? "item/".length() : "block/stones/".length());
        for (StoneSpec type : STONE_SPECS) {
            String typePrefix = type.registryId() + "_";
            if (!rest.startsWith(typePrefix)) continue;
            String tail = rest.substring(typePrefix.length());
            for (StoneVariant variant : StoneVariant.values()) {
                String suffix = variant.registrySuffix();
                if (!tail.startsWith(suffix)) continue;
                String form = tail.substring(suffix.length());
                String texture = stoneTexture(type, variant);
                if (itemModel) {
                    if (form.isEmpty()) return stoneItemJson(type.registryId() + "_" + suffix);
                    if (form.equals("_slab")) return stoneSlabItemJson(texture);
                } else {
                    if (form.isEmpty()) return stoneBlockJson(texture);
                    if (form.equals("_slab")) return stoneSlabJson(texture);
                    if (form.equals("_slab_side")) return stoneSlabSideJson(texture);
                }
            }
        }
        return null;
    }

    private static void forEachStoneBlockstateStem(Consumer<String> sink) {
        for (StoneSpec type : STONE_SPECS) {
            for (StoneVariant variant : StoneVariant.values()) {
                String base = type.registryId() + "_" + variant.registrySuffix();
                sink.accept(base);
                sink.accept(base + "_slab");
            }
        }
    }

    private static String stoneBlockstateJson(String base) {
        return toJson(object("variants", object("", object("model", "gt6ce:block/stones/" + base))));
    }

    private static String stoneSlabBlockstateJson(String base) {
        String slab = "gt6ce:block/stones/" + base + "_slab";
        String side = "gt6ce:block/stones/" + base + "_slab_side";
        return toJson(object(
                "variants",
                object(
                        "facing=down", object("model", slab),
                        "facing=up", object("model", slab, "x", 180),
                        "facing=north", object("model", side),
                        "facing=south", object("model", side, "y", 180),
                        "facing=east", object("model", side, "y", 90),
                        "facing=west", object("model", side, "y", 270))));
    }

    private static @Nullable String renderStoneBlockstate(String stem) {
        for (StoneSpec type : STONE_SPECS) {
            String typePrefix = type.registryId() + "_";
            if (!stem.startsWith(typePrefix)) continue;
            String tail = stem.substring(typePrefix.length());
            for (StoneVariant variant : StoneVariant.values()) {
                String suffix = variant.registrySuffix();
                if (!tail.startsWith(suffix)) continue;
                String form = tail.substring(suffix.length());
                String base = type.registryId() + "_" + suffix;
                if (form.isEmpty()) return stoneBlockstateJson(base);
                if (form.equals("_slab")) return stoneSlabBlockstateJson(base);
            }
        }
        return null;
    }

    // ----------------------------------------------------------------- piles

    /** Sprite folder of a pile family; gem plates keep the combined {@code plategem} name. */
    private static String pileTextureFolder(String kind) {
        return kind.equals("gem_plate") ? "plategem" : kind;
    }

    private static String pileJson(String kind, int count) {
        String folder = "gt6ce:block/machines/placeables/" + pileTextureFolder(kind) + "/";
        List<Object> elements = new ArrayList<>(count == 0 ? 1 : count);
        if (count == 0) {
            elements.add(pileElement(0, 0, 0, 16, 1, 16));
        } else if (kind.equals("ingot")) {
            addIngotElements(elements, count);
        } else {
            addPlateElements(elements, count);
        }
        Map<String, Object> textures =
                object("particle", folder + "top", "surface_0", folder + "top", "surface_1", folder + "sides");
        return toJson(object("parent", BLOCK_PARENT, "textures", textures, "elements", elements));
    }

    /** Ingots stack eight per layer, alternating between four rows and four columns. */
    private static void addIngotElements(List<Object> elements, int count) {
        for (int index = 0; index < count; index++) {
            int layer = index / 8;
            int slot = index % 8;
            int y0 = layer * 2;
            if (layer % 2 == 0) {
                int column = slot % 4;
                int row = slot / 4;
                elements.add(pileElement(0.5 + column * 4, y0, 0.5 + row * 8, 3.5 + column * 4, y0 + 2, 7.5 + row * 8));
            } else {
                int column = slot / 4;
                int row = slot % 4;
                elements.add(pileElement(0.5 + column * 8, y0, 0.5 + row * 4, 7.5 + column * 8, y0 + 2, 3.5 + row * 4));
            }
        }
    }

    /** Plates fill four quadrants in turn, each stack growing one pixel per full round. */
    private static void addPlateElements(List<Object> elements, int count) {
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            int stacked = (count + 3 - quadrant) / 4;
            if (stacked <= 0) continue;
            double x0 = quadrant < 2 ? 0.5 : 8.5;
            double z0 = quadrant % 2 == 0 ? 0.5 : 8.5;
            elements.add(pileElement(x0, 0, z0, x0 + 7, stacked, z0 + 7));
        }
    }

    private static Map<String, Object> pileElement(double x0, double y0, double z0, double x1, double y1, double z1) {
        Map<String, Object> faces = object(
                "down", object("texture", "#surface_0", "tintindex", 0),
                "east", object("texture", "#surface_1", "tintindex", 0),
                "north", object("texture", "#surface_1", "tintindex", 0),
                "south", object("texture", "#surface_1", "tintindex", 0),
                "up", object("texture", "#surface_0", "tintindex", 0),
                "west", object("texture", "#surface_1", "tintindex", 0));
        return object(
                "faces", faces,
                "from", array(number(x0), number(y0), number(z0)),
                "to", array(number(x1), number(y1), number(z1)));
    }

    private static void forEachPileModelPath(Consumer<String> sink) {
        for (String kind : PILE_KINDS) {
            for (int count = 0; count <= PILE_MAX; count++) {
                sink.accept("block/decoration/pile/" + kind + "_" + count);
            }
        }
    }

    private static @Nullable String renderPileModel(String modelPath) {
        String rest = modelPath.substring("block/decoration/pile/".length());
        int separator = rest.lastIndexOf('_');
        if (separator < 0) return null;
        String kind = rest.substring(0, separator);
        if (!PILE_KINDS.contains(kind)) return null;
        int count = parseIndex(rest.substring(separator + 1), PILE_MAX);
        return count < 0 ? null : pileJson(kind, count);
    }

    private static void forEachPileBlockstateStem(Consumer<String> sink) {
        sink.accept("ingot_pile");
        sink.accept("plate_pile");
        sink.accept("plate_gem_pile");
    }

    private static String pileBlockstateJson(String kind) {
        Map<String, Object> variants = new LinkedHashMap<>();
        for (int count = 0; count <= PILE_MAX; count++) {
            variants.put("stack=" + count, object("model", "gt6ce:block/decoration/pile/" + kind + "_" + count));
        }
        return toJson(object("variants", variants));
    }

    private static @Nullable String renderPileBlockstate(String stem) {
        String kind = switch (stem) {
            case "ingot_pile" -> "ingot";
            case "plate_pile" -> "plate";
            case "plate_gem_pile" -> "gem_plate";
            default -> null;
        };
        return kind == null ? null : pileBlockstateJson(kind);
    }

    // ------------------------------------------------------------- logistics

    private static String logisticsColoredIn(String family) {
        return family.equals("tank_extender") ? TANK_COLORED : HAMMER_COLORED;
    }

    private static String logisticsOverlayIn(String family) {
        return switch (family) {
            case "filter_prefix", "filter_slots", "inventory_extender" -> BRIDGE_OVERLAY;
            case "inventory_tank_extender" -> "gt6ce:block/machines/extenders/inv_tank/overlay/in";
            case "tank_extender" -> "gt6ce:block/machines/extenders/tank/overlay/in";
            case "universal_extender" -> "gt6ce:block/machines/extenders/universal/overlay/in";
            default -> throw new IllegalArgumentException("Unknown logistics family: " + family);
        };
    }

    private static String logisticsOverlayOut(String family) {
        return switch (family) {
            case "filter_prefix", "filter_slots", "inventory_extender" -> FILTER_OVERLAY_OUT;
            case "inventory_tank_extender" -> "gt6ce:block/machines/extenders/inv_tank/overlay/out";
            case "tank_extender" -> "gt6ce:block/machines/extenders/tank/overlay/out";
            case "universal_extender" -> "gt6ce:block/machines/extenders/universal/overlay/out";
            default -> throw new IllegalArgumentException("Unknown logistics family: " + family);
        };
    }

    /** Overlay side sprite of a family, or {@code null} when its overlay draws only the two connections. */
    private static @Nullable String logisticsOverlaySide(String family) {
        return switch (family) {
            case "filter_prefix" -> "gt6ce:block/machines/extenders/filter_prefix/overlay/side";
            case "filter_slots" -> "gt6ce:block/machines/extenders/filter_slots/overlay/side";
            case "universal_extender" -> "gt6ce:block/machines/extenders/universal/overlay/side";
            case "inventory_extender", "inventory_tank_extender", "tank_extender" -> null;
            default -> throw new IllegalArgumentException("Unknown logistics family: " + family);
        };
    }

    /** Reports whether a family's overlay confines itself to the two connected faces. */
    private static boolean logisticsOverlayTwoFace(String family) {
        return switch (family) {
            case "inventory_extender", "inventory_tank_extender", "tank_extender" -> true;
            case "filter_prefix", "filter_slots", "universal_extender" -> false;
            default -> throw new IllegalArgumentException("Unknown logistics family: " + family);
        };
    }

    private static String logisticsJson(String compositeLoader, String family, String facing, String secondary) {
        boolean singleFace = facing.equals(secondary);
        String coloredIn = logisticsColoredIn(family);
        Map<String, Object> coloredTextures = new LinkedHashMap<>();
        coloredTextures.put("in", coloredIn);
        if (!singleFace) coloredTextures.put("out", coloredIn);
        coloredTextures.put("side", CASING_HULL);

        Map<String, Object> overlayTextures = new LinkedHashMap<>();
        overlayTextures.put("in", logisticsOverlayIn(family));
        if (!singleFace) overlayTextures.put("out", logisticsOverlayOut(family));
        String overlaySide = logisticsOverlaySide(family);
        if (overlaySide != null) overlayTextures.put("side", overlaySide);

        boolean twoFace = logisticsOverlayTwoFace(family);
        Map<String, Object> colored = object(
                "textures", coloredTextures,
                "elements", List.of(logisticsElement(facing, secondary, false, true)),
                "render_type", CUTOUT);
        Map<String, Object> overlay = object(
                "textures", overlayTextures,
                "elements", List.of(logisticsElement(facing, secondary, twoFace, false)),
                "render_type", CUTOUT);
        return toJson(object(
                "parent", BLOCK_PARENT,
                "loader", compositeLoader,
                "render_type", CUTOUT,
                "textures", object("particle", CASING_HULL),
                "children", object("colored", colored, "overlay", overlay)));
    }

    /**
     * One full-cube element whose {@code facing} side takes {@code #in} and whose {@code secondary}
     * side takes {@code #out}; every other side falls back to {@code #side}.
     */
    private static Map<String, Object> logisticsElement(
            String facing, String secondary, boolean twoFace, boolean tinted) {
        Map<String, Object> faces = new LinkedHashMap<>();
        for (String direction : ALL_FACINGS) {
            if (twoFace && !direction.equals(facing) && !direction.equals(secondary)) continue;
            String texture = direction.equals(facing) ? "#in" : direction.equals(secondary) ? "#out" : "#side";
            Map<String, Object> face = object("texture", texture, "cullface", direction);
            if (tinted) face.put("tintindex", 0);
            faces.put(direction, face);
        }
        return object("from", array(0, 0, 0), "to", array(16, 16, 16), "faces", faces);
    }

    private static void forEachLogisticsModelPath(Consumer<String> sink) {
        for (String family : LOGISTICS_FAMILIES) {
            for (String facing : ALL_FACINGS) {
                for (String secondary : ALL_FACINGS) {
                    sink.accept("block/logistics/" + family + "/" + facing + "_" + secondary);
                }
            }
        }
    }

    private static @Nullable String renderLogisticsModel(String modelPath, String compositeLoader) {
        String rest = modelPath.substring("block/logistics/".length());
        int separator = rest.indexOf('/');
        if (separator < 0) return null;
        String family = rest.substring(0, separator);
        if (!LOGISTICS_FAMILIES.contains(family)) return null;
        String faces = rest.substring(separator + 1);
        int underscore = faces.indexOf('_');
        if (underscore < 0) return null;
        String facing = faces.substring(0, underscore);
        String secondary = faces.substring(underscore + 1);
        if (!ALL_FACINGS.contains(facing) || !ALL_FACINGS.contains(secondary)) return null;
        return logisticsJson(compositeLoader, family, facing, secondary);
    }

    private static void forEachFilterBlockstateStem(Consumer<String> sink) {
        FILTER_BLOCKSTATES.keySet().forEach(sink);
    }

    /**
     * Filter blockstates carry a {@code secondary} property plus a {@code secondary_set} flag; while
     * the flag is clear the filter shows its default connection, the opposite face.
     */
    private static String filterBlockstateJson(String family) {
        Map<String, Object> variants = new LinkedHashMap<>();
        for (String facing : ALL_FACINGS) {
            for (String secondary : ALL_FACINGS) {
                variants.put(
                        "facing=" + facing + ",secondary=" + secondary + ",secondary_set=false",
                        object("model", "gt6ce:block/logistics/" + family + "/" + facing + "_" + opposite(facing)));
                variants.put(
                        "facing=" + facing + ",secondary=" + secondary + ",secondary_set=true",
                        object("model", "gt6ce:block/logistics/" + family + "/" + facing + "_" + secondary));
            }
        }
        return toJson(object("variants", variants));
    }

    private static @Nullable String renderFilterBlockstate(String stem) {
        String family = FILTER_BLOCKSTATES.get(stem);
        return family == null ? null : filterBlockstateJson(family);
    }

    private static void forEachExtenderBlockstateStem(Consumer<String> sink) {
        EXTENDER_BLOCKSTATES.forEach(sink);
    }

    private static String extenderBlockstateJson(String family) {
        Map<String, Object> variants = new LinkedHashMap<>();
        for (String facing : ALL_FACINGS) {
            for (String secondary : ALL_FACINGS) {
                variants.put(
                        "facing=" + facing + ",secondary=" + secondary,
                        object("model", "gt6ce:block/logistics/" + family + "/" + facing + "_" + secondary));
            }
        }
        return toJson(object("variants", variants));
    }

    // --------------------------------------------------------- reactor cores

    /** Reactor core model path; the default hot and cold outlet pair keeps the unsuffixed name. */
    private static String reactorCorePath(String size, String hot, String cold) {
        String prefix = "block/machine/energy/reactor_core" + (size.equals("2x2") ? "_2x2" : "");
        return hot.equals("down") && cold.equals("down") ? prefix : prefix + "_" + hot + "_" + cold;
    }

    private static String reactorColoredTexture(String size, String hot, String cold, String direction) {
        if (direction.equals(hot) || direction.equals(cold) || direction.equals("down")) return REACTOR_PIPE;
        if (direction.equals("up")) {
            return "gt6ce:block/machines/generators/reactor_core_" + size + "/colored/top";
        }
        return REACTOR_SIDE;
    }

    private static String reactorOverlayTexture(String size, String hot, String cold, String direction) {
        if (direction.equals(hot)) return REACTOR_OVERLAY_FACE;
        if (direction.equals(cold)) return REACTOR_OVERLAY_FACE_2;
        if (direction.equals("up")) {
            return "gt6ce:block/machines/generators/reactor_core_" + size + "/overlay/top";
        }
        return REACTOR_OVERLAY_SIDE;
    }

    private static String reactorCoreJson(String compositeLoader, String size, String hot, String cold) {
        boolean includeDown = hot.equals("down") || cold.equals("down");
        Map<String, Object> coloredTextures = new LinkedHashMap<>();
        Map<String, Object> overlayTextures = new LinkedHashMap<>();
        for (String direction : ALL_FACINGS) {
            coloredTextures.put(direction, reactorColoredTexture(size, hot, cold, direction));
            if (direction.equals("down") && !includeDown) continue;
            overlayTextures.put(direction, reactorOverlayTexture(size, hot, cold, direction));
        }
        coloredTextures.put("particle", coloredTextures.get("north"));
        overlayTextures.put("particle", overlayTextures.get("north"));
        return toJson(object(
                "parent",
                BLOCK_PARENT,
                "loader",
                compositeLoader,
                "display",
                size.equals("2x2") ? object() : reactorCoreDisplay(),
                "children",
                object(
                        "colored", reactorChild(coloredTextures, true, true),
                        "overlay", reactorChild(overlayTextures, false, includeDown)),
                "textures",
                object("particle", coloredTextures.get("north"))));
    }

    private static Map<String, Object> reactorChild(Map<String, Object> textures, boolean tinted, boolean includeDown) {
        return object(
                "parent", BLOCK_PARENT,
                "render_type", CUTOUT,
                "ambientocclusion", false,
                "textures", textures,
                "elements", reactorElements(tinted, includeDown));
    }

    /** The six shell plates of a reactor core; the bottom plate drops out when no outlet faces down. */
    private static List<Object> reactorElements(boolean tinted, boolean includeDown) {
        List<Object> elements = new ArrayList<>(6);
        if (includeDown) {
            elements.add(reactorElement(0, 0, 0, 16, 2, 16, "down", "up", "#down", tinted));
        }
        elements.add(reactorElement(0, 14, 0, 16, 16, 16, "up", "down", "#up", tinted));
        elements.add(reactorElement(0, 0, 0, 16, 16, 2, "north", "south", "#north", tinted));
        elements.add(reactorElement(0, 0, 14, 16, 16, 16, "south", "north", "#south", tinted));
        elements.add(reactorElement(0, 0, 0, 2, 16, 16, "west", "east", "#west", tinted));
        elements.add(reactorElement(14, 0, 0, 16, 16, 16, "east", "west", "#east", tinted));
        return elements;
    }

    private static Map<String, Object> reactorElement(
            int x0,
            int y0,
            int z0,
            int x1,
            int y1,
            int z1,
            String firstFace,
            String secondFace,
            String texture,
            boolean tinted) {
        Map<String, Object> faces = new LinkedHashMap<>();
        faces.put(firstFace, reactorFace(texture, tinted));
        faces.put(secondFace, reactorFace(texture, tinted));
        return object("from", array(x0, y0, z0), "to", array(x1, y1, z1), "faces", faces);
    }

    private static Map<String, Object> reactorFace(String texture, boolean tinted) {
        Map<String, Object> face = object("texture", texture, "uv", array(0, 0, 16, 16));
        if (tinted) face.put("tintindex", 0);
        return face;
    }

    private static Map<String, Object> reactorCoreDisplay() {
        return object(
                "gui", display(30, 225, 0, 0, 0, 0, 0.625),
                "ground", display(0, 0, 0, 0, 3, 0, 0.25),
                "fixed", display(0, 0, 0, 0, 0, 0, 0.5),
                "thirdperson_righthand", display(75, 45, 0, 0, 2.5, 0, 0.375),
                "firstperson_righthand", display(0, 45, 0, 0, 0, 0, 0.4),
                "firstperson_lefthand", display(0, 225, 0, 0, 0, 0, 0.4));
    }

    private static Map<String, Object> display(
            int rotateX, int rotateY, int rotateZ, double shiftX, double shiftY, double shiftZ, double scale) {
        return object(
                "rotation", array(rotateX, rotateY, rotateZ),
                "translation", array(number(shiftX), number(shiftY), number(shiftZ)),
                "scale", array(number(scale), number(scale), number(scale)));
    }

    private static void forEachReactorCoreModelPath(Consumer<String> sink) {
        for (String size : REACTOR_SIZES) {
            for (String hot : ALL_FACINGS) {
                for (String cold : ALL_FACINGS) {
                    sink.accept(reactorCorePath(size, hot, cold));
                }
            }
        }
    }

    private static @Nullable String renderReactorCoreModel(String modelPath, String compositeLoader) {
        String rest = modelPath.substring("block/machine/energy/reactor_core".length());
        String size = "1x1";
        if (rest.startsWith("_2x2")) {
            size = "2x2";
            rest = rest.substring("_2x2".length());
        }
        if (rest.isEmpty()) return reactorCoreJson(compositeLoader, size, "down", "down");
        if (!rest.startsWith("_")) return null;
        String[] parts = rest.substring(1).split("_", -1);
        if (parts.length != 2) return null;
        if (!ALL_FACINGS.contains(parts[0]) || !ALL_FACINGS.contains(parts[1])) return null;
        if ("down".equals(parts[0]) && "down".equals(parts[1])) return null;
        return reactorCoreJson(compositeLoader, size, parts[0], parts[1]);
    }

    private static void forEachReactorCoreBlockstateStem(Consumer<String> sink) {
        sink.accept("reactor_core");
        sink.accept("reactor_core_2x2");
    }

    private static String reactorCoreBlockstateJson(String size) {
        Map<String, Object> variants = new LinkedHashMap<>();
        for (String hot : ALL_FACINGS) {
            for (String cold : ALL_FACINGS) {
                variants.put(
                        "hot_outlet=" + hot + ",cold_outlet=" + cold,
                        object("model", "gt6ce:" + reactorCorePath(size, hot, cold)));
            }
        }
        return toJson(object("variants", variants));
    }

    private static @Nullable String renderReactorCoreBlockstate(String stem) {
        return switch (stem) {
            case "reactor_core" -> reactorCoreBlockstateJson("1x1");
            case "reactor_core_2x2" -> reactorCoreBlockstateJson("2x2");
            default -> null;
        };
    }

    // ------------------------------------------------------------ batteries

    private static String batteryArchitecture(ChemicalBatterySpec spec) {
        return spec.chemistry().advanced ? "advanced" : "standard";
    }

    /** Model path prefix of one battery, covering its architecture and voltage. */
    private static String batteryPrefix(ChemicalBatterySpec spec) {
        return batteryArchitecture(spec) + "_" + spec.voltage();
    }

    private static String batteryBlockModelPath(ChemicalBatterySpec spec, int level) {
        return "block/chemical_battery/" + batteryPrefix(spec) + "_" + level;
    }

    private static String batteryBlockJson(ChemicalBatterySpec spec, int level) {
        int inset = spec.inset();
        String base = "gt6ce:" + spec.texture();
        List<Object> elements = new ArrayList<>(2);
        Map<String, Object> shellFaces = object(
                "north", object("texture", "#sides"),
                "south", object("texture", "#sides"),
                "east", object("texture", "#sides"),
                "west", object("texture", "#sides"),
                "up", object("texture", "#top"),
                "down", object("texture", "#bottom"));
        elements.add(object(
                "from", array(inset, 0, inset),
                "to", array(16 - inset, spec.height(), 16 - inset),
                "faces", shellFaces));
        if (level > 0) {
            Map<String, Object> barFaces = object(
                    "north", object("texture", "#bar", "tintindex", 0),
                    "south", object("texture", "#bar", "tintindex", 0),
                    "east", object("texture", "#bar", "tintindex", 0),
                    "west", object("texture", "#bar", "tintindex", 0));
            elements.add(object(
                    "from", array(chargeBound(inset - 1, ".968"), 1, chargeBound(inset - 1, ".968")),
                    "to", array(chargeBound(16 - inset, ".032"), 1 + level, chargeBound(16 - inset, ".032")),
                    "faces", barFaces));
        }
        Map<String, Object> textures = object(
                "bottom", base + "/bottom",
                "top", base + "/top",
                "sides", base + "/sides",
                "bar", base + "/bar",
                "particle", base + "/sides");
        return toJson(
                object("parent", BLOCK_PARENT, "render_type", CUTOUT, "textures", textures, "elements", elements));
    }

    /** Charge bar bound, written as {@code whole + suffix} so the fraction never picks up float noise. */
    private static Raw chargeBound(int whole, String fraction) {
        return new Raw(whole + fraction);
    }

    private static String batteryItemJson(ChemicalBatterySpec spec) {
        String modelPrefix = "gt6ce:block/chemical_battery/" + batteryPrefix(spec) + "_";
        List<Object> overrides = new ArrayList<>(spec.scale());
        for (int level = 1; level <= spec.scale(); level++) {
            overrides.add(object(
                    "predicate",
                    object("gt6ce:battery_charge", (double) level / spec.scale()),
                    "model",
                    modelPrefix + level));
        }
        return toJson(object("parent", modelPrefix + "0", "overrides", overrides));
    }

    private static void forEachBatteryModelPath(Consumer<String> sink) {
        Set<String> emitted = new LinkedHashSet<>();
        for (ChemicalBatterySpec spec : ChemicalBatterySpec.all()) {
            if (emitted.add(batteryPrefix(spec))) {
                for (int level = 0; level <= spec.scale(); level++) {
                    sink.accept(batteryBlockModelPath(spec, level));
                }
            }
            sink.accept("item/" + spec.id());
        }
    }

    private static @Nullable String renderBatteryBlockModel(String modelPath) {
        String rest = modelPath.substring("block/chemical_battery/".length());
        int separator = rest.lastIndexOf('_');
        if (separator < 0) return null;
        int level = parseIndex(rest.substring(separator + 1), BATTERY_MAX_SCALE);
        if (level < 0) return null;
        String prefix = rest.substring(0, separator);
        for (ChemicalBatterySpec spec : ChemicalBatterySpec.all()) {
            if (!batteryPrefix(spec).equals(prefix)) continue;
            return level > spec.scale() ? null : batteryBlockJson(spec, level);
        }
        return null;
    }

    private static @Nullable String renderBatteryItemModel(String modelPath) {
        String id = modelPath.substring("item/".length());
        for (ChemicalBatterySpec spec : ChemicalBatterySpec.all()) {
            if (spec.id().equals(id)) return batteryItemJson(spec);
        }
        return null;
    }

    private static void forEachBatteryBlockstateStem(Consumer<String> sink) {
        for (ChemicalBatterySpec spec : ChemicalBatterySpec.all()) {
            sink.accept(spec.id());
        }
    }

    private static String batteryBlockstateJson(ChemicalBatterySpec spec) {
        return toJson(object("variants", object("", object("model", "gt6ce:" + batteryBlockModelPath(spec, 0)))));
    }

    private static @Nullable String renderBatteryBlockstate(String stem) {
        for (ChemicalBatterySpec spec : ChemicalBatterySpec.all()) {
            if (spec.id().equals(stem)) return batteryBlockstateJson(spec);
        }
        return null;
    }

    // ------------------------------------------------------------- compasses

    private static String compassItemJson(String mode) {
        List<Object> overrides = new ArrayList<>(COMPASS_DIRECTIONS.size());
        for (int frame = 1; frame <= COMPASS_DIRECTIONS.size(); frame++) {
            overrides.add(object(
                    "predicate",
                    object("gt6ce:compass_frame", frame),
                    "model",
                    "gt6ce:item/compass/" + mode + "_" + frame));
        }
        Map<String, Object> textures =
                object("layer0", "gt6ce:item/randomtools/compass_" + mode, "layer1", "gt6ce:item/iconsets/compass_n");
        return toJson(object("parent", ITEM_PARENT, "textures", textures, "overrides", overrides));
    }

    private static String compassNeedleJson(String mode, int frame) {
        Map<String, Object> textures = object(
                "layer0", "gt6ce:item/randomtools/compass_" + mode,
                "layer1", "gt6ce:item/iconsets/compass_" + COMPASS_DIRECTIONS.get(frame - 1));
        return toJson(object("parent", ITEM_PARENT, "textures", textures));
    }

    private static void forEachCompassModelPath(Consumer<String> sink) {
        for (String mode : COMPASS_MODES) {
            sink.accept("item/compass_" + mode);
            for (int frame = 1; frame <= COMPASS_DIRECTIONS.size(); frame++) {
                sink.accept("item/compass/" + mode + "_" + frame);
            }
        }
    }

    private static @Nullable String renderCompassModel(String modelPath) {
        if (modelPath.startsWith("item/compass/")) {
            String rest = modelPath.substring("item/compass/".length());
            int separator = rest.lastIndexOf('_');
            if (separator < 0) return null;
            String mode = rest.substring(0, separator);
            int frame = parseIndex(rest.substring(separator + 1), COMPASS_DIRECTIONS.size());
            if (!COMPASS_MODES.contains(mode) || frame < 1) return null;
            return compassNeedleJson(mode, frame);
        }
        if (!modelPath.startsWith("item/compass_")) return null;
        String mode = modelPath.substring("item/compass_".length());
        return COMPASS_MODES.contains(mode) ? compassItemJson(mode) : null;
    }

    // --------------------------------------------------------- filter covers

    private static void forEachFilterCoverModelPath(Consumer<String> sink) {
        for (String id : FILTER_COVER_FOLDERS.keySet()) {
            sink.accept("item/" + id);
            sink.accept("item/" + id + "_inverted");
        }
    }

    private static @Nullable String renderFilterCoverModel(String modelPath) {
        if (!modelPath.startsWith("item/")) return null;
        String name = modelPath.substring("item/".length());
        if (name.endsWith("_inverted")) {
            String baseId = name.substring(0, name.length() - "_inverted".length());
            String folder = FILTER_COVER_FOLDERS.get(baseId);
            if (folder == null) return null;
            return toJson(object(
                    "parent",
                    ITEM_PARENT,
                    "textures",
                    object(
                            "layer0",
                            "gt6ce:item/technological/blank_cover",
                            "layer1",
                            "gt6ce:block/machines/covers/" + folder + "/inverted")));
        }
        if (!FILTER_COVER_FOLDERS.containsKey(name)) return null;
        List<Object> overrides = List.of(
                object("predicate", object("gt6ce:cover_inverted", 1), "model", "gt6ce:item/" + name + "_inverted"));
        return toJson(object(
                "parent",
                ITEM_PARENT,
                "textures",
                object("layer0", "gt6ce:item/technological/" + name),
                "overrides",
                overrides));
    }

    // -------------------------------------------------------------- scaffolds

    /** Every scaffold design 1 model path, derived from {@link #SCAFFOLD_MATERIALS}. */
    private static Set<String> scaffoldPaths() {
        Set<String> paths = new LinkedHashSet<>(SCAFFOLD_MATERIALS.size() + 1);
        paths.add("block/tool/scaffold_1");
        for (String material : SCAFFOLD_MATERIALS) {
            paths.add("block/tool/scaffold_" + material + "_1");
        }
        return Collections.unmodifiableSet(paths);
    }

    /** Renders the first scaffold design, whose rods take the material's solid block sprite. */
    public static String scaffoldDesign1Json(String compositeLoader, String rodTexture) {
        Objects.requireNonNull(rodTexture, "rodTexture");
        List<Object> baseElements = List.of(
                scaffoldElement(0, 10, 0, 16, 16, 16, "#plate", ALL_FACINGS),
                scaffoldElement(0, 0, 0, 2, 10, 2, "#rod", ALL_FACINGS),
                scaffoldElement(0, 0, 14, 2, 10, 16, "#rod", ALL_FACINGS),
                scaffoldElement(14, 0, 0, 16, 10, 2, "#rod", ALL_FACINGS),
                scaffoldElement(14, 0, 14, 16, 10, 16, "#rod", ALL_FACINGS),
                scaffoldElement(2, 4, 0, 14, 5, 4, "#plate", ALL_FACINGS),
                scaffoldElement(2, 12, 0, 14, 13, 4, "#plate", ALL_FACINGS));
        List<Object> hatchElements = List.of(
                scaffoldElement(0, 10, 0, 16, 16, 16, "#hatch", VERTICAL_FACES),
                scaffoldElement(2, 4, 0, 14, 5, 4, "#hatch", VERTICAL_FACES),
                scaffoldElement(2, 12, 0, 14, 13, 4, "#hatch", VERTICAL_FACES));
        Map<String, Object> base = object(
                "textures",
                object("particle", SCAFFOLD_PLATE, "plate", SCAFFOLD_PLATE, "rod", rodTexture),
                "elements",
                baseElements);
        Map<String, Object> hatch = object(
                "textures", object("hatch", SCAFFOLD_HATCH),
                "elements", hatchElements,
                "render_type", CUTOUT);
        return toJson(object(
                "parent",
                BLOCK_PARENT,
                "loader",
                compositeLoader,
                "textures",
                object("particle", SCAFFOLD_PLATE),
                "children",
                object("base", base, "hatch", hatch)));
    }

    // ------------------------------------------------------------------ json

    /** Marker for a number written verbatim, used where a fraction must keep an exact spelling. */
    private record Raw(String text) {}

    /** Builds an ordered JSON object from alternating keys and values. */
    private static Map<String, Object> object(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("JSON objects need an even number of arguments");
        }
        Map<String, Object> node = new LinkedHashMap<>(keysAndValues.length * 2);
        for (int index = 0; index < keysAndValues.length; index += 2) {
            node.put((String) keysAndValues[index], keysAndValues[index + 1]);
        }
        return node;
    }

    private static List<Object> array(Object... values) {
        return List.of(values);
    }

    /** Boxes {@code value} as an integer when it has no fractional part, keeping emitted numbers short. */
    private static Object number(double value) {
        if (value == Math.floor(value) && Math.abs(value) < 1.0e7) return Integer.valueOf((int) value);
        return Double.valueOf(value);
    }

    private static String toJson(Object node) {
        StringBuilder out = new StringBuilder(384);
        appendJson(out, node);
        return out.toString();
    }

    private static void appendJson(StringBuilder out, Object node) {
        if (node instanceof Map<?, ?> members) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> member : members.entrySet()) {
                if (!first) out.append(',');
                first = false;
                JsonEscaper.appendQuoted(out, (String) member.getKey());
                out.append(':');
                appendJson(out, member.getValue());
            }
            out.append('}');
        } else if (node instanceof List<?> items) {
            out.append('[');
            for (int index = 0; index < items.size(); index++) {
                if (index > 0) out.append(',');
                appendJson(out, items.get(index));
            }
            out.append(']');
        } else if (node instanceof String text) {
            JsonEscaper.appendQuoted(out, text);
        } else if (node instanceof Raw raw) {
            out.append(raw.text());
        } else if (node instanceof Number || node instanceof Boolean) {
            out.append(node);
        } else {
            throw new IllegalArgumentException("Unsupported JSON node: " + node);
        }
    }

    // --------------------------------------------------------------- helpers

    private static String opposite(String facing) {
        return switch (facing) {
            case "down" -> "up";
            case "up" -> "down";
            case "north" -> "south";
            case "south" -> "north";
            case "west" -> "east";
            case "east" -> "west";
            default -> throw new IllegalArgumentException("Unknown facing: " + facing);
        };
    }

    /** Parses a decimal index in {@code [0, max]}, or returns {@code -1} when the text is not one. */
    private static int parseIndex(String text, int max) {
        if (text.isEmpty() || text.length() > 9) return -1;
        if (text.length() > 1 && text.charAt(0) == '0') return -1;
        int value = 0;
        for (int index = 0; index < text.length(); index++) {
            char digit = text.charAt(index);
            if (digit < '0' || digit > '9') return -1;
            value = value * 10 + (digit - '0');
        }
        return value <= max ? value : -1;
    }

    private static Map<String, Object> scaffoldElement(
            int x0, int y0, int z0, int x1, int y1, int z1, String texture, List<String> faces) {
        Map<String, Object> faceNodes = new LinkedHashMap<>();
        for (String face : faces) {
            faceNodes.put(face, object("texture", texture, "tintindex", 0));
        }
        return object("from", array(x0, y0, z0), "to", array(x1, y1, z1), "faces", faceNodes);
    }
}
