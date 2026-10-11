package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.crucible.*;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.blockentity.machine.*;
import com.gregtech.gregtech.content.multiblock.SharedLargeCrucibleStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.*;
import java.util.List;

/** Three finite original gas/fire, actual ordinary tick, and formed large-vessel boundaries. */
@net.minecraftforge.gametest.GameTestHolder("gregtech_crucible_hazards")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class CrucibleHazardSourceTests {
    private static BlockPos arena(GameTestHelper h,int y) {
        var pos=h.absolutePos(new BlockPos(2,0,2)).atY(y);
        for(var p:BlockPos.betweenClosed(pos.offset(-7,-2,-7),pos.offset(7,7,7)))
            h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),3);
        return pos;
    }
    private static Cow cow(GameTestHelper h,BlockPos pos,double x) {
        var cow=new Cow(EntityType.COW,h.getLevel());cow.setNoAi(true);cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);cow.setHealth(20);
        cow.setPos(pos.getX()+x,pos.getY()+1,pos.getZ()+0.5);h.getLevel().addFreshEntity(cow);return cow;
    }
    private static Block block(String path) {return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech",path));}
    private static void temperature(GameTestHelper h,com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity entity,long t) {
        var tag=entity.saveWithoutMetadata();
        tag.putLong("gt.temperature",t);tag.putLong("gt.temperature.old",t);tag.putLong("gt.energy.buffer",0);tag.putInt("gt.cooldown",100);
        entity.load(tag);
    }
    private static void tick(GameTestHelper h,com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity entity) {com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity.serverTick(h.getLevel(),entity.getBlockPos(),entity.getBlockState(),entity);}
    @GameTest(template="test_empty",timeoutTicks=80)
    public static void sourceGasBoundsDamageAndWdFireEligibility(GameTestHelper h) {
        var pos=arena(h,205);
        for(var profile:List.of(CrucibleHazards.SMALL,CrucibleHazards.LARGE)) {
            var inside=cow(h,pos,1.5);var outer=cow(h,pos,4.6);
            SmelteryFireHelper.vaporize(h.getLevel(),pos,profile,new CrucibleProcess.Vapor(GTMaterialRegistry.get("Water"),GTValues.U));
            float expected=profile.largeVessel()?5.84F:2.92F;
            h.assertTrue(Math.abs(inside.getHealth()-(20F-expected))<0.0001F,"original material373K and2/4 multiplier");
            h.assertTrue(Math.abs(outer.getHealth()-(profile.largeVessel()?14.16F:20F))<0.0001F,"actual small3 and large5 gas bounds");
            inside.discard();outer.discard();
            h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"steam has no ignition");
        }
        h.assertTrue(!SmelteryFireHelper.igniteAt(h.getLevel(),pos,true),"checked isolated air cannot ignite");
        h.getLevel().setBlock(pos.east(),Blocks.CHEST.defaultBlockState(),3);
        h.assertTrue(SmelteryFireHelper.igniteAt(h.getLevel(),pos,true),"source chest neighbor permits fire");
        h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        h.assertTrue(!SmelteryFireHelper.igniteAt(h.getLevel(),pos,false),"collision-bearing solid is not replaced");
        h.getLevel().setBlock(pos,Blocks.RED_CARPET.defaultBlockState(),3);
        h.assertTrue(SmelteryFireHelper.igniteAt(h.getLevel(),pos,false),"original carpet exception is eligible");
        h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=80)
    public static void actualOrdinarySteamAndExplosivePhaseTick(GameTestHelper h) {
        var pos=arena(h,235);var block=block("smelting_crucible_steel");
        h.getLevel().setBlock(pos,block.defaultBlockState(),3);
        var entity=(com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(entity.addMaterialStacks(List.of(CrucibleMaterialStack.of(GTMaterialRegistry.get("Water"),GTValues.U)),293),"native accepts water");
        temperature(h,entity,373);tick(h,entity);
        h.assertTrue(entity.getContentView().isEmpty() && h.getLevel().getBlockState(pos).is(block),"native steam phase empties charge and keeps hull");
        for(var p:BlockPos.betweenClosed(pos.offset(-3,-1,-3),pos.offset(3,3,3)))
            h.assertTrue(!h.getLevel().getBlockState(p).is(Blocks.FIRE),"steam cannot ignite source radius");
        h.assertTrue(entity.addMaterialStacks(List.of(CrucibleMaterialStack.of(GTMaterialRegistry.get("Gunpowder"),GTValues.U)),293),"native accepts gunpowder");
        temperature(h,entity,314);tick(h,entity);
        h.assertTrue(entity.getContentView().isEmpty() && !h.getLevel().getBlockState(pos).is(block),"native explosive vapor destroys hull and contents");
        h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=80)
    public static void formedLargeContactAndExactMeltdownBoundary(GameTestHelper h) {
        var pos=arena(h,265);var block=(com.gregtech.gregtech.block.machine.LargeCrucibleControllerBlock)block("large_steel_crucible");
        h.getLevel().setBlock(pos,block.defaultBlockState(),3);
        for(var cell:SharedLargeCrucibleStructure.CELLS) if(!cell.air())
            h.getLevel().setBlock(pos.offset(cell.x(),cell.y(),cell.z()),block.variant().wall().defaultBlockState(),3);
        var entity=(LargeCrucibleControllerBlockEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(entity.isStructureOk() && entity.getMeltDownLimitK()==2250,"actual formed original Steel limit2250K");
        temperature(h,entity,1000);var victim=cow(h,pos,.5);
        block.stepOn(h.getLevel(),pos,h.getLevel().getBlockState(pos),victim);
        h.assertTrue(Math.abs(victim.getHealth()-6F)<0.0001F,"large main contact uses original uncapped damage14");victim.discard();
        var wall=pos.east();victim=cow(h,wall,.5);
        block.variant().wall().stepOn(h.getLevel(),wall,h.getLevel().getBlockState(wall),victim);
        h.assertTrue(Math.abs(victim.getHealth()-6F)<0.0001F,"bound wall delegates original uncapped contact");victim.discard();
        temperature(h,entity,2250);LargeCrucibleControllerBlockEntity.serverTick(h.getLevel(),pos,h.getLevel().getBlockState(pos),entity);
        h.assertTrue(h.getLevel().getBlockState(pos).is(block),"at truncated limit the hull survives");
        temperature(h,entity,2251);LargeCrucibleControllerBlockEntity.serverTick(h.getLevel(),pos,h.getLevel().getBlockState(pos),entity);
        for(var p:BlockPos.betweenClosed(pos.offset(-1,0,-1),pos.offset(1,2,1)))
            h.assertTrue(h.getLevel().getBlockState(p).is(Blocks.LAVA)
                    && h.getLevel().getBlockState(p).getValue(LiquidBlock.LEVEL)==1,"source27 cells become flowing lava level1");
        h.succeed();
    }
}
