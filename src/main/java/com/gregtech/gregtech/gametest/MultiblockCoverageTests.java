package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.data.generated.GT6MultiblockIds;
import com.gregtech.gregtech.data.BlockHarvestPolicy;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * GT6's {@code "Multiblock Machines"} registration inventory, including known inactive shells.
 *
 * <p>The porting notes claimed for a long time that the port had 15 multiblock hosts and was missing
 * the "per-material variants" (8 crucibles, 6 coils, 12 tank valves, 5 boiler barometers, the wall
 * grades, 4 generator housings). That count came from reading {@code registry/GTMultiblocks} alone and
 * never looking at {@code content/multiblock/LargeMachineParts} — which carries GT6's numeric part ids,
 * including exactly those variants — nor at {@code LargeMachineLayouts}, which hosts the blueprint
 * machines ({@code largecentrifuge}, {@code largecrusher}, {@code largeautoclave}, …). The audit tool
 * had the same blind spot: it classified every <em>controller</em> as missing because controllers are
 * registered under the port's own ids.</p>
 *
 * <p>These two tests are the correction: the generated table
 * ({@code data/generated/GT6MultiblockIds}, produced by {@code tools/extract_gt6_multiblock_ids.py}
 * from GT6's own registration file) is walked row by row, and the layout tables' part ids are resolved
 * too — so a dropped variant, a renamed id or a typo in a layout breaks the gate instead of quietly
 * leaving an unbuildable structure. Original numeric IDs still backed only by passive port blocks are
 * counted separately as placeholders, not as completed parts.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class MultiblockCoverageTests {

    /** Registered block paths, for the controller rows: they may carry a per-material suffix. */
    private static Set<String> blockPaths() {
        Set<String> paths = new HashSet<>();
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            if (id != null) paths.add(id.getPath());
        }
        return paths;
    }

    /**
     * Every one of GT6's 117 multiblock registrations has a counterpart: the part rows must answer
     * through {@code LargeMachineParts.find(gt6Id)} (which is the table carrying GT6's numeric ids),
     * the controller rows through a registered block whose path is the port id or starts with it
     * (the material-tiered machines are {@code <machine>_<material>}). A controller row is counted
     * only when its numeric host is no longer a passive port; separate behavior tests cover parity.
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void everyGt6MultiblockRegistrationHasAPortCounterpart(GameTestHelper helper) {
        Set<String> paths = blockPaths();
        List<String> broken = new ArrayList<>();
        int parts = 0;
        int controllers = 0;
        int placeholders = 0;

        for (GT6MultiblockIds.Row row : GT6MultiblockIds.ROWS) {
            if (row.kind() != GT6MultiblockIds.Kind.CONTROLLER) {
                var part = LargeMachineParts.find(row.gt6Id());
                if (part == null || !part.isPresent()) {
                    broken.add(row.gt6Id() + " " + row.gt6Name() + ": LargeMachineParts.find returned nothing");
                    continue;
                }
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(part.get());
                if (id == null || part.get() == Blocks.AIR) {
                    broken.add(row.gt6Id() + " " + row.gt6Name() + ": the part block is not registered");
                    continue;
                }
                if (row.kind() == GT6MultiblockIds.Kind.PLACEHOLDER) {
                    if (!(part.get() instanceof com.gregtech.gregtech.block.machine.MultiblockPortBlock))
                        broken.add(row.gt6Id() + " " + row.gt6Name()
                                + ": labelled placeholder but no longer a passive port; update the audit");
                    placeholders++;
                } else {
                    parts++;
                }
            } else {
                String portId = row.portId();
                boolean found = portId != null && (paths.contains(portId)
                        || paths.stream().anyMatch(path -> path.startsWith(portId + "_")));
                if (!found) {
                    broken.add(row.gt6Id() + " " + row.gt6Name() + ": no block gregtech:" + portId
                            + " (nor a " + portId + "_<material> variant)");
                    continue;
                }
                var numericHost = LargeMachineParts.find(row.gt6Id());
                if (numericHost != null && numericHost.isPresent()
                        && numericHost.get() instanceof com.gregtech.gregtech.block.machine.MultiblockPortBlock) {
                    broken.add(row.gt6Id() + " " + row.gt6Name()
                            + ": labelled active controller but registered as a passive port");
                    continue;
                }
                controllers++;
            }
        }

        GregTech.LOGGER.info("[multiblock] rows={} parts={} controllers={} placeholders={} missing={}",
                GT6MultiblockIds.ROWS.size(), parts, controllers, placeholders, GT6MultiblockIds.MISSING);

        helper.assertTrue(GT6MultiblockIds.MISSING == 0,
                "the generated table lists " + GT6MultiblockIds.MISSING + " unhosted GT6 registrations");
        helper.assertTrue(GT6MultiblockIds.ROWS.size() == 117,
                "GT6's Multiblock Machines group has 117 registrations, the table has "
                        + GT6MultiblockIds.ROWS.size());
        helper.assertTrue(broken.isEmpty(), "GT6 multiblock registrations without a port counterpart: "
                + broken);
        helper.assertTrue(parts == GT6MultiblockIds.PART_COUNT
                        && controllers == GT6MultiblockIds.CONTROLLER_COUNT
                        && placeholders == GT6MultiblockIds.PLACEHOLDER_COUNT,
                "the split matches the table: parts " + parts + "/" + GT6MultiblockIds.PART_COUNT
                        + ", controllers " + controllers + "/" + GT6MultiblockIds.CONTROLLER_COUNT
                        + ", placeholders " + placeholders + "/" + GT6MultiblockIds.PLACEHOLDER_COUNT);
        List<String> unmineable = new ArrayList<>();
        for (var registered : LargeMachineParts.blocks()) {
            Block block = registered.get();
            var state = block.defaultBlockState();
            BlockHarvestPolicy.Tool tool = BlockHarvestPolicy.tool(block);
            TagKey<Block> tag = TagKey.create(Registries.BLOCK,
                    ResourceLocation.parse(BlockHarvestPolicy.tag(tool)));
            if ((tool != BlockHarvestPolicy.Tool.PICKAXE
                    && tool != BlockHarvestPolicy.Tool.AXE
                    && tool != BlockHarvestPolicy.Tool.WRENCH) || !state.is(tag))
                unmineable.add(registered.getId().toString());
        }
        helper.assertTrue(unmineable.isEmpty(),
                "original GT6 structural parts/controllers must have a correct mining tool: " + unmineable);
        helper.succeed();
    }

    /**
     * Every part id the large-machine layouts place resolves to a block.
     *
     * <p>The layouts ({@code LargeMachineLayouts.Cell}) carry GT6's numeric ids, and the structure
     * builder turns them into blocks through {@code LargeMachineParts.block(id)}, which throws on an
     * unknown id. A typo there would only show up when a player builds that machine, so this walks every
     * layout instead.</p>
     */
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void everyPartIdTheLayoutsUseResolves(GameTestHelper helper) {
        List<String> broken = new ArrayList<>();
        Set<Integer> distinct = new TreeSet<>();
        int cells = 0;

        for (String machine : LargeMachineLayouts.machines()) {
            List<LargeMachineLayouts.Cell> layout = LargeMachineLayouts.cells(machine);
            if (layout == null || layout.isEmpty()) {
                broken.add(machine + ": the layout is empty");
                continue;
            }
            for (LargeMachineLayouts.Cell cell : layout) {
                cells++;
                if (cell.part() == 0) continue;              // air cells carry no part id
                distinct.add(cell.part());
                if (LargeMachineParts.find(cell.part()) == null) {
                    broken.add(machine + " places GT6 part " + cell.part() + ", which the port has no id for");
                }
            }
        }

        GregTech.LOGGER.info("[multiblock] layouts={} cells={} distinctPartIds={}",
                LargeMachineLayouts.machines().size(), cells, distinct.size());

        helper.assertTrue(!LargeMachineLayouts.machines().isEmpty(), "the layouts are registered");
        helper.assertTrue(broken.isEmpty(), "layout part ids that do not resolve: " + broken);
        // Measured 2026-09-18: 13 layouts, 708 cells, 19 distinct part ids. The floor sits below that
        // so a layout table emptied by accident fails here instead of passing vacuously.
        helper.assertTrue(distinct.size() >= 15,
                "the layouts use a real spread of parts, got " + distinct.size());
        helper.succeed();
    }
}
