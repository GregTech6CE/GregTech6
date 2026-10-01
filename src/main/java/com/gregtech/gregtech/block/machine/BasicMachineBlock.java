package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** GT6 basic machine block with wrench rotation (3x3 grid face selection) and right-click GUI. */
public class BasicMachineBlock extends GTFacingMachineBlock implements EntityBlock {
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    private final BasicMachineSpec basicSpec;
    private Supplier<BlockEntityType<?>> beTypeSupplier = () -> null;

    public BasicMachineBlock(BasicMachineSpec basicSpec, Properties properties) {
        super(new MachineSpec(basicSpec.id(), basicSpec.material().getLocalName(), basicSpec.material().getColor(),
                0, 0, basicSpec.textureFolder(), basicSpec.hardness(), basicSpec.blastResistance()),
                properties);
        this.basicSpec = basicSpec;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
                .setValue(LIT, false).setValue(RUNNING, false));
    }

    public BasicMachineSpec basicSpec() { return basicSpec; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, RUNNING);
    }

    public void setBeTypeSupplier(Supplier<BlockEntityType<?>> supplier) {
        this.beTypeSupplier = supplier;
    }

    @Nullable
    public BlockEntityType<?> beType() {
        return beTypeSupplier.get();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        BlockEntityType<?> ourType = beTypeSupplier.get();
        if (type != ourType) return null;
        @SuppressWarnings("unchecked")
        BlockEntityTicker<T> ticker = (lvl, pos, st, be) ->
                BasicMachineBlockEntity.serverTick(lvl, pos, st, (BasicMachineBlockEntity) be);
        return ticker;
    }

    /** Factory hook so subclasses (multiblock controllers) can supply their own BE. */
    public BasicMachineBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        if(basicSpec.machineName().equals("massfab"))return new com.gregtech.gregtech.blockentity.machine.CompactMatterFabricatorBlockEntity(type,pos,state);
        if(basicSpec.machineName().equals("largemassfab"))return new com.gregtech.gregtech.blockentity.machine.MatterFabricatorBlockEntity(type,pos,state);
        if(com.gregtech.gregtech.content.multiblock.LargeMachineLayouts.cells(basicSpec.machineName())!=null) return new com.gregtech.gregtech.blockentity.machine.LargeRecipeMachineBlockEntity(type,pos,state);
        return new BasicMachineBlockEntity(type, pos, state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BlockEntityType<?> type = beTypeSupplier.get();
        BasicMachineBlockEntity be = createBlockEntity(type, pos, state);
        be.setSpec(basicSpec);
        return be;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // Wrench rotation first (from GTFacingMachineBlock)
        InteractionResult wrenchResult = super.use(state, level, pos, player, hand, hit);
        if (wrenchResult != InteractionResult.PASS) {
            return wrenchResult;
        }

        ItemStack held = player.getItemInHand(hand);
        BlockEntity be = level.getBlockEntity(pos);

        if(be instanceof BasicMachineBlockEntity panelHost){
            var result=com.gregtech.gregtech.content.cover.PanelCoverInteraction.use(panelHost,player,hand,hit,false);
            if(result!=InteractionResult.PASS)return result;
        }
        // Covers: attach a cover item to the clicked face; crowbar removes it.
        if (be instanceof BasicMachineBlockEntity coverBe) {
            if (isCoverItem(held)) {
                if (!level.isClientSide && coverBe.attachCover(hit.getDirection(), held)) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (GTToolHelper.matchesTool(held, com.gregtech.gregtech.api.tool.GTToolType.CROWBAR)) {
                if (!level.isClientSide) {
                    ItemStack removed = coverBe.removeCover(hit.getDirection());
                    if (!removed.isEmpty() && !player.addItem(removed)) {
                        player.drop(removed, false);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        // §108: the utility covers of CoverUtilityBehaviors. GT6 CoverCrafting:49-54 opens a workbench
        // on a bare-handed click of the cover's face; the item/fluid filters and the item retriever
        // take the held stack as their filter (CoverFilterItem:115-127, CoverFilterFluid:117-129,
        // CoverRetrieverItem:123-136), the screwdriver flips whitelist/blacklist and the soft hammer
        // clears it (:58-70, CoverRetrieverItem:94-102). Nothing here fires when no such cover is on
        // the clicked face - the checks are all keyed on the face's cover id.
        if (be instanceof BasicMachineBlockEntity utilityBe) {
            Direction side = hit.getDirection();
            String coverId = com.gregtech.gregtech.content.cover.CoverItems.behavior(utilityBe.getCover(side));
            if (held.isEmpty()
                    && com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.CRAFTING_TABLE.equals(coverId)) {
                if (!level.isClientSide) {
                    com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.clickCraftingCover(
                            player, (net.minecraft.server.level.ServerLevel) level, pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!held.isEmpty()) {
                if (GTToolHelper.matchesTool(held, com.gregtech.gregtech.api.tool.GTToolType.SCREWDRIVER)
                        && utilityBe.configureFilterCover(side, true, false)) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                if (GTToolHelper.matchesTool(held, com.gregtech.gregtech.api.tool.GTToolType.SOFT_HAMMER)
                        && utilityBe.configureFilterCover(side, false, true)) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                if (!isCoverItem(held) && utilityBe.clickFilterCover(side, held)) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }

        // Fluid injection from held fluid container (GT6: right-click machine with fluid cell)
        if (!held.isEmpty() && be instanceof BasicMachineBlockEntity basicBe) {
            IFluidHandlerItem fluidItem = held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                    .resolve().orElse(null);
            if (fluidItem != null && basicBe.getTanksInput() != null && basicBe.getTanksInput().length > 0) {
                if (!level.isClientSide) {
                    FluidStack available = fluidItem.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                    if (!available.isEmpty()) {
                        int accepted = basicBe.fill(available, IFluidHandler.FluidAction.SIMULATE);
                        if (accepted > 0) {
                            FluidStack drained = fluidItem.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
                            if (!drained.isEmpty()) {
                                basicBe.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                                player.setItemInHand(hand, fluidItem.getContainer());
                            }
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return useRemainder(state, level, pos, player, hand, be);
    }

    /**
     * Cover items attachable to machine faces.
     *
     * <p>Delegated to {@link com.gregtech.gregtech.content.cover.CoverItems#isCover}: GT6 gives the
     * cover role to the vanilla redstone torch/repeater ({@code GT_API:799-802}) and to the compact
     * electric pump/conveyor/robot arm ({@code MultiItemTechnological:50-53}), so neither is a
     * {@code TechItem} in the port.</p>
     */
    private static boolean isCoverItem(ItemStack stack) {
        return com.gregtech.gregtech.content.cover.CoverItems.isCover(stack);
    }

    // ── Redstone emission from cover_redstone_torch / cover_redstone_repeater ──

    /**
     * §108: GT6's asphalt cover boosts whoever walks over it ({@code CoverAsphalt:38-41}: the entity's
     * horizontal motion is multiplied while it moves, is on the ground, is not sneaking and is not in
     * water). It is bound to the Asphalt Panel block ({@code Loader_MultiTileEntities:2054}), so the
     * face that matters is the one being walked on - the machine's top.
     */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BasicMachineBlockEntity be) {
            String top = com.gregtech.gregtech.content.cover.CoverItems.behavior(be.getCover(Direction.UP));
            if (com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.ASPHALT_PANEL.equals(top)) {
                com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.walkOverAsphalt(entity);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BasicMachineBlockEntity bmbe) {
            return bmbe.getRedstoneSignal(side.getOpposite());
        }
        return 0;
    }
    @Override public int getDirectSignal(BlockState state,BlockGetter level,BlockPos pos,Direction side){
        return level.getBlockEntity(pos) instanceof BasicMachineBlockEntity machine?machine.getStrongRedstoneSignal(side.getOpposite()):0;
    }

    private InteractionResult useRemainder(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockEntity be) {
        // Right-click with empty hand or non-tool opens GUI
        if (!level.isClientSide && !GTToolHelper.isTool(player.getItemInHand(hand))) {
            if (be instanceof BasicMachineBlockEntity basicBe && player instanceof ServerPlayer sp) {
                NetworkHooks.openScreen(sp, basicBe, buf -> {
                    buf.writeUtf(basicSpec.machineName());
                    // Write fluid tank contents for initial client sync
                    int inputCount = basicBe.getTanks();
                    int fluidSlotBase = basicBe.inputSlots() + basicBe.outputSlots();
                    for (int i = 0; i < inputCount; i++) {
                        FluidStack fs = basicBe.getFluidInTank(i);
                        buf.writeFluidStack(fs);
                    }
                });
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof com.gregtech.gregtech.api.inventory.BlockContents contents) {
            contents.dropContents();
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, next, moving);
    }
    @Override protected ItemStack createMachineDrop(BlockState state, BlockEntity entity) {
        var stack=createMachineDrop(state);
        if(entity instanceof BasicMachineBlockEntity machine) {
            var data=machine.saveWithoutMetadata();
            data.remove("gt.inventory");
            for(int i=0;i<6;i++) data.remove("gt_cover_"+i);
            stack.getOrCreateTag().put("BlockEntityTag",data);
        }
        return stack;
    }
    @Override public java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        return java.util.List.of(createMachineDrop(state,builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)));
    }
}
