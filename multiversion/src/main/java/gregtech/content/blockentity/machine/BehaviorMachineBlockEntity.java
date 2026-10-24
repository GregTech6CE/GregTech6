package gregtech.content.blockentity.machine;

import gregtech.internal.addon.AddonMachines;
import gregtech.internal.addon.BehaviorEnergyRules;
import gregtech.api.addon.EnergyOutput;
import gregtech.api.addon.BehaviorMachine;
import gregtech.api.addon.CrucibleContent;
import gregtech.api.addon.CrucibleMold;
import gregtech.api.addon.MachineFace;
import gregtech.internal.addon.PartEnergyRules;
import gregtech.api.addon.BehaviorRangeRules;
import gregtech.internal.addon.BehaviorMachineHosts;
import gregtech.api.addon.platform.BehaviorMachineClientView;
import gregtech.api.addon.platform.BehaviorState;
//? if >=1.21.1 {
/*import gregtech.api.addon.platform.EnergyBlock;
*///?} else {
//?}
import gregtech.api.addon.platform.BoundPart;
import gregtech.api.addon.platform.EnergyPart;
import gregtech.api.addon.platform.PartBehavior;
import gregtech.api.addon.platform.PartContext;
import gregtech.api.addon.platform.MachineBehavior;
import gregtech.api.addon.platform.MachineBehaviorContext;
import gregtech.internal.multiblock.MultiblockLayout;
import gregtech.internal.multiblock.PartBindings;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.internal.energy.EnergyBlockViews;
import gregtech.internal.energy.FaceConfig;
import gregtech.internal.energy.IEnergyBlock;
import gregtech.internal.machine.BasicMachineEnergy;
import gregtech.internal.machine.ITileEntityCrucible;
import gregtech.internal.machine.ITileEntityMold;
import gregtech.internal.material.GTMaterialRegistry;
import gregtech.content.block.machine.BehaviorMachineBlock;
import gregtech.client.gui.BehaviorMachineMenu;
import gregtech.content.data.GregTechTags;
//? if >=1.21.1 {
/**///?} else {
import gregtech.api.addon.platform.EnergyBlock;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.minecraft.core.HolderLookup;
*///?} else {
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Shell block entity. The addon behavior never sees this class. */
public class BehaviorMachineBlockEntity extends BlockEntity implements EnergyBlock, MenuProvider,
        gregtech.internal.fluid.CompressedGasReceiver, ITileEntityCrucible,
        gregtech.internal.multiblock.MultiblockPortOwner, gregtech.api.signal.GtSignalSource,
        BehaviorMachineClientView, gregtech.internal.multiblock.tracking.TrackedMultiblock {
    private final BehaviorMachine declaration;
    private final AddonMachines.AcceptedBehavior accepted;
    private final MachineBehavior behavior;
    private final ItemStackHandler items;
    private final Tank[] tanks;
    private final StoredState state = new StoredState();
    private final Context context = new Context();
    private final PipeItems pipeItems = new PipeItems();
    private final PipeFluids pipeFluids = new PipeFluids();
    private final IItemHandler[] itemSides = new IItemHandler[6];
    private final IFluidHandler[] fluidSides = new IFluidHandler[6];
//? if >=1.21.1 {
/**///?} else {
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> pipeItems);
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> pipeFluids);
    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] sidedItemCaps = new LazyOptional[6];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IFluidHandler>[] sidedFluidCaps = new LazyOptional[6];
//?}
    private final GregTechTags.Tag energyTag;
    private final GregTechTags.Tag energyOutTag;
    private final GregTechTags.Tag auxiliaryTag;
    private long stored;
    private long auxiliaryStored;
    /** Energy type locked by the first accepted packet when the main input is dynamic. */
    private String dynamicEnergy;
    private long capacity;
    private boolean capacitySet;
    private int toggles;
    private int progress;
    private boolean working;
    private boolean powered;
    private final PartBindings<BlockPos, MultiblockLayout.Role> structureBindings = new PartBindings<>();
    private boolean formed;
    private boolean checkingStructure;
    private List<EnergyPart> energyParts = List.of();
    private List<BoundPart> boundParts = List.of();
    private Map<String, Integer> repeatCounts = Map.of();
    private int breaksLeft;
    private int itemsLeft;
    @Nullable private UUID placerId;
    private String placerName = "";
    private final java.util.Set<String> clientKeys;
    private boolean clientStateDirty;

    public BehaviorMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        BehaviorMachineBlock block = (BehaviorMachineBlock) state.getBlock();
        this.accepted = block.accepted();
        this.declaration = accepted.declaration();
        this.behavior = BehaviorMachineHosts.create(accepted.addonId(), declaration.casing().machineType());
        this.items = new ItemStackHandler(declaration.items().size()) {
            @Override protected void onContentsChanged(int slot) { setChanged(); }
        };
        this.tanks = new Tank[declaration.fluids().size()];
        for (int i = 0; i < tanks.length; i++) tanks[i] = new Tank(declaration.fluids().get(i));
        this.toggles = declaration.initialToggles();
        this.capacity = declaredCapacity();
        this.energyTag = !hasInput() || declaration.energy().dynamic() ? null : GregTechTags.Energy.ALL.stream()
                .filter(tag -> tag.getShortName().equalsIgnoreCase(declaration.energy().type()))
                .findFirst().orElseThrow();
        this.energyOutTag = energyOutName().isBlank() ? null : GregTechTags.Energy.ALL.stream()
                .filter(tag -> tag.getShortName().equalsIgnoreCase(energyOutName()))
                .findFirst().orElseThrow();
        this.auxiliaryTag = declaration.auxiliary() == null ? null : GregTechTags.Energy.ALL.stream()
                .filter(tag -> tag.getShortName().equalsIgnoreCase(declaration.auxiliary().type()))
                .findFirst().orElseThrow();
        this.clientKeys = java.util.Set.copyOf(declaration.clientState());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BehaviorMachineBlockEntity machine) {
        machine.tickServer(level, pos);
    }

    public BehaviorMachine declaration() { return declaration; }
    public AddonMachines.AcceptedBehavior accepted() { return accepted; }

    @Override
    public long gtSignal(int side) {
        if (level == null || level.isClientSide || !declaration.signalOutput() || side < 0 || side > 5) return 0;
        return gregtech.api.signal.GtSignalCarrier.clamp(
                behavior.gtSignal(context, Direction.from3DDataValue(side)));
    }
    public BehaviorState state() { return state; }

    @Override public String addonId() { return accepted.addonId(); }
    @Override public String machineType() { return declaration.casing().machineType(); }
    @Override public int getInt(String key) { requireClientKey(key); return state.getInt(key); }
    @Override public long getLong(String key) { requireClientKey(key); return state.getLong(key); }
    @Override public boolean getBoolean(String key) { requireClientKey(key); return state.getBoolean(key); }
    @Override public String getString(String key) { requireClientKey(key); return state.getString(key); }

    private void requireClientKey(String key) {
        if (!clientKeys.contains(key))
            throw new IllegalArgumentException("Behavior machine client state is not declared: " + key);
    }
    public ItemStackHandler inventory() { return items; }
    public IItemHandler items() { return pipeItems; }
    public IFluidHandler fluids() { return pipeFluids; }
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
    public boolean toggle(int index) { return context.toggle(index); }

    @Override
    public boolean fillMoldAtSide(ITileEntityMold mold, int crucibleSide, int moldSide) {
        if (mold == null) return false;
        if (crucibleSide < 0 || crucibleSide > 5 || moldSide < 0 || moldSide > 5) return false;
        return behavior.fillMoldAtSide(context, moldTarget(mold),
                Direction.from3DDataValue(crucibleSide), Direction.from3DDataValue(moldSide));
    }

    @Override
    public long getCrucibleTemperature() {
        return behavior.getCrucibleTemperature(context);
    }

    @Override
    public long getCrucibleContentAmount() {
        return behavior.getCrucibleContentAmount(context);
    }

    /** Read-only crucible snapshot for a bound part. Nothing is removed and no controller is kept. */
    public CrucibleContent crucibleContent() {
        return new CrucibleContent(behavior.getCrucibleMaterial(context),
                behavior.getCrucibleContentAmount(context),
                behavior.getCrucibleTemperature(context));
    }

    private static CrucibleMold moldTarget(ITileEntityMold mold) {
        return (material, amount, temperature, side) -> {
            if (material == null || !material.valid() || material.resolve() == null) return 0;
            var resolved = GTMaterialRegistry.get(material.resolve().name());
            return resolved.isValid() ? mold.fillMold(resolved, amount, temperature, side) : 0;
        };
    }
