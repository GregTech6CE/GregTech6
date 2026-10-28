/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Native adaptation of Behavior_Tool and ITileEntityMultiBlockController.Util. */
package gregtech.engine.multiblock;

import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.content.multiblock.LargeMachineParts;
import gregtech.content.multiblock.PlainStructureBlocks;
import gregtech.content.multiblock.SharedLargeMachineLayouts;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class MultiblockTools {
    private MultiblockTools() {}

    public static InteractionResult use(UseOnContext context) {
        var stack = context.getItemInHand();
        boolean builder = GTToolHelper.matchesTool(stack, GTToolType.BUILDER_WAND);
        if (!builder && !GTToolHelper.matchesTool(stack, GTToolType.MAGNIFYING_GLASS)) return InteractionResult.PASS;
        var level = context.getLevel();
        var player = context.getPlayer();
        if (level.isClientSide || player == null || !level.hasChunkAt(context.getClickedPos()))
            return InteractionResult.PASS;
        var entity = level.getBlockEntity(context.getClickedPos());
        var messages = new ArrayList<Component>();
        long cost = entity instanceof MultiblockToolTarget target
                ? target.useMultiblockTool(context, messages)
                : entity instanceof gregtech.engine.multiblock.tracking.TrackedMultiblock tracked
                        ? buildTracked(context, tracked)
                        : 0;
        messages.forEach(message -> player.sendSystemMessage(message));
        if (cost <= 0) return InteractionResult.PASS;
        GTToolHelper.damageForToolClickReturn(
                stack,
                cost,
                player,
                context.getHand() == net.minecraft.world.InteractionHand.OFF_HAND
                        ? EquipmentSlot.OFFHAND
                        : EquipmentSlot.MAINHAND);
        level.playSound(
                null,
                context.getClickedPos(),
                builder ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_YES,
                SoundSource.PLAYERS,
                1,
                1);
        return InteractionResult.SUCCESS;
    }

    /** Controllers with a fixed declarative structure can share the same builder rules. */
    public static long buildTracked(
            UseOnContext context, gregtech.engine.multiblock.tracking.TrackedMultiblock machine) {
        if (!GTToolHelper.matchesTool(context.getItemInHand(), GTToolType.BUILDER_WAND)
                || !(machine.structureDefinition()
                        instanceof gregtech.engine.multiblock.tracking.StructureDefinition.Declarative definition))
            return 0;
        var origin = machine.trackedEntity().getBlockPos();
        var pattern = gregtech.engine.multiblock.tracking.StructurePlan.compile(machine);
        if (pattern == null) return 0;
        boolean wholeStructure = buildsWholeStructure(context, origin);
        for (var cell : gregtech.engine.multiblock.tracking.StructurePlan.minimum(pattern)) {
            var pos = origin.offset(cell.offset());
            if (pos.equals(origin) || !context.getLevel().hasChunkAt(pos)) continue;
            // Preserve every accepted alternative, including replaceable terrain or fluids.
            if (cell.matcher() != null
                    && cell.matcher()
                                    .match(
                                            context.getLevel(),
                                            pos,
                                            context.getLevel().getBlockState(pos))
                            != gregtech.engine.multiblock.tracking.CompiledPattern.FAILED) continue;
            buildCell(context, pos, cell.specification(), wholeStructure);
        }
        gregtech.engine.multiblock.tracking.StructureTracking.invalidate(machine);
        return MultiblockToolRules.BUILDER_COST;
    }

    /**
     * Sneak-right-clicking the controller builds the whole definition in one pass; an ordinary click
     * keeps the original per-click ring around the clicked block.
     */
    public static boolean buildsWholeStructure(UseOnContext use, BlockPos controller) {
        var player = use.getPlayer();
        return player != null && player.isShiftKeyDown() && use.getClickedPos().equals(controller);
    }

    /**
     * Builds one structure cell from the player's inventory: the primary choice first, then its
     * alternatives. Cells that name air, terrain, a fluid or a tag have no single block and are skipped.
     */
    public static boolean buildCell(
            UseOnContext use, BlockPos pos, LargeMachineLayouts.Cell cell, boolean wholeStructure) {
        if (cell == null || cell.role() == MultiblockLayout.Role.AIR) return false;
        Block primary = buildable(cell);
        if (primary != null && build(use, pos, primary, wholeStructure)) return true;
        for (var alternative : cell.alternatives()) {
            Block block = buildable(alternative);
            if (block != null && block != primary && build(use, pos, block, wholeStructure)) return true;
        }
        return false;
    }

    private static Block buildable(LargeMachineLayouts.Cell cell) {
        if (cell.plain() != null) return PlainStructureBlocks.placeable(cell.plain());
        return cell.part() > 0 ? LargeMachineParts.block(cell.part()) : null;
    }

    private static Block buildable(SharedLargeMachineLayouts.Choice choice) {
        if (choice.plain() != null) return PlainStructureBlocks.placeable(choice.plain());
        return choice.part() > 0 ? LargeMachineParts.block(choice.part()) : null;
    }

    /** The diagnostic scan behind the magnifier: every cell the definition rejects, not just the first. */
    public static void appendDiagnosis(
            gregtech.engine.multiblock.tracking.TrackedMultiblock machine, List<Component> messages) {
        for (var problem : gregtech.engine.multiblock.tracking.StructureDiagnostic.inspect(machine))
            messages.add(Component.literal(problem.describe()));
    }

    /** Original FluidTankGT.content; preserve full long amounts and translatable native names. */
    public static Component tankContent(gregtech.engine.fluid.FluidTankGT tank, String emptyMessage) {
        if (tank.isEmpty()) return Component.literal(emptyMessage);
        var fluid = tank.getFluidLong();
        return Component.literal(MultiblockToolRules.amount(tank.getAmount()) + " L of ")
                .append(fluid.getDisplayName())
                .append(gregtech.content.registry.GTFluids.isGas(fluid) ? " (Gaseous)" : " (Liquid)");
    }

    /**
     * Exact structure cell placement; never the ordinary wand's copy-on-top operation.
     * {@code wholeStructure} drops the per-click reach ring for a sneak-click on the controller.
     */
    public static boolean build(UseOnContext use, BlockPos pos, Block expected, boolean wholeStructure) {
        var level = use.getLevel();
        var player = use.getPlayer();
        var clicked = use.getClickedPos();
        if (level.isClientSide
                || player == null
                || (!wholeStructure
                        && !MultiblockToolRules.inBuilderReach(
                                pos.getX() - clicked.getX(), pos.getY() - clicked.getY(), pos.getZ() - clicked.getZ()))
                || !level.isInWorldBounds(pos)
                || !level.hasChunkAt(pos)
                || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player, pos)) return false;
        var state = level.getBlockState(pos);
        if (!(state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || state.getBlock() instanceof BushBlock
                || state.getBlock() instanceof SnowLayerBlock
                || state.getBlock() instanceof BaseFireBlock)) return false;
        ItemStack source = ItemStack.EMPTY;
        if (player.getAbilities().instabuild) source = new ItemStack(expected);
        else
            for (int i = player.getInventory().getContainerSize() - 1; i >= 0; i--) {
                var candidate = player.getInventory().getItem(i);
                if (!candidate.isEmpty() && candidate.is(expected.asItem())) {
                    source = candidate;
                    break;
                }
            }
        if (source.isEmpty()
                || !(source.getItem() instanceof BlockItem item)
                || !player.mayUseItemAt(pos, Direction.UP, source)) return false;
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var context = new BlockPlaceContext(level, player, use.getHand(), source, hit) {
            @Override
            public BlockPos getClickedPos() {
                return pos;
            }

            @Override
            public boolean canPlace() {
                return true;
            }

            @Override
            public boolean replacingClickedOnBlock() {
                return true;
            }
        };
        // BlockItem preserves native item data, placement checks/updates and consumes only after success.
        if (!item.place(context).consumesAction() || !level.getBlockState(pos).is(expected)) return false;
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1, 1);
        return true;
    }
}
