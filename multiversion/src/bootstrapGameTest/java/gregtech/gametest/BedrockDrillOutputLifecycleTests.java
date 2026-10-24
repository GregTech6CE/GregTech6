package gregtech.gametest;

import gregtech.block.machine.OriginalBedrockDrillControllerBlock;
import gregtech.blockentity.machine.BedrockDrillControllerBlockEntity;
import gregtech.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.content.multiblock.LargeMachineParts;
import gregtech.content.multiblock.OriginalHighTechControllerLayouts;
import gregtech.data.GregTechTags;
import gregtech.registry.GTBlockEntities;
import gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
//?}

/** Registered native drilling/output paths; replacement is not claimed as an actual chunk unload. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class BedrockDrillOutputLifecycleTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

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

    private static boolean sameItem(ItemStack expected, ItemStack actual) {
//? if >=1.21.1 {
/*        return ItemStack.isSameItemSameComponents(expected, actual);
*///?} else {
        return ItemStack.isSameItemSameTags(expected, actual);
//?}
    }

    private static int itemCount(ChestBlockEntity chest) {
        int total = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) total += chest.getItem(slot).getCount();
        return total;
    }

    private static void onlyProduct(GameTestHelper h, ChestBlockEntity chest, ItemStack product) {
        h.assertTrue(itemCount(chest) == product.getCount(), "native chest contains exactly one completed product");
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            var stack = chest.getItem(slot);
            h.assertTrue(stack.isEmpty() || sameItem(product, stack), "export preserves the actual mined item identity");
        }
    }

    private static void tick(GameTestHelper h, BedrockDrillControllerBlockEntity drill) {
        var block = (OriginalBedrockDrillControllerBlock) drill.getBlockState().getBlock();
        var ticker = block.getTicker(h.getLevel(), drill.getBlockState(), GTBlockEntities.BEDROCK_DRILL.get());
        h.assertTrue(ticker != null, "registered drill supplies its native server ticker");
        ticker.tick(h.getLevel(), drill.getBlockPos(), drill.getBlockState(), drill);
    }

    /** Preserve touched native cells and BE data; teardown suppresses drops of disposable test stock. */
    private static final class Fixture implements AutoCloseable {
        private record Saved(BlockState state, CompoundTag data) {}
        final GameTestHelper h;
        final BlockPos pos;
        final java.util.Map<BlockPos, Saved> cells = new java.util.LinkedHashMap<>();
        BedrockDrillControllerBlockEntity drill;
        IItemHandler output;
        IFluidHandler fluids;

        Fixture(GameTestHelper h, int index) {
            this.h = h;
            // Outside the 1x1x1 template and every other suite fixture; each case has its own region.
            pos = new BlockPos(23_000_000 + index * 128, 200, 23_000_000);
        }

        void set(BlockPos p, BlockState state) {
            if (!cells.containsKey(p)) {
                var entity = h.getLevel().getBlockEntity(p);
                cells.put(p.immutable(), new Saved(h.getLevel().getBlockState(p), entity == null ? null : save(h, entity)));
            }
            h.getLevel().removeBlockEntity(p);
            h.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
            h.getLevel().setBlock(p, state, FLAGS);
        }

        void build() {
            // Include the full shell, output chest and neighboring cells so terrain/adjacent
            // containers cannot affect this test. Every touched cell is restored in close().
            for (var p : BlockPos.betweenClosed(pos.offset(-3, -6, -3), pos.offset(3, 2, 3)))
                set(p, Blocks.AIR.defaultBlockState());
            var block = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof OriginalBedrockDrillControllerBlock)
                    .findFirst().orElseThrow();
            set(pos, block.defaultBlockState());
            for (var cell : OriginalHighTechControllerLayouts.bedrockDrill().entrySet())
                set(pos.offset(cell.getKey()), cell.getValue().defaultBlockState());
            set(pos.above(), Blocks.AIR.defaultBlockState());
            h.assertTrue(h.getLevel().getBlockEntity(pos) instanceof BedrockDrillControllerBlockEntity,
                    "actual registered drill creates its native controller");
            drill = (BedrockDrillControllerBlockEntity) h.getLevel().getBlockEntity(pos);
            h.assertTrue(StructureTestSupport.ok(drill), "registered drill heads and titanium shell form over native bedrock");
//? if >=1.21.1 {
/*            output = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            fluids = h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
*///?} else {
            output = drill.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
            fluids = drill.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null);
