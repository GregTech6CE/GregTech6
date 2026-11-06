package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.api.material.*;
import gregtech.content.block.misc.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Wave 45: Panels, Crafting Tables, Filters, Extenders, Auto Tools, Long Distance Transport. */
public final class GTMiscBlocks {
    private static final List<RegisteredBlock<? extends Block>> ALL = new ArrayList<>();

    // N10: Decorative panels (6 types)
    public static RegisteredBlock<PanelBlock> PANEL_WOOD;
    public static RegisteredBlock<PanelBlock> PANEL_CONCRETE;
    public static RegisteredBlock<PanelBlock> PANEL_CFOAM;
    public static RegisteredBlock<PanelBlock> PANEL_ASPHALT;
    public static RegisteredBlock<PanelBlock> PANEL_COLORED_GRAY;
    public static RegisteredBlock<PanelBlock> PANEL_COLORED_BLACK;

    // N9: Crafting Tables (2)
    public static RegisteredBlock<AdvancedCraftingTableBlock> ADVANCED_CRAFTING_TABLE;
    public static final List<RegisteredBlock<AdvancedCraftingTableBlock>> ADVANCED_CRAFTING_TABLES = new ArrayList<>();

    public static AdvancedCraftingTableBlock advancedCraftingTable(gregtech.api.material.GTMaterial material) {
        for (var table : ADVANCED_CRAFTING_TABLES) if (table.get().material() == material) return table.get();
        throw new IllegalArgumentException("No crafting table for " + material.getName());
    }

    public static RegisteredBlock<ChargingCraftingTableBlock> CHARGING_CRAFTING_TABLE;
    public static final List<RegisteredBlock<ChargingCraftingTableBlock>> CHARGING_CRAFTING_TABLES = new ArrayList<>();

    public static ChargingCraftingTableBlock chargingCraftingTable(gregtech.api.material.GTMaterial material) {
        for (var table : CHARGING_CRAFTING_TABLES) if (table.get().material() == material) return table.get();
        throw new IllegalArgumentException("No charging crafting table for " + material.getName());
    }

    // N8: Filters (4)
    public static RegisteredBlock<FilterBlock> FILTER_ITEMS;
    public static RegisteredBlock<FilterBlock> FILTER_FLUIDS;
    public static RegisteredBlock<FilterBlock> FILTER_ITEMS_FLUIDS;
    public static RegisteredBlock<FilterBlock> FILTER_OREDICT;

    // N8: Extenders (4)
    public static RegisteredBlock<Block> EXTENDER_BASIC;
    public static RegisteredBlock<Block> EXTENDER_ADVANCED;
    public static RegisteredBlock<Block> EXTENDER_ELITE;
    public static RegisteredBlock<Block> EXTENDER_WIRELESS;
    public static final java.util.Map<gregtech.content.logistics.ExtenderSpec, RegisteredBlock<SourceExtenderBlock>>
            SOURCE_EXTENDERS = new java.util.EnumMap<>(gregtech.content.logistics.ExtenderSpec.class);

