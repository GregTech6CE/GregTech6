package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.energy.FaceConfig;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.machine.SteamEngineData;

import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/** Steam -> KU engine. Implements GT6 steam engine physics: energy storage (mEN),
 *  variable output based on fill level, preheating, and soft shutdown (no explosion). */
public class KineticSteamEngineBlockEntity extends EngineBaseBlockEntity implements com.gregtech.gregtech.api.machine.MachineControl.Provider {

    SteamEngineData spec;
    FluidTankGT steamTank;
    FluidTankGT distilledWaterTank;
    boolean enabled = true;
    boolean shutdown;
    private boolean emitted,working;
    private final com.gregtech.gregtech.api.machine.MachineControl control=com.gregtech.gregtech.api.machine.MachineControls.switchable(this,
            this::isEnabled,value->{this.enabled=value;setChanged();},()->working,()->emitted);
    @Override public com.gregtech.gregtech.api.machine.MachineControl machineControl(Direction side){return control;}
    private int engineState, piston;
    private long engineTicks;
    long clientOutputRate;

    /** Pre-resolved distilled water Fluid, cached at setSpec() time. */
    private net.minecraft.world.level.material.Fluid cachedDistilledWaterFluid;
    public int pressureState(){return engineState;}

    public KineticSteamEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setSpec(SteamEngineData spec) {
        this.spec = spec;
        this.clientOutputRate = spec.outputRate();
        this.steamTank = new FluidTankGT(spec.steamCapacity()).setOnChanged(this::setChanged);
        this.distilledWaterTank = new FluidTankGT(spec.steamCapacity()).setOnChanged(this::setChanged);
        this.faceConfig = defaultFaceConfig();
    }

    public SteamEngineData spec() { return spec; }
    @Nullable public FluidTankGT steamTank() { return steamTank; }
    @Nullable public FluidTankGT distilledWaterTank() { return distilledWaterTank; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; if(v)shutdown=false; setChanged(); }
    public boolean isShutdown() { return shutdown; }

    /** Reset from forced shutdown, clear energy and return to off state. */
    public void resetShutdown() {
        this.shutdown = false;
        this.enabled = false;
        this.kuEnergy = 0;
        setChanged();
    }

    @Override protected long outputRate() { return spec != null ? spec.outputRate() : clientOutputRate; }
    @Override protected long inputRate() { return 0; }
    @Override protected GregTechTags.Tag inputEnergyType() { return GregTechTags.Energy.STEAM; }

    public long energyCapacity() { return outputRate() * 2000; }

    @Override
    public FaceConfig defaultFaceConfig() {
        return FaceConfig.builder()
                .fluidIn(FaceConfig.BACK)
                .fluidOut(FaceConfig.LEFT, FaceConfig.RIGHT, FaceConfig.TOP, FaceConfig.BOTTOM)
                .energyOut(FaceConfig.FRONT)
                .build();
    }

    // ---- KU output gating --------------------------------------------------

    /** Original activity condition uses the sampled pressure state and available stroke energy. */
    @Override
    protected boolean isReadyToOutput() {
        if (shutdown || !enabled) return false;
        long cap = energyCapacity();
        if (cap <= 0) return false;
        long output=computeOutput();
        return kuEnergy>output&&output*2>outputRate();
    }