//?}
            h.assertTrue(output != null && fluids != null, "native item and lubricant capabilities are registered");
            h.assertTrue(fluids.fill(new FluidStack(GTFluids.still("Lubricant").get(), 200), IFluidHandler.FluidAction.EXECUTE) == 200,
                    "actual lubricant fills two real drilling cycles");
        }

        void charge() {
            var port = (MultiblockPortBlockEntity) h.getLevel().getBlockEntity(pos.offset(1, -1, 0));
            h.assertTrue(port.isBoundTo(pos) && port.doEnergyInjection(GregTechTags.Energy.RU, Direction.EAST, 2048, 16, true) == 16,
                    "real bound input port accepts exactly 32768 RU");
        }

        ItemStack mine() {
            charge();
            tick(h, drill);
            var product = output.getStackInSlot(0).copy();
            h.assertTrue(!product.isEmpty() && product.getCount() == 1, "real powered and lubricated mining produces one native item");
            resources(0, 100);
            return product;
        }

        void resources(long energy, int lubricant) {
            h.assertTrue(drill.getEnergyStored(GregTechTags.Energy.RU, null) == energy
                            && fluids.getFluidInTank(0).getAmount() == lubricant,
                    "drill preserves exact RU/lubricant: " + energy + "/" + lubricant);
        }

        ChestBlockEntity chest() {
            set(pos.above(), Blocks.CHEST.defaultBlockState());
            h.assertTrue(h.getLevel().getBlockEntity(pos.above()) instanceof ChestBlockEntity, "native upper chest exists");
            return (ChestBlockEntity) h.getLevel().getBlockEntity(pos.above());
        }

        @Override public void close() {
            for (var p : cells.keySet()) h.getLevel().removeBlockEntity(p);
            for (var entry : cells.entrySet()) {
                var p = entry.getKey(); var saved = entry.getValue();
                h.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), FLAGS);
                h.getLevel().setBlock(p, saved.state(), FLAGS);
                var entity = h.getLevel().getBlockEntity(p);
                if (entity != null && saved.data() != null) load(h, entity, saved.data());
            }
        }
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void intactDrillExportsCompletedNativeProduct(GameTestHelper h) {
        try (var f = new Fixture(h, 0)) {
            f.build(); var product = f.mine(); var chest = f.chest();
            tick(h, f.drill);
            onlyProduct(h, chest, product);
            h.assertTrue(f.output.getStackInSlot(0).isEmpty(), "completed stock leaves the intact drill");
            f.resources(0, 100);
            tick(h, f.drill);
            onlyProduct(h, chest, product);
            f.resources(0, 100);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void brokenDrillExportsCompletedProductWithoutMiningAndResumesAfterRepair(GameTestHelper h) {
        try (var f = new Fixture(h, 1)) {
            f.build(); var product = f.mine(); f.charge();
            var head = f.pos.offset(1, -4, 0);
            h.getLevel().setBlock(head, Blocks.AIR.defaultBlockState(), FLAGS);
            h.assertTrue(!StructureTestSupport.ok(f.drill), "breaking an actual required drill head invalidates the shell");
            var chest = f.chest();
            tick(h, f.drill);
            onlyProduct(h, chest, product);
            h.assertTrue(f.output.getStackInSlot(0).isEmpty(), "broken shell does not trap already-mined output");
            f.resources(32768, 100);
            tick(h, f.drill);
            onlyProduct(h, chest, product);
            f.resources(32768, 100);
            h.assertTrue(f.output.getStackInSlot(0).isEmpty(), "broken shell cannot spend the next fully funded cycle");
            h.getLevel().setBlock(head, LargeMachineParts.block(18103).defaultBlockState(), FLAGS);
            gregtech.internal.multiblock.tracking.StructureScheduler.flush(h.getLevel());
            tick(h, f.drill);
            h.assertTrue(StructureTestSupport.ok(f.drill) && f.output.getStackInSlot(0).getCount() == 1,
                    "repair reforms the real shell and resumes native mining");
            var second = f.output.getStackInSlot(0).copy();
            f.resources(0, 0);
            onlyProduct(h, chest, product);
            tick(h, f.drill);
            h.assertTrue(itemCount(chest) == product.getCount() + second.getCount() && f.output.getStackInSlot(0).isEmpty(),
                    "two completed real cycles export exactly two items without duplication");
            tick(h, f.drill);
            h.assertTrue(itemCount(chest) == 2, "additional empty tick cannot duplicate either product");
            f.resources(0, 0);
        }
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void removedDrillCannotExportPendingNativeProduct(GameTestHelper h) {
        try (var f = new Fixture(h, 2)) {
            f.build(); var product = f.mine(); var chest = f.chest();
            var old = f.drill; var pending = f.output; var oldState = old.getBlockState();
            // Native BE removal/replacement retains the old object's genuinely mined stock.
            // This is an invalidated-callback test, not a simulated ChunkEvent.Unload claim.
            f.set(f.pos, oldState);
            var replacement = (BedrockDrillControllerBlockEntity) h.getLevel().getBlockEntity(f.pos);
            h.assertTrue(old.isRemoved() && replacement != old && StructureTestSupport.ok(replacement)
                            && pending.getStackInSlot(0).getCount() == 1,
                    "removed old identity retains its real pending product while replacement is installed");
            tick(h, old);
            h.assertTrue(itemCount(chest) == 0 && sameItem(product, pending.getStackInSlot(0))
                            && pending.getStackInSlot(0).getCount() == 1,
                    "stale native ticker cannot export the removed owner's completed stock");
            tick(h, replacement);
            h.assertTrue(itemCount(chest) == 0 && replacement.getEnergyStored(GregTechTags.Energy.RU, null) == 0,
                    "replacement neither inherits nor duplicates the old controller's resources");
        }
        h.succeed();
    }
}
