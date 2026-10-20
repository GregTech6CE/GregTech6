package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.blockentity.machine.LargeRecipeMachineBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineLayouts;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.jei.MultiblockInfoData;
import com.gregtech.gregtech.jei.RecipeMachines;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Sample addon machines, the viewer index, and built-in registry ids. */
@GameTestHolder("gregtech_card11_sample")
@PrefixGameTestTemplate(false)
public final class AddonSampleMachineTests {
    private AddonSampleMachineTests() {}

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void builtInIdsStayInGregtechAndViewersSeeTheSample(GameTestHelper helper) throws Exception {
        var blocks = new ArrayList<String>();
        var entities = new ArrayList<String>();
        int builtIn = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof BasicMachineBlock machine)) continue;
            var spec = machine.basicSpec();
            var key = BuiltInRegistries.BLOCK.getKey(machine);
            helper.assertTrue(key.getNamespace().equals(spec.namespace()) && key.getPath().equals(spec.id()),
                    "block id follows the spec " + key);
            var expectedEntity = ResourceLocation.parse(spec.namespace() + ":be_" + spec.id());
            ResourceLocation entityKey;
            if (BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(expectedEntity)) {
                entityKey = expectedEntity;
            } else {
                var probe = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
                helper.getLevel().setBlockAndUpdate(probe, machine.defaultBlockState());
                var entity = helper.getLevel().getBlockEntity(probe);
                helper.assertTrue(entity != null, "placed block entity " + key);
                entityKey = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType());
                helper.getLevel().removeBlock(probe, false);
                helper.assertTrue(entityKey != null && entityKey.getNamespace().equals(spec.namespace()),
                        "controller block entity stays in " + spec.namespace() + " " + entityKey);
            }
            var itemKey = BuiltInRegistries.ITEM.getKey(machine.asItem());
            helper.assertTrue(itemKey.equals(key), "item id matches the block " + itemKey);
            if ("gt6ce".equals(spec.namespace())) builtIn++;
            blocks.add(key.toString());
            entities.add(entityKey.toString());
        }
        helper.assertTrue(builtIn > 0, "built-in machines registered");
        helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("gt6ce:sample_press")),
                "sample press is not a gregtech block");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("addon_sample:sample_press")),
                "sample press block");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("addon_sample:large_sample_press")),
                "large sample press block");
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("addon_sample:be_sample_press")),
                "sample press block entity");
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ResourceLocation.parse("addon_sample:be_large_sample_press")),
                "large sample press block entity");
        helper.assertTrue(BuiltInRegistries.MENU.containsKey(ResourceLocation.parse("gt6ce:basic_machine")),
                "shared menu");
        helper.assertTrue(!BuiltInRegistries.MENU.containsKey(ResourceLocation.parse("addon_sample:basic_machine")),
                "addon does not get its own machine menu");
        var press = map("addon_sample.recipe.press");
        var large = map("addon_sample.recipe.large_press");
        helper.assertTrue(!press.mRecipeList.isEmpty() && !large.mRecipeList.isEmpty(), "datapack rows reached both maps");
        var catalysts = RecipeMachines.collect();
        helper.assertTrue(catalysts.getOrDefault(press, java.util.List.of()).stream()
                .anyMatch(stack -> stack.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("addon_sample:sample_press")))),
                "press catalyst");
        helper.assertTrue(catalysts.getOrDefault(large, java.util.List.of()).stream()
                .anyMatch(stack -> stack.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("addon_sample:large_sample_press")))),
                "large press catalyst");
        var largeBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("addon_sample:large_sample_press"));
        helper.assertTrue(MultiblockInfoData.recipes().stream().anyMatch(info -> info.controller().is(largeBlock.asItem())
                && info.cells().size() == 18), "structure preview includes the controller and 17 casing cells");
        blocks.sort(Comparator.naturalOrder());
        entities.sort(Comparator.naturalOrder());
        var lines = new ArrayList<String>();
        lines.add("blocks " + blocks.size());
        lines.addAll(blocks);
        lines.add("block_entities " + entities.size());
        lines.addAll(entities);
        lines.add("menu gt6ce:basic_machine");
//? if >=1.21.1 {
/*        var directory = helper.getLevel().getServer().getServerDirectory();
*///?} else {
        var directory = helper.getLevel().getServer().getServerDirectory().toPath();
