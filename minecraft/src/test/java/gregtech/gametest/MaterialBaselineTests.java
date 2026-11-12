package gregtech.gametest;

import gregtech.api.addon.material.MaterialInfoTool;
import gregtech.api.addon.material.GregTechMaterials;
import gregtech.api.addon.material.MaterialFormKind;
import gregtech.api.addon.material.MaterialKey;
import gregtech.api.addon.material.MaterialProcessKeys;
import gregtech.api.material.GTValues;
import gregtech.api.material.PulverizationMaterialHelper;
import gregtech.api.material.SmeltingMaterialHelper;
import gregtech.content.block.machine.MoldBlock;
import gregtech.content.block.machine.MoldItemData;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.GTToolItem;
import gregtech.content.material.Materials;
import gregtech.content.recipe.CraftingMaterialForms;
import gregtech.content.recipe.MachineRecipeOverlay;
import gregtech.content.recipe.MaterialRecoveryRules;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
import gregtech.engine.fluid.PortableFluidContainerSpec;
import gregtech.engine.machine.crucible.CrucibleItemInput;
import gregtech.engine.material.ItemComposition;
import gregtech.engine.material.ItemMaterialRegistry;
import gregtech.engine.material.ItemCompositionSource;
import gregtech.engine.material.MaterialEquivalence;
import gregtech.engine.recipe.Recipe;
import gregtech.engine.recipe.RecipeMap;
import gregtech.engine.tool.GTToolType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Observations and correctness checks are separate; known differences are named in each assertion. */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class MaterialBaselineTests {
    private static final String BATCH = "material_baseline";
    private static final AtomicBoolean RELOAD_COMPLETE = new AtomicBoolean();
    private static final AtomicReference<Throwable> RELOAD_FAILURE = new AtomicReference<>();

    private MaterialBaselineTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void materialSpiInstallsLinkedCompositionAndGeneratedForm(GameTestHelper helper) {
        var material = gregtech.engine.material.GTMaterialRegistry.get("BaselineRuntimeCompound");
        helper.assertTrue(material.isValid(), "SPI material installed");
        helper.assertTrue("material_baseline_test".equals(gregtech.engine.addon.AddonMaterials.owner(material)), "SPI ownership");
        helper.assertTrue(!gregtech.engine.material.GTMaterialRegistry.isCoreMaterial(material), "addon identity is separate from core");
        helper.assertTrue(material.getCompositionComponents().size() == 2 && material.getCompositionDivider() == 3, "SPI composition linked");
        var indexed = MaterialInfoTool.composition(new MaterialKey(material.getName())).orElseThrow();
        helper.assertTrue(indexed.divider() == 3 && indexed.parts().size() == 2 && !indexed.crucible(), "legacy SPI composition reaches the frozen index");
        helper.assertTrue(indexed.parts().get(0).material().equals("Iron") && indexed.parts().get(0).amount() == GTValues.U * 2
                && indexed.parts().get(1).material().equals("Copper") && indexed.parts().get(1).amount() == GTValues.U, "indexed composition retains order and amounts");
        var iron = new MaterialKey("Iron");
        helper.assertTrue(SmeltingMaterialHelper.target(iron).equals(SmeltingMaterialHelper.target(Materials.Iron))
                && SmeltingMaterialHelper.target(iron).isPresent(), "default smelting agrees across query entries");
        helper.assertTrue(PulverizationMaterialHelper.target(iron).equals(PulverizationMaterialHelper.target(Materials.Iron))
                && PulverizationMaterialHelper.target(iron).isPresent(), "default pulverization agrees across query entries");
        var dust = GTItems.getStack(MaterialPrefix.dust, material, 3);
        helper.assertFalse(dust.isEmpty(), "host generated the requested dust form");
        helper.assertTrue(find(MachineRecipeMaps.Centrifuge, List.of(dust), List.of()) != null, "SPI decomposition is queryable");
        observe(helper, "spi_dust", dust);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void addonDecompositionExecutesThroughThePublishedCentrifugeRow(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        var block = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "centrifuge_bronze"));
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var machine = (BasicMachineBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(machine != null, "centrifuge fixture is registered");
        machine.setSpec(((BasicMachineBlock) block).basicSpec());
        var material = gregtech.engine.material.GTMaterialRegistry.get("BaselineRuntimeCompound");
        var dust = GTItems.getStack(MaterialPrefix.dust, material, 3);
        machine.inventory().setStackInSlot(0, dust);
        machine.inventoryChanged();
        var config = new gregtech.engine.machine.RecipeProcessor.Config() {
            public RecipeMap recipeMap() {
                return machine.recipeMap();
            }

            public boolean searchOutputTanks() {
                return false;
            }

            public boolean exclusiveOutputs() {
                return false;
            }

            public int parallelLimit() {
                return 1;
            }

            public boolean parallelScalesDuration() {
                return true;
            }

            public boolean cheapOverclocking() {
                return false;
            }

            public boolean usesTimeEnergy() {
                return false;
            }

            public int efficiency() {
                return 10_000;
            }

            public long inputMinimum() {
                return 16;
            }

            public long inputMaximum() {
                return 64;
            }

            public long nominalEnergyInput() {
                return 32;
            }

            public long startupEnergyFor(Recipe recipe) {
                return 0;
            }
        };
        var io = new gregtech.engine.machine.RecipeProcessor.IO() {
            public gregtech.engine.machine.MachineInventory inventory() {
                return machine.autoIOInventory();
            }

            public net.minecraft.world.level.Level level() {
                return level;
            }

            public ItemStack autocraftingProgram() {
                return ItemStack.EMPTY;
            }

            public void setStartupEnergy(long value) {}

            public void clearStartupEnergy() {}

            public void markJobStarted() {}

            public net.minecraft.world.level.block.entity.BlockEntity owner() {
                return machine;
            }
        };
        var gate = new gregtech.engine.machine.RecipeProcessor.Gate() {
            public boolean structureComplete() {
                return true;
            }

            public boolean switchesAllowRunning() {
                return true;
            }

            public boolean recipeStartAllowed() {
                return true;
            }

            public boolean lightAllowsStart() {
                return true;
            }
        };
        var processor = new gregtech.engine.machine.RecipeProcessor(config, io, gate);
        helper.assertTrue(
                processor.startJob() == gregtech.engine.machine.RecipeProcessor.StartResult.STARTED,
                "published decomposition row did not start through the shared recipe processor");
        helper.assertTrue(
                machine.inventory().getStackInSlot(0).isEmpty(),
                "centrifuge consumed its decomposition input");
        helper.assertTrue(
                processor.recipeDetails() != null
                        && processor.recipeDetails().duration() == 876
                        && processor.recipeDetails().power() == 42,
                "decomposition plan supplied duration and EU/t to the machine: " + processor.recipeDetails());
        helper.assertTrue(
                processor.advance(processor.totalWork()),
                "centrifuge did not complete its planned work; work=" + processor.totalWork()
                        + ", progress=" + processor.progress());
        helper.assertTrue(
                processor.flushOutputs(),
                "centrifuge did not flush its planned outputs; pending=" + processor.pendingOutputs());
        var iron = machine.inventory().getStackInSlot(machine.inputSlots());
        var copper = machine.inventory().getStackInSlot(machine.inputSlots() + 1);
        helper.assertTrue(
                iron.is(GTItems.getStack(MaterialPrefix.dust, Materials.Iron, 1).getItem())
                        && iron.getCount() == 2,
                "centrifuge emitted two Iron dust");
        helper.assertTrue(
                copper.is(GTItems.getStack(MaterialPrefix.dust, Materials.Copper, 1).getItem())
                        && copper.getCount() == 1,
                "centrifuge emitted one Copper dust");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void registeredFormsPublishItemBlockAndFluidBindings(GameTestHelper helper) {
        var iron = new MaterialKey("Iron");
        var ironBindings = GregTechMaterials.formBindings().find(iron);
        helper.assertTrue(
                ironBindings.stream().anyMatch(binding -> binding.kind() == MaterialFormKind.ITEM),
                "iron has a registered item binding");
        helper.assertTrue(
                ironBindings.stream()
                        .anyMatch(binding -> binding.id().equals("minecraft:iron_ingot")),
                "explicit vanilla iron ingot is part of the same form inventory");
        helper.assertTrue(
                ironBindings.stream().anyMatch(binding -> binding.kind() == MaterialFormKind.BLOCK),
                "iron has a registered block binding");
        var hydrogenBindings = GregTechMaterials.formBindings().find(new MaterialKey("Hydrogen"));
        helper.assertTrue(
                hydrogenBindings.stream().anyMatch(binding -> binding.kind() == MaterialFormKind.FLUID),
                "hydrogen has a registered fluid binding");
        helper.assertTrue(
                GregTechMaterials.forms().rules(iron).stream()
                        .anyMatch(rule -> rule.kind() == MaterialFormKind.ITEM
                                && GregTechMaterials.formBindings()
                                        .present(iron, rule.form())),
                "allowed item rules intersect native bindings");
        helper.assertTrue(
                "ores".equals(MaterialEquivalence.commonGroup("ore"))
                        && "raw_ore_chunks".equals(MaterialEquivalence.commonGroup("rawOreChunk")),
                "external ore tag names remain mapped");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void storageBlocksExposeCompositionAndBothAdmissions(GameTestHelper helper) {
        var iron = ItemMaterialRegistry.get(new ItemStack(Items.IRON_BLOCK)).orElseThrow();
        helper.assertTrue(iron.components().size() == 1 && iron.material().resolve() == Materials.Iron.resolve()
                && iron.amount() == 9 * GTValues.U, "iron block contains exactly 9U Iron");
        helper.assertTrue(CrucibleItemInput.isValid(new ItemStack(Items.IRON_BLOCK)), "crucible admits iron block");
        for (Item item : List.of(Items.IRON_BLOCK, Items.COPPER_BLOCK)) {
            var stack = new ItemStack(item);
            helper.assertTrue(ItemMaterialRegistry.get(stack).isPresent(), "missing composition: " + id(stack));
            helper.assertTrue(ItemMaterialRegistry.canRecover(stack), "storage block is recoverable: " + id(stack));
            observe(helper, id(stack), stack);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void traversalAndSingleQueriesUseTheSameEffectiveSnapshot(GameTestHelper helper) {
        long tagged = 0;
        for (var entry : ItemMaterialRegistry.entries().entrySet()) {
            helper.assertTrue(
                    ItemMaterialRegistry.base(entry.getKey()).orElseThrow().equals(entry.getValue()),
                    "traversal and lookup disagree for " + id(new ItemStack(entry.getKey())));
            helper.assertTrue(
                    ItemMaterialRegistry.get(new ItemStack(entry.getKey())).orElseThrow().equals(entry.getValue()),
                    "stack lookup differs from base for " + id(new ItemStack(entry.getKey())));
            helper.assertFalse(entry.getValue().sources().isEmpty(), "composition has no provenance");
            if (entry.getValue().sources().stream()
                    .anyMatch(source -> source.kind() == ItemCompositionSource.Kind.FORM_TAG)) tagged++;
        }
        helper.assertTrue(ItemMaterialRegistry.tagInputReady(), "tag input generation is ready at runtime");
        helper.assertTrue(
                ItemMaterialRegistry.compositionStatus(Items.DRAGON_EGG)
                        != ItemMaterialRegistry.CompositionLookupState.TAG_INPUT_NOT_READY,
                "completed tag input distinguishes absence from an unpublished generation");
        helper.assertTrue(
                gregtech.content.recipe.ExternalOreProcessing.taggedCompositions() == 0 || tagged > 0,
                "loaded common-form tags contribute effective compositions");
        helper.assertTrue(ItemMaterialRegistry.explicit(Items.IRON_BLOCK).isPresent(), "explicit diagnostics remain");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void seventyTwoUnitsExposePlanAndQueryableOutput(GameTestHelper helper) {
        var stack = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
        var data = ItemMaterialRegistry.get(stack).orElseThrow();
        helper.assertTrue(data.components().size() == 1 && data.material().resolve() == Materials.Gold.resolve()
                && data.amount() == 72 * GTValues.U, "enchanted golden apple carries 72U Gold");
        var plan = MaterialRecoveryRules.dustPlan(data.amount());
        helper.assertTrue(plan.complete() && plan.representedUnits() == 72 * GTValues.U,
                "72U dust planner preserves every unit");
        helper.assertTrue(
                plan.stackCount() == 2
                        && plan.piles().get(0).equals(new MaterialRecoveryRules.DustPile(MaterialPrefix.dust, 64))
                        && plan.piles().get(1).equals(new MaterialRecoveryRules.DustPile(MaterialPrefix.dust, 8)),
                "72U uses legal 64 dust plus 8 dust stacks");
        var oneSlot = gregtech.util.OM.dust(Materials.Gold, 72 * GTValues.U);
        var allSlots = gregtech.util.OM.dustAll(Materials.Gold, 72 * GTValues.U);
        helper.assertTrue(
                oneSlot.getCount() == 64
                        && allSlots.size() == 2
                        && allSlots.get(0).getCount() == 64
                        && allSlots.get(1).getCount() == 8,
                "single-slot and complete dust adapters project the same plan");
        var recipe = find(MachineRecipeMaps.Shredder, List.of(stack), List.of());
        helper.assertTrue(recipe != null, "72U item has a queryable recycling recipe");
        helper.assertTrue(
                recipe.mOutputs.length == 2
                        && recipe.mOutputs[0].is(GTItems.getStack(MaterialPrefix.dust, Materials.Gold, 1)
                                .getItem())
                        && recipe.mOutputs[0].getCount() == 64
                        && recipe.mOutputs[1].is(GTItems.getStack(MaterialPrefix.dust, Materials.Gold, 1)
                                .getItem())
                        && recipe.mOutputs[1].getCount() == 8,
                "final shredder row preserves 72U Gold");
        write(helper, "72u.tsv", List.of("input\t" + describe(stack), "plan\t" + plan.piles(),
                "lookup\t" + (recipe == null ? "absent" : signature(MachineRecipeMaps.Shredder, recipe))));
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void capsuleRecyclingUsesTheSharedStaticShredderPlan(GameTestHelper helper) {
        var spec = Arrays.stream(PortableFluidContainerSpec.values())
                .filter(candidate -> candidate.id().equals("cell_tin"))
                .findFirst()
                .orElseThrow();
        var input = new ItemStack(gregtech.platform.Mc.item(
                ResourceLocation.fromNamespaceAndPath("gt6ce", "fluid_" + spec.id())));
        var composition = ItemMaterialRegistry.get(input).orElseThrow();
        var expected = MaterialRecoveryRules.planShredderStatic(
                composition, MachineRecipeMaps.Shredder.mOutputItemsCount);
        var recipe = find(MachineRecipeMaps.Shredder, List.of(input), List.of());
        helper.assertTrue(expected.accepted() && recipe != null, "capsule Shredder recipe exists");
        helper.assertTrue(recipe.mDuration == expected.duration(), "capsule duration comes from the shared plan");
        helper.assertTrue(
                recipe.mRecoveryOwnership != null
                        && recipe.mRecoveryOwnership.scope()
                                == Recipe.RecoveryOwnership.Scope.EXPLICIT
                        && recipe.mRecoveryOwnership.family().equals("capsule-cell"),
                "capsule row carries typed explicit ownership");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void compositeHeadKeepsEveryComponentAcrossQueries(GameTestHelper helper) {
        var stack = GTItems.getStack(MaterialPrefix.toolHeadDrill, Materials.Titanium, 1);
        helper.assertFalse(stack.isEmpty(), "composite drill head exists");
        var explicit = ItemMaterialRegistry.explicit(stack.getItem()).orElseThrow();
        var actual = ItemMaterialRegistry.get(stack).orElseThrow();
        helper.assertTrue(explicit.equals(actual), "registry and pristine stack agree");
        helper.assertTrue(actual.components().size() == 2, "drill head has two components");
        helper.assertTrue(actual.components().get(0).material().resolve() == Materials.Titanium.resolve()
                && actual.components().get(0).amount() == 4 * GTValues.U, "primary: 4U Titanium");
        helper.assertTrue(actual.components().get(1).material().resolve() == Materials.Steel.resolve()
                && actual.components().get(1).amount() == 4 * GTValues.U, "secondary: 4U Steel");
        var form = MaterialEquivalence.form(stack);
        helper.assertTrue(form == null, "a composite tool head is not a pure material form");
        helper.assertTrue(explicit.components().stream()
                        .anyMatch(component -> component.material().resolve() == Materials.Steel.resolve()),
                "equivalence rejection does not discard the secondary Steel");
        var plan = MaterialRecoveryRules.planShredder(actual, MachineRecipeMaps.Shredder.mOutputItemsCount);
        helper.assertTrue(plan.accepted(), "complete composite composition has a shredder plan");
        helper.assertTrue(
                plan.outputs().stream()
                        .map(output -> output.material().resolve())
                        .toList()
                        .containsAll(List.of(
                                Materials.Titanium.getTargetPulverMaterial().resolve(),
                                Materials.Steel.getTargetPulverMaterial().resolve())),
                "composite shredder plan converts every component through its own pulverization target");
        observe(helper, "composite_head", stack);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void damagedToolsAndContainersExposeStateAdmission(GameTestHelper helper) {
        var tool = new ItemStack(Items.IRON_PICKAXE);
        helper.assertTrue(ItemMaterialRegistry.get(tool).isPresent(), "iron pickaxe composition exists");
        var full = ItemMaterialRegistry.get(tool).orElseThrow();
        observe(helper, "tool_full", tool);
        tool.setDamageValue(tool.getMaxDamage() / 2);
        var half = ItemMaterialRegistry.get(tool).orElseThrow();
        for (int i = 0; i < full.components().size(); i++)
            helper.assertTrue(half.components().get(i).amount() == full.components().get(i).amount()
                    * (tool.getMaxDamage() - tool.getDamageValue()) / tool.getMaxDamage(), "damage scales component " + i);
        observe(helper, "tool_partial", tool);
        helper.assertTrue(
                ItemMaterialRegistry.recoveryRejection(tool)
                        == ItemMaterialRegistry.RecoveryRejection.ELIGIBLE,
                "partial tools retain an eligible scaled composition");
        var expectedHalf = MaterialRecoveryRules.planShredder(half, MachineRecipeMaps.Shredder.mOutputItemsCount);
        var halfRecipe = find(MachineRecipeMaps.Shredder, List.of(tool), List.of());
        helper.assertTrue(
                expectedHalf.accepted() && halfRecipe != null,
                "partial tools query the Shredder from their remaining composition");
        helper.assertTrue(
                halfRecipe.mOutputs.length == expectedHalf.stackCount(),
                "partial tools recover exactly the scaled Shredder output");
        tool.setDamageValue(tool.getMaxDamage());
        helper.assertTrue(ItemMaterialRegistry.get(tool).orElseThrow().components().isEmpty(), "exhausted tool has no positive material");
        helper.assertFalse(ItemMaterialRegistry.canRecover(tool), "no recovery from exhausted tool");
        helper.assertTrue(
                ItemMaterialRegistry.recoveryRejection(tool)
                        == ItemMaterialRegistry.RecoveryRejection.NO_COMPONENTS,
                "exhausted recovery rejection names the empty composition");
        observe(helper, "tool_exhausted", tool);
        var nativeTool = GTToolItem.create(GTToolType.PICKAXE, Materials.Iron, gregtech.engine.material.GTMaterialRegistry.get("Wood"));
        helper.assertFalse(nativeTool.isEmpty(), "native material tool exists");
        helper.assertTrue(ItemMaterialRegistry.get(nativeTool).isEmpty(), "observed difference: native tool instance has no registry composition");
        observe(helper, "native_tool", nativeTool);
        var empty = new ItemStack(Items.BUCKET);
        var filled = new ItemStack(Items.WATER_BUCKET);
        helper.assertTrue(CrucibleItemInput.isValid(empty), "empty bucket is meltable");
        helper.assertFalse(CrucibleItemInput.isValid(filled), "filled bucket is protected");
        observe(helper, "bucket_empty", empty);
        observe(helper, "bucket_filled", filled);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void shapedMoldsExposeShapeCompositionAndRejection(GameTestHelper helper) {
        var block = BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce", "mold_stone"));
        helper.assertTrue(block instanceof MoldBlock, "stone casting mold is registered on both nodes");
        var empty = new ItemStack(block);
        int mask = gregtech.content.blockentity.machine.MoldBlockEntity.randomDungeonShape(net.minecraft.util.RandomSource.create(0));
        var shaped = MoldItemData.withShape(empty.copy(), mask);
        var composition = ItemMaterialRegistry.get(empty).orElseThrow();
        helper.assertTrue(composition.components().size() == 1 && composition.amount() == 45 * GTValues.U,
                "stone mold carries its 45U construction composition");
        helper.assertTrue(ItemMaterialRegistry.canRecover(empty), "plain stone mold is admitted for recovery");
        helper.assertTrue(MoldItemData.shape(shaped) == mask, "casting cavity survives on the item");
        helper.assertTrue(ItemMaterialRegistry.get(empty).equals(ItemMaterialRegistry.get(shaped)), "shape keeps composition");
        helper.assertFalse(ItemMaterialRegistry.hasStoredContents(shaped), "a casting cavity is shape state, not inventory");
        helper.assertTrue(CrucibleItemInput.isValid(shaped), "a shaped casting mold remains meltable");
        helper.assertTrue(ItemMaterialRegistry.canRecover(shaped), "a shaped casting mold keeps bulk recovery");
        observe(helper, "casting_mold_empty", empty);
        observe(helper, "casting_mold_shaped", shaped);
        var press = GTTechnological.get("press_bullet_casing_shape_small");
        helper.assertTrue(press != null, "press mold is registered");
        helper.assertTrue(ItemMaterialRegistry.get(new ItemStack(press)).isEmpty(),
                "observed difference: registered press mold has no composition (#350)");
        observe(helper, "press_mold", new ItemStack(press));
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void solidAndFluidDecompositionAreQueryable(GameTestHelper helper) {
        var salt = GTItems.getStack(MaterialPrefix.dust, gregtech.engine.material.GTMaterialRegistry.get("Powellite"), 6);
        var solid = find(MachineRecipeMaps.Electrolyzer,
                List.of(new ItemStack(GTTechnological.selectorTag(0)), salt), List.of());
        helper.assertTrue(solid != null, "Powellite decomposition is queryable with mode 0 (accepted #318 rule)");
        helper.assertTrue(
                solid.isCatalystInput(0) && Arrays.equals(solid.explicitCatalystInputs(), new int[] {0}),
                "the electrolyser selector is an explicit presence-only catalyst");
        helper.assertTrue(
                solid.mDuration == 1752 && solid.mEUt == 84,
                "Powellite decomposition retains its planned duration and EU/t");
        helper.assertTrue(
                solid.mOutputs.length == 2
                        && solid.mOutputs[0].is(GTItems.getStack(MaterialPrefix.dust, Materials.Calcium, 1)
                                .getItem())
                        && solid.mOutputs[0].getCount() == 1
                        && solid.mOutputs[1].is(GTItems.getStack(MaterialPrefix.dust, Materials.Molybdenum, 1)
                                .getItem())
                        && solid.mOutputs[1].getCount() == 1,
                "Powellite solid outputs retain declaration order and quantities");
        helper.assertTrue(
                Arrays.stream(solid.mFluidOutputs)
                        .anyMatch(output -> output != null
                                && output.getAmount() == 4000
                                && "oxygen".equals(BuiltInRegistries.FLUID
                                        .getKey(output.getFluid())
                                        .getPath())),
                "Powellite oxygen output uses the declared gas conversion");
        var water = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 3000);
        var liquid = find(MachineRecipeMaps.Electrolyzer,
                List.of(new ItemStack(GTTechnological.selectorTag(0))), List.of(water));
        helper.assertTrue(liquid != null, "water decomposition is queryable with fluid input");
        var alloy = gregtech.engine.material.GTMaterialRegistry.get("AluminiumAlloy");
        var alloyDust = GTItems.getStack(MaterialPrefix.dust, alloy, 45);
        var alloyBlock = gregtech.content.registry.GTBlocks.getStack(
                gregtech.engine.prefix.BlockMaterialPrefix.blockDust, alloy);
        alloyBlock.setCount(5);
        helper.assertTrue(
                find(MachineRecipeMaps.Centrifuge, List.of(alloyDust), List.of()) != null
                        && find(MachineRecipeMaps.Centrifuge, List.of(alloyBlock), List.of()) != null,
                "AluminiumAlloy without a fluid binding retains both solid decomposition inputs");
        helper.assertTrue(Arrays.stream(solid.mOutputs).anyMatch(s -> s != null && !s.isEmpty())
                || Arrays.stream(solid.mFluidOutputs).anyMatch(s -> s != null && !s.isEmpty()), "solid decomposition has outputs");
        helper.assertTrue(Arrays.stream(liquid.mFluidOutputs).anyMatch(s -> s != null && !s.isEmpty()), "water decomposition has fluid outputs");
        write(helper, "decomposition.tsv", List.of(signature(MachineRecipeMaps.Electrolyzer, solid), signature(MachineRecipeMaps.Electrolyzer, liquid)));
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void decompositionRespectsFormsAndExactFluidIdentities(GameTestHelper helper) {
        var owned = gregtech.content.recipe.DecompositionRecipeLoader.owned();
        for (var test : MaterialBaselineAddon.DECOMPOSITIONS) {
            var material = gregtech.engine.material.GTMaterialRegistry.get(test.material());
            var key = new MaterialKey(material.getName());
            helper.assertTrue(MaterialInfoTool.currentProcess(key, MaterialProcessKeys.DECOMPOSITION)
                            .equals(GregTechMaterials.processes().findSingle(key, MaterialProcessKeys.DECOMPOSITION)),
                    test.material() + " process queries agree");
            var dust = GTItems.getStack(MaterialPrefix.dust, material, 9);
            var block = gregtech.content.registry.GTBlocks.getStack(
                    gregtech.engine.prefix.BlockMaterialPrefix.blockDust, material);
            helper.assertFalse(dust.isEmpty() || block.isEmpty(), test.material() + " solid fixtures registered");
            boolean dustRow = owned.stream().anyMatch(row -> Arrays.stream(row.mInputs)
                    .anyMatch(input -> input != null && input.is(dust.getItem())));
            boolean blockRow = owned.stream().anyMatch(row -> Arrays.stream(row.mInputs)
                    .anyMatch(input -> input != null && input.is(block.getItem())));
            helper.assertTrue(dustRow == test.form().equals("dust"), test.material() + " dust permission");
            helper.assertTrue(blockRow == test.form().equals("blockDust"), test.material() + " block permission");
            String fluidId = test.fluid() == null || !test.fluid().contains(":")
                    ? "material_baseline_test:" + test.material().toLowerCase(java.util.Locale.ROOT)
                    : test.fluid();
            var id = ResourceLocation.tryParse(fluidId);
            boolean registered = id != null && BuiltInRegistries.FLUID.containsKey(id);
            boolean allowsFluid = test.form().equals("liquid") || test.form().equals("gas");
            Recipe row = null;
            if (registered) {
                var fluid = BuiltInRegistries.FLUID.get(id);
                row = owned.stream().filter(candidate -> Arrays.stream(candidate.mFluidInputs)
                        .anyMatch(input -> input != null && input.getFluid() == fluid)).findFirst().orElse(null);
            }
            helper.assertTrue((row != null) == (registered && allowsFluid), test.material() + " fluid permission and identity");
            if (row != null) {
                helper.assertTrue(row.mFluidInputs.length == 1 && row.mFluidInputs[0].getAmount() == 1000,
                        test.material() + " exact fluid conversion after recipe reduction");
                helper.assertTrue(row.mOutputs.length == 1 && row.mOutputs[0].getCount() == 1
                                && row.mOutputs[0].is(GTItems.getStack(MaterialPrefix.dust, Materials.Iron, 1).getItem()),
                        test.material() + " complete solid output");
                helper.assertTrue(row.mDuration == 292 && row.mEUt == 126,
                        test.material() + " reduced duration and unchanged EU/t");
            }
        }
        var outputMaterial = gregtech.engine.material.GTMaterialRegistry.get("BaselineFluidOutput");
        var outputRow = find(MachineRecipeMaps.Centrifuge,
                List.of(GTItems.getStack(MaterialPrefix.dust, outputMaterial, 2)), List.of());
        helper.assertTrue(outputRow != null && outputRow.mFluidOutputs.length == 1,
                "namespaced fluid output does not discard the entire decomposition plan");
        helper.assertTrue(outputRow.mFluidOutputs[0].getAmount() == 1000
                        && BuiltInRegistries.FLUID.getKey(outputRow.mFluidOutputs[0].getFluid()).toString()
                                .equals("material_baseline_test:baselineliquidonly"),
                "namespaced addon fluid output retains identity and quantity");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void compositionAdmissionAndCookingPolicyStayIndependent(GameTestHelper helper) {
        var descriptive = ItemMaterialRegistry.entries().entrySet().stream()
                .filter(entry -> entry.getValue().sources().stream()
                        .anyMatch(source -> source.kind() == ItemCompositionSource.Kind.VANILLA_JSON))
                .findFirst()
                .orElseThrow();
        var stack = new ItemStack(descriptive.getKey());
        helper.assertTrue(ItemMaterialRegistry.get(stack).isPresent(), "descriptive composition is queryable");
        helper.assertTrue(ItemMaterialRegistry.canMelt(stack), "composition existence remains independently meltable");
        helper.assertTrue(
                ItemMaterialRegistry.canRecover(stack),
                "GT6 has no separate item-level bulk recovery authorization");
        helper.assertTrue(
                ItemMaterialRegistry.recoveryRejection(stack)
                        == ItemMaterialRegistry.RecoveryRejection.ELIGIBLE,
                "composition admission remains independent of the selected process");
        helper.assertFalse(
                ItemMaterialRegistry.hasMaterialProcessingRule(stack),
                "descriptive vanilla data does not delete material cooking rules");
        var materialItem = GTItems.getStack(MaterialPrefix.ingot, Materials.Iron, 1);
        helper.assertTrue(
                ItemMaterialRegistry.hasMaterialProcessingRule(materialItem),
                "native material forms retain material cooking policy");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void addonThermalRecoveryUsesDeclaredTargetAndPhase(GameTestHelper helper) {
        var material = gregtech.engine.material.GTMaterialRegistry.get("BaselineThermalAddon");
        var dust = GTItems.getStack(MaterialPrefix.dust, material, 1);
        helper.assertFalse(dust.isEmpty(), "addon dust form is registered");
        var smelting = GregTechMaterials.processes()
                .findSingle(new MaterialKey(material.getName()), MaterialProcessKeys.SMELTING)
                .filter(rule -> rule.value().amount() > 0)
                .orElseThrow();
        helper.assertTrue(
                material.getName().equals(smelting.value().material().name()),
                "addon smelting target is its declared material");
        helper.assertTrue(
                gregtech.engine.recipe.chem.MaterialFluidBinding.findAll(material.getName()).size() == 1,
                "addon has one declared thermal phase");
        var composition = ItemMaterialRegistry.base(dust.getItem()).orElseThrow();
        helper.assertTrue(
                composition.prefix() == MaterialPrefix.dust
                        && composition.material().resolve() == material
                        && composition.components().size() == 1,
                "addon dust uses its authoritative composition");
        helper.assertTrue(ItemMaterialRegistry.canRecover(dust), "addon dust is recoverable");
        var recipe = find(MachineRecipeMaps.Smelter, List.of(dust), List.of());
        var matchingRows = MachineRecipeMaps.Smelter.mRecipeList.stream()
                .filter(row -> row.mInputs.length == 1
                        && !row.mInputs[0].isEmpty()
                        && row.mInputs[0].getItem() == dust.getItem())
                .toList();
        helper.assertTrue(
                recipe != null,
                "addon material participates in thermal recovery; matching-row-count="
                        + matchingRows.size()
                        + "; input-matches="
                        + matchingRows.stream().anyMatch(row -> row.matchesItemInput(0, dust)));
        helper.assertTrue(
                Arrays.stream(recipe.mFluidOutputs)
                        .anyMatch(output -> output != null && !output.isEmpty()),
                "addon thermal row uses its declared native phase");
        helper.assertTrue(
                Arrays.stream(recipe.mFluidOutputs).anyMatch(output -> output.getAmount() == 1000),
                "one addon material unit uses its declared 1000 mB phase scale");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void tagCandidateReplacementIsAtomicAndRemovalIsObservable(GameTestHelper helper) {
        var item = Items.DRAGON_EGG;
        helper.assertTrue(ItemMaterialRegistry.explicit(item).isEmpty(), "tag probe item has no explicit composition");
        var first = taggedComposition(Materials.Copper, 4);
        try {
            ItemMaterialRegistry.publishTagCandidates(Map.of(item, List.of(first)));
            var effective = ItemMaterialRegistry.base(item).orElseThrow();
            var diagnosis = ItemMaterialRegistry.compositionDiagnosis(item).orElseThrow();
            helper.assertTrue(
                    effective.equals(diagnosis.selected()),
                    "composition diagnosis exposes the effective selection");
            helper.assertTrue(
                    diagnosis.decisions().stream()
                            .anyMatch(decision -> decision.status()
                                    == gregtech.engine.material.ItemCompositionDecision.Status.SELECTED),
                    "composition diagnosis records the selected source");
            long revision = ItemMaterialRegistry.compositionRevision();
            var generation = ItemMaterialRegistry.compositionState();

            ItemMaterialRegistry.beginTagGeneration();
            var pending = ItemMaterialRegistry.compositionState();
            helper.assertFalse(pending.tagInputReady(), "a new tag generation starts before its input is ready");
            helper.assertTrue(
                    pending.tagGeneration() == generation.tagGeneration() + 1,
                    "starting a session advances the tag generation exactly once");
            helper.assertTrue(
                    ItemMaterialRegistry.base(item).isEmpty(),
                    "a new generation does not expose the previous server's tag candidates");
            helper.assertTrue(pending.explicitClosed(), "explicit definitions remain process-static");
            ItemMaterialRegistry.publishTagCandidates(Map.of(item, List.of(first)));
            revision = ItemMaterialRegistry.compositionRevision();

            IllegalStateException conflict = null;
            try {
                ItemMaterialRegistry.publishTagCandidates(Map.of(
                        item,
                        List.of(taggedComposition(Materials.Iron, 1), taggedComposition(Materials.Gold, 2))));
            } catch (IllegalStateException failure) {
                conflict = failure;
            }
            helper.assertTrue(conflict != null, "conflicting tag candidates must fail");
            helper.assertTrue(
                    effective.equals(ItemMaterialRegistry.base(item).orElseThrow()),
                    "failed tag publication preserves the previous effective composition");
            helper.assertTrue(
                    ItemMaterialRegistry.compositionRevision() == revision,
                    "failed tag publication does not advance the composition revision");

            ItemMaterialRegistry.publishTagCandidates(Map.of(item, List.of()));
            helper.assertTrue(
                    ItemMaterialRegistry.base(item).isEmpty(),
                    "removed tag candidates drop the effective composition");
            helper.assertTrue(
                    ItemMaterialRegistry.compositionDiagnosis(item).isEmpty(),
                    "removed tag candidates also withdraw their diagnosis");

            var descriptive = ItemMaterialRegistry.entries().entrySet().stream()
                    .filter(entry -> entry.getValue().sources().stream()
                            .anyMatch(source -> source.kind() == ItemCompositionSource.Kind.VANILLA_JSON))
                    .findFirst()
                    .orElseThrow();
            var declaredItem = descriptive.getKey();
            ItemMaterialRegistry.publishTagCandidates(
                    Map.of(declaredItem, List.of(taggedComposition(MaterialPrefix.ingot, Materials.Iron, GTValues.U))));
            helper.assertTrue(
                    ItemMaterialRegistry.base(declaredItem).orElseThrow().sources().stream()
                            .allMatch(source -> source.kind() == ItemCompositionSource.Kind.VANILLA_JSON),
                    "explicit JSON remains the selected composition over a tag fallback");
            helper.assertFalse(
                    ItemMaterialRegistry.hasMaterialProcessingRule(new ItemStack(declaredItem)),
                    "a discarded lower-priority tag does not authorize cooking rules");

            boolean closed = false;
            try {
                ItemMaterialRegistry.register(
                        Items.DRAGON_EGG,
                        new ItemComposition(null, Materials.Copper, 4),
                        ItemCompositionSource.Kind.VANILLA_JSON,
                        "gt6ce:test/closed-explicit",
                        "GameTest closed explicit registration");
            } catch (IllegalStateException expected) {
                closed = expected.getMessage().contains("closed");
            }
            helper.assertTrue(closed, "explicit item composition registration closes after final validation");
        } finally {
            ItemMaterialRegistry.publishTagCandidates(Map.of(item, List.of()));
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 200)
    public static void tagDrivenShredderRowsFollowTheEffectiveSnapshot(GameTestHelper helper) {
        var item = Items.CARROT_ON_A_STICK;
        var stack = new ItemStack(item);
        helper.assertTrue(ItemMaterialRegistry.explicit(item).isEmpty(), "probe item starts without explicit material");
        var cachedSnapshot = ItemMaterialRegistry.entries();
        helper.assertTrue(
                cachedSnapshot == ItemMaterialRegistry.entries(),
                "unchanged composition revision reuses the traversal snapshot");
        try {
            ItemMaterialRegistry.publishTagCandidates(
                    Map.of(item, List.of(taggedComposition(MaterialPrefix.ingot, Materials.Copper, GTValues.U))));
            helper.assertFalse(
                    cachedSnapshot == ItemMaterialRegistry.entries(),
                    "published tag changes invalidate the traversal snapshot");
            gregtech.content.recipe.ShredderRecyclingRecipes.rebuildTagRows();
            var recipe = find(MachineRecipeMaps.Shredder, List.of(stack), List.of());
            helper.assertTrue(recipe != null, "tag-derived composition creates a Shredder row");
            helper.assertTrue(recipe.mMaterialRecovery, "tag-derived Shredder row uses recovery admission");
            helper.assertTrue(
                    Arrays.stream(recipe.mOutputs)
                            .anyMatch(output -> output.is(GTItems.getStack(MaterialPrefix.dust, Materials.Copper, 1)
                                    .getItem())),
                    "tag-derived Shredder row uses the resolved composition");
            var damaged = stack.copy();
            damaged.setDamageValue(1);
            helper.assertTrue(
                    find(MachineRecipeMaps.Shredder, List.of(damaged), List.of()) != null,
                    "tag-derived Shredder row rebuilds output from damaged input");
            CraftingMaterialForms.rebuild(true);
            var resolvedForm = CraftingMaterialForms.form(stack);
            helper.assertTrue(
                    resolvedForm != null
                            && resolvedForm.prefix().equals("ingot")
                            && resolvedForm.material().equals("Copper"),
                    "form consumers read the resolved tag composition");
            gregtech.engine.material.MaterialFormContent.publish();
            helper.assertTrue(
                    GregTechMaterials.formBindings().find(new MaterialKey("Copper")).stream()
                            .anyMatch(binding -> binding.id().equals("minecraft:carrot_on_a_stick")),
                    "published form bindings include the tag-derived item");

            ItemMaterialRegistry.publishTagCandidates(Map.of());
            gregtech.content.recipe.ShredderRecyclingRecipes.rebuildTagRows();
            helper.assertTrue(
                    ItemMaterialRegistry.compositionStatus(item)
                            == ItemMaterialRegistry.CompositionLookupState.ABSENT,
                    "cleared tag generation remains distinct from an unpublished generation");
            helper.assertTrue(
                    find(MachineRecipeMaps.Shredder, List.of(stack), List.of()) == null,
                    "removing the tag composition withdraws its Shredder row");
            gregtech.engine.material.MaterialFormContent.publish();
            helper.assertFalse(
                    GregTechMaterials.formBindings().find(new MaterialKey("Copper")).stream()
                            .anyMatch(binding -> binding.id().equals("minecraft:carrot_on_a_stick")),
                    "removing the tag composition withdraws its public form binding");
        } finally {
            ItemMaterialRegistry.publishTagCandidates(Map.of());
            gregtech.content.recipe.ShredderRecyclingRecipes.clearTagRows();
            CraftingMaterialForms.rebuild();
        }
        helper.succeed();
    }

    // GameTest runs batches sequentially, so the asynchronous reload cannot overlap other tests.
    @GameTest(template = "test_empty", batch = "material_baseline_reload", timeoutTicks = 6000)
    public static void realResourceReloadDoesNotAccumulateRules(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var packs = List.copyOf(server.getPackRepository().getSelectedIds());
        int applications = MachineRecipeOverlay.applications();
        long compositionRevision = ItemMaterialRegistry.compositionRevision();
        var compositions = compositionSnapshot();
        capture(helper, "startup");
        var before = ownedCounts();
        var sample = describe(new ItemStack(Items.IRON_BLOCK));
        RELOAD_COMPLETE.set(false);
        RELOAD_FAILURE.set(null);
        server.reloadResources(packs).thenCompose(ignored -> server.reloadResources(packs)).whenComplete((ignored, failure) -> {
            RELOAD_FAILURE.set(failure);
            RELOAD_COMPLETE.set(true);
        });
        awaitReload(helper, applications, compositionRevision, compositions, before, sample);
    }

    private static void awaitReload(
            GameTestHelper helper,
            int applications,
            long compositionRevision,
            Map<String, ItemComposition> compositions,
            List<Integer> before,
            String sample) {
        if (!RELOAD_COMPLETE.get()) {
            helper.runAfterDelay(
                    1, () -> awaitReload(helper, applications, compositionRevision, compositions, before, sample));
            return;
        }
        var failure = RELOAD_FAILURE.get();
        if (failure != null) {
            helper.fail("resource reload failed: " + failure);
            return;
        }
        helper.assertTrue(
                MachineRecipeOverlay.applications() >= applications + 2,
                "both real reloads applied the overlay");
        helper.assertTrue(
                before.equals(ownedCounts()), "owned rules do not accumulate: " + before + " -> " + ownedCounts());
        helper.assertTrue(
                sample.equals(describe(new ItemStack(Items.IRON_BLOCK))),
                "fixed composition is stable after reload");
        helper.assertTrue(
                ItemMaterialRegistry.compositionRevision() >= compositionRevision,
                "reload invalidates composition-dependent caches");
        helper.assertTrue(
                MachineRecipeOverlay.lastPublication().recipeRevision() == MachineRecipeOverlay.generation(),
                "the successful recipe overlay publishes one stable material/composition/recipe stamp");
        helper.assertTrue(
                MachineRecipeOverlay.lastPublication().compositionRevision()
                        == ItemMaterialRegistry.compositionRevision(),
                "the publication stamp names the composition snapshot used by the recipe overlay");
        helper.assertTrue(
                compositions.equals(compositionSnapshot()),
                "repeat reload preserves effective composition membership and values");
        capture(helper, "reload");
        var reloadedRecipe = find(MachineRecipeMaps.Shredder, List.of(new ItemStack(Items.IRON_INGOT)), List.of());
        helper.assertTrue(
                reloadedRecipe != null,
                "iron ingot recycling remains queryable after reload");
        var powellite =
                GTItems.getStack(MaterialPrefix.dust, gregtech.engine.material.GTMaterialRegistry.get("Powellite"), 6);
        var reloadedDecomposition = find(
                MachineRecipeMaps.Electrolyzer,
                List.of(new ItemStack(GTTechnological.selectorTag(0)), powellite),
                List.of());
        helper.assertTrue(reloadedDecomposition != null, "decomposition row is rebuilt after reload");
        helper.succeed();
    }

    private static List<Integer> ownedCounts() {
        return List.of(gregtech.content.recipe.ExternalOreProcessing.owned().size(),
                gregtech.content.recipe.DecompositionRecipeLoader.owned().size(),
                gregtech.content.recipe.ShredderRecyclingRecipes.owned().size(),
                gregtech.content.recipe.SharedThermalInputRows.owned().size());
    }

    private static Recipe find(RecipeMap map, List<ItemStack> items, List<FluidStack> fluids) {
        return map.findRecipe(items, fluids, false, map.mInputItemsCount, map.mOutputItemsCount);
    }

    private static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

    private static ItemComposition taggedComposition(gregtech.api.material.GTMaterial material, long amount) {
        return taggedComposition(null, material, amount);
    }

    private static ItemComposition taggedComposition(
            MaterialPrefix prefix, gregtech.api.material.GTMaterial material, long amount) {
        return new ItemComposition(
                prefix,
                List.of(gregtech.api.material.MaterialComponent.of(material, amount)),
                material,
                List.of(new ItemCompositionSource(ItemCompositionSource.Kind.FORM_TAG, "probe", "test", "", "")),
                true);
    }

    private static Map<String, ItemComposition> compositionSnapshot() {
        var entries = new TreeMap<String, ItemComposition>();
        for (var entry : ItemMaterialRegistry.entries().entrySet())
            entries.put(id(new ItemStack(entry.getKey())), entry.getValue());
        return Map.copyOf(entries);
    }

    private static String describe(ItemStack stack) {
        var data = ItemMaterialRegistry.get(stack).orElse(null);
        String state = "\tdamage=" + stack.getDamageValue() + "/" + stack.getMaxDamage() + "\tshape=" + MoldItemData.shape(stack);
        if (data == null) return id(stack) + "\tabsent" + state;
        return id(stack) + "\t" + (data.prefix() == null ? "-" : data.prefix().getName())
                + "\t" + data.material().resolve().getName() + "\t" + data.components().stream()
                .map(c -> c.material().resolve().getName() + "=" + c.amount()).sorted().collect(Collectors.joining(","))
                + "\t" + data.bulkRecoveryAllowed() + "\t" + ItemMaterialRegistry.canRecover(stack)
                + "\t" + ItemMaterialRegistry.canMelt(stack) + "\t" + ItemMaterialRegistry.hasStoredContents(stack)
                + "\t" + CrucibleItemInput.parse(stack).stream().map(c -> c.material.getName() + "=" + c.amount).sorted().collect(Collectors.joining(","))
                + "\t" + data.source() + state;
    }

    private static void observe(GameTestHelper helper, String name, ItemStack stack) {
        write(helper, name.replace(':', '_') + ".tsv", List.of(describe(stack)));
    }

    private static String signature(RecipeMap map, Recipe row) {
        var slots = new ArrayList<String>();
        for (int i = 0; i < row.mInputs.length; i++) {
            var unit = row.materialInputDisplay(i);
            // A tag's representative is selected from an unordered set; it is not the input contract.
            String input = row.itemTag(i) == null ? item(row.mInputs[i])
                    : "tag:" + row.itemTag(i).location() + "*" + row.mInputs[i].getCount();
            slots.add("in[" + i + "]=" + input + (unit == null ? "" : ":unit:" + unit.amount() + ":" + unit.rounding())
                    + ":tag=" + (row.itemTag(i) == null ? "-" : row.itemTag(i).location())
                    + ":catalyst=" + row.isCatalystInput(i) + ":guard=" + row.hasItemInputGuard(i));
        }
        for (int i = 0; i < row.mOutputs.length; i++) slots.add("out[" + i + "]=" + item(row.mOutputs[i]) + ":chance=" + row.getOutputChance(i));
        for (int i = 0; i < row.mFluidInputs.length; i++) slots.add("fluidIn[" + i + "]="
                + (row.fluidTag(i) == null ? fluid(row.mFluidInputs[i])
                    : "tag:" + row.fluidTag(i).location() + "*" + row.mFluidInputs[i].getAmount())
                + ":tag=" + (row.fluidTag(i) == null ? "-" : row.fluidTag(i).location())
                + ":catalyst=" + row.isCatalystFluidInput(i) + ":presence=" + row.isPresenceFluidInput(i));
        for (int i = 0; i < row.mFluidOutputs.length; i++) slots.add("fluidOut[" + i + "]=" + fluid(row.mFluidOutputs[i]));
        return map.mNameInternal + "\t" + String.join(";", slots) + "\t" + row.mDuration + "\t" + row.mEUt + "\t" + row.mSpecialValue
                + "\t" + row.mEnabled + "/" + row.mFakeRecipe + "/" + row.mNeedsEmptyOutput + "/" + row.mMaterialRecovery
                + "/" + row.mRequiresEmptyContainerInputs + "/" + row.mExactItemInputs + "/" + row.mExplicitCatalystsOnly;
    }

    private static String item(ItemStack stack) { return stack == null || stack.isEmpty() ? "-" : id(stack) + "*" + stack.getCount(); }
    private static String fluid(FluidStack stack) { return stack == null || stack.isEmpty() ? "-" : BuiltInRegistries.FLUID.getKey(stack.getFluid()) + "*" + stack.getAmount(); }

    private static void capture(GameTestHelper helper, String phase) {
        write(helper, phase + "-items.tsv", BuiltInRegistries.ITEM.stream().map(ItemStack::new)
                .filter(s -> ItemMaterialRegistry.base(s.getItem()).isPresent()).map(MaterialBaselineTests::describe).sorted().toList());
        var rows = new ArrayList<String>();
        for (var map : RecipeMap.RECIPE_MAP_LIST) for (var row : map.mRecipeList) rows.add(signature(map, row));
        rows.sort(String::compareTo);
        write(helper, phase + "-recipes.tsv", rows);
        var tags = new java.util.TreeMap<String, String>();
        for (var map : RecipeMap.RECIPE_MAP_LIST) for (var row : map.mRecipeList) {
            for (int i = 0; i < row.mInputs.length; i++) {
                var tag = row.itemTag(i);
                if (tag != null) tags.computeIfAbsent("item:" + tag.location(), key -> BuiltInRegistries.ITEM.getTag(tag)
                        .map(set -> set.stream().map(holder -> BuiltInRegistries.ITEM.getKey(holder.value()).toString())
                                .sorted().collect(Collectors.joining(","))).orElse(""));
            }
            for (int i = 0; i < row.mFluidInputs.length; i++) {
                var tag = row.fluidTag(i);
                if (tag != null) tags.computeIfAbsent("fluid:" + tag.location(), key -> BuiltInRegistries.FLUID.getTag(tag)
                        .map(set -> set.stream().map(holder -> BuiltInRegistries.FLUID.getKey(holder.value()).toString())
                                .sorted().collect(Collectors.joining(","))).orElse(""));
            }
        }
        write(helper, phase + "-tags.tsv", tags.entrySet().stream().map(entry -> entry.getKey() + "\t" + entry.getValue()).toList());
        write(helper, phase + "-forms.tsv", BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof gregtech.engine.material.MaterialFormItem)
                .map(item -> {
                    var form = (gregtech.engine.material.MaterialFormItem) item;
                    return BuiltInRegistries.ITEM.getKey(item) + "\t" + form.getPrefix().getName() + "\t"
                            + form.getMaterial().resolve().getName() + "\t" + form.getPrefix().getMaterialWeight()
                            + "\t" + form.getPrefix().isValidFor(form.getMaterial());
                }).sorted().toList());
        write(helper, phase + "-block-forms.tsv", BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof gregtech.content.block.MaterialBlockLike)
                .map(block -> {
                    var form = (gregtech.content.block.MaterialBlockLike) block;
                    return BuiltInRegistries.BLOCK.getKey(block) + "\t" + BuiltInRegistries.ITEM.getKey(block.asItem())
                            + "\t" + form.prefix().getName() + "\t" + form.material().resolve().getName()
                            + "\t" + form.prefix().getMaterialWeight() + "\t" + form.prefix().isValidFor(form.material());
                }).sorted().toList());
    }

    private static void write(GameTestHelper helper, String file, List<String> rows) {
        try {
            Path run = Path.of("").toAbsolutePath();
            Path root = run;
            while (root != null && !Files.isRegularFile(root.resolve("settings.gradle"))) root = root.getParent();
            if (root == null) throw new IllegalStateException("cannot find repository root for baseline output");
            Path output = root.resolve("work/material-system-refactor/runtime")
                    .resolve(net.minecraft.SharedConstants.getCurrentVersion().getName()).resolve(run.getFileName());
            Files.createDirectories(output);
            Files.write(output.resolve(file), rows);
        } catch (Exception failure) { helper.fail("cannot write material baseline: " + failure); }
    }
}
