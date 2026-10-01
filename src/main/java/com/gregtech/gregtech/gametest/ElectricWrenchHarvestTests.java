package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.tool.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTElectricItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class ElectricWrenchHarvestTests {
    @GameTest(template="test_empty")
    public static void actualSurvivalDismantleChargesOnceCollectsMachineAndSpillsContentsOnce(GameTestHelper h) {
        var block=MachineRegistry.basicMachines().stream().map(net.minecraftforge.registries.RegistryObject::get)
                .filter(b->b.basicSpec().recipeMap()!=null&&b.basicSpec().recipeMap().mInputItemsCount>0).findFirst().orElseThrow();
        var local=new BlockPos(2,2,2);h.setBlock(local,block);var pos=h.absolutePos(local);var state=h.getLevel().getBlockState(pos);
        var machine=(BasicMachineBlockEntity)h.getBlockEntity(local);machine.inventory().setStackInSlot(0,new ItemStack(Items.DIAMOND,3));
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"GTWrenchTest"));
        player.setGameMode(GameType.SURVIVAL);player.setOnGround(true);
        var tool=GTElectricItems.ELECTRIC_WRENCH.get();var stack=tool.assembled(Materials.Titanium,100000);
        stack.getOrCreateTag().putLong("gt.charge",10000);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        h.assertTrue(!stack.isDamageableItem()&&com.gregtech.gregtech.api.tool.GTToolHelper.canCollectMachineDrop(stack,state),
                "custom material wear must not become a zero-durability vanilla tool that blocks collection");
        h.assertTrue(state.canHarvestBlock(h.getLevel(),pos,player),"actual survival player can harvest with charged electric wrench");
        float hardness=state.getDestroySpeed(h.getLevel(),pos),speed=2*Materials.Titanium.getToolSpeed();
        h.assertTrue(Math.abs(state.getDestroyProgress(player,h.getLevel(),pos)-speed/hardness/30)<.0001,
                "machine override uses material speed through vanilla/Forge progress");
        h.assertTrue(player.gameMode.destroyBlock(pos),"actual server survival destruction succeeds");
        h.assertTrue(tool.getEnergyStored(stack,GregTechTags.Energy.EU)==10000-(long)Math.ceil(50D*hardness),"one hardness-scaled energy debit");
        h.assertTrue(player.getInventory().countItem(block.asItem())==1,"one machine collected into inventory");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(.9));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(block.asItem())).mapToInt(e->e.getItem().getCount()).sum()==0,"no duplicate machine on ground");
        h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum()==3,"stored contents spill exactly once");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void materialQualityOverridesFixedIronTierAndEmptyToolsCannotHarvest(GameTestHelper h) {
        var material=GTMaterialRegistry.allMaterials().stream().filter(ElectricToolAssembly::validMaterial)
                .max(java.util.Comparator.comparingInt(com.gregtech.gregtech.api.material.GTMaterial::getToolQuality)).orElseThrow();
        var block=ForgeRegistries.BLOCKS.getValues().stream().filter(b->BlockHarvestPolicy.level(b)>3
                &&BlockHarvestPolicy.level(b)<=material.getToolQuality()&&ElectricWrenchHarvest.target(b.defaultBlockState())).findFirst().orElseThrow();
        var player=h.makeMockSurvivalPlayer();var pos=h.absolutePos(new BlockPos(1,1,1));
        var tool=GTElectricItems.ELECTRIC_WRENCH.get();var weak=tool.assembled(Materials.Steel,10000);weak.getOrCreateTag().putLong("gt.charge",1000);
        player.setItemInHand(InteractionHand.MAIN_HAND,weak);
        h.assertTrue(!block.defaultBlockState().canHarvestBlock(h.getLevel(),pos,player),"weak head cannot bypass high material tier");
        var strong=tool.assembled(material,10000);strong.getOrCreateTag().putLong("gt.charge",1000);player.setItemInHand(InteractionHand.MAIN_HAND,strong);
        h.assertTrue(block.defaultBlockState().canHarvestBlock(h.getLevel(),pos,player),"Forge high-tier check reads actual head, not Tiers.IRON");
        strong.getOrCreateTag().putLong("gt.charge",0);
        h.assertTrue(!block.defaultBlockState().canHarvestBlock(h.getLevel(),pos,player)&&tool.getDestroySpeed(strong,block.defaultBlockState())==0,"empty electric wrench cannot harvest or accelerate machine");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void originalVanillaTargetsAndEnergyNodeTagsExcludeUnrelatedBlocks(GameTestHelper h) {
        var tool=GTElectricItems.ELECTRIC_WRENCH.get();var stack=tool.assembled(Materials.Steel,10000);stack.getOrCreateTag().putLong("gt.charge",1000);
        for(var block:new Block[]{Blocks.HOPPER,Blocks.DISPENSER,Blocks.DROPPER,Blocks.PISTON,Blocks.STICKY_PISTON,Blocks.REDSTONE_LAMP})
            h.assertTrue(tool.isCorrectToolForDrops(stack,block.defaultBlockState()),"original wrench target: "+block);
        for(var block:new Block[]{Blocks.STONE,Blocks.OAK_LOG,Blocks.DIAMOND_ORE,Blocks.BEDROCK})
            h.assertTrue(!tool.isCorrectToolForDrops(stack,block.defaultBlockState()),"not an invented universal mining tool: "+block);
        var transformer=ForgeRegistries.BLOCKS.getValue(GregTech.id("transformer_lv_mv"));
        h.assertTrue(BlockHarvestPolicy.tool(transformer)==BlockHarvestPolicy.Tool.WRENCH&&tool.isCorrectToolForDrops(stack,transformer.defaultBlockState()),"energy node now uses its wrench tag");
        h.succeed();
    }
}
