package gregtech.compat.common;

import gregtech.api.addon.worldgen.OreVeinDeclaration;
import gregtech.api.material.GTMaterial;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.content.block.RockOreBlock;
import gregtech.content.block.stone.StoneType;
import gregtech.content.block.stone.StoneVariant;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.generated.OreMaterials;
import gregtech.content.registry.GTBlocks;
import gregtech.content.registry.GTItems;
import gregtech.content.worldgen.BoundaryGemOres;
import gregtech.content.worldgen.GTOreBlockResolver;
import gregtech.content.worldgen.GTOreVeins;
import gregtech.content.worldgen.GTOreVeins.HeightRange;
import gregtech.content.worldgen.GTStoneLayersGen;
import gregtech.content.worldgen.MineralWorldgenRules;
import gregtech.content.worldgen.OreGenerationSummary;
import gregtech.content.worldgen.OreSourceCatalog;
import gregtech.content.worldgen.OreSourceCatalog.Kind;
import gregtech.content.worldgen.OreVeinLayout;
import gregtech.content.worldgen.SpecialOreDeposits;
import gregtech.content.worldgen.StoneLayerField;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.material.MaterialPresentation;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.platform.Mc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Worldgen catalog shared by the recipe viewers, independent of local geology and bedrock switches. */
public class WorldgenInfoData {
    protected WorldgenInfoData() {}

    public record VeinInfo(
            String name,
            String dimension,
            int minY,
            int maxY,
            int weight,
            int totalWeight,
            GTMaterial top,
            GTMaterial bottom,
            GTMaterial between,
            GTMaterial spread,
            StratumInfo layer,
            HeightRange reserve,
            boolean subregional,
            SpecialSite site) {
        static VeinInfo special(String name, HeightRange heights, SpecialSite site) {
            return new VeinInfo(
                    name,
                    OreVeinDeclaration.OVERWORLD,
                    heights.min(),
                    heights.max(),
                    0,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    site);
        }
    }

    public record SmallOreInfo(List<OrePreview> ores, List<Component> captions) {
        public List<ItemStack> aliases() {
            return ores.stream()
                    .flatMap(ore -> oreAliases(ore.material(), true).stream())
                    .toList();
        }
    }

    public record LayerInfo(ItemStack stone, List<StratumOre> ores) {}

    public static ItemStack oreStack(GTMaterial material) {
        return oreStack(material, false);
    }

    public static ItemStack smallOreStack(GTMaterial material) {
        return oreStack(material, true);
    }

    private static ItemStack oreStack(GTMaterial material, boolean small) {
        var placement = GTOreBlockResolver.resolve(OreHostStone.STONE, material.resolve(), small);
        return placement == null
                ? ItemStack.EMPTY
                : new ItemStack(placement.state().getBlock());
    }

    public static List<VeinInfo> buildVeins() {
        var summary = OreGenerationSummary.current();
        List<VeinInfo> out = new ArrayList<>();
        for (String dimension : OreSourceCatalog.current().veinDimensions()) {
            var generation = summary.dimensions().get(dimension);
            if (generation == null || !generation.veins()) continue;
            var veins = OreSourceCatalog.current().veins(dimension);
            int totalWeight = GTOreVeins.selectionWeight(dimension, veins);
            for (GTOreVeins.OreVein v : veins) {
                var heights = v.heights(dimension).intersect(generation.heights());
                var reserve = GTOreVeins.reserveHeights(dimension, v.name(), generation.heights())
                        .orElse(null);
                if (heights.isEmpty() && reserve == null) continue;
                out.add(new VeinInfo(
                        v.name(),
                        dimension,
                        heights.min(),
                        heights.max(),
                        v.weight(),
                        totalWeight,
                        v.top(),
                        v.bottom(),
                        v.between(),
                        v.spread(),
                        null,
                        reserve,
                        v.subregional(),
                        null));
            }
        }
        HeightRange stratumHeights = overworldStrata(summary);
        if (stratumHeights != null) {
            var seen = new LinkedHashSet<StratumKey>();
            for (GTStoneLayersGen.LayerDef layer : StoneLayerField.layers()) {
                ItemStack stone = layerStone(layer);
                appendStratumOres(
                        out,
                        seen,
                        layer.material(),
                        null,
                        false,
                        List.of(stone),
                        stratumOres(layer, stone, true, summary, stratumHeights));
            }
            record Contact(String first, String second, boolean bothSides) {}
            var contacts = new LinkedHashMap<Contact, List<GTStoneLayersGen.OreDef>>();
            for (GTStoneLayersGen.ContactDef contact : GTStoneLayersGen.CONTACTS) {
                String first = contact.materialA();
                String second = contact.materialB();
                if (contact.bothSides() && first.compareTo(second) > 0) {
                    first = contact.materialB();
                    second = contact.materialA();
                }
                contacts.computeIfAbsent(new Contact(first, second, contact.bothSides()), key -> new ArrayList<>())
                        .addAll(contact.ores());
            }
            contacts.forEach((contact, ores) -> appendStratumOres(
                    out,
                    seen,
                    contact.first(),
                    contact.second(),
                    contact.bothSides(),
                    contactStones(contact.first(), contact.second()),
                    stratumOres(ores, false, summary, stratumHeights)));
        }
        appendSpecialSites(out, summary);
        return out;
    }

