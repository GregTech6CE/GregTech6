package gregtech.content.blockentity.energy;

import gregtech.content.block.energy.ElectricTransformerBlock;
import gregtech.content.energy.ElectricTransformerControl;
import gregtech.engine.energy.EnergyTransfer;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

/** Step-up/step-down ratios, directional ports and emission for electric and rotation transformers. */
final class TransformerNode {
    private final EnergyNodeBlockEntity host;
    private boolean inverted;
    private boolean negativeRotationInput;
    private ElectricTransformerControl electricControl;

    TransformerNode(EnergyNodeBlockEntity host) {
        this.host = host;
    }

    void configureElectric() {
        electricControl = new ElectricTransformerControl(host::setChanged);
    }

    ElectricTransformerControl electricControl() {
        return electricControl;
    }

    boolean inverted() {
        return inverted;
    }

    boolean toggleInverted() {
        if (host.isRotationTransformer() || host.isElectricTransformer()) {
            host.bufferedInput = 0;
            negativeRotationInput = false;
        }
        inverted = !inverted;
        if (host.isElectricTransformer()) electricControl.select(inverted);
        host.setChanged();
        return inverted;
    }

    long effectiveInputRate() {
        return inverted ? host.spec().outputRate() : host.spec().inputRate();
    }

    long effectiveOutputRate() {
        return inverted ? host.spec().inputRate() : host.spec().outputRate();
    }

    boolean acceptsFrom(@Nullable Direction side, boolean theoretical) {
        if (host.isElectricTransformer()) {
            if (!theoretical && !electricControl.accepts()) return false;
            return side == null || (inverted ? side != host.facing() : side == host.facing());
        }
        return side == null || side == (inverted ? host.facing().getOpposite() : host.facing());
    }

    boolean emitsTo(@Nullable Direction side) {
        if (host.isElectricTransformer())
            return side == null || (inverted ? side == host.facing() : side != host.facing());
        return side == null || side == rotationOutputFace();
    }

    private Direction rotationOutputFace() {
        return inverted ? host.facing() : host.facing().getOpposite();
    }

    void recordRotationInput(long packetSize) {
        negativeRotationInput = packetSize < 0;
    }

    long minimumInput() {
        var spec = host.spec();
        if (!inverted) return spec.inputRate() <= 16 ? 1 : spec.inputRate() / 2;
        long minimumOutputSize = Math.max(1, spec.outputRate() / 2);
        return minimumOutputSize <= 8 ? 1 : minimumOutputSize;
    }

    long maximumInput() {
        var spec = host.spec();
        return inverted ? Math.max(spec.inputRate(), spec.outputRate() * 8) : spec.inputRate() * 2;
    }

    long minimumOutput() {
        var spec = host.spec();
        return inverted ? spec.inputRate() * 3 / 4 : Math.max(1, spec.outputRate() / 2);
    }

    void emitElectric() {
        host.panels().beforeTick();
        var spec = host.spec();
        long offeredPackets = inverted ? 1 : spec.inputRate() / spec.outputRate();
        long packetSize = inverted ? host.bufferedInput : host.bufferedInput / offeredPackets;
        long maxPacketSize = host.getEnergySizeOutputMax(spec.outType(), null);
        if (electricControl.mode() > 0) {
            packetSize = Math.min(packetSize, electricControl.limit(maxPacketSize));
        }

        boolean canEmit = packetSize >= host.getEnergySizeOutputMin(host.spec().outType(), null);
        boolean emitted = false;
        if (canEmit) {
            if (packetSize > maxPacketSize) {
                host.handleInputOverload(packetSize);
            } else {
                long receivedPackets =
                        EnergyTransfer.emitEnergyToNetwork(host.spec().outType(), packetSize, offeredPackets, host);
                long acceptedPackets = Math.min(offeredPackets, receivedPackets);
                host.conversionMeter().output(host.getLevel().getGameTime(), packetSize, acceptedPackets);
                if (receivedPackets > 0) {
                    host.bufferedInput -= acceptedPackets * packetSize;
                    emitted = true;
                }
            }
        }

        electricControl.tick(canEmit, emitted);
        host.coolDownOverload(canEmit);
        var activity = ElectricTransformerBlock.ACTIVITY;
        if (host.getBlockState().hasProperty(activity)
                && host.getBlockState().getValue(activity) != electricControl.visual()) {
            host.getLevel()
                    .setBlock(
                            host.getBlockPos(),
                            host.getBlockState().setValue(activity, electricControl.visual()),
                            Block.UPDATE_ALL);
        }
        host.panels().afterTick();
    }

    void emitRotation() {
        var spec = host.spec();
        long packetSize = inverted ? host.bufferedInput : host.bufferedInput * spec.outputRate() / spec.inputRate();
        long minPacketSize = host.getEnergySizeOutputMin(spec.outType(), rotationOutputFace());
        long maxPacketSize = host.getEnergySizeOutputMax(host.spec().outType(), rotationOutputFace());
        if (packetSize >= minPacketSize && packetSize <= maxPacketSize) {
            long acceptedPackets = EnergyTransfer.emitEnergyToSide(
                    host.spec().outType(),
                    rotationOutputFace(),
                    negativeRotationInput ? -packetSize : packetSize,
                    inverted ? 1 : 4,
                    host);
            host.conversionMeter().output(host.getLevel().getGameTime(), packetSize, acceptedPackets);
        }
        // The rotation transformer wastes up to one input maximum even with no receiver.
        if (host.bufferedInput > 0) {
            host.bufferedInput = Math.max(0, host.bufferedInput - maximumInput());
            host.setChanged();
        }
    }

    void save(CompoundTag tag) {
        if (host.isElectricTransformer()) electricControl.save(tag);
        tag.putBoolean("gt.inverted", inverted);
        if (host.isRotationTransformer()) tag.putBoolean("gt.rotation_negative_input", negativeRotationInput);
    }

    void loadDirection(CompoundTag tag) {
        inverted = tag.getBoolean("gt.inverted");
        if (host.isRotationTransformer()) negativeRotationInput = tag.getBoolean("gt.rotation_negative_input");
    }
}
