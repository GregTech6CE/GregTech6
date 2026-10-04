package com.gregtech.gregtech.blockentity.tool;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.inventory.BlockContents;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.block.tool.ProcessingToolBlock;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.*;
import java.util.*;

/** GT6 bowl/bath/juicer: manual 32 GU recipes, finite tanks and atomic processing. */
public final class ProcessingToolBlockEntity extends BlockEntity implements BlockContents {
    private final RecipeMap recipes;
    private final boolean juicer;
    private final boolean wooden;
    private final boolean bowl;
    private final ItemStackHandler items;
    private final FluidTankGT[] inputs, outputs;
    private final IItemHandler itemHandler;
    private LazyOptional<IItemHandler> itemCap;
    private LazyOptional<IFluidHandler> fluidCap;
    public ProcessingToolBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.PROCESSING_TOOL.get(), pos, state);
        String id = ((ProcessingToolBlock)state.getBlock()).toolId();
        var profile=com.gregtech.gregtech.content.tool.OpenVesselRules.profile(id);
        juicer = profile.juicer();
        wooden = profile.wooden();
        bowl = profile.bowl();
        recipes = juicer ? MachineRecipeMaps.Juicer : id.startsWith("bathing_pot") ? MachineRecipeMaps.Bath : MachineRecipeMaps.Mixer;
        items = new ItemStackHandler(recipes.mInputItemsCount + recipes.mOutputItemsCount) {
            @Override public boolean isItemValid(int slot, ItemStack stack) { return slot < recipes.mInputItemsCount && recipes.containsInput(stack); }
            @Override protected void onContentsChanged(int slot) { changed(); }
        };
        inputs = tanks(recipes.mInputFluidCount, com.gregtech.gregtech.content.tool.OpenVesselRules.inputCapacity(wooden));
        outputs = tanks(recipes.mOutputFluidCount, com.gregtech.gregtech.content.tool.OpenVesselRules.outputCapacity(juicer,wooden));
        itemHandler = new IItemHandler() {
            public int getSlots() { return items.getSlots(); }
            public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
            public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
            public boolean isItemValid(int slot, ItemStack stack) { return items.isItemValid(slot, stack); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return items.insertItem(slot, stack, simulate); }
            public ItemStack extractItem(int slot, int amount, boolean simulate) { return slot < recipes.mInputItemsCount ? ItemStack.EMPTY : items.extractItem(slot, amount, simulate); }
        };
        itemCap = LazyOptional.of(() -> itemHandler);
        fluidCap = LazyOptional.of(() -> fluids);
    }
    private FluidTankGT[] tanks(int count, int capacity) {
        var tanks = new FluidTankGT[count];
        var material = wooden ? com.gregtech.gregtech.content.material.generated.WoodMaterials.Wood
                : bowl || juicer ? com.gregtech.gregtech.content.material.Materials.Ceramic
                : com.gregtech.gregtech.content.material.Materials.StainlessSteel;
        for (int i=0;i<count;i++) tanks[i] = new FluidTankGT(capacity)
                .setMaxTemperature(material.getMeltingPoint() - (wooden ? 0 : 100)).setOnChanged(this::changed);
        return tanks;
    }
    /** GT6's open-vessel fill gates; the wooden bath accepts only simple, non-hazardous liquids. */
    private boolean acceptsFluid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var fluid = stack.getFluid();
        var entry = com.gregtech.gregtech.registry.GTFluids.entryForFluid(fluid);
        long temperature = entry == null ? fluid.getFluidType().getTemperature() : entry.temperature();
        int density = fluid.getFluidType().getDensity();
        long limit = inputs.length == 0 ? Long.MAX_VALUE : inputs[0].maxTemperature();
        boolean simple = fluid == net.minecraft.world.level.material.Fluids.WATER
                || fluid == net.minecraft.world.level.material.Fluids.FLOWING_WATER
                || entry != null && entry.hasFlag(com.gregtech.gregtech.data.RegisteredFluids.FluidFlags.SIMPLE);
        return com.gregtech.gregtech.content.tool.OpenVesselRules.accepts(wooden,bowl,temperature,limit,density,simple,
                com.gregtech.gregtech.api.fluid.FluidHazards.isGas(fluid),com.gregtech.gregtech.api.fluid.FluidHazards.isAcid(fluid),
                entry != null && entry.hasFlag(com.gregtech.gregtech.data.RegisteredFluids.FluidFlags.MAGIC));
    }
    /** Original GT6: collect rainfall every 600 ticks, doubled during thunderstorms. */
    public static int rainfallAmount(float downfall, float temperature, boolean thunder) {
        return com.gregtech.gregtech.content.tool.OpenVesselRules.rainfallAmount(downfall,temperature,thunder);
    }
    public void collectRain() {
        if (juicer || level == null || level.isClientSide || !level.isRainingAt(worldPosition.above())) return;
        var above = worldPosition.above();
        var cover = level.getBlockState(above);
        if (!cover.getFluidState().isEmpty() || cover.isFaceSturdy(level, above, Direction.DOWN)
                || cover.isFaceSturdy(level, above, Direction.UP)) return;
        var climate = level.getBiome(worldPosition).value().getModifiedClimateSettings();
        int amount = rainfallAmount(climate.downfall(), climate.temperature(), level.isThundering());
        if (amount == 0) return;
        var water = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, amount);
        // Use the original input-tank preference without requiring an installed recipe for water.
        for (var tank : inputs) if (tank.getFluidInTank(0).isFluidEqual(water)) {
            tank.fill(water, IFluidHandler.FluidAction.EXECUTE); return;
        }
        for (var tank : inputs) if (tank.isEmpty()) {
            tank.fill(water, IFluidHandler.FluidAction.EXECUTE); return;
        }
    }
    public RecipeMap recipes() { return recipes; }
    public IItemHandlerModifiable inventory() { return items; }
    private List<ItemStack> inputItems() {
        var result = new ArrayList<ItemStack>();
        for (int i=0;i<recipes.mInputItemsCount;i++) result.add(items.getStackInSlot(i));
        return result;
    }
    private List<FluidStack> inputFluids() { return Arrays.stream(inputs).map(t -> t.getFluidInTank(0)).toList(); }
    /** Output capacity is checked at worst-case yields before inputs or RNG are touched. */
    public boolean process(Player player){return process(player,ItemStack.EMPTY);}
    public boolean processMixer(Player player,ItemStack stack){return player!=null&&player.mayBuild()&&level.mayInteract(player,worldPosition)&&bowl&&stack.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric&&electric.toolName().equals("Mixer")&&electric.isPoweredUsable(stack)&&process(player,stack);}
    private boolean process(Player player,ItemStack powered) {
        if (juicer || level == null || level.isClientSide) return false;
        for (var recipe : recipes.mRecipeList) {
            if (!recipe.mEnabled || recipe.mFakeRecipe || !com.gregtech.gregtech.content.tool.OpenVesselRules.recipePower(recipe.mEUt)) continue;
            var remaining = RecipeInputs.consume(recipe, inputItems(), inputFluids(), 1);
            if (remaining == null || !canOutput(recipe)) continue;
            for (int i=0;i<remaining.items().size();i++) items.setStackInSlot(i, remaining.items().get(i));
            for (int i=0;i<inputs.length;i++) inputs[i].setFluid(remaining.fluids().get(i));
            for (int i=0;i<recipe.mOutputs.length;i++) {
                var out = recipe.mOutputs[i];
                long count = recipe.rollOutputCount(i, 1, level.random::nextInt);
                if (count > 0) mergeOutput(i, out.copyWithCount((int)count));
            }
            for (int i=0;i<recipe.mFluidOutputs.length;i++) outputs[i].fill(recipe.mFluidOutputs[i], IFluidHandler.FluidAction.EXECUTE);
            if(powered.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric){long total=Math.max(1,Math.abs(recipe.mEUt)*(long)recipe.mDuration);electric.consumeInteractionEnergy(powered,Math.max(1,(total+3)/4),player);}
            else if (player != null) {
                double divisor = com.gregtech.gregtech.content.tool.OpenVesselRules.exhaustionDivisor(juicer,recipes==MachineRecipeMaps.Bath);
                player.causeFoodExhaustion((float)Math.min(Float.MAX_VALUE, Math.abs((double)recipe.mEUt)*recipe.mDuration/divisor));
            }
            changed(); return true;
        }
        return false;
    }
    private boolean canOutput(Recipe recipe) {
        if (recipe.mOutputs.length > recipes.mOutputItemsCount || recipe.mFluidOutputs.length > outputs.length) return false;
        if (recipe.mNeedsEmptyOutput) {
            for (int i=recipes.mInputItemsCount;i<items.getSlots();i++) if (!items.getStackInSlot(i).isEmpty()) return false;
            for (var tank : outputs) if (!tank.isEmpty()) return false;
        }
        for (int i=0;!juicer && i<recipe.mOutputs.length;i++) {
            var out=recipe.mOutputs[i]; if (out == null || out.isEmpty()) continue;
            var present=items.getStackInSlot(recipes.mInputItemsCount+i);
            if ((!present.isEmpty() && !ItemStack.isSameItemSameTags(present,out)) || present.getCount()+out.getCount()>out.getMaxStackSize()) return false;
        }
        for (int i=0;i<recipe.mFluidOutputs.length;i++) {
            var out=recipe.mFluidOutputs[i];
            if (out == null || out.isEmpty()) continue;
            // GT6 Juicer.canOutput checks existing liquid, not capacity minus the next batch.
            if (juicer && outputs[i].getFluidInTank(0).getAmount() >= Math.max(1000L,1L+out.getAmount())) return false;
            if (outputs[i].fill(out,IFluidHandler.FluidAction.SIMULATE)!=out.getAmount()) return false;
        }
        return true;
    }
    private void mergeOutput(int index, ItemStack stack) {
        int slot=recipes.mInputItemsCount+index;
        var out=items.getStackInSlot(slot).copy();
        if (out.isEmpty()) out=stack; else out.grow(stack.getCount());
        items.setStackInSlot(slot,out);
    }
    public void interact(Player player, InteractionHand hand, Direction side) {
        if (level == null || level.isClientSide || !player.mayBuild() || !level.mayInteract(player,worldPosition)) return;
        var held=player.getItemInHand(hand);
        if(held.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric&&electric.toolName().equals("Mixer")){if(side==Direction.UP)processMixer(player,held);return;}
        if (com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(held,com.gregtech.gregtech.api.tool.GTToolType.PLUNGER)) {
            for (var tank : allTanks()) if (!tank.drain(1000,IFluidHandler.FluidAction.EXECUTE).isEmpty()) {
                com.gregtech.gregtech.api.tool.GTToolHelper.damageForUse(held,1,player); return;
            }
        }
        if (juicer) {
            if (!juiceHeld(player,hand)) FluidUtil.interactWithFluidHandler(player,hand,fluids);
            return;
        }
        if (FluidUtil.interactWithFluidHandler(player,hand,fluids)) return;
        if (side == Direction.UP && !player.isShiftKeyDown() && process(player)) return;
        if (collectOutputs(player)) return;
        if (!held.isEmpty()) {
            ItemStack remaining=held.copy();
            for (int i=0;i<recipes.mInputItemsCount;i++) remaining=items.insertItem(i,remaining,false);
            if (!player.getAbilities().instabuild) player.setItemInHand(hand,remaining);
        } else {
            for (int i=0;i<recipes.mInputItemsCount;i++) {
                var stack=items.extractItem(i,Integer.MAX_VALUE,false);
                if (!stack.isEmpty()) { give(player,stack); return; }
            }
        }
    }
    /** GT6 has no item inventory here: recipes consume the held stack and give every solid output. */
    private boolean juiceHeld(Player player, InteractionHand hand) {
        var held=player.getItemInHand(hand);
        if (held.isEmpty()) return false;
        for (var recipe:recipes.mRecipeList) {
            if (!recipe.mEnabled || recipe.mFakeRecipe || !com.gregtech.gregtech.content.tool.OpenVesselRules.recipePower(recipe.mEUt)) continue;
            var remaining=RecipeInputs.consume(recipe,List.of(held),List.of(),1);
            if (remaining==null || !canOutput(recipe)) continue;
            if (!player.getAbilities().instabuild) player.setItemInHand(hand,remaining.items().get(0));
            for (int i=0;i<recipe.mOutputs.length;i++) {
                long count=recipe.rollOutputCount(i,1,level.random::nextInt);
                while (count>0) {
                    int amount=(int)Math.min(count,recipe.mOutputs[i].getMaxStackSize());
                    give(player,recipe.mOutputs[i].copyWithCount(amount));
                    count-=amount;
                }
            }
            for (int i=0;i<recipe.mFluidOutputs.length;i++)
                if (recipe.mFluidOutputs[i]!=null) outputs[i].fill(recipe.mFluidOutputs[i],IFluidHandler.FluidAction.EXECUTE);
            player.causeFoodExhaustion((float)Math.min(Float.MAX_VALUE,Math.abs((double)recipe.mEUt)*recipe.mDuration/10000));
            level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.SLIME_SQUISH,
                    net.minecraft.sounds.SoundSource.BLOCKS,.5F,1F);
            changed();
            return true;
        }
        return false;
    }
    private boolean collectOutputs(Player player) {
        for (int i=recipes.mInputItemsCount;i<items.getSlots();i++) {
            var stack=items.extractItem(i,Integer.MAX_VALUE,false);
            if (!stack.isEmpty()) { give(player,stack); return true; }
        }
        return false;
    }
    private static void give(Player player,ItemStack stack) { if (!player.addItem(stack)) player.drop(stack,false); }
    private List<FluidTankGT> allTanks() { var result=new ArrayList<FluidTankGT>(Arrays.asList(outputs)); result.addAll(Arrays.asList(inputs)); return result; }
    private final IFluidHandler fluids = new IFluidHandler() {
        public int getTanks() { return inputs.length+outputs.length; }
        public FluidStack getFluidInTank(int tank) { return allTanks().get(tank).getFluidInTank(0).copy(); }
        public int getTankCapacity(int tank) { return allTanks().get(tank).getTankCapacity(0); }
        public boolean isFluidValid(int tank,FluidStack stack) { return tank>=outputs.length && acceptsFluid(stack); }
        public int fill(FluidStack stack,FluidAction action) {
            if (!acceptsFluid(stack)) return 0;
            for (var tank:inputs) if (tank.getFluidInTank(0).isFluidEqual(stack)) return tank.fill(stack,action);
            for (var tank:inputs) if (tank.isEmpty()) return tank.fill(stack,action);
            return 0;
        }
        public FluidStack drain(FluidStack stack,FluidAction action) {
            for (var tank:allTanks()) if (tank.getFluidInTank(0).isFluidEqual(stack)) return tank.drain(stack,action);
            return FluidStack.EMPTY;
        }
        public FluidStack drain(int amount,FluidAction action) {
            for (var tank:allTanks()) if (!tank.isEmpty()) return tank.drain(amount,action);
            return FluidStack.EMPTY;
        }
    };
    public FluidStack displayFluid() { for(var tank:allTanks()) if(!tank.isEmpty()) return tank.getFluidInTank(0).copy(); return FluidStack.EMPTY; }
    public ItemStack displayItem() { for(int i=items.getSlots()-1;i>=0;i--) if(!items.getStackInSlot(i).isEmpty()) return items.getStackInSlot(i).copy(); return ItemStack.EMPTY; }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @javax.annotation.Nullable Direction side) {
        if (cap==ForgeCapabilities.ITEM_HANDLER) return juicer ? LazyOptional.empty() : itemCap.cast();
        if (cap==ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCap.invalidate(); fluidCap.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        itemCap = LazyOptional.of(() -> itemHandler);
        fluidCap = LazyOptional.of(() -> fluids);
    }
    private void changed() { setChanged(); if(level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); }
    @Override public void dropContents() { BlockContents.drop(this,items); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("gt.items",items.serializeNBT());
        var list=new ListTag(); for(var tank:allTanks()) { var data=new CompoundTag(); tank.writeToNBT(data); list.add(data); } tag.put("gt.tanks",list);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); if(tag.contains("gt.items")) items.deserializeNBT(tag.getCompound("gt.items"));
        var list=tag.getList("gt.tanks",10); var tanks=allTanks(); for(int i=0;i<tanks.size() && i<list.size();i++) tanks.get(i).readFromNBT(list.getCompound(i));
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(net.minecraft.network.Connection connection,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet) { if(packet.getTag()!=null) load(packet.getTag()); }
}