    public static List<Component> veinCaptions(VeinInfo vein) {
        if (vein.site() != null) return vein.site().captions();
        double percent = 100.0 * vein.weight() / Math.max(1, vein.totalWeight());
        boolean end = OreVeinDeclaration.END.equals(vein.dimension());
        String label = end ? "chance" : "bonus_vein_share";
        String shape = OreVeinLayout.Shape.forVein(vein.name()).name().toLowerCase(Locale.ROOT);
        var name = Component.translatableWithFallback("gt6ce.jei.vein." + vein.name(), vein.name());
        List<Component> lines = new ArrayList<>(List.of(
                Component.translatable("gt6ce.jei.info.vein_name", name),
                Component.translatable("gt6ce.jei.info.dimension", vein.dimension())));
        if (vein.minY() <= vein.maxY()) {
            lines.add(Component.translatable("gt6ce.jei.info.height", vein.minY(), vein.maxY()));
            if (vein.weight() > 0)
                lines.add(
                        Component.translatable("gt6ce.jei.info." + label, String.format(Locale.ROOT, "%.2f", percent)));
        }
        lines.add(Component.translatable(
                "gt6ce.jei.info.vein_shape", Component.translatable("gt6ce.jei.shape." + shape)));
        var regional = OreSourceCatalog.current().regionalPlans(vein.dimension()).stream()
                .filter(plan -> plan.includes(vein.name()))
                .findFirst()
                .orElse(null);
        if (regional != null) {
            int cells = regional.cells();
            String spacing = end ? "island_regional_vein" : vein.subregional() ? "subregional_vein" : "regional_vein";
            lines.add(Component.translatable("gt6ce.jei.info." + spacing, cells * OreVeinLayout.CELL_SIZE));
            if (vein.reserve() != null) {
                lines.add(Component.translatable(
                        "gt6ce.jei.info.vein_reserve_height",
                        vein.reserve().min(),
                        vein.reserve().max()));
            }
        }
        return lines;
    }

    public record StratumOre(GTMaterial material, int minY, int maxY, int chanceDiv, String blockId) {}

    public record OrePreview(ItemStack stack, List<Component> captions, GTMaterial material) {
        public List<Component> tooltip() {
            var lines = new ArrayList<>(captions);
            lines.addAll(oreSourceCaptions(material));
            return lines;
        }
    }

    private record OreIdentity(GTMaterial material, String blockId) {}

    public record StratumInfo(Component host, boolean contact, List<ItemStack> stones, List<StratumOre> ores) {}

    public record SpecialSite(List<ItemStack> hosts, List<OrePreview> ores, List<Component> captions) {}

