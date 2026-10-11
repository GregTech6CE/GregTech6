package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.inventory.BottleCrateBlock;
import com.gregtech.gregtech.blockentity.inventory.BottleCrateBlockEntity;
import com.gregtech.gregtech.block.tool.FluidAttachmentBlock;
import com.gregtech.gregtech.item.BottleItem;
import com.gregtech.gregtech.worldgen.dungeon.*;
import com.gregtech.gregtech.worldgen.GTDungeonFeature;
import com.gregtech.gregtech.block.stone.StoneType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashSet;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class WorkshopSupplyRepairTests {
    private static GTDungeonData data(GameTestHelper h,long seed) {
        var p=h.absolutePos(BlockPos.ZERO);
        return new GTDungeonData(h.getLevel(),p.getX(),p.getY(),p.getZ(),StoneType.LIMESTONE,StoneType.SLATE,3,
                new byte[5][5],2,2,0,new long[GTDungeonFeature.KEY_COUNT],new ItemStack[GTDungeonFeature.KEY_COUNT],
                new boolean[GTDungeonFeature.KEY_COUNT],new HashSet<>(),new HashSet<>(),RandomSource.create(seed));
    }

    @GameTest(template="test_empty")
    public static void originalBottleRollsPopulateRealCratesAndPreserveRandomSequence(GameTestHelper h) {
        String[] chemicals={"mercury_bottle","mercury_bottle","mercury_bottle","glue_bottle","lubricant_bottle","ink_bottle","purple_drink","holy_water","bottled_indigo_dye"};
        String[] fluids={null,"Purple_Drink","Vodka","Mead","Whiskey_GlenMcKenner","Wine_Grape_Purple"};
        String[] drinks={null,"purple_drink","vodka","mead","glen_mckenner","ricardo_sanchez"};
        Direction[] facings={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
        int filled=0,empty=0,absent=0;
        var pos=h.absolutePos(new BlockPos(1,1,1));
        for(int kind=0;kind<fluids.length;kind++) for(long seed=0;seed<64;seed++) {
            h.getLevel().removeBlock(pos,false); var data=data(h,seed);
            if(kind==0) DungeonBottleSupplies.chemicals(data,1,1,1); else DungeonBottleSupplies.drinks(data,1,1,1,fluids[kind]);
            var crate=(BottleCrateBlockEntity)h.getLevel().getBlockEntity(pos); var random=RandomSource.create(seed);
            h.assertTrue(crate!=null && crate.getBlockState().getValue(BottleCrateBlock.FACING)==facings[random.nextInt(4)],"facing roll precedes contents");
            for(int slot=0;slot<9;slot++) {
                // Direct transcription of GT6's ternaries, independent from production helper calls.
                boolean full=kind>0 ? random.nextInt(3)==0 : slot<6 ? random.nextInt(4)<3 : random.nextBoolean();
                boolean blank=!full && random.nextBoolean();
                int count=(full||blank)?1+random.nextInt(kind==0?8:4):0;
                var actual=crate.items().getStackInSlot(slot);
                h.assertTrue(actual.getCount()==count,"original conditional count roll: kind="+kind+" slot="+slot+" seed="+seed);
                if(full) {
                    h.assertTrue(actual.is(ForgeRegistries.ITEMS.getValue(GregTech.id(kind==0?chemicals[slot]:drinks[kind]))),"correct bottle in its original slot"); filled++;
                } else if(blank) { h.assertTrue(actual.is(BottleItem.emptyBottle().getItem()),"GT empty bottle"); empty++; }
                else {h.assertTrue(actual.isEmpty(),"absent slot stays absent"); absent++;}
            }
            h.assertTrue(data.next(100000)==random.nextInt(100000),"subsequent room random stream has exact original call count/order");
            var saved=crate.saveWithoutMetadata(); crate.load(saved);
            h.assertTrue(saved.equals(crate.saveWithoutMetadata()),"preloaded inventory saves without rerolling");
        }
        h.assertTrue(filled>0 && empty>0 && absent>0,"seed sweep exercised all conditional branches");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void bottleCrateUsesOriginalToolsWoodBoltsAndReturnsGlueBottle(GameTestHelper h) {
        var level=h.getLevel(); var player=h.makeMockSurvivalPlayer();
        h.assertTrue(level.getRecipeManager().byKey(GregTech.id("bottle_crate")).isEmpty(),"old eight-plank placeholder removed");
        var recipe=(ShapedRecipe)level.getRecipeManager().byKey(GregTech.id("hand/storage/bottle_crate")).orElseThrow();
        var grid=new TransientCraftingContainer(new CraftingMenu(0,player.getInventory()),3,3);
        for(int slot=0;slot<9;slot++) {
            var candidates=recipe.getIngredients().get(slot).getItems();
            h.assertTrue(candidates.length>0,"required original ingredient resolves in slot "+slot);
            var stack=candidates[0].copy();
            if(stack.getItem() instanceof com.gregtech.gregtech.item.GTToolItem) stack=com.gregtech.gregtech.item.GTToolItem.create(
                    com.gregtech.gregtech.api.tool.GTToolHelper.getType(stack),com.gregtech.gregtech.content.material.Materials.Steel,
                    com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood"));
            grid.setItem(slot,stack);
        }
        for(String glue:new String[]{"glue_bottle","tar_bottle","green_slime_bottle","pink_slime_bottle","blue_slime_bottle"}) {
            grid.setItem(4,new ItemStack(ForgeRegistries.ITEMS.getValue(GregTech.id(glue))));
            h.assertTrue(recipe.matches(grid,level),"original glue alternative crafts: "+glue);
            h.assertTrue(recipe.assemble(grid,level.registryAccess()).is(com.gregtech.gregtech.registry.GTStorage.BOTTLE_CRATE.get().asItem()),"one treated crate output");
            var remaining=recipe.getRemainingItems(grid);
            for(int slot=0;slot<3;slot++) h.assertTrue(!remaining.get(slot).isEmpty() && remaining.get(slot).getDamageValue()>grid.getItem(slot).getDamageValue(),"tool is returned with wear");
            h.assertTrue(remaining.get(4).is(BottleItem.emptyBottle().getItem()) && remaining.get(4).getCount()==1,"glue portion returns exactly one empty bottle");
            for(int slot:new int[]{3,5,6,7,8}) h.assertTrue(remaining.get(slot).isEmpty(),"wood and bolts are consumed");
        }
        var bolt=grid.getItem(6); grid.setItem(6,new ItemStack(Items.IRON_NUGGET));
        h.assertTrue(!recipe.matches(grid,level),"metal nugget cannot replace wooden bolt"); grid.setItem(6,bolt);
        grid.setItem(4,new ItemStack(Items.GLASS_BOTTLE)); h.assertTrue(!recipe.matches(grid,level),"empty bottle is not glue");
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void workshopStainlessAttachmentsTransferToTheActualWaterDrum(GameTestHelper h) {
        var data=data(h,2); WorkshopFluidStation.place(data,2,2,2);
        var pos=h.absolutePos(new BlockPos(2,2,2)); var level=h.getLevel();
        var handler=level.getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(AssertionError::new);
        h.assertTrue(handler.getFluidInTank(0).getAmount()==64000,"original full stainless water drum");
        var player=h.makeMockSurvivalPlayer();
        var west=pos.west(); var north=pos.north(); var top=pos.above();
        h.assertTrue(level.getBlockState(west).getValue(FluidAttachmentBlock.FACING)==Direction.EAST
                && level.getBlockState(north).getValue(FluidAttachmentBlock.FACING)==Direction.SOUTH
                && level.getBlockState(top).getValue(FluidAttachmentBlock.FACING)==Direction.DOWN,"all three attachments point into drum");
        for(var tap:new BlockPos[]{west,north}) {
            player.getInventory().clearContent(); player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            var state=level.getBlockState(tap); state.getBlock().use(state,level,tap,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(tap),Direction.UP,tap,false));
            h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET),"real tap click fills a bucket");
        }
        h.assertTrue(handler.getFluidInTank(0).getAmount()==62000,"both taps drain the same tank");
        var state=level.getBlockState(top); state.getBlock().use(state,level,top,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(top),Direction.UP,top,false));
        h.assertTrue(handler.getFluidInTank(0).getAmount()==63000 && player.getMainHandItem().is(Items.BUCKET),"ceiling funnel fills the drum and returns bucket");
        h.succeed();
    }
}
