package gregtech.client;

import gregtech.content.block.energy.BatteryBoxBlock;
import gregtech.content.block.energy.ElectricTransformerBlock;
import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.block.energy.MagnetMachineBlock;
import gregtech.content.block.energy.OriginalMotorBlock;
import gregtech.content.block.energy.RotaryConverterBlock;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.BurningBoxBlock;
import gregtech.content.block.machine.GTFacingMachineBlock;
import gregtech.content.block.machine.SolidBurningBoxBlock;
import gregtech.content.registry.GTBasicMachines;
import gregtech.content.registry.GTBurningBoxes;
import gregtech.content.registry.GTEnergyNodes;
import gregtech.content.registry.GTMultiblocks;
import gregtech.engine.machine.OrthogonalMachineModels;
import gregtech.engine.mod.GregTechIdentity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Which state of a machine is which row of {@code machine_models.tsv}, and how that row is rotated.
 *
 * <p>The cache-stabilised blockstate JSON files spelled this out per block: one model file per
 * lit/running/activity combination and one variant per facing. The rules below are those files
 * written once, so the shared pack's blockstates and the item display aliasing share a single
 * source.
 */
public final class OrthogonalMachineFamilies {
    /** The namespace that owns {@code machine_models.tsv}; other packs keep their own static models. */
    private static final String TABLE_NAMESPACE = GregTechIdentity.REGISTRY_NAMESPACE;

    private OrthogonalMachineFamilies() {}

    /** One table row plus the bakery rotation the blockstate previously carried. */
    public record Variant(ResourceLocation modelId, int xRot, int yRot, boolean uvLock) {
        public static Variant flat(ResourceLocation modelId) {
            return new Variant(modelId, 0, 0, false);
        }
    }

    /** True when the model id names a row of the table, which is the only geometry this client bakes. */
    public static boolean usesTable(ResourceLocation modelId) {
        return TABLE_NAMESPACE.equals(modelId.getNamespace()) && OrthogonalMachineModels.contains(modelId.getPath());
    }

    /** True when at least one inventory model of the block is a table row. */
    public static boolean usesTable(Block block) {
        for (Variant variant : itemVariants(block)) if (usesTable(variant.modelId())) return true;
        return false;
    }

    /**
     * The basic machines that take their geometry from the table, in registration order: the
     * registered basic machines plus the coke-oven controller, which is a basic machine registered
     * with the multiblocks. Addon machines keep their own static models, so a block of theirs never
     * reaches the placeholder pass.
     */
    public static List<BasicMachineBlock> basicMachineBlocks() {
        List<BasicMachineBlock> machines = new ArrayList<>();
        for (var entry : GTBasicMachines.all()) if (entry.isPresent()) machines.add(entry.get());
        // The cryo distillation controller is a basic machine too, but its static blockstate and item
        // model are still shipped, so it stays out of this list on purpose.
        if (GTMultiblocks.COKE_OVEN_MAIN.isPresent())
            machines.add((BasicMachineBlock) GTMultiblocks.COKE_OVEN_MAIN.get());
        return machines.stream().filter(OrthogonalMachineFamilies::usesTable).toList();
    }

    /** Blocks that take their geometry from the table, in registration order. */
    public static List<Block> blocks() {
        List<Block> blocks = new ArrayList<>(basicMachineBlocks());
        for (var entry : GTBurningBoxes.solidBoxes())
            if (entry.isPresent() && usesTable(entry.get())) blocks.add(entry.get());
        for (var entry : GTBurningBoxes.fuelBoxes())
            if (entry.isPresent() && usesTable(entry.get())) blocks.add(entry.get());
        for (var entry : GTEnergyNodes.all()) {
            // Solar panels and the plain energy nodes keep their own dedicated geometry.
            if (entry.isPresent() && sharedCube(entry.get()) && usesTable(entry.get())) blocks.add(entry.get());
        }
        return blocks;
    }

