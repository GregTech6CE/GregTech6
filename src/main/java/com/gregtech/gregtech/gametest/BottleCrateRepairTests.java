package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.inventory.BottleCrateBlock;
import com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity;
import com.gregtech.gregtech.content.tool.BottleCrateGeometry;
import com.gregtech.gregtech.registry.GTStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class BottleCrateRepairTests {
    private static BottleCrateBlockEntity place(GameTestHelper h,BlockPos pos) {
        h.getLevel().setBlock(pos,GTStorage.BOTTLE_CRATE.get().defaultBlockState(),3);
        return (BottleCrateBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void click(BottleCrateBlockEntity crate,Player player,int slot) {
        var pos=crate.getBlockPos();
        var hit=new BlockHitResult(Vec3.atLowerCornerOf(pos).add((3+5*(slot%3))/16.,6/16.,(3+5*(slot/3))/16.),Direction.UP,pos,false);
        crate.getBlockState().getBlock().use(crate.getBlockState(),crate.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit);
    }

    @GameTest(template="test_empty")
    public static void allNineCellsTakeAndReturnWholeStacksWithoutRotation(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,1,1)); var crate=place(h,pos); var player=h.makeMockSurvivalPlayer();
        for(var face:Direction.Plane.HORIZONTAL) {
            h.getLevel().setBlock(pos,crate.getBlockState().setValue(BottleCrateBlock.FACING,face),3);
            for(int slot=0;slot<9;slot++) {
                var stack=new ItemStack(Items.GLASS_BOTTLE,slot+2); stack.getOrCreateTag().putInt("cell",slot);
                player.setItemInHand(InteractionHand.MAIN_HAND,stack); click(crate,player,slot);
                h.assertTrue(player.getMainHandItem().isEmpty() && crate.items().getStackInSlot(slot).getCount()==slot+2,"whole stack goes into selected cell "+slot+" / "+face);
            }
            for(int slot=0;slot<9;slot++) {
                player.getInventory().clearContent(); click(crate,player,slot);
                h.assertTrue(player.getMainHandItem().getCount()==slot+2 && player.getMainHandItem().getTag().getInt("cell")==slot,"whole selected stack returns to hand");
                h.assertTrue(crate.items().getStackInSlot(slot).isEmpty(),"selected cell emptied");
            }
        }
        for(var item:new net.minecraft.world.item.Item[]{Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION,Items.EXPERIENCE_BOTTLE,com.gregtech.gregtech.item.BottleItem.emptyBottle().getItem()})
            h.assertTrue(BottleCrateBlock.isBottle(new ItemStack(item)),"accept bottle family "+item);
        h.assertTrue(!BottleCrateBlock.isBottle(new ItemStack(Items.WATER_BUCKET)),"buckets are not bottles");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void fullInventoryLeavesWholeStackAndSyncPreservesSlot(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,1,1)); var crate=place(h,pos); var player=h.makeMockSurvivalPlayer();
        crate.items().setStackInSlot(8,new ItemStack(Items.GLASS_BOTTLE,12));
        for(int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        player.getInventory().setItem(5,new ItemStack(Items.GLASS_BOTTLE,60));
        click(crate,player,8);
        h.assertTrue(crate.items().getStackInSlot(8).getCount()==12 && player.getInventory().getItem(5).getCount()==60,"no partial merge and no overflow drop when full");
        var client=new BottleCrateBlockEntity(pos,crate.getBlockState()); client.load(crate.getUpdateTag());
        h.assertTrue(client.items().getStackInSlot(8).getCount()==12 && client.items().getStackInSlot(0).isEmpty(),"display synchronization preserves sparse cell positions");
        h.assertTrue(crate.getUpdatePacket()!=null,"inventory has a client update packet");
        player.getInventory().setItem(6,ItemStack.EMPTY); click(crate,player,8);
        h.assertTrue(player.getInventory().getItem(6).getCount()==12 && crate.items().getStackInSlot(8).isEmpty(),"one free slot receives entire stack");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).isEmpty(),"retrieval never spills bottles");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void packedCrateSurvivesBothLootOrdersAndCreativeBreak(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(1,1,1));
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        for(int mode=0;mode<3;mode++) {
            level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).forEach(ItemEntity::discard);
            var crate=place(h,pos); var state=crate.getBlockState();
            var stack=new ItemStack(Items.GLASS_BOTTLE,37); stack.getOrCreateTag().putString("custom","saved");
            crate.items().setStackInSlot(7,stack);
            if(mode==0) level.destroyBlock(pos,true);
            else {
                var player=mode==1?h.makeMockSurvivalPlayer():h.makeMockPlayer();
                state.getBlock().playerWillDestroy(level,pos,state,player); level.removeBlock(pos,false);
                if(mode==1) state.getBlock().playerDestroy(level,player,pos,state,crate,ItemStack.EMPTY);
            }
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
            h.assertTrue(drops.size()==1 && drops.get(0).getItem().is(GTStorage.BOTTLE_CRATE.get().asItem()),"exactly one packed crate in drop order "+mode);
            h.assertTrue(crate.items().getStackInSlot(7).isEmpty(),"old resolved inventory is cleared");
            var packed=drops.get(0).getItem().copy(); drops.get(0).discard();
            var player=h.makeMockSurvivalPlayer(); player.setPos(pos.getX()+3,pos.getY()+1,pos.getZ()+3);
            var context=new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,packed,new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.5,0,.5),Direction.UP,pos.below(),false));
            h.assertTrue(((BlockItem)packed.getItem()).place(context).consumesAction(),"actual BlockItem placement restores packed crate");
            var restored=(BottleCrateBlockEntity)level.getBlockEntity(pos);
            h.assertTrue(restored!=null && ItemStack.matches(stack,restored.items().getStackInSlot(7)) && restored.items().getStackInSlot(0).isEmpty(),"count, custom NBT and sparse slot restored");
            level.removeBlock(pos,false);
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void selectionCollisionAndBottleBoundsMatchGT6(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,1,1)); var crate=place(h,pos); var state=crate.getBlockState();
        h.assertTrue(state.getShape(h.getLevel(),pos).bounds().maxY==6/16.,"selection height is six pixels");
        h.assertTrue(state.getCollisionShape(h.getLevel(),pos,CollisionContext.empty()).bounds().maxY==10/16.,"physical collision height is ten pixels");
        for(int slot=0;slot<9;slot++) {
            var fluid=BottleCrateGeometry.bounds(slot,0); var body=BottleCrateGeometry.bounds(slot,1); var cap=BottleCrateGeometry.bounds(slot,2);
            h.assertTrue(body.contains(fluid.getCenter()) && body.maxY==13/16. && cap.minY==13/16. && cap.maxY==1,"fluid inset, body and neck align");
        }
        double[] coordinates={0,5.5/16.-.00001,5.5/16.,10.5/16.-.00001,10.5/16.,1}; int[] columns={0,0,1,1,2,2};
        for(int x=0;x<6;x++) for(int z=0;z<6;z++) {
            var hit=new BlockHitResult(Vec3.atLowerCornerOf(pos).add(coordinates[x],.375,coordinates[z]),Direction.UP,pos,false);
            h.assertTrue(BottleCrateBlock.slotAt(pos,hit)==columns[x]+3*columns[z],"exact GT6 cell boundaries");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void wrenchRotatesFrameWithoutMovingInventoryAndWoodBurns(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,1,1)); var crate=place(h,pos); var player=h.makeMockSurvivalPlayer();
        crate.items().setStackInSlot(8,new ItemStack(Items.GLASS_BOTTLE,9));
        var tool=com.gregtech.gregtech.item.GTToolItem.create(com.gregtech.gregtech.api.tool.GTToolType.WRENCH,
                com.gregtech.gregtech.content.material.Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        for(var face:Direction.Plane.HORIZONTAL) {
            var hit=new BlockHitResult(Vec3.atCenterOf(pos).add(face.getStepX()*.5,0,face.getStepZ()*.5),face,pos,false);
            var state=crate.getBlockState(); state.getBlock().use(state,h.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit);
            h.assertTrue(crate.getBlockState().getValue(BottleCrateBlock.FACING)==face,"shared wrench rotates frame");
            h.assertTrue(crate.items().getStackInSlot(8).getCount()==9,"rotation does not retrieve or relocate bottles");
        }
        var state=crate.getBlockState();
        h.assertTrue(state.getBlock().getFlammability(state,h.getLevel(),pos,Direction.UP)==150
                && state.getBlock().getFireSpreadSpeed(state,h.getLevel(),pos,Direction.UP)==150,"GT6 wooden crate flammability");
        h.assertTrue(state.getDestroySpeed(h.getLevel(),pos)==.5f,"GT6 hardness");
        h.succeed();
    }

}
