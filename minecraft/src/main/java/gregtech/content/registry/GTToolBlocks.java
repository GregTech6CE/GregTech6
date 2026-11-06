package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

//? if >=1.21.1 {
/*import gregtech.content.block.tool.DynamiteBlock;
import gregtech.content.material.Materials;
import gregtech.api.material.GTMaterial;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;

/^* Mining charges, portable containers and fluid attachments; manual stations register separately. ^/
*///?} else {

import gregtech.content.block.tool.CrankBlock;
import gregtech.content.block.tool.ManualToolBlock;
import gregtech.content.blockentity.tool.ManualToolBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 manual tool stations (mortar, grind stone, sifting table, crank, ...). */
//?}
public final class GTToolBlocks {
//? if >=1.21.1 {
    /*private GTToolBlocks() {
    }
    public static final java.util.List<RegisteredBlock<gregtech.content.block.tool.PortableContainerBlock>> PORTABLE = new java.util.ArrayList<>();
    public static RegisteredBlock<DynamiteBlock> DYNAMITE, BOOMSTICK, DYNAMITE_STRONG;

    private static RegisteredBlock<DynamiteBlock> dynamite(ContentRegistrar registrar, String id, float resistance, int fortune, GTMaterial material) {
        return registrar.block(REGISTRY_NAMESPACE, id, () -> new DynamiteBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(.2f,
                0).sound(SoundType.GRASS).noOcclusion().instabreak(), resistance, fortune, material.getColor()),
                block -> new BlockItem(block, new Item.Properties().stacksTo(64)));
    }

    public static java.util.List<RegisteredBlock<DynamiteBlock>> explosives() {
        return java.util.List.of(DYNAMITE, BOOMSTICK, DYNAMITE_STRONG);
    }

    private static BlockBehaviour.Properties attachmentProperties(String id) {
        var p = BlockBehaviour.Properties.of().noOcclusion();
        if (java.util.Set.of("tap", "nozzle",
                "cap_nozzle").contains(id)) return p.mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(1.5f,
                6).sound(SoundType.METAL);
        if (id.equals("fluid_funnel")) return p.mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(2,
                6).sound(SoundType.METAL).requiresCorrectToolForDrops();
        return p.strength(.5f, 6);
    }

    public static void registerAll(ContentRegistrar registrar) {
        DYNAMITE = dynamite(registrar, "dynamite", 10, 5, Materials.Red);
        BOOMSTICK = dynamite(registrar, "boomstick", 10, 3, Materials.Orange);
        DYNAMITE_STRONG = dynamite(registrar, "strong_dynamite", 40, 5, Materials.Purple);
        for (var spec : gregtech.engine.fluid.PortableFluidContainerSpec.values()) {
            var block = registrar.blockWithoutItem(REGISTRY_NAMESPACE, "fluid_" + spec.id(), () -> new gregtech.content.block.tool.PortableContainerBlock(spec,
                    BlockBehaviour.Properties.of().strength(1f).noOcclusion()));
            PORTABLE.add(block);
            registrar.item(REGISTRY_NAMESPACE, "fluid_" + spec.id(), () -> new gregtech.content.item.PortableFluidContainerItem(block.get(), spec,
                    new Item.Properties().stacksTo(spec.shapeId().equals("cell") ? 64 : 16)));
        }
        var attachments = new java.util.ArrayList<>(gregtech.content.tool.FluidAttachmentSpec.all());
        for (String id : java.util.List.of("tap", "nozzle", "fluid_funnel",
                "cap_nozzle")) attachments.add(new gregtech.content.tool.FluidAttachmentSpec(id, id, java.util.Set.of("cap_nozzle",
                "nozzle").contains(id) ? Materials.Steel : Materials.Ceramic, false));
        for (var spec : attachments) {
            registrar.block(REGISTRY_NAMESPACE, spec.id(), () -> new gregtech.content.block.tool.FluidAttachmentBlock(spec,
                    attachmentProperties(spec.id())), block -> new BlockItem(block, new Item.Properties().stacksTo(64)));
        }
    }

    public static void submit(IEventBus bus) {
        GTFluidComponents.TYPES.register(bus);
    }
*///?} else {
    private static final List<RegisteredBlock<ManualToolBlock>> MANUAL = new ArrayList<>();
    private static final List<RegisteredBlock<? extends net.minecraft.world.level.block.Block>> ALL_SIMPLE = new ArrayList<>();

