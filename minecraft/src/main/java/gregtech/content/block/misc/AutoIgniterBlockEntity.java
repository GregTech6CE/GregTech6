package gregtech.content.block.misc;

import gregtech.content.data.GregTechTags;
import gregtech.engine.tool.PoweredToolTarget;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Original ten-tick trigger and hundred-tick recharge pause. */
public class AutoIgniterBlockEntity extends AutoToolBlockEntity {
    private int cooldown;

    public AutoIgniterBlockEntity(BlockPos pos, BlockState state) {
        super(gregtech.content.registry.GTBlockEntities.AUTO_IGNITER.get(), pos, state);
    }

    @Override
    protected GregTechTags.Tag energyType() {
        return GregTechTags.Energy.EU;
    }

    @Override
    protected boolean acceptsSide(Direction side) {
        return side != facing();
    }

    @Override
    public long doInject(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) {
        if (!isEnergyAcceptingFrom(type, side, false) || size <= 0 || amount <= 0) return 0;
        if (execute) {
            if (gregtech.content.tool.AutomaticToolRules.overvoltage(size, input())) {
                gregtech.engine.energy.MachineOverload.apply(this, size, energyType());
                return 1;
            }
            if (cooldown == 0 && energy < input() * 10) energy += size;
            setChanged();
        }
        return 1;
    }

    @Override
    public void tick() {
        if (level == null
                || level.isClientSide
                || !gregtech.content.tool.AutomaticToolRules.igniterDue(level.getGameTime())) return;
        if (cooldown > 0) {
            cooldown--;
            setChanged();
        }
        if (stopped || energy == 0) return;
        var target = worldPosition.relative(facing());
        long budget = gregtech.content.tool.AutomaticToolRules.igniterBudget(energy);
        boolean success = level.getBlockEntity(target) instanceof PoweredToolTarget tool
                && tool.usePoweredIgniter(facing().getOpposite(), budget, quality());
        if (!success)
            success = gregtech.content.item.behavior.ItemBehaviors.igniteToolClick(
                            level,
                            target,
                            facing().getOpposite(),
                            null,
                            net.minecraft.world.item.ItemStack.EMPTY,
                            false,
                            0.5F,
                            0.5F,
                            0.5F)
                    > 0;
        var state = level.getBlockState(target);
        if (!success
                && (CampfireBlock.canLight(state) || CandleBlock.canLight(state) || CandleCakeBlock.canLight(state))) {
            level.setBlockAndUpdate(
                    target,
                    state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, true));
            success = true;
        }
        if (!success && state.isAir() && BaseFireBlock.canBePlacedAt(level, target, facing())) {
            level.setBlockAndUpdate(target, BaseFireBlock.getState(level, target));
            success = true;
        }
        if (success) level.playSound(null, target, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1, 1);
        energy = 0;
        cooldown = gregtech.content.tool.AutomaticToolRules.RECHARGE_STEPS;
        setChanged();
    }

    public int cooldown() {
        return cooldown;
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        tag.putInt("gt.cooldown", cooldown);
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        cooldown = Math.max(
                0, Math.min(gregtech.content.tool.AutomaticToolRules.RECHARGE_STEPS, tag.getInt("gt.cooldown")));
    }
}