    private static void appendSpecialSites(List<VeinInfo> rows, OreGenerationSummary summary) {
        var active = summary.specialDeposits();
        var coltan = active.get(Kind.COLTAN_FIELD);
        var centre = active.get(Kind.COLTAN_BEDROCK);
        if (coltan != null || centre != null) rows.add(coltanSite(coltan, centre));
        var ocean = active.get(Kind.OCEAN_PYLON);
        if (ocean != null) {
            for (var pylon : SpecialOreDeposits.OCEAN_PYLONS) {
                var host = new ItemStack(GTBlocks.getStoneState(pylon.stone(), StoneVariant.STONE)
                        .getBlock());
                var hosts = List.of(host);
                var ore = withHost(oreStack(pylon.material()), hosts);
                int divisor = SpecialOreDeposits.OCEAN_ORE_DIVISOR;
                rows.add(specialSite(
                        pylon.stone().registryId(),
                        Kind.OCEAN_PYLON,
                        ocean,
                        hosts,
                        List.of(new OrePreview(ore, List.of(), pylon.material())),
                        List.of(
                                Component.translatable("gt6ce.jei.info.pylon_host", host.getHoverName()),
                                Component.translatable("gt6ce.jei.info.height", ocean.min(), ocean.max()),
                                Component.translatable(
                                        "gt6ce.jei.info.stratum_roll",
                                        divisor,
                                        String.format(Locale.ROOT, "%.2f", 100d / divisor)))));
            }
        }
        var floor = active.get(Kind.DUNGEON_BEDROCK);
        if (floor != null) {
            var rolls = SpecialOreDeposits.DUNGEON_ROLLS;
            var ores = new ArrayList<OrePreview>();
            for (GTMaterial material : new LinkedHashSet<>(rolls)) {
                var detail = List.of(siteMaterialRoll(rolls, material));
                ores.add(new OrePreview(bedrockOreStack(material), detail, material));
                var raw = GTBlocks.getObject(BlockMaterialPrefix.blockRaw, material);
                if (raw != null && raw.isPresent())
                    ores.add(new OrePreview(new ItemStack(raw.get()), detail, material));
            }
            int rawY = floor.min() + SpecialOreDeposits.DUNGEON_RAW_ORE_OFFSET;
            rows.add(specialSite(
                    "dungeon_mine",
                    Kind.DUNGEON_BEDROCK,
                    floor,
                    List.of(new ItemStack(Items.BEDROCK)),
                    ores,
                    List.of(
                            Component.translatable("gt6ce.jei.info.bedrock_height", floor.min()),
                            Component.translatable("gt6ce.jei.info.dungeon_raw_ore_height", rawY, rawY + 2))));
        }
    }

    private static VeinInfo coltanSite(HeightRange field, HeightRange centre) {
        var ores = new ArrayList<OrePreview>();
        List<Component> captions = new ArrayList<>();
        if (field != null) {
            var rolls = SpecialOreDeposits.COLTAN_ROLLS;
            for (GTMaterial material : new LinkedHashSet<>(rolls)) {
                var detail = List.of(siteMaterialRoll(rolls, material));
                ores.add(new OrePreview(smallOreStack(material), detail, material));
                ores.add(new OrePreview(oreStack(material), detail, material));
            }
            captions.add(Component.translatable("gt6ce.jei.info.height", field.min(), field.max()));
            captions.add(
                    Component.translatable("gt6ce.jei.info.coltan_small_radius", SpecialOreDeposits.COLTAN_RADIUS));
            captions.add(Component.translatable(
                    "gt6ce.jei.info.coltan_full_radius", SpecialOreDeposits.COLTAN_FULL_ORE_RADIUS));
        }
        if (centre != null) {
            var material = OreMaterials.Coltan;
            ores.add(new OrePreview(bedrockOreStack(material), List.of(), material));
            captions.add(Component.translatable("gt6ce.jei.info.coltan_centre", centre.min()));
        }
        return specialSite(
                "coltan_field",
                field == null ? Kind.COLTAN_BEDROCK : Kind.COLTAN_FIELD,
                field == null ? centre : field,
                List.of(),
                ores,
                captions);
    }

    private static Component siteMaterialRoll(List<GTMaterial> rolls, GTMaterial material) {
        return Component.translatable(
                "gt6ce.jei.info.site_material_roll", Collections.frequency(rolls, material), rolls.size());
    }

    private static VeinInfo specialSite(
            String name,
            Kind kind,
            HeightRange heights,
            List<ItemStack> hosts,
            List<OrePreview> ores,
            List<Component> details) {
        List<Component> captions = new ArrayList<>();
        captions.add(sourceName(kind));
        captions.add(Component.translatable("gt6ce.jei.info.dimension", OreVeinDeclaration.OVERWORLD));
        captions.addAll(details);
        var site = new SpecialSite(
                hosts, ores.stream().filter(ore -> !ore.stack().isEmpty()).toList(), List.copyOf(captions));
        return VeinInfo.special(name, heights, site);
    }

    private record StratumKey(
            String above, String below, boolean bothSides, List<Item> stones, List<StratumOre> ores) {}