//? if >=1.21.1 {
/*
    public IItemHandler itemCapability(@Nullable Direction side) { return itemHandler(side); }

    @Nullable
    public IFluidHandler fluidCapability(@Nullable Direction side) { return fluidHandler(side); }
*///?} else {
//?}

    /** Unsided when {@code side} is null or the machine declares no {@code MachineIo}. A closed face is null. */
    @Nullable
    public IItemHandler itemHandler(@Nullable Direction side) {
        if (side == null || declaration.io() == null) return pipeItems;
        int rel = relative(side);
        boolean insert = FaceConfig.has(declaration.itemInputMask(), rel);
        boolean extract = FaceConfig.has(declaration.itemOutputMask(), rel);
        if (!insert && !extract) return null;
        IItemHandler cached = itemSides[rel];
        if (cached == null) itemSides[rel] = cached = new PipeItems(faceAt(rel), insert, extract);
        return cached;
    }

    /** Unsided when {@code side} is null or the machine declares no {@code MachineIo}. A closed face is null. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (tanks.length == 0) return null;
        if (side == null || declaration.io() == null) return pipeFluids;
        int rel = relative(side);
        boolean fill = FaceConfig.has(declaration.fluidInputMask(), rel);
        boolean drain = FaceConfig.has(declaration.fluidOutputMask(), rel);
        if (!fill && !drain) return null;
        IFluidHandler cached = fluidSides[rel];
        if (cached == null) fluidSides[rel] = cached = new PipeFluids(faceAt(rel), fill, drain);
        return cached;
    }

    private static MachineFace faceAt(int relative) {
        for (MachineFace face : MachineFace.values()) if (face.index() == relative) return face;
        return MachineFace.FRONT;
    }

    public boolean behaviorAllows(int slot, ItemStack stack) {
        return behavior.canInsert(context, slot, stack);
    }

    public boolean flip(int index) {
        if (index < 0 || index >= declaration.toggles().size()) return false;
        context.setToggle(index, !context.toggle(index));
        behavior.onToggle(context, index);
        setChanged();
        return true;
    }

    public void rememberPlacer(Player player) {
        if (player == null) return;
        placerId = player.getUUID();
        String name = player.getGameProfile().getName();
        placerName = name == null ? "" : name;
        setChanged();
    }

    public void dropContents() {
        if (level == null) return;
        behavior.beforeBreak(context);
        for (int slot = 0; slot < items.getSlots(); slot++)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(slot));
        for (ItemStack stack : state.items.values()) {
            if (!stack.isEmpty())
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
        }
    }

    public boolean structureFormed() {
        if (gregtech.internal.multiblock.tracking.StructureTrackingMode.legacy()) return formed;
        return isStructureOk();
    }

    private void tickServer(Level level, BlockPos pos) {
        breaksLeft = BehaviorRangeRules.BREAKS_PER_TICK;
        itemsLeft = BehaviorRangeRules.ITEMS_PER_TICK;
        pollEnergy(level, pos);
        pollAuxiliary(level, pos);
        if (gregtech.internal.multiblock.tracking.StructureTrackingMode.legacy()) refreshStructure();
        else formed = isStructureOk();
        powered = stored > 0;
        working = false;
        behavior.tick(context);
        stored = BehaviorEnergyRules.finishTick(stored, capacity, accepted.energyInMax());
        BlockState state = getBlockState();
        if (state.getValue(BehaviorMachineBlock.RUNNING) != powered || state.getValue(BehaviorMachineBlock.LIT) != working)
            level.setBlock(pos, state.setValue(BehaviorMachineBlock.RUNNING, powered).setValue(BehaviorMachineBlock.LIT, working), 3);
        if (clientStateDirty) {
            clientStateDirty = false;
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
        return side == null || FaceConfig.has(declaration.energyMask(), relative(side));
    }

    private boolean auxiliaryFace(Direction side) {
        return declaration.auxiliary() != null && side != null
                && FaceConfig.has(declaration.auxiliaryMask(), relative(side));
    }

    private boolean auxiliaryAccepts(String energy, Direction side) {
        return auxiliaryFace(side) && energy != null && energy.equalsIgnoreCase(declaration.auxiliary().type());
    }

    private boolean sameEnergy(String energy, String type) {
        return energy != null && type != null && energy.equalsIgnoreCase(type);
    }

    /** True when the main input takes {@code energy}; a dynamic input accepts any GT type until it locks. */
    private boolean mainAccepts(String energy) {
        if (!hasInput() || energy == null) return false;
        var input = declaration.energy();
        if (!input.dynamic()) return sameEnergy(energy, input.type());
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
        return declaration.output() == null ? "" : declaration.output().type();
    }

    private long energyOutSize() {
        return declaration.output() == null ? 0 : declaration.output().maxSize();
    }

    private long energyOutAmount() {
        return declaration.output() == null ? 0 : declaration.output().maxAmount();
    }

    private boolean emits(Direction side) {
        return declaration.emitsEnergy() && (side == null || FaceConfig.has(declaration.energyOutMask(), relative(side)));
    }

    private long outputSize(String energy, Direction side) {
        return isEnergyEmittingTo(energy, side, true) ? energyOutSize() : 0;
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
            long used = gregtech.internal.energy.EnergyTransfer.emitEnergyToSide(
                    energyOutTag, world(face), size, left, this);
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
        long sent = gregtech.internal.energy.EnergyTransfer.insertEnergyInto(
                energyOutTag, side, size, packets, this, receiver);
        if (sent > 0) {
            stored -= Math.multiplyExact(magnitude, sent);
            setChanged();
        }
        return sent;
    }

    @Nullable
    private EnergyPartView boundPart(String name) {
        if (!formed || name == null) return null;
        for (EnergyPart part : energyParts)
            if (part instanceof EnergyPartView view && view.name().equals(name)) return view;
        return null;
    }

    private long pushToPart(String name, String type, long size, long amount) {
        if (level == null || name == null || type == null || amount < 1) return 0;
        var view = boundPart(name);
        if (view == null || !view.controllerInput() || !type.equalsIgnoreCase(view.outputType())) return 0;
        var output = view.part.declaration().energy();
        long magnitude = output.allowNegative() ? Math.abs(size) : size;
        if (magnitude < 1) return 0;
        long request = Math.multiplyExact(magnitude, amount);
        view.setOutput(size, amount);
        return view.charge(request);
    }

    private long emitToPart(String name, long size, long amount) {
        if (level == null || name == null) return 0;
        var view = boundPart(name);
        if (view == null) return 0;
        var output = view.part.declaration().energy();
        if (output == null) return 0;
        BehaviorEnergyRules.requireEmit(size, amount, output.minSize(), output.maxSize(), output.maxAmount(),
                output.allowNegative());
        String offered = declaration.output() != null ? declaration.output().type()
                : dynamicEnergy != null ? dynamicEnergy
                : declaration.energy() != null && !declaration.energy().dynamic() ? declaration.energy().type() : null;
        if (offered != null && !offered.equalsIgnoreCase(output.type())) return 0;
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

    private int relative(Direction side) {
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
            @Override public int getCount() { return 3 + tanks.length * 2; }
        };
    }

    private int menuValue(int index) {
        if (index == 0) return (int) Math.min(Integer.MAX_VALUE, stored);
        if (index == 1) return progress;
        if (index == 2) return toggles;
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
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability(side).cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER && tanks.length > 0) {
            LazyOptional<IFluidHandler> fluids = fluidCapability(side);
            return fluids == null ? LazyOptional.empty() : fluids.cast();
        }
        return super.getCapability(cap, side);
    }

    private LazyOptional<IItemHandler> itemCapability(@Nullable Direction side) {
        if (side == null || declaration.io() == null) return itemCap;
        int rel = relative(side);
        if (itemHandler(side) == null) return LazyOptional.empty();
        if (sidedItemCaps[rel] == null) {
            IItemHandler handler = itemSides[rel];
            sidedItemCaps[rel] = LazyOptional.of(() -> handler);
        }
        return sidedItemCaps[rel];
    }

    @Nullable
    private LazyOptional<IFluidHandler> fluidCapability(@Nullable Direction side) {
        if (fluidHandler(side) == null) return null;
        if (side == null || declaration.io() == null) return fluidCap;
        int rel = relative(side);
        if (sidedFluidCaps[rel] == null) {
            IFluidHandler handler = fluidSides[rel];
            sidedFluidCaps[rel] = LazyOptional.of(() -> handler);
        }
        return sidedFluidCaps[rel];
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        fluidCap.invalidate();
        for (int i = 0; i < sidedItemCaps.length; i++) {
            if (sidedItemCaps[i] != null) sidedItemCaps[i].invalidate();
            if (sidedFluidCaps[i] != null) sidedFluidCaps[i].invalidate();
        }
    }

