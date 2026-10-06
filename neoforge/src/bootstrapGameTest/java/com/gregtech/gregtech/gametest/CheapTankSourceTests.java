package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.blockentity.machine.TankBlockEntity;
import com.gregtech.gregtech.content.transport.fluid.CheapWoodBarrelCatalog;
import com.gregtech.gregtech.content.transport.TransportCraftingCatalog;
import com.gregtech.gregtech.content.recipe.VanillaRecoveryRecipes;
import com.gregtech.gregtech.content.recipe.MaterialRecoveryRules;
import com.gregtech.gregtech.data.generated.GT6Materials;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;
import static com.gregtech.gregtech.gametest.TransportMaterialPortTests.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_cheap_tanks") @net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class CheapTankSourceTests {
    private static void recover(GameTestHelper h, ItemStack stack) {
        var recipe=VanillaRecoveryRecipes.recipes().stream().filter(r->r.mInputs[0].is(stack.getItem())).findFirst().orElseThrow();
        h.assertTrue(RecipeInputs.consume(recipe,List.of(stack),List.of(),1)!=null,"actual clean recovery predicate");
        var available=new HashMap<GTMaterial,Long>();
        for(var c:ItemMaterialRegistry.get(stack).orElseThrow().components())
            available.merge(c.material().getTargetPulverMaterial().resolve(),MaterialRecoveryRules.pulverizedAmount(c.material(),c.amount()),Math::addExact);
        for(var out:recipe.mOutputs){var form=MaterialEquivalence.form(out);h.assertTrue(form!=null,"real registered dust output");available.merge(form.material(),-form.prefix().getMaterialWeight()*out.getCount(),Long::sum);}
        h.assertTrue(available.values().stream().allMatch(v->v>=0&&v<GTValues.U/72),"every component recovered without creating material");
        var unsafe=stack.copy();stored(unsafe);
        h.assertTrue(RecipeInputs.consume(recipe,List.of(unsafe),List.of(),1)==null,"arbitrary empty storage tag remains guarded");
        checkCrucible(h,stack);
    }
    private static void wear(GameTestHelper h,List<ItemStack> stacks,List<ItemStack> remaining) {
        for(int i=0;i<stacks.size();i++)if(stacks.get(i).getItem() instanceof GTToolItem t)
            h.assertTrue(remaining.get(i).is(t)&&remaining.get(i).getDamageValue()==t.toolType().damagePerCraft(),"actual per-craft tool wear");
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void fourSourceCheapBarrelsCraftStoreAndRecover(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,4,4));var player=TankItemLifecycleTests.player(h,pos);
        for(var entry:CheapWoodBarrelCatalog.ENTRIES){
            var description=TransportCraftingCatalog.rows().stream().filter(r->r.output().equals("gregtech:"+entry.id())).findFirst().orElseThrow();
            var recipe=row(h,description.path());var stacks=inputs(description);
            for(var plank:List.of(Items.OAK_PLANKS,Items.SPRUCE_PLANKS)) {
                for(int slot:List.of(3,5,6,8))stacks.set(slot,new ItemStack(plank));
                h.assertTrue(recipe.matches(grid(stacks),h.getLevel()),"source plankAnyWood accepts actual vanilla wood species");
            }
            wear(h,stacks,recipe.getRemainingItems(grid(stacks)));
            var decoded=wire(h,recipe);h.assertTrue(decoded.matches(grid(stacks),h.getLevel()),"cheap barrel native recipe synchronization");
            var crafted=recipe.assemble(grid(stacks),h.getLevel().registryAccess());
            h.assertTrue(crafted.is(item("gregtech:"+entry.id()).getItem())&&crafted.getCount()==1&&crafted.getMaxStackSize()==16,"four distinct outputs / source empty stacking");
            h.assertTrue(units(ItemMaterialRegistry.get(crafted).orElseThrow()).equals(Map.of(GT6Materials.Woods.Wood,GTValues.U*4,entry.rod(),GTValues.U*2)),"four wood and two rod units");
            recover(h,crafted);
            var tank=TankItemLifecycleTests.place(h,player,pos,crafted);
            h.assertTrue(tank.spec().capacity()==8000&&tank.spec().maxTemperature()==340&&tank.spec().hardness()==1F&&tank.spec().blastResistance()==5F,"source cheap barrel capacity / temperature / physical properties");
            h.assertTrue(tank.getFluidTank().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,10000),FluidAction.EXECUTE)==8000,"actual fill stops at source capacity");
            var filled=TankItemLifecycleTests.harvest(h,player,tank);
            h.assertTrue(filled.getMaxStackSize()==1&&!ItemMaterialRegistry.canRecover(filled),"filled source barrel remains unique and protected");
            tank=TankItemLifecycleTests.place(h,player,pos,filled);
            h.assertTrue(tank.getFluidTank().drain(10000,FluidAction.EXECUTE).getAmount()==8000,"real replacement and drain conserve stored water");
            var clean=TankItemLifecycleTests.harvest(h,player,tank);recover(h,clean);
            if(entry.id().equals("wood_barrel")) {
                tank=TankItemLifecycleTests.place(h,player,pos,clean);
                tank.getFluidTank().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1),FluidAction.EXECUTE);
                var legacy=TankItemLifecycleTests.worldData(h,tank);
                legacy.getCompound("gt.tank").putLong("Capacity",16000);legacy.getCompound("gt.tank").putLong("Amount",12000);
                var oldItem=item("gregtech:wood_barrel");TankItemLifecycleTests.setItemData(oldItem,legacy);
                tank=TankItemLifecycleTests.place(h,player,pos,oldItem);
                h.assertTrue(tank.getFluidTank().baseCapacity()==16000&&tank.getFluidTank().getAmount()==12000,"generated former 16k item shape preserves capacity and water");
                var oldDrop=TankItemLifecycleTests.harvest(h,player,tank);
                tank=TankItemLifecycleTests.place(h,player,pos,oldDrop);
                h.assertTrue(tank.getFluidTank().baseCapacity()==16000&&tank.getFluidTank().getAmount()==12000,"legacy capacity survives real harvest and replacement");
                TankItemLifecycleTests.harvest(h,player,tank);
            }
        }
        h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void logisticsSourceCompositionAndFilledInputProtection(GameTestHelper h) {
        long u=GTValues.U;var output=item("gregtech:logistics_tank");
        var expected=Map.of(GT6Materials.Elements.W,u*6+u/9*4,GT6Materials.Compounds.TinAlloy,u*4,
                GT6Materials.Elements.Os,u/8*6,GT6Materials.Compounds.EnderPearl,u,
                GT6Materials.Elements.Al,u+u/9,GT6Materials.Elements.Pt,u,GT6Materials.Compounds.Emerald,u);
        h.assertTrue(units(ItemMaterialRegistry.get(output).orElseThrow()).equals(expected),"source recursive known REV inputs; no invented circuit data");recover(h,output);
        var recipe=(net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput>)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("gregtech:logistics/logistics_tank")).orElseThrow().value();
        var stacks=new ArrayList<ItemStack>();
        for(var ingredient:recipe.getIngredients()) {var stack=ingredient.getItems()[0].copyWithCount(1);
            if(stack.getItem() instanceof GTToolItem t)stack=GTToolItem.create(t.toolType(),com.gregtech.gregtech.content.material.Materials.Steel,GT6Materials.Woods.Wood);
            stacks.add(stack);}
        h.assertTrue(recipe.matches(grid(stacks),h.getLevel()),"actual empty tungsten drum source crafting entry");
        h.assertTrue(recipe.assemble(grid(stacks),h.getLevel().registryAccess()).is(output.getItem()),"actual logistics crafting result");
        wear(h,stacks,recipe.getRemainingItems(grid(stacks)));var decoded=wire(h,recipe);
        var pos=h.absolutePos(new BlockPos(4,4,4));var player=TankItemLifecycleTests.player(h,pos);
        var tank=TankItemLifecycleTests.place(h,player,pos,stacks.get(7));
        tank.getFluidTank().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),FluidAction.EXECUTE);
        stacks.set(7,TankItemLifecycleTests.harvest(h,player,tank));
        h.assertTrue(!recipe.matches(grid(stacks),h.getLevel())&&!decoded.matches(grid(stacks),h.getLevel()),"filled drum protected before and after native synchronization");
        tank=TankItemLifecycleTests.place(h,player,pos,stacks.get(7));tank.getFluidTank().drain(1000,FluidAction.EXECUTE);
        stacks.set(7,TankItemLifecycleTests.harvest(h,player,tank));
        h.assertTrue(recipe.matches(grid(stacks),h.getLevel())&&decoded.matches(grid(stacks),h.getLevel()),"actually drained drum re-enters crafting");
        h.succeed();
    }
}
