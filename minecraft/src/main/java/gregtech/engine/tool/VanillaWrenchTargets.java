package gregtech.engine.tool;

import gregtech.content.block.machine.MachineRotationType;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * The vanilla half of GT6's wrench click: the block list that draws GT6's 3×3 wrench overlay
 * ({@code GT_API_Proxy_Client}) and the rotation policy of {@code ToolCompat}.
 *
 * <p>The twelve original blocks are the only ones upstream knows; the observer, barrel and crafter
 * did not exist in 1.7.10 and are extended here with the same face grid and facing-side pickup.
 * The crafter's state is a front/top pair instead of a single facing, so it has no single-direction
 * preview spec and is handled by {@code gregtech.event.VanillaWrenchHandler} alone.</p>
 *
 * <p>The pillar, workbench and bookshelf branches of the same {@code ToolCompat} method have no
 * grid and live in the handler. Modern block states replace the 1.7.10 metadata, so the policy is
 * read from the state property rather than from a metadata number.</p>
 */
public final class VanillaWrenchTargets {
    private VanillaWrenchTargets() {}

    /** GT6's rotation policy for the block, or {@code null} when the wrench has no face grid on it. */
    @Nullable
    public static MachineRotationType policy(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof PistonBaseBlock || block instanceof DispenserBlock || block instanceof ObserverBlock) {
            return MachineRotationType.ALL;
        }
        if (block instanceof HopperBlock) {
            return MachineRotationType.BOTTOM_HORIZONTAL;
        }
        // Blast furnaces and smokers are modern blocks that GT6 never had.
        if (block instanceof ChestBlock
                || block instanceof EnderChestBlock
                || block instanceof BarrelBlock
                || block instanceof PumpkinBlock
                || block == Blocks.FURNACE) {
            return MachineRotationType.HORIZONTAL;
        }
        return null;
    }

    /** The state's directed property; every block in the grid list names it {@code facing}. */
    @Nullable
    public static DirectionProperty facing(BlockState state) {
        for (var property : state.getProperties()) {
            if ("facing".equals(property.getName()) && property instanceof DirectionProperty facing) {
                return facing;
            }
        }
        return null;
    }

    /** The preview spec for the client overlay, or {@code null} when the held tool gets no grid. */
    @Nullable
    public static ToolInteractionSpec spec(BlockState state, ItemStack tool) {
        if (!GTToolHelper.isMachineWrench(tool)) {
            return null;
        }
        MachineRotationType rotation = policy(state);
        DirectionProperty facing = facing(state);
        if (rotation == null || facing == null) {
            return null;
        }
        return ToolInteractionSpec.facing(facing, rotation);
    }
}
