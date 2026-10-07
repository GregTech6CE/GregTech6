package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.misc.*;
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
    public static AdvancedCraftingTableBlock advancedCraftingTable(com.gregtech.gregtech.api.material.GTMaterial material){
        for(var table:ADVANCED_CRAFTING_TABLES)if(table.get().material()==material)return table.get();
        throw new IllegalArgumentException("No crafting table for "+material.getName());
    }
    public static RegistryObject<ChargingCraftingTableBlock> CHARGING_CRAFTING_TABLE;
    public static final List<RegistryObject<ChargingCraftingTableBlock>> CHARGING_CRAFTING_TABLES=new ArrayList<>();
    public static ChargingCraftingTableBlock chargingCraftingTable(com.gregtech.gregtech.api.material.GTMaterial material){
        for(var table:CHARGING_CRAFTING_TABLES)if(table.get().material()==material)return table.get();
        throw new IllegalArgumentException("No charging crafting table for "+material.getName());
    }

    // N8: Filters (4)
    public static RegistryObject<FilterBlock> FILTER_ITEMS;
    public static RegistryObject<FilterBlock> FILTER_FLUIDS;
    public static RegistryObject<FilterBlock> FILTER_ITEMS_FLUIDS;
    public static RegistryObject<FilterBlock> FILTER_OREDICT;

    // N8: Extenders (4)
    public static RegistryObject<ExtenderBlock> EXTENDER_BASIC;
    public static RegistryObject<ExtenderBlock> EXTENDER_ADVANCED;
    public static RegistryObject<ExtenderBlock> EXTENDER_ELITE;
    public static RegistryObject<ExtenderBlock> EXTENDER_WIRELESS;
    public static final java.util.Map<com.gregtech.gregtech.content.logistics.ExtenderSpec,RegistryObject<SourceExtenderBlock>> SOURCE_EXTENDERS=new java.util.EnumMap<>(com.gregtech.gregtech.content.logistics.ExtenderSpec.class);
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
        GTBlocks.BLOCK_ITEMS.register(id, () -> com.gregtech.gregtech.content.logistics.LongDistanceCatalog.find(id)!=null
                ? new com.gregtech.gregtech.item.LongDistanceBlockItem(ro.get(),new Item.Properties().stacksTo(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get(id).stackLimit()),com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get(id))
                : ro.get() instanceof PanelBlock panel ? new com.gregtech.gregtech.item.PanelBlockItem(panel,new Item.Properties()) : new BlockItem(ro.get(), new Item.Properties().stacksTo(ro.get() instanceof FilterBlock || ro.get() instanceof AdvancedCraftingTableBlock?16:64)));
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

        for(var spec:com.gregtech.gregtech.content.transport.PanelCatalog.ALL)
            if(!java.util.Set.of("wood","concrete","cfoam","asphalt","colored_gray","colored_black").contains(spec.material()))GTBlocks.BLOCK_ITEMS.register(spec.id(),()->new com.gregtech.gregtech.item.PanelItem(spec,new Item.Properties()));

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
        EXTENDER_BASIC = reg("extender_basic", () -> new ExtenderBlock(props(MapColor.METAL, 4f)));
        EXTENDER_ADVANCED = reg("extender_advanced", () -> new ExtenderBlock(props(MapColor.METAL, 6f)));
        EXTENDER_ELITE = reg("extender_elite", () -> new ExtenderBlock(props(MapColor.METAL, 8f)));
        EXTENDER_WIRELESS = reg("extender_wireless", () -> new ExtenderBlock(props(MapColor.METAL, 10f)));
        for(var spec:com.gregtech.gregtech.content.logistics.ExtenderSpec.values()){
            var block=GTBlocks.BLOCKS.register(spec.id,()->new SourceExtenderBlock(spec,props(MapColor.METAL,6f)));
            SOURCE_EXTENDERS.put(spec,block);ALL.add(block);
            GTBlocks.BLOCK_ITEMS.register(spec.id,()->new BlockItem(block.get(),new Item.Properties().stacksTo(16)));
        }

        // N8: Auto Igniters (6 tiers)
        AUTO_IGNITER_STEEL = reg("auto_igniter_steel", () -> new AutoIgniterBlock(props(MapColor.METAL, com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_steel").hardness()), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_steel").input(), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_steel").quality()));
        AUTO_IGNITER_ALUMINIUM = reg("auto_igniter_aluminium", () -> new AutoIgniterBlock(props(MapColor.METAL, com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_aluminium").hardness()), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_aluminium").input(), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_aluminium").quality()));
        AUTO_IGNITER_STAINLESS = reg("auto_igniter_stainless", () -> new AutoIgniterBlock(props(MapColor.METAL, com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_stainless").hardness()), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_stainless").input(), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_stainless").quality()));
        AUTO_IGNITER_TITANIUM = reg("auto_igniter_titanium", () -> new AutoIgniterBlock(props(MapColor.METAL, com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_titanium").hardness()), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_titanium").input(), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_titanium").quality()));
        AUTO_IGNITER_TUNGSTEN = reg("auto_igniter_tungsten", () -> new AutoIgniterBlock(props(MapColor.METAL, com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_tungsten").hardness()), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_tungsten").input(), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_tungsten").quality()));
        AUTO_IGNITER_ULTIMET = reg("auto_igniter_ultimet", () -> new AutoIgniterBlock(props(MapColor.METAL, com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_ultimet").hardness()), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_ultimet").input(), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_igniter_ultimet").quality()));

        // N8: Auto Hammers (4 tiers)
        AUTO_HAMMER_STEEL = reg("auto_hammer_steel", () -> new AutoHammerBlock(props(MapColor.METAL, com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_steel").hardness()), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_steel").input(), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_steel").quality()));
        AUTO_HAMMER_ALUMINIUM = reg("auto_hammer_aluminium", () -> new AutoHammerBlock(props(MapColor.METAL, com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_aluminium").hardness()), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_aluminium").input(), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_aluminium").quality()));
        AUTO_HAMMER_TITANIUM = reg("auto_hammer_titanium", () -> new AutoHammerBlock(props(MapColor.METAL, com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_titanium").hardness()), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_titanium").input(), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_titanium").quality()));
        AUTO_HAMMER_TUNGSTEN = reg("auto_hammer_tungsten", () -> new AutoHammerBlock(props(MapColor.METAL, com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_tungsten").hardness()), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_tungsten").input(), com.gregtech.gregtech.content.tool.AutomaticToolRules.profile("auto_hammer_tungsten").quality()));

        // N5: Long Distance Transport
        reg("long_dist_wire_lead", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire_lead"),longDistanceProperties("long_dist_wire_lead")));
        reg("long_dist_wire_gold", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire_gold"),longDistanceProperties("long_dist_wire_gold")));
        reg("long_dist_wire_electrum", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire_electrum"),longDistanceProperties("long_dist_wire_electrum")));
        reg("long_dist_wire_blue_alloy", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire_blue_alloy"),longDistanceProperties("long_dist_wire_blue_alloy")));
        reg("long_dist_wire_electrotine_alloy", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire_electrotine_alloy"),longDistanceProperties("long_dist_wire_electrotine_alloy")));
        reg("long_dist_wire_aluminium", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire_aluminium"),longDistanceProperties("long_dist_wire_aluminium")));
        reg("long_dist_wire_tungsten", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire_tungsten"),longDistanceProperties("long_dist_wire_tungsten")));
        reg("long_dist_wire_tungsten_steel", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire_tungsten_steel"),longDistanceProperties("long_dist_wire_tungsten_steel")));
        reg("long_dist_wire_platinum", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire_platinum"),longDistanceProperties("long_dist_wire_platinum")));
        reg("long_dist_wire_naquadah", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire_naquadah"),longDistanceProperties("long_dist_wire_naquadah")));
        reg("long_dist_wire_graphene", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire_graphene"),longDistanceProperties("long_dist_wire_graphene")));
        reg("long_dist_pipe_tungsten", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_pipe_tungsten"),longDistanceProperties("long_dist_pipe_tungsten")));
        reg("long_dist_pipe_adamantium", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_pipe_adamantium"),longDistanceProperties("long_dist_pipe_adamantium")));
        reg("long_dist_pipe_draconium", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_pipe_draconium"),longDistanceProperties("long_dist_pipe_draconium")));
        LONG_DIST_PIPE = reg("long_dist_pipe", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_pipe"), longDistanceProperties("long_dist_pipe")));
        LONG_DIST_WIRE = reg("long_dist_wire", () -> new LongDistPipeBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get("long_dist_wire"), longDistanceProperties("long_dist_wire")));
        LONG_DIST_ENDPOINT_ITEM = reg("long_dist_endpoint_item", () -> new LongDistEndpointBlock(false, longDistanceProperties("long_dist_endpoint_item")));
        LONG_DIST_ENDPOINT_FLUID = reg("long_dist_endpoint_fluid", () -> new LongDistEndpointBlock(true, longDistanceProperties("long_dist_endpoint_fluid")));
        LONG_DIST_TRANSFORMER_ULV = reg("long_dist_transformer_ulv", () -> new LongDistanceTransformerBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.voltage("long_dist_transformer_ulv"), props(MapColor.METAL, 4f)));
        LONG_DIST_TRANSFORMER_LV = reg("long_dist_transformer_lv", () -> new LongDistanceTransformerBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.voltage("long_dist_transformer_lv"), props(MapColor.METAL, 4f)));
        LONG_DIST_TRANSFORMER_MV = reg("long_dist_transformer_mv", () -> new LongDistanceTransformerBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.voltage("long_dist_transformer_mv"), props(MapColor.METAL, 4f)));
        LONG_DIST_TRANSFORMER_ZPM = reg("long_dist_transformer_zpm", () -> new LongDistanceTransformerBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.voltage("long_dist_transformer_zpm"), props(MapColor.METAL, 4f)));
        LONG_DIST_TRANSFORMER_UV = reg("long_dist_transformer_uv", () -> new LongDistanceTransformerBlock(com.gregtech.gregtech.content.logistics.LongDistanceCatalog.voltage("long_dist_transformer_uv"), props(MapColor.METAL, 4f)));
    }

    private static PanelBlock panel(String material){var s=com.gregtech.gregtech.content.transport.PanelCatalog.get(material);return new PanelBlock(s.material(),s.tint(),props(switch(s.mapColor()){case "WOOD"->MapColor.WOOD;case "STONE"->MapColor.STONE;case "WOOL"->MapColor.WOOL;case "COLOR_BLACK"->MapColor.COLOR_BLACK;default->MapColor.COLOR_GRAY;},s.hardness()));}

    private static BlockBehaviour.Properties longDistanceProperties(String id) {
        var spec=com.gregtech.gregtech.content.logistics.LongDistanceCatalog.get(id);
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(spec.hardness(),spec.resistance())
                .requiresCorrectToolForDrops().sound(spec.kind().equals("WIRE")?net.minecraft.world.level.block.SoundType.WOOL:net.minecraft.world.level.block.SoundType.METAL);
    }
    private static BlockBehaviour.Properties props(MapColor color, float hardness) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(hardness, hardness).requiresCorrectToolForDrops();
    }
}
