package gregtech.content.blockentity.misc;

import gregtech.content.blockentity.GTBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {



/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
//?}
import org.jetbrains.annotations.Nullable;

/**
 * GT6's wild bumblebee hive ({@code MultiTileEntityBumbleHive}, {@code gregtech/tileentity/misc}):
 * a nine-slot container that GT6 fills with one comb, a princess and a handful of drones when the
 * world generator places it ({@code WorldgenHives.placeHive}).
 *
 * <p>GT6 never exposes those slots: {@code getAccessibleSlotsFromSide2} returns an empty array and
 * both {@code canInsertItem2}/{@code canExtractItem2} are false, so hoppers cannot touch a hive —
 * the only way to get the bees out is to break it (and only a player's break drops them,
 * {@code onBlockHarvested}/{@code canDrop}).</p>
 */
public class BumbleHiveBlockEntity extends GTBlockEntity {

    /** GT6's {@code getDefaultInventory}: nine slots. */
    public static final int SLOTS = 9;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }
    };

    public BumbleHiveBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.content.registry.GTBlockEntities.BUMBLE_HIVE.get(), pos, state);
    }

    /** The hive's contents, in GT6's slot order. */
    public ItemStackHandler inventory() { return inventory; }

    /** Upgrade the old color-property block after its inventory has loaded. */
    public void migrateLegacyVariant() {
        if (level == null || level.isClientSide || !getBlockState().hasProperty(gregtech.content.block.misc.BumbleHiveBlock.COLOR)) return;
        var color = getBlockState().getValue(gregtech.content.block.misc.BumbleHiveBlock.COLOR);
        var target = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", "bumble_hive_" + color.getName()));
        if (!(target instanceof gregtech.content.block.misc.FixedBumbleHiveBlock)) return;
        var contents = new ItemStack[SLOTS];
        for (int i = 0; i < SLOTS; i++) contents[i] = inventory.getStackInSlot(i).copy();
        if (level.setBlock(worldPosition, target.defaultBlockState(), 3)
                && level.getBlockEntity(worldPosition) instanceof BumbleHiveBlockEntity migrated) {
            for (int i = 0; i < SLOTS; i++) migrated.inventory.setStackInSlot(i, contents[i]);
        }
    }

    /** Everything inside, for the drop logic and the tooltips. */
    public java.util.List<ItemStack> contents() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) out.add(stack.copy());
        }
        return out;
    }

    /** Empties the hive (GT6 clears the inventory once the contents dropped). */
    public void clearContents() {
        for (int i = 0; i < SLOTS; i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.put("inventory", gregtech.engine.stack.HandlerNbt.save(inventory, lookup));
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        if (tag.contains("inventory")) gregtech.engine.stack.HandlerNbt.load(inventory, tag.getCompound("inventory"), lookup);
    }

    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.writeUpdateTag(tag, lookup);
        tag.put("inventory", gregtech.engine.stack.HandlerNbt.save(inventory, lookup));
    }

//? if >=1.21.1 {
//?} else {
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            // GT6 exposes no slots on any side: a hive is not a container for automation.
            return LazyOptional.empty();
        }
        return super.getCapability(capability, side);
    }
//?}

    /** Kept for the tests: the raw handler without the capability gate. */
    public IItemHandler rawHandler() { return inventory; }
}
