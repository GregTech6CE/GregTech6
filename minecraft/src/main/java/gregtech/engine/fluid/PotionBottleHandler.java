package gregtech.engine.fluid;

import gregtech.content.fluid.VanillaPotionBottling;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
*///?} else {
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
//?}

/** Native finite bottle capability shared by faucets, tanks and automatic container transfers. */
public final class PotionBottleHandler implements IFluidHandlerItem {
    private ItemStack container;

    public PotionBottleHandler(ItemStack container) { this.container = container; }

    public static boolean acceptsItem(Item item) {
        return item instanceof gregtech.content.item.BottleItem || item == Items.GLASS_BOTTLE || item == Items.POTION || item == Items.SPLASH_POTION
                || item == Items.LINGERING_POTION || BuiltInRegistries.ITEM.getKey(item)
                .equals(ResourceLocation.fromNamespaceAndPath("gt6ce", "bottle_empty"));
    }

    public static ItemStack filled(FluidStack fluid) {
        if (fluid.isEmpty()) return ItemStack.EMPTY;
        var plain = new FluidStack(fluid.getFluid(), fluid.getAmount());
//? if >=1.21.1 {
        /*if (!FluidStack.isSameFluidSameComponents(fluid, plain)) return ItemStack.EMPTY;
*///?} else {
        if (!fluid.isFluidEqual(plain)) return ItemStack.EMPTY;
//?}
        if (fluid.getFluid() == Fluids.WATER) return waterBottle();
        var bottles = gregtech.content.data.generated.GTBottlesGen.ENTRIES;
        for (int i = 0; i < bottles.length; i += 3) {
            var item = gregtech.content.food.GTFoodItems.itemOrNull(bottles[i]);
            if (item instanceof gregtech.content.item.BottleItem bottle && bottle.fluid() == fluid.getFluid())
                return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack waterBottle() {
//? if >=1.21.1 {
        /*return PotionContents.createItemStack(Items.POTION, Potions.WATER);
*///?} else {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
//?}
    }

    /** Reject custom effects/names/components: draining must not silently discard potion data. */
    public static FluidStack contents(ItemStack stack) {
        if (stack.getItem() instanceof gregtech.content.item.BottleItem bottle) {
            var fluid = bottle.fluid();
            return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, VanillaPotionBottling.AMOUNT);
        }
        if (!stack.is(Items.POTION)) return FluidStack.EMPTY;
//? if >=1.21.1 {
        /*var potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
        if (potion.isEmpty() || potion.get().value() != Potions.WATER) return FluidStack.EMPTY;
*///?} else {
        if (PotionUtils.getPotion(stack) != Potions.WATER) return FluidStack.EMPTY;
//?}
        return new FluidStack(Fluids.WATER, VanillaPotionBottling.AMOUNT);
    }

    @Override public ItemStack getContainer() { return container; }
    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? contents(container) : FluidStack.EMPTY; }
    @Override public int getTankCapacity(int tank) { return tank == 0 ? VanillaPotionBottling.AMOUNT : 0; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && !filled(stack).isEmpty(); }

    @Override public int fill(FluidStack resource, FluidAction action) {
        if (container.getCount() != 1 || container.getItem() instanceof gregtech.content.item.BottleItem || container.is(Items.POTION) || container.is(Items.SPLASH_POTION)
                || container.is(Items.LINGERING_POTION) || resource.getAmount() < VanillaPotionBottling.AMOUNT)
            return 0;
        var result = filled(resource);
        if (result.isEmpty()) return 0;
        if (action.execute()) container = result;
        return VanillaPotionBottling.AMOUNT;
    }

    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        var stored = contents(container);
//? if >=1.21.1 {
        /*if (!FluidStack.isSameFluidSameComponents(stored, resource)) return FluidStack.EMPTY;
*///?} else {
        if (!stored.isFluidEqual(resource)) return FluidStack.EMPTY;
//?}
        return drain(resource.getAmount(), action);
    }

    @Override public FluidStack drain(int amount, FluidAction action) {
        if (container.getCount() != 1 || amount < VanillaPotionBottling.AMOUNT) return FluidStack.EMPTY;
        var stored = contents(container);
        if (!stored.isEmpty() && action.execute()) container = container.getItem() instanceof gregtech.content.item.BottleItem
                ? gregtech.content.item.BottleItem.emptyBottle() : new ItemStack(Items.GLASS_BOTTLE);
        return stored;
    }
}
