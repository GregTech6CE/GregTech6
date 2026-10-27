package gregtech.content.recipe;

import gregtech.content.tool.WoodenBucketCatalog;
import gregtech.engine.fluid.PortableFluidContainerSpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
import java.util.ArrayList;
import java.util.List;

/** Native examples for fluid-content ingredients, never for recipes consuming the vessel itself. */
final class FiniteCraftingVessels {
    private FiniteCraftingVessels() {}

    static List<ItemStack> examples(Fluid... fluids) {
        return examples(1000, fluids);
    }

    static List<ItemStack> examples(int amount, Fluid... fluids) {
        var empty = new ArrayList<ItemStack>();
        // Keep the familiar tin cell first, then include every native vessel that really fits.
        add(empty, "fluid_cell_tin");
        for (var spec : PortableFluidContainerSpec.values())
            if (spec != PortableFluidContainerSpec.CELL_TIN) add(empty, "fluid_" + spec.id());
        for (var band : WoodenBucketCatalog.BANDS)
            add(empty, WoodenBucketCatalog.itemId(band, WoodenBucketCatalog.empty()));
        empty.add(new ItemStack(Items.BUCKET));
        var result = new ArrayList<ItemStack>();
        for (var fluid : fluids) {
            if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) continue;
            var required = new FluidStack(fluid, amount);
            for (var vessel : empty) {
//? if >=1.21.1 {
                /*var handler = FluidUtil.getFluidHandler(vessel.copy()).orElse(null);
*///?} else {
                var handler = FluidUtil.getFluidHandler(vessel.copy()).resolve().orElse(null);
//?}
                if (handler == null || handler.fill(required, IFluidHandler.FluidAction.EXECUTE) != amount) continue;
                var filled = handler.getContainer().copy();
                var before = FluidUtil.getFluidContained(filled).orElse(FluidStack.EMPTY);
                // Wooden buckets canonicalize some families; never advertise a different fluid.
                if (!sameFluid(before, required) || before.getAmount() != amount) continue;
                var drained = handler.drain(required, IFluidHandler.FluidAction.EXECUTE);
                var after = FluidUtil.getFluidContained(handler.getContainer()).orElse(FluidStack.EMPTY);
                if (!sameFluid(drained, required) || drained.getAmount() != amount
                        || before.getAmount() - after.getAmount() != amount) continue;
//? if >=1.21.1 {
                /*if (result.stream().noneMatch(existing -> ItemStack.isSameItemSameComponents(existing, filled)))
*///?} else {
                if (result.stream().noneMatch(existing -> ItemStack.isSameItemSameTags(existing, filled)))
//?}
                    result.add(filled);
            }
        }
        if (result.isEmpty()) throw new IllegalStateException("No finite crafting vessel for requested fluids");
        return List.copyOf(result);
    }

    private static boolean sameFluid(FluidStack first, FluidStack second) {
//? if >=1.21.1 {
        /*return FluidStack.isSameFluidSameComponents(first, second);
*///?} else {
        return first.isFluidEqual(second);
//?}
    }

    private static void add(List<ItemStack> stacks, String path) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path));
        if (item != Items.AIR) stacks.add(new ItemStack(item));
    }
}
