package com.gregtech.gregtech.mixin;

import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Original progressmeter also exposes the vanilla spawner's raw countdown. */
@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {
    @Accessor("spawnDelay") int gregtech$spawnDelay();
}