//?}
    @Override
    public boolean isEnergyType(String energy, Direction side, boolean emitting) {
        if (energy == null) return false;
        if (emitting) return declaration.emitsEnergy() && energy.equalsIgnoreCase(energyOutName());
        return mainAccepts(energy)
                || (declaration.auxiliary() != null && sameEnergy(energy, declaration.auxiliary().type()));
    }

    @Override public java.util.Collection<String> getEnergyTypes(Direction side) {
        var names = new java.util.ArrayList<String>();
        if (hasInput()) {
            if (dynamicEnergy != null) names.add(dynamicEnergy);
            else if (declaration.energy().dynamic())
                for (var tag : GregTechTags.Energy.ALL_GT) names.add(tag.getShortName());
            else names.add(declaration.energy().type());
        }
        if (declaration.auxiliary() != null && !containsEnergy(names, declaration.auxiliary().type()))
            names.add(declaration.auxiliary().type());
        if (declaration.emitsEnergy() && !containsEnergy(names, energyOutName()))
            names.add(energyOutName());
        return List.copyOf(names);
    }

    private static boolean containsEnergy(java.util.List<String> names, String energy) {
        for (String name : names) if (name.equalsIgnoreCase(energy)) return true;
        return false;
    }

    @Override
    public boolean isEnergyAcceptingFrom(String energy, Direction side, boolean theoretical) {
        if (auxiliaryAccepts(energy, side)) return true;
        return mainAccepts(energy) && accepts(side);
    }

    @Override
    public boolean isEnergyEmittingTo(String energy, Direction side, boolean theoretical) {
        return isEnergyType(energy, side, true) && emits(side);
    }

    @Override
    public long doInject(String energy, Direction side, long size, long amount, boolean doInject) {
        if (auxiliaryAccepts(energy, side)) return injectAuxiliary(energy, side, size, amount, doInject);
        if (!hasInput() || !isEnergyAcceptingFrom(energy, side, false)
                || size == 0 || size == Long.MIN_VALUE || amount <= 0) return 0;
        var input = declaration.energy();
        boolean any = input.anyPacket();
        var decision = BehaviorEnergyRules.admitInput(stored, bufferCapacity(), size, amount,
                any ? 1 : accepted.energyInMin(), any ? Long.MAX_VALUE : accepted.energyInMax(),
                any, input.overload(), any || input.rangeSpecified());
        if (!takePacket(decision.packets(), decision.overload(), energy, side, size, amount)) return 0;
        if (decision.overload()) {
            if (doInject) gregtech.internal.energy.MachineOverload.apply(this, Math.abs(size), energy);
            return decision.packets();
        }
        if (doInject && decision.packets() > 0) {
            if (input.dynamic() && dynamicEnergy == null) {
                var locked = tagFor(energy);
                dynamicEnergy = locked == null ? energy : locked.getShortName();
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
        if (!takePacket(decision.packets(), decision.overload(), energy, side, size, amount)) return 0;
        if (decision.overload()) {
            if (doInject) gregtech.internal.energy.MachineOverload.apply(this, Math.abs(size), energy);
            return decision.packets();
        }
        if (doInject && decision.packets() > 0) {
            auxiliaryStored = BasicMachineEnergy.add(auxiliaryStored, auxiliary.capacity(), size, decision.packets());
            setChanged();
        }
        return decision.packets();
    }

    /** False after the face and buffer checks means this packet is refused whole. */
    private boolean takePacket(long packets, boolean overload, String energy, Direction side, long size, long amount) {
        if (packets <= 0 && !overload) return false;
        MachineFace face = side == null ? null : clickedFace(side);
        return behavior.acceptsEnergy(context, energy, face, size, amount);
    }

    @Override
    public long getEnergyDemanded(String energy, Direction side, long size) {
        if (auxiliaryAccepts(energy, side)) {
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

    @Override public long getEnergyOffered(String energy, Direction side, long size) { return 0; }
    @Override public long getEnergySizeInputRecommended(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side)) return declaration.auxiliary().perTick();
        return mainAccepts(energy) ? declaration.energy().perTick() : 0;
    }
    @Override public long getEnergySizeOutputRecommended(String energy, Direction side) { return outputSize(energy, side); }
    @Override public long getEnergySizeOutputMin(String energy, Direction side) { return outputSize(energy, side); }
    @Override public long getEnergySizeOutputMax(String energy, Direction side) { return outputSize(energy, side); }
    @Override public long getEnergySizeInputMin(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side))
            return declaration.auxiliary().anyPacket() ? 1 : declaration.auxiliary().rangeMin();
        if (!mainAccepts(energy)) return 0;
        return declaration.energy().anyPacket() ? 1 : accepted.energyInMin();
    }
    @Override public long getEnergySizeInputMax(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side))
            return declaration.auxiliary().anyPacket() ? Long.MAX_VALUE : declaration.auxiliary().rangeMax();
        if (!mainAccepts(energy)) return 0;
        return declaration.energy().anyPacket() ? Long.MAX_VALUE : accepted.energyInMax();
    }

    @Override public boolean isEnergyCapacitorType(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side)) return true;
        return hasInput() && capacity > 0 && mainAccepts(energy) && accepts(side);
    }

    @Override public long getEnergyStored(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side)) return auxiliaryStored;
        return isEnergyCapacitorType(energy, side) ? stored : 0;
    }

    @Override public long getEnergyCapacity(String energy, Direction side) {
        if (auxiliaryAccepts(energy, side)) return declaration.auxiliary().capacity();
        return isEnergyCapacitorType(energy, side) ? capacity : 0;
    }

    @Override public java.util.Collection<String> getEnergyCapacitorTypes(Direction side) {
        var names = new java.util.ArrayList<String>();
        if (hasInput() && capacity > 0) {
            if (dynamicEnergy != null) names.add(dynamicEnergy);
            else if (declaration.energy().dynamic())
                for (var tag : GregTechTags.Energy.ALL_GT) names.add(tag.getShortName());
            else names.add(declaration.energy().type());
        }
        if (declaration.auxiliary() != null && !containsEnergy(names, declaration.auxiliary().type()))
            names.add(declaration.auxiliary().type());
        return List.copyOf(names);
    }

    @Override