    /** The row and rotation that draws one world state; null for blocks with their own geometry. */
    public static @Nullable Variant world(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof BasicMachineBlock basic) {
            boolean lit = state.getValue(GTFacingMachineBlock.LIT);
            // Any lit machine shows its working overlay; running alone is the standby overlay.
            String suffix = lit ? "_active" : state.getValue(BasicMachineBlock.RUNNING) ? "_running" : "";
            return new Variant(
                    basic.basicSpec().modelId(suffix),
                    0,
                    horizontalRotation(state.getValue(GTFacingMachineBlock.FACING)),
                    false);
        }
        if (block instanceof GTFacingMachineBlock box
                && (box instanceof SolidBurningBoxBlock || box instanceof BurningBoxBlock)) {
            boolean lit = state.getValue(GTFacingMachineBlock.LIT);
            // Each facing has its own model, so the geometry itself is never rotated.
            return Variant.flat(tableId("block/machine/" + burningBoxFolder(box) + "/"
                    + state.getValue(GTFacingMachineBlock.FACING).getSerializedName() + (lit ? "_on" : "_off")));
        }
        if (block instanceof EnergyNodeBlock node && sharedCube(node)) {
            Direction facing = state.getValue(EnergyNodeBlock.FACING);
            return energyNodeVariant(node, state, facing);
        }
        return null;
    }

    /**
     * Inventory models in priority order.
     *
     * <p>A basic machine lists its working overlay first and its idle shell second: the GUI bake
     * takes the first row that exists, the world display the idle one. Burning boxes and energy
     * nodes list an idle row only, which is the item row itself.
     */
    public static List<Variant> itemVariants(Block block) {
        if (block instanceof BasicMachineBlock basic) {
            return List.of(
                    Variant.flat(basic.basicSpec().modelId("_active")),
                    Variant.flat(basic.basicSpec().modelId("")));
        }
        if (block instanceof GTFacingMachineBlock box
                && (box instanceof SolidBurningBoxBlock || box instanceof BurningBoxBlock)) {
            // The item always shows the extinguished front, so it never looks lit before placement.
            return List.of(Variant.flat(tableId("block/machine/" + burningBoxFolder(box) + "/north_off")));
        }
        if (block instanceof EnergyNodeBlock node && sharedCube(node)) {
            // Every node shows its idle shell; a running front never reaches an item.
            return List.of(
                    Variant.flat(tableId("block/machine/energy/" + node.spec().id())));
        }
        return List.of();
    }

    /** True for the energy nodes that are a plain cube rotating on all six faces. */
    public static boolean sharedCube(EnergyNodeBlock node) {
        return node instanceof BatteryBoxBlock
                || node instanceof ElectricTransformerBlock
                || node instanceof RotaryConverterBlock // also covers the heaters and motors
                || node instanceof MagnetMachineBlock;
    }

    private static Variant energyNodeVariant(EnergyNodeBlock node, BlockState state, Direction facing) {
        String suffix;
        if (node instanceof BatteryBoxBlock) {
            int charge = state.getValue(BatteryBoxBlock.CHARGE_STATE);
            suffix = charge == 0 ? "" : "_" + charge;
        } else if (node instanceof ElectricTransformerBlock) {
            int activity = state.getValue(ElectricTransformerBlock.ACTIVITY);
            suffix = activity == 0 ? "" : "_" + activity;
        } else if (node instanceof OriginalMotorBlock) {
            // An idle motor shows the plain shell; a running one shows its direction and speed.
            suffix = state.getValue(RotaryConverterBlock.ACTIVITY) == 0
                    ? ""
                    : (state.getValue(OriginalMotorBlock.COUNTER_CLOCKWISE) ? "_l" : "_r")
                            + (state.getValue(OriginalMotorBlock.FAST) ? "f" : "s");
        } else if (node instanceof MagnetMachineBlock) {
            suffix = state.getValue(MagnetMachineBlock.ACTIVE) ? "_active" : "";
        } else {
            suffix = state.getValue(RotaryConverterBlock.ACTIVITY) == 0 ? "" : "_active";
        }
        return rotated(tableId("block/machine/energy/" + node.spec().id() + suffix), facing);
    }

    /** The six axis settings the static energy blockstates used for one shared model. */
    private static Variant rotated(ResourceLocation modelId, Direction facing) {
        return switch (facing) {
            case NORTH -> new Variant(modelId, 0, 0, false);
            case EAST -> new Variant(modelId, 0, 90, true);
            case SOUTH -> new Variant(modelId, 0, 180, true);
            case WEST -> new Variant(modelId, 0, 270, true);
            case UP -> new Variant(modelId, 270, 0, true);
            case DOWN -> new Variant(modelId, 90, 0, true);
        };
    }

    /** A model id this repository owns, for the rows written here rather than handed over by a spec. */
    private static ResourceLocation tableId(String path) {
        return ResourceLocation.fromNamespaceAndPath(TABLE_NAMESPACE, path);
    }

    /** Basic machines are horizontal only: north is the unrotated model. */
    private static int horizontalRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    /**
     * Brick boxes have their own shell; the fluid boxes are named after their fuel. The static
     * {@code burning_box_solid_brick} blockstate pointed at the solid shell, which is why the brick
     * box looked like plain steel.
     */
    private static String burningBoxFolder(GTFacingMachineBlock block) {
        if (block instanceof BurningBoxBlock fuel) {
            return switch (fuel.fuelType()) {
                case LIQUID -> "burning_liquid";
                case GAS -> "burning_gas";
                case FLUIDIZED_BED -> "burning_fluidbed";
                case SOLID -> "burning_box_solid";
            };
        }
        return block.spec().id().contains("_brick") ? "burning_box_brick" : "burning_box_solid";
    }
}
