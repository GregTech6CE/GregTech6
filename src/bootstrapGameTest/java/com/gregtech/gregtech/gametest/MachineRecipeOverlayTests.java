package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.recipe.MachineRecipeOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** One datapack reload path: add, remove, tag expansion, a real machine, and a second identical reload. */
@GameTestHolder("gregtech_machine_overlay")
@PrefixGameTestTemplate(false)
public final class MachineRecipeOverlayTests {
    private MachineRecipeOverlayTests() {}

    @GameTest(template = "test_empty", batch = "machine_recipe_overlay", timeoutTicks = 8000)
    public static void datapackAddsAndRemovesMachineRecipes(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var ids = new ArrayList<>(server.getPackRepository().getSelectedIds());
        for (int round = 0; round < 2; round++) server.reloadResources(ids).join();
        int untouched = MachineRecipeOverlay.staticRows();
        var crusher = MachineRecipeMaps.Crusher;
        var victim = uniqueInput(crusher);
        helper.assertTrue(victim != null, "crusher has a one-input recipe that can be removed exactly");
        Item input = onlyInput(victim).getItem();
        String inputId = BuiltInRegistries.ITEM.getKey(input).toString();
        helper.assertTrue(crusher.mRecipeList.contains(victim), "the victim starts in the crusher map");

        Path root = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("gt-machine-overlay-probe");
        helper.assertTrue(!Files.exists(root), "temporary machine overlay pack must not overwrite an existing pack");
        Path addition = root.resolve("data/gregtech/recipes/machine/heart_of_the_sea.json");
        Path tagged = root.resolve("data/gregtech/recipes/machine/soul_fire.json");
        Path removal = root.resolve("data/gregtech/recipes/machine/remove_unique.json");
        Path metadata = root.resolve("pack.mcmeta");
        Files.createDirectories(addition.getParent());
        try {
            Files.writeString(metadata, "{\"pack\":{\"pack_format\":15,\"description\":\"Temporary GT machine recipe overlay\"}}");
            Files.writeString(addition, addition(crusher.mNameInternal, "minecraft:heart_of_the_sea", "minecraft:nether_star"));
            Files.writeString(tagged, tagAddition(crusher.mNameInternal, "minecraft:soul_fire_base_blocks", "minecraft:diamond"));
            Files.writeString(removal, removal(crusher.mNameInternal, inputId, onlyInput(victim).getCount()));
            server.getPackRepository().reload();
            var withProbe = new ArrayList<>(ids);
            withProbe.add("file/gt-machine-overlay-probe");
            server.reloadResources(withProbe).join();
            assertOverlay(helper, crusher, victim, input);
            int overlaid = MachineRecipeOverlay.staticRows();
            server.reloadResources(withProbe).join();
            helper.assertTrue(MachineRecipeOverlay.staticRows() == overlaid, "a second reload does not add the datapack row again");
            assertOverlay(helper, crusher, victim, input);

            var block = MachineRegistry.basicMachines().stream().map(entry -> entry.get())
                    .filter(candidate -> candidate.basicSpec().machineName().equals("crusher")).findFirst().orElseThrow();
            var pos = BlockPos.ZERO;
            helper.setBlock(pos, block);
            var machine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            machine.setSpec(BasicMachineSpec.builder("overlay_crusher", block.basicSpec().material())
                    .machineType("crusher").energy(GregTechTags.Energy.TU, 1).energyRange(1, 1).recipes(crusher)
                    .faces(block.basicSpec().faceConfig()).build());
            machine.inventory().setStackInSlot(0, new ItemStack(Items.HEART_OF_THE_SEA));
            for (int tick = 0; tick < 64; tick++)
                BasicMachineBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), machine.getBlockState(), machine);
            helper.assertTrue(machine.inventory().getStackInSlot(1).is(Items.NETHER_STAR), "the crusher processes the datapack recipe");

