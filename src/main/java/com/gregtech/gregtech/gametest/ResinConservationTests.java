package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.block.wood.*;
import com.gregtech.gregtech.block.tool.SapBagBlock;
import com.gregtech.gregtech.blockentity.SapBagBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class ResinConservationTests {
    private static BlockHitResult hit(BlockPos pos, Direction face) { return new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false); }
    @GameTest(template="test_empty")
    public static void rubberResinCannotReplayWithFullInventory(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(1,1,1)); var hole=GTTreeHoles.hole(WoodSpecies.RUBBER);
        var resin=hole.resinItem().getItem(); var area=new AABB(pos).inflate(2);
        for(boolean full:new boolean[]{false,true}) {
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).forEach(net.minecraft.world.entity.item.ItemEntity::discard);
            level.setBlock(pos,hole.defaultBlockState().setValue(TreeHoleBlock.RESIN,true),3);
            var player=h.makeMockPlayer(); player.getAbilities().instabuild=false; player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
            if(full) for(int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.APPLE,64));
            for(int click=0;click<2;click++) hole.use(level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit(pos,Direction.NORTH));
            int world=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).stream().filter(e->e.getItem().is(resin)).mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(world+player.getInventory().countItem(resin)==1,"two clicks produce exactly one resin, full="+full);
            h.assertTrue(!level.getBlockState(pos).getValue(TreeHoleBlock.RESIN),"successful world drop still consumes tree resin");
        }
        var player=h.makeMockPlayer(); level.setBlock(pos,hole.defaultBlockState().setValue(TreeHoleBlock.RESIN,true),3);
        hole.use(level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit(pos,Direction.SOUTH));
        h.assertTrue(level.getBlockState(pos).getValue(TreeHoleBlock.RESIN),"back face cannot harvest");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE));
        hole.use(level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit(pos,Direction.NORTH));
        h.assertTrue(level.getBlockState(pos).getValue(TreeHoleBlock.RESIN),"held block is allowed to place without harvesting");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void sapTransfersOnlyOneContainerFromHeldStack(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(1,1,1)); var hole=GTTreeHoles.hole(WoodSpecies.MAPLE);
        FluidStack sap=hole.resinFluid(); var empty=stackableContainer(sap);
        h.assertTrue(!empty.isEmpty(),"a real registered stackable sap container exists");
        for(boolean full:new boolean[]{false,true}) {
            var area=new AABB(pos).inflate(2); level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).forEach(net.minecraft.world.entity.item.ItemEntity::discard);
            level.setBlock(pos,hole.defaultBlockState().setValue(TreeHoleBlock.RESIN,true),3);
            var player=h.makeMockPlayer(); player.getAbilities().instabuild=false; player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
            if(full) for(int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.APPLE,64));
            var stack=empty.copyWithCount(3); stack.getOrCreateTag().putString("repairMarker","kept"); player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            hole.use(level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit(pos,Direction.NORTH));
            var remaining=player.getMainHandItem();
            h.assertTrue(remaining.getCount()==2 && remaining.getTag().getString("repairMarker").equals("kept"),"two unchanged empty containers remain in hand");
            int fluid=0; for(int i=0;i<36;i++) fluid+=amount(player.getInventory().getItem(i));
            for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area)) fluid+=amount(entity.getItem());
            h.assertTrue(fluid==250 && !level.getBlockState(pos).getValue(TreeHoleBlock.RESIN),"exactly one 250 mB tap delivered, including full inventory");
        }
        var bagBlock=(SapBagBlock)GTToolBlocks.SAP_BAG.get(); level.setBlock(pos,bagBlock.defaultBlockState(),3);
        var bag=(SapBagBlockEntity)level.getBlockEntity(pos); var supply=sap.copy(); supply.setAmount(1000); bag.fill(supply,FluidAction.EXECUTE);
        var player=h.makeMockPlayer(); player.setItemInHand(InteractionHand.MAIN_HAND,empty.copyWithCount(3));
        bagBlock.use(bag.getBlockState(),level,pos,player,InteractionHand.MAIN_HAND,hit(pos,Direction.NORTH));
        int fluid=0; for(int i=0;i<36;i++) fluid+=amount(player.getInventory().getItem(i));
        h.assertTrue(player.getMainHandItem().getCount()==2 && fluid>0 && fluid+bag.tank().getAmount()==1000,"bag hand transfer conserves fluid and stacked containers");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void sapBagPersistsAutomationAndBoundsItsResinSlot(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(1,1,1)); var block=(SapBagBlock)GTToolBlocks.SAP_BAG.get();
        level.setBlock(pos,block.defaultBlockState(),3); var bag=(SapBagBlockEntity)level.getBlockEntity(pos);
        var cap=bag.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElseThrow();
        var fluid=GTTreeHoles.hole(WoodSpecies.MAPLE).resinFluid(); var chunk=level.getChunkAt(pos);
        chunk.setUnsaved(false); cap.fill(fluid,FluidAction.SIMULATE); h.assertTrue(!chunk.isUnsaved(),"simulation does not dirty chunk");
        cap.fill(fluid,FluidAction.EXECUTE); h.assertTrue(chunk.isUnsaved(),"capability fill marks chunk for persistence");
        chunk.setUnsaved(false); cap.drain(100,FluidAction.EXECUTE); h.assertTrue(chunk.isUnsaved(),"capability drain marks chunk for persistence");
        bag.load(bag.saveWithoutMetadata()); h.assertTrue(bag.tank().getAmount()==150,"automated amount survives save/load");
        var old=bag.getCapability(ForgeCapabilities.FLUID_HANDLER); bag.invalidateCaps(); h.assertTrue(!old.isPresent(),"old handle invalidated"); bag.reviveCaps();
        h.assertTrue(bag.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent(),"capability restored after revive");
        var hole=GTTreeHoles.hole(WoodSpecies.RUBBER); var neighbor=pos.north();
        bag.tank().setEmpty(); bag.setStored(hole.resinItem().copyWithCount(64));
        level.setBlock(neighbor,hole.defaultBlockState().setValue(TreeHoleBlock.FACING,Direction.SOUTH).setValue(TreeHoleBlock.RESIN,true),3);
        bag.collect(); h.assertTrue(bag.stored().getCount()==64 && !level.getBlockState(neighbor).getValue(TreeHoleBlock.RESIN),"GT6 full bag consumes tap but does not exceed slot capacity");
        bag.load(bag.saveWithoutMetadata()); h.assertTrue(bag.stored().getCount()==64,"full slot survives byte Count save");
        level.setBlock(neighbor,hole.defaultBlockState().setValue(TreeHoleBlock.RESIN,true),3); bag.collect();
        h.assertTrue(level.getBlockState(neighbor).getValue(TreeHoleBlock.RESIN),"bag cannot collect from back of a hole");
        var area=new AABB(pos).inflate(1); level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).forEach(net.minecraft.world.entity.item.ItemEntity::discard);
        bag.fill(fluid,FluidAction.EXECUTE); level.destroyBlock(pos,true);
        int resin=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).stream().filter(e->e.getItem().is(hole.resinItem().getItem())).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(resin==64 && bag.stored().isEmpty() && bag.tank().isEmpty(),"non-player removal drops resin once and trashes fluid as GT6 does");
        h.succeed();
    }
    private static int amount(ItemStack stack) { return FluidUtil.getFluidContained(stack).map(FluidStack::getAmount).orElse(0)*stack.getCount(); }
    @GameTest(template="test_empty")
    public static void treeScanDoesNotLoadUnloadedCanopyChunks(GameTestHelper h) {
        var pos=new BlockPos(20000000,100,20000000);
        h.assertTrue(!h.getLevel().hasChunkAt(pos),"test starts in an unloaded region");
        for(var species:new WoodSpecies[]{WoodSpecies.RUBBER,WoodSpecies.MAPLE,WoodSpecies.RAINBOWOOD}) {
            h.assertTrue(com.gregtech.gregtech.blockentity.TreeHoleBlockEntity.countTreeLeaves(h.getLevel(),pos,species)==0,"unloaded tree cannot refill");
            for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) h.assertTrue(!h.getLevel().hasChunkAt(pos.offset(dx*16,0,dz*16)),"scan does not load neighboring chunks");
        }
        h.succeed();
    }
    private static ItemStack stackableContainer(FluidStack fluid) {
        for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS) {
            var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
            if(id==null || !id.getNamespace().equals("gregtech") || !id.getPath().startsWith("fluid_")) continue;
            var stack=new ItemStack(item); if(stack.getMaxStackSize()<3) continue;
            if(com.gregtech.gregtech.api.fluid.HandContainerTransfer.prepare(stack,fluid,true)!=null) return stack;
        }
        return ItemStack.EMPTY;
    }
}