    public static Block[] extenderBlocks() {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(
                                EXTENDER_BASIC.get(),
                                EXTENDER_ADVANCED.get(),
                                EXTENDER_ELITE.get(),
                                EXTENDER_WIRELESS.get()),
                        SOURCE_EXTENDERS.values().stream().map(RegisteredBlock::get))
                .toArray(Block[]::new);
    }

    // N8: Auto Tools (10: 6 igniters + 4 hammers)
    public static RegisteredBlock<AutoIgniterBlock> AUTO_IGNITER_STEEL;
    public static RegisteredBlock<AutoIgniterBlock> AUTO_IGNITER_ALUMINIUM;
    public static RegisteredBlock<AutoIgniterBlock> AUTO_IGNITER_STAINLESS;
    public static RegisteredBlock<AutoIgniterBlock> AUTO_IGNITER_TITANIUM;
    public static RegisteredBlock<AutoIgniterBlock> AUTO_IGNITER_TUNGSTEN;
    public static RegisteredBlock<AutoIgniterBlock> AUTO_IGNITER_ULTIMET;
    public static RegisteredBlock<AutoHammerBlock> AUTO_HAMMER_STEEL;
    public static RegisteredBlock<AutoHammerBlock> AUTO_HAMMER_BRONZE;
    public static RegisteredBlock<AutoHammerBlock> AUTO_HAMMER_TITANIUM;
    public static RegisteredBlock<AutoHammerBlock> AUTO_HAMMER_TUNGSTEN_STEEL;

    // N5: Long Distance Transport (7)
    public static RegisteredBlock<LongDistPipeBlock> LONG_DIST_PIPE;
    public static RegisteredBlock<LongDistPipeBlock> LONG_DIST_WIRE;
    public static RegisteredBlock<LongDistEndpointBlock> LONG_DIST_ENDPOINT_ITEM;
    public static RegisteredBlock<LongDistEndpointBlock> LONG_DIST_ENDPOINT_FLUID;
    public static RegisteredBlock<LongDistanceTransformerBlock> LONG_DIST_TRANSFORMER_ULV;
    public static RegisteredBlock<LongDistanceTransformerBlock> LONG_DIST_TRANSFORMER_LV;
    public static RegisteredBlock<LongDistanceTransformerBlock> LONG_DIST_TRANSFORMER_MV;
    public static RegisteredBlock<LongDistanceTransformerBlock> LONG_DIST_TRANSFORMER_ZPM;
    public static RegisteredBlock<LongDistanceTransformerBlock> LONG_DIST_TRANSFORMER_UV;

    private GTMiscBlocks() {}

    public static List<RegisteredBlock<? extends Block>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static List<RegisteredBlock<? extends Block>> panels() {
        return List.of(PANEL_WOOD, PANEL_CONCRETE, PANEL_CFOAM, PANEL_ASPHALT, PANEL_COLORED_GRAY, PANEL_COLORED_BLACK);
    }

    public static List<RegisteredBlock<? extends Block>> autoTools() {
        return List.of(
                AUTO_IGNITER_STEEL,
                AUTO_IGNITER_ALUMINIUM,
                AUTO_IGNITER_STAINLESS,
                AUTO_IGNITER_TITANIUM,
                AUTO_IGNITER_TUNGSTEN,
                AUTO_IGNITER_ULTIMET,
                AUTO_HAMMER_STEEL,
                AUTO_HAMMER_BRONZE,
                AUTO_HAMMER_TITANIUM,
                AUTO_HAMMER_TUNGSTEN_STEEL);
    }

    private static <T extends Block> RegisteredBlock<T> reg(
            ContentRegistrar registrar, String id, Supplier<T> blockSupplier) {
        RegisteredBlock<T> ro = registrar.block(
                REGISTRY_NAMESPACE,
                id,
                blockSupplier,
                block -> gregtech.content.logistics.LongDistanceCatalog.find(id) != null
                        ? new gregtech.content.item.LongDistanceBlockItem(
                                block,
                                new Item.Properties()
                                        .stacksTo(gregtech.content.logistics.LongDistanceCatalog.get(id)
                                                .stackLimit()),
                                gregtech.content.logistics.LongDistanceCatalog.get(id))
                        : block instanceof PanelBlock panel
                                ? new gregtech.content.item.PanelBlockItem(panel, new Item.Properties())
                                : new BlockItem(
                                        block,
                                        new Item.Properties()
                                                .stacksTo(
                                                        block instanceof FilterBlock
                                                                        || block
                                                                                instanceof AdvancedCraftingTableBlock
                                                                ? 16
                                                                : 64)));
        ALL.add(ro);
        return ro;
    }

    public static void registerAll(ContentRegistrar registrar) {
        // N10: Panels
        PANEL_WOOD = reg(registrar, "panel_wood", () -> panel("wood"));
        PANEL_CONCRETE = reg(registrar, "panel_concrete", () -> panel("concrete"));
        PANEL_CFOAM = reg(registrar, "panel_cfoam", () -> panel("cfoam"));
        PANEL_ASPHALT = reg(registrar, "panel_asphalt", () -> panel("asphalt"));
        PANEL_COLORED_GRAY = reg(registrar, "panel_colored_gray", () -> panel("colored_gray"));
        PANEL_COLORED_BLACK = reg(registrar, "panel_colored_black", () -> panel("colored_black"));

        for (var spec : gregtech.content.transport.PanelCatalog.ALL)
            if (!java.util.Set.of("wood", "concrete", "cfoam", "asphalt", "colored_gray", "colored_black")
                    .contains(spec.material()))
                registrar.blockItem(
                        REGISTRY_NAMESPACE,
                        spec.id(),
                        () -> new gregtech.content.item.PanelItem(spec, new Item.Properties()));

        // N9: Crafting Tables
        for (var spec : GTStorageMetals.ALL) {
            String id = spec.suffix().equals("steel")
                    ? "advanced_crafting_table"
                    : "advanced_crafting_table_" + spec.suffix();
            var table = reg(registrar, 
                    id,
                    () -> new AdvancedCraftingTableBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()));
            ADVANCED_CRAFTING_TABLES.add(table);
            if (spec.suffix().equals("steel")) ADVANCED_CRAFTING_TABLE = table;
        }
        for (var spec : GTStorageMetals.ALL) {
            String id = spec.suffix().equals("steel")
                    ? "charging_crafting_table"
                    : "charging_crafting_table_" + spec.suffix();
            var table = reg(registrar, 
                    id,
                    () -> new ChargingCraftingTableBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()));
            CHARGING_CRAFTING_TABLES.add(table);
            if (spec.suffix().equals("steel")) CHARGING_CRAFTING_TABLE = table;
        }

        // N8: Filters
        FILTER_ITEMS = reg(registrar, "filter_items", () -> new FilterBlock("items", props(MapColor.METAL, 6f)));
        FILTER_FLUIDS = reg(registrar, "filter_fluids", () -> new FilterBlock("fluids", props(MapColor.METAL, 6f)));
        FILTER_ITEMS_FLUIDS = reg(
                registrar, "filter_items_fluids", () -> new FilterBlock("items_fluids", props(MapColor.METAL, 6f)));
        FILTER_OREDICT = reg(registrar, "filter_oredict", () -> new FilterBlock("oredict", props(MapColor.METAL, 6f)));

        // N8: Extenders
        EXTENDER_BASIC = RegisteredBlock.reference("inventory_tank_extender");
        EXTENDER_ADVANCED = RegisteredBlock.reference("inventory_tank_extender");
        EXTENDER_ELITE = RegisteredBlock.reference("inventory_tank_extender");
        EXTENDER_WIRELESS = RegisteredBlock.reference("inventory_tank_extender");
        for (var spec : gregtech.content.logistics.ExtenderSpec.values()) {
            var block = registrar.block(
                    REGISTRY_NAMESPACE,
                    spec.id,
                    () -> new SourceExtenderBlock(spec, props(MapColor.METAL, 6f)),
                    bare -> new BlockItem(bare, new Item.Properties().stacksTo(16)));
            SOURCE_EXTENDERS.put(spec, block);
            ALL.add(block);
        }

        // N8: Auto Igniters (6 tiers)
        AUTO_IGNITER_STEEL = reg(registrar, 
                "auto_igniter_steel",
                () -> new AutoIgniterBlock(
                        props(
                                MapColor.METAL,
                                gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_steel")
                                        .hardness()),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_steel")
                                .input(),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_steel")
                                .quality()));
        AUTO_IGNITER_ALUMINIUM = reg(registrar, 
                "auto_igniter_aluminium",
                () -> new AutoIgniterBlock(
                        props(
                                MapColor.METAL,
                                gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_aluminium")
                                        .hardness()),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_aluminium")
                                .input(),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_aluminium")
                                .quality()));
        AUTO_IGNITER_STAINLESS = reg(registrar, 
                "auto_igniter_stainless",
                () -> new AutoIgniterBlock(
                        props(
                                MapColor.METAL,
                                gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_stainless")
                                        .hardness()),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_stainless")
                                .input(),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_stainless")
                                .quality()));
        AUTO_IGNITER_TITANIUM = reg(registrar, 
                "auto_igniter_titanium",
                () -> new AutoIgniterBlock(
                        props(
                                MapColor.METAL,
                                gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_titanium")
                                        .hardness()),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_titanium")
                                .input(),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_titanium")
                                .quality()));
        AUTO_IGNITER_TUNGSTEN = reg(registrar, 
                "auto_igniter_tungsten",
                () -> new AutoIgniterBlock(
                        props(
                                MapColor.METAL,
                                gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_tungsten")
                                        .hardness()),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_tungsten")
                                .input(),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_tungsten")
                                .quality()));
        AUTO_IGNITER_ULTIMET = reg(registrar, 
                "auto_igniter_ultimet",
                () -> new AutoIgniterBlock(
                        props(
                                MapColor.METAL,
                                gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_ultimet")
                                        .hardness()),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_ultimet")
                                .input(),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_ultimet")
                                .quality()));

        // N8: Auto Hammers (4 tiers)
        AUTO_HAMMER_STEEL = reg(registrar, 
                "auto_hammer_steel",
                () -> new AutoHammerBlock(
                        props(
                                MapColor.METAL,
                                gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_steel")
                                        .hardness()),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_steel")
                                .input(),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_steel")
                                .quality()));
        AUTO_HAMMER_BRONZE = reg(registrar, 
                "auto_hammer_bronze",
                () -> new AutoHammerBlock(
                        props(
                                MapColor.METAL,
                                gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_bronze")
                                        .hardness()),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_bronze")
                                .input(),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_bronze")
                                .quality()));
        AUTO_HAMMER_TITANIUM = reg(registrar, 
                "auto_hammer_titanium",
                () -> new AutoHammerBlock(
                        props(
                                MapColor.METAL,
                                gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_titanium")
                                        .hardness()),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_titanium")
                                .input(),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_titanium")
                                .quality()));
        AUTO_HAMMER_TUNGSTEN_STEEL = reg(registrar, 
                "auto_hammer_tungsten_steel",
                () -> new AutoHammerBlock(
                        props(
                                MapColor.METAL,
                                gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_tungsten_steel")
                                        .hardness()),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_tungsten_steel")
                                .input(),
                        gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_tungsten_steel")
                                .quality()));

        // The common catalog owns every original line and endpoint.
        LONG_DIST_PIPE = RegisteredBlock.reference("long_dist_pipe_fluid");
        LONG_DIST_WIRE = RegisteredBlock.reference("long_dist_wire_ev");
        LONG_DIST_ENDPOINT_ITEM = RegisteredBlock.reference("long_dist_endpoint_item");
        LONG_DIST_ENDPOINT_FLUID = RegisteredBlock.reference("long_dist_endpoint_fluid");
        LONG_DIST_TRANSFORMER_ULV = RegisteredBlock.reference("long_dist_transformer_ulv");
        LONG_DIST_TRANSFORMER_LV = RegisteredBlock.reference("long_dist_transformer_lv");
        LONG_DIST_TRANSFORMER_MV = RegisteredBlock.reference("long_dist_transformer_mv");
        LONG_DIST_TRANSFORMER_ZPM = RegisteredBlock.reference("long_dist_transformer_zpm");
        LONG_DIST_TRANSFORMER_UV = RegisteredBlock.reference("long_dist_transformer_uv");
        for (var entry : GTLongDistance.all())
            ALL.add(RegisteredBlock.reference(entry.getId().getPath()));
    }

    private static PanelBlock panel(String material) {
        var s = gregtech.content.transport.PanelCatalog.get(material);
        return new PanelBlock(
                s.material(),
                s.tint(),
                props(
                        switch (s.mapColor()) {
                            case "WOOD" -> MapColor.WOOD;
                            case "STONE" -> MapColor.STONE;
                            case "WOOL" -> MapColor.WOOL;
                            case "COLOR_BLACK" -> MapColor.COLOR_BLACK;
                            default -> MapColor.COLOR_GRAY;
                        },
                        s.hardness()));
    }

    private static BlockBehaviour.Properties longDistanceProperties(String id) {
        var spec = gregtech.content.logistics.LongDistanceCatalog.get(id);
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(spec.hardness(), spec.resistance())
                .requiresCorrectToolForDrops()
                .sound(
                        spec.kind().equals("WIRE")
                                ? net.minecraft.world.level.block.SoundType.WOOL
                                : net.minecraft.world.level.block.SoundType.METAL);
    }

    private static BlockBehaviour.Properties props(MapColor color, float hardness) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(hardness, hardness)
                .requiresCorrectToolForDrops();
    }
}
