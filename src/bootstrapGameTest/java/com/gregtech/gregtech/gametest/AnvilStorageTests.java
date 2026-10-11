package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class AnvilStorageTests {
    @GameTest(template="test_empty")
    public static void automatedInputsAndVisualIdentity(GameTestHelper h) {
        var pos=new BlockPos(1,1,1); h.setBlock(pos,GTToolBlocks.ANVILS.get(0).get());
        var anvil=(MaterialAnvilBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        var io=anvil.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(()->new AssertionError("missing input"));
        var recipe=MachineRecipeMaps.Anvil.mRecipeList.stream().filter(r->r.mEnabled&&!r.mFakeRecipe&&r.mInputs.length>0).findFirst().orElseThrow();
        var stack=recipe.mInputs[0].copyWithCount(2);
        h.assertTrue(io.insertItem(0,stack,true).isEmpty()&&anvil.workpiece(0).isEmpty(),"simulation cannot alter workpieces");
        h.assertTrue(io.insertItem(0,stack,false).isEmpty()&&anvil.workpiece(0).getCount()==2,"automation inserts recipe inputs");
        var exposed=io.getStackInSlot(0);exposed.setCount(0);
        h.assertTrue(anvil.workpiece(0).getCount()==2&&io.extractItem(0,64,false).isEmpty(),"no external mutation or extraction");
        var hammer=GTToolHelper.write(GTToolItems.empty(GTToolType.HARD_HAMMER),com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Steel"),com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        hammer.setDamageValue(hammer.getMaxDamage());
        var data=anvil.saveWithoutMetadata();data.put("gt.work0",hammer.save(new net.minecraft.nbt.CompoundTag()));anvil.load(data);
        h.assertTrue(io.insertItem(1,stack,false).getCount()==2,"stored hammer blocks both automated work slots");
        var clientCopy=new MaterialAnvilBlockEntity(h.absolutePos(pos),anvil.getBlockState());clientCopy.handleUpdateTag(anvil.getUpdateTag());
        for(var face:Direction.Plane.HORIZONTAL)for(int slot=0;slot<2;slot++) {
            var parts=com.gregtech.gregtech.content.tool.AnvilHammerDisplay.parts(clientCopy.workpiece(0),slot,face);
            h.assertTrue(parts.size()==2&&parts.get(0).bounds().intersects(parts.get(1).bounds()),"synced worn hammer remains two joined solid parts");
            h.assertTrue(parts.get(0).material()==GTToolHelper.getHead(hammer)&&parts.get(1).material()==GTToolHelper.getHandle(hammer),"head and shaft retain separate materials");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void originalStoredHammerGeometry(GameTestHelper h) {
        for(var facing:Direction.Plane.HORIZONTAL) for(int slot=0;slot<2;slot++) {
            var head=com.gregtech.gregtech.content.tool.AnvilWorkpieceGeometry.bounds(8,slot,facing);
            var handle=com.gregtech.gregtech.content.tool.AnvilWorkpieceGeometry.bounds(9,1-slot,facing);
            h.assertTrue(head.minY==.75 && head.maxY==1 && handle.minY==13/16d && handle.maxY==15/16d,
                    "original head and shaft heights");
            h.assertTrue(head.intersects(handle),"shaft joins head in either slot and every facing");
            double length=facing.getAxis()==Direction.Axis.Z?handle.getXsize():handle.getZsize();
            h.assertTrue(length==14/16d,"original 14 pixel shaft spans both workpiece positions");
        }
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void sideSplitAndStoredHammer(GameTestHelper h) {
        var pos = new BlockPos(1,1,1); h.setBlock(pos,GTToolBlocks.ANVILS.get(0).get());
        var absolute = h.absolutePos(pos);
        var anvil = (MaterialAnvilBlockEntity)h.getLevel().getBlockEntity(absolute);
        var player = h.makeMockSurvivalPlayer();
        var input = MachineRecipeMaps.Anvil.mRecipeList.stream().filter(r -> r.mEnabled && !r.mFakeRecipe && r.mInputs.length>0)
                .findFirst().orElseThrow().mInputs[0].copyWithCount(5);
        input.getOrCreateTag().putString("split_test","retained");
        var top = new BlockHitResult(Vec3.atLowerCornerOf(absolute).add(.25,.75,.25),Direction.UP,absolute,false);
        var side = new BlockHitResult(Vec3.atLowerCornerOf(absolute).add(.25,.6,0),Direction.NORTH,absolute,false);
        player.setItemInHand(InteractionHand.MAIN_HAND,input.copy());
        anvil.interact(player,InteractionHand.MAIN_HAND,top);
        h.assertTrue(anvil.workpiece(0).getCount()==5 && player.getMainHandItem().isEmpty(),"top deposits workpiece");
        anvil.interact(player,InteractionHand.MAIN_HAND,side);
        h.assertTrue(anvil.workpiece(0).getCount()==2 && anvil.workpiece(1).getCount()==2,"side evenly splits five into two pairs");
        h.assertTrue(anvil.workpiece(1).getTag().getString("split_test").equals("retained"),"split preserves tags");
        h.assertTrue(player.getInventory().countItem(input.getItem())==1,"odd remainder returned exactly once");
        anvil.interact(player,InteractionHand.MAIN_HAND,side);
        h.assertTrue(anvil.workpiece(0).getCount()==2 && anvil.workpiece(1).getCount()==2,"two occupied slots are not redistributed");
        var cleared = anvil.saveWithoutMetadata(); cleared.remove("gt.work0"); cleared.remove("gt.work1"); anvil.load(cleared);
        var hammer = GTToolHelper.write(GTToolItems.empty(GTToolType.HARD_HAMMER),
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Steel"),
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        player.getInventory().clearContent(); player.setItemInHand(InteractionHand.MAIN_HAND,hammer.copy());
        anvil.interact(player,InteractionHand.MAIN_HAND,top);
        h.assertTrue(GTToolHelper.matchesTool(anvil.workpiece(0),GTToolType.HARD_HAMMER),"empty anvil stores hammer");
        anvil.load(anvil.saveWithoutMetadata());
        h.assertTrue(ItemStack.isSameItemSameTags(hammer,anvil.workpiece(0)),"stored hammer survives NBT");
        anvil.interact(player,InteractionHand.MAIN_HAND,top);
        h.assertTrue(anvil.workpiece(0).isEmpty() && player.getInventory().countItem(hammer.getItem())==1,"top returns stored hammer once");
        h.succeed();
    }
}
