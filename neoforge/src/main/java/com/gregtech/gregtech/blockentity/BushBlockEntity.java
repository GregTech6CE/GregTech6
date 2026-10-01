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
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * GT6's berry bush logic ({@code MultiTileEntityBush}).
 *
 * <p>GT6 grows a bush every 128 ticks by {@code mSpeed} increments, where {@code mSpeed} is 1 on
 * plantable ground, plus the same amount again when it is raining, or — under a roof — when the light
 * level is above 9. A stage advances whenever GT6's byte-sized growth counter overflows, i.e. every
 * 256 increments ({@code if (++mGrowth == 0) mStage++}), which its own comment calls out as a
 * deliberate quirk. The port keeps all of it: 128-tick cycles, one stage per 256 increments, the
 * rain and light bonuses.
 */
public class BushBlockEntity extends BlockEntity {
    /** GT6 checks {@code SERVER_TIME % 128} — the growth cycle length. */
    public static final int CYCLE_TICKS = com.gregtech.gregtech.content.plant.BushGrowthRules.CYCLE_TICKS;
    /** GT6's byte overflow counter: a stage advances every 256 growth increments. */
    public static final int GROWTH_PER_STAGE = com.gregtech.gregtech.content.plant.BushGrowthRules.GROWTH_PER_STAGE;
    /** GT6's light gate when the bush cannot see the sky. */
    public static final int LIGHT_GATE = com.gregtech.gregtech.content.plant.BushGrowthRules.LIGHT_GATE;

    private String berryId = "";
    private int growth;

    public BushBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.BUSH.get(), pos, state);
    }

    public String berryId() { return berryId; }

    public void setBerry(String id) {
        berryId = id == null ? "" : id;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** The berries this bush hands out (GT6 keeps the item stack it was planted with). */
    public ItemStack berryStack(int count) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", berryId));
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    public int growth() { return growth; }

    public int stage() {
        return getBlockState().getValue(BushBlock.STAGE);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;
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
        if (berryId.isEmpty()) return 0;
        int speed = speed();
        if (speed <= 0) return 0;
        if (state.getValue(BushBlock.STAGE) >= 3) return 0;
        int increments;
        if (level.canSeeSky(worldPosition.above())) {
            // GT6: mSpeed increments, plus the same again while it rains on the bush.
            increments = speed + (level.isRainingAt(worldPosition.above()) ? speed : 0);
        } else if (level.getMaxLocalRawBrightness(worldPosition.above()) > LIGHT_GATE) {
            increments = speed;
        } else {
            return 0;
        }
        int stage = state.getValue(BushBlock.STAGE);
        var result=com.gregtech.gregtech.content.plant.BushGrowthRules.advance(growth,stage,increments);
        growth=result.counter();stage=result.stage();
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
        BlockState ground = level.getBlockState(worldPosition.below());
        return BushBlock.isPlantableGround(ground) ? 1 : 0;
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        berryId = tag.getString("berry");
        growth = tag.getInt("growth");
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        if (!berryId.isEmpty()) tag.putString("berry", berryId);
        tag.putInt("growth", growth);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        return saveWithoutMetadata(lookup);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Client-side tint helper: GT6's colour for this bush at its current stage. */
    public int tintColour(int tintIndex) {
        GTBerryBushes.BerryType type = berryId.isEmpty() ? null : GTBerryBushes.byId(berryId);
        if (tintIndex == 0) {
            return type == null ? GTBerryBushes.NO_BERRY_COLOUR : type.bush();
        }
        return GTBerryBushes.stageColour(type, stage());
    }
}
