/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. Source boiler tool and structure rules. */
package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.multiblock.MultiblockLayout;
import com.gregtech.gregtech.api.multiblock.MultiblockPortOwner;
import com.gregtech.gregtech.api.multiblock.PartBindings;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.machine.OriginalLargeBoilerControllerBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerSpecs;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * GT6 MultiTileEntityLargeBoiler, for the five original numeric controller variants.
 * Their wall material and rated output are fixed by the placed controller item.
 */
public final class OriginalLargeBoilerControllerBlockEntity extends GTEnergyBlockEntity
        implements MultiblockPortOwner, com.gregtech.gregtech.api.multiblock.MultiblockToolTarget, com.gregtech.gregtech.api.sensor.CompressionSensorSource {
    private static final int WATER_CAPACITY = com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerParameters.WATER_CAPACITY;
    private static final int HU_PER_WATER = com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerParameters.HU_PER_WATER;
    private static final int STEAM_PER_WATER = com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerParameters.STEAM_PER_WATER;

    private final OriginalLargeBoilerSpecs.Variant variant;
    private final PartBindings<BlockPos, MultiblockLayout.Role> bindings = new PartBindings<>();
    private final FluidTankGT water = new FluidTankGT(WATER_CAPACITY).setOnChanged(this::setChanged);
    private final FluidTankGT steam;
    private long heat;
    private int efficiency = 10_000;
    private int cooldown = 128;
    private int barometer;
    private boolean structureOkay;
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.empty();

    public OriginalLargeBoilerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.ORIGINAL_LARGE_BOILER.get(), pos, state);
        variant = ((OriginalLargeBoilerControllerBlock) state.getBlock()).variant();
        steam = new FluidTankGT(variant.steamCapacity()).setOnChanged(this::setChanged);
    }

    public OriginalLargeBoilerSpecs.Variant variant() { return variant; }
    public long storedHeat() { return heat; }
    public long waterAmount() { return water.getAmount(); }
    @Override public long gibblValue(int side) { return steam.getAmount(); }
    @Override public long gibblMaximum(int side) { return steam.getCapacity(); }
    public long steamAmount() { return steam.getAmount(); }
    public int efficiency() { return efficiency; }
    public int barometerValue() { return barometer; }
    public IFluidHandler directFluidHandler() { return controllerFluids; }

    private Direction front() { return getBlockState().getValue(HorizontalDirectionalBlock.FACING); }
    private BlockPos bodyCentre() { return worldPosition.relative(front().getOpposite()); }

    /** Source claims each correct cell even when the shell has missing or obstructed cells. */
    @Override public boolean isStructureOk() {
        if(level==null||isRemoved())return false;
        if(level.isClientSide)return structureOkay;
        var centre=bodyCentre();
        var parts=new LinkedHashMap<BlockPos,MultiblockLayout.Role>();
        boolean complete=true;
        for(var cell:com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerParameters.CHECK_ORDER) {
            var position=centre.offset(cell.x(),cell.y(),cell.z());
            if(!level.hasChunkAt(position)){complete=false;continue;}
            if(position.equals(worldPosition))continue;
            if(cell.role()==com.gregtech.gregtech.api.multiblock.StructureGrid.Role.AIR) {
                if(!level.getBlockState(position).isAir())complete=false;
                continue;
            }
            var required=cell.role()==com.gregtech.gregtech.api.multiblock.StructureGrid.Role.HEAT_INPUT
                    ?GTMultiblocks.HEAT_TRANSMITTER.get():variant.wall();
            if(!level.getBlockState(position).is(required)
                    ||!(level.getBlockEntity(position) instanceof MultiblockPortBlockEntity port)||!port.canBind(worldPosition)) {
                complete=false;continue;
            }
            parts.put(position,MultiblockLayout.Role.valueOf(cell.role().name()));
        }
        boolean claimed=bindings.update(parts,
                position->level.getBlockEntity(position) instanceof MultiblockPortBlockEntity port&&port.canBind(worldPosition),
                (position,role)->((MultiblockPortBlockEntity)level.getBlockEntity(position)).bind(worldPosition,role),this::release);
        boolean formed=complete&&claimed;
        if(structureOkay!=formed){structureOkay=formed;syncToClient();}
        return formed;
    }
    @Override public boolean containsToolPosition(BlockPos pos) {
        var centre=bodyCentre();
        return com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerParameters.contains(
                pos.getX()-centre.getX(),pos.getY()-centre.getY(),pos.getZ()-centre.getZ());
    }
    @Override public long useMultiblockTool(net.minecraft.world.item.context.UseOnContext context,List<Component> messages) {
        if(level==null||level.isClientSide||isRemoved()||!containsToolPosition(context.getClickedPos()))return 0;
        if(GTToolHelper.matchesTool(context.getItemInHand(),GTToolType.BUILDER_WAND)) {
            var centre=bodyCentre();
            for(var cell:com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerParameters.CHECK_ORDER) {
                var position=centre.offset(cell.x(),cell.y(),cell.z());
                if(position.equals(worldPosition)||cell.role()==com.gregtech.gregtech.api.multiblock.StructureGrid.Role.AIR)continue;
                var block=cell.role()==com.gregtech.gregtech.api.multiblock.StructureGrid.Role.HEAT_INPUT
                        ?GTMultiblocks.HEAT_TRANSMITTER.get():variant.wall();
                com.gregtech.gregtech.api.multiblock.MultiblockTools.build(context,position,block);
            }
            isStructureOk();
            return com.gregtech.gregtech.api.multiblock.MultiblockToolRules.BUILDER_COST;
        }
        if(GTToolHelper.matchesTool(context.getItemInHand(),GTToolType.MAGNIFYING_GLASS)) {
            messages.addAll(magnifyingGlassMessages());
            return com.gregtech.gregtech.api.multiblock.MultiblockToolRules.MAGNIFIER_COST;
        }
        return 0;
    }
    public List<Component> magnifyingGlassMessages() {
        boolean previous=structureOkay,formed=isStructureOk();
        if(!previous||!formed)return List.of(Component.literal(
                com.gregtech.gregtech.api.multiblock.MultiblockToolRules.structureMessage(previous,formed)));
        // This source override replaces the generic "formed already" line with boiler details.
        return List.of(Component.literal(com.gregtech.gregtech.content.multiblock.OriginalLargeBoilerParameters.calcificationMessage(efficiency)),
                com.gregtech.gregtech.api.multiblock.MultiblockTools.tankContent(water,"WARNING: NO WATER!!!"));
    }

    private void release(BlockPos position) {
        if (level != null && level.hasChunkAt(position)
                && level.getBlockEntity(position) instanceof MultiblockPortBlockEntity port)
            port.release(worldPosition);
    }
    @Override public void setRemoved() { bindings.clear(this::release); super.setRemoved(); }
    private boolean shellLoaded() {
        if (level == null) return false;
        for (var cell : OriginalLargeBoilerSpecs.LAYOUT.cells())
            if (!level.hasChunkAt(cell.at(worldPosition, front()))) return false;
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  OriginalLargeBoilerControllerBlockEntity boiler) {
        if (level.isClientSide) return;
        boolean formed = boiler.isStructureOk();
        if (formed) boiler.boil();
        // GT6 cools its heat and steam buffers even after a wall is removed.
        boiler.coolDown();
        if (!formed) {
            // GT6 bursts when a pressurised shell is damaged; an unloaded chunk is not damage.
            if (boiler.barometer > 4 && boiler.shellLoaded()) boiler.explode();
            return;
        }
        boiler.pushSteam();
        int pressure = (int) Math.min(31, boiler.steam.getAmount() * 31 / boiler.variant.steamCapacity());
        if (pressure != boiler.barometer) {
            boiler.barometer = pressure;
            boiler.syncToClient();
        }
        if (boiler.heat > boiler.variant.heatCapacity()
                || boiler.steam.getAmount() >= boiler.variant.steamCapacity()) boiler.explode();
    }

    private void boil() {
        if (heat < HU_PER_WATER || water.isEmpty() || level == null) return;
        var steamFluid = GTFluids.still("Steam");
        if (steamFluid == null || !steamFluid.isPresent()) return;
        // Source GT6 caps conversions by total steam capacity / 2560, not output space.
        long conversions = Math.min(variant.steamCapacity() / 2560,
                Math.min(heat / HU_PER_WATER, water.getAmount()));
        if (conversions <= 0) return;
        water.remove(conversions);
        if (efficiency > 5000 && !water.isEmpty() && !isDistilledWater()
                && level.random.nextInt(10) == 0)
            efficiency = (int) Math.max(5000, efficiency - conversions);
        long produced = conversions * STEAM_PER_WATER * efficiency / 10_000L;
        steam.setFluid(new FluidStack(steamFluid.get(), 1), steam.getAmount() + produced);
        heat -= conversions * HU_PER_WATER;
        cooldown = 128;
        setChanged();
    }
    private boolean isDistilledWater() {
        var distilled = GTFluids.still("DistW");
        return distilled != null && distilled.isPresent() && !water.isEmpty()
                && water.getFluid().getFluid().isSame(distilled.get());
    }
    private void coolDown() {
        if (cooldown-- > 0) return;
        cooldown = 0;
        heat = Math.max(0, heat - variant.steamOutput() * 32);
        steam.remove(variant.steamOutput() * 64);
        if (heat == 0) cooldown = 128;
        setChanged();
    }

    /** The source pushes to one top and four side openings only above half pressure. */
    private void pushSteam() {
        if (level == null || steam.getAmount() <= variant.steamCapacity() / 2) return;
        long excess = steam.getAmount() - variant.steamCapacity() / 2;
        long rate = excess > variant.steamCapacity() / 4
                ? variant.steamOutput() * 2 : variant.steamOutput();
        long budget = Math.min(rate, excess);
        BlockPos centre = bodyCentre();
        var targets = new java.util.ArrayList<Output>();
        addOutput(targets, centre.above(3), Direction.DOWN);
        for (Direction side : Direction.Plane.HORIZONTAL)
            addOutput(targets, centre.above().relative(side, 2), side.getOpposite());
        if (targets.isEmpty()) return;
        // Divide evenly, then use spare allowance where a receiver fills less than its share.
        long remaining = budget;
        int count = targets.size();
        for (Output target : targets) {
            int offer = (int) Math.min(Integer.MAX_VALUE, remaining / count);
            remaining -= moveSteam(target, offer);
            count--;
        }
        if (remaining > 0) for (Output target : targets) {
            int offer = (int) Math.min(Integer.MAX_VALUE, remaining);
            remaining -= moveSteam(target, offer);
            if (remaining <= 0) break;
        }
    }
    private record Output(IFluidHandler handler) {}
    private void addOutput(java.util.List<Output> outputs, BlockPos pos, Direction exposedFace) {
        if (level == null || !level.hasChunkAt(pos)) return;
        var entity = level.getBlockEntity(pos);
        if (entity == null) return;
        IFluidHandler handler = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, exposedFace)
                .resolve().orElse(null);
        if (handler != null) outputs.add(new Output(handler));
    }
    private int moveSteam(Output output, int offered) {
        if (offered <= 0) return 0;
        FluidStack stack = steam.drain(offered, IFluidHandler.FluidAction.SIMULATE);
        if (stack.isEmpty()) return 0;
        int possible = output.handler().fill(stack, IFluidHandler.FluidAction.SIMULATE);
        if (possible <= 0) return 0;
        stack.setAmount(Math.min(possible, stack.getAmount()));
        int accepted = output.handler().fill(stack, IFluidHandler.FluidAction.EXECUTE);
        int moved = Math.max(0, Math.min(accepted, stack.getAmount()));
        steam.remove(moved);
        return moved;
    }

    /** No heat survives destruction. The source's pressure-dependent blast scales with steam. */
    public void explode() {
        if (level == null || level.isClientSide || isRemoved()) return;
        float power = (float) (2 + Math.max(1, Math.sqrt(steam.getAmount()) / 1000.0));
        level.removeBlock(worldPosition, false);
        level.explode(null, worldPosition.getX() + .5, worldPosition.getY() + .5,
                worldPosition.getZ() + .5, power, Level.ExplosionInteraction.BLOCK);
    }

    public InteractionResult onToolUse(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if(GTToolHelper.matchesTool(held,GTToolType.MAGNIFYING_GLASS)||GTToolHelper.matchesTool(held,GTToolType.BUILDER_WAND)) {
            if(level.isClientSide)return InteractionResult.SUCCESS;
            return com.gregtech.gregtech.api.multiblock.MultiblockTools.use(new net.minecraft.world.item.context.UseOnContext(player,hand,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition),front(),worldPosition,false)));
        }
        if (GTToolHelper.matchesTool(held, GTToolType.PLUNGER)) {
            if (!level.isClientSide) {
                if (!water.isEmpty()) water.setEmpty();
                else steam.setEmpty();
                GTToolHelper.damageForToolClickReturn(held, 100L, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (GTToolHelper.matchesTool(held, GTToolType.CHISEL) && efficiency < 10_000) {
            if (!level.isClientSide) {
                if (barometer > 15) explode();
                else {
                    long hot = heat + steam.getAmount() / 2;
                    int repaired = 10_000 - efficiency;
                    efficiency = 10_000;
                    steam.setEmpty();
                    heat = 0;
                    if (hot > 2000) player.hurt(player.damageSources().inFire(), hot / 2000f);
                    GTToolHelper.damageForToolClickReturn(held, repaired, player);
                    setChanged();
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        var heldId = ForgeRegistries.ITEMS.getKey(held.getItem());
        if (heldId != null && heldId.getNamespace().equals("gregtech")
                && heldId.getPath().equals("quicksilver_thermometer")) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("message.gregtech.large_boiler.heat",
                        heat, variant.heatCapacity()), false);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override public boolean isEnergyType(GregTechTags.Tag type, @Nullable Direction side, boolean emitting) {
        return !emitting && type == GregTechTags.Energy.HU;
    }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return List.of(GregTechTags.Energy.HU);
    }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, @Nullable Direction side,
                                                    boolean theoretical) {
        return type == GregTechTags.Energy.HU && (theoretical || isStructureOk());
    }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type, @Nullable Direction side,
                                                 boolean theoretical) { return false; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.HU ? variant.heatInputRecommended() : 0;
    }
    @Override public long getEnergySizeInputMin(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.HU ? 1 : 0;
    }
    @Override public long getEnergySizeInputMax(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.HU ? Long.MAX_VALUE : 0;
    }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type, @Nullable Direction side) {
        return 0;
    }
    @Override public long getEnergyOffered(GregTechTags.Tag type, @Nullable Direction side, long size) { return 0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag type, @Nullable Direction side, long size) {
        return type == GregTechTags.Energy.HU && size > 0 ? variant.heatInputRecommended() : 0;
    }
    @Override public long getEnergyStored(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.HU ? heat : 0;
    }
    @Override public long getEnergyCapacity(GregTechTags.Tag type, @Nullable Direction side) {
        return type == GregTechTags.Energy.HU ? variant.heatCapacity() : 0;
    }
    @Override public long doInject(GregTechTags.Tag type, @Nullable Direction side, long size,
                                   long amount, boolean execute) {
        return injectHeat(type, size, amount, execute);
    }
    private long injectHeat(GregTechTags.Tag type, long size, long amount, boolean execute) {
        if (type != GregTechTags.Energy.HU || size <= 0 || amount <= 0 || !isStructureOk()) return 0;
        long accepted = Math.min(amount, (Long.MAX_VALUE - heat) / size);
        if (accepted > 0 && execute) {
            heat += accepted * size;
            cooldown = Math.max(cooldown, 32);
            setChanged();
        }
        return accepted;
    }
    @Override public Collection<GregTechTags.Tag> portEnergyTypes(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.HEAT_INPUT ? List.of(GregTechTags.Energy.HU) : List.of();
    }
    @Override public long portEnergyInputRecommended(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return role == MultiblockLayout.Role.HEAT_INPUT && type == GregTechTags.Energy.HU
                ? variant.heatInputRecommended() : 0;
    }
    @Override public long portEnergyInputMin(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return portEnergyTypes(role).contains(type) ? 1 : 0;
    }
    @Override public long portEnergyInputMax(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return portEnergyTypes(role).contains(type) ? Long.MAX_VALUE : 0;
    }
    @Override public long portEnergyDemanded(MultiblockLayout.Role role, GregTechTags.Tag type, long size) {
        return portEnergyTypes(role).contains(type) && size > 0 ? variant.heatInputRecommended() : 0;
    }
    @Override public long portEnergyStored(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return portEnergyTypes(role).contains(type) ? heat : 0;
    }
    @Override public long portEnergyCapacity(MultiblockLayout.Role role, GregTechTags.Tag type) {
        return portEnergyTypes(role).contains(type) ? variant.heatCapacity() : 0;
    }
    @Override public long injectPortEnergy(MultiblockLayout.Role role, GregTechTags.Tag type,
                                            long size, long amount, boolean execute) {
        return role == MultiblockLayout.Role.HEAT_INPUT ? injectHeat(type, size, amount, execute) : 0;
    }

    private static boolean boilerWater(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getFluid().is(FluidTags.WATER)) return true;
        var entry = GTFluids.entryForFluid(stack.getFluid());
        if (entry != null) return entry.hasFlag(RegisteredFluids.FluidFlags.WATER);
        var id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        return id != null && id.getPath().endsWith("water");
    }
    private final IFluidHandler controllerFluids = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? water.getFluid() : tank == 1 ? steam.getFluid() : FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int tank) {
            return tank == 0 ? water.getCapacity() : tank == 1 ? steam.getCapacity() : 0;
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && boilerWater(stack);
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            return isStructureOk() && boilerWater(stack) ? water.fill(stack, action) : 0;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            return isStructureOk() ? steam.drain(stack, action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            return isStructureOk() ? steam.drain(amount, action) : FluidStack.EMPTY;
        }
    };
    private IFluidHandler port(boolean input) {
        return new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int tank) {
                return tank == 0 ? (input ? water : steam).getFluid() : FluidStack.EMPTY;
            }
            @Override public int getTankCapacity(int tank) {
                return tank == 0 ? (input ? water : steam).getCapacity() : 0;
            }
            @Override public boolean isFluidValid(int tank, FluidStack stack) {
                return input && tank == 0 && boilerWater(stack);
            }
            @Override public int fill(FluidStack stack, FluidAction action) {
                return input && isStructureOk() && boilerWater(stack) ? water.fill(stack, action) : 0;
            }
            @Override public FluidStack drain(FluidStack stack, FluidAction action) {
                return !input && isStructureOk() ? steam.drain(stack, action) : FluidStack.EMPTY;
            }
            @Override public FluidStack drain(int amount, FluidAction action) {
                return !input && isStructureOk() ? steam.drain(amount, action) : FluidStack.EMPTY;
            }
        };
    }
    private final IFluidHandler waterPort = port(true);
    private final IFluidHandler steamPort = port(false);
    @Override public IFluidHandler portFluids(MultiblockLayout.Role role) {
        return role == MultiblockLayout.Role.FLUID_INPUT ? waterPort
                : role == MultiblockLayout.Role.FLUID_OUTPUT ? steamPort : null;
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER && !isRemoved()) {
            if (!fluidCap.isPresent()) fluidCap = LazyOptional.of(() -> controllerFluids);
            return fluidCap.cast();
        }
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        fluidCap = LazyOptional.of(() -> controllerFluids);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("gt.heat", heat);
        tag.putInt("gt.cooldown", cooldown);
        tag.putInt("gt.efficiency", efficiency);
        tag.putInt("gt.barometer", barometer);
        tag.putBoolean("gt.state.str",structureOkay);
        CompoundTag waterTag = new CompoundTag(); water.writeToNBT(waterTag); tag.put("gt.water", waterTag);
        CompoundTag steamTag = new CompoundTag(); steam.writeToNBT(steamTag); tag.put("gt.steam", steamTag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        heat = Math.max(0, tag.getLong("gt.heat"));
        structureOkay=tag.getBoolean("gt.state.str");
        cooldown = tag.contains("gt.cooldown") ? Math.max(0, Math.min(128, tag.getInt("gt.cooldown"))) : 128;
        efficiency = tag.contains("gt.efficiency")
                ? com.gregtech.gregtech.content.machine.OriginalFunctionalTooltipData.boilerEfficiency(tag.getInt("gt.efficiency")) : 10_000;
        if (tag.contains("gt.water")) water.readFromNBT(tag.getCompound("gt.water"));
        if (tag.contains("gt.steam")) steam.readFromNBT(tag.getCompound("gt.steam"));
        water.setCapacity(WATER_CAPACITY);
        steam.setCapacity(variant.steamCapacity());
        barometer = (int) Math.min(31, steam.getAmount() * 31 / variant.steamCapacity());
    }
}
