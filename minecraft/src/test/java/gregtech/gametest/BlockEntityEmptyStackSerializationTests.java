package gregtech.gametest;

import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.block.energy.ReactorCoreBlock;
import gregtech.content.block.inventory.LockerBlock;
import gregtech.content.block.tool.MaterialAnvilBlock;
import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.blockentity.energy.ReactorCoreBlockEntity;
import gregtech.content.blockentity.inventory.LockerBlockEntity;
import gregtech.content.blockentity.tool.MaterialAnvilBlockEntity;
import gregtech.engine.stack.NbtCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.function.Predicate;

/** Regression checks for empty item/fluid stack encoding in shared BE serialization. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class BlockEntityEmptyStackSerializationTests {
    private static final String BATCH = "be_empty_stack_nbt";

    private BlockEntityEmptyStackSerializationTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void emptyStacksAndBlockEntitiesRoundTripCleanly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HolderLookup.Provider lookup = level.registryAccess();

        CompoundTag emptyItem = (CompoundTag) NbtCompat.saveItem(ItemStack.EMPTY, lookup);
        helper.assertTrue(NbtCompat.readItem(emptyItem, lookup).isEmpty(), "empty ItemStack round-trips empty");

        CompoundTag emptyFluid = (CompoundTag) NbtCompat.saveFluid(FluidStack.EMPTY, lookup);
        helper.assertTrue(NbtCompat.readFluid(emptyFluid, lookup).isEmpty(), "empty FluidStack round-trips empty");

        assertBlockEntityRoundTrip(helper, level, new BlockPos(0, 1, 0),
                b -> b instanceof ReactorCoreBlock, ReactorCoreBlockEntity.class, "reactor core");
        assertBlockEntityRoundTrip(helper, level, new BlockPos(1, 1, 0),
                b -> b instanceof MaterialAnvilBlock, MaterialAnvilBlockEntity.class, "material anvil");
        assertBlockEntityRoundTrip(helper, level, new BlockPos(2, 1, 0),
                b -> b instanceof LockerBlock, LockerBlockEntity.class, "locker");
        assertBlockEntityRoundTrip(helper, level, new BlockPos(3, 1, 0),
                b -> b instanceof EnergyNodeBlock node && node.spec().batterySlots() > 0,
                EnergyNodeBlockEntity.class, "battery box");

        helper.succeed();
    }

    private static void assertBlockEntityRoundTrip(GameTestHelper helper, ServerLevel level, BlockPos relPos,
            Predicate<Block> blockMatcher, Class<? extends BlockEntity> beClass, String label) {
        Block block = BuiltInRegistries.BLOCK.stream().filter(blockMatcher).findFirst()
                .orElseThrow(() -> new IllegalStateException("missing registered block for " + label));
        BlockPos pos = helper.absolutePos(relPos);
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        BlockEntity be = level.getBlockEntity(pos);
        helper.assertTrue(beClass.isInstance(be), label + " block entity should exist");

        HolderLookup.Provider lookup = level.registryAccess();
        CompoundTag saved = NbtCompat.saveWithoutMetadata(be, lookup);
        CompoundTag itemTag = NbtCompat.saveForBlockItem(be, lookup);
        CompoundTag updateTag = NbtCompat.getUpdateTag(be, lookup);
        helper.assertTrue(!itemTag.isEmpty() || saved.isEmpty(), label + " item snapshot is valid");
        helper.assertTrue(updateTag != null, label + " update tag is non-null");
        NbtCompat.loadBlockEntity(be, saved, lookup);
    }
}
