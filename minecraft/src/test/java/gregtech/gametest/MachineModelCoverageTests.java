package gregtech.gametest;

import gregtech.client.OrthogonalMachineFamilies;
import gregtech.content.block.energy.OriginalMotorBlock;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.EngineBlock;
import gregtech.content.registry.GTBasicMachines;
import gregtech.content.registry.GTBurningBoxes;
import gregtech.content.registry.GTEnergyNodes;
import gregtech.content.registry.GTEngines;
import gregtech.content.registry.GTMultiblocks;
import gregtech.content.registry.GTPumps;
import gregtech.engine.machine.EngineType;
import gregtech.engine.machine.OrthogonalMachineModels;
import gregtech.engine.mod.GregTechIdentity;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * {@code machine_models.tsv} serves exactly the families that lost their static blockstates.
 *
 * <p>The table only replaces models this repository generated, so the client must serve a table row
 * for every state of those blocks and must leave every other block alone: a family that drops out of
 * the list keeps a static model that no longer exists, and a family that creeps in gets placeholder
 * geometry over art that still ships.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MachineModelCoverageTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void onlyTheControllersWithoutStaticModelsUseTheTable(GameTestHelper helper) {
        List<Block> blocks = OrthogonalMachineFamilies.blocks();
        helper.assertTrue(
                blocks.contains(GTMultiblocks.COKE_OVEN_MAIN.get()),
                "The coke-oven controller lost the table");
        helper.assertFalse(
                blocks.contains(GTMultiblocks.CRYO_DISTILLATION_MAIN.get()),
                "The cryogenic distillation controller still ships static models, so it stays out");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void everyTableBlockStateNamesATableRow(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        int states = 0;
        for (Block block : OrthogonalMachineFamilies.blocks()) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            List<OrthogonalMachineFamilies.Variant> variants = OrthogonalMachineFamilies.itemVariants(block);
            boolean inventoryRow =
                    variants.stream().anyMatch(variant -> OrthogonalMachineFamilies.usesTable(variant.modelId()));
            if (!inventoryRow) {
                problems.add(id + " has no inventory row");
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                OrthogonalMachineFamilies.Variant variant = OrthogonalMachineFamilies.world(state);
                if (variant == null) {
                    problems.add(id + "#" + state + " has no variant");
                } else if (!OrthogonalMachineModels.contains(variant.modelId().getPath())) {
                    problems.add(id + "#" + state + " names a missing row " + variant.modelId());
                }
                states++;
            }
        }
        helper.assertTrue(states > 0, "No table block states were checked");
        helper.assertTrue(problems.isEmpty(), "Blocks the table cannot draw: " + problems);
        System.out.println("MACHINE_MODEL_COVERAGE states=" + states);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void everyTableFamilyIsListed(GameTestHelper helper) {
        List<Block> blocks = OrthogonalMachineFamilies.blocks();
        List<String> problems = new ArrayList<>();
        int basics = 0;
        for (var entry : GTBasicMachines.all()) {
            if (!entry.isPresent()) continue;
            BasicMachineBlock machine = entry.get();
            // Addon machines are registered through the same catalog but keep their own namespace,
            // whose model files and geometry are the addon's own; only this repository's reach the table.
            boolean ours = GregTechIdentity.REGISTRY_NAMESPACE.equals(machine.basicSpec().modelId("").getNamespace());
            if (blocks.contains(machine) != ours)
                problems.add("basic machine " + BuiltInRegistries.BLOCK.getKey(machine));
            if (ours) basics++;
        }
        int boxes = 0;
        for (var entry : GTBurningBoxes.solidBoxes()) {
            if (!entry.isPresent()) continue;
            boxes++;
            if (!blocks.contains(entry.get()))
                problems.add("solid burning box " + BuiltInRegistries.BLOCK.getKey(entry.get()));
        }
        for (var entry : GTBurningBoxes.fuelBoxes()) {
            if (!entry.isPresent()) continue;
            boxes++;
            if (!blocks.contains(entry.get()))
                problems.add("new burning box " + BuiltInRegistries.BLOCK.getKey(entry.get()));
        }
        int nodes = 0;
        for (var entry : GTEnergyNodes.all()) {
            if (!entry.isPresent() || !OrthogonalMachineFamilies.sharedCube(entry.get())) continue;
            nodes++;
            if (!blocks.contains(entry.get()))
                problems.add("energy node " + BuiltInRegistries.BLOCK.getKey(entry.get()));
        }
        helper.assertTrue(basics > 0 && boxes > 0 && nodes > 0, "No table family was checked");
        helper.assertTrue(problems.isEmpty(), "Table geometry without a listed block: " + problems);
        helper.assertTrue(
                blocks.contains(GTMultiblocks.COKE_OVEN_MAIN.get()),
                "The coke-oven controller is a basic machine registered with the multiblocks");
        for (String asset : new String[] {
            "/assets/gt6ce/blockstates/cryo_distillation_main.json",
            "/assets/gt6ce/models/item/cryo_distillation_main.json"
        }) {
            helper.assertTrue(
                    MachineModelCoverageTests.class.getResource(asset) != null,
                    "The cryogenic distillation controller keeps its static model: " + asset);
        }
        System.out.println("MACHINE_MODEL_COVERAGE basics=" + basics + " boxes=" + boxes + " nodes=" + nodes);
        helper.succeed();
    }

    /**
     * Inventory models keep the working overlay for the GUI and the idle shell everywhere else.
     *
     * <p>The GUI bake is the first row, so picking a machine in a GUI still shows its overlay; the
     * held, dropped and item-framed copy takes the idle geometry, and a burning box never shows a
     * lit front before it is placed. Engines and the rotational pumps ship static item JSON, so
     * their files must point at the idle row too.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void itemModelsMatchDisplayStatePolicy(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        int basics = 0;
        for (BasicMachineBlock machine : OrthogonalMachineFamilies.basicMachineBlocks()) {
            basics++;
            String id = BuiltInRegistries.BLOCK.getKey(machine).toString();
            List<OrthogonalMachineFamilies.Variant> variants = OrthogonalMachineFamilies.itemVariants(machine);
            String active = machine.basicSpec().modelId("_active").getPath();
            String idle = machine.basicSpec().modelId("").getPath();
            if (variants.size() != 2) {
                problems.add(id + " has " + variants.size() + " item rows instead of an active and an idle row");
                continue;
            }
            if (!active.equals(variants.get(0).modelId().getPath())) {
                problems.add(id + " lists " + variants.get(0).modelId().getPath() + " instead of " + active);
            }
            if (!idle.equals(variants.get(1).modelId().getPath())) {
                problems.add(id + " lists " + variants.get(1).modelId().getPath() + " instead of " + idle);
            } else if (!OrthogonalMachineModels.contains(idle)) {
                problems.add(idle + " is not a table row");
            }
        }

        int nodes = 0;
        int motors = 0;
        for (var entry : GTEnergyNodes.all()) {
            if (!entry.isPresent() || !OrthogonalMachineFamilies.sharedCube(entry.get())) continue;
            var node = entry.get();
            nodes++;
            if (node instanceof OriginalMotorBlock) motors++;
            String expected = "block/machine/energy/" + node.spec().id();
            String row = firstInventoryRow(node);
            if (!expected.equals(row)) {
                problems.add(BuiltInRegistries.BLOCK.getKey(node) + " shows " + row + " instead of " + expected);
            } else if (!OrthogonalMachineModels.contains(row)) {
                problems.add(row + " is not a table row");
            }
        }

        int boxes = 0;
        for (Block block : burningBoxes()) {
            boxes++;
            String row = firstInventoryRow(block);
            // The folder differs per box, so only the extinguished front is pinned here.
            if (row == null || !row.endsWith("/north_off")) {
                problems.add(BuiltInRegistries.BLOCK.getKey(block) + " shows " + row + " instead of an idle front");
            } else if (!OrthogonalMachineModels.contains(row)) {
                problems.add(row + " is not a table row");
            }
        }

        int staticModels = 0;
        for (Block block : staticItemModelBlocks()) {
            String json = itemModelJson(block);
            // An addon's item model ships with the addon, not here.
            if (json == null) continue;
            staticModels++;
            if (json.contains("_active")) {
                problems.add(BuiltInRegistries.BLOCK.getKey(block) + " item model points at an active row");
            }
        }
        if (OrthogonalMachineModels.contains("block/machine/pump/rotational_pump_active")) {
            problems.add("the table still generates the rotational pump's active row");
        }

        helper.assertTrue(basics > 0 && nodes > 0 && motors > 0 && boxes > 0 && staticModels > 0,
                "No family was checked: basics=" + basics + " nodes=" + nodes + " motors=" + motors
                        + " boxes=" + boxes + " static models=" + staticModels);
        helper.assertTrue(problems.isEmpty(), "Items that do not match the display policy: " + problems);
        System.out.println("MACHINE_MODEL_COVERAGE basics=" + basics + " nodes=" + nodes + " motors=" + motors
                + " boxes=" + boxes + " staticModels=" + staticModels);
        helper.succeed();
    }

    /** Every burning box, solid and liquid, gas or fluidized bed. */
    private static List<Block> burningBoxes() {
        List<Block> boxes = new ArrayList<>();
        for (var entry : GTBurningBoxes.solidBoxes()) if (entry.isPresent()) boxes.add(entry.get());
        for (var entry : GTBurningBoxes.fuelBoxes()) if (entry.isPresent()) boxes.add(entry.get());
        return boxes;
    }

    /** The row the inventory model shows first; null when the block has no inventory row. */
    private static String firstInventoryRow(Block block) {
        List<OrthogonalMachineFamilies.Variant> variants = OrthogonalMachineFamilies.itemVariants(block);
        return variants.isEmpty() ? null : variants.get(0).modelId().getPath();
    }

    /** Blocks whose item model is a static JSON file: the rotational pumps and the engines. */
    private static List<Block> staticItemModelBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (var entry : GTPumps.all()) if (entry.isPresent()) blocks.add(entry.get());
        for (var entry : GTEngines.all()) {
            if (!entry.isPresent()) continue;
            EngineBlock engine = entry.get();
            // Steam, electric and flux engines keep their own item art, which this change leaves alone.
            if (engine.engineType() == EngineType.ROTATION || engine.engineType() == EngineType.DIESEL) {
                blocks.add(engine);
            }
        }
        return blocks;
    }

    /** The block item's static model JSON, or null when the block ships none in this pack. */
    private static String itemModelJson(Block block) {
        String path = "/assets/gt6ce/models/item/" + BuiltInRegistries.BLOCK.getKey(block).getPath() + ".json";
        try (InputStream stream = MachineModelCoverageTests.class.getResourceAsStream(path)) {
            return stream == null ? null : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Cannot read " + path, e);
        }
    }
}