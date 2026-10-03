package com.gregtech.gregtech.gametest;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.GameType;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.item.GTToolItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@GameTestHolder("gregtech_issues")
@PrefixGameTestTemplate(false)
public final class IssueOilTests {
    @GameTest(template="test_empty",timeoutTicks=40)
    public static void oilsAndGasHaveOriginalFlammability(GameTestHelper h) {
        for(String id:List.of("liquid_extra_heavy_oil","liquid_heavy_oil","liquid_medium_oil","liquid_light_oil","gas_natural_gas")) {
            var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech",id));
            h.assertTrue(block instanceof com.gregtech.gregtech.block.GTWorldFluidBlock,"registered combustible "+id);
            h.assertTrue(block.getFlammability(block.defaultBlockState(),h.getLevel(),BlockPos.ZERO,Direction.UP)==1000,"source flammability "+id);
            var flow=BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("gregtech",id+"_flowing"));
            h.assertTrue(block.getFlammability(flow.defaultFluidState().createLegacyBlock(),h.getLevel(),BlockPos.ZERO,Direction.UP)==1000,"flowing flammability "+id);
        }h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=80)
    public static void neighboringVanillaIgniterConsumesOilAndGas(GameTestHelper h) {
        var origin=h.absolutePos(new BlockPos(1,2,1));var level=h.getLevel();var targets=new ArrayList<BlockPos>();
        int i=0;for(String id:List.of("liquid_heavy_oil","liquid_light_oil","gas_natural_gas")) {
            var pos=origin.offset(i++*4,0,0);targets.add(pos);level.setBlockAndUpdate(pos.below(),Blocks.OBSIDIAN.defaultBlockState());
            level.setBlockAndUpdate(pos.east().below(),Blocks.OBSIDIAN.defaultBlockState());
            level.setBlockAndUpdate(pos.east(),Blocks.AIR.defaultBlockState());
            var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech",id));level.setBlockAndUpdate(pos,block.defaultBlockState());
            var p=h.makeMockPlayer(GameType.SURVIVAL);var lighter=new ItemStack(Items.FLINT_AND_STEEL);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,lighter);
            var use=new net.minecraft.world.item.context.UseOnContext(level,p,net.minecraft.world.InteractionHand.MAIN_HAND,lighter,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos.east().below()),Direction.UP,pos.east().below(),false));
            Items.FLINT_AND_STEEL.useOn(use);
        }
        h.runAfterDelay(3,()->{for(var pos:targets)h.assertTrue(level.getBlockState(pos).getFluidState().isEmpty(),"ignited fluid consumed "+pos);h.succeed();});
    }
    @GameTest(template="test_empty",timeoutTicks=30)
    public static void noncombustibleWaterSurvivesHeat(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","swampwater"));
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());h.getLevel().setBlockAndUpdate(pos.below(),Blocks.LAVA.defaultBlockState());
        ((com.gregtech.gregtech.block.GTWorldFluidBlock)block).tick(block.defaultBlockState(),h.getLevel(),pos,net.minecraft.util.RandomSource.create(42));
        h.assertTrue(h.getLevel().getBlockState(pos).is(block),"water does not ignite");h.succeed();
    }
}
