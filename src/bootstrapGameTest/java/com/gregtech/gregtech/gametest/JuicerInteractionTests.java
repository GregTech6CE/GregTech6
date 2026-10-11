package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class JuicerInteractionTests {
    private static final BlockPos POS=new BlockPos(2,2,2);
    private static Recipe fixture() {
        return new Recipe(new ItemStack[]{new ItemStack(Items.BARRIER)},
                new ItemStack[]{new ItemStack(Items.DIAMOND),new ItemStack(Items.EMERALD,2),new ItemStack(Items.IRON_INGOT,3)},
                null,null,null,new FluidStack[]{new FluidStack(Fluids.WATER,200)},20,16,0);
    }
    private static ProcessingToolBlockEntity place(GameTestHelper h) {
        h.setBlock(POS,ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("gregtech:juicer")));
        return (ProcessingToolBlockEntity)h.getBlockEntity(POS);
    }
    private static int count(net.minecraft.world.entity.player.Player p,Item item) {
        int total=0;for(var stack:p.getInventory().items)if(stack.is(item))total+=stack.getCount();return total;
    }
    private static void click(GameTestHelper h,net.minecraft.world.entity.player.Player p,Direction face) {
        var pos=h.absolutePos(POS);var state=h.getLevel().getBlockState(pos);
        state.getBlock().use(state,h.getLevel(),pos,p,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false));
    }
    @GameTest(template="coin_pile_space")
    public static void heldIngredientsAndEverySolidOutputOnAllFaces(GameTestHelper h) {
        var recipe=fixture();MachineRecipeMaps.Juicer.mRecipeList.add(recipe);
        try {
            var be=place(h);
            for(var face:Direction.values()) {
                var player=h.makeMockSurvivalPlayer();
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BARRIER,2));
                click(h,player,face);
                h.assertTrue(player.getMainHandItem().getCount()==1,"one hand ingredient consumed on "+face);
                h.assertTrue(count(player,Items.DIAMOND)==1&&count(player,Items.EMERALD)==2&&count(player,Items.IRON_INGOT)==3,
                        "every solid output delivered directly on "+face);
                h.assertTrue(be.displayFluid().getAmount()==200,"liquid retained in vessel");
                be.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new AssertionError()).drain(1000,IFluidHandler.FluidAction.EXECUTE);
            }
            h.assertTrue(!be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(),"GT6 juicer has no automated item inventory");
            h.succeed();
        } finally {MachineRecipeMaps.Juicer.mRecipeList.remove(recipe);}
    }
    @GameTest(template="coin_pile_space")
    public static void outputThresholdRejectsWithoutConsumingHand(GameTestHelper h) {
        var recipe=fixture();MachineRecipeMaps.Juicer.mRecipeList.add(recipe);
        try {
            var be=place(h);var saved=be.saveWithoutMetadata();
            var tank=saved.getList("gt.tanks",10).getCompound(0);tank.putLong("Amount",1000);
            tank.put("Fluid",new FluidStack(Fluids.WATER,1000).writeToNBT(new net.minecraft.nbt.CompoundTag()));be.load(saved);
            var player=h.makeMockSurvivalPlayer();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BARRIER,2));
            click(h,player,Direction.UP);
            h.assertTrue(player.getMainHandItem().getCount()==2&&be.displayFluid().getAmount()==1000&&count(player,Items.DIAMOND)==0,
                    "existing 1000 mB blocks a 200 mB batch without consuming or yielding");
            be.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new AssertionError()).drain(100,IFluidHandler.FluidAction.EXECUTE);
            click(h,player,Direction.NORTH);
            h.assertTrue(player.getMainHandItem().getCount()==1&&be.displayFluid().getAmount()==1100,
                    "GT6 tests existing amount: 900 mB allows one 200 mB batch");
            click(h,player,Direction.NORTH);
            h.assertTrue(player.getMainHandItem().getCount()==1&&be.displayFluid().getAmount()==1100,"next batch stops until draining");
            h.succeed();
        } finally {MachineRecipeMaps.Juicer.mRecipeList.remove(recipe);}
    }
    @GameTest(template="coin_pile_space")
    public static void fullInventoryDropsAllResiduesAndDeniedUseDoesNothing(GameTestHelper h) {
        var recipe=fixture();MachineRecipeMaps.Juicer.mRecipeList.add(recipe);
        try {
            var be=place(h);var player=h.makeMockSurvivalPlayer();
            var center=Vec3.atCenterOf(h.absolutePos(POS));player.setPos(center.x,center.y,center.z);
            for(int slot=0;slot<player.getInventory().items.size();slot++)player.getInventory().items.set(slot,new ItemStack(Items.STONE,64));
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BARRIER,2));
            player.getAbilities().mayBuild=false;click(h,player,Direction.SOUTH);
            h.assertTrue(player.getMainHandItem().getCount()==2&&be.displayFluid().isEmpty(),"denied use has no effects");
            player.getAbilities().mayBuild=true;click(h,player,Direction.SOUTH);
            var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(center,center).inflate(2));
            int diamonds=0,emeralds=0,iron=0;
            for(var entity:drops) {
                var stack=entity.getItem();
                if(stack.is(Items.DIAMOND))diamonds+=stack.getCount();
                if(stack.is(Items.EMERALD))emeralds+=stack.getCount();
                if(stack.is(Items.IRON_INGOT))iron+=stack.getCount();
            }
            h.assertTrue(diamonds==1&&emeralds==2&&iron==3,"all three full-inventory outputs dropped exactly once");
            h.assertTrue(player.getMainHandItem().getCount()==1&&be.displayFluid().getAmount()==200,"input and liquid committed once");
            h.succeed();
        } finally {MachineRecipeMaps.Juicer.mRecipeList.remove(recipe);}
    }

}
