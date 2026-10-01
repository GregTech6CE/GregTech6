package com.gregtech.gregtech.block.energy;

import com.gregtech.gregtech.api.energy.GearboxSpec;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.blockentity.energy.GearboxBlockEntity;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.util.GTPlacementCode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** GT6 custom gearbox: wrench mounts physical gears; monkey wrench selects axle axis. */
public class GearboxBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private final GearboxSpec spec;

    public GearboxBlock(GearboxSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public GearboxSpec spec() { return spec; }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return spec.id().equals("gearbox_wood") ? 150 : super.getFlammability(state, level, pos, face);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return spec.id().equals("gearbox_wood") ? 150 : super.getFireSpreadSpeed(state, level, pos, face);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GearboxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GTBlockEntities.GEARBOX.get()) return null;
        return (l, pos, s, be) -> GearboxBlockEntity.serverTick(l, pos, s, (GearboxBlockEntity) be);
    }

    /** As in GT6 writeItemNBT2, a dismantled gearbox keeps its mounted gears. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack stack = new ItemStack(this);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof GearboxBlockEntity gearbox) {
            stack.addTagElement("BlockEntityTag", gearbox.saveForItem());
        }
        return List.of(stack);
    }

    private static Direction wrenchSide(BlockPos pos, BlockHitResult hit) {
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
        return GTPlacementCode.getSideWrenching(hit.getDirection(),
                (float) local.x, (float) local.y, (float) local.z);
    }

    private static int matchingGearSlot(Player player, GearboxSpec spec) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (MaterialItem.isMaterialItem(candidate, MaterialPrefix.gearGt, spec.material())) return i;
        }
        return -1;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        boolean monkey = GTToolHelper.isMonkeyWrench(held);
        boolean wrench = GTToolHelper.isMachineWrench(held) && !monkey;
        boolean hammer = GTToolHelper.isSoftHammer(held);
        boolean glass = GTToolHelper.isMagnifyingGlass(held);
        if (!monkey && !wrench && !hammer && !glass) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof GearboxBlockEntity gearbox)) return InteractionResult.PASS;

        Direction side = wrenchSide(pos, hit);
        if (wrench) {
            if (gearbox.hasGear(side)) {
                ItemStack gear = gearbox.unmountGear(side);
                if (!gear.isEmpty() && !player.getInventory().add(gear)) Block.popResource(level, pos, gear);
            } else {
                ItemStack gear = GTItems.getStack(MaterialPrefix.gearGt, spec.material());
                if (gear.isEmpty()) return InteractionResult.PASS;
                if (!player.getAbilities().instabuild) {
                    int slot = matchingGearSlot(player, spec);
                    if (slot < 0) {
                        player.sendSystemMessage(Component.literal("A matching large gear is required."));
                        return InteractionResult.CONSUME;
                    }
                    player.getInventory().removeItem(slot, 1);
                }
                gearbox.mountGear(side);
            }
            GTToolHelper.damageForToolClickReturn(held, 10000, player);
            return InteractionResult.CONSUME;
        }
        if (monkey) {
            gearbox.toggleAxis(side.getAxis());
            if (gearbox.hasAxis(side.getAxis())) level.setBlockAndUpdate(pos, state.setValue(FACING, side));
            GTToolHelper.damageForToolClickReturn(held, 10000, player);
            return InteractionResult.CONSUME;
        }
        if (hammer) {
            gearbox.toggleJammed();
            GTToolHelper.damageForToolClickReturn(held, 10000, player);
            return InteractionResult.CONSUME;
        }
        player.sendSystemMessage(Component.literal(gearbox.gearsWork()
                ? gearbox.isJammed() ? "Gears interlocked, but stopped." : "Gears interlocked properly."
                : "Gears interlocked improperly."));
        GTToolHelper.damageForToolClickReturn(held, 1, player);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gt.tooltip.gearbox.speed", spec.maxSpeed()));
        tooltip.add(Component.translatable("gt.tooltip.gearbox.use"));
    }
}
