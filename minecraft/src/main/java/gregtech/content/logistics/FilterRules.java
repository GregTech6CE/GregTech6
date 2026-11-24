package gregtech.content.logistics;

import gregtech.engine.stack.StackCompat;
import net.minecraft.world.item.*;

/** Matches a filter block's ghost templates and resolves ore dictionary prefixes. */
public final class FilterRules {
    private FilterRules() {
    }

    public static boolean itemMatches(ItemStack template, ItemStack candidate, boolean ignoreItemData) {
        if (template.isEmpty() || candidate.isEmpty()) return false;
        return ignoreItemData ? ItemStack.isSameItem(template, candidate) : StackCompat.sameItem(template, candidate);
    }

    public static String prefix(ItemStack candidateStack) {
        if (candidateStack.isEmpty())
            return "";
        if (candidateStack.getItem() instanceof gregtech.engine.material.MaterialFormItem formItem)
            return formItem.getPrefix().getName();
        if (candidateStack.getItem() instanceof gregtech.engine.inventory.ContainerShapeLike formItem
                && formItem.shapeId().equals("cell"))
            return "capcellcon";
        if (candidateStack.getItem() instanceof BlockItem formItem
                && formItem.getBlock() instanceof gregtech.engine.inventory.PipeFormLike pipeForm)
            return pipePrefix(pipeForm.pipeSizeName());
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
