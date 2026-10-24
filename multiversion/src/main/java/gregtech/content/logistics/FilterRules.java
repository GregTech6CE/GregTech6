package gregtech.content.logistics;

import gregtech.content.item.MaterialItem;
import net.minecraft.world.item.*;

/**
 * Logistics template adapter: an untagged template ignores stack data, not registry identity.
 * This is not the original item-retriever cover's metadata-only set matching.
 */
public final class FilterRules {
    private FilterRules() {
    }

    public static boolean itemMatches(ItemStack templateStack, ItemStack candidateStack) {
//? if >=1.21.1 {
        /*return FilterPolicy.matches(!templateStack.isEmpty() && !candidateStack.isEmpty(),
                templateStack.is(candidateStack.getItem()), templateStack.getComponentsPatch().isEmpty(),
                ItemStack.isSameItemSameComponents(templateStack, candidateStack));
*///?} else {
        return FilterPolicy.matches(!templateStack.isEmpty() && !candidateStack.isEmpty(),
                templateStack.is(candidateStack.getItem()), !templateStack.hasTag(),
                ItemStack.isSameItemSameTags(templateStack, candidateStack));
//?}
    }

    public static String prefix(ItemStack candidateStack) {
        if (candidateStack.isEmpty())
            return "";
        if (candidateStack.getItem() instanceof gregtech.internal.material.MaterialFormItem formItem)
            return formItem.getPrefix().getName();
//? if >=1.21.1 {
        /*if (candidateStack.getItem() instanceof gregtech.internal.inventory.ContainerShapeLike formItem
                && formItem.shapeId().equals("cell"))
            return "capcellcon";
        if (candidateStack.getItem() instanceof BlockItem formItem
                && formItem.getBlock() instanceof gregtech.internal.inventory.PipeFormLike pipeForm)
            return pipePrefix(pipeForm.pipeSizeName());
*///?} else {
        if (candidateStack.getItem() instanceof gregtech.content.item.PortableFluidContainerItem formItem
                && formItem.spec().shapeId().equals("cell"))
            return "capcellcon";
        if (candidateStack.getItem() instanceof BlockItem formItem
                && formItem.getBlock() instanceof gregtech.content.block.machine.FluidPipeBlock pipeForm)
            return pipePrefix(pipeForm.spec().size().name());
        if (candidateStack.getItem() instanceof BlockItem formItem
                && formItem.getBlock() instanceof gregtech.content.block.machine.ItemPipeBlock pipeForm)
            return pipePrefix(pipeForm.spec().size().name());
//?}
        if (candidateStack.getItem() instanceof BlockItem formItem
                && formItem.getBlock() instanceof gregtech.content.block.MaterialBlockLike materialBlock)
            return materialBlock.prefix().getName();
        // Known standard material forms bridge Forge tag conventions to original prefixes.
        // Do not treat arbitrary forge tags (e.g. ores/metals) as interchangeable prefixes.
        for (String[] tagMapping : new String[][]{
                {"ingots", "ingot"},
                {"nuggets", "nugget"},
                {"dusts", "dust"},
                {"plates", "plate"},
                {"rods", "stick"},
                {"gems", "gem"}})
//? if >=1.21.1 {
            /*if (candidateStack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", tagMapping[0])))
                    || candidateStack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", tagMapping[0]))))
                return tagMapping[1];
*///?} else {
            if (candidateStack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", tagMapping[0]))))
                return tagMapping[1];
//?}
        return "";
    }

    private static String pipePrefix(String sizeName) {
        var prefixName = new StringBuilder("pipe");
        for (String sizeWord : sizeName.toLowerCase(java.util.Locale.ROOT).split("_"))
            prefixName.append(Character.toUpperCase(sizeWord.charAt(0))).append(sizeWord.substring(1));
        return prefixName.toString();
    }
}
