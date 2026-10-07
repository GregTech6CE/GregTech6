package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.tool.ElectricToolWear;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech_repair") @PrefixGameTestTemplate(false)
public final class ElectricToolWearTests {
    @GameTest(template="test_empty")
    public static void materialWearLotteryAndPersistenceAreIndependentOfCharge(GameTestHelper h) {
        var tool=GTElectricItems.ELECTRIC_WRENCH.get();var stack=tool.assembled(Materials.Steel,100000);
        stack.getOrCreateTag().putLong("gt.charge",1234);
        h.assertTrue(ElectricToolWear.maximum(tool,stack)==Materials.Steel.getToolDurability()*100,
                "original material durability times 100, multiplier one");
        int[] bound={0};
        ElectricToolWear.apply(tool,stack,100,b->{bound[0]=b;return 1;});
        h.assertTrue(bound[0]==Math.max(10,Materials.Steel.getToolQuality()*20)&&ElectricToolWear.damage(stack)==0,
                "quality controls electric wear lottery; losing roll adds no damage");
        ElectricToolWear.apply(tool,stack,100,b->0);
        h.assertTrue(ElectricToolWear.damage(stack)==100&&tool.getEnergyStored(stack,GregTechTags.Energy.EU)==1234,
                "material wear stores separate original units, not charge or vanilla damage");
        var copy=ItemStack.of(stack.save(new CompoundTag()));
        h.assertTrue(ElectricToolWear.damage(copy)==100&&tool.headMaterial(copy)==Materials.Steel,"wear and material survive NBT");
        ElectricToolWear.apply(tool,copy,Long.MAX_VALUE,b->0);
        h.assertTrue(ElectricToolWear.damage(copy)==Long.MAX_VALUE&&!tool.canInteract(copy),"overflow saturates and broken material disables interaction");
        h.assertTrue(ElectricToolWear.scrapRandomBound(GTElectricItems.ELECTRIC_DRILL.get())==3
                &&ElectricToolWear.scrapRandomBound(GTElectricItems.ELECTRIC_CHAINSAW.get())==9
                &&ElectricToolWear.scrapRandomBound(tool)==17
                &&ElectricToolWear.scrapRandomBound(GTElectricItems.ELECTRIC_SCREWDRIVER.get())==5,"GT6 tool-head material scrap bounds");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void realToolUseBreaksToMaterialScrapOnceAndCreativeDoesNotWear(GameTestHelper h) {
        var tool=GTElectricItems.ELECTRIC_WRENCH.get();var stack=tool.assembled(Materials.Steel,100000);
        var player=h.makeMockSurvivalPlayer();player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        stack.getOrCreateTag().putLong("gt.charge",500);
        stack.getOrCreateTagElement("GT.ToolStats").putLong("k",ElectricToolWear.maximum(tool,stack)-1);
        int bound=ElectricToolWear.chanceBound(tool,stack);long seed=0;
        while(RandomSource.create(seed).nextInt(bound)!=0)seed++;
        player.getRandom().setSeed(seed);player.getAbilities().instabuild=true;
        var before=stack.save(new CompoundTag());tool.consumeInteractionEnergy(stack,100,player);
        h.assertTrue(before.equals(stack.save(new CompoundTag())),"creative use changes neither wear nor charge");
        player.getAbilities().instabuild=false;player.getRandom().setSeed(seed);
        tool.consumeInteractionEnergy(stack,100,player);
        h.assertTrue(stack.isEmpty(),"real material lottery consumes broken tool");
        var scrap=GTItems.getStack(MaterialPrefix.scrapGt,Materials.Steel,1).getItem();
        int count=player.getInventory().countItem(scrap);
        h.assertTrue(count>=1&&count<=17,"broken wrench returns same-material scrap, not a free battery");
        tool.inventoryTick(stack,h.getLevel(),player,0,true);
        h.assertTrue(player.getInventory().countItem(scrap)==count,"next inventory tick cannot duplicate broken scraps");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void loadedBrokenToolCannotWorkAndIsRecoveredByInventoryTick(GameTestHelper h) {
        var tool=GTElectricItems.ELECTRIC_DRILL.get();var stack=tool.assembled(Materials.Titanium,20000);
        stack.getOrCreateTag().putLong("gt.charge",1000);
        stack.getOrCreateTagElement("GT.ToolStats").putLong("k",ElectricToolWear.maximum(tool,stack));
        var copy=ItemStack.of(stack.save(new CompoundTag()));
        h.assertTrue(!tool.hasEnergyForUse(copy)&&!tool.consumeEnergy(copy),"loaded broken drill cannot place another charge");
        var player=h.makeMockSurvivalPlayer();player.setItemInHand(InteractionHand.MAIN_HAND,copy);
        tool.inventoryTick(copy,h.getLevel(),player,0,true);
        int count=player.getInventory().countItem(GTItems.getStack(MaterialPrefix.scrapGt,Materials.Titanium,1).getItem());
        h.assertTrue(copy.isEmpty()&&count>=1&&count<=3,"loaded broken drill yields titanium rod scraps");
        h.succeed();
    }
}
