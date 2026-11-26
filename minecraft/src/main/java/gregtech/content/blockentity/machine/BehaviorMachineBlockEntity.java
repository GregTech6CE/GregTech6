package gregtech.content.blockentity.machine;

import gregtech.engine.addon.AddonMachines;
import gregtech.engine.addon.BehaviorEnergyRules;
import gregtech.api.energy.EnergyTransfer;
import gregtech.api.energy.EnergyType;
import gregtech.api.energy.EnergyOutput;
import gregtech.api.addon.machine.BehaviorMachine;
import gregtech.api.addon.recipe.CrucibleContent;
import gregtech.api.addon.recipe.CrucibleMold;
import gregtech.api.machine.BlockFace;
import gregtech.api.machine.MachineFace;
import gregtech.engine.addon.PartEnergyRules;
import gregtech.engine.addon.BehaviorMachineHosts;
import gregtech.api.addon.platform.BehaviorMachineClientView;
import gregtech.api.addon.platform.EnergyAccess;
import gregtech.api.addon.platform.BehaviorState;
import gregtech.api.addon.platform.EnergyBlock;
import gregtech.api.addon.platform.PartBehavior;
import gregtech.api.addon.platform.PartContext;
import gregtech.api.addon.platform.MachineBehavior;
import gregtech.engine.multiblock.MultiblockLayout;
import gregtech.engine.multiblock.tracking.PatternMatch;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.engine.energy.EnergyBlockViews;
import gregtech.engine.energy.FaceConfig;
import gregtech.engine.energy.IEnergyBlock;
import gregtech.engine.machine.BasicMachineEnergy;
import gregtech.engine.machine.ITileEntityCrucible;
import gregtech.engine.machine.ITileEntityMold;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.block.machine.BehaviorMachineBlock;
import gregtech.content.blockentity.GTBlockEntity;
import gregtech.content.blockentity.machine.BehaviorMachineTanks.Tank;
import gregtech.engine.inventory.menu.BehaviorMachineMenu;
import gregtech.content.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
//?}

//? if >=1.21.1 {
/**///?} else {
import javax.annotation.Nonnull;
//?}
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Shell block entity. The addon behavior never sees this class. */
public class BehaviorMachineBlockEntity extends GTBlockEntity implements EnergyBlock, MenuProvider,
        gregtech.engine.fluid.CompressedGasReceiver, ITileEntityCrucible,
        gregtech.engine.multiblock.MultiblockPortOwner, gregtech.api.signal.GtSignalSource,
        BehaviorMachineClientView, gregtech.engine.multiblock.tracking.TrackedMultiblock {
    private final BehaviorMachine declaration;
    private final AddonMachines.AcceptedBehavior accepted;
    private final MachineBehavior behavior;
    private final ItemStackHandler items;
    private final Tank[] tanks;
    private final BehaviorMachineState state;
    private final BehaviorMachineContext context;
    private final BehaviorMachinePanel panel;
    private final BehaviorMachineWorld worldAccess;
    private final BehaviorMachineTanks fluidAccess;
    private final BehaviorMachineIOPorts ioPorts;
    private final GregTechTags.Tag energyTag;
    private final GregTechTags.Tag energyOutTag;
    private final GregTechTags.Tag auxiliaryTag;
    private long stored;
    private long auxiliaryStored;
    /** Energy type locked by the first accepted packet when the main input is dynamic. */
    private String dynamicEnergy;
    private long capacity;
    private boolean capacitySet;
    private boolean powered;
    private final BehaviorMachineStructure structure = new BehaviorMachineStructure(this);

    public BehaviorMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        BehaviorMachineBlock block = (BehaviorMachineBlock) state.getBlock();
        this.accepted = block.accepted();
        this.declaration = accepted.declaration();
        this.behavior = BehaviorMachineHosts.create(accepted.addonId(), declaration.casing().machineType());
        this.items = new ItemStackHandler(declaration.items().size()) {
            @Override protected void onContentsChanged(int slot) { setChanged(); }
        };
        this.fluidAccess = new BehaviorMachineTanks(declaration.fluids(), this::setChanged);
        this.tanks = fluidAccess.entries();
        this.panel = new BehaviorMachinePanel(declaration.toggles().size(), declaration.initialToggles(), this::setChanged);
        this.capacity = declaredCapacity();
        this.energyTag = !hasInput() || declaration.energy().dynamic() ? null : GregTechTags.Energy.ALL.stream()
                .filter(tag -> tag.getShortName().equalsIgnoreCase(declaration.energy().type().name()))
                .findFirst().orElseThrow();
        this.energyOutTag = energyOutName().isBlank() ? null : GregTechTags.Energy.ALL.stream()
                .filter(tag -> tag.getShortName().equalsIgnoreCase(energyOutName()))
                .findFirst().orElseThrow();
        this.auxiliaryTag = declaration.auxiliary() == null ? null : GregTechTags.Energy.ALL.stream()
                .filter(tag -> tag.getShortName().equalsIgnoreCase(declaration.auxiliary().type().name()))
                .findFirst().orElseThrow();
        this.state = new BehaviorMachineState(declaration.clientState(), this::setChanged);
        var itemAccess = new BehaviorMachineItems(declaration, items);
        this.worldAccess = new BehaviorMachineWorld(this, declaration.maxRange(), itemAccess);
        this.context = new BehaviorMachineContext(this, itemAccess, fluidAccess, behaviorEnergy(), structure, panel, worldAccess);
        this.ioPorts = new BehaviorMachineIOPorts(this, declaration, behavior, context, items, fluidAccess);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BehaviorMachineBlockEntity machine) {
        machine.tickServer(level, pos);
    }

    public BehaviorMachine declaration() { return declaration; }
    public AddonMachines.AcceptedBehavior accepted() { return accepted; }

    @Override
    public long gtSignal(BlockFace side) {
        if (level == null || level.isClientSide || !declaration.signalOutput() || side == null) return 0;
        return gregtech.api.signal.GtSignalCarrier.clamp(
                behavior.gtSignal(context, Direction.from3DDataValue(side.index())));
    }
    public BehaviorState state() { return state; }

    @Override public String addonId() { return accepted.addonId(); }
    @Override public String machineType() { return declaration.casing().machineType(); }
    @Override public int getInt(String key) { state.requireClientKey(key); return state.getInt(key); }
    @Override public long getLong(String key) { state.requireClientKey(key); return state.getLong(key); }
    @Override public boolean getBoolean(String key) { state.requireClientKey(key); return state.getBoolean(key); }
    @Override public String getString(String key) { state.requireClientKey(key); return state.getString(key); }

    public ItemStackHandler inventory() { return items; }
    public IItemHandler items() { return ioPorts.items(); }
    public IFluidHandler fluids() { return ioPorts.fluids(); }
    public int tanks() { return tanks.length; }
    public long storedEnergy() { return stored; }
    public long energyCapacity() { return capacity; }
    /** Gauge maximum: the storage cap, or the rated input when the machine does not store. */
    public long energyBarMax() { return bufferCapacity(); }
    public boolean onUse(Player player, InteractionHand hand) { return behavior.onUse(context, player, hand); }
    public boolean onTool(gregtech.api.addon.platform.AddonTool tool, Player player,
                          InteractionHand hand, MachineFace face) {
        return behavior.onTool(context, tool, player, hand, face);
    }
    public MachineFace clickedFace(Direction side) {
        int index = relative(side);
        for (MachineFace face : MachineFace.values()) if (face.index() == index) return face;
        return MachineFace.FRONT;
    }
    public MachineBehavior behavior() { return behavior; }
    public boolean toggle(int index) { return panel.toggle(index); }

    @Override
    public boolean fillMoldAtSide(ITileEntityMold mold, int crucibleSide, int moldSide) {
        if (mold == null) return false;
        if (crucibleSide < 0 || crucibleSide > 5 || moldSide < 0 || moldSide > 5) return false;
        return behavior.fillMoldAtSide(context, moldTarget(mold),
                Direction.from3DDataValue(crucibleSide), Direction.from3DDataValue(moldSide));
    }

    @Override
    public long getCrucibleTemperature() {
        return behavior.crucibleContent(context).temperature();
    }

    @Override
    public long getCrucibleContentAmount() {
        return behavior.crucibleContent(context).amount();
    }

    /** Read-only crucible snapshot for a bound part. Nothing is removed and no controller is kept. */
    public CrucibleContent crucibleContent() {
        return behavior.crucibleContent(context);
    }

    private static CrucibleMold moldTarget(ITileEntityMold mold) {
        return (material, amount, temperature, side) -> {
            if (material == null) return 0;
            var resolved = GTMaterialRegistry.get(material.name());
            return resolved.isValid() ? mold.fillMold(resolved, amount, temperature, side.index()) : 0;
        };
    }
