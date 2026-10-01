package com.gregtech.gregtech.item;


import com.gregtech.gregtech.content.food.GTDrinks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * A GT6 drink bottle ({@code MultiItemBottles}): right-clicking drinks 250&nbsp;mB of the fluid it holds
 * and hands the empty bottle back.
 *
 * <p>GT6 gives every {@code MultiItemBottles} entry a {@code FoodStatFluid}, i.e. the bottle <em>is</em>
 * the 250&nbsp;mB portion of that fluid, and the numbers come from {@code DrinksGT} (the port's
 * {@link GTDrinks} table, §93/§96). Bottles whose fluid the port does not have — and bottles of fluids
 * that are not drinks at all (mercury, ink, glue, lubricant, tar, the cooking oils) — pass instead of
 * being consumed, the same call the per-fluid {@link FluidItem} makes.
 *
 * <p>Drinking gives back GT6's own empty bottle ({@code bottle_empty}, the {@code bottle} material
 * prefix's "Empty") and only falls back to a vanilla glass bottle when that item is missing.
 */
public class BottleItem extends Item {
    /** GT6's bottle size: every {@code MultiItemBottles} row is {@code .make(250)}. */
    public static final int BOTTLE_AMOUNT = 250;
    /** GT6 {@code OP.bottle.dat(MT.Empty)} — the empty bottle a drink hands back. */
    public static final String EMPTY_BOTTLE_ID = "bottle_empty";

    private final String fluidKey;

    public BottleItem(String fluidKey, Properties properties) {
        super(properties);
        this.fluidKey = fluidKey;
    }

    /** The GT6 fluid expression this bottle was generated from ({@code Beer}, {@code potion.x}, …). */
    public String fluidKey() {
        return fluidKey;
    }

    /** The port fluid this bottle holds, or {@code null} when the port has no such fluid. */
    public Fluid fluid() {
        return GTDrinks.fluidForField(fluidKey);
    }

    /** GT6's empty bottle stack ({@code bottle_empty}), or a vanilla glass bottle when it is absent. */
    public static ItemStack emptyBottle() {
        Item item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",EMPTY_BOTTLE_ID));
        return item == null || item == Items.AIR ? new ItemStack(Items.GLASS_BOTTLE) : new ItemStack(item);
    }

    /** Crafting consumes one 250 mB bottle and returns the empty container, as in GT6. */
    @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
    @Override public ItemStack getCraftingRemainingItem(ItemStack stack) { return emptyBottle(); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Fluid fluid = fluid();
        if (fluid == null || GTDrinks.rowFor(fluid) == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            GTDrinks.drink(player, new FluidStack(fluid, BOTTLE_AMOUNT));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                com.gregtech.gregtech.content.food.GTFoodItems.deliverContainer(player,emptyBottle());
            }
        }
        return InteractionResultHolder.consume(stack);
    }
}
