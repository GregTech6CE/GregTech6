package com.gregtech.gregtech.block.tool;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.blockentity.tool.BumbliaryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * GT6's bumbliary ({@code MultiTileEntityBumbliary}, GT6 id 32741) and its advanced variant
 * ({@code MultiTileEntityBumbliaryAdvanced}, GT6 id 32007, {@code Loader_MultiTileEntities:2221-2222}):
 * the block around {@link BumbliaryBlockEntity}.
 *
 * <p>GT6 gives both machines one block each with a full-size icon per face; the port keeps the
 * placeholder's own model for the standard machine and ships GT6's {@code bumbliary_adv} art for the
 * advanced one. Both blocks extend {@link ShapedToolBlock} for GT6's cube shape, its wrench rotation
 * and the block state the blockstate files describe.</p>
 *
 * <h2>Interaction ({@code onBlockActivated3:282-296})</h2>
 * <p>Only the <b>top</b> face opens the machine ({@code SIDES_TOP[aSide]}); every other side falls
 * through to the shared tool interactions, which keeps the wrench rotation working. GT6 also charges
 * five minutes of breeding countdown for the disturbance ({@code :289},
 * {@code BumbliaryBlockEntity#onOpened}) and lets a living queen sting the player once. A creative player opens GT6's scoop GUI without
 * the penalty ({@code :285-288}). A usable scoop opens that same alternate mode and
 * takes GT6's 10000-unit tool-click cost. The mode is synced as menu data; a live queen
 * remains locked by the common inventory predicate in both modes.</p>
 *
 * <p>The quicksilver thermometer reads temperature and humidity on any face without opening
 * the nest, delaying breeding, or provoking its queen (GT6 onToolClick2:299-315).</p>
 */
public class BumbliaryBlock extends ShapedToolBlock implements EntityBlock {

    private final boolean advanced;

    /**
     * @param id the registered block id ({@code bumbliary} or {@code advanced_bumbliary}); the shape,
     *           the tint and the wrench rotation always come from GT6's own {@code bumbliary} entry
     * @param advanced whether this is GT6's advanced machine ({@code MultiTileEntityBumbliaryAdvanced})
     */
    public BumbliaryBlock(String id, boolean advanced, Properties properties) {
        super("bumbliary", properties);
        this.advanced = advanced;
    }

    /** GT6's advanced bumbliary (id 32007) with its 20 slots and its 3x3x3 tooltip range. */
    public boolean advanced() { return advanced; }

    /**
     * GT6 registers the standard bumbliary with {@code ANY.Wood} and the advanced one with
     * {@code MT.StainlessSteel} ({@code Loader_MultiTileEntities:2221-2222}), so the shared art is
     * tinted with the machine's material.
     */
    @Override
    public int tintRgb() {
        return advanced ? com.gregtech.gregtech.content.material.Materials.StainlessSteel.getColor()
                : super.tintRgb();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BumbliaryBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> {
            if (be instanceof BumbliaryBlockEntity machine) machine.serverTick();
        };
    }

    @Override
    protected InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // GT6 :283 `SIDES_TOP[aSide]`: the bees are only reachable from above.
        var heldId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getItemInHand(hand).getItem());
        if (net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", "quicksilver_thermometer").equals(heldId)
                && level.getBlockEntity(pos) instanceof BumbliaryBlockEntity machine) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable(
                    "message.gregtech.bumbliary.climate", machine.temperature(), Float.toString(machine.humidity())), false);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hit.getDirection() == Direction.UP
                && level.getBlockEntity(pos) instanceof BumbliaryBlockEntity machine) {
            ItemStack held = player.getItemInHand(hand);
            boolean scoop = GTToolHelper.matchesTool(held, com.gregtech.gregtech.api.tool.GTToolType.SCOOP);
            if (player.isCreative() || scoop || held.isEmpty() || !GTToolHelper.isTool(held)) {
                if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                    // GT6 :289-290: disturbance delays breeding and the queen defends the nest.
                    if (!player.isCreative()) {
                        machine.onOpened();
                        machine.attackEntity(player);
                    }
                    boolean scoopMode = scoop || player.isCreative();
                    serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, user) -> new com.gregtech.gregtech.client.gui.BumbliaryContainerMenu(
                                    id, inventory, machine, scoopMode), machine.getDisplayName()));
                    if (scoop) GTToolHelper.damageForToolClickReturn(held, 10000, player,
                            hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                                    : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.interact(state, level, pos, player, hand, hit);
    }

    /**
     * GT6's {@code breakDrop:346} and {@code breakBlock} ({@code TileEntityBase05Inventories:152-171}):
     * the machine hands its whole inventory out when it disappears - a player's break, a piston or an
     * explosion alike - and a running machine ({@code mLife > 0}) kills every bee on the way out, so a
     * broken bumbliary never gives a live queen back.
     *
     * <p>Both paths call {@code dropContents()}, which empties the inventory: a player's break hits
     * {@link #playerWillDestroy} first and {@link #onRemove} then finds nothing left, so nothing can
     * be duplicated. {@code onRemove} is the one that covers pistons and explosions, exactly like the
     * other port containers.</p>
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BumbliaryBlockEntity machine) {
            machine.dropContents();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof BumbliaryBlockEntity machine) {
            machine.dropContents();
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /**
     * GT6's three tooltips ({@code addToolTips:87-92}): the machine's range, the scoop hint and the
     * thermometer hint. The port reuses its own thermometer key and adds the two missing ones.
     */
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gregtech.machine.range", advanced ? "3x3x3" : "7x7x7"));
        tooltip.add(Component.translatable("tooltip.gregtech.machine.tool.scoop"));
        tooltip.add(Component.translatable("tooltip.gregtech.crucible.thermometer"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
