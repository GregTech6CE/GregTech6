package gregtech.content.blockentity.energy;

import gregtech.content.block.energy.ElectricTransformerBlock;
import gregtech.content.data.GregTechTags.Tag;
import gregtech.content.energy.ElectricTransformerControl;
import gregtech.engine.energy.EnergyTransfer;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** The two transformers share step-up and step-down packet limits. */
abstract class TransformerNode extends BufferedEnergyNode {
    TransformerNode(EnergyNodeBlockEntity host) {
        super(host);
    }

    boolean inverted() {
        return host.transformerInverted();
    }

    @Override
    long inputRate() {
        return host.spec().inputRate();
    }

    @Override
    boolean acceptsPacket(Tag type, @Nullable Direction side) {
        return host.isEnergyAcceptingFrom(type, side, false);
    }

    @Override
    long minInput(Tag type, @Nullable Direction side) {
        if (!acceptsInputType(type)) return super.minInput(type, side);
        var spec = host.spec();
        if (!inverted()) return spec.inputRate() <= 16 ? 1 : spec.inputRate() / 2;
        long minimumOutputSize = Math.max(1, spec.outputRate() / 2);
        return minimumOutputSize <= 8 ? 1 : minimumOutputSize;
    }

    @Override
    long maxInput(Tag type, @Nullable Direction side) {
        if (!acceptsInputType(type)) return super.maxInput(type, side);
        var spec = host.spec();
        return inverted() ? Math.max(spec.inputRate(), spec.outputRate() * 8) : spec.inputRate() * 2;
    }

    @Override
    long minOutput(Tag type, @Nullable Direction side) {
        if (!acceptsOutputType(type)) return super.minOutput(type, side);
        var spec = host.spec();
        return inverted() ? spec.inputRate() * 3 / 4 : Math.max(1, spec.outputRate() / 2);
    }

    static final class Electric extends TransformerNode {
        private ElectricTransformerControl electricControl;

        Electric(EnergyNodeBlockEntity host) {
            super(host);
            configure();
        }

        void configure() {
            electricControl = new ElectricTransformerControl(host::setChanged);
        }

        @Override
        ElectricTransformerControl control() {
            return electricControl;
        }

        @Override
        boolean acceptsFrom(@Nullable Direction side, boolean theoretical) {
            if (!theoretical && !electricControl.accepts()) return false;
            return side == null || (inverted() ? side != host.facing() : side == host.facing());
        }

        @Override
        boolean emitsTo(Tag type, @Nullable Direction side, boolean theoretical) {
            return side == null || (inverted() ? side == host.facing() : side != host.facing());
        }

        @Override
        boolean overloads(Tag type, @Nullable Direction side, long magnitude) {
            return magnitude > host.getEnergySizeInputMax(type, side);
        }

        @Override
        void tickCoversBeforeDevice(Level level) {}

        @Override
        void emitOutput() {
            host.panels().beforeTick();
            var spec = host.spec();
            long offeredPackets = inverted() ? 1 : spec.inputRate() / spec.outputRate();
            long packetSize = inverted() ? host.bufferedInput : host.bufferedInput / offeredPackets;
            long maxPacketSize = host.getEnergySizeOutputMax(spec.outType(), null);
            if (electricControl.mode() > 0) {
                packetSize = Math.min(packetSize, electricControl.limit(maxPacketSize));
            }

            boolean canEmit =
                    packetSize >= host.getEnergySizeOutputMin(host.spec().outType(), null);
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
    }

    static final class Rotation extends TransformerNode {
        boolean negativeInput;

        Rotation(EnergyNodeBlockEntity host) {
            super(host);
        }

        private Direction outputFace() {
            return inverted() ? host.facing() : host.facing().getOpposite();
        }

        @Override
        boolean acceptsFrom(@Nullable Direction side, boolean theoretical) {
            return side == null || side == outputFace().getOpposite();
        }

        @Override
        boolean emitsTo(Tag type, @Nullable Direction side, boolean theoretical) {
            return side == null || side == outputFace();
        }

        @Override
        void recordInput(long size) {
            negativeInput = size < 0;
        }

        @Override
        void emitOutput() {
            var spec = host.spec();
            long packetSize =
                    inverted() ? host.bufferedInput : host.bufferedInput * spec.outputRate() / spec.inputRate();
            long minPacketSize = host.getEnergySizeOutputMin(spec.outType(), outputFace());
            long maxPacketSize = host.getEnergySizeOutputMax(host.spec().outType(), outputFace());
            if (packetSize >= minPacketSize && packetSize <= maxPacketSize) {
                long acceptedPackets = EnergyTransfer.emitEnergyToSide(
                        host.spec().outType(),
                        outputFace(),
                        negativeInput ? -packetSize : packetSize,
                        inverted() ? 1 : 4,
                        host);
                host.conversionMeter().output(host.getLevel().getGameTime(), packetSize, acceptedPackets);
            }
            // The rotation transformer wastes up to one input maximum even with no receiver.
            if (host.bufferedInput > 0) {
                host.bufferedInput =
                        Math.max(0, host.bufferedInput - maxInput(host.spec().inType(), null));
                host.setChanged();
            }
        }
    }
}
