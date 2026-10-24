/**
 * Copyright (c) 2023 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech.content.block.misc;

import gregtech.content.logistics.LongDistanceCatalog;
import net.minecraft.world.level.block.Block;

/** Native block boundary for flattened GT6 long-distance wire/pipe metadata. */
public class LongDistPipeBlock extends Block {
    public enum Kind {
        ITEM_PIPE, FLUID_PIPE, WIRE
    }
    private final Kind kind;
    private final long maximumVoltage;
    private final long maximumTemperature;
    private final String networkIdentity;

    public LongDistPipeBlock(LongDistanceCatalog.Spec spec, Properties properties) {
        super(properties);
        kind = Kind.valueOf(spec.kind());
        maximumVoltage = spec.voltage();
        maximumTemperature = spec.maximumTemperature();
        networkIdentity = spec.networkIdentity();
    }

    public boolean isWire() {
        return kind == Kind.WIRE;
    }

    public long maximumVoltage() {
        return maximumVoltage;
    }

    public long maximumTemperature() {
        return maximumTemperature;
    }

    public String networkIdentity() {
        return networkIdentity;
    }

    public boolean sameLine(LongDistPipeBlock other) {
        return networkIdentity.equals(other.networkIdentity);
    }

    /** Only the appropriate original item/fluid line can connect to an endpoint. */
    public boolean acceptsPipeline(boolean fluid) {
        return kind == (fluid ? Kind.FLUID_PIPE : Kind.ITEM_PIPE);
    }

    @Override
    public int getFlammability(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
        return isWire() ? 150 : 0;
    }

    @Override
    public int getFireSpreadSpeed(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side) {
        return isWire() ? 150 : 0;
    }

    @Override
    public void onPlace(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState previous, boolean moving) {
        if (!state.is(previous.getBlock())) LongDistanceTopology.changed(level);
        super.onPlace(state, level, pos, previous, moving);
    }

    @Override
    public void onRemove(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) LongDistanceTopology.changed(level);
        super.onRemove(state, level, pos, next, moving);
    }
}