//? if >=1.21.1 {
/*    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        writeMachine(tag, lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeMachine(tag);
//?}
    }

    @Override
//? if >=1.21.1 {
/*    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        readMachine(tag, lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
        readMachine(tag);
//?}
    }

//? if >=1.21.1 {
/*    public void writeMachine(CompoundTag tag, HolderLookup.Provider lookup) {
*///?} else {
    public void writeMachine(CompoundTag tag) {
//?}
        tag.putLong("gt.energy", stored);
        if (declaration.energy() != null && declaration.energy().dynamic() && dynamicEnergy != null)
            tag.putString("gt.energy_type", dynamicEnergy);
        if (declaration.auxiliary() != null) tag.putLong("gt.aux_energy", auxiliaryStored);
        if (capacitySet) tag.putLong("gt.capacity", capacity);
        tag.putInt("gt.progress", progress);
        tag.putInt("gt.toggles", toggles);
        if (placerId != null) {
            tag.putString("gt.placer", placerId.toString());
            tag.putString("gt.placer_name", placerName);
        }
//? if >=1.21.1 {
/*        tag.put("gt.items", items.serializeNBT(lookup));
*///?} else {
        tag.put("gt.items", items.serializeNBT());
//?}
        ListTag fluids = new ListTag();
        for (Tank tank : tanks) {
            CompoundTag row = new CompoundTag();
            ResourceLocation name = BuiltInRegistries.FLUID.getKey(tank.fluid);
            row.putString("Fluid", name == null ? "" : name.toString());
            row.putInt("Amount", tank.amount);
            fluids.add(row);
        }
        tag.put("gt.fluids", fluids);
//? if >=1.21.1 {
/*        tag.put("gt.behavior", state.save(lookup));
*///?} else {
        tag.put("gt.behavior", state.save());
//?}
    }

//? if >=1.21.1 {
/*    public void readMachine(CompoundTag tag, HolderLookup.Provider lookup) {
*///?} else {
    public void readMachine(CompoundTag tag) {
//?}
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
        progress = tag.getInt("gt.progress");
        toggles = tag.getInt("gt.toggles");
        placerId = null;
        placerName = "";
        if (tag.contains("gt.placer")) {
            try {
                placerId = UUID.fromString(tag.getString("gt.placer"));
                placerName = tag.getString("gt.placer_name");
            } catch (IllegalArgumentException ignored) {
                placerId = null;
            }
        }
//? if >=1.21.1 {
/*        if (tag.contains("gt.items")) items.deserializeNBT(lookup, tag.getCompound("gt.items"));
*///?} else {
        if (tag.contains("gt.items")) items.deserializeNBT(tag.getCompound("gt.items"));
//?}
        ListTag fluids = tag.getList("gt.fluids", Tag.TAG_COMPOUND);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].amount = 0;
            tanks[i].fluid = Fluids.EMPTY;
            if (i >= fluids.size()) continue;
            CompoundTag row = fluids.getCompound(i);
            ResourceLocation name = ResourceLocation.tryParse(row.getString("Fluid"));
            Fluid fluid = name == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(name);
            int amount = row.getInt("Amount");
            if (fluid != null && fluid != Fluids.EMPTY && amount > 0) {
                tanks[i].fluid = fluid;
                tanks[i].amount = Math.min(tanks[i].capacity, amount);
            }
        }
//? if >=1.21.1 {
/*        if (tag.contains("gt.behavior")) state.load(lookup, tag.getCompound("gt.behavior"));
*///?} else {
        if (tag.contains("gt.behavior")) state.load(tag.getCompound("gt.behavior"));
//?}
    }

//? if >=1.21.1 {
/*    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return writeClientState(new CompoundTag());
    }

    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClientState(tag);
    }

    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet,
                                      HolderLookup.Provider lookup) {
        if (packet.getTag() != null) readClientState(packet.getTag());
    }
*///?} else {
    @Override public CompoundTag getUpdateTag() {
        return writeClientState(new CompoundTag());
    }

    @Override public void handleUpdateTag(CompoundTag tag) {
        readClientState(tag);
    }

    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) readClientState(packet.getTag());
    }
