package com.gregtech.gregtech.mixin;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Modern XP orbs can represent several original orbs; do not delete the unconverted remainder. */
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbCountAccessor {@Accessor("count") int gregtech$count();}