//? if >=1.21.1 {
/*
    public IItemHandler itemCapability(@Nullable Direction side) { return itemHandler(side); }

    @Nullable
    public IFluidHandler fluidCapability(@Nullable Direction side) { return fluidHandler(side); }
*///?} else {
//?}

    @Nullable
    public IItemHandler itemHandler(@Nullable Direction side) {
        return ioPorts.itemHandler(side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return ioPorts.fluidHandler(side);
    }

    public boolean behaviorAllows(int slot, ItemStack stack) {
        return behavior.canInsert(context, slot, stack);
    }

    public boolean flip(int index) {
        if (index < 0 || index >= declaration.toggles().size()) return false;
        panel.setToggle(index, !panel.toggle(index));
        behavior.onToggle(context, index);
        setChanged();
        return true;
    }

    public void rememberPlacer(Player player) {
        worldAccess.rememberPlacer(player);
    }

    public void dropContents() {
        if (level == null) return;
        behavior.beforeBreak(context);
        for (int slot = 0; slot < items.getSlots(); slot++)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
        state.forEachStoredItem(stack ->
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack));
    }

    public boolean structureFormed() {
        return structure.formed();
    }

    private void tickServer(Level level, BlockPos pos) {
        worldAccess.beginTick();
        pollEnergy(level, pos);
        pollAuxiliary(level, pos);
        powered = stored > 0;
        panel.working(false);
        behavior.tick(context);
        stored = BehaviorEnergyRules.finishTick(stored, capacity, accepted.energyInMax());
        BlockState state = getBlockState();
        boolean working = panel.working();
        if (state.getValue(BehaviorMachineBlock.RUNNING) != powered || state.getValue(BehaviorMachineBlock.LIT) != working)
            level.setBlock(pos, state.setValue(BehaviorMachineBlock.RUNNING, powered).setValue(BehaviorMachineBlock.LIT, working), 3);
        if (this.state.consumeClientChanges()) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void pollEnergy(Level level, BlockPos pos) {
        if (!hasInput()) return;
        long size = declaration.energy().perTick();
        var tags = mainInputTags();
        for (Direction side : Direction.values()) {
            if (!accepts(side)) continue;
            BlockPos neighborPos = pos.relative(side);
            if (!level.hasChunkAt(neighborPos)) continue;
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            IEnergyBlock source = EnergyBlockViews.view(neighbor);
            if (source == null) continue;
            for (var tag : tags) {
                if (!source.isEnergyEmittingTo(tag, side.getOpposite(), false)) continue;
                long offered = source.getEnergyOffered(tag, side.getOpposite(), size);
                long requested = BasicMachineEnergy.accepted(stored, bufferCapacity(), size, offered);
                if (requested <= 0) continue;
                long extracted = source.doEnergyExtraction(tag, side.getOpposite(), size, requested, true);
                if (extracted > 0) {
                    stored = BasicMachineEnergy.add(stored, bufferCapacity(), size, extracted);
                    if (declaration.energy().dynamic() && dynamicEnergy == null) {
                        dynamicEnergy = tag.getShortName();
                        setChanged();
                    }
                    break;
                }
            }
        }
    }

    private void pollAuxiliary(Level level, BlockPos pos) {
        if (auxiliaryTag == null || declaration.auxiliary() == null) return;
        long size = declaration.auxiliary().perTick();
        long cap = declaration.auxiliary().capacity();
        for (Direction side : Direction.values()) {
            if (!auxiliaryFace(side)) continue;
            BlockPos neighborPos = pos.relative(side);
            if (!level.hasChunkAt(neighborPos)) continue;
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            IEnergyBlock source = EnergyBlockViews.view(neighbor);
            if (source == null || !source.isEnergyEmittingTo(auxiliaryTag, side.getOpposite(), false)) continue;
            long offered = source.getEnergyOffered(auxiliaryTag, side.getOpposite(), size);
            long requested = BasicMachineEnergy.accepted(auxiliaryStored, cap, size, offered);
            if (requested <= 0) continue;
            long extracted = source.doEnergyExtraction(auxiliaryTag, side.getOpposite(), size, requested, true);
            if (extracted > 0) auxiliaryStored = BasicMachineEnergy.add(auxiliaryStored, cap, size, extracted);
        }
    }

    private boolean accepts(Direction side) {
        return side == null || FaceConfig.has(declaration.energyFaces(), relative(side));
    }

    private boolean auxiliaryFace(Direction side) {
        return declaration.auxiliary() != null && side != null
                && FaceConfig.has(declaration.auxiliaryFaces(), relative(side));
    }

    private boolean auxiliaryAccepts(String energy, Direction side) {
        return auxiliaryFace(side) && energy != null && energy.equalsIgnoreCase(declaration.auxiliary().type().name());
    }

    private boolean sameEnergy(String energy, String type) {
        return energy != null && type != null && energy.equalsIgnoreCase(type);
    }

    /** True when the main input takes {@code energy}; a dynamic input accepts any GT type until it locks. */
    private boolean mainAccepts(String energy) {
        if (!hasInput() || energy == null) return false;
        var input = declaration.energy();
        if (!input.dynamic()) return sameEnergy(energy, input.type().name());
        if (dynamicEnergy != null) return sameEnergy(energy, dynamicEnergy);
        for (var tag : GregTechTags.Energy.ALL_GT) if (sameEnergy(energy, tag.getShortName())) return true;
        return false;
    }

    private java.util.List<GregTechTags.Tag> mainInputTags() {
        if (!hasInput()) return java.util.List.of();
        if (!declaration.energy().dynamic()) return java.util.List.of(energyTag);
        if (dynamicEnergy == null) return GregTechTags.Energy.ALL_GT;
        var tag = tagFor(dynamicEnergy);
        return tag == null ? java.util.List.of() : java.util.List.of(tag);
    }

    private static GregTechTags.Tag tagFor(String name) {
        for (var tag : GregTechTags.Energy.ALL_GT) if (tag.getShortName().equalsIgnoreCase(name)) return tag;
        return null;
    }

    private String energyOutName() {
        return declaration.output() == null ? "" : declaration.output().type().name();
    }

    private long energyOutSize() {
        return declaration.output() == null ? 0 : declaration.output().maxSize();
    }

    private long energyOutAmount() {
        return declaration.output() == null ? 0 : declaration.output().maxAmount();
    }

    private boolean emits(Direction side) {
        return declaration.emitsEnergy() && (side == null || FaceConfig.has(declaration.outputFaces(), relative(side)));
    }

    private long outputSize(String energy, Direction side) {
        return isEnergyEmittingTo(EnergyType.of(energy), side, true) ? energyOutSize() : 0;
    }

    private boolean hasInput() { return declaration.energy() != null; }

    /** One full output packet. An output-only machine keeps generated energy here until {@code emit}. */
    private long outputRoom() {
        long size = energyOutSize();
        long amount = energyOutAmount();
        if (size < 1 || amount < 1) return 0;
        if (amount > Long.MAX_VALUE / size) return Long.MAX_VALUE;
        return size * amount;
    }

    private long declaredCapacity() {
        return hasInput() ? declaration.energy().capacity() : outputRoom();
    }

    private long bufferCapacity() {
        if (!hasInput()) return Math.max(0, capacity);
        return BehaviorEnergyRules.buffer(capacity, accepted.energyInMax());
    }

    private long emit(long size, long amount) {
        EnergyOutput output = declaration.output();
        if (output == null) {
            BehaviorEnergyRules.requireEmit(size, amount, 0, 0);
            return 0;
        }
        BehaviorEnergyRules.requireEmit(size, amount, output.minSize(), output.maxSize(), output.maxAmount(),
                output.allowNegative());
        if (energyOutTag == null || level == null) return 0;
        long magnitude = output.allowNegative() ? Math.abs(size) : size;
        long affordable = stored / magnitude;
        long left = Math.min(amount, Math.max(0, affordable));
        long sent = 0;
        for (MachineFace face : output.faces()) {
            if (left <= 0) break;
            Direction outward = world(face);
            long used = structureDefinition().formsImmediately()
                    ? gregtech.engine.energy.EnergyTransfer.emitEnergyToSide(energyOutTag, outward, size, left, this)
                    : gregtech.engine.multiblock.StructureOutputs.energy(this,
                            gregtech.engine.multiblock.StructureOutputs.at(this, gregtech.api.multiblock.PortChannel.ENERGY_OUTPUT,
                                    worldPosition.relative(outward), outward.getOpposite(), false), energyOutTag, size, left);
            sent += used;
            left -= used;
        }
        if (sent > 0) {
            stored -= Math.multiplyExact(magnitude, sent);
            setChanged();
        }
        return sent;
    }

    private long emitTo(BlockPos target, Direction side, long size, long amount) {
        EnergyOutput output = declaration.output();
        if (output == null) {
            BehaviorEnergyRules.requireEmit(size, amount, 0, 0);
            return 0;
        }
        BehaviorEnergyRules.requireEmit(size, amount, output.minSize(), output.maxSize(), output.maxAmount(),
                output.allowNegative());
        if (energyOutTag == null || level == null || target == null) return 0;
        if (!level.hasChunkAt(target)) return 0;
        BlockEntity receiver = level.getBlockEntity(target);
        if (receiver == null || receiver.isRemoved()) return 0;
        long magnitude = output.allowNegative() ? Math.abs(size) : size;
        long packets = Math.min(amount, Math.max(0, stored) / magnitude);
        if (packets <= 0) return 0;
        boolean single = structureDefinition().formsImmediately();
        if (!single && side == null) return 0;
        long sent = single ? gregtech.engine.energy.EnergyTransfer.insertEnergyInto(energyOutTag, side, size, packets, receiver)
                : gregtech.engine.multiblock.StructureOutputs.energy(this,
                        gregtech.engine.multiblock.StructureOutputs.at(this, gregtech.api.multiblock.PortChannel.ENERGY_OUTPUT,
                                target, side, false), energyOutTag, size, packets);
        if (sent > 0) {
            stored -= Math.multiplyExact(magnitude, sent);
            setChanged();
        }
        return sent;
    }

    private long emitFromPorts(long size, long amount) {
        EnergyTransfer.requestUnits(size, amount);
        if (amount == 0) return 0;
        EnergyOutput output = declaration.output();
        if (output == null) {
            BehaviorEnergyRules.requireEmit(size, amount, 0, 0);
            return 0;
        }
        BehaviorEnergyRules.requireEmit(size, amount, output.minSize(), output.maxSize(), output.maxAmount(),
                output.allowNegative());
        if (energyOutTag == null || !isStructureOk()) return 0;
        long magnitude = output.allowNegative() ? Math.abs(size) : size;
        long packets = Math.min(amount, Math.max(0, stored) / magnitude);
        long sent = gregtech.engine.multiblock.StructurePortHandlers.emitEnergy(this, energyOutTag, size, packets);
        if (sent > 0) {
            stored -= Math.multiplyExact(magnitude, sent);
            setChanged();
        }
        return sent;
    }

    private long pushToPart(String name, String type, long size, long amount) {
        if (level == null || name == null || type == null || amount < 1) return 0;
        var view = structure.boundEnergyPart(name);
        if (view == null || !view.controllerInput() || !type.equalsIgnoreCase(view.outputType())) return 0;
        var output = view.output();
        long magnitude = output.allowNegative() ? Math.abs(size) : size;
        if (magnitude < 1) return 0;
        long request = Math.multiplyExact(magnitude, amount);
        view.setOutput(size, amount);
        return view.charge(request);
    }

    private long emitToPart(String name, long size, long amount) {
        if (level == null || name == null) return 0;
        var view = structure.boundEnergyPart(name);
        if (view == null) return 0;
        var output = view.output();
        if (output == null) return 0;
        BehaviorEnergyRules.requireEmit(size, amount, output.minSize(), output.maxSize(), output.maxAmount(),
                output.allowNegative());
        String offered = declaration.output() != null ? declaration.output().type().name()
                : dynamicEnergy != null ? dynamicEnergy
                : declaration.energy() != null && !declaration.energy().dynamic() ? declaration.energy().type().name() : null;
        if (offered != null && !offered.equalsIgnoreCase(output.type().name())) return 0;
        long magnitude = output.allowNegative() ? Math.abs(size) : size;
        if (magnitude < 1) return 0;
        long packets = Math.min(amount, Math.max(0, stored) / magnitude);
        if (packets <= 0) return 0;
        view.setOutput(size, packets);
        long accepted = view.charge(Math.multiplyExact(magnitude, packets));
        if (accepted > 0) {
            stored -= accepted;
            setChanged();
        }
        return accepted;
    }

    private Direction world(MachineFace face) {
        Direction facing = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        return switch (face) {
            case BOTTOM -> Direction.DOWN;
            case TOP -> Direction.UP;
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
        };
    }

    final int relative(Direction side) {
        Direction facing = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        if (side == Direction.DOWN) return FaceConfig.BOTTOM;
        if (side == Direction.UP) return FaceConfig.TOP;
        return switch (facing) {
            case SOUTH -> switch (side) {
                case SOUTH -> FaceConfig.FRONT; case NORTH -> FaceConfig.BACK;
                case EAST -> FaceConfig.RIGHT; case WEST -> FaceConfig.LEFT;
                default -> FaceConfig.FRONT;
            };
            case WEST -> switch (side) {
                case WEST -> FaceConfig.FRONT; case EAST -> FaceConfig.BACK;
                case NORTH -> FaceConfig.LEFT; case SOUTH -> FaceConfig.RIGHT;
                default -> FaceConfig.FRONT;
            };
            case EAST -> switch (side) {
                case EAST -> FaceConfig.FRONT; case WEST -> FaceConfig.BACK;
                case SOUTH -> FaceConfig.LEFT; case NORTH -> FaceConfig.RIGHT;
                default -> FaceConfig.FRONT;
            };
            default -> switch (side) {
                case NORTH -> FaceConfig.FRONT; case SOUTH -> FaceConfig.BACK;
                case WEST -> FaceConfig.RIGHT; case EAST -> FaceConfig.LEFT;
                default -> FaceConfig.FRONT;
            };
        };
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BehaviorMachineMenu(id, inventory, this);
    }

    public ContainerData menuData() {
        return new ContainerData() {
            @Override public int get(int index) { return menuValue(index); }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 7 + tanks.length * 2; }
        };
    }

    private int menuValue(int index) {
        int energyStart = 3 + tanks.length * 2;
        // Container data is transmitted as signed shorts on Forge; split all 64 bits explicitly.
        if (index >= energyStart && index < energyStart + 4)
            return (int) (stored >>> ((index - energyStart) * 16)) & 0xFFFF;
        if (index == 0) return (int) Math.min(Integer.MAX_VALUE, stored);
        if (index == 1) return panel.progressUnits();
        if (index == 2) return panel.toggleMask();
        int tanksStart = 3;
        if (index < tanksStart + tanks.length) return tanks[index - tanksStart].amount;
        int fluid = index - tanksStart - tanks.length;
        if (fluid < 0 || fluid >= tanks.length || tanks[fluid].amount <= 0 || tanks[fluid].fluid == Fluids.EMPTY) return -1;
        return BuiltInRegistries.FLUID.getId(tanks[fluid].fluid);
    }

