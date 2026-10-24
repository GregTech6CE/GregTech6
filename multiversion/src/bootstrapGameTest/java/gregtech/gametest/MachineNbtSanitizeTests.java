package gregtech.gametest;

import gregtech.internal.energy.FaceConfig;
import gregtech.internal.machine.BasicMachineSpec;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import gregtech.block.machine.BasicMachineBlock;
import gregtech.blockentity.machine.BasicMachineBlockEntity;
import gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
//?}

/** Save-load values are treated as untrusted input: re-derived or clamped on load. */
@GameTestHolder("gregtech_nbt_sanitize")
@PrefixGameTestTemplate(false)
public final class MachineNbtSanitizeTests {
    private MachineNbtSanitizeTests() {}

    private static BasicMachineBlock crusher() {
        return (BasicMachineBlock) BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof BasicMachineBlock basic && basic.basicSpec().machineName().equals("crusher"))
                .findFirst().orElseThrow();
    }

    private static CompoundTag save(GameTestHelper h, BlockEntity entity) {
//? if >=1.21.1 {
/*        return entity.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return entity.saveWithoutMetadata();
//?}
    }

    private static void load(GameTestHelper h, BlockEntity entity, CompoundTag tag) {
//? if >=1.21.1 {
/*        entity.loadWithComponents(tag, h.getLevel().registryAccess());
*///?} else {
        entity.load(tag);
//?}
    }

    private static void tick(GameTestHelper h, BasicMachineBlockEntity machine) {
        BasicMachineBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }

    @GameTest(template = "test_empty")
    public static void pendingJobSurvivesTamperedValues(GameTestHelper h) {
        var block = crusher();
        var pos = h.absolutePos(new BlockPos(4, 3, 4));
        var map = new RecipeMap(null, "gt.recipe.test.sanitize_" + java.util.UUID.randomUUID(),
                "Sanitize fixture", "test", 1, 1, 1, 0, 1, 0, 1, false, false, false, false);
        try {
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var machine = (BasicMachineBlockEntity) h.getLevel().getBlockEntity(pos);
            var spec = BasicMachineSpec.builder("sanitize_fixture", block.basicSpec().material())
                    .machineType("crusher").energy(GregTechTags.Energy.TU, 1).energyRange(1, 1)
                    .processingEfficiency(10000).parallel(1).recipes(map).faces(FaceConfig.ALL_SIDES).build();
            machine.setSpec(spec);
            map.mRecipeList.add(new Recipe(new ItemStack[]{new ItemStack(Items.COBBLESTONE)},
                    new ItemStack[]{new ItemStack(Items.STICK, 4)}, null, null, null,
                    new FluidStack[]{new FluidStack(Fluids.WATER, 100)}, 4, 1, 0));
            machine.inventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 2));
            tick(h, machine);
            h.assertTrue(machine.minimumEnergy() == 1 && machine.parallelCount() == 1
                    && machine.machineControl(null).progressMax() == 4, "fixture starts a four-TU job");

            var snapshot = save(h, machine);
            snapshot.putLong("gt.energy", Long.MAX_VALUE);
            snapshot.putLong("gt.progress", 999L);
            snapshot.putLong("gt.max_progress", 999L);
            snapshot.putInt("gt.parallel", 9999);
            snapshot.putLong("gt.min_energy", -100L);
            h.getLevel().removeBlockEntity(pos);
            var restored = new BasicMachineBlockEntity(machine.getType(), pos, machine.getBlockState());
            restored.setSpec(spec);
            h.getLevel().setBlockEntity(restored);
            load(h, restored, snapshot);

            h.assertTrue(restored.getEnergyTick() == spec.energyInMax(), "saved energy is capped at the spec buffer");
            h.assertTrue(restored.parallelCount() == spec.parallelLimit(), "saved parallel is clamped to the spec limit");
            h.assertTrue(restored.minimumEnergy() == 1 && restored.machineControl(null).progressMax() == 4,
                    "resolvable job cost is recomputed from the recipe");
            h.assertTrue(restored.machineControl(null).progress() == 4,
                    "saved progress is clamped to the recomputed job size");
        } finally {
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
            RecipeMap.unregister(map);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void idleForgedJobStateIsCleared(GameTestHelper h) {
        var block = crusher();
        var pos = h.absolutePos(new BlockPos(4, 3, 4));
        try {
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var machine = (BasicMachineBlockEntity) h.getLevel().getBlockEntity(pos);
            var tag = new CompoundTag();
            tag.putLong("gt.energy", Long.MAX_VALUE);
            tag.putLong("gt.progress", 999L);
            tag.putLong("gt.max_progress", 999L);
            tag.putInt("gt.parallel", 9999);
            tag.putLong("gt.min_energy", -100L);
            load(h, machine, tag);
            h.assertTrue(machine.getEnergyTick() == machine.spec().energyInMax(),
                    "idle saved energy is capped at the spec buffer");
            h.assertTrue(machine.parallelCount() == machine.spec().parallelLimit(),
                    "idle saved parallel is clamped to the spec limit");
            h.assertTrue(machine.minimumEnergy() == 0 && machine.machineControl(null).progressMax() == 0
                    && machine.machineControl(null).progress() == 0,
                    "idle forged job state is cleared");
        } finally {
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
        h.succeed();
    }
}
