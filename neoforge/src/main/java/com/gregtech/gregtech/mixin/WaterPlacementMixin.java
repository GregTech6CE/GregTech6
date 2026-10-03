package com.gregtech.gregtech.mixin;

import com.gregtech.gregtech.api.fluid.WaterFamilyIdentity;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.MangroveRootsBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vanilla waterlogged placement uses fluid identity instead of the common water tag. */
@Mixin({BarrierBlock.class, AmethystClusterBlock.class, CandleBlock.class, CeilingHangingSignBlock.class, ChainBlock.class, DecoratedPotBlock.class, EnderChestBlock.class, FenceBlock.class, HangingRootsBlock.class, IronBarsBlock.class, LadderBlock.class, LanternBlock.class, LightningRodBlock.class, MangroveRootsBlock.class, ScaffoldingBlock.class, SeaPickleBlock.class, SlabBlock.class, StairBlock.class, StandingSignBlock.class, WallBlock.class, WallHangingSignBlock.class, WallSignBlock.class, BaseRailBlock.class, CampfireBlock.class, ChestBlock.class, LeavesBlock.class, MangrovePropaguleBlock.class, PointedDripstoneBlock.class, SculkSensorBlock.class, SculkShriekerBlock.class, TrapDoorBlock.class})
public abstract class WaterPlacementMixin {
    @Redirect(method = "getStateForPlacement", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;"), require = 1)
    private Fluid gregtech$waterFamilyForPlacement(FluidState state) {
        return WaterFamilyIdentity.forVanillaCheck(state);
    }
}
