package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity;
import com.gregtech.gregtech.recipe.FiniteBottleFillingRecipe;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import com.gregtech.gregtech.registry.GTFluidItems;
import com.gregtech.gregtech.registry.GTToolBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class AdvancedBumbliaryRecipeTests {
    private static TransientCraftingContainer grid() {
        return new TransientCraftingContainer(new AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        },3,3);
    }

    @GameTest(template="test_empty", timeoutTicks=200)
    public static void advancedRecipeConsumesTwoRealHoneyContainers(GameTestHelper h) {
        var id=GregTech.id("tool_blocks/advanced_bumbliary");
        var recipe=(ToolShapedRecipe)h.getLevel().getRecipeManager().byKey(id).orElseThrow();
        var grid=grid();
        for(int i=0;i<9;i++) {
            h.assertTrue(recipe.getIngredients().get(i).getItems().length>0,"resolved ingredient "+i);
            grid.setItem(i,recipe.getIngredients().get(i).getItems()[0].copy());
        }
        h.assertTrue(recipe.matches(grid,h.getLevel()),"original PRP/HBH/PCP grid matches");
        h.assertTrue(recipe.assemble(grid,h.getLevel().registryAccess()).is(GTToolBlocks.ADVANCED_BUMBLIARY.get().asItem()),"functional advanced machine output");
        var remains=recipe.getRemainingItems(grid);
        for(int slot:new int[]{3,5}) {
            h.assertTrue(!remains.get(slot).isEmpty(),"finite honey vessel is returned");
            h.assertTrue(FluidUtil.getFluidContained(remains.get(slot)).orElse(FluidStack.EMPTY).isEmpty(),"each vessel loses exactly 1000 mB");
            h.assertTrue(FluidUtil.getFluidContained(grid.getItem(slot)).orElseThrow().getAmount()==1000,"remainder calculation does not mutate input");
        }
        h.assertTrue(!recipe.getIngredients().get(1).test(new ItemStack(ForgeRegistries.ITEMS.getValue(GregTech.id("honey_comb")))),"ordinary honey comb is not crossbred");
        h.assertTrue(recipe.getIngredients().get(1).getItems().length==10,"all ten GT6 crossbred comb families available");
        var honey=FluidUtil.getFluidContained(grid.getItem(3)).orElseThrow().getFluid();
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            ToolShapedRecipe.SERIALIZER.toNetwork(buffer,recipe);
            var decoded=ToolShapedRecipe.SERIALIZER.fromNetwork(id,buffer);
            h.assertTrue(decoded.matches(grid,h.getLevel()) && buffer.readableBytes()==0,"client recipe retains finite ingredients");
            grid.setItem(3,new ItemStack(GTFluidItems.forFluid(honey)));
            h.assertTrue(!decoded.matches(grid,h.getLevel()),"display/infinite honey cannot craft the machine");
        } finally { buffer.release(); }
        var handler=FluidUtil.getFluidHandler(recipe.getIngredients().get(3).getItems()[0].copy()).resolve().orElseThrow();
        handler.drain(1,IFluidHandler.FluidAction.EXECUTE);
        grid.setItem(3,handler.getContainer());
        h.assertTrue(!recipe.matches(grid,h.getLevel()),"999 mB does not satisfy a 1000 mB vessel");
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=200)
    public static void finiteVesselRemaindersFollowOffsetAndMirroring(GameTestHelper h) {
        var finite=FiniteBottleFillingRecipe.container("Honey");
        var base=new ShapedRecipe(GregTech.id("test_honey_offset"),"",CraftingBookCategory.MISC,2,1,
                NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.DIRT),finite),new ItemStack(Items.STONE));
        var recipe=new ToolShapedRecipe(base,true);
        for(boolean mirrored:new boolean[]{false,true}) {
            var grid=grid();
            int fluidSlot=mirrored?7:8;
            grid.setItem(mirrored?8:7,new ItemStack(Items.DIRT));
            grid.setItem(fluidSlot,finite.getItems()[0].copy());
            h.assertTrue(recipe.matches(grid,h.getLevel()),"shifted and mirrored grid matches");
            var remains=recipe.getRemainingItems(grid);
            h.assertTrue(!remains.get(fluidSlot).isEmpty() && FluidUtil.getFluidContained(remains.get(fluidSlot)).orElse(FluidStack.EMPTY).isEmpty(),"drained vessel returned at actual fluid slot");
            h.assertTrue(remains.get(mirrored?8:7).isEmpty(),"ordinary ingredient receives no phantom vessel");
        }
        h.succeed();
    }

    @GameTest(template="test_empty", timeoutTicks=200)
    public static void thermometerReadsAnyFaceWithoutOpeningNest(GameTestHelper h) {
        BlockPos pos=new BlockPos(1,1,1);
        h.setBlock(pos,GTToolBlocks.BUMBLIARY.get());
        var machine=(BumbliaryBlockEntity)h.getBlockEntity(pos);
        var player=h.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ForgeRegistries.ITEMS.getValue(GregTech.id("quicksilver_thermometer"))));
        long before=machine.countdown();
        for(Direction side:Direction.values()) {
            var absolute=h.absolutePos(pos);
            var result=machine.getBlockState().getBlock().use(machine.getBlockState(),h.getLevel(),absolute,player,
                    InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(absolute),side,absolute,false));
            h.assertTrue(result.consumesAction(),"thermometer handled on "+side);
            h.assertTrue(machine.countdown()==before,"reading does not delay breeding");
        }
        h.succeed();
    }
}
