package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTElectricItems;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class WoodworkingPlacementTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h) {
        var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"GTSupplyTest"));
        p.setGameMode(GameType.SURVIVAL);
        var saw=GTElectricItems.ELECTRIC_CHAINSAW.get().assembled(Materials.Steel,10000);
        saw.getOrCreateTag().putLong("gt.charge",500);p.setItemInHand(InteractionHand.MAIN_HAND,saw);return p;
    }
    private static boolean use(GameTestHelper h,net.minecraft.world.entity.player.Player p,BlockPos local,Direction side) {
        var pos=h.absolutePos(local);var stack=p.getMainHandItem();
        return stack.getItem().useOn(new UseOnContext(h.getLevel(),p,InteractionHand.MAIN_HAND,stack,
                new BlockHitResult(Vec3.atCenterOf(pos).add(side.getStepX()*.5,side.getStepY()*.5,side.getStepZ()*.5),side,pos,false))).consumesAction();
    }
    @GameTest(template="test_empty")
    public static void reverseInventorySaplingPlacementPreservesToolAndCreativeSupplies(GameTestHelper h) {
        var p=player(h);var local=new BlockPos(2,2,2);h.setBlock(local,Blocks.DIRT);
        p.getInventory().items.set(10,new ItemStack(Items.OAK_SAPLING,3));
        var birch=new ItemStack(Items.BIRCH_SAPLING,4);birch.getOrCreateTag().putString("test","keep");p.getInventory().items.set(35,birch);
        var toolBefore=p.getMainHandItem().save(new net.minecraft.nbt.CompoundTag());
        h.assertTrue(use(h,p,local,Direction.UP),"charged chainsaw plants from inventory");
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(local.above())).is(Blocks.BIRCH_SAPLING),"highest inventory index wins");
        h.assertTrue(birch.getCount()==3&&birch.getTag().getString("test").equals("keep")&&p.getInventory().items.get(10).getCount()==3,"only selected sapling consumed and NBT retained");
        h.assertTrue(toolBefore.equals(p.getMainHandItem().save(new net.minecraft.nbt.CompoundTag())),"placement never consumes charge, wear or tool");
        h.setBlock(local.above(),Blocks.AIR);p.setGameMode(GameType.CREATIVE);
        h.assertTrue(use(h,p,local,Direction.UP)&&birch.getCount()==3,"creative retains supplies");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void workbenchAndSaplingTargetRestrictionsAndManualTools(GameTestHelper h) {
        var p=player(h);var local=new BlockPos(2,2,2);h.setBlock(local,Blocks.OAK_LOG);
        var tables=new ItemStack(Items.CRAFTING_TABLE,4);p.getInventory().items.set(35,tables);
        h.assertTrue(!use(h,p,local,Direction.EAST)&&tables.getCount()==4,"workbench cannot be placed against wood");
        h.setBlock(local,Blocks.STONE);
        h.assertTrue(use(h,p,local,Direction.EAST)&&h.getLevel().getBlockState(h.absolutePos(local.east())).is(Blocks.CRAFTING_TABLE)&&tables.getCount()==3,"stone permits a workbench on side");
        p.getInventory().items.set(35,new ItemStack(Items.OAK_SAPLING,4));h.setBlock(local,Blocks.DIRT);
        h.assertTrue(!use(h,p,local,Direction.NORTH),"sapling behavior only tries upper face");
        for(var type:new GTToolType[]{GTToolType.AXE,GTToolType.DOUBLE_AXE,GTToolType.SAW}) {
            h.setBlock(local.above(),Blocks.AIR);p.setItemInHand(InteractionHand.MAIN_HAND,GTToolItem.create(type,Materials.Steel,Materials.Steel));
            var before=p.getMainHandItem().save(new net.minecraft.nbt.CompoundTag());
            h.assertTrue(use(h,p,local,Direction.UP),"manual woodworking tool shares planting: "+type);
            h.assertTrue(before.equals(p.getMainHandItem().save(new net.minecraft.nbt.CompoundTag())),"manual tool not damaged by planting");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void placementCancellationAndEmptyToolLeaveInventoryUntouched(GameTestHelper h) {
        var p=player(h);var local=new BlockPos(2,2,2);h.setBlock(local,Blocks.DIRT);
        var saplings=new ItemStack(Items.OAK_SAPLING,3);p.getInventory().items.set(35,saplings);
        var pos=h.absolutePos(local.above());
        java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent> deny=e->{if(e.getEntity()==p&&e.getPos().equals(pos))e.setCanceled(true);};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST,false,net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent.class,deny);
        try {h.assertTrue(!use(h,p,local,Direction.UP),"cancelled placement not consumed");}
        finally {net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(deny);}
        h.assertTrue(h.getLevel().getBlockState(pos).isAir()&&saplings.getCount()==3,"Forge rollback preserves block and source stack");
        p.getMainHandItem().getOrCreateTag().putLong("gt.charge",0);
        h.assertTrue(!use(h,p,local,Direction.UP)&&saplings.getCount()==3,"uncharged survival tool cannot place");
        p.getMainHandItem().getOrCreateTag().putLong("gt.charge",500);p.getAbilities().mayBuild=false;
        h.assertTrue(!use(h,p,local,Direction.UP)&&saplings.getCount()==3,"build permission respected");h.succeed();
    }
}
