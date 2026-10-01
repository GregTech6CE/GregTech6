package com.gregtech.gregtech.registration;

import com.gregtech.gregtech.api.tool.ToolInteractionSpec;
import com.gregtech.gregtech.api.tool.ToolInteractions;
import com.gregtech.gregtech.block.machine.MachineRotationType;
import com.gregtech.gregtech.util.GTPlacementCode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Face mapping at the world border, original GT6 corners, and legal rotation properties. */
public final class ToolInteractionContracts {
    public static void main(String[] args) {
        check(com.gregtech.gregtech.data.MachineRecipeMaps.Mortar != com.gregtech.gregtech.data.MachineRecipeMaps.Sharpening, "Mortar recipes must not enter Sharpening");
        for (Direction face : Direction.values()) {
            check(GTPlacementCode.getSideWrenching(face, .5F, .5F, .5F) == face, "center " + face);
            check(GTPlacementCode.getSideWrenching(face, .1F, .1F, .1F) == face.getOpposite(), "corner " + face);
        }
        check(com.gregtech.gregtech.api.energy.EnergyPackets.magnitude(-16, true) == 16, "reverse RU packet");
        check(com.gregtech.gregtech.api.energy.EnergyPackets.magnitude(-16, false) == 0, "unsigned energy");
        check(com.gregtech.gregtech.api.energy.EnergyPackets.magnitude(Long.MIN_VALUE, true) == 0, "magnitude overflow");
        check(com.gregtech.gregtech.api.energy.EnergyPackets.fitting(Long.MAX_VALUE, 50, 16) == 3, "packet multiplication overflow");
        int[] positions = {0, -100, 100, -29999990, 29999990};
        for (int coordinate : positions) {
            BlockPos pos = new BlockPos(coordinate, 80, coordinate);
            BlockHitResult hit = new BlockHitResult(new Vec3(coordinate + .1, 80.5, coordinate), Direction.NORTH, pos, false);
            check(ToolInteractions.selectedFace(hit) == Direction.WEST, "world-coordinate rounding " + coordinate);
        }
        var horizontal = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
        var all = DirectionProperty.create("facing");
        for (Direction face : Direction.values()) {
            check(ToolInteractionSpec.facing(horizontal, MachineRotationType.HORIZONTAL).allows(face) == face.getAxis().isHorizontal(), "horizontal policy");
            check(ToolInteractionSpec.facing(all, MachineRotationType.ALL).allows(face), "six-way policy");
            check(ToolInteractionSpec.facing(horizontal, MachineRotationType.ALL).allows(face) == face.getAxis().isHorizontal(), "property validation");
        }
        System.out.println("Tool interaction contracts passed: centers, corners, large coordinates, four/six-way policies.");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
