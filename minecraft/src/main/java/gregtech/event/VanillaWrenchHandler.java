package gregtech.event;

import gregtech.content.block.machine.MachineRotationType;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.ToolInteractionSpec;
import gregtech.engine.tool.ToolInteractionTarget;
import gregtech.engine.tool.ToolInteractions;
import gregtech.engine.tool.VanillaWrenchTargets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

//? if >=1.21.1 {
/*import net.minecraft.core.FrontAndTop;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
//?}

/**
 * GT6's wrench and screwdriver clicks on vanilla blocks, which no block of this port sees because
 * the original ran them through {@code ToolCompat}.
 *
 * <p>The click order mirrors {@code ToolCompat.java:298-366}: pillars cycle their axis, the
 * workbench and bookshelf are taken apart, and the twelve blocks of
 * {@link VanillaWrenchTargets} pop when the clicked 3×3 cell is the face they already point at and
 * rotate to the selected cell otherwise. A click that lands on no legal cell is left alone, so the
 * block's own right-click still runs, exactly like the original returning {@code 0}.</p>
 *
 * <p>Comparators and repeaters rotate clockwise under the screwdriver
 * ({@code ToolCompat.java:246-250}); the wrench never touches them.</p>
 */
@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID)
public final class VanillaWrenchHandler {
    private VanillaWrenchHandler() {}

    private enum Action {
        NONE,
        PILLAR,
        POP,
        FACING,
        CRAFTER,
        DIODE
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void click(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockHitResult hit = event.getHitVec();
        if (player == null || hit == null || player.isSpectator()) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (held.isEmpty()) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        // GT blocks run this click in Block#use, which also feeds ToolInteractions.
        if (state.getBlock() instanceof ToolInteractionTarget) {
            return;
        }
        boolean wrench = GTToolHelper.isMachineWrench(held);
        boolean screwdriver = GTToolHelper.isScrewdriver(held);
        if (!wrench && !screwdriver) {
            return;
        }
        if (!player.mayBuild() || !level.mayInteract(player, pos)) {
            return;
        }
        Direction side = ToolInteractions.selectedFace(hit);
        Action action = wrench ? wrenchAction(state, side) : diodeAction(state);
        if (action == Action.NONE) {
            return;
        }
        BlockState next = switch (action) {
            case PILLAR -> state.setValue(BlockStateProperties.AXIS, nextAxis(state.getValue(BlockStateProperties.AXIS)));
            case FACING -> state.setValue(VanillaWrenchTargets.facing(state), side);
//? if >=1.21.1 {
/*            case CRAFTER -> crafterNext(state, side);
*///?} else {
            case CRAFTER -> state;
//?}
            case DIODE -> state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise());
            case POP, NONE -> state;
        };
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide) {
            return;
        }
        if (action == Action.POP) {
            pop(level, pos, state, player);
        } else if (!level.setBlockAndUpdate(pos, next)) {
            return;
        }
        playWrench(level, pos);
        EquipmentSlot slot = event.getHand() == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        GTToolHelper.damageForToolClickReturn(held, action == Action.PILLAR ? 5000L : 10000L, player, slot);
    }

    private static Action wrenchAction(BlockState state, Direction side) {
        if (state.getBlock() instanceof RotatedPillarBlock && state.hasProperty(BlockStateProperties.AXIS)) {
            return Action.PILLAR;
        }
        if (state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.BOOKSHELF)) {
            return Action.POP;
        }
//? if >=1.21.1 {
/*        if (state.getBlock() == Blocks.CRAFTER) {
            return crafterFront(state) == side ? Action.POP : Action.CRAFTER;
        }
*///?}
        MachineRotationType policy = VanillaWrenchTargets.policy(state);
        DirectionProperty facing = VanillaWrenchTargets.facing(state);
        if (policy == null || facing == null || !settled(state)) {
            return Action.NONE;
        }
        if (state.getValue(facing) == side) {
            return Action.POP;
        }
        return ToolInteractionSpec.facing(facing, policy).allows(state, side) ? Action.FACING : Action.NONE;
    }

    private static Action diodeAction(BlockState state) {
        if (!(state.getBlock() instanceof ComparatorBlock) && !(state.getBlock() instanceof RepeaterBlock)) {
            return Action.NONE;
        }
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? Action.DIODE : Action.NONE;
    }

    /**
     * The 1.7.10 branches only touch metadata below 6, so extended pistons and triggered dispensers
     * ignore the wrench. A modern double chest stores its partner in {@code TYPE}; rotating one half
     * would break the pair, and 1.7.10 had no such pairing.
     */
    private static boolean settled(BlockState state) {
        if (state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED)) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.TRIGGERED) && state.getValue(BlockStateProperties.TRIGGERED)) {
            return false;
        }
        return !state.hasProperty(BlockStateProperties.CHEST_TYPE)
                || state.getValue(BlockStateProperties.CHEST_TYPE) == ChestType.SINGLE;
    }

//? if >=1.21.1 {
/*    // The crafter stores a front/top pair, so only the front is re-aimed; the top follows it.
    private static Direction crafterFront(BlockState state) {
        return state.getValue(BlockStateProperties.ORIENTATION).front();
    }

    private static BlockState crafterNext(BlockState state, Direction side) {
        Direction top = side.getAxis().isHorizontal() ? Direction.UP : Direction.NORTH;
        for (FrontAndTop candidate : FrontAndTop.values()) {
            if (candidate.front() == side && candidate.top() == top) {
                return state.setValue(BlockStateProperties.ORIENTATION, candidate);
            }
        }
        return state;
    }
*///?}

    /** GT6 order: {@code (meta + 4) & 15} walks Y → X → Z in the 1.7.10 pillar metadata. */
    private static Direction.Axis nextAxis(Direction.Axis axis) {
        return switch (axis) {
            case Y -> Direction.Axis.X;
            case X -> Direction.Axis.Z;
            case Z -> Direction.Axis.Y;
        };
    }

    /** GT6 {@code ST.give}: the block goes to the inventory, spilling only when it is full. */
    private static void pop(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        ItemStack drop = new ItemStack(state.getBlock().asItem());
        if (blockEntity instanceof Container container) {
            Containers.dropContents(level, pos, container);
        }
        level.removeBlock(pos, false);
        if (!drop.isEmpty() && !player.getInventory().add(drop)) {
            Block.popResource(level, pos, drop);
        }
    }

    private static void playWrench(Level level, BlockPos pos) {
//? if >=1.21.1 {
        /*level.playSound(null, pos, gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
*///?} else {
        level.playSound(null, pos, gregtech.content.registry.GTSounds.WRENCH.get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
//?}
    }
}
