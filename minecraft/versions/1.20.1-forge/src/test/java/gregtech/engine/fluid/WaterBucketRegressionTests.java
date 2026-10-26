package gregtech.engine.fluid;

import gregtech.content.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class WaterBucketRegressionTests {
    private WaterBucketRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void vanillaBucketsAndModContainersCollectWorldWater(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 90);
        for (String field : new String[]{"Water", "Ocean", "River_Water", "Swampwater"}) {
            var fluid = field.equals("Water") ? Fluids.WATER : GTFluids.still(field).get();
            var source = fluid.defaultFluidState().createLegacyBlock();
            var pickup = (BucketPickup) source.getBlock();
            helper.assertTrue(fluid.getBucket() == Items.WATER_BUCKET, field + " must expose the vanilla bucket");
            helper.assertTrue(pickup.getPickupSound().orElse(null) == SoundEvents.BUCKET_FILL,
                    field + " must use the vanilla filling sound");

            level.setBlockAndUpdate(pos, source);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            var hit = level.clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),
                    player.getEyePosition().add(player.getLookAngle().scale(player.getBlockReach())),
                    net.minecraft.world.level.ClipContext.Block.OUTLINE,
                    net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY, player));
            helper.assertTrue(hit.getBlockPos().equals(pos), field + " ray must target source, got " + hit.getBlockPos());
            var filled = Items.BUCKET.use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(filled.getObject().is(Items.WATER_BUCKET), field + " BucketItem.use: "
                    + filled.getResult() + ", " + filled.getObject() + ", remaining " + level.getBlockState(pos));
            helper.assertTrue(level.isEmptyBlock(pos), field + " pickup consumes one source");

            level.setBlockAndUpdate(pos, source);
            var handler = new BucketPickupHandlerWrapper(pickup, level, pos);
            var request = new FluidStack(Fluids.WATER, 1000);
            helper.assertTrue(handler.getFluidInTank(0).isFluidEqual(request), field + " container preview must be water");
            helper.assertTrue(handler.drain(500, FluidAction.EXECUTE).isEmpty(), "Partial buckets cannot remove a source");
            helper.assertTrue(handler.drain(1000, FluidAction.SIMULATE).isFluidEqual(request), field + " amount simulation");
            helper.assertTrue(handler.drain(request, FluidAction.SIMULATE).isFluidEqual(request), field + " typed simulation");
            helper.assertTrue(level.getBlockState(pos).equals(source), "Simulation must leave the water in place");
            helper.assertTrue(handler.drain(request, FluidAction.EXECUTE).isFluidEqual(request), field + " typed pickup");
            helper.assertTrue(level.isEmptyBlock(pos), "Typed pickup removes the source");

            level.setBlockAndUpdate(pos, source);
            helper.assertTrue(handler.drain(1000, FluidAction.EXECUTE).isFluidEqual(request), field + " amount pickup");
            level.setBlockAndUpdate(pos, source.setValue(LiquidBlock.LEVEL, 1));
            if (!field.equals("Water"))
                helper.assertTrue(handler.drain(1000, FluidAction.SIMULATE).isEmpty(), "GT flowing water cannot fill a bucket");
            helper.assertTrue(pickup.pickupBlock(level, pos, level.getBlockState(pos)).isEmpty(), "Flowing pickup is refused");
            helper.assertTrue(!level.isEmptyBlock(pos), "Refusing pickup must not delete flowing water");
        }
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void fluidsWithoutBucketsAreNotDeleted(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        var source = GTFluids.still("Oil_Medium").get().defaultFluidState().createLegacyBlock();
        level.setBlockAndUpdate(pos, source);
        var pickup = (BucketPickup) source.getBlock();
        helper.assertTrue(pickup.pickupBlock(level, pos, source).isEmpty(), "Oil has no vanilla bucket");
        helper.assertTrue(level.getBlockState(pos).equals(source), "An unsupported bucket must not delete oil");
        var handler = new BucketPickupHandlerWrapper(pickup, level, pos);
        helper.assertTrue(handler.drain(1000, FluidAction.SIMULATE).isEmpty(), "Do not promise an unsupported pickup");
        helper.assertTrue(handler.drain(1000, FluidAction.EXECUTE).isEmpty(), "Do not drain an unsupported pickup");
        helper.assertTrue(level.getBlockState(pos).equals(source), "Failed container pickup must preserve oil");
        helper.succeed();
    }
}