    private static ItemStack layerStone(GTStoneLayersGen.LayerDef layer) {
        if (layer.stoneType() != null) {
            var type = StoneType.valueOf(layer.stoneType());
            return new ItemStack(
                    GTBlocks.getStoneState(type, StoneVariant.STONE).getBlock());
        }
        if (layer.blockId() != null) {
            Item item = Mc.item(layerBlockId(layer));
            if (item != Items.AIR) return new ItemStack(item);
        }
        return new ItemStack(Items.STONE);
    }

    private static List<ItemStack> contactStones(String above, String below) {
        var stones = new LinkedHashMap<Item, ItemStack>();
        for (String material : List.of(above, below)) {
            for (var layer : StoneLayerField.layers()) {
                if (!layer.material().equals(material)) continue;
                ItemStack stone = layerStone(layer);
                stones.putIfAbsent(stone.getItem(), stone);
            }
        }
        return List.copyOf(stones.values());
    }

    public static List<ItemStack> veinHosts(VeinInfo vein) {
        if (vein.site() != null) return vein.site().hosts();
        return vein.layer() == null ? List.of() : vein.layer().stones();
    }

    private static Component materialName(String name) {
        GTMaterial material = GTMaterialRegistry.get(name).resolve();
        return material.isValid() ? MaterialPresentation.name(material) : Component.literal(name);
    }

    private static void appendStratumOres(
            List<VeinInfo> out,
            Set<StratumKey> seen,
            String above,
            String below,
            boolean bothSides,
            List<ItemStack> stones,
            List<StratumOre> ores) {
        if (ores.isEmpty()
                || !seen.add(new StratumKey(
                        above,
                        below,
                        bothSides,
                        stones.stream().map(ItemStack::getItem).toList(),
                        ores))) return;
        String contactKey = bothSides ? "gt6ce.jei.info.contact_both" : "gt6ce.jei.info.contact_ordered";
        Component host = below == null
                ? materialName(above)
                : Component.translatable(contactKey, materialName(above), materialName(below));
        int min = ores.stream().mapToInt(StratumOre::minY).min().orElseThrow();
        int max = ores.stream().mapToInt(StratumOre::maxY).max().orElseThrow();
        out.add(new VeinInfo(
                above,
                "minecraft:overworld",
                min,
                max,
                0,
                0,
                null,
                null,
                null,
                null,
                new StratumInfo(host, below != null, stones, ores),
                null,
                false,
                null));
    }

    private static List<StratumOre> stratumOres(
            GTStoneLayersGen.LayerDef layer,
            ItemStack stone,
            boolean includeSeam,
            OreGenerationSummary summary,
            HeightRange terrain) {
        if (includeSeam && stone.getItem() instanceof BlockItem item && item.getBlock() instanceof RockOreBlock seam) {
            int minY = layer.noDeep() ? StoneLayerField.SHALLOW_LAYER_MIN_Y : -64;
            var heights = new HeightRange(minY, 319).intersect(terrain);
            if (heights.isEmpty()) return List.of();
            return List.of(new StratumOre(
                    seam.material().resolve(),
                    heights.min(),
                    heights.max(),
                    0,
                    layerBlockId(layer).toString()));
        }
        return stratumOres(layer.ores(), layer.noDeep(), summary, terrain);
    }

    private static List<StratumOre> stratumOres(
            List<GTStoneLayersGen.OreDef> definitions,
            boolean noDeep,
            OreGenerationSummary summary,
            HeightRange terrain) {
        var ores = new ArrayList<StratumOre>();
        for (var def : definitions) {
            if (summary.disabledStratumBlocks().contains(def.blockId())) continue;
            GTMaterial material = GTMaterialRegistry.get(def.material()).resolve();
            if (!material.isValid()) continue;
            int min = MineralWorldgenRules.stratumOreMinY(def);
            if (noDeep) min = Math.max(min, StoneLayerField.SHALLOW_LAYER_MIN_Y);
            var heights = new HeightRange(min, MineralWorldgenRules.stratumOreMaxY(def)).intersect(terrain);
            if (!heights.isEmpty())
                ores.add(new StratumOre(material, heights.min(), heights.max(), def.chanceDiv(), def.blockId()));
        }
        return List.copyOf(ores);
    }

    private static List<GTMaterial> veinMaterials(VeinInfo vein) {
        return List.of(vein.top(), vein.bottom(), vein.between(), vein.spread()).stream()
                .map(GTMaterial::resolve)
                .distinct()
                .toList();
    }

