package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.block.tool.CrankBlock;
import com.gregtech.gregtech.block.tool.ManualToolBlock;
import com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import static com.gregtech.gregtech.registry.GTItems.ITEMS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 manual tool stations (mortar, grind stone, sifting table, crank, ...). */
public final class GTToolBlocks {
    private static final List<RegistryObject<ManualToolBlock>> MANUAL = new ArrayList<>();
    private static final List<RegistryObject<net.minecraft.world.level.block.Block>> ALL_SIMPLE = new ArrayList<>();

    /** GT6's sap bag (multiple-tile 32758) — a real tank block that drains an adjacent tree hole. */
    public static RegistryObject<net.minecraft.world.level.block.Block> SAP_BAG;
    public static RegistryObject<CrankBlock> CRANK;
    public static final List<RegistryObject<com.gregtech.gregtech.block.tool.MaterialAnvilBlock>> ANVILS = new ArrayList<>();
    public static RegistryObject<com.gregtech.gregtech.block.tool.RopeBlock> ROPE;
    public static RegistryObject<com.gregtech.gregtech.block.tool.RopeBlock> ROPE_SILK;
    public static RegistryObject<com.gregtech.gregtech.block.tool.RopeBlock> ROPE_GRASS;
    public static RegistryObject<com.gregtech.gregtech.block.tool.RopeBlock> ROPE_VINE;
    public static RegistryObject<com.gregtech.gregtech.block.tool.RopeBlock> ROPE_PLASTIC;
    public static RegistryObject<com.gregtech.gregtech.block.tool.RopeBlock> ROPE_STEEL;
    public static RegistryObject<com.gregtech.gregtech.block.tool.DynamiteBlock> DYNAMITE;
    public static RegistryObject<com.gregtech.gregtech.block.tool.DynamiteBlock> BOOMSTICK;
    public static RegistryObject<com.gregtech.gregtech.block.tool.DynamiteBlock> DYNAMITE_STRONG;
    public static RegistryObject<com.gregtech.gregtech.block.tool.TapBlock> TAP;
    public static RegistryObject<com.gregtech.gregtech.block.tool.TapBlock> NOZZLE;
    public static RegistryObject<com.gregtech.gregtech.block.tool.DustFunnelBlock> DUST_FUNNEL;
    /** GT6's bumbliary (id 32741): the bee breeding machine. */
    public static RegistryObject<com.gregtech.gregtech.block.tool.BumbliaryBlock> BUMBLIARY;
    /** GT6's advanced bumbliary (id 32007): 20 slots instead of 36. */
    public static RegistryObject<com.gregtech.gregtech.block.tool.BumbliaryBlock> ADVANCED_BUMBLIARY;

    public static final java.util.List<RegistryObject<com.gregtech.gregtech.block.tool.PortableContainerBlock>> PORTABLE = new java.util.ArrayList<>();

    public static final List<RegistryObject<com.gregtech.gregtech.block.tool.ScaffoldBlock>> SCAFFOLDS = new ArrayList<>();
    public static com.gregtech.gregtech.block.tool.ScaffoldBlock scaffold(com.gregtech.gregtech.api.material.GTMaterial material) {
        for (var entry : SCAFFOLDS) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No scaffold for " + material.getName());
    }

    private GTToolBlocks() {}

    public static List<RegistryObject<ManualToolBlock>> manual() {
        return Collections.unmodifiableList(MANUAL);
    }

    /** All simple tool blocks (batch 3: anvil, mixing_bowl, etc.). */
    public static List<RegistryObject<net.minecraft.world.level.block.Block>> simpleBlocks() {
        return Collections.unmodifiableList(ALL_SIMPLE);
    }

    private static void add(String id, ManualToolBlockEntity.Kind kind, BlockBehaviour.Properties props) {        RegistryObject<ManualToolBlock> block = GTBlocks.BLOCKS.register(id, () -> new ManualToolBlock(kind, props));
        MANUAL.add(block);
        // GT6 Misc Tool Blocks (Mortar/Juicer/Grindstone/Sifting Table rows): 16 per stack
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
    }

