package gregtech.content.material;

import gregtech.content.block.inventory.LootChestBlock;
import gregtech.content.block.inventory.MetalChestBlock;
import gregtech.content.block.machine.MoldBlock;
import gregtech.content.recipe.ClayMoldCatalog;
import gregtech.engine.material.ItemMaterialRuntimeFacts;
import gregtech.engine.stack.StackData;
import java.util.Set;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Interprets GT6 block-specific stack payloads without teaching the material engine those blocks. */
final class ItemMaterialContentFacts implements ItemMaterialRuntimeFacts.StoredContentsPolicy {
    static final ItemMaterialContentFacts INSTANCE = new ItemMaterialContentFacts();

    private ItemMaterialContentFacts() {}

    @Override
    public boolean hasStoredContents(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof LootChestBlock) {
//? if >=1.21.1 {
            /*var loot = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
            if (loot == null || !loot.copyTag().getBoolean("GTLootGenerated")) return true;
*///?} else {
            var loot = stack.getTagElement("BlockEntityTag");
            if (loot == null || !loot.getBoolean("GTLootGenerated")) return true;
//?}
        }
        var tag = StackData.read(stack);
        if (tag != null
                && (hasStoredBlockEntityData(stack, tag.getCompound("BlockEntityTag"))
                        || tag.contains("Items")
                        || tag.contains("EntityTag")
                        || tag.contains("ChargedProjectiles"))) return true;
//? if >=1.21.1 {
        /*var blockData = stack.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        return (blockData != null && hasStoredBlockEntityData(stack, blockData.copyTag()))
                || stack.has(net.minecraft.core.component.DataComponents.ENTITY_DATA)
                || stack.has(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA)
                || stack.has(net.minecraft.core.component.DataComponents.CONTAINER_LOOT)
                || stack.has(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES)
                || (stack.get(net.minecraft.core.component.DataComponents.CONTAINER) != null
                    && !stack.get(net.minecraft.core.component.DataComponents.CONTAINER).equals(
                            net.minecraft.world.item.component.ItemContainerContents.EMPTY));
*///?} else {
        return false;
//?}
    }

    private static boolean hasStoredBlockEntityData(ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
        if (tag.isEmpty()) return false;
        if (stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof MoldBlock
                && Set.of("id", ClayMoldCatalog.SHAPE_KEY).containsAll(tag.getAllKeys())
                && "gt6ce:mold".equals(tag.getString("id"))) return false;
        // Old empty chest drops carry loot bookkeeping even though their inventory drops separately.
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof MetalChestBlock))
            return true;
        if (!Set.of("id", "GTLootGenerated", "GTDungeonLootSeed").containsAll(tag.getAllKeys())) return true;
        return item.getBlock() instanceof LootChestBlock && !tag.getBoolean("GTLootGenerated");
    }
}
