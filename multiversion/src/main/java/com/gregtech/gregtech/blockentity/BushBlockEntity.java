package com.gregtech.gregtech.blockentity;

import com.gregtech.gregtech.block.plant.BushBlock;
import com.gregtech.gregtech.content.plant.GTBerryBushes;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}
import org.jetbrains.annotations.Nullable;

/**
 * GT6's berry bush logic ({@code MultiTileEntityBush}).
 *
 * <p>The shared rules retain the 128-tick growth cycle and byte-sized counter rollover: one stage
 * advances per 256 growth increments. Rain and light inputs currently come from modern Level
 * queries; their environment contracts differ from the original GT6 helpers and are audited
 * separately. This entity also maintains the current fixed-species identity and legacy migration.
 */
public class BushBlockEntity extends BlockEntity {
    /** GT6 checks {@code SERVER_TIME % 128} — the growth cycle length. */
    public static final int CYCLE_TICKS = com.gregtech.gregtech.content.plant.BushGrowthRules.CYCLE_TICKS;
    /** GT6's byte overflow counter: a stage advances every 256 growth increments. */
    public static final int GROWTH_PER_STAGE = com.gregtech.gregtech.content.plant.BushGrowthRules.GROWTH_PER_STAGE;
    /** GT6's light gate when the bush cannot see the sky. */
    public static final int LIGHT_GATE = com.gregtech.gregtech.content.plant.BushGrowthRules.LIGHT_GATE;

    private String legacyBerry = "";
    private int growth;
    private boolean supportChecked;

