package gregtech.gametest;

import gregtech.api.mod.GregTechIdentity;
import gregtech.api.sensor.TemperatureSource;
import gregtech.api.signal.GtSignalReceiver;
import gregtech.api.signal.GtSignalSource;
import gregtech.api.transport.FluidPipeConnectable;
import gregtech.api.transport.ItemPipeConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
*///?} else {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
//?}

/** GameTest-only probes. They are not in the production mod jar. */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?}
public final class SignalProbes {
    public static Block SOURCE;
    public static Block RECEIVER;
    public static Block TAGGED;
    public static Block FLUID_CONNECTABLE;
    public static Block ITEM_CONNECTABLE;
    public static BlockEntityType<?> TYPE;

    private SignalProbes() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            if (SOURCE != null) return;
            SOURCE = new SignalProbeBlock(true);
            RECEIVER = new SignalProbeBlock(false);
            TAGGED = new Block(BlockBehaviour.Properties.of());
            FLUID_CONNECTABLE = new PipeProbeBlock(true, false);
            ITEM_CONNECTABLE = new PipeProbeBlock(false, true);
            helper.register(id("signal_probe_source"), SOURCE);
            helper.register(id("signal_probe_receiver"), RECEIVER);
            helper.register(id("signal_probe_tagged"), TAGGED);
            helper.register(id("pipe_probe_fluid"), FLUID_CONNECTABLE);
            helper.register(id("pipe_probe_item"), ITEM_CONNECTABLE);
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            if (TYPE != null) return;
            if (SOURCE == null || RECEIVER == null) throw new IllegalStateException("signal probe blocks missing");
            TYPE = BlockEntityType.Builder.of(SignalProbes::create, SOURCE, RECEIVER).build(null);
            helper.register(id("signal_probe"), TYPE);
        });
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", path);
    }

    private static BlockEntity create(BlockPos pos, BlockState state) {
        if (state.is(SOURCE)) return new SignalProbeSourceBlockEntity(pos, state);
        return new SignalProbeReceiverBlockEntity(pos, state);
    }

    static final class SignalProbeBlock extends Block implements EntityBlock {
        private final boolean source;

        SignalProbeBlock(boolean source) {
            super(BlockBehaviour.Properties.of());
            this.source = source;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return source ? new SignalProbeSourceBlockEntity(pos, state) : new SignalProbeReceiverBlockEntity(pos, state);
        }
    }

    /** Opt-in pipe connection with no block entity; one face type per probe. */
    static final class PipeProbeBlock extends Block implements FluidPipeConnectable, ItemPipeConnectable {
        private final boolean fluid;
        private final boolean item;

        PipeProbeBlock(boolean fluid, boolean item) {
            super(BlockBehaviour.Properties.of());
            this.fluid = fluid;
            this.item = item;
        }

        @Override
        public boolean canConnectFluidPipe(Direction side) {
            return fluid;
        }

        @Override
        public boolean canConnectItemPipe(Direction side) {
            return item;
        }
    }

    static final class SignalProbeSourceBlockEntity extends BlockEntity implements GtSignalSource, TemperatureSource {
        long emitted;
        int queriedSide = Integer.MIN_VALUE;
        long temperature;
        long maximum;

        SignalProbeSourceBlockEntity(BlockPos pos, BlockState state) {
            super(TYPE, pos, state);
        }

        @Override
        public long gtSignal(int side) {
            queriedSide = side;
            return emitted;
        }

        @Override
        public long temperatureK(int side) {
            return temperature;
        }

        @Override
        public long maximumK(int side) {
            return maximum;
        }
    }

    static final class SignalProbeReceiverBlockEntity extends BlockEntity implements GtSignalReceiver {
        SignalProbeReceiverBlockEntity(BlockPos pos, BlockState state) {
            super(TYPE, pos, state);
        }
    }
}
