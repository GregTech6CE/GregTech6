package gregtech.engine.fluid;

import gregtech.content.item.WoodenBucketItem;
import gregtech.content.tool.WoodenBucketCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.ArrayList;
import java.util.List;

/** Bucket aliases for viewer lookup only; native fluid recipe execution remains exact. */
public final class FluidContainerDisplayBinding {
    private FluidContainerDisplayBinding() {}
    private static final java.util.Map<net.minecraft.world.level.material.Fluid, List<ItemStack>> CACHE = new java.util.IdentityHashMap<>();
    private static List<BucketItem> buckets;
    public static void invalidate() { CACHE.clear(); buckets = null; }
//? if >=1.21.1 {
    /*public static List<ItemStack> containers(net.neoforged.neoforge.fluids.FluidStack fluid) {
*///?} else {
    public static List<ItemStack> containers(net.minecraftforge.fluids.FluidStack fluid) {
//?}
        if (fluid.isEmpty()) return List.of();
        return CACHE.computeIfAbsent(fluid.getFluid(), FluidContainerDisplayBinding::find);
    }
    private static List<ItemStack> find(net.minecraft.world.level.material.Fluid fluid) {
        var result = new ArrayList<ItemStack>();
        if (buckets == null) buckets = java.util.stream.StreamSupport.stream(BuiltInRegistries.ITEM.spliterator(), false)
                .filter(BucketItem.class::isInstance).map(BucketItem.class::cast).toList();
        for (BucketItem bucket : buckets) {
            var stored = fluidOf(bucket);
            if (stored != net.minecraft.world.level.material.Fluids.EMPTY && stored.isSame(fluid)) result.add(new ItemStack(bucket));
        }
        var content = WoodenBucketItem.contentOf(fluid);
        if (content != null && content.id().equals("water") && result.stream().noneMatch(s -> s.is(Items.WATER_BUCKET)))
            result.add(new ItemStack(Items.WATER_BUCKET));
        if (content != null) for (var band : WoodenBucketCatalog.BANDS) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", WoodenBucketCatalog.itemId(band, content)));
            if (item != Items.AIR) result.add(new ItemStack(item));
        }
        return List.copyOf(result);
    }

    /** The fluid a vanilla bucket carries: a public field on 1.21.1, an accessor on 1.20.1. */
    private static net.minecraft.world.level.material.Fluid fluidOf(BucketItem bucket) {
//? if >=1.21.1 {
        /*return bucket.content;
*///?} else {
        return bucket.getFluid();
//?}
    }

    public static List<ItemStack> bucketVariants(ItemStack empty) {
        if (!(empty.getItem() instanceof WoodenBucketItem bucket) || !bucket.content().empty()) return List.of();
        return WoodenBucketCatalog.CONTENTS.stream().map(content -> new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", WoodenBucketCatalog.itemId(bucket.band(), content)))))
                .filter(stack -> !stack.isEmpty()).toList();
    }
}