    /** Limit per-tick output to {@code computeOutput()} — GT6 variable-output
     *  range (0.5×–2× rated), never the full buffer.  Prevents external
     *  pullers from instantly draining the thermal mass. */
    @Override
    protected long maxKuOutputPerTick() {
        return computeOutput();
    }

    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type,@Nullable Direction side){return type==GregTechTags.Energy.KU?outputRate():0;}
    // The original steam engine actively pushes one piston stroke and spends it even if rejected.
    @Override public long getEnergyOffered(GregTechTags.Tag type,@Nullable Direction side,long size){return 0;}
    @Override public long doExtract(GregTechTags.Tag type,@Nullable Direction side,long size,long amount,boolean execute){return 0;}
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag type,@Nullable Direction side,boolean theoretical){return false;}
    @Override public long doInject(GregTechTags.Tag type,@Nullable Direction side,long size,long amount,boolean execute){return 0;}
    @Override public long getEnergyDemanded(GregTechTags.Tag type,@Nullable Direction side,long size){return 0;}
    @Override public long getEnergyCapacity(GregTechTags.Tag type,@Nullable Direction side){return type==GregTechTags.Energy.KU?energyCapacity():0;}

    // ---- Status display ---------------------------------------------------

    public String statusDescription() {
        if (shutdown) return "SHUTDOWN — Over Capacity";
        if (!enabled) return "OFF";
        long cap = energyCapacity();
        if (cap <= 0) return "OFF";
        long pct = kuEnergy * 100 / cap;
        if (!isReadyToOutput()) return "STANDBY — Preheating (" + pct + "%)";
        return "RUNNING — Output " + computeOutput() + " KU/t (" + pct + "%)";
    }

    long computeOutput() {return outputRate()*(engineState+1)/16;}
    private static boolean isSteam(FluidStack fluid){
        var steam=com.gregtech.gregtech.registry.GTFluids.still("Steam");
        return !fluid.isEmpty()&&steam!=null&&steam.isBound()&&fluid.getFluid().isSame(steam.get());
    }
    private IFluidHandler steamInput(){return new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int slot){return steamTank.getFluid().copy();}
        public int getTankCapacity(int slot){return (int)steamTank.capacity();}
        public boolean isFluidValid(int slot,FluidStack fluid){return isSteam(fluid);}
        public int fill(FluidStack fluid,FluidAction action){return !isRemoved()&&isSteam(fluid)?steamTank.fill(fluid,action):0;}
        public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.EMPTY;}
        public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
    };}

    // ---- Capabilities -------------------------------------------------------

    public IFluidHandler fluidHandler(@Nullable Direction side){
        FaceConfig fc=resolvedFaceConfig();
        if(steamTank!=null&&(side==null||FaceConfig.has(fc.fluidInputs(),relativeDir(side))))return steamInput();
        if(distilledWaterTank!=null&&(side==null||FaceConfig.has(fc.fluidOutputs(),relativeDir(side))))return new com.gregtech.gregtech.api.fluid.FluidPort(distilledWaterTank,false,true);
        return null;
    }
    // ---- NBT ----------------------------------------------------------------

    @Override
    public void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        if (steamTank != null) {
            CompoundTag t = new CompoundTag();
            steamTank.writeToNBT(t,lookup);
            tag.put("steamTank", t);
        }
        if (distilledWaterTank != null) {
            CompoundTag t = new CompoundTag();
            distilledWaterTank.writeToNBT(t,lookup);
            tag.put("waterTank", t);
        }
        tag.putLong("outputRate", clientOutputRate);
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("shutdown", shutdown);
        tag.putInt("gt.engine_state",engineState);tag.putInt("gt.piston",piston);
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        if (tag.contains("steamTank") && steamTank != null) {
            steamTank.readFromNBT(tag.getCompound("steamTank"),lookup);
        }
        if (tag.contains("waterTank") && distilledWaterTank != null) {
            distilledWaterTank.readFromNBT(tag.getCompound("waterTank"),lookup);
        }
        clientOutputRate = tag.getLong("outputRate");
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        shutdown = tag.getBoolean("shutdown");
        engineState=Math.max(0,Math.min(31,tag.getInt("gt.engine_state")));piston=tag.getInt("gt.piston")&3;
        kuEnergy=Math.max(0,Math.min(energyCapacity(),kuEnergy));
        if(spec!=null)steamTank.setCapacity(spec.steamCapacity());
    }

    // ---- Tick ---------------------------------------------------------------

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, KineticSteamEngineBlockEntity be) {
        if (level.isClientSide||be.spec==null||be.steamTank==null)return;
        long cap=be.energyCapacity();
        if(be.enabled&&!be.shutdown&&isSteam(be.steamTank.getFluid())){
            long ops=be.steamTank.getAmount()/be.spec.steamPerOp();
            if(ops>0){
                be.steamTank.remove(ops*be.spec.steamPerOp());
                be.kuEnergy+=ops*be.spec.efficiency()/100;
                be.addDistilledWater((int)ops);
            }
        }
        if(be.engineTicks++%20==0)be.engineState=(int)Math.min(31,be.kuEnergy*32/cap);
        be.working=be.isReadyToOutput();be.emitted=false;
        if(be.working){
            long output=be.computeOutput();
            if(be.engineTicks%(32-be.engineState)==0)be.piston=(be.piston+1)&3;
            be.emitted=EnergyTransfer.emitEnergyToNetwork(GregTechTags.Energy.KU,be.piston>1?-output:output,1,be)>0;
            be.kuEnergy-=output;
        }
        if(be.kuEnergy>=cap){
            be.kuEnergy=cap-1;
            if(be.engineState>30){be.shutdown=true;be.enabled=false;be.steamTank.setEmpty();}
            else be.engineState=31;
        }
        if(!be.enabled||be.shutdown){
            be.kuEnergy=Math.max(0,be.kuEnergy-Math.max(1,cap/64));
            if(be.kuEnergy==0)be.shutdown=false;
        }
        autoOutputDistilledWater(level,be);
        // GT6 emits recoverable exhaust to adjacent tanks, then discards unrecovered condensate.
        be.distilledWaterTank.setEmpty();
        be.setChanged();be.syncToClient();
    }

    private static void autoOutputDistilledWater(Level level, KineticSteamEngineBlockEntity be) {
        if (level.isClientSide || be.distilledWaterTank == null || be.distilledWaterTank.isEmpty()) return;
        int fluidOuts = be.resolvedFaceConfig().fluidOutputs();
        if (fluidOuts == 0) return;
        for (int relDir = 0; relDir < 6; relDir++) {
            if (!FaceConfig.has(fluidOuts, relDir)) continue;
            Direction side = be.relativeToAbsolute(relDir);
            if(!level.hasChunkAt(be.worldPosition.relative(side)))continue;
            BlockEntity adjBe = level.getBlockEntity(be.worldPosition.relative(side));
            if (adjBe == null) continue;
            IFluidHandler target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,adjBe.getBlockPos(),side.getOpposite());
            if (target == null) continue;
            FluidStack drained = be.distilledWaterTank.drain(1000, IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty()) continue;
            int filled = target.fill(drained, IFluidHandler.FluidAction.SIMULATE);
            if (filled <= 0) continue;
            drained.setAmount(Math.min(drained.getAmount(),filled));
            int accepted=target.fill(drained,IFluidHandler.FluidAction.EXECUTE);
            be.distilledWaterTank.remove(Math.max(0,Math.min(accepted,drained.getAmount())));
        }
    }

    void addDistilledWater(int amount) {
        if (amount <= 0 || distilledWaterTank == null) return;
        resolveDistilledWaterFluid();
        if (cachedDistilledWaterFluid == null) return;
        FluidStack existing = distilledWaterTank.getFluidLong();
        if (existing.isEmpty()) {
            distilledWaterTank.setFluid(new FluidStack(cachedDistilledWaterFluid, amount));
        } else if (FluidStack.isSameFluidSameComponents(existing,new FluidStack(cachedDistilledWaterFluid,1))) {
            distilledWaterTank.add(amount);
        }
    }

    /** Resolve the distilled water Fluid using multiple fallback paths, caching the result. */
    private void resolveDistilledWaterFluid() {
        if (cachedDistilledWaterFluid != null) return;
        // Path 1: GTFluids field lookup
        var dwRO = com.gregtech.gregtech.registry.GTFluids.still("DistW");
        if (dwRO != null && dwRO.isBound()) {
            cachedDistilledWaterFluid = dwRO.get();
            return;
        }
        var registry=net.minecraft.core.registries.BuiltInRegistries.FLUID;
        var id=ResourceLocation.fromNamespaceAndPath("gregtech","ic2distilledwater");
        if(registry.containsKey(id)){cachedDistilledWaterFluid=registry.get(id);return;}
        for(var entry:registry.entrySet())if(entry.getKey().location().getPath().equalsIgnoreCase("ic2distilledwater")){
            cachedDistilledWaterFluid=entry.getValue();return;
        }
    }
}
