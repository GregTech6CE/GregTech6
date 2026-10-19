package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.machine.*;
import com.gregtech.gregtech.content.multiblock.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.*;
/** One registration catalog for original multiblock controllers and structural parts. */
public final class GTMultiblocks {
    private GTMultiblocks() {}
    private static final Map<Integer, RegisteredBlock<Block>> NAMED = new LinkedHashMap<>();
    private static final Map<String, RegisteredBlock<Block>> CONTROLLERS = new LinkedHashMap<>();
    public static RegisteredBlock<Block> CENTRIFUGE_PART, ELECTROLYZER_PART, HEAT_TRANSMITTER,
            DISTILLATION_TOWER_PART, TANK_WALL, TANK_WALL_DENSE, FIRE_BRICKS,
            IMPLOSION_COMPRESSOR_WALL, LARGE_NIOBIUM_TITANIUM_COIL, LARGE_COPPER_COIL,
            LARGE_IRIDIUM_COIL, DENSE_TITANIUM_WALL, TUNGSTEN_WALL, LIGHTNING_ROD,
            COKE_OVEN_MAIN, CRYO_DISTILLATION_MAIN, HEAT_EXCHANGER_MAIN, LOGISTICS_CORE,
            IMPLOSION_COMPRESSOR_MAIN, FUSION_REACTOR_MAIN;
    public static RegisteredBlock<BasicMachineBlock> DISTILLATION_TOWER_MAIN;
    public static void registerAll() {
        if (!NAMED.isEmpty()) throw new IllegalStateException("Multiblocks registered twice");
        GTHeatExchangers.registerAll();
        for (var part : SharedLargeMachineParts.DEFINITIONS)
            NAMED.put(part.originalId(), RegisteredBlock.register(part.name(), () -> MultiblockBlockFactory.create(part)));
        for (var part : OriginalMultiblockPartData.ALL)
            if (!NAMED.containsKey(part.originalId()))
                NAMED.put(part.originalId(), RegisteredBlock.register(part.path(), () -> MultiblockBlockFactory.structural(part)));
        CENTRIFUGE_PART = findPart(18100); ELECTROLYZER_PART = findPart(18105);
        HEAT_TRANSMITTER = findPart(18101); DISTILLATION_TOWER_PART = findPart(18102);
        TANK_WALL = findPart(18002); TANK_WALL_DENSE = findPart(18022); FIRE_BRICKS = findPart(18000);
        IMPLOSION_COMPRESSOR_WALL = findPart(18023); LARGE_NIOBIUM_TITANIUM_COIL = findPart(18041);
        LARGE_COPPER_COIL = findPart(18040); LARGE_IRIDIUM_COIL = findPart(18045);
        DENSE_TITANIUM_WALL = findPart(18026); TUNGSTEN_WALL = findPart(18004); LIGHTNING_ROD = findPart(18104);
        COKE_OVEN_MAIN = controller("coke_oven_main", () -> new CokeOvenControllerBlock(properties(6)));
        CRYO_DISTILLATION_MAIN = controller("cryo_distillation_main", () -> new CryoDistillationControllerBlock(properties(6)));
        HEAT_EXCHANGER_MAIN = controller("heat_exchanger_main", () -> new LargeHeatExchangerControllerBlock(properties(6)));
        //? if >=1.21.1 {
        /*LOGISTICS_CORE = controller("logistics_core", () -> new LogisticsCoreControllerBlock(properties(6)));
        DISTILLATION_TOWER_MAIN = RegisteredBlock.reference("distillation_tower_main");
        *///?} else {
        LOGISTICS_CORE = RegisteredBlock.reference("logistics_core");
        IMPLOSION_COMPRESSOR_MAIN = controller("implosion_compressor_main", () -> new ImplosionCompressorControllerBlock(properties(12.5f)));
        FUSION_REACTOR_MAIN = controller("fusion_reactor_main", () -> new FusionReactorControllerBlock(properties(12.5f)));
        var original = OriginalMultiblockMachineParameters.distillationTower();
        var spec = new com.gregtech.gregtech.api.machine.BasicMachineSpec(original.id(), original.material(),
                original.machineName(), original.energyType(), original.tier(), original.energyIn(), original.energyOut(),
                original.hardness(), original.blastResistance(), com.gregtech.gregtech.api.energy.FaceConfig.from(original.faceConfig()),
                original.constructionMaterials(), com.gregtech.gregtech.data.MachineRecipeMaps.DistillationTower,
                original.parallelLimit(), original.energyInMin(), original.energyInMax());
        DISTILLATION_TOWER_MAIN = RegisteredBlock.wrap(spec.id(),
                com.gregtech.gregtech.api.machine.MachineRegistry.registerBasicMachine(spec, props -> new MultiblockControllerBlock(spec, props)));
        //?}
    }
    private static BlockBehaviour.Properties properties(float hardness) {
        return BlockBehaviour.Properties.of().strength(hardness, hardness).requiresCorrectToolForDrops();
    }
    private static RegisteredBlock<Block> controller(String id, java.util.function.Supplier<Block> factory) {
        var block = RegisteredBlock.register(id, factory); CONTROLLERS.put(id, block); return block;
    }
    public static RegisteredBlock<Block> findPart(int id) { return NAMED.get(id); }
    public static List<RegisteredBlock<Block>> parts() {
        return OriginalMultiblockPartData.ALL.stream().map(part -> NAMED.get(part.originalId())).toList();
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
