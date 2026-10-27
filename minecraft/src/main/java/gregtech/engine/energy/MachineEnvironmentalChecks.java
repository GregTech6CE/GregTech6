package gregtech.engine.energy;

import gregtech.GregTechConfig.Rule;
import gregtech.content.blockentity.machine.BurningBoxBlockEntity;
import gregtech.content.blockentity.machine.SolidBurningBoxBlockEntity;
import gregtech.content.data.GregTechTags;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.WeakHashMap;

/** GT6 TileEntityBase01Root.doDefaultStructuralChecks, sampled only for working energy devices. */
public final class MachineEnvironmentalChecks {
    private static final WeakHashMap<BlockEntity, Long> LAST_CHECK = new WeakHashMap<>();

    private MachineEnvironmentalChecks() {}

    public static boolean check(BlockEntity machine, IEnergyBlock energy) {
        var level = machine.getLevel();
        if (level == null || level.isClientSide || energy == null) return true;
        if (machine.isRemoved()) return false;
        // Transport has its own insulation/temperature rules, not the machine structural check.
        if (!(machine instanceof gregtech.content.blockentity.GTEnergyBlockEntity)) return true;
        // GT6 burning boxes emit HU without running the converter's structural checks.
        if (machine instanceof SolidBurningBoxBlockEntity || machine instanceof BurningBoxBlockEntity) return true;
        long now = level.getGameTime();
        Long last = LAST_CHECK.get(machine);
        if (last != null && now - last < 600) return true;
        LAST_CHECK.put(machine, now);
        for (var type : energy.getEnergyTypes(null)) for (Direction side : Direction.values()) {
            var adjacent = machine.getBlockPos().relative(side);
            if (!level.hasChunkAt(adjacent)) continue;
            if (GregTechTags.Energy.ALL_WEAK_TO_FIRE.contains(type) && !energy.isFireProof(side)
                    && level.getBlockState(adjacent).getBlock() instanceof BaseFireBlock && level.random.nextInt(10) == 0)
                return damage(machine, type, Rule.FIRE_EXPLOSIONS, Rule.FIRE_BREAKING);
            if (side == Direction.DOWN) continue;
            if (GregTechTags.Energy.ALL_WEAK_TO_WATER.contains(type)) {
                if (!energy.isWaterProof(side) && !level.getFluidState(adjacent).isEmpty())
                    return damage(machine, type, Rule.WATER_EXPLOSIONS, Rule.WATER_BREAKING);
                if (!energy.isRainProof(side) && level.isRainingAt(adjacent) && level.random.nextInt(100) == 0)
                    return damage(machine, type, Rule.RAIN_EXPLOSIONS, Rule.RAIN_BREAKING);
            }
            if (GregTechTags.Energy.ALL_WEAK_TO_THUNDER.contains(type) && !energy.isThunderProof(side)
                    && level.isThundering() && level.isRainingAt(adjacent) && level.random.nextInt(1000) == 0)
                return damage(machine, type, Rule.THUNDER_EXPLOSIONS, Rule.THUNDER_BREAKING);
        }
        return true;
    }

    private static boolean damage(BlockEntity machine, GregTechTags.Tag type, Rule explosion, Rule breaking) {
        if (explosion.enabled() || breaking.enabled()) MachineOverload.explode(machine,
                explosion.enabled() && GregTechTags.Energy.ALL_EXPLODING.contains(type) ? 4F : 0.1F);
        return false;
    }
}
