/* Copyright (c) 2025 GregTech-6 Team; Gregorius Techneticies.
 * LGPL-3.0-or-later. Adapted from TileEntityBase01Root.overcharge. */
package gregtech.internal.energy;

import gregtech.GregTechConfig;
import gregtech.content.energy.OriginalRotaryConverter;
import gregtech.data.GregTechTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Native effects of the original optional overload policy; packet limits stay in each device. */
public final class MachineOverload {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private MachineOverload() {}

    public static void apply(BlockEntity machine, long size, String energy) {
        var type = GregTechTags.Energy.ALL.stream()
                .filter(tag -> tag.getShortName().equalsIgnoreCase(energy) || tag.getId().equals(energy))
                .findFirst().orElse(null);
        apply(machine, size, type);
    }

    public static void apply(BlockEntity machine, long size, GregTechTags.Tag type) {
        var level = machine.getLevel();
        if (level == null || level.isClientSide) return;
        var pos = machine.getBlockPos();
        if (GregTechConfig.breakByOverload()) {
            float power = type == null ? 0.1F : OriginalRotaryConverter.overloadPower(size, type);
            explode(machine, GregTechConfig.explodeByOverload() ? power : 0.1F);
        }
        // GT6 maps kinetic interruption to random.break, even with both damage options disabled.
        boolean kinetic = type != null && GregTechTags.Energy.ALL_KINETIC.contains(type);
//? if >=1.21.1 {
        /*var sound = kinetic ? SoundEvents.ITEM_BREAK : SoundEvents.GENERIC_EXPLODE.value();
*///?} else {
        var sound = kinetic ? SoundEvents.ITEM_BREAK : SoundEvents.GENERIC_EXPLODE;
//?}
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1, 1);
        LOGGER.warn("Machine overcharged at {} with {} {}", pos, size, type == null ? "unknown energy" : type.getShortName());
    }

    /** Direct source explode(), also used by the automatic hammer's deliberate overload exception. */
    public static void explode(BlockEntity machine, float power) {
        var level = machine.getLevel();
        if (level == null || level.isClientSide || power <= 0 || machine.isRemoved()
                || level.getBlockEntity(machine.getBlockPos()) != machine) return;
        var pos = machine.getBlockPos();
        level.removeBlock(pos, false);
        if (power >= 1) {
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    power, Level.ExplosionInteraction.BLOCK);
        } else {
//? if >=1.21.1 {
            /*level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 1);
*///?} else {
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1, 1);
//?}
        }
    }
}
