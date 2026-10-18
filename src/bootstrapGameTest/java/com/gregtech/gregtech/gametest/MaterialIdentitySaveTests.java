package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.crucible.MoldShapes;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.api.material.StoredMaterial;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.MoldBlockEntity;
import com.gregtech.gregtech.blockentity.machine.SmeltingCrucibleBlockEntity;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.registry.GTItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_card15")
@PrefixGameTestTemplate(false)
public final class MaterialIdentitySaveTests {
    private MaterialIdentitySaveTests() {}

    @GameTest(template = "test_empty")
    public static void namesRoundTripAndOldIdsStillRead(GameTestHelper helper) {
        var copper = GTMaterialRegistry.get("Copper");
        var entry = new CompoundTag();
        StoredMaterial.writeStack(entry, copper, 5L);
        helper.assertTrue(entry.contains("name", CompoundTag.TAG_STRING) && !entry.contains("id"),
                "new crucible entry stores the name");
        helper.assertTrue("Copper".equals(entry.getString("name")), "crucible name is Copper");
        helper.assertTrue(StoredMaterial.readStackMaterial(entry) == copper, "name reads back");

        var legacy = new CompoundTag();
        legacy.putInt("id", copper.getId());
        legacy.putLong("amount", 5L);
        helper.assertTrue(StoredMaterial.readStackMaterial(legacy) == copper, "old numeric id reads the built-in material");

        var usb = new CompoundTag();
        StoredMaterial.writeName(usb, "gt.replicator.data", copper);
        helper.assertTrue("Copper".equals(usb.getString("gt.replicator.data")), "usb stores the name");
        helper.assertTrue(StoredMaterial.read(usb, "gt.replicator.data") == copper, "usb name reads back");

        var oldUsb = new CompoundTag();
        oldUsb.putShort("gt.replicator.data", (short) copper.getId());
        helper.assertTrue(StoredMaterial.read(oldUsb, "gt.replicator.data") == copper, "old usb short reads the built-in material");

        var display = new CompoundTag();
        StoredMaterial.writeDisplay(display, "gt.display.material", copper.getId());
        helper.assertTrue(StoredMaterial.readId(display, "gt.display.material") == copper.getId(),
                "display name reads back as the in-memory id");
        var oldDisplay = new CompoundTag();
        oldDisplay.putInt("gt.display.material", copper.getId());
        helper.assertTrue(StoredMaterial.readId(oldDisplay, "gt.display.material") == copper.getId(),
                "old display int reads back");
        var empty = new CompoundTag();
        StoredMaterial.writeDisplay(empty, "gt.display.material", -1);
        helper.assertTrue(StoredMaterial.readId(empty, "gt.display.material") == -1, "an empty display clears the material");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void meltAddonIngotAndPourIt(GameTestHelper helper) {
        var alloy = GTMaterialRegistry.get("SampleAlloy");
        helper.assertTrue(alloy.isValid() && com.gregtech.gregtech.api.addon.AddonMaterials.owns(alloy),
                "SampleAlloy is an addon material");
        var ingot = GTItems.getStack(MaterialPrefix.ingot, alloy, 1);
        helper.assertTrue(!ingot.isEmpty(), "the addon metal has an ingot");
        var pos = new BlockPos(2, 2, 2);
        var state = block("smelting_crucible_carbon").defaultBlockState();
        helper.setBlock(pos, state);
        var crucible = (SmeltingCrucibleBlockEntity) helper.getBlockEntity(pos);
        var absolute = helper.absolutePos(pos);
        heat(helper, crucible, absolute, state, alloy.getMeltingPoint() + 40);
        var dropped = new ItemEntity(helper.getLevel(), absolute.getX() + 0.5, absolute.getY() + 0.5,
                absolute.getZ() + 0.5, ingot.copy());
        helper.getLevel().addFreshEntity(dropped);
        SmeltingCrucibleBlockEntity.serverTick(helper.getLevel(), absolute, state, crucible);
        helper.assertTrue(crucible.getCacheStack().isEmpty(), "the crucible takes the dropped ingot");
        var melted = crucible.getContentView().stream()
                .filter(stack -> stack.material.resolve() == alloy && stack.amount >= GTValues.U).findFirst();
        helper.assertTrue(melted.isPresent(), "the ingot melts into the crucible");
        heat(helper, crucible, absolute, state, alloy.getMeltingPoint() + 40);
        var saved = crucible.saveWithoutMetadata();
        var entry = saved.getList("gt.materials", Tag.TAG_COMPOUND).getCompound(0);
        helper.assertTrue(entry.contains("name", Tag.TAG_STRING) && !entry.contains("id")
                        && "SampleAlloy".equals(entry.getString("name")),
                "the crucible save stores the addon name");
        helper.assertTrue(!saved.contains("gt.display.material")
                        || saved.get("gt.display.material").getId() == Tag.TAG_STRING,
                "the display field is a name when it is written");

        var moldPos = pos.east();
        helper.setBlock(moldPos, block("mold_carbon").defaultBlockState());
        var mold = (MoldBlockEntity) helper.getBlockEntity(moldPos);
        helper.assertTrue(mold.setDungeonShape(ingotShape()), "ingot mold cavity");
        helper.assertTrue(MoldShapes.recipe(ingotShape()).itemPrefix() == MaterialPrefix.ingot, "the cavity casts an ingot");
        helper.assertTrue(crucible.fillMoldAtSide(mold, Direction.EAST.ordinal(), Direction.WEST.ordinal()),
                "the crucible pours into the mold");
        var moldAbsolute = helper.absolutePos(moldPos);
        for (int i = 0; i < 80 && !mold.isContentSolidified(); i++) {
            MoldBlockEntity.serverTick(helper.getLevel(), moldAbsolute, mold.getBlockState(), mold);
        }
        var cast = mold.getMoldContentMaterial();
        helper.assertTrue(mold.isContentSolidified() && cast != null && cast.resolve() == alloy,
                "the casting solidifies as the addon alloy");
        mold.dropContent();
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(moldAbsolute).inflate(1));
        long casts = drops.stream().filter(entity -> {
            var stack = entity.getItem();
            return !stack.isEmpty() && MaterialItem.getMaterial(stack).resolve() == alloy
                    && MaterialItem.getPrefix(stack) == MaterialPrefix.ingot;
        }).count();
        helper.assertTrue(casts == 1, "pouring drops one addon ingot");

        var legacyPos = new BlockPos(2, 2, 6);
        helper.setBlock(legacyPos, state);
        var legacy = (SmeltingCrucibleBlockEntity) helper.getBlockEntity(legacyPos);
        var copper = GTMaterialRegistry.get("Copper");
        var old = new CompoundTag();
        var list = new ListTag();
        var oldEntry = new CompoundTag();
        oldEntry.putInt("id", copper.getId());
        oldEntry.putLong("amount", GTValues.U);
        list.add(oldEntry);
        old.put("gt.materials", list);
        legacy.load(old);
        helper.assertTrue(legacy.getContentView().size() == 1 && legacy.getContentView().get(0).material.resolve() == copper,
                "an old numeric crucible list loads the built-in material");
        var rewritten = legacy.saveWithoutMetadata().getList("gt.materials", Tag.TAG_COMPOUND).getCompound(0);
        helper.assertTrue("Copper".equals(rewritten.getString("name")) && !rewritten.contains("id"),
                "saving the old crucible rewrites the name");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 600)
    public static void scanAddonMaterialAndReplicateNamedIron(GameTestHelper helper) {
        var nitrate = GTMaterialRegistry.get("AmmoniumNitrate");
        var dust = GTItems.getStack(MaterialPrefix.dust, nitrate, 1);
        var blank = item("usb3_stick");
        var scan = MachineRecipeMaps.ScannerMolecular.findRecipe(List.of(dust, blank), List.of(), false, 3, 3,
                helper.getLevel(), null, ItemStack.EMPTY);
        helper.assertTrue(scan != null, "the scanner accepts addon dust");
        var consumed = RecipeInputs.consume(scan, List.of(dust, blank), List.of(), 1);
        var scanned = scan.mOutputs[0];
        var file = scanned.getTag().getCompound("gt.usb.data");
        helper.assertTrue(consumed != null && file.get("gt.replicator.data").getId() == Tag.TAG_STRING
                        && "AmmoniumNitrate".equals(file.getString("gt.replicator.data"))
                        && GTMaterialDataRecipes.scannedMaterial(scanned).resolve() == nitrate,
                "the scanned addon stick stores the name");
        helper.assertTrue(MachineRecipeMaps.Replicator.findRecipe(List.of(scanned), matter(nitrate), false, 3, 3,
                        helper.getLevel(), null, ItemStack.EMPTY) == null,
                "ammonium nitrate is not replicable");

        var iron = GTMaterialRegistry.get("Iron");
        var usb = GTMaterialDataRecipes.withMaterialData(blank.copy(), iron);
        helper.assertTrue(usb.getTag().getCompound("gt.usb.data").get("gt.replicator.data").getId() == Tag.TAG_STRING,
                "the iron stick used by the replicator stores a name");
        var machine = replicator(helper, new BlockPos(8, 2, 3));
        machine.inventory().setStackInSlot(0, usb.copy());
        var fluids = matter(iron);
        for (int i = 0; i < fluids.size(); i++) machine.getTanksInput()[i].setFluid(fluids.get(i));
        for (int i = 0; i < 2000; i++) {
            machine.doEnergyInjection(machine.spec().energyTag(), null, machine.spec().energyInMax(), 1, true);
            BasicMachineBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
        }
        var output = machine.inventory().getStackInSlot(MachineRecipeMaps.Replicator.mInputItemsCount);
        helper.assertTrue(!output.isEmpty() && MaterialItem.getMaterial(output).resolve() == iron,
                "a name-bearing iron stick replicates iron");
        helper.assertTrue(GTMaterialDataRecipes.scannedMaterial(machine.inventory().getStackInSlot(0)).resolve() == iron,
                "the replicator keeps the named stick");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void builtinTooltipFlagsMatchTheOldRanges(GameTestHelper helper) {
        var problems = new ArrayList<String>();
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            GTMaterial canonical = material.resolve();
            if (com.gregtech.gregtech.api.addon.AddonMaterials.owns(canonical)) {
                if (GTMaterialRegistry.isPeriodicTableElement(canonical) || GTMaterialRegistry.isVanillaMatter(canonical))
                    problems.add(canonical.getName() + " addon material was stamped");
                continue;
            }
            boolean oldElement = canonical.has(MaterialProperty.ELEMENT) && canonical.getId() > 0 && canonical.getId() < 8000
                    && !canonical.has(MaterialProperty.ANTIMATTER);
            if (GTMaterialRegistry.isPeriodicTableElement(canonical) != oldElement)
                problems.add(canonical.getName() + " periodic table");
            boolean oldVanilla = canonical.getId() >= 7900 && canonical.getId() <= 7912;
            if (GTMaterialRegistry.isVanillaMatter(canonical) != oldVanilla)
                problems.add(canonical.getName() + " vanilla matter");
        }
        helper.assertTrue(problems.isEmpty(), "tooltip flags drifted: " + problems);
        helper.succeed();
    }