    /** Search aliases, not additional guaranteed products. Small ores use the actual crushing target. */
    public static List<ItemStack> oreAliases(GTMaterial material, boolean small) {
        var prefix = small ? MaterialPrefix.crushed : MaterialPrefix.oreRaw;
        GTMaterial target = small ? material.getTargetCrushingMaterial().resolve() : material.resolve();
        ItemStack stack = GTItems.getStack(prefix, target);
        return stack.isEmpty() ? List.of() : List.of(stack);
    }

    public static List<OrePreview> veinPreviews(VeinInfo vein) {
        if (vein.site() != null) return vein.site().ores();
        if (vein.layer() != null) return stratumPreviews(vein.layer().ores(), veinHosts(vein));
        return veinMaterials(vein).stream()
                .map(material -> new OrePreview(oreStack(material), List.of(), material))
                .filter(preview -> !preview.stack().isEmpty())
                .toList();
    }

    public static List<OrePreview> layerPreviews(LayerInfo layer) {
        return stratumPreviews(layer.ores(), List.of(layer.stone()));
    }

    private static List<OrePreview> stratumPreviews(List<StratumOre> ores, List<ItemStack> hosts) {
        var grouped = new LinkedHashMap<OreIdentity, List<Component>>();
        for (var ore : ores) {
            List<Component> captions =
                    grouped.computeIfAbsent(new OreIdentity(ore.material(), ore.blockId()), key -> new ArrayList<>());
            captions.add(Component.translatable("gt6ce.jei.info.height", ore.minY(), ore.maxY()));
            if (ore.chanceDiv() == 0) captions.add(Component.translatable("gt6ce.jei.info.seam_ore"));
            else
                captions.add(Component.translatable(
                        "gt6ce.jei.info.stratum_roll",
                        ore.chanceDiv(),
                        String.format(Locale.ROOT, "%.2f", 100d / ore.chanceDiv())));
        }
        var result = new ArrayList<OrePreview>();
        grouped.forEach((ore, captions) -> {
            ItemStack stack = ore.blockId().isEmpty()
                    ? withHost(oreStack(ore.material()), hosts)
                    : new ItemStack(Mc.block(ResourceLocation.parse(ore.blockId())));
            if (!stack.isEmpty()) result.add(new OrePreview(stack, List.copyOf(captions), ore.material()));
        });
        return result;
    }

    public static List<Component> oreSourceCaptions(GTMaterial material) {
        var sources = OreSourceCatalog.current().sources(material);
        if (sources.isEmpty()) return List.of();
        var summary = OreGenerationSummary.current();
        var available = new LinkedHashMap<String, EnumSet<OreSourceCatalog.Kind>>();
        for (var source : sources) {
            if (summary.includes(source)) {
                available
                        .computeIfAbsent(source.dimension(), key -> EnumSet.noneOf(OreSourceCatalog.Kind.class))
                        .add(source.kind());
            }
        }
        if (available.isEmpty()) return List.of();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gt6ce.jei.info.ore_sources").withStyle(ChatFormatting.GRAY));
        available.forEach((dimension, kinds) -> {
            var line =
                    Component.translatableWithFallback("gt6ce.jei.dimension." + dimension.replace(':', '.'), dimension);
            for (var kind : kinds) {
                line.append(" · ").append(sourceName(kind));
            }
            lines.add(line);
        });
        return lines;
    }

    private static Component sourceName(Kind kind) {
        return Component.translatable("gt6ce.jei.source." + kind.name().toLowerCase(Locale.ROOT));
    }

