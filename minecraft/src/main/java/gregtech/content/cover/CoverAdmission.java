package gregtech.content.cover;

import gregtech.content.block.misc.AdvancedCraftingTableBlockEntity;
import gregtech.content.blockentity.BushBlockEntity;
import gregtech.content.blockentity.SapBagBlockEntity;
import gregtech.content.blockentity.TreeHoleBlockEntity;
import gregtech.content.blockentity.energy.EnergyNodeBlockEntity;
import gregtech.content.blockentity.energy.PlacedReactorRodBlockEntity;
import gregtech.content.blockentity.inventory.BookShelfBlockEntity;
import gregtech.content.blockentity.inventory.BottleCrateBlockEntity;
import gregtech.content.blockentity.inventory.DrawerQuadBlockEntity;
import gregtech.content.blockentity.inventory.EnderGarbageBlockEntity;
import gregtech.content.blockentity.inventory.EnderGarbageDumpBlockEntity;
import gregtech.content.blockentity.inventory.MassStorageBlockEntity;
import gregtech.content.blockentity.inventory.SafeBlockEntity;
import gregtech.content.blockentity.machine.CrucibleCrossingBlockEntity;
import gregtech.content.blockentity.machine.CrucibleFaucetBlockEntity;
import gregtech.content.blockentity.machine.EngineBaseBlockEntity;
import gregtech.content.blockentity.machine.HopperBlockEntity;
import gregtech.content.blockentity.machine.LargeGasTurbineControllerBlockEntity;
import gregtech.content.blockentity.machine.LargeTurbineControllerBlockEntity;
import gregtech.content.blockentity.machine.MoldBasinBlockEntity;
import gregtech.content.blockentity.machine.MoldBlockEntity;
import gregtech.content.blockentity.machine.QueueHopperBlockEntity;
import gregtech.content.blockentity.misc.BumbleHiveBlockEntity;
import gregtech.content.blockentity.misc.CoinPileBlockEntity;
import gregtech.content.blockentity.misc.PileBlockEntity;
import gregtech.content.blockentity.sensor.SensorBlockEntity;
import gregtech.content.blockentity.tool.AdvancedButtonBlockEntity;
import gregtech.content.blockentity.tool.BumbliaryBlockEntity;
import gregtech.content.blockentity.tool.CoinMoldBlockEntity;
import gregtech.content.blockentity.tool.DustFunnelBlockEntity;
import gregtech.content.blockentity.tool.DynamiteBlockEntity;
import gregtech.content.blockentity.tool.ManualToolBlockEntity;
import gregtech.content.blockentity.tool.MaterialAnvilBlockEntity;
import gregtech.content.blockentity.tool.PortableContainerBlockEntity;
import gregtech.content.blockentity.tool.ProcessingToolBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Host admission for covers: the port of GT6 {@code TileEntityBase04Covers.allowCovers(aSide)} and
 * {@code allowCover(aSide, aCover)}.
 *
 * <p>{@link gregtech.mixin.ComponentCoverHostMixin} gives every block entity cover storage, so
 * "the entity has a cover slot" is not the same as "GT6 lets a cover be attached here". Every attach
 * path — the cover interaction handler, the native {@code attachCover} stores, the mixin fallback and
 * the panel items' {@code useOn} — must ask this class, otherwise the panel items bypass the
 * interaction handler and still attach covers to hosts GT6 refuses.</p>
 */
public final class CoverAdmission {
    private CoverAdmission() {}