    public BushBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.BUSH.get(), pos, state);
    }

    public String berryId() {
        var block = (BushBlock) getBlockState().getBlock();
        return block.berryId().isEmpty() ? legacyBerry : block.berryId();
    }

    /** Change the registered species; only the old untyped ID reads a legacy berry key. */
    public void setBerry(String id) {
        var variant = com.gregtech.gregtech.registry.GTBushes.byBerry(id);
        if (variant == null) return;
        var state = variant.defaultBlockState().setValue(BushBlock.STAGE, stage())
                .setValue(BushBlock.SUPPORT, getBlockState().getValue(BushBlock.SUPPORT));
        if (level == null) {
            setBlockState(state);
            legacyBerry = "";
            return;
        }
        if (level.isClientSide) return;
        int savedGrowth = growth;
        level.setBlock(worldPosition, state, 3);
        if (level.getBlockEntity(worldPosition) instanceof BushBlockEntity next) {
            next.legacyBerry = "";
            next.growth = savedGrowth;
            next.setChanged();
        }
    }

    /** The berries this bush hands out (GT6 keeps the item stack it was planted with). */
    public ItemStack berryStack(int count) {
        var key = GTBerryBushes.itemId(berryId());
        if (key == null) return ItemStack.EMPTY;
//? if >=1.21.1 {
        /*var item = BuiltInRegistries.ITEM.get(key);
*///?} else {
        var item = ForgeRegistries.ITEMS.getValue(key);
//?}
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    public int growth() {
        return growth;
    }

    public int stage() {
        return getBlockState().getValue(BushBlock.STAGE);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
        if (!legacyBerry.isEmpty() && ((BushBlock) getBlockState().getBlock()).berryId().isEmpty()
                && com.gregtech.gregtech.registry.GTBushes.byBerry(legacyBerry) != null) {
            setBerry(legacyBerry);
            return;
        }
        if (!supportChecked || level.getGameTime() % CYCLE_TICKS == 64) {
            supportChecked = true;
            if (!refreshSupport()) return;
        }
        if (level.getGameTime() % CYCLE_TICKS != 0) return;
        grow();
    }

    /**
     * One GT6 growth cycle (the {@code SERVER_TIME % 128 == 0} body). Public so tests can drive it
     * without ticking 128 times.
     *
     * @return the number of growth increments applied
     */
    public int grow() {
        if (level == null) return 0;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BushBlock)) return 0;
        if (!refreshSupport() || berryId().isEmpty()) return 0;
        state = getBlockState();
        int speed = speed();
        if (speed <= 0) return 0;
        if (state.getValue(BushBlock.STAGE) >= 3) return 0;
        int increments;
        var support = BushBlock.isCore(state) ? null : net.minecraft.core.Direction.from3DDataValue(state.getValue(BushBlock.SUPPORT));
        var sample = worldPosition.offset(support == null ? 0 : support.getStepX(),
                support == net.minecraft.core.Direction.UP ? 1 : 2, support == null ? 0 : support.getStepZ());
        if (level.canSeeSky(worldPosition.above())) {
            // GT6: mSpeed increments, plus the same again while it rains on the bush.
            increments = speed + (level.isRainingAt(sample) ? speed : 0);
        } else if (level.getMaxLocalRawBrightness(sample) > LIGHT_GATE) {
            increments = speed;
        } else {
            return 0;
        }
        int stage = state.getValue(BushBlock.STAGE);
        var result = com.gregtech.gregtech.content.plant.BushGrowthRules.advance(growth, stage, increments);
        growth = result.counter();
        stage = result.stage();
        if (stage != state.getValue(BushBlock.STAGE)) {
            level.setBlock(worldPosition, state.setValue(BushBlock.STAGE, stage), 3);
        }
        setChanged();
        return increments;
    }

    /**
     * GT6's {@code mSpeed}: 1 on plantable greens, 0 otherwise. GT6 gives 2 for enchanted aether
     * grass, which the port does not have (documented).
     */
    public int speed() {
        if (level == null) return 0;
        BlockState state = getBlockState();
        if (!BushBlock.isCore(state)) {
            var parent = level.getBlockEntity(worldPosition.relative(net.minecraft.core.Direction.from3DDataValue(state.getValue(BushBlock.SUPPORT))));
            return parent instanceof BushBlockEntity bush && BushBlock.isCore(bush.getBlockState()) ? bush.speed() : 0;
        }
        BlockState ground = level.getBlockState(worldPosition.below());
        return BushBlock.isPlantableGround(ground) ? 1 : 0;
    }

    /** GT6 copies its core's berry and speed, removes snow, and pops unsupported branches. */
    public boolean refreshSupport() {
        if (level == null || level.isClientSide) return true;
        if (level.getBlockState(worldPosition.above()).is(net.minecraft.world.level.block.Blocks.SNOW))
            level.removeBlock(worldPosition.above(), false);
        BlockState state = getBlockState();
        if (BushBlock.isCore(state)) return true;
        BlockPos parentPos = worldPosition.relative(net.minecraft.core.Direction.from3DDataValue(state.getValue(BushBlock.SUPPORT)));
        if (level.getBlockEntity(parentPos) instanceof BushBlockEntity parent) {
            if (!berryId().equals(parent.berryId())) {
                setBerry(parent.berryId());
                return false;
            }
            return true;
        }
        if (!(level.getBlockState(parentPos).getBlock() instanceof BushBlock)) {
            level.destroyBlock(worldPosition, true);
            return false;
        }
        return true;
    }

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        legacyBerry = ((BushBlock) getBlockState().getBlock()).berryId().isEmpty() ? tag.getString("berry") : "";
        growth = tag.getInt("growth");
    }

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        if (((BushBlock) getBlockState().getBlock()).berryId().isEmpty() && !legacyBerry.isEmpty())
            tag.putString("berry", legacyBerry);
        tag.putInt("growth", growth);
    }

    @Override
//? if >=1.21.1 {
    /*public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        return saveWithoutMetadata(lookup);
*///?} else {
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
//?}
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Client-side tint helper: GT6's colour for this bush at its current stage. */
    public int tintColour(int tintIndex) {
        GTBerryBushes.BerryType type = berryId().isEmpty() ? null : GTBerryBushes.byId(berryId());
        if (tintIndex == 0) {
            return type == null ? GTBerryBushes.NO_BERRY_COLOUR : type.bush();
        }
        return GTBerryBushes.stageColour(type, stage());
    }
}
