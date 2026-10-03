package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.GTWaterParity;
import com.gregtech.gregtech.registry.GTWorldWaterFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_watercompat")
@PrefixGameTestTemplate(false)
public final class WaterCompatibilityTests {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("gregtech", path); }
    private static java.util.List<Block> waters() {
        var result = new java.util.ArrayList<Block>();
        result.add(Blocks.WATER);
        for (String path : GTWaterParity.WORLD_WATER_PATHS) result.add(BuiltInRegistries.BLOCK.get(id(path)));
        return result;
    }
    private static java.util.List<Block> lilies() {
        var result = new java.util.ArrayList<Block>(); result.add(Blocks.LILY_PAD);
        for (Block b : BuiltInRegistries.BLOCK)
            if (b instanceof WaterlilyBlock && BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals("gregtech")) result.add(b);
        return result;
    }
    @GameTest(template="test_empty", timeoutTicks=80)
    public static void realItemsPlaceAllLiliesOnEveryWorldWater(GameTestHelper h) {
        var level=h.getLevel(); var water=h.absolutePos(new BlockPos(2,2,2)); var above=water.above();
        var plants=lilies(); h.assertTrue(plants.size()==18,"vanilla lily plus all 17 GT lilies");
        int checked=0;
        for(Block liquid:waters()) for(Block plant:plants) {
            level.setBlock(above,Blocks.AIR.defaultBlockState(),2);
            level.setBlock(water,liquid.defaultBlockState(),2);
            var original=level.getFluidState(water).getType();
            h.assertTrue(GTWorldWaterFluid.isWaterFamily(original),"registered world water "+liquid);
            var player=h.makeMockSurvivalPlayer();
            player.setPos(water.getX()+0.5,water.getY()+2,water.getZ()+0.5);
            player.setXRot(90); player.setYRot(0);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(plant,2));
            var result=plant.asItem().use(level,player,InteractionHand.MAIN_HAND);
            h.assertTrue(result.getResult().consumesAction()&&level.getBlockState(above).is(plant),"native item placement "+plant+" on "+liquid);
            h.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount()==1,"one lily consumed");
            h.assertTrue(level.getFluidState(water).getType()==original,"placement keeps registered GT fluid identity");
            h.assertTrue(level.getBlockState(above).canSurvive(level,above),"placed lily survives"); checked++;
        }
        com.mojang.logging.LogUtils.getLogger().info("WATER_COMPAT_LILIES placements={} nativeItems=true",checked);
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void surfacePlantsPreserveSourceEmptyIceAndNonwaterRules(GameTestHelper h) {
        var level=h.getLevel(); var water=h.absolutePos(new BlockPos(2,2,2)); var above=water.above();
        level.setBlock(above,Blocks.AIR.defaultBlockState(),2);
        level.setBlock(water,Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,1),2);
        var flowSurvival=new java.util.HashMap<Block,Boolean>();
        for(Block plant:lilies()) flowSurvival.put(plant,plant.defaultBlockState().canSurvive(level,above));
        level.setBlock(water,Blocks.WATER.defaultBlockState(),2);
        level.setBlock(above,Blocks.WATER.defaultBlockState(),2);
        var coveredSurvival=new java.util.HashMap<Block,Boolean>();
        for(Block plant:lilies()) coveredSurvival.put(plant,plant.defaultBlockState().canSurvive(level,above));
        for(Block liquid:waters()) {
            level.setBlock(above,Blocks.AIR.defaultBlockState(),2);
            level.setBlock(water,liquid.defaultBlockState(),2);
            h.assertTrue(Blocks.FROGSPAWN.defaultBlockState().canSurvive(level,above),"frogspawn accepts "+liquid);
            level.setBlock(water,liquid.defaultBlockState().setValue(LiquidBlock.LEVEL,1),2);
            h.assertTrue(!Blocks.FROGSPAWN.defaultBlockState().canSurvive(level,above),"frogspawn rejects flowing "+liquid);
            for(Block plant:lilies()) {
                h.assertTrue(plant.defaultBlockState().canSurvive(level,above)==flowSurvival.get(plant),"vanilla flowing survival parity "+plant+" on "+liquid);
                var player=h.makeMockSurvivalPlayer();
                player.setPos(water.getX()+0.5,water.getY()+2,water.getZ()+0.5); player.setXRot(90);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(plant));
                plant.asItem().use(level,player,InteractionHand.MAIN_HAND);
                h.assertTrue(level.getBlockState(above).isAir()&&player.getItemInHand(InteractionHand.MAIN_HAND).getCount()==1,"native item rejects flowing "+liquid);
            }
            level.setBlock(water,liquid.defaultBlockState(),2);
            level.setBlock(above,Blocks.WATER.defaultBlockState(),2);
            h.assertTrue(!Blocks.FROGSPAWN.defaultBlockState().canSurvive(level,above),"frogspawn rejects submerged surface");
            for(Block plant:lilies()) h.assertTrue(plant.defaultBlockState().canSurvive(level,above)==coveredSurvival.get(plant),"vanilla covered survival parity");
        }
        level.setBlock(above,Blocks.AIR.defaultBlockState(),2);
        for(Block nonwater:java.util.List.of(Blocks.LAVA,Blocks.STONE,BuiltInRegistries.BLOCK.get(id("oil")))) {
            level.setBlock(water,nonwater.defaultBlockState(),2);
            h.assertTrue(!Blocks.FROGSPAWN.defaultBlockState().canSurvive(level,above),"frogspawn rejects nonwater");
            for(Block plant:lilies()) h.assertTrue(!plant.defaultBlockState().canSurvive(level,above),"lily rejects nonwater "+nonwater);
        }
        level.setBlock(water,Blocks.ICE.defaultBlockState(),2);
        for(Block plant:lilies()) h.assertTrue(plant.defaultBlockState().canSurvive(level,above),"vanilla ice support retained");
        h.assertTrue(!Blocks.FROGSPAWN.defaultBlockState().canSurvive(level,above),"frogspawn still rejects ice");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void vanillaBlocksAndContainerApiAcceptGtSourceWater(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(2,2,2));
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(pos.west(),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(pos.above(),Blocks.STONE.defaultBlockState(),2);
        var player=h.makeMockSurvivalPlayer();
        player.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()-1); player.setXRot(45);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        int checked=0;
        for(Block block:java.util.List.of(Blocks.OAK_SLAB, Blocks.OAK_STAIRS, Blocks.OAK_FENCE, Blocks.COBBLESTONE_WALL, Blocks.IRON_BARS, Blocks.OAK_TRAPDOOR, Blocks.OAK_SIGN, Blocks.OAK_WALL_SIGN, Blocks.OAK_HANGING_SIGN, Blocks.OAK_WALL_HANGING_SIGN, Blocks.CHEST, Blocks.ENDER_CHEST, Blocks.CHAIN, Blocks.LANTERN, Blocks.LIGHTNING_ROD, Blocks.CANDLE, Blocks.AMETHYST_CLUSTER, Blocks.HANGING_ROOTS, Blocks.MANGROVE_ROOTS, Blocks.SCAFFOLDING, Blocks.SEA_PICKLE, Blocks.RAIL, Blocks.CAMPFIRE, Blocks.OAK_LEAVES, Blocks.MANGROVE_PROPAGULE, Blocks.POINTED_DRIPSTONE, Blocks.SCULK_SENSOR, Blocks.SCULK_SHRIEKER, Blocks.DECORATED_POT)) {
            level.setBlock(pos,Blocks.WATER.defaultBlockState(),2);
            var baseline=block.getStateForPlacement(new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,new ItemStack(block),hit));
            h.assertTrue(baseline!=null&&baseline.hasProperty(BlockStateProperties.WATERLOGGED)&&baseline.getValue(BlockStateProperties.WATERLOGGED),"vanilla source fixture "+block);
            for(Block liquid:waters()) {
                level.setBlock(pos,liquid.defaultBlockState(),2);
                var original=level.getFluidState(pos).getType();
                var actual=block.getStateForPlacement(new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,new ItemStack(block),hit));
                h.assertTrue(actual==baseline,"native placement parity "+block+" in "+liquid+": "+actual);
                h.assertTrue(level.getFluidState(pos).getType()==original,"placement query preserves registered fluid"); checked++;
            }
        }
        var slab=(SimpleWaterloggedBlock)Blocks.OAK_SLAB;
        for(Block liquid:waters()) {
            var source=liquid.defaultBlockState().getFluidState();
            h.assertTrue(slab.canPlaceLiquid(level,pos,Blocks.OAK_SLAB.defaultBlockState(),source.getType()),"common container accepts tagged source");
            level.setBlock(pos,Blocks.OAK_SLAB.defaultBlockState(),2);
            h.assertTrue(slab.placeLiquid(level,pos,level.getBlockState(pos),source)&&level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED),"direct container filling accepts "+liquid);
            level.setBlock(pos,Blocks.OAK_SLAB.defaultBlockState(),2);
            var flowing=liquid.defaultBlockState().setValue(LiquidBlock.LEVEL,1).getFluidState();
            h.assertTrue(!slab.placeLiquid(level,pos,level.getBlockState(pos),flowing),"direct container still rejects flowing water");
        }
        h.assertTrue(!slab.canPlaceLiquid(level,pos,Blocks.OAK_SLAB.defaultBlockState(),Fluids.LAVA),"common container rejects lava");
        com.mojang.logging.LogUtils.getLogger().info("WATER_COMPAT_PLACEMENT queries={} commonContainer=true",checked);
        h.succeed();
    }
}