    private static ItemStack withHost(ItemStack stack, List<ItemStack> hosts) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof OreBlock ore)) return stack;
        // A contact can generate against either face; keep the generic ore when the host is ambiguous.
        if (hosts.size() != 1 || !(hosts.get(0).getItem() instanceof BlockItem stone)) return stack;
        OreHostStone background = GTOreBlockResolver.stoneOf(stone.getBlock().defaultBlockState());
        return background == null ? stack : OreBlock.withStone(stack, ore.stateFor(background));
    }

    public static List<ItemStack> veinAliases(VeinInfo vein) {
        List<ItemStack> aliases = new ArrayList<>(veinHosts(vein));
        if (vein.site() != null) {
            var materials = new LinkedHashSet<GTMaterial>();
            for (var ore : vein.site().ores()) {
                if (materials.add(ore.material())) {
                    aliases.addAll(oreAliases(ore.material(), false));
                    aliases.addAll(oreAliases(ore.material(), true));
                }
                if (OreBlock.stoneOfStack(ore.stack()) == OreHostStone.BEDROCK) {
                    aliases.add(OreBlock.brokenStack(ore.material(), OreHostStone.STONE));
                }
            }
            return aliases;
        }
        if (vein.layer() != null) {
            for (var ore : new LinkedHashSet<>(vein.layer().ores().stream()
                    .map(entry -> new OreIdentity(entry.material(), entry.blockId()))
                    .toList())) addOreAliases(aliases, ore);
            return aliases;
        }
        for (GTMaterial material : veinMaterials(vein)) {
            addOreAliases(aliases, new OreIdentity(material, ""));
        }
        return aliases;
    }

    private static void addOreAliases(List<ItemStack> aliases, OreIdentity ore) {
        if (ore.blockId().startsWith("minecraft:")) {
            aliases.addAll(explicitAliases(ore.blockId()));
        } else {
            aliases.addAll(oreAliases(ore.material(), false));
            if (ore.blockId().isEmpty()) {
                aliases.add(smallOreStack(ore.material()));
                aliases.addAll(oreAliases(ore.material(), true));
            }
        }
    }

    private static List<ItemStack> explicitAliases(String blockId) {
        Item item = switch (blockId) {
            case "minecraft:iron_ore" -> Items.RAW_IRON;
            case "minecraft:gold_ore" -> Items.RAW_GOLD;
            case "minecraft:copper_ore" -> Items.RAW_COPPER;
            case "minecraft:diamond_ore" -> Items.DIAMOND;
            case "minecraft:emerald_ore" -> Items.EMERALD;
            case "minecraft:lapis_ore" -> Items.LAPIS_LAZULI;
            case "minecraft:redstone_ore" -> Items.REDSTONE;
            case "minecraft:coal_ore" -> Items.COAL;
            default -> Items.AIR;
        };
        return item == Items.AIR ? List.of() : List.of(new ItemStack(item));
    }

    public static List<ItemStack> layerAliases(LayerInfo layer) {
        var aliases = new ArrayList<ItemStack>();
        for (var ore : new LinkedHashSet<>(layer.ores().stream()
                .map(entry -> new OreIdentity(entry.material(), entry.blockId()))
                .toList())) addOreAliases(aliases, ore);
        return aliases;
    }

    /** Captions shared by JEI and EMI; individual ore heights and rolls belong to their slots. */
    public static List<Component> stratumCaptions(VeinInfo vein) {
        StratumInfo info = vein.layer();
        return List.of(
                Component.translatable("gt6ce.jei.info.layer_stone", info.host()),
                Component.translatable("gt6ce.jei.info.dimension", vein.dimension()),
                Component.translatable("gt6ce.jei.info.height", vein.minY(), vein.maxY()));
    }

    public static List<SmallOreInfo> buildSmallOres() {
        var summary = OreGenerationSummary.current();
        List<SmallOreInfo> out = new ArrayList<>();
        var catalog = OreSourceCatalog.current();
        for (String dimension : catalog.smallOreDimensions()) {
            var generation = summary.dimensions().get(dimension);
            if (generation == null || !generation.smallOres()) continue;
            var terrain = generation.heights();
            for (var ore : catalog.smallOres(dimension)) {
                var heights = ore.heights(dimension, terrain.min(), terrain.max());
                if (heights.isEmpty()) continue;
                var stack = smallOreStack(ore.material());
                if (stack.isEmpty()) continue;
                int min = MineralWorldgenRules.scatterAttempts(ore.amount(), bound -> 0);
                int max = MineralWorldgenRules.scatterAttempts(ore.amount(), bound -> bound - 1);
                String attempts = min == max ? Integer.toString(min) : min + "–" + max;
                String curve = ore.distribution() == GTOreVeins.Distribution.TRIANGLE ? "triangular" : "uniform";
                out.add(new SmallOreInfo(
                        List.of(new OrePreview(stack, List.of(), ore.material())),
                        List.of(
                                Component.translatable("gt6ce.jei.info.dimension", dimension),
                                Component.translatable("gt6ce.jei.info.height", heights.min(), heights.max()),
                                Component.translatable("gt6ce.jei.info." + curve),
                                Component.translatable("gt6ce.jei.info.small_ore_attempts", attempts))));
            }
        }
        summary.dimensions().forEach((dimension, generation) -> {
            if (!generation.strata()) return;
            var pool = BoundaryGemOres.MATERIALS;
            var gems = pool.stream()
                    .map(gem -> new OrePreview(smallOreStack(gem), List.of(siteMaterialRoll(pool, gem)), gem))
                    .filter(gem -> !gem.stack().isEmpty())
                    .toList();
            if (!gems.isEmpty()) {
                out.add(new SmallOreInfo(
                        gems,
                        List.of(
                                sourceName(Kind.BOUNDARY_GEM),
                                Component.translatable("gt6ce.jei.info.dimension", dimension),
                                Component.translatable("gt6ce.jei.info.boundary_gem", summary.boundaryGemDivisor()))));
            }
        });
        return out;
    }

    public static List<LayerInfo> buildLayers() {
        var summary = OreGenerationSummary.current();
        HeightRange terrain = overworldStrata(summary);
        if (terrain == null) return List.of();
        var layers = new LinkedHashMap<Item, LinkedHashSet<StratumOre>>();
        for (GTStoneLayersGen.LayerDef def : StoneLayerField.layers()) {
            ItemStack stone = layerStone(def);
            layers.computeIfAbsent(stone.getItem(), key -> new LinkedHashSet<>())
                    .addAll(stratumOres(def, stone, false, summary, terrain));
        }
        return layers.entrySet().stream()
                .map(entry -> new LayerInfo(new ItemStack(entry.getKey()), List.copyOf(entry.getValue())))
                .toList();
    }

    private static HeightRange overworldStrata(OreGenerationSummary summary) {
        var overworld = summary.dimensions().get(OreVeinDeclaration.OVERWORLD);
        return overworld != null && overworld.strata() ? overworld.heights() : null;
    }

    private static ResourceLocation layerBlockId(GTStoneLayersGen.LayerDef def) {
        String id = def.blockId();
        return id.contains(":") ? ResourceLocation.parse(id) : Mc.gregtechId(id);
    }

    public record BedrockInfo(
            String name, String dimension, GTMaterial material, int floor, int chance, int density, ItemStack flower) {}

    public static List<BedrockInfo> buildBedrockOres() {
        var summary = OreGenerationSummary.current();
        var catalog = OreSourceCatalog.current();
        List<BedrockInfo> out = new ArrayList<>();
        summary.bedrock().forEach((dimension, table) -> {
            for (var ore : catalog.bedrockOres(dimension, OreSourceCatalog.Geology.DISABLED)) {
                Integer chance = table.divisors().get(ore.name());
                if (chance == null) continue;
                Item flower = ore.flowerId() == null ? Items.AIR : Mc.item(Mc.gregtechId(ore.flowerId()));
                out.add(new BedrockInfo(
                        ore.name(),
                        dimension,
                        ore.material(),
                        table.floor(),
                        chance,
                        summary.bedrockDensity(),
                        flower == null || flower == Items.AIR ? ItemStack.EMPTY : new ItemStack(flower)));
            }
        });
        return out;
    }

    public static ItemStack bedrockOreStack(GTMaterial material) {
        var stack = oreStack(material);
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof OreBlock ore)
            return OreBlock.withStone(stack, ore.stateFor(OreHostStone.BEDROCK));
        return stack;
    }

    public static List<ItemStack> bedrockAliases(BedrockInfo info) {
        var aliases = new ArrayList<>(oreAliases(info.material(), false));
        var stone = OreVeinDeclaration.NETHER.equals(info.dimension()) ? OreHostStone.NETHERRACK : OreHostStone.STONE;
        ItemStack broken = OreBlock.brokenStack(info.material(), stone);
        if (!broken.isEmpty()) aliases.add(broken);
        return aliases;
    }

    public static List<Component> bedrockCaptions(BedrockInfo ore) {
        String indicator = !ore.flower().isEmpty()
                ? "bedrock_flower"
                : OreVeinDeclaration.NETHER.equals(ore.dimension()) ? "bedrock_nether_rock" : "bedrock_rock";
        return List.of(
                Component.translatable("gt6ce.jei.info.dimension", ore.dimension()),
                Component.translatable("gt6ce.jei.info.bedrock_height", ore.floor()),
                Component.translatable(
                        "gt6ce.jei.info.bedrock_chance",
                        String.format(
                                Locale.ROOT,
                                "%.4f",
                                MineralWorldgenRules.bedrockChancePercent(ore.chance(), ore.density()))),
                Component.translatable("gt6ce.jei.info." + indicator));
    }
}
