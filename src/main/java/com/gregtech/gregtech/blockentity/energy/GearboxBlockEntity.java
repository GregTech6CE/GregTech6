package com.gregtech.gregtech.blockentity.energy;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.energy.GearboxSpec;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.block.energy.GearboxBlock;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nullable;
import java.util.Collection;

/** GT6 custom gearbox: six physical gears and an optional X/Y/Z axle. */
public class GearboxBlockEntity extends BlockEntity implements IEnergyBlock {
    /** Low six bits: mounted faces; next two bits: X/Y/Z axle selection. */
    public static final ModelProperty<Integer> VISUAL_CONFIGURATION = new ModelProperty<>();
    /** GT6 mRotationData: bit six means running, low bits select each gear's direction. */
    public static final ModelProperty<Integer> VISUAL_ROTATION = new ModelProperty<>();
    private byte gearMask;
    private byte axisCode; // 0 none, 1 X, 2 Y, 3 Z
    private boolean jammed;
    private long currentSpeed;
    private long currentPower;
    private int rotationMask;
    private int visualRotationMask;
    private long lastVisualInputTick = Long.MIN_VALUE;
    private int inputSides;
    private int outputOrder;
    private long transferredLast;
    private long age;
    private long inputTick = Long.MIN_VALUE;

    public GearboxBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.GEARBOX.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) requestModelDataUpdate();
    }

    private GearboxSpec spec() {
        return ((GearboxBlock) getBlockState().getBlock()).spec();
    }

    private static int bit(Direction face) {
        return 1 << face.ordinal();
    }

    private static byte code(Direction.Axis axis) {
        return (byte) switch (axis) {
            case X -> 1;
            case Y -> 2;
            case Z -> 3;
        };
    }

    private boolean onAxis(Direction face) {
        return axisCode != 0 && axisCode == code(face.getAxis());
    }

    private boolean connected(Direction face) {
        return onAxis(face) || hasGear(face);
    }

    public boolean hasGear(Direction face) {
        return (gearMask & bit(face)) != 0;
    }

    public int gearCount() {
        return Integer.bitCount(gearMask & 63);
    }

    public boolean hasAxis(Direction.Axis axis) {
        return axisCode == code(axis);
    }

    public boolean isJammed() {
        return jammed;
    }

    /** Original GT6 checkGears: opposite pairs need their own axle; triangles jam. */
    public boolean gearsWork() {
        return com.gregtech.gregtech.content.energy.GearboxRotationRules.gearsWork(gearMask,axisCode);
    }

    /** Caller must consume a matching large material gear from inventory first. */
    public void mountGear(Direction face) {
        gearMask |= bit(face);
        configurationChanged();
    }

    public ItemStack unmountGear(Direction face) {
        if (!hasGear(face)) return ItemStack.EMPTY;
        gearMask &= ~bit(face);
        configurationChanged();
        return GTItems.getStack(MaterialPrefix.gearGt, spec().material());
    }

    public void toggleAxis(Direction.Axis axis) {
        byte selected = code(axis);
        axisCode = axisCode == selected ? 0 : selected;
        configurationChanged();
    }

    public void toggleJammed() {
        jammed = !jammed;
        if (jammed) {
            discardRotation();
            visualRotationMask = 0;
        }
        sync();
    }

    private void configurationChanged() {
        jammed = false;
        discardRotation();
        visualRotationMask = 0;
        sync();
    }

    private void discardRotation() {
        currentPower = 0;
        currentSpeed = 0;
        rotationMask = 0;
        inputSides = 0;
        inputTick = Long.MIN_VALUE;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void showRotation(int mask, long tick) {
        lastVisualInputTick = tick;
        if (visualRotationMask != mask) {
            visualRotationMask = mask;
            sync();
        }
    }

    private void expireRotation(long tick) {
        // Keep one tick of visual state, so a continuously supplied gearbox does
        // not flicker when its own ticker runs before the supplying machine.
        if (visualRotationMask != 0 && lastVisualInputTick < tick - 1) {
            visualRotationMask = 0;
            sync();
        }
    }

    public CompoundTag saveForItem() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("gearMask", gearMask);
        tag.putByte("axisCode", axisCode);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        gearMask = (byte) (tag.getByte("gearMask") & 63);
        axisCode = (byte) Math.min(3, tag.getByte("axisCode") & 255);
        jammed = tag.getBoolean("jammed");
        discardRotation();
        visualRotationMask = 0;
        lastVisualInputTick = Long.MIN_VALUE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putByte("gearMask", gearMask);
        tag.putByte("axisCode", axisCode);
        tag.putBoolean("jammed", jammed);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("gearMask", gearMask);
        tag.putByte("axisCode", axisCode);
        tag.putByte("visualRotationMask", (byte) visualRotationMask);
        tag.putBoolean("jammed", jammed);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        gearMask = (byte) (tag.getByte("gearMask") & 63);
        axisCode = (byte) Math.min(3, tag.getByte("axisCode") & 255);
        visualRotationMask = tag.getByte("visualRotationMask") & 127;
        jammed = tag.getBoolean("jammed");
        requestModelDataUpdate();
        if (level != null && level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(VISUAL_CONFIGURATION, ((axisCode & 3) << 6) | (gearMask & 63))
                .with(VISUAL_ROTATION, visualRotationMask)
                .build();
    }

    private boolean freeAxle(Direction face) {
        return onAxis(face) && !hasGear(face) && !hasGear(face.getOpposite());
    }

    private int adjacentGears(Direction side) {
        int result = 0;
        for (Direction face : Direction.values()) {
            if (face != side && face != side.getOpposite() && hasGear(face)) result |= bit(face);
        }
        return result;
    }

    /** Original getRotations: rotation consistency is per-face, not merely input sign. */
    private int rotations(Direction input, boolean negative) {
        return com.gregtech.gregtech.content.energy.GearboxRotationRules.rotations(gearMask,axisCode,input.ordinal(),negative);
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag type, @Nullable Direction side, boolean emitting) {
        return type == GregTechTags.Energy.RU;
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return GregTechTags.Energy.RU.asList();
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return type == GregTechTags.Energy.RU && side != null && connected(side) && (theoretical || !jammed);
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return type == GregTechTags.Energy.RU && side != null && connected(side);
    }

    @Override
    public long doEnergyInjection(GregTechTags.Tag type, @Nullable Direction side,
                                  long size, long amount, boolean doInject) {
        if (type != GregTechTags.Energy.RU || side == null || !connected(side)
                || jammed || size == 0 || amount <= 0) return 0;
        if (!doInject) return amount;
        inputSides |= bit(side);
        if (size == Long.MIN_VALUE || Math.abs(size) > spec().maxSpeed()) {
            if (age >= 10) breakGears();
            return amount;
        }
        if (freeAxle(side)) {
            return EnergyTransfer.emitEnergyToSide(type, side.getOpposite(), size, amount, this);
        }
        if (!gearsWork()) return amount;
        int newRotation = rotations(side, size < 0);
        if (newRotation == 0) return 0;
        long tick = level == null ? 0 : level.getGameTime();
        if (currentPower > 0 && inputTick != tick) return 0;
        if (currentPower > 0 && newRotation != rotationMask) {
            jammed = true;
            discardRotation();
            visualRotationMask = 0;
            sync();
            return amount;
        }
        long accepted = Math.min(amount, Long.MAX_VALUE - currentPower);
        if (accepted <= 0) return 0;
        if (currentPower == 0) {
            currentSpeed = Math.abs(size);
            rotationMask = newRotation;
            inputTick = tick;
        } else {
            currentSpeed = Math.min(currentSpeed, Math.abs(size));
        }
        currentPower += accepted;
        showRotation(newRotation, tick);
        setChanged();
        return accepted;
    }

    private void breakGears() {
        if (level != null && !level.isClientSide) {
            int count = gearCount();
            ItemStack scrap = GTItems.getStack(MaterialPrefix.scrapGt, spec().material(), 9 + level.random.nextInt(27));
            if (count > 0 && !scrap.isEmpty()) Block.popResource(level, worldPosition, scrap);
            ItemStack survivors = GTItems.getStack(MaterialPrefix.gearGt, spec().material(), Math.max(0, count - 1));
            if (count > 1 && !survivors.isEmpty()) Block.popResource(level, worldPosition, survivors);
        }
        gearMask = 0;
        axisCode = 0;
        configurationChanged();
    }

    /** Emit every server tick; output priority rotates and unaccepted packets remain buffered. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, GearboxBlockEntity gearbox) {
        gearbox.age++;
        gearbox.transferredLast = 0;
        gearbox.expireRotation(level.getGameTime());
        if (gearbox.jammed || !gearbox.gearsWork()) {
            gearbox.discardRotation();
            if (gearbox.visualRotationMask != 0) {
                gearbox.visualRotationMask = 0;
                gearbox.sync();
            }
            return;
        }
        if (gearbox.currentPower <= 0 || gearbox.currentSpeed <= 0) {
            gearbox.inputSides = 0;
            return;
        }
        long before = gearbox.currentPower;
        int outputs = 0;
        for (Direction face : Direction.values()) {
            if ((gearbox.inputSides & bit(face)) == 0
                    && (gearbox.hasGear(face) || gearbox.onAxis(face) && gearbox.hasGear(face.getOpposite()))) {
                outputs++;
            }
        }
        if (outputs == 0) {
            gearbox.inputSides = 0;
            return;
        }
        for (int pass = 0; pass < 3 && gearbox.currentPower > 0; pass++) {
            boolean progress = false;
            int remainingOutputs = outputs;
            for (int i = 0; i < 6 && gearbox.currentPower > 0; i++) {
                Direction face = Direction.from3DDataValue((gearbox.outputOrder + i) % 6);
                if ((gearbox.inputSides & bit(face)) != 0) continue;
                boolean geared = gearbox.hasGear(face);
                boolean through = gearbox.onAxis(face) && gearbox.hasGear(face.getOpposite());
                if (!geared && !through) continue;
                long share = Math.max(1, gearbox.currentPower / remainingOutputs--);
                long signedSpeed = geared
                        ? ((gearbox.rotationMask & bit(face)) != 0 ? gearbox.currentSpeed : -gearbox.currentSpeed)
                        : ((gearbox.rotationMask & bit(face.getOpposite())) == 0 ? gearbox.currentSpeed : -gearbox.currentSpeed);
                long used = EnergyTransfer.emitEnergyToSide(GregTechTags.Energy.RU, face,
                        signedSpeed, Math.min(share, gearbox.currentPower), gearbox);
                if (used > 0) {
                    gearbox.currentPower -= Math.min(used, gearbox.currentPower);
                    progress = true;
                }
            }
            if (!progress) break;
        }
        gearbox.outputOrder = (gearbox.outputOrder + 1) % 6;
        gearbox.transferredLast = (before - gearbox.currentPower) * gearbox.currentSpeed;
        if (gearbox.currentPower == 0) gearbox.discardRotation();
        gearbox.inputSides = 0;
        gearbox.setChanged();
    }

    public long transferredLast() {
        return transferredLast;
    }

    @Override
    public long doEnergyExtraction(GregTechTags.Tag type, @Nullable Direction side, long size, long amount, boolean doExtract) { return 0; }
    @Override
    public long getEnergyDemanded(GregTechTags.Tag type, @Nullable Direction side, long size) {
        return isEnergyAcceptingFrom(type, side, false) ? Long.MAX_VALUE - currentPower : 0;
    }
    @Override
    public long getEnergyOffered(GregTechTags.Tag type, @Nullable Direction side, long size) { return 0; }
    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag type, @Nullable Direction side) { return 0; }
    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag type, @Nullable Direction side) { return 0; }
    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag type, @Nullable Direction side) { return spec().maxSpeed() / 2; }
    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag type, @Nullable Direction side) { return spec().maxSpeed() / 2; }
    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag type, @Nullable Direction side) { return spec().maxSpeed(); }
    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag type, @Nullable Direction side) { return spec().maxSpeed(); }
}
