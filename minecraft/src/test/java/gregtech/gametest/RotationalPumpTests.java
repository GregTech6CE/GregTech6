package gregtech.gametest;

import gregtech.content.block.energy.PumpBlock;
import gregtech.content.blockentity.energy.PumpBlockEntity;
import gregtech.content.data.GregTechTags;
import gregtech.engine.energy.PumpWorkRules;
import gregtech.engine.stack.NbtCompat;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class RotationalPumpTests {
    private RotationalPumpTests() {}

    @GameTest(template = "test_empty")
    public static void pumpSkipsWaterloggedStorageAndDrainsSources(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "rotational_pump_bronze"));
        var state = block.defaultBlockState().setValue(PumpBlock.FACING, Direction.EAST);
        level.setBlockAndUpdate(pos, state);
        var pump = (PumpBlockEntity) level.getBlockEntity(pos);
        // Resume at the center of the normal 128-wide scan so the next three columns are in the fixture.
        var cursor = new CompoundTag();
        cursor.putInt("pumpX", PumpWorkRules.SCAN_AREA / 2);
        cursor.putInt("pumpZ", PumpWorkRules.SCAN_AREA / 2);
        NbtCompat.loadBlockEntity(pump, cursor, level.registryAccess());
        pump.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 32, PumpWorkRules.START_ENERGY / 32, true);

        var chestPos = pos.east();
        var waterlogged = Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlockAndUpdate(chestPos, waterlogged);
        var chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 2));
        PumpBlockEntity.serverTick(level, pos, state, pump);
        helper.assertTrue(level.getBlockState(chestPos).equals(waterlogged) && level.getBlockEntity(chestPos) == chest
                        && chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 2,
                "Scanning a waterlogged chest must preserve its state, block entity, and contents");
        helper.assertTrue(pump.tankForInspection().isEmpty() && pump.storedPumpEnergy() == PumpWorkRules.START_ENERGY,
                "Skipping a waterlogged host must leave the tank and RU buffer unchanged");

        var sourcePos = chestPos.south();
        for (var source : List.of(Blocks.WATER, Blocks.LAVA)) {
            level.setBlockAndUpdate(sourcePos, source.defaultBlockState());
            for (int tick = 0; tick < 3; tick++) PumpBlockEntity.serverTick(level, pos, state, pump);
            var stored = pump.tankForInspection().getFluidInTank(0);
            helper.assertTrue(level.getBlockState(sourcePos).isAir() && stored.getAmount() == 1000
                            && stored.getFluid() == source.defaultBlockState().getFluidState().getType(),
                    "The pump must remove one source block and store one bucket of the same fluid");
            helper.assertTrue(pump.storedPumpEnergy() == PumpWorkRules.START_ENERGY - PumpWorkRules.DRAIN_ENERGY,
                    "A successful drain must spend exactly one operation's RU");
            pump.tankForInspection().setEmpty();
            pump.doEnergyInjection(GregTechTags.Energy.RU, Direction.WEST, 32, PumpWorkRules.DRAIN_ENERGY / 32, true);
            sourcePos = sourcePos.south();
        }
        helper.succeed();
    }
}
