package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.block.inventory.SafeBlock;
import com.gregtech.gregtech.blockentity.inventory.SafeBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class SafeRotationTests {
    @GameTest(template="test_empty")
    public static void sharedWrenchPermissionsAndSixFacesAgree(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(1,1,1));
        var block=GTStorage.SAFE.get(); level.setBlock(pos,block.defaultBlockState(),3);
        var safe=(SafeBlockEntity)level.getBlockEntity(pos); var player=h.makeMockPlayer();
        var tool=GTToolItem.create(GTToolType.WRENCH,Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var spec=ToolInteractions.describe(safe.getBlockState(),tool);
        h.assertTrue(spec!=null && spec.facing().getPossibleValues().size()==6,"safe declares a six-face wrench operation");
        for(var face:Direction.values()) h.assertTrue(ToolInteractions.canApply(spec,safe.getBlockState(),level,pos,player,tool,face),"unclaimed safe preview permits each face");
        h.assertTrue(!safe.saveWithoutMetadata().hasUUID("gt.owner"),"overlay feasibility never claims ownership");
        for(var face:Direction.values()) {
            var hit=new BlockHitResult(Vec3.atCenterOf(pos).add(face.getStepX()*.5,face.getStepY()*.5,face.getStepZ()*.5),face,pos,false);
            block.use(safe.getBlockState(),level,pos,player,InteractionHand.MAIN_HAND,hit);
            h.assertTrue(level.getBlockState(pos).getValue(SafeBlock.FACING)==face,"real wrench selects "+face);
            h.assertTrue(level.getBlockEntity(pos)==safe,"rotation retains inventory entity");
        }
        h.assertTrue(safe.saveWithoutMetadata().getUUID("gt.owner").equals(player.getUUID()),"first actual wrench use claims mechanical safe");
        safe.setOwner(java.util.UUID.randomUUID());
        var before=safe.getBlockState(); int damage=tool.getDamageValue();
        h.assertTrue(!ToolInteractions.canApply(spec,before,level,pos,player,tool,Direction.UP),"overlay denies nonowner");
        ToolInteractions.use(before,level,pos,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false));
        h.assertTrue(level.getBlockState(pos)==before && tool.getDamageValue()==damage,"shared direct use cannot bypass owner or damage denied tool");
        // Existing unrestricted targets keep their previous behavior with the default permission hooks.
        var chest=GTStorage.chest(Materials.Steel); level.setBlock(pos,chest.defaultBlockState(),3);
        ToolInteractions.use(level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(.5,0,0),Direction.EAST,pos,false));
        h.assertTrue(level.getBlockState(pos).getValue(com.gregtech.gregtech.block.inventory.MetalChestBlock.FACING)==Direction.EAST,"ordinary chest still rotates through shared tool path");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void placementPitchAndLockedSafeRotationMatchGT6(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(1,1,1));
        var block=GTStorage.safe(Materials.Steel,true); var player=h.makeMockPlayer();
        player.setYRot(0);
        float[] angles={64.99F,65,90,-64.99F,-65,-90};
        Direction[] expected={Direction.NORTH,Direction.UP,Direction.UP,Direction.NORTH,Direction.DOWN,Direction.DOWN};
        for(int i=0;i<angles.length;i++) {
            player.setXRot(angles[i]);
            var context=new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,new ItemStack(block),
                    new BlockHitResult(Vec3.atLowerCornerOf(pos).add(.5,0,.5),Direction.UP,pos.below(),false));
            h.assertTrue(block.getStateForPlacement(context).getValue(SafeBlock.FACING)==expected[i],"GT6 pitch threshold: "+angles[i]);
        }
        level.setBlock(pos,block.defaultBlockState(),3);
        var safe=(SafeBlockEntity)level.getBlockEntity(pos); safe.setKeyId(771);
        var tool=GTToolItem.create(GTToolType.WRENCH,Materials.Steel,com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
        player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false);
        block.use(safe.getBlockState(),level,pos,player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(safe.getBlockState().getValue(SafeBlock.FACING)==Direction.NORTH,"locked key safe cannot rotate");
        safe.useKey(GTDungeonKeys.stack(0,771,0));
        block.use(safe.getBlockState(),level,pos,player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(safe.getBlockState().getValue(SafeBlock.FACING)==Direction.UP && safe.opened(),"unlocked key safe rotates without resetting lock");
        h.succeed();
    }
}