//?}

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private CompoundTag writeClientState(CompoundTag tag) {
        CompoundTag ints = new CompoundTag();
        CompoundTag longs = new CompoundTag();
        CompoundTag bools = new CompoundTag();
        CompoundTag strings = new CompoundTag();
        for (String key : clientKeys) {
            Integer integer = state.ints.get(key);
            if (integer != null) ints.putInt(key, integer);
            Long value = state.longs.get(key);
            if (value != null) longs.putLong(key, value);
            Boolean flag = state.bools.get(key);
            if (flag != null) bools.putBoolean(key, flag);
            String text = state.strings.get(key);
            if (text != null) strings.putString(key, text);
        }
        tag.put("i", ints);
        tag.put("l", longs);
        tag.put("b", bools);
        tag.put("s", strings);
        gregtech.internal.multiblock.tracking.ClientFormedSync.write(this, tag);
        return tag;
    }

    private void readClientState(CompoundTag tag) {
        CompoundTag ints = tag.getCompound("i");
        for (String key : ints.getAllKeys()) if (clientKeys.contains(key)) state.ints.put(key, ints.getInt(key));
        CompoundTag longs = tag.getCompound("l");
        for (String key : longs.getAllKeys()) if (clientKeys.contains(key)) state.longs.put(key, longs.getLong(key));
        CompoundTag bools = tag.getCompound("b");
        for (String key : bools.getAllKeys()) if (clientKeys.contains(key)) state.bools.put(key, bools.getBoolean(key));
        CompoundTag strings = tag.getCompound("s");
        for (String key : strings.getAllKeys()) if (clientKeys.contains(key)) state.strings.put(key, strings.getString(key));
        gregtech.internal.multiblock.tracking.ClientFormedSync.read(this, tag);
    }

    private boolean breakOne(BlockPos target) {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel server) || target == null) return false;
        int dx = target.getX() - worldPosition.getX();
        int dy = target.getY() - worldPosition.getY();
        int dz = target.getZ() - worldPosition.getZ();
        if (!BehaviorRangeRules.inside(declaration.maxRange(), dx, dy, dz)) return false;
        if (breaksLeft <= 0 || !level.hasChunkAt(target)) return false;
        BlockState state = level.getBlockState(target);
        if (state.isAir() || state.getDestroySpeed(level, target) < 0) return false;
        Player actor = actor(server);
        List<ItemStack> drops = Block.getDrops(state, server, target, level.getBlockEntity(target), actor, ItemStack.EMPTY);
        if (!dropsFit(drops) || !breakAllowed(server, target, state, actor)) return false;
        if (!level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) return false;
        for (ItemStack drop : drops) context.insertOutput(drop);
        breaksLeft--;
        setChanged();
        return true;
    }

    private int collect(int radius, boolean voidThem) {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel server)) return 0;
        if (!BehaviorRangeRules.radiusAllowed(declaration.maxRange(), radius) || itemsLeft <= 0) return 0;
        var box = new AABB(worldPosition.getX() - radius, worldPosition.getY() - radius, worldPosition.getZ() - radius,
                worldPosition.getX() + radius + 1.0, worldPosition.getY() + radius + 1.0, worldPosition.getZ() + radius + 1.0);
        int taken = 0;
        for (ItemEntity entity : server.getEntitiesOfClass(ItemEntity.class, box)) {
            if (itemsLeft <= 0) break;
            if (!entity.isAlive()) continue;
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) continue;
            if (voidThem) {
                entity.discard();
            } else {
                int before = stack.getCount();
                ItemStack left = context.insertOutput(stack.copy());
                if (left.getCount() == before) continue;
                if (left.isEmpty()) entity.discard();
                else entity.setItem(left);
            }
            itemsLeft--;
            taken++;
        }
        if (taken > 0) setChanged();
        return taken;
    }

    private boolean dropsFit(List<ItemStack> drops) {
        ItemStack[] before = new ItemStack[items.getSlots()];
        for (int i = 0; i < before.length; i++) before[i] = items.getStackInSlot(i).copy();
        boolean ok = true;
        for (ItemStack drop : drops) {
            if (drop == null || drop.isEmpty()) continue;
            if (!context.insertOutput(drop).isEmpty()) { ok = false; break; }
        }
        for (int i = 0; i < before.length; i++) items.setStackInSlot(i, before[i]);
        return ok;
    }

    private Player actor(ServerLevel server) {
        if (placerId != null && server.getServer() != null) {
            ServerPlayer online = server.getServer().getPlayerList().getPlayer(placerId);
            if (online != null) return online;
        }
        UUID id = placerId == null ? new UUID(0, 0) : placerId;
        String name = placerName == null || placerName.isBlank() ? "gt6ce" : placerName;
        if (name.length() > 16) name = name.substring(0, 16);
        var profile = new com.mojang.authlib.GameProfile(id, name);
//? if >=1.21.1 {
/*        var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.get(server, profile);
*///?} else {
        var fake = net.minecraftforge.common.util.FakePlayerFactory.get(server, profile);
//?}
        fake.setPos(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
        return fake;
    }

    private static boolean breakAllowed(ServerLevel server, BlockPos pos, BlockState state, Player player) {
//? if >=1.21.1 {
/*        var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(server, pos, state, player);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
*///?} else {
        var event = new net.minecraftforge.event.level.BlockEvent.BreakEvent(server, pos, state, player);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
//?}
    }

    private boolean insertable(int slot) {
        var role = declaration.items().get(slot).role();
        return role == BehaviorMachine.SlotRole.INPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private boolean extractable(int slot) {
        var role = declaration.items().get(slot).role();
        return role == BehaviorMachine.SlotRole.OUTPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private boolean fillable(int tank) {
        var role = declaration.fluids().get(tank).role();
        return role == BehaviorMachine.SlotRole.INPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private boolean drainable(int tank) {
        var role = declaration.fluids().get(tank).role();
        return role == BehaviorMachine.SlotRole.OUTPUT || role == BehaviorMachine.SlotRole.ANY;
    }

    private final class Context implements MachineBehaviorContext {
        @Override public Level level() { return BehaviorMachineBlockEntity.this.level; }
        @Override public BlockPos pos() { return worldPosition; }
        @Override public Direction facing() {
            return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        }
        @Override public int tier() { return declaration.casing().tier(); }
        @Override public int itemSlots() { return items.getSlots(); }
        @Override public ItemStack item(int slot) { return items.getStackInSlot(slot); }
        @Override public void setItem(int slot, ItemStack stack) { items.setStackInSlot(slot, stack == null ? ItemStack.EMPTY : stack.copy()); }
        @Override public ItemStack insertOutput(ItemStack stack) {
            ItemStack left = stack == null ? ItemStack.EMPTY : stack.copy();
            for (int slot = 0; slot < items.getSlots() && !left.isEmpty(); slot++) {
                if (declaration.items().get(slot).role() == BehaviorMachine.SlotRole.OUTPUT)
                    left = items.insertItem(slot, left, false);
            }
            for (int slot = 0; slot < items.getSlots() && !left.isEmpty(); slot++) {
                if (declaration.items().get(slot).role() == BehaviorMachine.SlotRole.ANY)
                    left = items.insertItem(slot, left, false);
            }
            return left;
        }
        @Override public int tanks() { return tanks.length; }
        @Override public FluidStack fluid(int tank) { return tanks[tank].copy(); }
        @Override public int capacity(int tank) { return tanks[tank].capacity; }
        @Override public int fill(int tank, FluidStack stack, boolean simulate) {
            int taken = tanks[tank].fill(stack, simulate);
            if (!simulate && taken > 0) setChanged();
            return taken;
        }
        @Override public FluidStack drain(int tank, int amount, boolean simulate) {
            FluidStack drained = tanks[tank].drain(amount, simulate);
            if (!simulate && !drained.isEmpty()) setChanged();
            return drained;
        }
        @Override public long energy() { return hasInput() ? stored : 0; }
        @Override public boolean consume(long amount) {
            if (amount < 0) throw new IllegalArgumentException("negative energy");
            if (!hasInput() || stored < amount) return false;
            stored -= amount;
            setChanged();
            return true;
        }
        @Override public long energy(String type) {
            if (declaration.auxiliary() == null || !sameEnergy(type, declaration.auxiliary().type())) return 0;
            return auxiliaryStored;
        }
        @Override public boolean consume(String type, long amount) {
            if (amount < 0) throw new IllegalArgumentException("negative energy");
            if (declaration.auxiliary() == null || !sameEnergy(type, declaration.auxiliary().type())) return false;
            if (auxiliaryStored < amount) return false;
            auxiliaryStored -= amount;
            setChanged();
            return true;
        }
        @Override public long generate(long amount) {
            if (amount < 0) throw new IllegalArgumentException("negative energy");
            long room = bufferCapacity() - Math.min(stored, bufferCapacity());
            long added = Math.min(amount, Math.max(0, room));
            if (added <= 0) return 0;
            stored += added;
            setChanged();
            return added;
        }
        @Override public long capacity() { return capacity; }
        @Override public void setCapacity(long value) {
            if (value < 0) throw new IllegalArgumentException("gt.capacity " + value);
            capacity = value;
            capacitySet = true;
            setChanged();
        }
        @Override public long emit(long size, long amount) { return BehaviorMachineBlockEntity.this.emit(size, amount); }
        @Override public long emitTo(BlockPos target, Direction side, long size, long amount) {
            return BehaviorMachineBlockEntity.this.emitTo(target, side, size, amount);
        }
        @Override public long pushToPart(String part, String type, long size, long amount) {
            return BehaviorMachineBlockEntity.this.pushToPart(part, type, size, amount);
        }
        @Override public long emit(String part, long size, long amount) {
            return BehaviorMachineBlockEntity.this.emitToPart(part, size, amount);
        }
        @Override public boolean toggle(int index) { return (toggles & (1 << index)) != 0; }
        @Override public void setToggle(int index, boolean on) {
            if (index < 0 || index >= declaration.toggles().size())
                throw new IllegalArgumentException("toggle " + index);
            toggles = on ? toggles | (1 << index) : toggles & ~(1 << index);
            setChanged();
        }
        @Override public float progress() { return progress / 1000.0F; }
        @Override public void progress(float fraction) {
            float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
            int next = Math.round(clamped * 1000.0F);
            if (next != progress) {
                progress = next;
                setChanged();
            }
        }
        @Override public void working(boolean value) { working = value; }
        @Override public boolean structureFormed() { return formed; }
        @Override public List<EnergyPart> energyParts() { return formed ? energyParts : List.of(); }
        @Override public int structureRepeats(String key) { return formed && key != null ? repeatCounts.getOrDefault(key, 0) : 0; }
        @Override public List<BoundPart> parts() { return formed ? boundParts : List.of(); }
        @Override public boolean conditionsMet() {
            for (EnergyPart part : energyParts()) if (part.condition() && !part.satisfied()) return false;
            return true;
        }
        @Override public BehaviorState state() { return state; }
        @Override public boolean breakBlock(BlockPos pos) { return breakOne(pos); }
        @Override public int collectItems(int radius, boolean voidThem) { return collect(radius, voidThem); }
        @Override public int pushCompressedGas(Direction face, FluidStack stack) {
            if (face == null || stack == null || stack.isEmpty() || level == null) return 0;
            BlockPos target = worldPosition.relative(face);
            if (!level.hasChunkAt(target)) return 0;
            if (!(level.getBlockEntity(target) instanceof gregtech.internal.fluid.CompressedGasReceiver receiver))
                return 0;
            return receiver.receiveCompressedGas(face.getOpposite(), stack);
        }
    }

    @Override public int receiveCompressedGas(Direction side, FluidStack stack) {
        if (!valveFace(side) || stack == null || stack.isEmpty()
                || !gregtech.internal.fluid.FluidHazards.isGas(stack.getFluid())) return 0;
        FluidStack remaining = stack.copy();
        int stored = 0;
        for (Tank tank : tanks) {
            if (remaining.isEmpty()) break;
            int taken = tank.fill(remaining, true);
            if (taken <= 0) continue;
            tank.fill(remaining, false);
            stored += taken;
            remaining.setAmount(remaining.getAmount() - taken);
        }
        if (stored > 0) setChanged();
        return stored;
    }

    @Override public FluidStack extractCompressedGas(Direction side, int amount, boolean execute) {
        if (!valveFace(side) || amount <= 0) return FluidStack.EMPTY;
        for (Tank tank : tanks) {
            FluidStack preview = tank.drain(amount, true);
            if (preview.isEmpty() || !gregtech.internal.fluid.FluidHazards.isGas(preview.getFluid())) continue;
            if (!execute) return preview;
            FluidStack taken = tank.drain(amount, false);
            if (!taken.isEmpty()) setChanged();
            return taken;
        }
        return FluidStack.EMPTY;
    }

    private boolean valveFace(Direction side) {
        MachineFace valve = declaration.gasValve();
        if (valve == null || side == null) return false;
        Direction facing = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        return PartFaces.relative(facing, side) == valve;
    }

    public LargeMachineLayouts.Layout trackedLayout() {
        return LargeMachineLayouts.layout(declaration.casing().machineType());
    }

    public boolean acceptsTrackedPart(LargeMachineLayouts.Cell cell, net.minecraft.world.level.block.Block expected,
                                      net.minecraft.world.level.block.Block actual) {
        return expected == actual;
    }

    @Override public gregtech.internal.multiblock.tracking.StructureDefinition structureDefinition() {
        return gregtech.internal.multiblock.tracking.LayoutStructures.behavior(this);
    }

    @Override public boolean isStructureOk() {
        return gregtech.internal.multiblock.tracking.StructureTracking.query(this);
    }

    @Override public boolean scanStructure() {
        if (checkingStructure) return false;
        checkingStructure = true;
        try {
            if (level == null || isRemoved()) {
                structureBindings.clear(this::releaseStructure);
                clearStructureCaches();
                return false;
            }
            var layout = trackedLayout();
            if (layout == null) return true;
            boolean formedNow = claimStructure(layout);
            if (!formedNow) clearStructureCaches();
            return formedNow;
        } finally {
            checkingStructure = false;
        }
    }

    @Override public void structureFormedChanged(boolean now) {
        formed = now;
        if (!now) clearStructureCaches();
    }

    private void clearStructureCaches() {
        energyParts = List.of();
        boundParts = List.of();
        repeatCounts = Map.of();
    }

    private void refreshStructure() {
        formed = scanStructure();
    }

    private boolean claimStructure(LargeMachineLayouts.Layout layout) {
        if (level == null || isRemoved()) {
            structureBindings.clear(this::releaseStructure);
            return false;
        }
        Direction front = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        var parts = new LinkedHashMap<BlockPos, MultiblockLayout.Role>();
        var matchedCells = new ArrayList<LargeMachineLayouts.Cell>();
        for (var cell : layout.cells()) {
            BlockPos cellPos = cell.at(worldPosition, front);
            var matched = matchingCell(cell, cellPos);
            if (matched == null) {
                structureBindings.clear(this::releaseStructure);
                gregtech.internal.multiblock.tracking.StructureClaims.cleared(this);
                return false;
            }
            matchedCells.add(matched);
            if (matched.role() != MultiblockLayout.Role.AIR && !matched.plainBlock())
                parts.put(cellPos, matched.role());
        }
        var repeatPlaced = new ArrayList<LargeMachineLayouts.Placed>();
        var counts = new LinkedHashMap<String, Integer>();
        for (var repeat : layout.repeats()) {
            var matches = new HashMap<BlockPos, LargeMachineLayouts.Cell>();
            var placed = LargeMachineLayouts.repeatCells(level, worldPosition, front, repeat, (cell, pos) -> {
                var matched = matchingCell(cell, pos);
                if (matched == null) return false;
                matches.put(pos, matched);
                return true;
            });
            if (placed == null) {
                structureBindings.clear(this::releaseStructure);
                gregtech.internal.multiblock.tracking.StructureClaims.cleared(this);
                return false;
            }
            counts.put(repeat.key(), placed.size() / repeat.cells().size());
            for (var cell : placed) {
                var matched = matches.get(cell.pos());
                repeatPlaced.add(new LargeMachineLayouts.Placed(matched, cell.pos()));
                if (matched.role() != MultiblockLayout.Role.AIR && !matched.plainBlock())
                    parts.put(cell.pos(), matched.role());
            }
        }
        var claimed = gregtech.internal.multiblock.tracking.StructureClaims.update(this, true, structureBindings, parts,
                behaviorDesignHash(matchedCells, repeatPlaced),
                pos -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).canBind(worldPosition),
                (pos, role) -> ((MultiblockPortBlockEntity) level.getBlockEntity(pos)).bind(worldPosition, role),
                this::releaseStructure);
        if (claimed == gregtech.internal.multiblock.tracking.StructureClaims.Result.FAILED) return false;
        if (claimed == gregtech.internal.multiblock.tracking.StructureClaims.Result.APPLIED) {
            gregtech.content.multiblock.StructureDesigns.apply(level, worldPosition, front, matchedCells);
            gregtech.content.multiblock.StructureDesigns.apply(level, repeatPlaced);
        }
        var found = new ArrayList<EnergyPart>();
        var bound = new ArrayList<BoundPart>();
        for (BlockPos cellPos : parts.keySet()) {
            var occupant = level.getBlockEntity(cellPos);
            if (occupant instanceof AddonEnergyPartBlockEntity energy) {
                found.add(new EnergyPartView(energy));
                bound.add(new BoundView(energy));
            } else if (occupant instanceof AddonFunctionalPartBlockEntity functional) {
                bound.add(new BoundView(functional));
            }
        }
        energyParts = List.copyOf(found);
        boundParts = List.copyOf(bound);
        repeatCounts = Map.copyOf(counts);
        return true;
    }

    private static int behaviorDesignHash(List<LargeMachineLayouts.Cell> cells, List<LargeMachineLayouts.Placed> placed) {
        int hash = 1;
        for (var cell : cells) hash = 31 * hash + cell.design();
        for (var cell : placed) hash = 31 * hash + cell.cell().design();
        return hash;
    }

    private LargeMachineLayouts.Cell matchingCell(LargeMachineLayouts.Cell cell, BlockPos cellPos) {
        gregtech.internal.multiblock.tracking.StructureScanProbe.matcher();
        if (!level.hasChunkAt(cellPos)) return null;
        if (cell.role() == MultiblockLayout.Role.AIR) return level.isEmptyBlock(cellPos) ? cell : null;
        if (matchesChoice(cell.part(), cell.plain(), cellPos)) return cell;
        for (var alternative : cell.alternatives())
            if (matchesChoice(alternative.part(), alternative.plain(), cellPos))
                return new LargeMachineLayouts.Cell(cell.right(), cell.up(), cell.back(), alternative.part(),
                        cell.role(), alternative.plain(), cell.design(), List.of());
        return null;
    }

    private boolean matchesChoice(int part, String plain, BlockPos cellPos) {
        if (plain != null) return LargeMachineLayouts.matchesPlain(level, cellPos, plain);
        return level.getBlockState(cellPos).getBlock()
                == gregtech.content.multiblock.LargeMachineParts.block(part)
                && level.getBlockEntity(cellPos) instanceof MultiblockPortBlockEntity;
    }

    private void releaseStructure(BlockPos pos) {
        if (level != null && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity part)
            part.release(worldPosition);
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            gregtech.internal.multiblock.tracking.StructureTracking.attach(this);
    }

    @Override public void onChunkUnloaded() {
        gregtech.internal.multiblock.tracking.StructureTracking.onChunkUnloaded(this);
        super.onChunkUnloaded();
    }

    @Override public void setRemoved() {
        if (!gregtech.internal.multiblock.tracking.StructureTracking.unloading(this))
            structureBindings.clear(this::releaseStructure);
        gregtech.internal.multiblock.tracking.StructureTracking.onRemoved(this);
        energyParts = List.of();
        boundParts = List.of();
        repeatCounts = Map.of();
        formed = false;
        super.setRemoved();
    }

    private final class EnergyPartView implements EnergyPart {
        private final AddonEnergyPartBlockEntity part;
        private EnergyPartView(AddonEnergyPartBlockEntity part) { this.part = part; }
        @Override public BlockPos pos() { return part.getBlockPos(); }
        @Override public String name() { return part.path(); }
        @Override public long stored() { return part.stored(); }
        @Override public long capacity() { return part.capacity(); }
        @Override public long charge(long amount) {
            return part.isBoundTo(worldPosition) ? part.charge(amount) : 0;
        }
        @Override public void setOutput(long size, long amount) {
            if (!part.isBoundTo(worldPosition))
                throw new IllegalStateException("Energy part is not bound to this machine");
            part.setOutput(size, amount);
        }
        @Override public void stop() {
            if (part.isBoundTo(worldPosition)) part.stop();
        }
        @Override public boolean condition() { return part.condition(); }
        @Override public boolean satisfied() { return part.satisfied(); }
        private boolean controllerInput() {
            var declaration = part.declaration();
            return declaration.energy() != null && declaration.energyInput() != null;
        }
        private String outputType() { return part.outputType(); }
    }

    private final class BoundView implements BoundPart {
        private final MultiblockPortBlockEntity port;
        @Nullable private final AddonEnergyPartBlockEntity energy;
        @Nullable private final FunctionalPartRuntime function;

        private BoundView(AddonEnergyPartBlockEntity energy) {
            this.port = energy;
            this.energy = energy;
            this.function = energy.function();
        }

        private BoundView(AddonFunctionalPartBlockEntity functional) {
            this.port = functional;
            this.energy = null;
            this.function = functional.function();
        }

        private boolean live() { return !port.isRemoved() && port.isBoundTo(worldPosition); }

        @Override public BlockPos pos() { return port.getBlockPos(); }

        @Override public String name() {
            if (energy != null) return energy.path();
            return port instanceof AddonFunctionalPartBlockEntity functional ? functional.name() : "";
        }

        @Override public EnergyPart energy() { return energy != null && live() ? new EnergyPartView(energy) : null; }

        @Override public PartBehavior behavior() { return live() && function != null ? function.behavior() : null; }

        @Override public PartContext context() { return behavior() == null ? null : function.context(); }

        @Override public int slots() { return live() && function != null ? function.slotCount() : 0; }

        @Override public ItemStack item(int slot) {
            return live() && function != null ? function.item(slot) : ItemStack.EMPTY;
        }
    }

    private final class PipeItems implements IItemHandler {
        @Nullable private final MachineFace face;
        private final boolean allowInsert;
        private final boolean allowExtract;

        private PipeItems() { this(null, true, true); }

        private PipeItems(@Nullable MachineFace face, boolean allowInsert, boolean allowExtract) {
            this.face = face;
            this.allowInsert = allowInsert;
            this.allowExtract = allowExtract;
        }

        @Override public int getSlots() { return items.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!allowInsert || !insertable(slot) || stack == null || stack.isEmpty()
                    || !behavior.canInsert(context, slot, stack, face)) return stack;
            return items.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return allowExtract && extractable(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return allowInsert && insertable(slot) && behavior.canInsert(context, slot, stack, face);
        }
    }

    private final class PipeFluids implements IFluidHandler {
        @Nullable private final MachineFace face;
        private final boolean allowFill;
        private final boolean allowDrain;

        private PipeFluids() { this(null, true, true); }

        private PipeFluids(@Nullable MachineFace face, boolean allowFill, boolean allowDrain) {
            this.face = face;
            this.allowFill = allowFill;
            this.allowDrain = allowDrain;
        }

        @Override public int getTanks() { return tanks.length; }
        @Override public FluidStack getFluidInTank(int tank) { return tanks[tank].copy(); }
        @Override public int getTankCapacity(int tank) { return tanks[tank].capacity; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return allowFill && fillable(tank) && behavior.canFill(context, tank, stack, face);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!allowFill || resource == null || resource.isEmpty()) return 0;
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!fillable(tank) || !behavior.canFill(context, tank, resource, face)) continue;
                int taken = behavior.acceptFluid(context, tank, resource, action.simulate());
                if (taken > 0) return taken;
            }
            return 0;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!allowDrain || resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!drainable(tank) || tanks[tank].fluid != resource.getFluid()) continue;
                FluidStack drained = tanks[tank].drain(resource.getAmount(), action.simulate());
                if (!action.simulate() && !drained.isEmpty()) setChanged();
                return drained;
            }
            return FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            if (!allowDrain) return FluidStack.EMPTY;
            for (int tank = 0; tank < tanks.length; tank++) {
                if (!drainable(tank)) continue;
                FluidStack drained = tanks[tank].drain(maxDrain, action.simulate());
                if (!drained.isEmpty()) {
                    if (!action.simulate()) setChanged();
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }
    }

    private static final class Tank {
        final int capacity;
        Fluid fluid = Fluids.EMPTY;
        int amount;

        Tank(BehaviorMachine.FluidSlot slot) { this.capacity = slot.capacity(); }

        FluidStack copy() {
            return amount <= 0 || fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
        }

        int fill(FluidStack stack, boolean simulate) {
            if (stack == null || stack.isEmpty()) return 0;
            if (amount > 0 && fluid != stack.getFluid()) return 0;
            int taken = Math.min(capacity - amount, stack.getAmount());
            if (taken <= 0) return 0;
            if (!simulate) {
                fluid = stack.getFluid();
                amount += taken;
            }
            return taken;
        }

        FluidStack drain(int requested, boolean simulate) {
            if (amount <= 0 || requested <= 0 || fluid == Fluids.EMPTY) return FluidStack.EMPTY;
            int taken = Math.min(amount, requested);
            Fluid drained = fluid;
            if (!simulate) {
                amount -= taken;
                if (amount == 0) fluid = Fluids.EMPTY;
            }
            return new FluidStack(drained, taken);
        }
    }

    private final class StoredState implements BehaviorState {
        final Map<String, Integer> ints = new LinkedHashMap<>();
        final Map<String, Long> longs = new LinkedHashMap<>();
        final Map<String, Boolean> bools = new LinkedHashMap<>();
        final Map<String, String> strings = new LinkedHashMap<>();
        final Map<String, ItemStack> items = new LinkedHashMap<>();

        @Override public int getInt(String key) { return ints.getOrDefault(check(key), 0); }
        @Override public void setInt(String key, int value) {
            String checked = check(key);
            Integer previous = ints.put(checked, value);
            if (previous == null || previous != value) clientChanged(checked);
            setChanged();
        }
        @Override public long getLong(String key) { return longs.getOrDefault(check(key), 0L); }
        @Override public void setLong(String key, long value) {
            String checked = check(key);
            Long previous = longs.put(checked, value);
            if (previous == null || previous != value) clientChanged(checked);
            setChanged();
        }
        @Override public boolean getBoolean(String key) { return bools.getOrDefault(check(key), false); }
        @Override public void setBoolean(String key, boolean value) {
            String checked = check(key);
            Boolean previous = bools.put(checked, value);
            if (previous == null || previous != value) clientChanged(checked);
            setChanged();
        }
        @Override public String getString(String key) { return strings.getOrDefault(check(key), ""); }
        @Override public void setString(String key, String value) {
            if (value == null) throw new IllegalArgumentException("null behavior string");
            if (value.length() > 256) throw new IllegalArgumentException("behavior string is too long");
            String checked = check(key);
            if (!value.equals(strings.put(checked, value))) clientChanged(checked);
            setChanged();
        }
        @Override public ItemStack item(String key) {
            ItemStack stack = items.get(check(key));
            return stack == null ? ItemStack.EMPTY : stack.copy();
        }
        @Override public void setItem(String key, ItemStack stack) {
            String checked = check(key);
            if (stack == null || stack.isEmpty()) items.remove(checked);
            else items.put(checked, stack.copy());
            setChanged();
        }
        private void clientChanged(String key) {
            if (clientKeys.contains(key)) clientStateDirty = true;
        }

//? if >=1.21.1 {
/*        CompoundTag save(HolderLookup.Provider lookup) {
*///?} else {
        CompoundTag save() {
//?}
            CompoundTag tag = new CompoundTag();
            CompoundTag intTag = new CompoundTag();
            ints.forEach(intTag::putInt);
            CompoundTag longTag = new CompoundTag();
            longs.forEach(longTag::putLong);
            CompoundTag boolTag = new CompoundTag();
            bools.forEach(boolTag::putBoolean);
            CompoundTag stringTag = new CompoundTag();
            strings.forEach(stringTag::putString);
            CompoundTag itemTag = new CompoundTag();
//? if >=1.21.1 {
/*            items.forEach((key, stack) -> { if (!stack.isEmpty()) itemTag.put(key, stack.save(lookup)); });
*///?} else {
            items.forEach((key, stack) -> { if (!stack.isEmpty()) itemTag.put(key, stack.save(new CompoundTag())); });
//?}
            tag.put("i", intTag);
            tag.put("l", longTag);
            tag.put("b", boolTag);
            tag.put("s", stringTag);
            tag.put("t", itemTag);
            return tag;
        }

//? if >=1.21.1 {
/*        void load(HolderLookup.Provider lookup, CompoundTag tag) {
*///?} else {
        void load(CompoundTag tag) {
//?}
            ints.clear(); longs.clear(); bools.clear(); strings.clear(); items.clear();
            CompoundTag intTag = tag.getCompound("i");
            for (String key : intTag.getAllKeys()) ints.put(key, intTag.getInt(key));
            CompoundTag longTag = tag.getCompound("l");
            for (String key : longTag.getAllKeys()) longs.put(key, longTag.getLong(key));
            CompoundTag boolTag = tag.getCompound("b");
            for (String key : boolTag.getAllKeys()) bools.put(key, boolTag.getBoolean(key));
            CompoundTag stringTag = tag.getCompound("s");
            for (String key : stringTag.getAllKeys()) strings.put(key, stringTag.getString(key));
            CompoundTag itemTag = tag.getCompound("t");
            for (String key : itemTag.getAllKeys()) {
//? if >=1.21.1 {
/*                ItemStack stack = ItemStack.parseOptional(lookup, itemTag.getCompound(key));
*///?} else {
                ItemStack stack = ItemStack.of(itemTag.getCompound(key));
//?}
                if (!stack.isEmpty()) items.put(key, stack);
            }
        }

        private static String check(String key) {
            if (key == null || !key.matches("[a-z0-9_.]{1,64}"))
                throw new IllegalArgumentException("Invalid behavior state key: " + key);
            return key;
        }
    }

    // ── Structure ports ──────────────────────────────────────────────────

    private static boolean itemInput(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ITEM_IO || role == MultiblockLayout.Role.ITEM_FLUID_IO
                || role == MultiblockLayout.Role.ITEM_FLUID_INPUT || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY_INPUT
                || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY;
    }

    private static boolean itemOutput(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ITEM_IO || role == MultiblockLayout.Role.ITEM_FLUID_IO
                || role == MultiblockLayout.Role.ITEM_FLUID_OUTPUT || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY;
    }

    private static boolean fluidInput(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.FLUID_INPUT || role == MultiblockLayout.Role.FLUID_IO
                || role == MultiblockLayout.Role.ITEM_FLUID_IO || role == MultiblockLayout.Role.ITEM_FLUID_INPUT
                || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY_INPUT || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY;
    }

    private static boolean fluidOutput(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.FLUID_OUTPUT || role == MultiblockLayout.Role.FLUID_IO
                || role == MultiblockLayout.Role.ITEM_FLUID_IO || role == MultiblockLayout.Role.ITEM_FLUID_OUTPUT
                || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY;
    }

    private static boolean energyInput(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ENERGY_INPUT || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY_INPUT
                || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY;
    }

    private static boolean energyOutput(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.ENERGY_OUTPUT || role == MultiblockLayout.Role.ITEM_FLUID_ENERGY;
    }

    private boolean itemSlotAllows(int slot, boolean input) {
        if (slot < 0 || slot >= declaration.items().size()) return false;
        var role = declaration.items().get(slot).role();
        return role == BehaviorMachine.SlotRole.ANY
                || role == (input ? BehaviorMachine.SlotRole.INPUT : BehaviorMachine.SlotRole.OUTPUT);
    }

    private boolean tankAllows(int tank, boolean input) {
        if (tank < 0 || tank >= declaration.fluids().size()) return false;
        var role = declaration.fluids().get(tank).role();
        return role == BehaviorMachine.SlotRole.ANY
                || role == (input ? BehaviorMachine.SlotRole.INPUT : BehaviorMachine.SlotRole.OUTPUT);
    }

    @Override public IItemHandler portItems(MultiblockLayout.Role role) {
        if (!itemInput(role) && !itemOutput(role)) return null;
        return new IItemHandler() {
            @Override public int getSlots() { return items.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return itemInput(role) && itemSlotAllows(slot, true) ? items.insertItem(slot, stack, simulate) : stack;
            }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return itemOutput(role) && itemSlotAllows(slot, false)
                        ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }
            @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return itemInput(role) && itemSlotAllows(slot, true) && items.isItemValid(slot, stack);
            }
        };
    }

    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) {
        if ((!fluidInput(role) && !fluidOutput(role)) || tanks.length == 0) return null;
        return new IFluidHandler() {
            @Override public int getTanks() { return tanks.length; }
            @Override public FluidStack getFluidInTank(int tank) { return tanks[tank].copy(); }
            @Override public int getTankCapacity(int tank) { return tanks[tank].capacity; }
            @Override public boolean isFluidValid(int tank, FluidStack stack) {
                return fluidInput(role) && tankAllows(tank, true) && tanks[tank].fill(stack, true) > 0;
            }
            @Override public int fill(FluidStack stack, FluidAction action) {
                if (!fluidInput(role) || stack == null || stack.isEmpty()) return 0;
                int remaining = stack.getAmount();
                int filled = 0;
                for (int tank = 0; tank < tanks.length && remaining > 0; tank++) {
                    if (!tankAllows(tank, true)) continue;
                    int taken = tanks[tank].fill(new FluidStack(stack.getFluid(), remaining), action.simulate());
                    filled += taken;
                    remaining -= taken;
                }
                return filled;
            }
            @Override public FluidStack drain(FluidStack stack, FluidAction action) {
                if (!fluidOutput(role) || stack == null || stack.isEmpty()) return FluidStack.EMPTY;
                return drainMatching(stack.getFluid(), stack.getAmount(), action.simulate());
            }
            @Override public FluidStack drain(int amount, FluidAction action) {
                if (!fluidOutput(role) || amount <= 0) return FluidStack.EMPTY;
                return drainMatching(null, amount, action.simulate());
            }
            private FluidStack drainMatching(Fluid fluid, int amount, boolean simulate) {
                FluidStack out = FluidStack.EMPTY;
                int remaining = amount;
                for (int tank = 0; tank < tanks.length && remaining > 0; tank++) {
                    if (!tankAllows(tank, false)) continue;
                    if (fluid != null && tanks[tank].fluid != fluid) continue;
                    FluidStack got = tanks[tank].drain(remaining, simulate);
                    if (got.isEmpty()) continue;
                    if (out.isEmpty()) out = got.copy();
                    else out.grow(got.getAmount());
                    remaining -= got.getAmount();
                }
                return out;
            }
        };
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
                var tag = tagFor(declaration.energy().type());
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
        if (!formed || !energyInput(role) || type == null) return 0;
        if (auxiliaryTag != null && type == auxiliaryTag)
            return injectAuxiliary(type.getShortName(), null, size, amount, execute);
        return doInject(type.getShortName(), null, size, amount, execute);
    }
}
