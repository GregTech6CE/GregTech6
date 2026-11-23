package gregtech.content.blockentity.machine;

import gregtech.api.addon.machine.LayoutPart;
import gregtech.api.addon.platform.PartClientView;
import gregtech.content.block.machine.AddonFunctionalPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** A functional part with no energy role. Slots, tick, clicks and fields live on {@link FunctionalPartRuntime}. */
public class AddonFunctionalPartBlockEntity extends MultiblockPortBlockEntity
        implements FunctionalPartRuntime.Host, PartClientView {
    private final FunctionalPartRuntime function;
//? if >=1.21.1 {
//?} else {
    private final java.util.EnumMap<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> itemCaps =
            new java.util.EnumMap<>(Direction.class);
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> unsided =
            net.minecraftforge.common.util.LazyOptional.empty();
    private final java.util.EnumMap<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler>> fluidCaps =
            new java.util.EnumMap<>(Direction.class);
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> unsidedFluid =
            net.minecraftforge.common.util.LazyOptional.empty();
//?}

    public AddonFunctionalPartBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.content.registry.GTBlockEntities.ADDON_FUNCTIONAL_PART.get(), pos, state);
        this.function = new FunctionalPartRuntime(this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AddonFunctionalPartBlockEntity part) {
        part.function.tick();
    }

    @Override public FunctionalPartRuntime function() { return function; }

    @Override public Level level() { return level; }

    @Override public BlockPos pos() { return worldPosition; }

    @Override public BlockState blockState() { return getBlockState(); }

    @Override public String addonId() { return block().addonId(); }

    @Override public LayoutPart declaration() { return block().declaration(); }

    @Override public boolean removed() { return isRemoved(); }

    @Override public BlockPos controllerPos() { return super.controllerPos(); }

    @Override public boolean boundTo(BlockPos owner) { return owner != null && isBoundTo(owner); }

    @Override public void markChanged() { setChanged(); }

    @Override public void syncToClient() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override public boolean canBind(BlockPos owner) {
        return super.canBind(owner);
    }

    @Override public String name() {
        var id = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock());
        return id == null ? "" : id.getPath();
    }

    @Override public int slots() { return function.slotCount(); }

    @Override public ItemStack item(int slot) { return function.item(slot); }

    @Override public boolean occupied(int slot) { return !item(slot).isEmpty(); }

    @Override public int getInt(String key) { return function.context().state().getInt(key); }

    @Override public long getLong(String key) { return function.context().state().getLong(key); }

    @Override public boolean getBoolean(String key) { return function.context().state().getBoolean(key); }

    @Override public String getString(String key) { return function.context().state().getString(key); }

    private AddonFunctionalPartBlock block() { return (AddonFunctionalPartBlock) getBlockState().getBlock(); }

//? if >=1.21.1 {
    /*@Override public net.neoforged.neoforge.items.IItemHandler itemCapability(Direction side) {
        if (isRemoved()) return null;
        var own = function.items(side);
        return own != null ? own : super.itemCapability(side);
    }

    @Override public net.neoforged.neoforge.fluids.capability.IFluidHandler fluidCapability(Direction side) {
        if (isRemoved()) return null;
        var own = function.fluids(side);
        return own != null ? own : super.fluidCapability(side);
    }
*///?} else {
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && !isRemoved()
                && function.items(side) != null) {
            if (side == null) {
                if (!unsided.isPresent()) unsided = net.minecraftforge.common.util.LazyOptional.of(() -> function.items(null));
                return unsided.cast();
            }
            return itemCaps.computeIfAbsent(side, found -> net.minecraftforge.common.util.LazyOptional.of(() -> function.items(found))).cast();
        }
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER && !isRemoved()
                && function.fluids(side) != null) {
            if (side == null) {
                if (!unsidedFluid.isPresent()) unsidedFluid = net.minecraftforge.common.util.LazyOptional.of(() -> function.fluids(null));
                return unsidedFluid.cast();
            }
            return fluidCaps.computeIfAbsent(side, found -> net.minecraftforge.common.util.LazyOptional.of(() -> function.fluids(found))).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        unsided.invalidate();
        itemCaps.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        itemCaps.clear();
        unsidedFluid.invalidate();
        fluidCaps.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        fluidCaps.clear();
    }

    @Override public void reviveCaps() {
        super.reviveCaps();
        unsided = net.minecraftforge.common.util.LazyOptional.empty();
        unsidedFluid = net.minecraftforge.common.util.LazyOptional.empty();
    }
//?}

    @Override protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        saveFunction(tag, lookup);
    }

    @Override protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        loadFunction(tag, lookup);
    }

    @Override protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.writeUpdateTag(tag, lookup);
        saveFunction(tag, lookup);
    }

    @Override protected void readUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        loadFromTag(tag, lookup);
    }

    @Override protected void handleDataPacket(net.minecraft.network.Connection net,
            net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,
            @Nullable HolderLookup.Provider lookup) {
        if (packet.getTag() != null) loadFromTag(packet.getTag(), lookup);
    }

    private void saveFunction(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        function.save(tag, lookup);
    }

    private void loadFunction(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        function.load(tag, lookup);
    }
}
