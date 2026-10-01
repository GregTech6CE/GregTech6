package com.gregtech.gregtech.blockentity.tool;

import com.gregtech.gregtech.block.tool.AdvancedButtonBlock;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Persisted GT6 advanced-button configuration and tick-length redstone pulse. */
public final class AdvancedButtonBlockEntity extends BlockEntity {
    private boolean active;
    private boolean inverted;
    private boolean glowInverted;
    private boolean lampMode;
    private int strength = 15;
    private long length;
    private long maxLength = 20;

    public AdvancedButtonBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.ADVANCED_BUTTON.get(), pos, state);
    }

    public int strength() { return strength; }
    public boolean isLampMode() { return lampMode; }
    public boolean isActive() { return active; }
    public boolean isInverted() { return inverted; }
    public long maxLength() { return maxLength; }

    /** GT6 remoteActivate uses the same state transition without a click sound. */
    public boolean press() {
        if (lampMode) return false;
        if (maxLength > 0) {
            length = maxLength;
            active = !inverted;
        } else active = !active;
        refresh(true);
        return true;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (lampMode) {
            updateLampInput();
        } else if (active != inverted && maxLength > 0 && --length < 0) {
            active = inverted;
            refresh(true);
        }
    }

    public void updateLampInput() {
        if (!lampMode || level == null || level.isClientSide) return;
        boolean incoming = level.hasNeighborSignal(worldPosition);
        if (incoming != active) {
            active = incoming;
            refresh(true);
        }
    }

    public void adjustLength(int increment) {
        maxLength = maxLength > Long.MAX_VALUE - increment ? 20 : maxLength + increment;
        refresh(false);
    }

    public void adjustStrength(int increment) {
        strength = Math.floorMod(strength - 1 + increment, 15) + 1;
        refresh(true);
    }

    public void toggleInversion(boolean visual) {
        if (visual) glowInverted = !glowInverted;
        else {
            inverted = !inverted;
            active = !active;
        }
        refresh(true);
    }

    public void cycleMode(boolean sneaking) {
        if (lampMode) lampMode = false;
        else if (maxLength > 0) {
            lampMode = sneaking;
            maxLength = 0;
        } else maxLength = sneaking ? 20 : 1;
        if (lampMode) updateLampInput();
        refresh(true);
    }

    public Component status() {
        Component mode = Component.translatable("message.gregtech.advanced_button.mode."
                + (lampMode ? "lamp" : maxLength > 0 ? "button" : "switch"));
        Component polarity = Component.translatable("message.gregtech.advanced_button.polarity."
                + (inverted ? "inverted" : "normal"));
        return maxLength > 0
                ? Component.translatable("message.gregtech.advanced_button.status_timed",
                        mode, strength, maxLength, polarity)
                : Component.translatable("message.gregtech.advanced_button.status", mode, strength, polarity);
    }

    private void refresh(boolean neighbors) {
        setChanged();
        if (level == null || level.isClientSide) return;
        BlockState oldState = getBlockState();
        if (!(oldState.getBlock() instanceof AdvancedButtonBlock)) return;
        BlockState newState = oldState.setValue(AdvancedButtonBlock.ACTIVE, active)
                .setValue(AdvancedButtonBlock.LIT, active != glowInverted);
        if (oldState != newState) level.setBlock(worldPosition, newState, Block.UPDATE_ALL);
        else level.sendBlockUpdated(worldPosition, oldState, oldState, Block.UPDATE_CLIENTS);
        if (neighbors) {
            level.updateNeighborsAt(worldPosition, oldState.getBlock());
            level.updateNeighborsAt(worldPosition.relative(oldState.getValue(AdvancedButtonBlock.FACING)
                    .getOpposite()), oldState.getBlock());
        }
    }

    public CompoundTag saveItemConfig() {
        CompoundTag config = new CompoundTag();
        config.putBoolean("Inverted", inverted);
        config.putBoolean("GlowInverted", glowInverted);
        config.putBoolean("LampMode", lampMode);
        config.putInt("Strength", strength);
        config.putLong("MaxLength", maxLength);
        return config;
    }

    public void loadItemConfig(CompoundTag config) {
        inverted = config.getBoolean("Inverted");
        glowInverted = config.getBoolean("GlowInverted");
        lampMode = config.getBoolean("LampMode");
        strength = config.contains("Strength") ? Math.max(1, Math.min(15, config.getInt("Strength"))) : 15;
        maxLength = config.contains("MaxLength") ? Math.max(0, config.getLong("MaxLength")) : 20;
        // GT6 item NBT deliberately omits the live active/remaining-tick fields.
        active = false;
        refresh(true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("GT6Button", saveItemConfig());
        tag.putBoolean("Active", active);
        tag.putLong("Length", length);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("GT6Button")) {
            CompoundTag config = tag.getCompound("GT6Button");
            inverted = config.getBoolean("Inverted");
            glowInverted = config.getBoolean("GlowInverted");
            lampMode = config.getBoolean("LampMode");
            strength = config.contains("Strength") ? Math.max(1, Math.min(15, config.getInt("Strength"))) : 15;
            maxLength = config.contains("MaxLength") ? Math.max(0, config.getLong("MaxLength")) : 20;
        }
        active = tag.getBoolean("Active");
        length = tag.getLong("Length");
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }
}
