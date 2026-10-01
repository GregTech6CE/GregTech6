package com.gregtech.gregtech.blockentity.misc;


import com.gregtech.gregtech.content.food.SandwichIngredients;
import com.gregtech.gregtech.registry.GTBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The sixteen GT6 sandwich layer slots. An occupied slot marks the layer's bottom pixel. */
public final class SandwichBlockEntity extends BlockEntity {
    public static final int SLOTS = com.gregtech.gregtech.content.food.SandwichRules.SLOTS;
    private final ItemStack[] ingredients = new ItemStack[SLOTS];
    private boolean redstone;
    private boolean dropped;

    public SandwichBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.SANDWICH_BLOCK.get(), pos, state);
        Arrays.fill(ingredients, ItemStack.EMPTY);
        seedDefault();
    }

    private static ItemStack gt(String id) {
        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void seedDefault() {
        // GT6 MultiTileEntitySandwich.readFromNBT2: the bare sandwich block is a sample sandwich.
        ingredients[0] = gt("toast");
        ingredients[2] = gt("cooked_meat_bar");
        ingredients[4] = gt("cucumber_slice");
        ingredients[5] = gt("cheese_slice");
        ingredients[6] = gt("onion_slice");
        ingredients[7] = gt("dressing");
        ingredients[8] = gt("tomato_slice");
        ingredients[9] = gt("mayo");
        ingredients[10] = gt("pickle_slice");
        ingredients[11] = gt("toast");
    }

    public ItemStack ingredient(int slot) {
        return slot < 0 || slot >= SLOTS ? ItemStack.EMPTY : ingredients[slot].copy();
    }

    public int layerId(int slot) {
        var layer = slot < 0 || slot >= SLOTS ? null : SandwichIngredients.forItem(ingredients[slot]);
        return layer == null ? 255 : layer.id();
    }

    public int sizePixels() {
        int size = 1;
        for (int slot = 0; slot < SLOTS; slot++) {
            var layer = SandwichIngredients.forItem(ingredients[slot]);
            if (layer != null) size = Math.max(size, Math.min(16, slot + layer.thickness()));
        }
        return size;
    }

    public int comparatorSignal() {
        return sizePixels() - 1;
    }

    public int redstoneSignal() {
        return redstone ? comparatorSignal() : 0;
    }

    public boolean redstoneEnabled() { return redstone; }

    public ItemStack topIngredient() {
        for (int i = SLOTS - 1; i >= 0; i--) if (!ingredients[i].isEmpty()) return ingredients[i].copy();
        return ItemStack.EMPTY;
    }

    public int baseQuantity() {
        return ingredients[0].isEmpty() ? 1 : ingredients[0].getCount();
    }

    /** Returns the amount consumed, or zero if GT6 would reject this ingredient. */
    public int addIngredient(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var top = topIngredient();
        if (!top.isEmpty() && ItemStack.isSameItemSameComponents(top, stack)) return 0;
        int amount = baseQuantity();
        ItemStack remainder = stack.getCraftingRemainingItem();
        if (!remainder.isEmpty() && (remainder.is(Items.GLASS_BOTTLE)
                || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(remainder.getItem()).equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","bottle_empty"))))
            amount = com.gregtech.gregtech.content.food.SandwichRules.containerQuantity(amount,true);
        if (stack.getCount() < amount) return 0;
        if (!redstone && SandwichIngredients.isRedstone(stack)) {
            redstone = true;
            changed();
            return amount;
        }
        int slot = sizePixels();
        var layer = SandwichIngredients.forItem(stack);
        if (slot >= SLOTS || layer == null || slot + layer.thickness() > SLOTS
                || !ingredients[slot].isEmpty()) return 0;
        ingredients[slot] = stack.copyWithCount(amount);
        changed();
        return amount;
    }

    /** The block item can be eaten. GT6 adds every layer's food value, plus 0.5 saturation. */
    public int totalFood() { return totalFood(ingredients); }
    public float totalSaturation() { return totalSaturation(ingredients); }

    private static int totalFood(ItemStack[] stacks) {
        int food = 0;
        for (ItemStack stack : stacks) if (!stack.isEmpty()) {
            FoodProperties props = stack.getFoodProperties(null);
            food += props == null ? 1 : Math.max(1, props.nutrition());
        }
        return food;
    }

    private static float totalSaturation(ItemStack[] stacks) {
        float saturation = 0;
        for (ItemStack stack : stacks) if (!stack.isEmpty()) {
            FoodProperties props = stack.getFoodProperties(null);
            if (props != null) saturation = Math.max(saturation,props.nutrition()>0?props.saturation()/(2f*props.nutrition()):0f);
        }
        return com.gregtech.gregtech.content.food.SandwichRules.saturation(saturation);
    }

    public static int itemFood(ItemStack stack,net.minecraft.core.HolderLookup.Provider lookup) { return totalFood(readItemIngredients(stack,lookup)); }
    public static float itemSaturation(ItemStack stack,net.minecraft.core.HolderLookup.Provider lookup) { return totalSaturation(readItemIngredients(stack,lookup)); }

    private static ItemStack[] readItemIngredients(ItemStack stack,net.minecraft.core.HolderLookup.Provider lookup) {
        ItemStack[] result = new ItemStack[SLOTS];
        Arrays.fill(result, ItemStack.EMPTY);
        CompoundTag be=stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (be != null && be.contains("Ingredients", Tag.TAG_LIST)) readIngredients(be,result,lookup);
        else {
            result[0] = gt("toast"); result[2] = gt("cooked_meat_bar");
            result[4] = gt("cucumber_slice"); result[5] = gt("cheese_slice");
            result[6] = gt("onion_slice"); result[7] = gt("dressing");
            result[8] = gt("tomato_slice"); result[9] = gt("mayo");
            result[10] = gt("pickle_slice"); result[11] = gt("toast");
        }
        return result;
    }

    private static void readIngredients(CompoundTag tag,ItemStack[] output,net.minecraft.core.HolderLookup.Provider lookup) {
        Arrays.fill(output, ItemStack.EMPTY);
        ListTag list = tag.getList("Ingredients", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            int slot = row.getByte("Slot") & 255;
            if (slot < SLOTS) output[slot]=ItemStack.parseOptional(lookup==null?net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY):lookup,row.getCompound("Stack"));
        }
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    /** Makes GT6's single ingredient drop when possible, otherwise its preserved sandwich item. */
    public ItemStack dropItem() {
        int occupied = 0;
        ItemStack only = ItemStack.EMPTY;
        for (ItemStack stack : ingredients) if (!stack.isEmpty()) { occupied++; only = stack; }
        if (occupied == 1 && only.getCraftingRemainingItem().isEmpty()) return only.copy();
        ItemStack result = new ItemStack(com.gregtech.gregtech.registry.GTSandwich.SANDWICH_BLOCK.get().asItem(), baseQuantity());
        CompoundTag tag = new CompoundTag();
        writeFields(tag,level.registryAccess());
        tag.putString("id","gregtech:sandwich_block");
        result.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        return result;
    }

    public ItemStack consumeDrop() {
        if (dropped) return ItemStack.EMPTY;
        dropped = true;
        return dropItem();
    }

    public List<ItemStack> contents() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : ingredients) if (!stack.isEmpty()) out.add(stack.copy());
        return out;
    }

    @Override protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        if (tag.contains("Ingredients", Tag.TAG_LIST)) readIngredients(tag,ingredients,lookup);
        redstone = tag.getBoolean("Redstone");
        dropped = false;
    }

    private void writeFields(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        ListTag list = new ListTag();
        for (int i = 0; i < SLOTS; i++) if (!ingredients[i].isEmpty()) {
            CompoundTag row = new CompoundTag();
            row.putByte("Slot", (byte) i);
            row.put("Stack", ingredients[i].saveOptional(lookup));
            list.add(row);
        }
        tag.put("Ingredients", list);
        tag.putBoolean("Redstone", redstone);
    }

    @Override protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        writeFields(tag,lookup);
    }

    @Override public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