//? if >=1.21.1 {
/**///?} else {
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return ioPorts.itemCapability(side).cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER && tanks.length > 0) {
            LazyOptional<IFluidHandler> fluids = ioPorts.fluidCapability(side);
            return fluids == null ? LazyOptional.empty() : fluids.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        ioPorts.invalidateCaps();
    }

//?}
    @Override
    public boolean isEnergyType(EnergyType energy, Direction side, boolean emitting) {
        if (energy == null) return false;
        if (emitting) return declaration.emitsEnergy() && energy.name().equalsIgnoreCase(energyOutName());
        return mainAccepts(energy.name())
                || (declaration.auxiliary() != null
                        && sameEnergy(energy.name(), declaration.auxiliary().type().name()));
    }

    @Override public java.util.Collection<EnergyType> getEnergyTypes(Direction side) {
        var types = new java.util.ArrayList<EnergyType>();
        if (hasInput()) {
            if (dynamicEnergy != null) types.add(EnergyType.of(dynamicEnergy));
            else if (declaration.energy().dynamic())
                for (var tag : GregTechTags.Energy.ALL_GT) types.add(EnergyType.of(tag.getShortName()));
            else types.add(declaration.energy().type());
        }
        if (declaration.auxiliary() != null && !containsEnergy(types, declaration.auxiliary().type()))
            types.add(declaration.auxiliary().type());
        if (declaration.emitsEnergy() && !containsEnergy(types, EnergyType.of(energyOutName())))
            types.add(EnergyType.of(energyOutName()));
        return List.copyOf(types);
    }

    private static boolean containsEnergy(java.util.List<EnergyType> types, EnergyType energy) {
        for (EnergyType type : types) if (type.name().equalsIgnoreCase(energy.name())) return true;
        return false;
    }

    @Override
    public boolean isEnergyAcceptingFrom(EnergyType energy, Direction side, boolean theoretical) {
        if (auxiliaryAccepts(energy.name(), side)) return true;
        return mainAccepts(energy.name()) && accepts(side);
    }

    @Override
    public boolean isEnergyEmittingTo(EnergyType energy, Direction side, boolean theoretical) {
        return isEnergyType(energy, side, true) && emits(side);
    }

    @Override
    public long doInject(EnergyType energy, Direction side, long size, long amount, boolean doInject) {
        if (auxiliaryAccepts(energy.name(), side)) return injectAuxiliary(energy.name(), side, size, amount, doInject);
        if (!hasInput() || !isEnergyAcceptingFrom(energy, side, false)
                || size == 0 || size == Long.MIN_VALUE || amount <= 0) return 0;
        return injectMain(energy, side, size, amount, doInject);
    }

    private long injectMain(EnergyType energy, Direction side, long size, long amount, boolean doInject) {
        var input = declaration.energy();
        boolean any = input.anyPacket();
        var decision = BehaviorEnergyRules.admitInput(stored, bufferCapacity(), size, amount,
                any ? 1 : accepted.energyInMin(), any ? Long.MAX_VALUE : accepted.energyInMax(),
                any, input.overload(), any || input.rangeSpecified());
        if (!takePacket(decision.packets(), decision.overload(), energy, side, size, amount)) return 0;
        if (decision.overload()) {
            if (doInject) gregtech.engine.energy.MachineOverload.apply(this, Math.abs(size), energy.name());
            return decision.packets();
        }
        if (doInject && decision.packets() > 0) {
            if (input.dynamic() && dynamicEnergy == null) {
                var locked = tagFor(energy.name());
                dynamicEnergy = locked == null ? energy.name() : locked.getShortName();
            }
            stored = BasicMachineEnergy.add(stored, bufferCapacity(), size, decision.packets());
            setChanged();
        }
        return decision.packets();
    }

    private long injectAuxiliary(String energy, Direction side, long size, long amount, boolean doInject) {
        var auxiliary = declaration.auxiliary();
        boolean anyPacket = auxiliary.anyPacket();
        var decision = PartEnergyRules.admit(auxiliaryStored, auxiliary.capacity(), size, amount,
                anyPacket ? 1 : auxiliary.rangeMin(), anyPacket ? Long.MAX_VALUE : auxiliary.rangeMax(), anyPacket);
        if (!takePacket(decision.packets(), decision.overload(), EnergyType.of(energy), side, size, amount)) return 0;
        if (decision.overload()) {
            if (doInject) gregtech.engine.energy.MachineOverload.apply(this, Math.abs(size), energy);
            return decision.packets();
        }
        if (doInject && decision.packets() > 0) {
            auxiliaryStored = BasicMachineEnergy.add(auxiliaryStored, auxiliary.capacity(), size, decision.packets());
            setChanged();
        }
        return decision.packets();
    }

    /** False after the face and buffer checks means this packet is refused whole. */
    private boolean takePacket(long packets, boolean overload, EnergyType energy, Direction side, long size, long amount) {
        if (packets <= 0 && !overload) return false;
        MachineFace face = side == null ? null : clickedFace(side);
        return behavior.acceptsEnergy(context, energy, face, size, amount);
    }

    @Override
    public long getEnergyDemanded(EnergyType energy, Direction side, long size) {
        if (auxiliaryAccepts(energy.name(), side)) {
            if (size == 0 || size == Long.MIN_VALUE) return 0;
            var auxiliary = declaration.auxiliary();
            if (!auxiliary.anyPacket()) {
                long sizeAbs = Math.abs(size);
                if (sizeAbs < auxiliary.rangeMin() || sizeAbs > auxiliary.rangeMax()) return 0;
            }
            return BasicMachineEnergy.demanded(auxiliaryStored, auxiliary.capacity(), size);
        }
        if (!hasInput() || !isEnergyAcceptingFrom(energy, side, false) || size == 0 || size == Long.MIN_VALUE) return 0;
        var input = declaration.energy();
        if (!input.anyPacket()) {
            long sizeAbs = Math.abs(size);
            boolean outside = sizeAbs < accepted.energyInMin() || sizeAbs > accepted.energyInMax();
            if (outside && (input.rangeSpecified() || !input.overload())) return 0;
        }
        return BasicMachineEnergy.demanded(stored, bufferCapacity(), size);
    }

    @Override public long getEnergyOffered(EnergyType energy, Direction side, long size) { return 0; }
    @Override public long getEnergySizeInputRecommended(EnergyType energy, Direction side) {
        if (auxiliaryAccepts(energy.name(), side)) return declaration.auxiliary().perTick();
        return mainAccepts(energy.name()) ? declaration.energy().perTick() : 0;
    }
    @Override public long getEnergySizeOutputRecommended(EnergyType energy, Direction side) {
        return outputSize(energy.name(), side);
    }
    @Override public long getEnergySizeOutputMin(EnergyType energy, Direction side) {
        return outputSize(energy.name(), side);
    }
    @Override public long getEnergySizeOutputMax(EnergyType energy, Direction side) {
        return outputSize(energy.name(), side);
    }
    @Override public long getEnergySizeInputMin(EnergyType energy, Direction side) {
        if (auxiliaryAccepts(energy.name(), side))
            return declaration.auxiliary().anyPacket() ? 1 : declaration.auxiliary().rangeMin();
        if (!mainAccepts(energy.name())) return 0;
        return declaration.energy().anyPacket() ? 1 : accepted.energyInMin();
    }
    @Override public long getEnergySizeInputMax(EnergyType energy, Direction side) {
        if (auxiliaryAccepts(energy.name(), side))
            return declaration.auxiliary().anyPacket() ? Long.MAX_VALUE : declaration.auxiliary().rangeMax();
        if (!mainAccepts(energy.name())) return 0;
        return declaration.energy().anyPacket() ? Long.MAX_VALUE : accepted.energyInMax();
    }

    @Override public boolean isEnergyCapacitorType(EnergyType energy, Direction side) {
        if (auxiliaryAccepts(energy.name(), side)) return true;
        return hasInput() && capacity > 0 && mainAccepts(energy.name()) && accepts(side);
    }

    @Override public long getEnergyStored(EnergyType energy, Direction side) {
        if (auxiliaryAccepts(energy.name(), side)) return auxiliaryStored;
        return isEnergyCapacitorType(energy, side) ? stored : 0;
    }

    @Override public long getEnergyCapacity(EnergyType energy, Direction side) {
        if (auxiliaryAccepts(energy.name(), side)) return declaration.auxiliary().capacity();
        return isEnergyCapacitorType(energy, side) ? capacity : 0;
    }

    @Override public java.util.Collection<EnergyType> getEnergyCapacitorTypes(Direction side) {
        var types = new java.util.ArrayList<EnergyType>();
        if (hasInput() && capacity > 0) {
            if (dynamicEnergy != null) types.add(EnergyType.of(dynamicEnergy));
            else if (declaration.energy().dynamic())
                for (var tag : GregTechTags.Energy.ALL_GT) types.add(EnergyType.of(tag.getShortName()));
            else types.add(declaration.energy().type());
        }
        if (declaration.auxiliary() != null && !containsEnergy(types, declaration.auxiliary().type()))
            types.add(declaration.auxiliary().type());
        return List.copyOf(types);
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        writeMachine(tag, lookup);
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        readMachine(tag, lookup);
    }

    public void writeMachine(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        tag.putLong("gt.energy", stored);
        if (declaration.energy() != null && declaration.energy().dynamic() && dynamicEnergy != null)
            tag.putString("gt.energy_type", dynamicEnergy);
        if (declaration.auxiliary() != null) tag.putLong("gt.aux_energy", auxiliaryStored);
        if (capacitySet) tag.putLong("gt.capacity", capacity);
        panel.write(tag);
        worldAccess.writePlacer(tag);
        tag.put("gt.items", gregtech.engine.stack.HandlerNbt.save(items, lookup));
        fluidAccess.write(tag);
        tag.put("gt.behavior", state.save(lookup));
    }

    public void readMachine(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        capacity = BehaviorEnergyRules.readCapacity(tag.contains("gt.capacity"), tag.getLong("gt.capacity"), declaredCapacity());
        capacitySet = tag.contains("gt.capacity");
        stored = Math.max(0, Math.min(bufferCapacity(), tag.getLong("gt.energy")));
        dynamicEnergy = null;
        if (declaration.energy() != null && declaration.energy().dynamic() && tag.contains("gt.energy_type")) {
            var locked = tagFor(tag.getString("gt.energy_type"));
            if (locked != null) dynamicEnergy = locked.getShortName();
        }
        auxiliaryStored = 0;
        if (declaration.auxiliary() != null && tag.contains("gt.aux_energy"))
            auxiliaryStored = Math.max(0, Math.min(declaration.auxiliary().capacity(), tag.getLong("gt.aux_energy")));
        panel.read(tag);
        worldAccess.readPlacer(tag);
        if (tag.contains("gt.items")) gregtech.engine.stack.HandlerNbt.load(items, tag.getCompound("gt.items"), lookup);
        fluidAccess.read(tag);
        if (tag.contains("gt.behavior")) state.load(tag.getCompound("gt.behavior"), lookup);
    }

    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        writeClientState(tag);
    }

    @Override
    protected void readUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        readClientState(tag);
    }

    @Override
    protected void handleDataPacket(Connection net, ClientboundBlockEntityDataPacket packet,
            @Nullable HolderLookup.Provider lookup) {
        if (packet.getTag() != null) readClientState(packet.getTag());
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private CompoundTag writeClientState(CompoundTag tag) {
        state.writeClientState(tag);
        gregtech.engine.multiblock.tracking.ClientFormedSync.write(this, tag);
        return tag;
    }

    private void readClientState(CompoundTag tag) {
        state.readClientState(tag);
        gregtech.engine.multiblock.tracking.ClientFormedSync.read(this, tag);
    }

    private EnergyAccess behaviorEnergy() {
        return new EnergyAccess() {
            @Override public long mainStored() { return hasInput() ? stored : 0; }

            @Override public boolean consumeMain(long amount) {
                if (amount < 0) throw new IllegalArgumentException("negative energy");
                if (!hasInput() || stored < amount) return false;
                stored -= amount;
                setChanged();
                return true;
            }

            @Override public long generateMain(long amount) {
                if (amount < 0) throw new IllegalArgumentException("negative energy");
                long room = bufferCapacity() - Math.min(stored, bufferCapacity());
                long added = Math.min(amount, Math.max(0, room));
                if (added <= 0) return 0;
                stored += added;
                setChanged();
                return added;
            }

            @Override public long mainCapacity() { return capacity; }

            @Override public void setMainCapacity(long value) {
                if (value < 0) throw new IllegalArgumentException("gt.capacity " + value);
                capacity = value;
                capacitySet = true;
                setChanged();
            }

            @Override public long auxiliaryStored(EnergyType type) {
                String name = type.name();
                if (declaration.auxiliary() == null || !sameEnergy(name, declaration.auxiliary().type().name())) return 0;
                return auxiliaryStored;
            }

            @Override public boolean consumeAuxiliary(EnergyType type, long amount) {
                String name = type.name();
                if (amount < 0) throw new IllegalArgumentException("negative energy");
                if (declaration.auxiliary() == null || !sameEnergy(name, declaration.auxiliary().type().name()))
                    return false;
                if (auxiliaryStored < amount) return false;
                auxiliaryStored -= amount;
                setChanged();
                return true;
            }

            @Override public EnergyTransfer emitFromFaces(long packetSize, long packetCount) {
                return EnergyTransfer.fromPackets(packetSize, packetCount, emit(packetSize, packetCount));
            }

            @Override public EnergyTransfer emitFromPorts(long packetSize, long packetCount) {
                return EnergyTransfer.fromPackets(packetSize, packetCount,
                        BehaviorMachineBlockEntity.this.emitFromPorts(packetSize, packetCount));
            }

            @Override public EnergyTransfer emitTo(BlockPos target, Direction side, long packetSize, long packetCount) {
                return EnergyTransfer.fromPackets(packetSize, packetCount,
                        BehaviorMachineBlockEntity.this.emitTo(target, side, packetSize, packetCount));
            }

            @Override public EnergyTransfer transferToPart(String part, long packetSize, long packetCount) {
                return new EnergyTransfer(packetSize, packetCount, emitToPart(part, packetSize, packetCount));
            }

            @Override public EnergyTransfer supplyPart(String part, EnergyType type, long packetSize, long packetCount) {
                return new EnergyTransfer(packetSize, packetCount, pushToPart(part, type.name(), packetSize, packetCount));
            }
        };
    }

    @Override public int receiveCompressedGas(Direction side, FluidStack stack) {
        return valveFace(side) ? fluidAccess.receiveCompressedGas(stack) : 0;
    }

    @Override public FluidStack extractCompressedGas(Direction side, int amount, boolean execute) {
        return valveFace(side) ? fluidAccess.extractCompressedGas(amount, execute) : FluidStack.EMPTY;
    }

    private boolean valveFace(Direction side) {
        MachineFace valve = declaration.gasValve();
        if (valve == null || side == null) return false;
        Direction facing = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        return PartFaces.relative(facing, side) == valve;
    }

    public LargeMachineLayouts.Layout trackedLayout() {
        return gregtech.content.registry.MultiblockRegistration.layout(this);
    }

    public boolean acceptsTrackedPart(LargeMachineLayouts.Cell cell, net.minecraft.world.level.block.Block expected,
                                      net.minecraft.world.level.block.Block actual) {
        return expected == actual;
    }

    public gregtech.engine.multiblock.tracking.StructureDefinition.Declarative createStructureDefinition() {
        return createLayoutDefinition(this);
    }

    public static gregtech.engine.multiblock.tracking.StructureDefinition.Declarative createLayoutDefinition(
            BehaviorMachineBlockEntity machine) {
        return gregtech.engine.multiblock.tracking.LayoutStructures.definition(
                machine, machine::trackedLayout, () -> machine.declaration().casing().machineType());
    }

    @Override public boolean isStructureOk() {
        return gregtech.engine.multiblock.tracking.StructureTracking.query(this);
    }

    @Override public void structureFormedChanged(boolean now) {
        structure.setFormed(now);
    }

    @Override public void onStructureFormed() {
        if (trackedLayout() != null) behavior.onStructureFormed(context);
    }

    @Override public void onStructureBroken() {
        if (trackedLayout() != null) behavior.onStructureBroken(context);
    }

    @Override public boolean applyStructure(PatternMatch match) {
        return structure.apply(match);
    }

    @Override public void applyIncompleteStructure(PatternMatch match) {
        structure.applyIncomplete(match);
    }

    @Override public void releaseStructureClaims() {
        structure.releaseClaims();
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            gregtech.engine.multiblock.tracking.StructureTracking.attach(this);
    }

    @Override public void onChunkUnloaded() {
        gregtech.engine.multiblock.tracking.StructureTracking.onChunkUnloaded(this);
        super.onChunkUnloaded();
    }

    @Override public void setRemoved() {
        structure.onRemoved();
        super.setRemoved();
    }

    // ── Structure ports ──────────────────────────────────────────────────

    private static boolean energyInput(MultiblockLayout.Role role) {
        return role != null && role.allows(gregtech.api.multiblock.PortChannel.ENERGY_INPUT);
    }

    private static boolean energyOutput(MultiblockLayout.Role role) {
        return role != null && role.allows(gregtech.api.multiblock.PortChannel.ENERGY_OUTPUT);
    }

    @Override public IItemHandler portItems(MultiblockLayout.Role role) {
        return portItems(role, null);
    }

    @Override public IItemHandler portItems(MultiblockLayout.Role role, MachineFace face) {
        return ioPorts.portItems(role, face);
    }

    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) {
        return portFluids(role, null);
    }

    @Override public IFluidHandler portFluids(MultiblockLayout.Role role, MachineFace face) {
        return ioPorts.portFluids(role, face);
    }

    @Override public IFluidHandler portFluidTank(MultiblockLayout.Role role, int tank) {
        return portFluidTank(role, tank, null);
    }

    @Override public IFluidHandler portFluidTank(MultiblockLayout.Role role, int tank, MachineFace face) {
        return ioPorts.portFluidTank(role, tank, face);
    }

    @Override public java.util.Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role) {
        var tags = new java.util.ArrayList<GregTechTags.Tag>();
        if (energyInput(role) && hasInput()) {
            if (dynamicEnergy != null) {
                var tag = tagFor(dynamicEnergy);
                if (tag != null) tags.add(tag);
            } else if (declaration.energy().dynamic()) {
                tags.addAll(GregTechTags.Energy.ALL_GT);
            } else {
                var tag = tagFor(declaration.energy().type().name());
                if (tag != null) tags.add(tag);
            }
            if (auxiliaryTag != null && !tags.contains(auxiliaryTag)) tags.add(auxiliaryTag);
        }
        if (energyOutput(role) && declaration.emitsEnergy() && energyOutTag != null && !tags.contains(energyOutTag))
            tags.add(energyOutTag);
        return tags;
    }

    @Override public boolean emitsPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return energyOutput(role) && declaration.emitsEnergy() && type != null && type == energyOutTag;
    }
    @Override public boolean acceptsPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return energyInput(role) && type != null && (type == auxiliaryTag || mainAccepts(type.getShortName()));
    }

    @Override public long portEnergyOutputSize(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return emitsPortEnergy(role, type) ? energyOutSize() : 0;
    }

    @Override public long portEnergyInputRecommended(MultiblockLayout.Role role, GregTechTags.Tag type) {
        if (!energyInput(role) || type == null) return 0;
        if (auxiliaryTag != null && type == auxiliaryTag) return declaration.auxiliary().perTick();
        return mainAccepts(type.getShortName()) ? declaration.energy().perTick() : 0;
    }

    @Override public long portEnergyInputMin(MultiblockLayout.Role role, GregTechTags.Tag type) {
        if (!energyInput(role) || type == null) return 0;
        if (auxiliaryTag != null && type == auxiliaryTag)
            return declaration.auxiliary().anyPacket() ? 1 : declaration.auxiliary().rangeMin();
        return mainAccepts(type.getShortName())
                ? (declaration.energy().anyPacket() ? 1 : accepted.energyInMin()) : 0;
    }

    @Override public long portEnergyInputMax(MultiblockLayout.Role role, GregTechTags.Tag type) {
        if (!energyInput(role) || type == null) return 0;
        if (auxiliaryTag != null && type == auxiliaryTag)
            return declaration.auxiliary().anyPacket() ? Long.MAX_VALUE : declaration.auxiliary().rangeMax();
        return mainAccepts(type.getShortName())
                ? (declaration.energy().anyPacket() ? Long.MAX_VALUE : accepted.energyInMax()) : 0;
    }

    @Override public long portEnergyStored(MultiblockLayout.Role role, GregTechTags.Tag type) {
        if (!energyInput(role) || type == null) return 0;
        if (auxiliaryTag != null && type == auxiliaryTag) return auxiliaryStored;
        return mainAccepts(type.getShortName()) ? stored : 0;
    }

    @Override public long portEnergyCapacity(MultiblockLayout.Role role, GregTechTags.Tag type) {
        if (!energyInput(role) || type == null) return 0;
        if (auxiliaryTag != null && type == auxiliaryTag) return declaration.auxiliary().capacity();
        return mainAccepts(type.getShortName()) ? bufferCapacity() : 0;
    }

    @Override public long portEnergyDemanded(MultiblockLayout.Role role, GregTechTags.Tag type, long size) {
        if (!energyInput(role) || type == null || size <= 0) return 0;
        if (auxiliaryTag != null && type == auxiliaryTag)
            return BasicMachineEnergy.demanded(auxiliaryStored, declaration.auxiliary().capacity(), size);
        return mainAccepts(type.getShortName()) ? BasicMachineEnergy.demanded(stored, bufferCapacity(), size) : 0;
    }

    @Override public long injectPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type,
                                           long size, long amount, boolean execute) {
        return injectPortEnergy(role, type, null, size, amount, execute);
    }

    @Override public long injectPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type, MachineFace face,
                                           long size, long amount, boolean execute) {
        if (!structure.formed() || !energyInput(role) || type == null || size == 0 || size == Long.MIN_VALUE || amount <= 0) return 0;
        Direction side = face == null ? null : world(face);
        if (auxiliaryTag != null && type == auxiliaryTag)
            return injectAuxiliary(type.getShortName(), side, size, amount, execute);
        return mainAccepts(type.getShortName())
                ? injectMain(EnergyType.of(type.getShortName()), side, size, amount, execute) : 0;
    }
}
