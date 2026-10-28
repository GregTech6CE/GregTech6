/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * Native adaptation of Behavior_Tool and ITileEntityMultiBlockController.Util. */
package gregtech.engine.multiblock;

import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
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
import java.util.ArrayList;
import java.util.List;

public final class MultiblockTools {
    private MultiblockTools() {}

    public static InteractionResult use(UseOnContext context) {
        var stack = context.getItemInHand();
        boolean builder = GTToolHelper.matchesTool(stack, GTToolType.BUILDER_WAND);
        if (!builder && !GTToolHelper.matchesTool(stack, GTToolType.MAGNIFYING_GLASS)) return InteractionResult.PASS;
        var level = context.getLevel();
        var player = context.getPlayer();
        if (level.isClientSide || player == null || !level.hasChunkAt(context.getClickedPos())) return InteractionResult.PASS;
        var entity = level.getBlockEntity(context.getClickedPos());
        var messages = new ArrayList<Component>();
        long cost = entity instanceof MultiblockToolTarget target
                ? target.useMultiblockTool(context, messages)
                : entity instanceof gregtech.engine.multiblock.tracking.TrackedMultiblock tracked
                ? buildTracked(context, tracked) : 0;
        messages.forEach(message -> player.sendSystemMessage(message));
        if (cost <= 0) return InteractionResult.PASS;
        GTToolHelper.damageForToolClickReturn(stack, cost, player,
                context.getHand() == net.minecraft.world.InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
        level.playSound(null, context.getClickedPos(), builder ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_YES,
                SoundSource.PLAYERS, 1, 1);
        return InteractionResult.SUCCESS;
    }

    /** Controllers with a fixed declarative structure can share the same builder rules. */
    public static long buildTracked(UseOnContext context, gregtech.engine.multiblock.tracking.TrackedMultiblock machine) {
        if (!GTToolHelper.matchesTool(context.getItemInHand(), GTToolType.BUILDER_WAND)
                || !(machine.structureDefinition() instanceof gregtech.engine.multiblock.tracking.StructureDefinition.Declarative definition)) return 0;
        var origin = machine.trackedEntity().getBlockPos();
        var pattern = definition.compile(gregtech.engine.multiblock.tracking.LayoutStructures.facing(machine.trackedEntity()));
        if (pattern == null) return 0;
        for (int i = 0; i < pattern.fixedCount(); i++) {
            if (pattern.air(i)) continue;
            var cell = pattern.cell(i, 0);
            var pos = origin.offset(pattern.dx(i), pattern.dy(i), pattern.dz(i));
            if (pos.equals(origin) || cell.plainBlock()) continue;
            if (build(context, pos, cell.block())) continue;
            for (var alternative : cell.alternatives())
                if (alternative.plain() == null && alternative.part() > 0
                        && build(context, pos, gregtech.content.multiblock.LargeMachineParts.block(alternative.part()))) break;
        }
        gregtech.engine.multiblock.tracking.StructureTracking.invalidate(machine);
        return MultiblockToolRules.BUILDER_COST;
    }

    /** The diagnostic scan behind the magnifier: every cell the definition rejects, not just the first. */
    public static void appendDiagnosis(gregtech.engine.multiblock.tracking.TrackedMultiblock machine,
                                       List<Component> messages) {
        for (var problem : gregtech.engine.multiblock.tracking.StructureDiagnostic.inspect(machine))
            messages.add(Component.literal(problem.describe()));
    }

    /** Original FluidTankGT.content; preserve full long amounts and translatable native names. */
    public static Component tankContent(gregtech.engine.fluid.FluidTankGT tank,String emptyMessage) {
        if(tank.isEmpty())return Component.literal(emptyMessage);
        var fluid=tank.getFluidLong();
        return Component.literal(MultiblockToolRules.amount(tank.getAmount())+" L of ").append(fluid.getDisplayName())
                .append(gregtech.content.registry.GTFluids.isGas(fluid)?" (Gaseous)":" (Liquid)");
    }

    /** Exact structure cell placement; never the ordinary wand's copy-on-top operation. */
    public static boolean build(UseOnContext use, BlockPos pos, Block expected) {
        var level = use.getLevel();
        var player = use.getPlayer();
        var clicked = use.getClickedPos();
        if (level.isClientSide || player == null || !MultiblockToolRules.inBuilderReach(
                pos.getX() - clicked.getX(), pos.getY() - clicked.getY(), pos.getZ() - clicked.getZ())
                || !level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player, pos)) return false;
        var state = level.getBlockState(pos);
        if (!(state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES)
                || state.getBlock() instanceof BushBlock || state.getBlock() instanceof SnowLayerBlock
                || state.getBlock() instanceof BaseFireBlock)) return false;
        ItemStack source = ItemStack.EMPTY;
        if (player.getAbilities().instabuild) source = new ItemStack(expected);
        else for (int i = player.getInventory().getContainerSize() - 1; i >= 0; i--) {
            var candidate = player.getInventory().getItem(i);
            if (!candidate.isEmpty() && candidate.is(expected.asItem())) { source = candidate; break; }
        }
        if (source.isEmpty() || !(source.getItem() instanceof BlockItem item)
                || !player.mayUseItemAt(pos, Direction.UP, source)) return false;
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var context = new BlockPlaceContext(level, player, use.getHand(), source, hit) {
            @Override public BlockPos getClickedPos() { return pos; }
            @Override public boolean canPlace() { return true; }
            @Override public boolean replacingClickedOnBlock() { return true; }
        };
        // BlockItem preserves native item data, placement checks/updates and consumes only after success.
        if (!item.place(context).consumesAction() || !level.getBlockState(pos).is(expected)) return false;
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1, 1);
        return true;
    }
}
