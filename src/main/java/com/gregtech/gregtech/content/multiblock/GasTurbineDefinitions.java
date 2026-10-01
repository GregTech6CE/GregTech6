package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.machine.LargeGasTurbineControllerBlock;
import com.gregtech.gregtech.registry.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

/** GT6 Loader_MultiTileEntities 17231–17234. Values are normal HU/RU packet sizes. */
public final class GasTurbineDefinitions {
    public record Grade(String id, String rotor, String casingMaterial, int input, int output, int wallId, float hardness) {
        public int inputMaximum() { return input * 2; }
        public int outputMaximum() { return output * 2; }
        public int tint() { return GTMaterialRegistry.get(casingMaterial).getColor(); }
        public Block wall() { return wallId == 18022 ? GTMultiblocks.TANK_WALL_DENSE.get() : LargeMachineParts.block(wallId); }
        public boolean accepts(Block block) { return block == wall() || wallId == 18022 && block == GTMultiblocks.LARGE_GAS_TURBINE_WALL.get(); }
    }
    public static final List<Grade> GRADES=OriginalGeneratorParameters.GRADES.stream().map(p->new Grade(p.id(),p.rotor(),p.casingMaterial(),p.input(),p.output(),p.wallId(),p.hardness())).toList();
    private static final List<RegistryObject<Block>> BLOCKS = new ArrayList<>();
    private GasTurbineDefinitions() {}
    public static void register() {
        for (var grade : GRADES) BLOCKS.add(MachineBlockRegistration.block(grade.id(),
                props -> new LargeGasTurbineControllerBlock(grade, props)).strength(grade.hardness(), grade.hardness()).register());
        GTMultiblocks.LARGE_GAS_TURBINE_MAIN = BLOCKS.get(0);
    }
    public static List<RegistryObject<Block>> blocks() { return List.copyOf(BLOCKS); }
    public static Block[] registeredBlocks() {
        return java.util.stream.Stream.concat(
                BLOCKS.stream().map(RegistryObject::get),
                java.util.stream.IntStream.range(17231, 17235).mapToObj(LargeMachineParts::block))
                .toArray(Block[]::new);
    }
    public static Grade grade(Block block) { return ((LargeGasTurbineControllerBlock) block).grade(); }
    public static boolean isController(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof LargeGasTurbineControllerBlock;
    }
}
