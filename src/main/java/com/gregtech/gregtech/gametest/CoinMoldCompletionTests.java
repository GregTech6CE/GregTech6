package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.blockentity.tool.CoinMoldBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.tool.CoinMoldGeometry;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class CoinMoldCompletionTests {
    @GameTest(template="test_empty")
    public static void manuallyRetrieveUnstruckPlateButNotThroughAutomation(GameTestHelper h) {
        var block=ForgeRegistries.BLOCKS.getValue(GregTech.id("coin_mold"));var local=new BlockPos(2,2,2);h.setBlock(local,block);
        var mold=(CoinMoldBlockEntity)h.getBlockEntity(local);var pos=mold.getBlockPos();var player=h.makeMockSurvivalPlayer();
        var plate=GTItems.getStack(MaterialPrefix.plateTiny,Materials.Copper,1);
        h.assertTrue(mold.insertPlate(plate,false),"plate enters mold");
        h.assertTrue(mold.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new).extractItem(0,1,false).isEmpty(),"automation retains unfinished plate");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        block.use(mold.getBlockState(),h.getLevel(),pos,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.DOWN,pos,false));
        h.assertTrue(!mold.contents().isEmpty(),"bottom cannot retrieve");
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false);
        player.getAbilities().mayBuild=false;block.use(mold.getBlockState(),h.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(!mold.contents().isEmpty(),"permission denial cannot retrieve");player.getAbilities().mayBuild=true;
        for(int i=0;i<player.getInventory().items.size();i++)player.getInventory().items.set(i,new ItemStack(Items.STONE,64));
        block.use(mold.getBlockState(),h.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(!mold.contents().isEmpty(),"full inventory leaves plate in mold");
        player.getInventory().items.set(1,ItemStack.EMPTY);
        block.use(mold.getBlockState(),h.getLevel(),pos,player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(mold.contents().isEmpty()&&player.getInventory().items.get(1).is(GTItems.getStack(MaterialPrefix.plateTiny,Materials.Copper,1).getItem()),"side click retrieves unstruck plate exactly once");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void originalSelectionContentGeometryAndMaterialSync(GameTestHelper h) throws Exception {
        var block=ForgeRegistries.BLOCKS.getValue(GregTech.id("coin_mold"));var local=new BlockPos(2,2,2);h.setBlock(local,block);
        var mold=(CoinMoldBlockEntity)h.getBlockEntity(local);var pos=mold.getBlockPos();var state=mold.getBlockState();
        h.assertTrue(state.getShape(h.getLevel(),pos).bounds().equals(new AABB(0,0,0,1,.75,1))
                &&state.getCollisionShape(h.getLevel(),pos).bounds().equals(new AABB(0,0,0,1,.75,1)),"GT6 selection and collision ignore upper die lip");
        h.assertTrue(CoinMoldGeometry.CONTENT.equals(new AABB(5/16D,11/16D,5/16D,11/16D,13/16D,11/16D)),"original content render pass bounds");
        h.assertTrue(mold.displayedMaterial()==null,"empty mold has no metal insert");
        mold.insertPlate(GTItems.getStack(MaterialPrefix.plateTiny,Materials.Copper,1),false);
        var mirror=new CoinMoldBlockEntity(pos,state);mirror.load(mold.getUpdateTag());
        h.assertTrue(mirror.displayedMaterial()==Materials.Copper,"plate material reaches client tag");
        h.assertTrue(mold.strike(),"plate minted");mirror.load(mold.getUpdateTag());
        h.assertTrue(mirror.displayedMaterial()==Materials.Copper,"coin retains displayed material");
        mold.takeContents();mirror.load(mold.getUpdateTag());h.assertTrue(mirror.displayedMaterial()==null,"retrieval clears client insert");
        try(var stream=CoinMoldCompletionTests.class.getClassLoader().getResourceAsStream("assets/gregtech/models/block/tool/coin_mold.json")) {
            var model=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(String layer:new String[]{"colored","overlay"}) {
                var parts=model.getAsJsonObject("children").getAsJsonObject(layer).getAsJsonArray("elements");
                h.assertTrue(parts.size()==6,"mold has base, lip and four inward wall passes: "+layer);
                for(int i=2;i<6;i++)h.assertTrue(parts.get(i).getAsJsonObject().getAsJsonObject("faces").size()==1,"inner wall emits only original inward face");
            }
        }
        h.succeed();
    }
}
