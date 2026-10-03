package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.machine.BoilerSpec;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.block.machine.BoilerTankBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * GT6 single-block Steam Boiler Tank ({@code MultiTileEntityBoilerTank}).
 *
 * <p>The missing middle of the early steam chain: a burning box below feeds it
 * HU, it boils water ({@code 1 L water + 80 HU -> 160 L steam}) and pushes steam
 * up into a pipe/engine. Running dry overheats it and a full steam tank bursts it
 * — both explode (GT6 hazard). Non-distilled water causes calcification (efficiency
 * loss); chisel it off at low pressure. Magnifying glass inspects calcification.</p>
 */
public class BoilerTankBlockEntity extends GTEnergyBlockEntity {

    private BoilerSpec spec;
    private long heat;
    private int cooldown = 128;
    private short efficiency = 10000;
    private final FluidTankGT waterTank = new FluidTankGT(4_000).setOnChanged(this::setChanged);
    private FluidTankGT steamTank = new FluidTankGT(64_000).setOnChanged(this::setChanged);


    public BoilerTankBlockEntity(BlockPos pos, BlockState state) {
        this(com.gregtech.gregtech.registry.GTBlockEntities.STEAM_BOILER.get(), pos, state);
    }

    public BoilerTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.getBlock() instanceof BoilerTankBlock block) {
            setSpec(block.spec());
        }
    }

    public void setSpec(BoilerSpec spec) {
        this.spec = spec;
        this.steamTank = new FluidTankGT(spec.steamCapacity()).setOnChanged(this::setChanged);
    }

    public BoilerSpec spec() { return spec; }
    public long storedHeat() { return heat; }
    @Nullable public FluidTankGT waterTank() { return waterTank; }
    @Nullable public FluidTankGT steamTank() { return steamTank; }
    public short efficiency() { return efficiency; }
    /** Client-side smoothed gauge frame (0–31). Used by {@code BoilerBarometerRenderer}
     *  to prevent needle oscillation near the 50 % output threshold. */
    transient float smoothBarometerFrame;

    public int barometerValue() {
        long cap = steamTank.getCapacity();
        if (cap <= 0) return 0;
        return (int) Math.min(31, steamTank.getAmount() * 31 / cap);
    }

    /** Exponential-moving-average smoothed frame for the barometer needle.
     *  Call every render frame; returns a damped 0–31 value. */
    public int smoothBarometerFrame(float speed) {
        float target = Math.min(31f, (float) steamTank.getAmount() * 31f / Math.max(1, steamTank.getCapacity()));
        smoothBarometerFrame += (target - smoothBarometerFrame) * speed;
        return Math.round(Math.min(31, smoothBarometerFrame));
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, BoilerTankBlockEntity be) {
        if (be.spec == null || level.isClientSide) return;
        be.boil();
        be.coolDown();
        be.pushSteamUp();
        be.syncToClient();
        if (be.heat > be.spec.heatCapacity() || be.steamTank.getAmount() >= be.steamTank.getCapacity()) {
            be.explode();
        }
    }

    private void boil() {
        if (heat < BoilerSpec.HU_PER_WATER || waterTank.isEmpty()) return;
        var steamFluid = com.gregtech.gregtech.registry.GTFluids.still("Steam");
        if (steamFluid == null || !steamFluid.isBound()) return;

        long maxByRate = Math.max(1, steamTank.getCapacity() / 2560);
        long conversions = Math.min(maxByRate, Math.min(heat / BoilerSpec.HU_PER_WATER,
                waterTank.getAmount()));
        if (conversions <= 0) return;

        waterTank.remove(conversions);
        if(efficiency>5000&&!waterTank.isEmpty()&&!isDistilledWater()&&level.random.nextInt(10)==0)
            efficiency=(short)Math.max(5000,efficiency-conversions);
        long steam=conversions*BoilerSpec.STEAM_PER_WATER*efficiency/10000;
        steamTank.setFluid(new FluidStack(steamFluid.get(),1),steamTank.getAmount()+steam);
        heat-=conversions*BoilerSpec.HU_PER_WATER;
        cooldown=128;
        setChanged();
    }
    private void coolDown(){
        if(cooldown--<=0){
            cooldown=0;
            heat=Math.max(0,heat-spec.steamOutput()*32);
            steamTank.remove(spec.steamOutput()*64);
            if(heat==0)cooldown=128;
        }
    }

    private boolean isDistilledWater() {
        FluidStack water = waterTank.getFluid();
        if (water.isEmpty()) return false;
        var distilled=com.gregtech.gregtech.registry.GTFluids.still("DistW");
        return distilled!=null&&distilled.isBound()&&water.getFluid().isSame(distilled.get());
    }

    /** GT6 boilers emit steam upward; the output keeps the lower half of its pressure buffer. */
    private void pushSteamUp() {
        if (level == null || steamTank.getAmount() <= steamTank.getCapacity() / 2) return;
        if(!level.hasChunkAt(worldPosition.above()))return;
        var above = level.getBlockEntity(worldPosition.above());
        if (above == null) return;
        IFluidHandler target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,worldPosition.above(),Direction.DOWN);
        if (target == null) return;
        long want = steamTank.getAmount() > steamTank.getCapacity() / 4 * 3
                ? spec.steamOutput() * 2 : spec.steamOutput();
        FluidStack offer = steamTank.drain((int) Math.min(Math.min(want,steamTank.getAmount()-steamTank.getCapacity()/2), Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE);
        if (offer.isEmpty()) return;
        int filled = target.fill(offer, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) return;
        offer.setAmount(Math.min(filled,offer.getAmount()));
        int accepted=target.fill(offer,IFluidHandler.FluidAction.EXECUTE);
        steamTank.remove(Math.max(0,Math.min(accepted,offer.getAmount())));
    }

    public void explode() {
        if (level == null) return;
        float power = (float) Math.max(1.0, Math.sqrt(steamTank.getAmount()) / 100.0);
        level.removeBlock(worldPosition, false);
        level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                Math.min(8.0F, power), Level.ExplosionInteraction.BLOCK);
    }

    // ── Tool interactions ────────────────────────────────────────────────────

    public InteractionResult onBlockActivated(BlockState state, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level == null || level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);

        // Wrench: rotate facing
        if (GTToolHelper.matchesTool(held, GTToolType.WRENCH)
                || GTToolHelper.matchesTool(held, GTToolType.MONKEY_WRENCH)) {
            Direction current = state.getValue(BoilerTankBlock.FACING);
            Direction target = current.getClockWise();
            if (player.isShiftKeyDown()) target = current.getCounterClockWise();
            level.setBlock(worldPosition, state.setValue(BoilerTankBlock.FACING, target), 3);
            level.playSound(null, worldPosition,
                    com.gregtech.gregtech.content.transport.fluid.FluidTransportRegistries.WRENCH.get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            GTToolHelper.damageForToolClickReturn(held, 10000L, player);
            return InteractionResult.CONSUME;
        }

        // Chisel: clean calcification (explodes if steam > ~50%)
        if (GTToolHelper.matchesTool(held, GTToolType.CHISEL)) {
            if (barometerValue() > 15) {
                // Too much pressure — cleaning calcite blows up
                explode();
            } else {
                long hot=heat+steamTank.getAmount()/2;
                efficiency = 10000;
                steamTank.setEmpty();
                heat = 0;
                if (player != null && hot>2000) {
                    player.hurt(player.damageSources().inFire(), hot / 2000.0F);
                }
                setChanged();
                player.displayClientMessage(Component.literal("Calcification cleaned.")
                        .withStyle(ChatFormatting.GREEN), false);
            }
            GTToolHelper.damageForToolClickReturn(held, 10000L, player);
            return InteractionResult.CONSUME;
        }

        // Magnifying glass: inspect calcification and water
        if (GTToolHelper.matchesTool(held, GTToolType.MAGNIFYING_GLASS)) {
            if (efficiency < 10000) {
                double calcPct = (10000.0 - efficiency) / 100.0;
                player.displayClientMessage(
                        Component.literal("Calcification: " + String.format("%.2f", calcPct) + "%")
                                .withStyle(ChatFormatting.YELLOW), false);
            } else {
                player.displayClientMessage(
                        Component.literal("No Calcification in this Boiler")
                                .withStyle(ChatFormatting.GREEN), false);
            }
            if (!waterTank.isEmpty()) {
                String waterName = waterTank.getFluid().getDisplayName().getString();
                player.displayClientMessage(
                        Component.literal("Water: " + waterTank.getAmount() + "L of " + waterName)
                                .withStyle(ChatFormatting.GRAY), false);
            }
            return InteractionResult.CONSUME;
        }

        // Plunger: empty tanks
        if (GTToolHelper.matchesTool(held, GTToolType.PLUNGER)) {
            if (!steamTank.isEmpty()) {
                steamTank.setEmpty();
                player.displayClientMessage(Component.literal("Steam tank emptied.")
                        .withStyle(ChatFormatting.RED), false);
            } else if (!waterTank.isEmpty()) {
                waterTank.setEmpty();
                player.displayClientMessage(Component.literal("Water tank emptied.")
                        .withStyle(ChatFormatting.RED), false);
            }
            GTToolHelper.damageForToolClickReturn(held, 100L, player);
            setChanged();
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    // ── HU acceptance (any face, from the burning box below) ──────────────────

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return !emitting && energyType == GregTechTags.Energy.HU;
    }

    @Override
    public java.util.Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return List.of(GregTechTags.Energy.HU);
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return energyType == GregTechTags.Energy.HU;
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return false;
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.HU && spec != null ? spec.heatInputRecommended() : 0;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return 0;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return 0;
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (energyType != GregTechTags.Energy.HU || size <= 0 || spec == null) return 0;
        return (Long.MAX_VALUE-heat)/size;
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (energyType != GregTechTags.Energy.HU || amount <= 0 || size <= 0 || spec == null) return 0;
        long accepted=Math.min(amount,(Long.MAX_VALUE-heat)/size);
        if (doInject && accepted > 0) {
            heat += accepted * size;
            cooldown=Math.max(cooldown,32);
            setChanged();
        }
        return accepted;
    }

    @Override
    public long getEnergyStored(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.HU ? heat : 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.HU && spec != null ? spec.heatCapacity() : 0;
    }

    // ── Fluids: accept water from sides/bottom; steam is actively pushed ───────────────────────────────────

    private final IFluidHandler fluids = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) {
            return (tank == 0 ? waterTank.getFluid() : steamTank.getFluid()).copy();
        }
        @Override public int getTankCapacity(int tank) {
            return (int) (tank == 0 ? waterTank.getCapacity() : steamTank.getCapacity());
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && isWater(stack.getFluid());
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isWater(resource.getFluid())) return 0;
            return waterTank.fill(resource, action);
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    private static boolean isWater(net.minecraft.world.level.material.Fluid fluid) {
        if (fluid == net.minecraft.world.level.material.Fluids.WATER) return true;
        var key = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
        return key != null && java.util.Set.of("water","ic2distilledwater","riverwater","spectral_dew","cold_water","hot_water","ic2hotwater","boilingwater","watergeothermal","ice").contains(key.getPath());
    }

    public IFluidHandler fluidHandler(@Nullable Direction side){return side==Direction.UP?new com.gregtech.gregtech.api.fluid.FluidPort(fluids,false,false):fluids;}

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putLong("gt.heat", heat);
        tag.putInt("gt.cooldown",cooldown);
        tag.putShort("gt.efficiency", efficiency);
        CompoundTag w = new CompoundTag();
        waterTank.writeToNBT(w,lookup);
        tag.put("gt.water", w);
        CompoundTag s = new CompoundTag();
        steamTank.writeToNBT(s,lookup);
        tag.put("gt.steam", s);
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        heat = Math.max(0,tag.getLong("gt.heat"));
        cooldown=tag.contains("gt.cooldown")?Math.max(0,Math.min(128,tag.getInt("gt.cooldown"))):128;
        if (tag.contains("gt.efficiency")) efficiency = tag.getShort("gt.efficiency");
        else efficiency = 10000;
        if (tag.contains("gt.water")) waterTank.readFromNBT(tag.getCompound("gt.water"),lookup);
        if (tag.contains("gt.steam")) steamTank.readFromNBT(tag.getCompound("gt.steam"),lookup);
        waterTank.setCapacity(Math.max(4000,waterTank.getAmount()));
        if(spec!=null)steamTank.setCapacity(spec.steamCapacity());
        efficiency=(short)Math.max(0,Math.min(10000,efficiency));
    }
}