    /** GT6 {@code allowCovers(aSide)} for one host face. */
    public static boolean allowCovers(BlockEntity owner, Direction side) {
        if (owner == null || side == null) return false;
        // The mixin supplies storage on every block entity; only the mod's own blocks are cover hosts.
        if (!isGregTechEntity(owner)) return false;
        // Original base classes below TileEntityBase06Covers never carry covers.
        if (owner instanceof PileBlockEntity || owner instanceof CoinPileBlockEntity
                || owner instanceof ProcessingToolBlockEntity) return false;
        // Hosts whose allowCovers(aSide) is a plain F.
        if (owner instanceof MaterialAnvilBlockEntity
                || owner instanceof SensorBlockEntity
                || owner instanceof TreeHoleBlockEntity
                || owner instanceof PortableContainerBlockEntity
                || owner instanceof CrucibleFaucetBlockEntity
                || owner instanceof CrucibleCrossingBlockEntity
                || owner instanceof MoldBlockEntity
                || owner instanceof MoldBasinBlockEntity
                // MultiTileEntityCrucible/Smeltery: every crucible refuses covers, large or small.
                || owner instanceof gregtech.engine.machine.ITileEntityCrucible
                || owner instanceof SapBagBlockEntity
                || owner instanceof CoinMoldBlockEntity
                || owner instanceof DynamiteBlockEntity
                || owner instanceof BumbleHiveBlockEntity
                || owner instanceof BushBlockEntity
                || owner instanceof EnderGarbageBlockEntity
                || owner instanceof EnderGarbageDumpBlockEntity
                || owner instanceof BottleCrateBlockEntity
                || owner instanceof PlacedReactorRodBlockEntity
                || owner instanceof ManualToolBlockEntity
                || owner instanceof AdvancedButtonBlockEntity) return false;
        // MultiTileEntityHopper/DustFunnel: top face only.
        if (owner instanceof HopperBlockEntity || owner instanceof QueueHopperBlockEntity
                || owner instanceof DustFunnelBlockEntity) return side == Direction.UP;
        // MultiTileEntityBumbliary: bottom and horizontal faces.
        if (owner instanceof BumbliaryBlockEntity) return side != Direction.UP;
        // MultiTileEntityAdvancedCraftingTable: bottom and horizontal faces, never along the facing axis.
        if (owner instanceof AdvancedCraftingTableBlockEntity table) {
            Direction facing = facing(table, HorizontalDirectionalBlock.FACING);
            return facing == null || side != Direction.UP && side.getAxis() != facing.getAxis();
        }
        // MultiTileEntityBookShelf: every face not along the facing axis.
        if (owner instanceof BookShelfBlockEntity shelf) {
            Direction facing = facing(shelf, HorizontalDirectionalBlock.FACING);
            return facing == null || side.getAxis() != facing.getAxis();
        }
        // MultiTileEntitySafe and MultiTileEntityDrawerQuad: every face except the front.
        if (owner instanceof SafeBlockEntity safe) {
            Direction facing = facing(safe, DirectionalBlock.FACING);
            return facing == null || side != facing;
        }
        if (owner instanceof DrawerQuadBlockEntity drawer) {
            Direction facing = facing(drawer, DirectionalBlock.FACING);
            return facing == null || side != facing;
        }
        // MultiTileEntityLargeTurbine: every face except the front.
        if (owner instanceof LargeTurbineControllerBlockEntity || owner instanceof LargeGasTurbineControllerBlockEntity) {
            Direction facing = facing(owner, DirectionalBlock.FACING);
            return facing == null || side != facing;
        }
        // MultiTileEntityEngine*: only the two faces along the facing axis.
        if (owner instanceof EngineBaseBlockEntity engine) {
            Direction facing = facing(engine, DirectionalBlock.FACING);
            return facing == null || side.getAxis() == facing.getAxis();
        }
        // TileEntityBase08Battery: no covers at all. The transformer has fixed input/output faces,
        // so the port refuses covers there as well even though GT6's converter base allows them.
        if (owner instanceof EnergyNodeBlockEntity node && (node.isBatteryBox() || node.isElectricTransformer()))
            return false;
        return true;
    }

    /** GT6 {@code allowCover(aSide, aCover)}: the host may additionally restrict the cover type. */
    public static boolean allowCover(BlockEntity owner, Direction side, ItemStack stack) {
        if (!allowCovers(owner, side)) return false;
        // MultiTileEntityBarrelWood/Plastic: decorative covers only.
        if (owner instanceof MassStorageBlockEntity) return isDecorative(stack);
        return true;
    }

    /** GT6 {@code ICover.isDecorative}: texture, canvas and crafting covers. */
    public static boolean isDecorative(ItemStack stack) {
        String id = CoverItems.behavior(stack);
        return "material_plate_cover".equals(id) || "decorative_panel".equals(id)
                || "canvas_cover".equals(id) || CoverUtilityBehaviors.ASPHALT_PANEL.equals(id)
                || CoverUtilityBehaviors.CRAFTING_TABLE.equals(id);
    }

    /** True when at least one face accepts covers, e.g. for the targeting grid. */
    public static boolean allowAnyCover(BlockEntity owner) {
        if (owner == null) return false;
        for (Direction side : Direction.values()) if (allowCovers(owner, side)) return true;
        return false;
    }

    private static boolean isGregTechEntity(BlockEntity owner) {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().startsWith("gregtech.")) return true;
        return false;
    }

    private static Direction facing(BlockEntity owner, DirectionProperty property) {
        BlockState state = owner.getBlockState();
        return state.hasProperty(property) ? state.getValue(property) : null;
    }
}
