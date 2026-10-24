package gregtech.content.registry;

import gregtech.content.block.misc.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Wave 45: Panels, Crafting Tables, Filters, Extenders, Auto Tools, Long Distance Transport. */
public final class GTMiscBlocks {
    private static final List<RegistryObject<? extends Block>> ALL = new ArrayList<>();

    // N10: Decorative panels (6 types)
    public static RegistryObject<PanelBlock> PANEL_WOOD;
    public static RegistryObject<PanelBlock> PANEL_CONCRETE;
    public static RegistryObject<PanelBlock> PANEL_CFOAM;
    public static RegistryObject<PanelBlock> PANEL_ASPHALT;
    public static RegistryObject<PanelBlock> PANEL_COLORED_GRAY;
    public static RegistryObject<PanelBlock> PANEL_COLORED_BLACK;

    // N9: Crafting Tables (2)
    public static RegistryObject<AdvancedCraftingTableBlock> ADVANCED_CRAFTING_TABLE;
    public static final List<RegistryObject<AdvancedCraftingTableBlock>> ADVANCED_CRAFTING_TABLES=new ArrayList<>();
    public static AdvancedCraftingTableBlock advancedCraftingTable(gregtech.internal.material.GTMaterial material){
        for(var table:ADVANCED_CRAFTING_TABLES)if(table.get().material()==material)return table.get();
        throw new IllegalArgumentException("No crafting table for "+material.getName());
    }
    public static RegistryObject<ChargingCraftingTableBlock> CHARGING_CRAFTING_TABLE;
    public static final List<RegistryObject<ChargingCraftingTableBlock>> CHARGING_CRAFTING_TABLES=new ArrayList<>();
    public static ChargingCraftingTableBlock chargingCraftingTable(gregtech.internal.material.GTMaterial material){
        for(var table:CHARGING_CRAFTING_TABLES)if(table.get().material()==material)return table.get();
        throw new IllegalArgumentException("No charging crafting table for "+material.getName());
    }

    // N8: Filters (4)
    public static RegistryObject<FilterBlock> FILTER_ITEMS;
    public static RegistryObject<FilterBlock> FILTER_FLUIDS;
    public static RegistryObject<FilterBlock> FILTER_ITEMS_FLUIDS;
    public static RegistryObject<FilterBlock> FILTER_OREDICT;

