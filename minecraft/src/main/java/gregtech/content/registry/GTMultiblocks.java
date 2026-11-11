package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.block.machine.*;
import gregtech.content.multiblock.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?} else {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
//?}
import java.util.*;

/** One registration catalog for original multiblock controllers and structural parts. */
public final class GTMultiblocks {
    private GTMultiblocks() {}
    private static final Map<Integer, RegisteredBlock<Block>> NAMED = new LinkedHashMap<>();
    private static final Map<String, RegisteredBlock<Block>> CONTROLLERS = new LinkedHashMap<>();
    private static final List<AddonPart> ADDON_PARTS = new ArrayList<>();
    private static boolean addonQueued;
    public record AddonPart(RegisteredBlock<Block> block, String material, String texture) {}
    public static RegisteredBlock<Block> CENTRIFUGE_PART, ELECTROLYZER_PART, HEAT_TRANSMITTER,
            DISTILLATION_TOWER_PART, TANK_WALL, TANK_WALL_DENSE, FIRE_BRICKS,
            IMPLOSION_COMPRESSOR_WALL, LARGE_NIOBIUM_TITANIUM_COIL, LARGE_COPPER_COIL,
            LARGE_IRIDIUM_COIL, DENSE_TITANIUM_WALL, TUNGSTEN_WALL, LIGHTNING_ROD,
            COKE_OVEN_MAIN, CRYO_DISTILLATION_MAIN, HEAT_EXCHANGER_MAIN, LOGISTICS_CORE;
    public static RegisteredBlock<BasicMachineBlock> DISTILLATION_TOWER_MAIN;
    public static void registerAll(ContentRegistrar registrar) {
        if (!NAMED.isEmpty()) throw new IllegalStateException("Multiblocks registered twice");
        GTHeatExchangers.registerAll(registrar);
        for (var part : SharedLargeMachineParts.DEFINITIONS)
            NAMED.put(part.originalId(), controller(registrar, MultiblockBlockFactory.declaration(part)));
        for (var part : OriginalMultiblockPartData.ALL)
            if (!NAMED.containsKey(part.originalId()))
                NAMED.put(part.originalId(), registrar.block(
                        REGISTRY_NAMESPACE, part.path(), () -> MultiblockBlockFactory.structural(part), GTMultiblocks::blockItem));
        CENTRIFUGE_PART = findPart(18100); ELECTROLYZER_PART = findPart(18105);
        HEAT_TRANSMITTER = findPart(18101); DISTILLATION_TOWER_PART = findPart(18102);
        TANK_WALL = findPart(18002); TANK_WALL_DENSE = findPart(18022); FIRE_BRICKS = findPart(18000);
        IMPLOSION_COMPRESSOR_WALL = findPart(18023); LARGE_NIOBIUM_TITANIUM_COIL = findPart(18041);
        LARGE_COPPER_COIL = findPart(18040); LARGE_IRIDIUM_COIL = findPart(18045);
        DENSE_TITANIUM_WALL = findPart(18026); TUNGSTEN_WALL = findPart(18004); LIGHTNING_ROD = findPart(18104);
        COKE_OVEN_MAIN = controller(registrar, "coke_oven_main", () -> new CokeOvenControllerBlock(properties(6)), ControllerDeclarations.COKE_OVEN);
        CRYO_DISTILLATION_MAIN = controller(registrar, "cryo_distillation_main", () -> new CryoDistillationControllerBlock(properties(6)), ControllerDeclarations.CRYO_DISTILLATION);
        HEAT_EXCHANGER_MAIN = controller(registrar, "heat_exchanger_main", () -> new LargeHeatExchangerControllerBlock(properties(6)), ControllerDeclarations.HEAT_EXCHANGER);
        LOGISTICS_CORE = controller(registrar, "logistics_core", () -> new LogisticsCoreControllerBlock(properties(6)), ControllerDeclarations.LOGISTICS);
        DISTILLATION_TOWER_MAIN = RegisteredBlock.reference("distillation_tower_main");
    }
    /** Queues addon casings after the built-in parts. A call before addon machines install does nothing. */
    public static void registerAddonParts(ContentRegistrar registrar) {
        if (addonQueued || !gregtech.engine.addon.AddonMachines.installed()) return;
        addonQueued = true;
        for (var part : gregtech.engine.addon.AddonMachines.structureParts()) {
            var declaration = part.declaration();
            float hardness = declaration.hardness();
            boolean energyPart = declaration.energy() != null || declaration.energyInput() != null;
            boolean functional = declaration.functional();
            var properties = BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(hardness, hardness)
                    .requiresCorrectToolForDrops();
            if (energyPart) properties = properties.lightLevel(state ->
                    state.hasProperty(AddonEnergyPartBlock.SATISFIED) && state.getValue(AddonEnergyPartBlock.SATISFIED) ? 12 : 0);
            var partProperties = properties;
            var block = energyPart
                    ? registerAddon(registrar, part.addonId(), declaration.name(), () -> new AddonEnergyPartBlock(partProperties,
                            part.addonId(), declaration))
                    : functional
                    ? registerAddon(registrar, part.addonId(), declaration.name(), () -> new AddonFunctionalPartBlock(partProperties,
                            part.addonId(), declaration))
                    : registerAddon(registrar, part.addonId(), declaration.name(), () -> new MultiblockPortBlock(partProperties));
            if (NAMED.put(part.id(), block) != null)
                throw new IllegalStateException("Addon structure part id already used: " + part.id());
            ADDON_PARTS.add(new AddonPart(block, declaration.material(), declaration.texture()));
        }
    }
    private static RegisteredBlock<Block> registerAddon(ContentRegistrar registrar, String namespace, String path, java.util.function.Supplier<Block> factory) {
        return registrar.block(namespace, path, factory, block -> new BlockItem(block, new Item.Properties()));
    }
    private static BlockBehaviour.Properties properties(float hardness) {
        return BlockBehaviour.Properties.of().strength(hardness, hardness).requiresCorrectToolForDrops();
    }
    private static RegisteredBlock<Block> controller(ContentRegistrar registrar, String id, java.util.function.Supplier<Block> factory,
            gregtech.engine.multiblock.ControllerBinding<?> structure) {
        return controller(registrar, gregtech.engine.multiblock.ControllerDeclaration.multiblock(REGISTRY_NAMESPACE, id, factory, structure));
    }
    private static RegisteredBlock<Block> controller(ContentRegistrar registrar, gregtech.engine.multiblock.ControllerDeclaration<Block> declaration) {
        var block = MultiblockRegistration.register(declaration,
                (path, create) -> registrar.block(REGISTRY_NAMESPACE, path, create, GTMultiblocks::blockItem));
        CONTROLLERS.put(declaration.id().getPath(), block);
        return block;
    }
    /** Structural parts stack to 64; controllers use the same limit as other machines. */
    private static BlockItem blockItem(Block block) {
        return new BlockItem(block, new Item.Properties().stacksTo(block instanceof MultiblockPortBlock ? 64 : 16));
    }
    public static RegisteredBlock<Block> findPart(int id) { return NAMED.get(id); }
    public static List<RegisteredBlock<Block>> parts() {
        return OriginalMultiblockPartData.ALL.stream().map(part -> NAMED.get(part.originalId())).toList();
    }
    public static List<AddonPart> addonParts() { return List.copyOf(ADDON_PARTS); }
    /** Core structure ports plus addon casings. The block-entity type is built from this list. */
    public static Block[] portBlocks() {
        var blocks = new ArrayList<Block>();
        for (var part : parts()) {
            Block block = part.get();
            //? if >=1.21.1 {
            /*blocks.add(block);
            *///?} else {
            if (block instanceof MultiblockPortBlock) blocks.add(block);
            //?}
        }
        for (var part : ADDON_PARTS)
            if (!(part.block().get() instanceof AddonEnergyPartBlock)
                    && !(part.block().get() instanceof AddonFunctionalPartBlock)) blocks.add(part.block().get());
        return blocks.toArray(Block[]::new);
    }
    /** Addon energy parts. Empty when no addon declared one. Not valid for {@code gt6ce:multiblock_port}. */
    public static Block[] energyPartBlocks() {
        var blocks = new ArrayList<Block>();
        for (var part : ADDON_PARTS)
            if (part.block().get() instanceof AddonEnergyPartBlock energy) blocks.add(energy);
        return blocks.toArray(Block[]::new);
    }
    /** Addon functional parts with no energy role. Empty when no addon declared one. */
    public static Block[] functionalPartBlocks() {
        var blocks = new ArrayList<Block>();
        for (var part : ADDON_PARTS)
            if (part.block().get() instanceof AddonFunctionalPartBlock functional) blocks.add(functional);
        return blocks.toArray(Block[]::new);
    }
    private static List<RegisteredBlock<Block>> range(int low, int high) {
        return NAMED.entrySet().stream().filter(entry -> entry.getKey() >= low && entry.getKey() <= high).map(Map.Entry::getValue).toList();
    }
    public static List<RegisteredBlock<Block>> crucibles() { return range(17302, 17312); }
    public static List<RegisteredBlock<Block>> boilers() { return range(17201, 17205); }
    public static List<RegisteredBlock<Block>> tanks() { return range(17001, 17067); }
    public static List<RegisteredBlock<Block>> texturedBlocks() {
        var blocks = new LinkedHashSet<>(NAMED.values()); blocks.addAll(CONTROLLERS.values());
        //? if >=1.21.1 {
        //?} else {
        blocks.add(RegisteredBlock.reference("distillation_tower_main"));
        //?}
        return List.copyOf(blocks);
    }
    public static int tintOf(Block block) {
        if (block instanceof LargeCrucibleControllerBlock crucible) return crucible.variant().material().getColor();
        if (block instanceof LargeGasTurbineControllerBlock turbine) return turbine.grade().tint();
        if (block instanceof AxialGeneratorBlock generator) return generator.grade().tint();
        Integer named = LargeMachineParts.tint(block); if (named != null) return named;
        if (block instanceof BasicMachineBlock machine) return machine.basicSpec().material().getColor();
        var id = BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? 0xFFFFFF : GTMaterialRegistry.get(OriginalMultiblockTintPolicy.material(id.getPath())).getColor();
    }
}