//?}
        Files.write(directory.resolve("card11-registry.txt"), lines);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void samplePressConsumesEuAndEmitsNugget(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
        var machine = place(helper, "addon_sample:sample_press", pos);
        machine.inventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        run(helper, machine, 32, 40);
        helper.assertTrue(machine.inventory().getStackInSlot(0).isEmpty(), "press consumed the ingot");
        helper.assertTrue(machine.inventory().getStackInSlot(machine.inputSlots()).is(Items.GOLD_NUGGET), "press produced a gold nugget");
        helper.getLevel().removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void largeSamplePressFormsAndMovesItemsThroughPorts(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 0, 2)).atY(240);
        var machine = (LargeRecipeMachineBlockEntity) place(helper, "addon_sample:large_sample_press", pos);
        var front = Direction.NORTH;
        var cells = LargeMachineLayouts.cells("largesamplepress");
        helper.assertTrue(cells != null && cells.size() == 17, "sample layout has 17 casing cells");
        for (var cell : cells) helper.getLevel().setBlockAndUpdate(cell.at(pos, front), cell.block().defaultBlockState());
        helper.assertTrue(machine.isStructureOk(), "large sample press formed");
        var items = machine.portItems(Role.ITEM_FLUID_IO);
        helper.assertTrue(items.insertItem(0, new ItemStack(Items.IRON_BLOCK), false).isEmpty(), "item port accepted the block");
        helper.assertTrue(machine.injectPortEnergy(Role.ENERGY_INPUT, GregTechTags.Energy.EU, 512, 1024, true) > 0,
                "energy port accepted EU");
        for (int tick = 0; tick < 30; tick++) {
            machine.injectPortEnergy(Role.ENERGY_INPUT, GregTechTags.Energy.EU, 512, 1024, true);
            BasicMachineBlockEntity.serverTick(helper.getLevel(), pos, machine.getBlockState(), machine);
        }
        var output = items.extractItem(1, 64, false);
        helper.assertTrue(output.is(Items.GOLD_INGOT) && output.getCount() == 1, "item port returned a gold ingot");
        helper.assertTrue(items.getStackInSlot(0).isEmpty(), "input left the port");
        helper.getLevel().removeBlock(pos, false);
        for (var cell : cells) helper.getLevel().removeBlock(cell.at(pos, front), false);
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void builtInSaveKeepsItemsEnergyAndRecordedWork(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 0, 2)).atY(250);
        var block = (BasicMachineBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.parse("gt6ce:press_steel"));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState().setValue(BasicMachineBlock.FACING, Direction.NORTH));
        var machine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(pos);
        var idle = new CompoundTag();
        idle.putLong("gt.energy", 24);
//? if >=1.21.1 {
/*        machine.loadAdditional(idle, helper.getLevel().registryAccess());
*///?} else {
        machine.load(idle);
//?}
        helper.assertTrue(machine.getEnergyTick() == 24, "historical energy key reloads");
        machine.inventory().setStackInSlot(0, new ItemStack(Items.STICK, 3));
        machine.doEnergyInjection(machine.spec().energyTag(), null, machine.spec().energyIn(), machine.spec().energyIn(), true);
        BasicMachineBlockEntity.serverTick(helper.getLevel(), pos, machine.getBlockState(), machine);
        long progress = machine.machineControl(null).progress();
        long energy = machine.getEnergyTick();
//? if >=1.21.1 {
/*        var saved = new CompoundTag();
        machine.saveAdditional(saved, helper.getLevel().registryAccess());
*///?} else {
        var saved = machine.saveWithoutMetadata();
//?}
        helper.getLevel().removeBlock(pos, false);
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState().setValue(BasicMachineBlock.FACING, Direction.NORTH));
        var restored = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(pos);
//? if >=1.21.1 {
/*        restored.loadAdditional(saved, helper.getLevel().registryAccess());
*///?} else {
        restored.load(saved);
//?}
        helper.assertTrue(restored.inventory().getStackInSlot(0).is(Items.STICK)
                && restored.inventory().getStackInSlot(0).getCount() == 3, "saved items reload");
        helper.assertTrue(restored.getEnergyTick() == energy, "saved energy reloads");
        helper.assertTrue(progress == 0 || restored.machineControl(null).progress() == progress,
                "recorded work reloads when the save contains it");
        helper.getLevel().removeBlock(pos, false);
        helper.succeed();
    }

    private static RecipeMap map(String name) {
        for (var map : RecipeMap.RECIPE_MAP_LIST) if (name.equals(map.mNameInternal)) return map;
        throw new AssertionError("Missing recipe map " + name);
    }

    private static BasicMachineBlockEntity place(GameTestHelper helper, String id, BlockPos pos) {
        var block = (BasicMachineBlock) BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState().setValue(BasicMachineBlock.FACING, Direction.NORTH));
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        if (!(entity instanceof BasicMachineBlockEntity machine)) throw new AssertionError("No machine at " + id);
        return machine;
    }

    private static void run(GameTestHelper helper, BasicMachineBlockEntity machine, long packet, int ticks) {
        var pos = machine.getBlockPos();
        for (int tick = 0; tick < ticks; tick++) {
            machine.doEnergyInjection(GregTechTags.Energy.EU, null, packet, packet * 2, true);
            BasicMachineBlockEntity.serverTick(helper.getLevel(), pos, machine.getBlockState(), machine);
        }
    }
}
