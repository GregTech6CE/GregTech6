package gregtech.gametest;

import gregtech.content.block.misc.LongDistEndpointBlock;
import gregtech.content.block.misc.LongDistPipeBlock;
import gregtech.content.registry.GTMiscBlocks;
import gregtech.content.registry.GTTanks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** GT6 BlockLongDistPipe metadata 0 carries items; metadata 1+ carries fluids. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class LongDistPipelineTypeTests {
    private static final BlockPos SENDER = new BlockPos(1, 2, 1);
    private static final BlockPos RECEIVER = new BlockPos(5, 2, 1);
    private static final BlockPos TARGET = new BlockPos(6, 2, 1);

    private LongDistPipelineTypeTests() {}

    private static LongDistPipeBlock pipe(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
        if (!(block instanceof LongDistPipeBlock pipe)) throw new AssertionError(id + " is not a live pipeline block");
        return pipe;
    }

    private static void build(GameTestHelper helper, boolean fluid, Block pipe) {
        var endpoint = fluid ? GTMiscBlocks.LONG_DIST_ENDPOINT_FLUID.get() : GTMiscBlocks.LONG_DIST_ENDPOINT_ITEM.get();
        helper.setBlock(SENDER, endpoint.defaultBlockState().setValue(LongDistEndpointBlock.FACING, Direction.WEST));
        helper.setBlock(RECEIVER, endpoint.defaultBlockState().setValue(LongDistEndpointBlock.FACING, Direction.WEST));
        for (int x = 2; x <= 4; x++) helper.setBlock(new BlockPos(x, 2, 1), pipe);
    }

    @GameTest(template = "test_blueprint_empty")
    public static void itemLineUsesItemPipeAndRejectsFluidPipe(GameTestHelper helper) {
        LongDistPipeBlock item = pipe("long_dist_pipe_item");
        LongDistPipeBlock fluid = pipe("long_dist_pipe_fluid");
        helper.assertTrue(item.acceptsPipeline(false) && !item.acceptsPipeline(true)
                && fluid.acceptsPipeline(true) && !fluid.acceptsPipeline(false),
                "GT6 item and fluid pipe forms have distinct route types");
        build(helper, false, item);
        helper.setBlock(TARGET, Blocks.CHEST);
        var sender = helper.getLevel().getBlockEntity(helper.absolutePos(SENDER));
        var relay = sender.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST)
                .orElseThrow(IllegalStateException::new);
        ItemStack offered = new ItemStack(Items.DIAMOND, 3);
        helper.assertTrue(relay.getSlots() > 0 && relay.insertItem(0, offered, true).isEmpty(),
                "item pipes connect an item endpoint to the receiving chest");
        helper.setBlock(new BlockPos(3, 2, 1), fluid);
        helper.assertTrue(relay.getSlots() == 0 && relay.insertItem(0, offered, false).getCount() == 3,
                "replacing one item pipe with a fluid pipe immediately breaks the item route");
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_empty")
    public static void fluidLineUsesFluidPipeAndRejectsItemPipe(GameTestHelper helper) {
        LongDistPipeBlock fluid = pipe("long_dist_pipe_fluid");
        LongDistPipeBlock item = pipe("long_dist_pipe_item");
        build(helper, true, fluid);
        helper.setBlock(TARGET, GTTanks.DRUM_STEEL.get());
        var sender = helper.getLevel().getBlockEntity(helper.absolutePos(SENDER));
        var relay = sender.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST)
                .orElseThrow(IllegalStateException::new);
        FluidStack offered = new FluidStack(Fluids.WATER, 250);
        helper.assertTrue(relay.getTanks() > 0 && relay.fill(offered, FluidAction.SIMULATE) == 250,
                "fluid pipes connect a fluid endpoint to the receiving drum");
        helper.setBlock(new BlockPos(3, 2, 1), item);
        helper.assertTrue(relay.getTanks() == 0 && relay.fill(offered, FluidAction.EXECUTE) == 0,
                "replacing one fluid pipe with an item pipe immediately breaks the fluid route");
        helper.succeed();
    }

    @GameTest(template = "test_blueprint_empty")
    public static void legacyPipeAliasRetainsSourceFluidIdentity(GameTestHelper helper) {
        helper.assertTrue(GTMiscBlocks.LONG_DIST_PIPE.get() == pipe("long_dist_pipe_fluid")
                        && !GTMiscBlocks.LONG_DIST_PIPE.get().acceptsPipeline(false)
                        && GTMiscBlocks.LONG_DIST_PIPE.get().acceptsPipeline(true),
                "legacy field aliases the source fluid pipe and cannot impersonate an item line");
        build(helper, true, GTMiscBlocks.LONG_DIST_PIPE.get());
        helper.setBlock(TARGET, GTTanks.DRUM_STEEL.get());
        var sender = helper.getLevel().getBlockEntity(helper.absolutePos(SENDER));
        var relay = sender.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.WEST)
                .orElseThrow(IllegalStateException::new);
        helper.assertTrue(relay.getTanks() > 0,
                "the legacy field still connects its source fluid line");
        build(helper, false, GTMiscBlocks.LONG_DIST_PIPE.get());
        helper.setBlock(TARGET, Blocks.CHEST);
        var itemSender = helper.getLevel().getBlockEntity(helper.absolutePos(SENDER));
        var items = itemSender.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST).orElseThrow(IllegalStateException::new);
        var offered = new ItemStack(Items.APPLE, 3);
        helper.assertTrue(items.getSlots() == 0 && items.insertItem(0, offered, false).getCount() == 3,
                "fluid alias refuses item routing without consuming the offered stack");
        helper.succeed();
    }
}