    /** GT6's sap bag (multi-tile 32736) — a real tank block that drains an adjacent tree hole. */
    public static RegisteredBlock<? extends net.minecraft.world.level.block.Block> SAP_BAG;
    public static RegisteredBlock<CrankBlock> CRANK;
    public static final List<RegisteredBlock<gregtech.content.block.tool.MaterialAnvilBlock>> ANVILS = new ArrayList<>();
    public static RegisteredBlock<gregtech.content.block.tool.RopeBlock> ROPE;
    public static RegisteredBlock<gregtech.content.block.tool.RopeBlock> ROPE_SILK;
    public static RegisteredBlock<gregtech.content.block.tool.RopeBlock> ROPE_GRASS;
    public static RegisteredBlock<gregtech.content.block.tool.RopeBlock> ROPE_VINE;
    public static RegisteredBlock<gregtech.content.block.tool.RopeBlock> ROPE_PLASTIC;
    public static RegisteredBlock<gregtech.content.block.tool.RopeBlock> ROPE_STEEL;
    public static RegisteredBlock<gregtech.content.block.tool.DynamiteBlock> DYNAMITE;
    public static RegisteredBlock<gregtech.content.block.tool.DynamiteBlock> BOOMSTICK;
    public static RegisteredBlock<gregtech.content.block.tool.DynamiteBlock> DYNAMITE_STRONG;
    public static RegisteredBlock<gregtech.content.block.tool.TapBlock> TAP;
    public static RegisteredBlock<gregtech.content.block.tool.TapBlock> NOZZLE;
    public static RegisteredBlock<gregtech.content.block.tool.DustFunnelBlock> DUST_FUNNEL;

    /** GT6's bumbliary (id 32741): the bee breeding machine. */
    public static RegisteredBlock<gregtech.content.block.tool.BumbliaryBlock> BUMBLIARY;

    /** GT6's advanced bumbliary (id 32007): 20 slots instead of 36. */
    public static RegisteredBlock<gregtech.content.block.tool.BumbliaryBlock> ADVANCED_BUMBLIARY;

    public static final java.util.List<RegisteredBlock<gregtech.content.block.tool.PortableContainerBlock>> PORTABLE = new java.util.ArrayList<>();

    public static final List<RegisteredBlock<gregtech.content.block.tool.ScaffoldBlock>> SCAFFOLDS = new ArrayList<>();

    public static gregtech.content.block.tool.ScaffoldBlock scaffold(gregtech.api.material.GTMaterial material) {
        for (var entry : SCAFFOLDS) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No scaffold for " + material.getName());
    }

    private GTToolBlocks() {
    }

    public static List<RegisteredBlock<ManualToolBlock>> manual() {
        return Collections.unmodifiableList(MANUAL);
    }

    /** All simple tool blocks (batch 3: anvil, mixing_bowl, etc.). */
    public static List<RegisteredBlock<? extends net.minecraft.world.level.block.Block>> simpleBlocks() {
        return Collections.unmodifiableList(ALL_SIMPLE);
    }

    private static void add(ContentRegistrar registrar, String id, ManualToolBlockEntity.Kind kind, BlockBehaviour.Properties props) {
        RegisteredBlock<ManualToolBlock> block = registrar.block(REGISTRY_NAMESPACE, id,
                () -> new ManualToolBlock(kind, props),
                // GT6 Misc Tool Blocks (Mortar/Juicer/Grindstone/Sifting Table rows): 16 per stack
                tool -> new BlockItem(tool, new Item.Properties().stacksTo(16)));
        MANUAL.add(block);
    }

    /** One of GT6's six ropes: same model, tinted by the rope's material (GT6 does the same). */
    private static RegisteredBlock<gregtech.content.block.tool.RopeBlock> rope(ContentRegistrar registrar,
            String id, gregtech.api.material.GTMaterial material) {
        return registrar.block(REGISTRY_NAMESPACE, id, () -> new gregtech.content.block.tool.RopeBlock(
                        BlockBehaviour.Properties.of().mapColor(MapColor.WOOL)
                                .strength(0.5f, 0.5f).sound(SoundType.WOOL).noOcclusion().noCollission(),
                        material.getColor()),
                block -> new BlockItem(block, new Item.Properties().stacksTo(64)));
    }

    private static RegisteredBlock<gregtech.content.block.tool.RopeBlock> rope(ContentRegistrar registrar, gregtech.content.tool.UtilityToolRules.Rope spec) {
        return rope(registrar, spec.id(), gregtech.engine.material.GTMaterialRegistry.get(spec.material()));
    }

