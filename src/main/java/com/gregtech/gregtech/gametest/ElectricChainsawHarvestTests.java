package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.registry.GTElectricItems;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class ElectricChainsawHarvestTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h, long charge) {
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"GTSawTest"));
        p.setGameMode(GameType.SURVIVAL);p.setOnGround(true);
        var stack=GTElectricItems.ELECTRIC_CHAINSAW.get().assembled(Materials.Steel,100000);
        stack.getOrCreateTag().putLong("gt.charge",charge);p.setItemInHand(InteractionHand.MAIN_HAND,stack);return p;
    }
    private static int count(GameTestHelper h,BlockPos pos,Item item) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(.9)).stream()
                .filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
    }
    @GameTest(template="test_empty")
    public static void leafAndIceDropsAreConvertedOnceWithHardnessEnergy(GameTestHelper h) {
        var p=player(h,10000);var tool=GTElectricItems.ELECTRIC_CHAINSAW.get();var stack=p.getMainHandItem();
        long expected=10000;int x=1;
        for (var block:new Block[]{Blocks.OAK_LEAVES,Blocks.ICE,Blocks.PACKED_ICE}) {
            var local=new BlockPos(x++,2,2);h.setBlock(local,block);var pos=h.absolutePos(local);
            expected-=block==Blocks.OAK_LEAVES?10:25; // GT6 int*float product before ceil; not double-rounding .2 to 11.
            p.gameMode.destroyBlock(pos);
            h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"leaf/ice removed without leaving water: "+block);
            h.assertTrue(count(h,pos,block.asItem())==1,"exactly one converted drop: "+block);
            h.assertTrue(tool.getEnergyStored(stack,GregTechTags.Energy.EU)==expected,"one hardness charge per converted block");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void realLogHarvestAndEmptyStackRestrictions(GameTestHelper h) {
        var p=player(h,1000);var stack=p.getMainHandItem();var tool=GTElectricItems.ELECTRIC_CHAINSAW.get();
        var local=new BlockPos(2,2,2);h.setBlock(local,Blocks.OAK_LOG);var pos=h.absolutePos(local);var state=h.getLevel().getBlockState(pos);
        h.assertTrue(state.canHarvestBlock(h.getLevel(),pos,p),"charged saw harvests log");
        h.assertTrue(tool.getDestroySpeed(stack,state)==2*Materials.Steel.getToolSpeed(),"chainsaw material speed multiplier");
        h.assertTrue(p.gameMode.destroyBlock(pos),"ordinary log follows actual survival break pipeline");
        h.assertTrue(count(h,pos,Items.OAK_LOG)==1&&tool.getEnergyStored(stack,GregTechTags.Energy.EU)==900,"log drops once and costs 100 EU");
        stack.getOrCreateTag().putLong("gt.charge",0);
        h.assertTrue(tool.getDestroySpeed(stack,state)==0&&!tool.isCorrectToolForDrops(stack,state),"empty saw cannot harvest");
        stack.getOrCreateTag().putLong("gt.charge",1000);stack.setCount(2);
        h.assertTrue(tool.getDestroySpeed(stack,state)==0,"stacked saw is unusable");
        stack.setCount(1);
        h.assertTrue(!tool.isCorrectToolForDrops(stack,Blocks.STONE.defaultBlockState()),"saw is not a pickaxe");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void protectionCancellationAndCreativeNeverConvertOrCharge(GameTestHelper h) {
        var p=player(h,1000);var stack=p.getMainHandItem();var tool=GTElectricItems.ELECTRIC_CHAINSAW.get();
        var local=new BlockPos(2,2,2);h.setBlock(local,Blocks.OAK_LEAVES);var pos=h.absolutePos(local);
        java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> deny=e->{if(e.getPlayer()==p&&e.getPos().equals(pos))e.setCanceled(true);};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST,false,net.minecraftforge.event.level.BlockEvent.BreakEvent.class,deny);
        try {p.gameMode.destroyBlock(pos);} finally {net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(deny);}
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.OAK_LEAVES)&&count(h,pos,Items.OAK_LEAVES)==0,"cancelled break neither removes nor duplicates leaves");
        h.assertTrue(tool.getEnergyStored(stack,GregTechTags.Energy.EU)==1000,"cancelled break costs no energy");
        p.setGameMode(GameType.CREATIVE);p.gameMode.destroyBlock(pos);
        h.assertTrue(h.getLevel().getBlockState(pos).isAir()&&count(h,pos,Items.OAK_LEAVES)==0,"creative break has no converted drops");
        h.assertTrue(tool.getEnergyStored(stack,GregTechTags.Energy.EU)==1000,"creative break costs no energy");h.succeed();
    }
}