    /** One of GT6's six ropes: same model, tinted by the rope's material (GT6 does the same). */
    private static RegistryObject<com.gregtech.gregtech.block.tool.RopeBlock> rope(
            String id, com.gregtech.gregtech.api.material.GTMaterial material) {
        RegistryObject<com.gregtech.gregtech.block.tool.RopeBlock> block =
                GTBlocks.BLOCKS.register(id, () -> new com.gregtech.gregtech.block.tool.RopeBlock(
                        BlockBehaviour.Properties.of().mapColor(MapColor.WOOL)
                                .strength(0.5f, 0.5f).sound(SoundType.WOOL).noOcclusion().noCollission(),
                        material.getColor()));
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(64)));
        return block;
    }

    /** One of GT6's three explosives: same model, tinted, with the original's blast quality. */
    private static RegistryObject<com.gregtech.gregtech.block.tool.DynamiteBlock> dynamite(
            String id, float resistance, int fortune, com.gregtech.gregtech.api.material.GTMaterial material) {
        RegistryObject<com.gregtech.gregtech.block.tool.DynamiteBlock> block =
                GTBlocks.BLOCKS.register(id, () -> new com.gregtech.gregtech.block.tool.DynamiteBlock(
                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                                .strength(0.2f, 0.0f).sound(SoundType.GRASS).noOcclusion().instabreak(),
                        resistance, fortune, material.getColor()));
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(64)));
        return block;
    }

    /** GT6 default material tints: stone tools gray, the sifting table steel. */
    public static int tintOf(ManualToolBlockEntity.Kind kind) {        return switch (kind) {
            case MORTAR -> 0xCDCDCD;
            case GRINDSTONE -> 0xB5B5B5;
            case SIFTING -> com.gregtech.gregtech.content.material.Materials.Steel.getColor();
        };
    }

    public static void registerAll() {
        add("mortar_block", ManualToolBlockEntity.Kind.MORTAR, BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE).strength(2.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion());
        for (String material : new String[]{"Netherite", "Sapphire", "Diamond", "Amethyst"}) {
            String id = "mortar_" + material.toLowerCase(java.util.Locale.ROOT);
            var block = GTBlocks.BLOCKS.register(id, () -> new ManualToolBlock(ManualToolBlockEntity.Kind.MORTAR, material,
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1f, 5f).noOcclusion()));
            MANUAL.add(block);
            GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
        }
        add("grindstone_block", ManualToolBlockEntity.Kind.GRINDSTONE, BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE).strength(2.5f, 6.0f).requiresCorrectToolForDrops().noOcclusion());
        add("sifting_table", ManualToolBlockEntity.Kind.SIFTING, BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL).strength(2.0f, 2.0f).sound(SoundType.METAL).noOcclusion());
        CRANK = GTBlocks.BLOCKS.register("crank", () -> new CrankBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD).strength(1.0f, 1.0f).sound(SoundType.WOOD).noOcclusion()));
        GTBlocks.BLOCK_ITEMS.register("crank", () -> new BlockItem(CRANK.get(), new Item.Properties().stacksTo(16)));   // GT6 Hand Crank: 16
        ROPE = GTBlocks.BLOCKS.register("rope", () -> new com.gregtech.gregtech.block.tool.RopeBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOL)
                        .strength(0.5f, 0.5f).sound(SoundType.WOOL).noOcclusion().noCollission(),
                com.gregtech.gregtech.content.material.Materials.Brown.getColor()));   // GT6 Rope: MT.Brown
        GTBlocks.BLOCK_ITEMS.register("rope", () -> new BlockItem(ROPE.get(), new Item.Properties().stacksTo(64)));   // GT6 Ropes: 64
        // GT6's five other ropes (Loader_MultiTileEntities:2087-2091); the shared greyscale texture is
        // tinted by the rope's material, so no extra art is needed.
        ROPE_SILK = rope("rope_silk", com.gregtech.gregtech.content.material.Materials.White);      // :2087
        ROPE_GRASS = rope("rope_grass", com.gregtech.gregtech.content.material.Materials.Yellow);   // :2088
        ROPE_VINE = rope("rope_vine", com.gregtech.gregtech.content.material.Materials.Green);      // :2089
        ROPE_PLASTIC = rope("rope_plastic", com.gregtech.gregtech.content.material.Materials.Plastic);  // :2090
        ROPE_STEEL = rope("rope_steel", com.gregtech.gregtech.content.material.Materials.Steel);    // :2091
        DYNAMITE = GTBlocks.BLOCKS.register("dynamite", () -> new com.gregtech.gregtech.block.tool.DynamiteBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                        .strength(0.2f, 0.0f).sound(SoundType.GRASS).noOcclusion().instabreak(),
                10F, 5, com.gregtech.gregtech.content.material.Materials.Red.getColor()));   // GT6 Dynamite: MT.Red, quality 10
        GTBlocks.BLOCK_ITEMS.register("dynamite", () -> new BlockItem(DYNAMITE.get(), new Item.Properties().stacksTo(64)));   // GT6 Dynamite: 64
        // GT6's other two explosives (Loader_MultiTileEntities:2235,2237): the boomstick's quality is
        // 10 like the dynamite's, the strong dynamite's is 40 - four times the resistance threshold, with the same 3x3x3 volume.
        BOOMSTICK = dynamite("boomstick", 10F, 3, com.gregtech.gregtech.content.material.Materials.Orange);   // :2235
        DYNAMITE_STRONG = dynamite("strong_dynamite", 40F, 5, com.gregtech.gregtech.content.material.Materials.Purple);  // :2237
        TAP = GTBlocks.BLOCKS.register("tap", () -> new com.gregtech.gregtech.block.tool.TapBlock(false,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                        .strength(1.5f, 6.0f).sound(SoundType.METAL).noOcclusion()));
        GTBlocks.BLOCK_ITEMS.register("tap", () -> new BlockItem(TAP.get(), new Item.Properties().stacksTo(64)));   // GT6 taps: 64
        NOZZLE = GTBlocks.BLOCKS.register("nozzle", () -> new com.gregtech.gregtech.block.tool.TapBlock(true,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                        .strength(1.5f, 6.0f).sound(SoundType.METAL).noOcclusion()));
        GTBlocks.BLOCK_ITEMS.register("nozzle", () -> new BlockItem(NOZZLE.get(), new Item.Properties().stacksTo(64)));   // GT6 nozzles: 64
        DUST_FUNNEL = GTBlocks.BLOCKS.register("dust_funnel", () -> new com.gregtech.gregtech.block.tool.DustFunnelBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                        .strength(2.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()));
        GTBlocks.BLOCK_ITEMS.register("dust_funnel", () -> new BlockItem(DUST_FUNNEL.get(), new Item.Properties().stacksTo(16)));   // GT6 Dust Funnel: 16

        // === Wave 43 / F10: Tool blocks batch 3 ===
        com.gregtech.gregtech.content.tool.AnvilDefinitions.registerAll();
        simple("mixing_bowl",
                BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).strength(1f, 5f).sound(SoundType.STONE));
        // GT6 32705, 32707, 32720 and 32721: the table forms inherit the same
        // manual processing as their vessel, while wood has its own 4,000 mB tanks.
        simple("mixing_bowl_table",
                BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).strength(1f, 5f).sound(SoundType.STONE));
        simple("juicer",
                BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, 6f).requiresCorrectToolForDrops());
        simple("bathing_pot",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).requiresCorrectToolForDrops().sound(SoundType.METAL));
        simple("bathing_pot_table",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1f, 6f).requiresCorrectToolForDrops().sound(SoundType.METAL));
        simple("bathing_pot_wood",
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1f, 5f).sound(SoundType.WOOD));
        simple("bathing_pot_table_wood",
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1f, 5f).sound(SoundType.WOOD));
        simple("plant_pot",
                BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).strength(1f, 5f).sound(SoundType.STONE));
        // GT6's bumbliary (Loader_MultiTileEntities:2221, hardness 5.0/resistance 5.0) and the
        // advanced one (:2222, stainless steel, 6.0/6.0). This used to be a placeholder block; the
        // ids stay, so the existing lang keys and the model keep working.
        BUMBLIARY = bumbliary("bumbliary", false,
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(5f, 5f).sound(SoundType.WOOD));
        ADVANCED_BUMBLIARY = bumbliary("advanced_bumbliary", true,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6f, 6f).sound(SoundType.WOOD));
        SAP_BAG = simple("sap_bag",
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5f, 0.5f).sound(SoundType.WOOL));
        for (var spec : GTStorageMetals.ALL) {
            String id = spec.suffix().equals("steel") ? "scaffold" : "scaffold_" + spec.suffix();
            var block = GTBlocks.BLOCKS.register(id, () -> new com.gregtech.gregtech.block.tool.ScaffoldBlock(
                    spec.material(), BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(5f, spec.resistance()).sound(SoundType.METAL).noOcclusion()));
            SCAFFOLDS.add(block);
            ALL_SIMPLE.add((RegistryObject) block);
            GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(64)));
        }
        simple("fluid_funnel",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).requiresCorrectToolForDrops().sound(SoundType.METAL));
        simple("cap_nozzle",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 6f).sound(SoundType.METAL));
        simple("coin_mold",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6f, 6f).requiresCorrectToolForDrops().sound(SoundType.METAL));
        simple("advanced_button",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6f, 6f).sound(SoundType.METAL).noOcclusion());

        for(var spec:com.gregtech.gregtech.content.tool.FluidAttachmentSpec.all()){
            var block=GTBlocks.BLOCKS.register(spec.id(),()->new com.gregtech.gregtech.block.tool.FluidAttachmentBlock(spec,
                    BlockBehaviour.Properties.of().strength(0.5f,6).noOcclusion()));
            ALL_SIMPLE.add((RegistryObject)block);
            GTBlocks.BLOCK_ITEMS.register(spec.id(),()->new BlockItem(block.get(),new Item.Properties().stacksTo(16)));   // GT6 Fluid Containers: 16
        }
        // Preserve existing IDs while replacing placeholder items with finite fluid handlers.
        for (var spec : com.gregtech.gregtech.api.fluid.PortableFluidContainerSpec.values()) {
            var block = GTBlocks.BLOCKS.register("fluid_" + spec.id(), () -> new com.gregtech.gregtech.block.tool.PortableContainerBlock(spec,
                    BlockBehaviour.Properties.of().strength(1.0f).noOcclusion()));
            PORTABLE.add(block);
            ITEMS.register("fluid_" + spec.id(), () -> new com.gregtech.gregtech.item.PortableFluidContainerItem(
                    block.get(), spec, new Item.Properties().stacksTo(spec.shapeId().equals("cell")?64:16)));   // GT6 Fluid Containers: 16
        }

    }

    /**
     * One of GT6's two bumbliaries: a tool block that hosts the breeding machine
     * ({@code com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity}). GT6 registers both with
     * 16 items per stack ({@code Loader_MultiTileEntities:2221-2222}).
     */
    @SuppressWarnings("unchecked")
    private static RegistryObject<com.gregtech.gregtech.block.tool.BumbliaryBlock> bumbliary(
            String id, boolean advanced, BlockBehaviour.Properties props) {
        var block = GTBlocks.BLOCKS.register(id,
                () -> new com.gregtech.gregtech.block.tool.BumbliaryBlock(id, advanced, props));
        ALL_SIMPLE.add((RegistryObject) block);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
        return block;
    }

    /** Registers a simple tool block with no associated block entity. */
    @SuppressWarnings("unchecked")
    public static void anvil(String id, String material, long durability) {
        var block = GTBlocks.BLOCKS.register(id, () -> new com.gregtech.gregtech.block.tool.MaterialAnvilBlock(
                () -> com.gregtech.gregtech.api.material.GTMaterialRegistry.get(material), durability,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(10f, 1200f).requiresCorrectToolForDrops().sound(SoundType.ANVIL)));
        ANVILS.add(block);
        ALL_SIMPLE.add((RegistryObject) block);
        // GT6 registers every anvil with stack size 16 (Loader_MultiTileEntities:32025 onwards)
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
    }

    @SuppressWarnings("unchecked")
    private static RegistryObject<net.minecraft.world.level.block.Block> simple(String id, BlockBehaviour.Properties props) {
        var restored = java.util.Set.of("anvil", "mixing_bowl", "bathing_pot", "juicer", "plant_pot",
                "sap_bag", "fluid_funnel", "cap_nozzle");
        var block = GTBlocks.BLOCKS.register(id, () -> java.util.Set.of("mixing_bowl", "mixing_bowl_table",
                "juicer", "bathing_pot", "bathing_pot_table", "bathing_pot_wood", "bathing_pot_table_wood").contains(id)
                ? new com.gregtech.gregtech.block.tool.ProcessingToolBlock(id, props)
                : id.equals("plant_pot")
                ? new com.gregtech.gregtech.block.tool.PlantPotBlock(props)
                : id.equals("scaffold")
                ? new com.gregtech.gregtech.block.tool.ScaffoldBlock(props)
                : id.equals("coin_mold")
                ? new com.gregtech.gregtech.block.tool.CoinMoldBlock(props)
                : id.equals("advanced_button")
                ? new com.gregtech.gregtech.block.tool.AdvancedButtonBlock(props)
                : java.util.Set.of("fluid_funnel","cap_nozzle").contains(id)
                ? new com.gregtech.gregtech.block.tool.FluidAttachmentBlock(new com.gregtech.gregtech.content.tool.FluidAttachmentSpec(id,id,
                    id.equals("fluid_funnel")?com.gregtech.gregtech.content.material.Materials.Ceramic:com.gregtech.gregtech.content.material.Materials.Steel,false),props)
                : id.equals("sap_bag")
                ? new com.gregtech.gregtech.block.tool.SapBagBlock(id, props)
                : restored.contains(id)
                ? new com.gregtech.gregtech.block.tool.ShapedToolBlock(id, props)
                : new net.minecraft.world.level.block.Block(props));
        ALL_SIMPLE.add((RegistryObject) block);
        // The original registers scaffold (8400 series), the two fluid attachments, and the
        // advanced button with 64 per stack. The coin mold remains 16 per stack.
        boolean sixtyFour = java.util.Set.of("scaffold", "fluid_funnel", "cap_nozzle", "advanced_button").contains(id);
        GTBlocks.BLOCK_ITEMS.register(id, () -> new BlockItem(block.get(),
                new Item.Properties().stacksTo(sixtyFour ? 64 : 16)));
        return (RegistryObject<net.minecraft.world.level.block.Block>) block;
    }
}