    /** One of GT6's three explosives: same model, tinted, with the original's blast quality. */
    private static RegisteredBlock<gregtech.content.block.tool.DynamiteBlock> dynamite(ContentRegistrar registrar,
            String id, float resistance, int fortune, gregtech.api.material.GTMaterial material) {
        return registrar.block(REGISTRY_NAMESPACE, id, () -> new gregtech.content.block.tool.DynamiteBlock(
                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                                .strength(0.2f, 0.0f).sound(SoundType.GRASS).noOcclusion().instabreak(),
                        resistance, fortune, material.getColor()),
                block -> new BlockItem(block, new Item.Properties().stacksTo(64)));
    }

    /** GT6 default material tints: stone tools gray, the sifting table steel. */
    public static int tintOf(ManualToolBlockEntity.Kind kind) {
        return switch (kind) {
            case MORTAR -> 0xCDCDCD;
            case GRINDSTONE -> 0xB5B5B5;
            case SIFTING -> gregtech.content.material.Materials.Steel.getColor();
        };
    }

    public static void registerAll(ContentRegistrar registrar) {
        add(registrar, "mortar_block", ManualToolBlockEntity.Kind.MORTAR, BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE).strength(1.0f, 5.0f).requiresCorrectToolForDrops().noOcclusion());
        for (String material : new String[]{
                "Netherite", "Sapphire", "Diamond", "Amethyst"
        }) {
            String id = "mortar_" + material.toLowerCase(java.util.Locale.ROOT);
            var block = registrar.block(REGISTRY_NAMESPACE, id, () -> new ManualToolBlock(ManualToolBlockEntity.Kind.MORTAR, material,
                            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1f, 5f).noOcclusion()),
                    tool -> new BlockItem(tool, new Item.Properties().stacksTo(16)));
            MANUAL.add(block);
        }
        add(registrar, "grindstone_block", ManualToolBlockEntity.Kind.GRINDSTONE, BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE).strength(1.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion());
        add(registrar, "sifting_table", ManualToolBlockEntity.Kind.SIFTING, BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL).strength(1.0f, 6.0f).sound(SoundType.METAL).noOcclusion());
        CRANK = registrar.block(REGISTRY_NAMESPACE, "crank", () -> new CrankBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL).strength(1.0f, 6.0f).sound(SoundType.METAL).noOcclusion()),
                // GT6 Hand Crank: 16
                block -> new BlockItem(block, new Item.Properties().stacksTo(16)));
        ROPE = registrar.block(REGISTRY_NAMESPACE, "rope", () -> new gregtech.content.block.tool.RopeBlock(
                        BlockBehaviour.Properties.of().mapColor(MapColor.WOOL)
                                .strength(0.5f, 0.5f).sound(SoundType.WOOL).noOcclusion().noCollission(),
                        gregtech.content.material.Materials.Brown.getColor()),
                // GT6 Rope: MT.Brown
                block -> new BlockItem(block, new Item.Properties().stacksTo(64)));
        // GT6's five other ropes (Loader_MultiTileEntities:2087-2091); the shared greyscale texture is
        // tinted by the rope's material, so no extra art is needed.
        ROPE_SILK = rope(registrar, gregtech.content.tool.UtilityToolRules.ROPES.stream().filter(
                v -> v.id().equals("rope_silk")).findFirst().orElseThrow());
        ROPE_GRASS = rope(registrar, gregtech.content.tool.UtilityToolRules.ROPES.stream().filter(
                v -> v.id().equals("rope_grass")).findFirst().orElseThrow());
        ROPE_VINE = rope(registrar, gregtech.content.tool.UtilityToolRules.ROPES.stream().filter(
                v -> v.id().equals("rope_vine")).findFirst().orElseThrow());
        ROPE_PLASTIC = rope(registrar, gregtech.content.tool.UtilityToolRules.ROPES.stream().filter(
                v -> v.id().equals("rope_plastic")).findFirst().orElseThrow());
        ROPE_STEEL = rope(registrar, gregtech.content.tool.UtilityToolRules.ROPES.stream().filter(
                v -> v.id().equals("rope_steel")).findFirst().orElseThrow());
        DYNAMITE = registrar.block(REGISTRY_NAMESPACE, "dynamite", () -> new gregtech.content.block.tool.DynamiteBlock(
                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                                .strength(0.2f, 0.0f).sound(SoundType.GRASS).noOcclusion().instabreak(),
                        10F, 5, gregtech.content.material.Materials.Red.getColor()),
                // GT6 Dynamite: MT.Red, quality 10
                block -> new BlockItem(block, new Item.Properties().stacksTo(64)));
        // GT6's other two explosives (Loader_MultiTileEntities:2235,2237): the boomstick's quality is
        // 10 like the dynamite's, the strong dynamite's is 40 - four times the resistance threshold, with the same 3x3x3 volume.
        BOOMSTICK = dynamite(registrar, "boomstick", 10F, 3, gregtech.content.material.Materials.Orange);
        DYNAMITE_STRONG = dynamite(registrar, "strong_dynamite", 40F, 5, gregtech.content.material.Materials.Purple);
        TAP = registrar.block(REGISTRY_NAMESPACE, "tap", () -> new gregtech.content.block.tool.TapBlock(false,
                        BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                                .strength(1.5f, 6.0f).sound(SoundType.METAL).noOcclusion()),
                // GT6 taps: 64
                block -> new BlockItem(block, new Item.Properties().stacksTo(64)));
        NOZZLE = registrar.block(REGISTRY_NAMESPACE, "nozzle", () -> new gregtech.content.block.tool.TapBlock(true,
                        BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                                .strength(1.5f, 6.0f).sound(SoundType.METAL).noOcclusion()),
                // GT6 nozzles: 64
                block -> new BlockItem(block, new Item.Properties().stacksTo(64)));
        DUST_FUNNEL = registrar.block(REGISTRY_NAMESPACE, "dust_funnel", () -> new gregtech.content.block.tool.DustFunnelBlock(
                        BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                                .strength(2.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()),
                // GT6 Dust Funnel: 16
                block -> new BlockItem(block, new Item.Properties().stacksTo(16)));

        // === Wave 43 / F10: Tool blocks batch 3 ===
        gregtech.content.tool.AnvilDefinitions.registerAll(registrar);
        simple(registrar, "mixing_bowl",
                BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).strength(1f, 5f).sound(SoundType.STONE));
        // GT6 32705, 32707, 32720 and 32721: the table forms inherit the same
        // manual processing as their vessel, while wood has its own 4,000 mB tanks.
        simple(registrar, "mixing_bowl_table",
                BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).strength(1f, 5f).sound(SoundType.STONE));
        simple(registrar, "juicer",
                BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, 6f).requiresCorrectToolForDrops());
        simple(registrar, "bathing_pot",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, 6f).requiresCorrectToolForDrops().sound(SoundType.METAL));
        simple(registrar, "bathing_pot_table",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1f, 6f).requiresCorrectToolForDrops().sound(SoundType.METAL));
        simple(registrar, "bathing_pot_wood",
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1f, 5f).sound(SoundType.WOOD));
        simple(registrar, "bathing_pot_table_wood",
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1f, 5f).sound(SoundType.WOOD));
        simple(registrar, "plant_pot",
                BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).strength(1f, 5f).sound(SoundType.STONE));
        // GT6's bumbliary (Loader_MultiTileEntities:2221, hardness 5.0/resistance 5.0) and the
        // advanced one ({@code Loader_MultiTileEntities:2222}, stainless steel, 6.0/6.0). The port
        // keeps GT6's ids, so the existing lang keys and the model keep working.
        GTBumbleBlocks.initializeApiaries();
        BUMBLIARY = RegisteredBlock.reference("bumbliary");
        ADVANCED_BUMBLIARY = RegisteredBlock.reference("advanced_bumbliary");
        ALL_SIMPLE.add(BUMBLIARY);
        ALL_SIMPLE.add(ADVANCED_BUMBLIARY);
        SAP_BAG = simple(registrar, "sap_bag",
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5f, 0.5f).sound(SoundType.WOOL));
        for (var spec : GTStorageMetals.ALL) {
            String id = spec.suffix().equals("steel") ? "scaffold" : "scaffold_" + spec.suffix();
            var block = registrar.block(REGISTRY_NAMESPACE, id, () -> new gregtech.content.block.tool.ScaffoldBlock(
                            spec.material(), BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                                    .strength(5f, spec.resistance()).sound(SoundType.METAL).noOcclusion()),
                    scaffold -> new BlockItem(scaffold, new Item.Properties().stacksTo(64)));
            SCAFFOLDS.add(block);
            ALL_SIMPLE.add(block);
        }
        simple(registrar, "fluid_funnel",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).requiresCorrectToolForDrops().sound(SoundType.METAL));
        simple(registrar, "cap_nozzle",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 6f).sound(SoundType.METAL));
        simple(registrar, "coin_mold",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6f, 6f).requiresCorrectToolForDrops().sound(SoundType.METAL));
        simple(registrar, "advanced_button",
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6f, 6f).sound(SoundType.METAL).noOcclusion());

        for (var spec : gregtech.content.tool.FluidAttachmentSpec.all()) {
            var block = registrar.block(REGISTRY_NAMESPACE, spec.id(), () -> new gregtech.content.block.tool.FluidAttachmentBlock(spec,
                            BlockBehaviour.Properties.of().strength(0.5f, 6).noOcclusion()),
                    // Original taps, funnels and gas nozzles: 64
                    attachment -> new BlockItem(attachment, new Item.Properties().stacksTo(64)));
            ALL_SIMPLE.add(block);
        }
        // Preserve existing IDs while replacing placeholder items with finite fluid handlers.
        for (var spec : gregtech.engine.fluid.PortableFluidContainerSpec.values()) {
            var block = registrar.blockWithoutItem(REGISTRY_NAMESPACE, "fluid_" + spec.id(), () -> new gregtech.content.block.tool.PortableContainerBlock(spec,
                    BlockBehaviour.Properties.of().strength(1.0f).noOcclusion()));
            PORTABLE.add(block);
            // GT6 Fluid Containers: 16
            registrar.item(REGISTRY_NAMESPACE, "fluid_" + spec.id(), () -> new gregtech.content.item.PortableFluidContainerItem(
                    block.get(), spec, new Item.Properties().stacksTo(spec.shapeId().equals("cell") ? 64 : 16)));
        }

    }

    /** Registers a simple tool block with no associated block entity. */
    public static void anvil(ContentRegistrar registrar, String id, String material, long durability) {
        var block = registrar.block(REGISTRY_NAMESPACE, id, () -> new gregtech.content.block.tool.MaterialAnvilBlock(
                        () -> gregtech.engine.material.GTMaterialRegistry.get(material), durability,
                        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(10f, 1200f).requiresCorrectToolForDrops().sound(SoundType.ANVIL)),
                // GT6 registers every anvil with stack size 16 (the anvil multi-tiles start at id 32025).
                anvil -> new BlockItem(anvil, new Item.Properties().stacksTo(16)));
        ANVILS.add(block);
        ALL_SIMPLE.add(block);
    }

    private static RegisteredBlock<? extends net.minecraft.world.level.block.Block> simple(ContentRegistrar registrar, String id, BlockBehaviour.Properties props) {
        var restored = java.util.Set.of("anvil", "mixing_bowl", "bathing_pot", "juicer", "plant_pot",
                "sap_bag", "fluid_funnel", "cap_nozzle");
        // The original registers scaffold (8400 series), the two fluid attachments, and the
        // advanced button with 64 per stack. The coin mold remains 16 per stack.
        boolean sixtyFour = java.util.Set.of("scaffold", "fluid_funnel", "cap_nozzle", "advanced_button").contains(id);
        var block = registrar.block(REGISTRY_NAMESPACE, id, () -> java.util.Set.of("mixing_bowl", "mixing_bowl_table",
                        "juicer", "bathing_pot", "bathing_pot_table", "bathing_pot_wood", "bathing_pot_table_wood").contains(id)
                        ? new gregtech.content.block.tool.ProcessingToolBlock(id, props)
                        : id.equals("plant_pot")
                        ? new gregtech.content.block.tool.PlantPotBlock(props)
                        : id.equals("scaffold")
                        ? new gregtech.content.block.tool.ScaffoldBlock(props)
                        : id.equals("coin_mold")
                        ? new gregtech.content.block.tool.CoinMoldBlock(props)
                        : id.equals("advanced_button")
                        ? new gregtech.content.block.tool.AdvancedButtonBlock(props)
                        : java.util.Set.of("fluid_funnel", "cap_nozzle").contains(id)
                        ? new gregtech.content.block.tool.FluidAttachmentBlock(new gregtech.content.tool.FluidAttachmentSpec(id, id,
                                id.equals("fluid_funnel") ? gregtech.content.material.Materials.Ceramic
                                        : gregtech.content.material.Materials.Steel, false), props)
                        : id.equals("sap_bag")
                        ? new gregtech.content.block.tool.SapBagBlock(id, props)
                        : restored.contains(id)
                        ? new gregtech.content.block.tool.ShapedToolBlock(id, props)
                        : new net.minecraft.world.level.block.Block(props),
                tool -> new BlockItem(tool, new Item.Properties().stacksTo(sixtyFour ? 64 : 16)));
        ALL_SIMPLE.add(block);
        return block;
    }
//?}
}