            machine.inventory().setStackInSlot(1, ItemStack.EMPTY);
            machine.inventory().setStackInSlot(0, new ItemStack(Items.HEART_OF_THE_SEA));
            boolean started = false;
            for (int tick = 0; tick < 8 && !started; tick++) {
                BasicMachineBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), machine.getBlockState(), machine);
                started = machine.inventory().getStackInSlot(0).isEmpty() && machine.inventory().getStackInSlot(1).isEmpty();
            }
            helper.assertTrue(started, "the next job consumes its input before the output is flushed");
            server.reloadResources(withProbe).join();
            for (int tick = 0; tick < 64; tick++)
                BasicMachineBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), machine.getBlockState(), machine);
            helper.assertTrue(machine.inventory().getStackInSlot(1).is(Items.NETHER_STAR), "a reload keeps the output already rolled for the running job");

            long reindex = MachineRecipeOverlay.reindexMillis();
            helper.assertTrue(reindex < 120_000, "baseline reindex took " + reindex + " ms");
            server.reloadResources(withProbe).join();
            assertOverlay(helper, crusher, victim, input);
        } finally {
            if (!new java.util.HashSet<>(server.getPackRepository().getSelectedIds()).equals(new java.util.HashSet<>(ids)))
                server.reloadResources(ids).join();
            for (Path path : List.of(addition, tagged, removal, metadata, addition.getParent(),
                    addition.getParent().getParent(), addition.getParent().getParent().getParent(),
                    root.resolve("data"), root))
                Files.deleteIfExists(path);
        }
        helper.assertTrue(recipesWith(crusher, input) == 1, "dropping the pack restores the removed row");
        helper.assertTrue(MachineRecipeOverlay.staticRows() == untouched, "no datapack leaves the machine recipe count unchanged");
        helper.succeed();
    }

    private static void assertOverlay(GameTestHelper helper, com.gregtech.gregtech.api.recipe.RecipeMap crusher, Recipe victim, Item removedInput) {
        helper.assertTrue(!crusher.mRecipeList.contains(victim), "the datapack removal unlinked the matched row");
        helper.assertTrue(recipesWith(crusher, removedInput) == 0, "the exact input removal leaves no row of that item");
        var heart = crusher.findRecipe(List.of(new ItemStack(Items.HEART_OF_THE_SEA)), List.of(), false, 1, 12);
        helper.assertTrue(heart != null && heart.mOutputs[0].is(Items.NETHER_STAR), "heart of the sea crushes to a nether star");
        var sand = crusher.findRecipe(List.of(new ItemStack(Items.SOUL_SAND)), List.of(), false, 1, 12);
        var soil = crusher.findRecipe(List.of(new ItemStack(Items.SOUL_SOIL)), List.of(), false, 1, 12);
        helper.assertTrue(sand != null && sand.mOutputs[0].is(Items.DIAMOND), "soul sand expands from minecraft:soul_fire_base_blocks");
        helper.assertTrue(soil != null && soil.mOutputs[0].is(Items.DIAMOND), "soul soil expands from minecraft:soul_fire_base_blocks");
    }

    private static Recipe uniqueInput(com.gregtech.gregtech.api.recipe.RecipeMap map) {
        for (var recipe : map.mRecipeList) {
            var input = onlyInput(recipe);
            if (input == null || input.getCount() != 1) continue;
            if (input.is(Items.HEART_OF_THE_SEA) || input.is(Items.SOUL_SAND) || input.is(Items.SOUL_SOIL)) continue;
            if (recipesWith(map, input.getItem()) == 1) return recipe;
        }
        return null;
    }

    private static ItemStack onlyInput(Recipe recipe) {
        ItemStack found = null;
        for (var stack : recipe.mInputs) {
            if (stack == null || stack.isEmpty()) continue;
            if (found != null) return null;
            found = stack;
        }
        return found;
    }

    private static int recipesWith(com.gregtech.gregtech.api.recipe.RecipeMap map, Item item) {
        int count = 0;
        for (var recipe : map.mRecipeList)
            for (var stack : recipe.mInputs)
                if (stack != null && !stack.isEmpty() && stack.is(item)) { count++; break; }
        return count;
    }

    private static String addition(String map, String item, String output) {
        return "{\"type\":\"gregtech:machine\",\"map\":\"" + map + "\",\"item_inputs\":[{\"item\":\"" + item
                + "\"}],\"item_outputs\":[{\"item\":\"" + output + "\"}],\"eu_per_tick\":0,\"duration\":16}";
    }

    private static String tagAddition(String map, String tag, String output) {
        return "{\"type\":\"gregtech:machine\",\"map\":\"" + map + "\",\"item_inputs\":[{\"tag\":\"" + tag
                + "\"}],\"item_outputs\":[{\"item\":\"" + output + "\"}],\"eu_per_tick\":0,\"duration\":4}";
    }

    private static String removal(String map, String item, int count) {
        return "{\"type\":\"gregtech:machine_removal\",\"map\":\"" + map + "\",\"item_inputs\":[{\"item\":\""
                + item + "\",\"count\":" + count + "}]}";
    }
}