    // N8: Extenders (4)
    public static RegistryObject<Block> EXTENDER_BASIC;
    public static RegistryObject<Block> EXTENDER_ADVANCED;
    public static RegistryObject<Block> EXTENDER_ELITE;
    public static RegistryObject<Block> EXTENDER_WIRELESS;
    public static final java.util.Map<gregtech.content.logistics.ExtenderSpec,RegistryObject<SourceExtenderBlock>> SOURCE_EXTENDERS=new java.util.EnumMap<>(gregtech.content.logistics.ExtenderSpec.class);
    public static Block[] extenderBlocks(){
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(EXTENDER_BASIC.get(),EXTENDER_ADVANCED.get(),EXTENDER_ELITE.get(),EXTENDER_WIRELESS.get()),SOURCE_EXTENDERS.values().stream().map(RegistryObject::get)).toArray(Block[]::new);
    }

    // N8: Auto Tools (10: 6 igniters + 4 hammers)
    public static RegistryObject<AutoIgniterBlock> AUTO_IGNITER_STEEL;
    public static RegistryObject<AutoIgniterBlock> AUTO_IGNITER_ALUMINIUM;
    public static RegistryObject<AutoIgniterBlock> AUTO_IGNITER_STAINLESS;
    public static RegistryObject<AutoIgniterBlock> AUTO_IGNITER_TITANIUM;
    public static RegistryObject<AutoIgniterBlock> AUTO_IGNITER_TUNGSTEN;
    public static RegistryObject<AutoIgniterBlock> AUTO_IGNITER_ULTIMET;
    public static RegistryObject<AutoHammerBlock> AUTO_HAMMER_STEEL;
    public static RegistryObject<AutoHammerBlock> AUTO_HAMMER_ALUMINIUM;
    public static RegistryObject<AutoHammerBlock> AUTO_HAMMER_TITANIUM;
    public static RegistryObject<AutoHammerBlock> AUTO_HAMMER_TUNGSTEN;

    // N5: Long Distance Transport (7)
    public static RegistryObject<LongDistPipeBlock> LONG_DIST_PIPE;
    public static RegistryObject<LongDistPipeBlock> LONG_DIST_WIRE;
    public static RegistryObject<LongDistEndpointBlock> LONG_DIST_ENDPOINT_ITEM;
    public static RegistryObject<LongDistEndpointBlock> LONG_DIST_ENDPOINT_FLUID;
    public static RegistryObject<LongDistanceTransformerBlock> LONG_DIST_TRANSFORMER_ULV;
    public static RegistryObject<LongDistanceTransformerBlock> LONG_DIST_TRANSFORMER_LV;
    public static RegistryObject<LongDistanceTransformerBlock> LONG_DIST_TRANSFORMER_MV;
    public static RegistryObject<LongDistanceTransformerBlock> LONG_DIST_TRANSFORMER_ZPM;
    public static RegistryObject<LongDistanceTransformerBlock> LONG_DIST_TRANSFORMER_UV;

    private GTMiscBlocks() {}

    public static List<RegistryObject<? extends Block>> all() { return Collections.unmodifiableList(ALL); }

    public static List<RegistryObject<? extends Block>> panels() {
        return List.of(PANEL_WOOD, PANEL_CONCRETE, PANEL_CFOAM, PANEL_ASPHALT, PANEL_COLORED_GRAY, PANEL_COLORED_BLACK);
    }

    public static List<RegistryObject<? extends Block>> autoTools() {
        return List.of(AUTO_IGNITER_STEEL, AUTO_IGNITER_ALUMINIUM, AUTO_IGNITER_STAINLESS,
                AUTO_IGNITER_TITANIUM, AUTO_IGNITER_TUNGSTEN, AUTO_IGNITER_ULTIMET,
                AUTO_HAMMER_STEEL, AUTO_HAMMER_ALUMINIUM, AUTO_HAMMER_TITANIUM, AUTO_HAMMER_TUNGSTEN);
    }

    private static <T extends Block> RegistryObject<T> reg(String id, Supplier<T> blockSupplier) {
        RegistryObject<T> ro = GTBlocks.BLOCKS.register(id, blockSupplier);
        ALL.add(ro);
        GTBlocks.BLOCK_ITEMS.register(id, () -> gregtech.content.logistics.LongDistanceCatalog.find(id)!=null
                ? new gregtech.content.item.LongDistanceBlockItem(ro.get(),new Item.Properties().stacksTo(gregtech.content.logistics.LongDistanceCatalog.get(id).stackLimit()),gregtech.content.logistics.LongDistanceCatalog.get(id))
                : ro.get() instanceof PanelBlock panel ? new gregtech.content.item.PanelBlockItem(panel,new Item.Properties()) : new BlockItem(ro.get(), new Item.Properties().stacksTo(ro.get() instanceof FilterBlock || ro.get() instanceof AdvancedCraftingTableBlock?16:64)));
        return ro;
    }

    public static void registerAll() {
        // N10: Panels
        PANEL_WOOD = reg("panel_wood", () -> panel("wood"));
        PANEL_CONCRETE = reg("panel_concrete", () -> panel("concrete"));
        PANEL_CFOAM = reg("panel_cfoam", () -> panel("cfoam"));
        PANEL_ASPHALT = reg("panel_asphalt", () -> panel("asphalt"));
        PANEL_COLORED_GRAY = reg("panel_colored_gray", () -> panel("colored_gray"));
        PANEL_COLORED_BLACK = reg("panel_colored_black", () -> panel("colored_black"));

        for(var spec:gregtech.content.transport.PanelCatalog.ALL)
            if(!java.util.Set.of("wood","concrete","cfoam","asphalt","colored_gray","colored_black").contains(spec.material()))GTBlocks.BLOCK_ITEMS.register(spec.id(),()->new gregtech.content.item.PanelItem(spec,new Item.Properties()));

        // N9: Crafting Tables
        for(var spec:GTStorageMetals.ALL){
            String id=spec.suffix().equals("steel")?"advanced_crafting_table":"advanced_crafting_table_"+spec.suffix();
            var table=reg(id,()->new AdvancedCraftingTableBlock(spec.material(),BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(),spec.resistance()).requiresCorrectToolForDrops()));
            ADVANCED_CRAFTING_TABLES.add(table);if(spec.suffix().equals("steel"))ADVANCED_CRAFTING_TABLE=table;
        }
        for(var spec:GTStorageMetals.ALL){
            String id=spec.suffix().equals("steel")?"charging_crafting_table":"charging_crafting_table_"+spec.suffix();
            var table=reg(id,()->new ChargingCraftingTableBlock(spec.material(),BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(),spec.resistance()).requiresCorrectToolForDrops()));
            CHARGING_CRAFTING_TABLES.add(table);if(spec.suffix().equals("steel"))CHARGING_CRAFTING_TABLE=table;
        }

        // N8: Filters
        FILTER_ITEMS = reg("filter_items", () -> new FilterBlock("items", props(MapColor.METAL, 6f)));
        FILTER_FLUIDS = reg("filter_fluids", () -> new FilterBlock("fluids", props(MapColor.METAL, 6f)));
        FILTER_ITEMS_FLUIDS = reg("filter_items_fluids", () -> new FilterBlock("items_fluids", props(MapColor.METAL, 6f)));
        FILTER_OREDICT = reg("filter_oredict", () -> new FilterBlock("oredict", props(MapColor.METAL, 6f)));

        // N8: Extenders
        EXTENDER_BASIC = RegistryObject.create(gregtech.GregTech.id("inventory_tank_extender"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        EXTENDER_ADVANCED = RegistryObject.create(gregtech.GregTech.id("inventory_tank_extender"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        EXTENDER_ELITE = RegistryObject.create(gregtech.GregTech.id("inventory_tank_extender"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        EXTENDER_WIRELESS = RegistryObject.create(gregtech.GregTech.id("inventory_tank_extender"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        for(var spec:gregtech.content.logistics.ExtenderSpec.values()){
            var block=GTBlocks.BLOCKS.register(spec.id,()->new SourceExtenderBlock(spec,props(MapColor.METAL,6f)));
            SOURCE_EXTENDERS.put(spec,block);ALL.add(block);
            GTBlocks.BLOCK_ITEMS.register(spec.id,()->new BlockItem(block.get(),new Item.Properties().stacksTo(16)));
        }

        // N8: Auto Igniters (6 tiers)
        AUTO_IGNITER_STEEL = reg("auto_igniter_steel", () -> new AutoIgniterBlock(props(MapColor.METAL, gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_steel").hardness()), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_steel").input(), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_steel").quality()));
        AUTO_IGNITER_ALUMINIUM = reg("auto_igniter_aluminium", () -> new AutoIgniterBlock(props(MapColor.METAL, gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_aluminium").hardness()), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_aluminium").input(), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_aluminium").quality()));
        AUTO_IGNITER_STAINLESS = reg("auto_igniter_stainless", () -> new AutoIgniterBlock(props(MapColor.METAL, gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_stainless").hardness()), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_stainless").input(), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_stainless").quality()));
        AUTO_IGNITER_TITANIUM = reg("auto_igniter_titanium", () -> new AutoIgniterBlock(props(MapColor.METAL, gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_titanium").hardness()), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_titanium").input(), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_titanium").quality()));
        AUTO_IGNITER_TUNGSTEN = reg("auto_igniter_tungsten", () -> new AutoIgniterBlock(props(MapColor.METAL, gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_tungsten").hardness()), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_tungsten").input(), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_tungsten").quality()));
        AUTO_IGNITER_ULTIMET = reg("auto_igniter_ultimet", () -> new AutoIgniterBlock(props(MapColor.METAL, gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_ultimet").hardness()), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_ultimet").input(), gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_ultimet").quality()));

        // N8: Auto Hammers (4 tiers)
        AUTO_HAMMER_STEEL = reg("auto_hammer_steel", () -> new AutoHammerBlock(props(MapColor.METAL, gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_steel").hardness()), gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_steel").input(), gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_steel").quality()));
        AUTO_HAMMER_ALUMINIUM = reg("auto_hammer_aluminium", () -> new AutoHammerBlock(props(MapColor.METAL, gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_aluminium").hardness()), gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_aluminium").input(), gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_aluminium").quality()));
        AUTO_HAMMER_TITANIUM = reg("auto_hammer_titanium", () -> new AutoHammerBlock(props(MapColor.METAL, gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_titanium").hardness()), gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_titanium").input(), gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_titanium").quality()));
        AUTO_HAMMER_TUNGSTEN = reg("auto_hammer_tungsten", () -> new AutoHammerBlock(props(MapColor.METAL, gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_tungsten").hardness()), gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_tungsten").input(), gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_tungsten").quality()));

        // The common catalog owns every original line and endpoint.
        GTLongDistance.initialize();
        LONG_DIST_PIPE = RegistryObject.create(gregtech.GregTech.id("long_dist_pipe_fluid"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        LONG_DIST_WIRE = RegistryObject.create(gregtech.GregTech.id("long_dist_wire_ev"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        LONG_DIST_ENDPOINT_ITEM = RegistryObject.create(gregtech.GregTech.id("long_dist_endpoint_item"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        LONG_DIST_ENDPOINT_FLUID = RegistryObject.create(gregtech.GregTech.id("long_dist_endpoint_fluid"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        LONG_DIST_TRANSFORMER_ULV = RegistryObject.create(gregtech.GregTech.id("long_dist_transformer_ulv"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        LONG_DIST_TRANSFORMER_LV = RegistryObject.create(gregtech.GregTech.id("long_dist_transformer_lv"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        LONG_DIST_TRANSFORMER_MV = RegistryObject.create(gregtech.GregTech.id("long_dist_transformer_mv"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        LONG_DIST_TRANSFORMER_ZPM = RegistryObject.create(gregtech.GregTech.id("long_dist_transformer_zpm"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        LONG_DIST_TRANSFORMER_UV = RegistryObject.create(gregtech.GregTech.id("long_dist_transformer_uv"), net.minecraftforge.registries.ForgeRegistries.BLOCKS);
        for(var entry:GTLongDistance.all()) ALL.add(RegistryObject.create(entry.getId(),net.minecraftforge.registries.ForgeRegistries.BLOCKS));


    }

    private static PanelBlock panel(String material){var s=gregtech.content.transport.PanelCatalog.get(material);return new PanelBlock(s.material(),s.tint(),props(switch(s.mapColor()){case "WOOD"->MapColor.WOOD;case "STONE"->MapColor.STONE;case "WOOL"->MapColor.WOOL;case "COLOR_BLACK"->MapColor.COLOR_BLACK;default->MapColor.COLOR_GRAY;},s.hardness()));}

    private static BlockBehaviour.Properties longDistanceProperties(String id) {
        var spec=gregtech.content.logistics.LongDistanceCatalog.get(id);
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(),spec.resistance())
                .requiresCorrectToolForDrops().sound(spec.kind().equals("WIRE")?net.minecraft.world.level.block.SoundType.WOOL:net.minecraft.world.level.block.SoundType.METAL);
    }
    private static BlockBehaviour.Properties props(MapColor color, float hardness) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(hardness, hardness).requiresCorrectToolForDrops();
    }
}