    private static void heat(GameTestHelper helper, SmeltingCrucibleBlockEntity crucible, BlockPos absolute,
                             net.minecraft.world.level.block.state.BlockState state, long target) {
        for (int i = 0; i < 80 && crucible.getTemperature() < target
                && crucible.getTemperature() + 80 < crucible.getMeltDownLimitK(); i++) {
            crucible.doInject(GregTechTags.Energy.HU, Direction.DOWN, 400, 1, true);
            SmeltingCrucibleBlockEntity.serverTick(helper.getLevel(), absolute, state, crucible);
        }
        helper.assertTrue(crucible.getTemperature() >= target, "crucible reached " + target
                + " K, was " + crucible.getTemperature());
    }

    private static int ingotShape() {
        int shape = 0;
        for (int bit : new int[]{0, 1, 2, 5, 6, 7, 10, 11, 12, 15, 16, 17, 20, 21, 22}) shape |= 1 << bit;
        return shape;
    }

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech", path));
    }

    private static ItemStack item(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", path)));
    }

    private static List<FluidStack> matter(GTMaterial material) {
        var fluids = new ArrayList<FluidStack>();
        if (material.getNeutrons() > 0) fluids.add(GTFluids.stack("MatterNeutral", (int) material.getNeutrons()));
        if (material.getProtons() > 0) fluids.add(GTFluids.stack("MatterCharged", (int) material.getProtons()));
        return fluids;
    }

    private static BasicMachineBlockEntity replicator(GameTestHelper helper, BlockPos pos) {
        var block = java.util.stream.StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(), false)
                .filter(candidate -> candidate instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock machine
                        && machine.basicSpec().recipeMap() == MachineRecipeMaps.Replicator && machine.basicSpec().tier() == 1)
                .findFirst().orElseThrow();
        helper.setBlock(pos, block.defaultBlockState());
        for (var side : Direction.values()) helper.setBlock(pos.relative(side), Blocks.STONE.defaultBlockState());
        return (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }
}
