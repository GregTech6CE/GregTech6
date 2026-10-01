package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.fluid.FluidDisplayBinding;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.tool.AnvilWorkInputs;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class PortRegressionTests {
    @GameTest(template="test_blueprint_empty")
    public static void anvilTwoIngotsReachPlate(GameTestHelper helper) {
        for (String name : List.of("Iron", "Copper", "Steel", "Tin", "Bronze", "Nickel", "Cobalt")) {
            var material = GTMaterialRegistry.get(name);
            var ingot = GTItems.getStack(MaterialPrefix.ingot, material);
            var doubled = GTItems.getStack(MaterialPrefix.ingotDouble, material);
            var plate = GTItems.getStack(MaterialPrefix.plate, material);
            var join = MachineRecipeMaps.Anvil.mRecipeList.stream().filter(r ->
                    r.mOutputs.length > 0 && ItemStack.isSameItemSameTags(r.mOutputs[0], doubled)
                    && AnvilWorkInputs.consume(new ItemStack[]{ingot.copy(), ingot.copy()}, r.mInputs) != null).findFirst();
            helper.assertTrue(join.isPresent(), name + ": two ingots join into a double ingot");
            helper.assertTrue(AnvilWorkInputs.consume(new ItemStack[]{ingot.copy(), ItemStack.EMPTY}, join.orElseThrow().mInputs) == null,
                    "duplicate inputs cannot reuse a single ingot");
            var flatten = MachineRecipeMaps.Anvil.mRecipeList.stream().filter(r ->
                    r.mOutputs.length > 0 && ItemStack.isSameItemSameTags(r.mOutputs[0], plate)
                    && AnvilWorkInputs.consume(new ItemStack[]{doubled.copy(), ItemStack.EMPTY}, r.mInputs) != null).findFirst();
            helper.assertTrue(flatten.isPresent(), name + ": double ingot flattens into plate");
            helper.assertTrue(flatten.orElseThrow().getOutputChance(1) == 9000, "GT6 scrap probability is retained");
            helper.assertTrue(AnvilWorkInputs.consume(new ItemStack[]{doubled.copy(), ingot.copy()}, flatten.orElseThrow().mInputs) == null,
                    "flattening still requires an empty second work area");
        }
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void sharedControllerLayoutsMatchTheirControllers(GameTestHelper helper) {
        // The original bedrock drill extends five layers below its controller while Von da
        // Graagg extends seven above. Keep both inside the empty test structure.
        var origin = new net.minecraft.core.BlockPos(5,7,5);
        for (var entry : com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.all().entrySet()) {
            helper.setBlock(origin, entry.getKey());
            for (var cell : entry.getValue().entrySet()) helper.setBlock(origin.offset(cell.getKey()), cell.getValue());
            var entity = helper.getLevel().getBlockEntity(helper.absolutePos(origin));
            helper.assertTrue(entity instanceof com.gregtech.gregtech.api.multiblock.StructureController, "controller exposes common status");
            helper.assertTrue(com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.matches(entity), "every preview part matches validation");
            helper.assertTrue(((com.gregtech.gregtech.api.multiblock.StructureController)entity).isStructureOk(), "controller uses shared geometry");
            var broken = entry.getValue().entrySet().stream()
                    .filter(cell -> cell.getValue() != net.minecraft.world.level.block.Blocks.AIR)
                    .findFirst().orElseThrow().getKey();
            helper.setBlock(origin.offset(broken), net.minecraft.world.level.block.Blocks.AIR);
            helper.assertTrue(!com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.matches(entity), "missing preview part invalidates structure");
            for (var cell : entry.getValue().keySet()) helper.setBlock(origin.offset(cell), net.minecraft.world.level.block.Blocks.AIR);
            helper.setBlock(origin, net.minecraft.world.level.block.Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void materialNumericIdsAreUnique(GameTestHelper helper) {
        var ids = new HashMap<Integer, GTMaterial>();
        for (var material : GTMaterialRegistry.sortedMaterials()) {
            if (!material.isValid() || material.resolve() != material) continue;
            helper.assertTrue(ids.put(material.getId(), material) == null, "duplicate ID: " + material.getId());
            helper.assertTrue(GTMaterialRegistry.get(material.getId()) == material, "name/ID lookup differs: " + material.getName());
        }
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void multiblockInspectionHasServerStatus(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(2,2,2);
        helper.setBlock(pos, GTMultiblocks.LARGE_GAS_TURBINE_MAIN.get());
        var entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var data = com.gregtech.gregtech.integration.jade.MultiblockJadeProvider.inspect(entity);
        helper.assertTrue(data.contains("formed") && !data.getBoolean("formed"), "unbuilt controller is explicitly incomplete");
        helper.assertTrue(data.getList("energy",10).size() == 2, "gas turbine exposes RU and fuel heat buffers");
        helper.assertTrue(!data.contains("Items"), "inspection does not send the entire inventory save");
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void displayFluidRoundTrip(GameTestHelper helper) {
        for(var fluid:List.of(net.minecraft.world.level.material.Fluids.WATER,GTFluids.still("Steam").get(),GTFluids.flowing("Steam").get())) {
            var input=new FluidStack(fluid,147);
            input.getOrCreateTag().putString("test_variant","keep-this");
            var item=FluidDisplayBinding.display(input);
            var decoded=FluidDisplayBinding.resolve(item);
            helper.assertTrue(decoded.isFluidEqual(input)&&decoded.getAmount()==147,"display retains registry identity, amount and NBT");
            var resized=input.copy();resized.setAmount(500);
            helper.assertTrue(FluidDisplayBinding.subtype(item).equals(FluidDisplayBinding.subtype(FluidDisplayBinding.display(resized))),"JEI ignores amount in subtype");
            var cap=item.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(IllegalStateException::new);
            var drained=cap.drain(321,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
            helper.assertTrue(drained.isFluidEqual(input)&&drained.getAmount()==321,"creative display capability uses the same fluid and NBT");
            helper.assertTrue(FluidDisplayBinding.resolve(item).getAmount()==147,"simulation does not mutate display payload");
        }
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void anvilProcessing(GameTestHelper helper) {
        var gem=GTItems.getStack(MaterialPrefix.gem,Materials.Diamond,3);
        helper.assertTrue(!gem.isEmpty(),"diamond gem registered");
        var recipe=MachineRecipeMaps.Anvil.mRecipeList.stream().filter(r -> r.mEnabled && r.mInputs[0].getItem()==gem.getItem()).findFirst().orElseThrow();
        var remaining=AnvilWorkInputs.consume(new ItemStack[]{gem,ItemStack.EMPTY},recipe.mInputs);
        helper.assertTrue(remaining!=null&&remaining[0].getCount()==2&&gem.getCount()==3,"gem processing consumes one on a copy");
        helper.assertTrue(AnvilWorkInputs.consume(new ItemStack[]{gem,new ItemStack(Items.STONE)},recipe.mInputs)==null,"other workpiece must be empty");
        helper.assertTrue(!MachineRecipeMaps.AnvilBendBig.mRecipeList.isEmpty()&&!MachineRecipeMaps.AnvilBendSmall.mRecipeList.isEmpty(),"both bending sides have recipes");
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty",timeoutTicks=200)
    public static void creativeBindingParity(GameTestHelper helper) {
        long before=0,after=0,visible=0;
        for(var prefix:PrefixRegistry.all()) {
            var expected=new ArrayList<String>();var actual=new ArrayList<String>();
            for(var material:GTMaterialRegistry.sortedMaterials()) {
                before++;
                if(material.has(MaterialProperty.HIDDEN)||material.resolve()!=material||!prefix.isValidFor(material)) continue;
                var item=GTItems.getObject(prefix,material);
                if(item!=null&&item.isPresent()) expected.add(material.getName());
            }
            for(var binding:GTItems.creativeEntries(prefix)) {
                after++;var material=binding.material();
                if(material.has(MaterialProperty.HIDDEN)||material.resolve()!=material||!prefix.isValidFor(material)) continue;
                if(binding.item().isPresent()) {
                    actual.add(material.getName());
                    helper.assertTrue(GTItems.getCreativeStack(prefix,material).getCount()==1,"one item per creative entry");
                }
            }
            helper.assertTrue(expected.equals(actual),"creative ordering and membership preserved for "+prefix.getName());
            visible+=actual.size();
        }
        com.gregtech.gregtech.GregTech.LOGGER.info("Creative binding parity: candidates {} -> {}, visible entries {}",before,after,visible);
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void recipePowerDisplay(GameTestHelper helper) {
        var recipe = new com.gregtech.gregtech.api.recipe.Recipe(null, null, null, null, null, null, 192, 32, 0);
        var stats = com.gregtech.gregtech.api.recipe.RecipePowerStats.of(recipe, 1);
        helper.assertTrue(stats.costs().longValueExact() == 6144 && stats.usage().longValueExact() == 32
                && stats.tier().longValueExact() == 32 && stats.power() == 1
                && stats.time() == 192 && stats.timeUnit().equals("ticks"), "original five-line example");
        helper.assertTrue(com.gregtech.gregtech.api.recipe.RecipePowerStats.of(recipe, 4).tier().longValueExact() == 8,
                "tier is usage divided by map power");
        for (long ticks : new long[]{1199, 1200, 35999, 36000}) {
            var timed = new com.gregtech.gregtech.api.recipe.Recipe(null, null, null, null, null, null, ticks, -32, 0);
            var view = com.gregtech.gregtech.api.recipe.RecipePowerStats.of(timed, 1);
            helper.assertTrue(view.generating() && view.time() == (ticks < 1200 ? ticks : ticks < 36000 ? ticks / 20 : ticks / 1200), "generation and original time thresholds");
        }
        var huge = new com.gregtech.gregtech.api.recipe.Recipe(null, null, null, null, null, null, Long.MAX_VALUE, Long.MIN_VALUE, 0);
        helper.assertTrue(com.gregtech.gregtech.api.recipe.RecipePowerStats.of(huge, 1).costs().signum() > 0, "energy costs do not overflow");
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void vanillaProcessing(GameTestHelper helper) {
        var brick = recipeUsing(MachineRecipeMaps.Crusher, Items.BRICKS);
        helper.assertTrue(brick.mDuration == 64 && brick.mEUt == 16 && brick.mOutputs.length == 4,
                "brick crushing preserves time, energy and separate outputs");
        helper.assertTrue(java.util.Arrays.equals(brick.mChances, new long[]{10000, 9000, 8000, 7000}), "independent recovery chances");
        helper.assertTrue(recipeUsing(MachineRecipeMaps.Hammer, Items.BRICK_SLAB).mOutputs[0].getCount() == 1, "slab hammer yield");
        helper.assertTrue(recipeUsing(MachineRecipeMaps.Mortar, Items.BONE).mOutputs[0].getCount() == 2, "mortar bone yield");
        helper.assertTrue(recipeUsing(MachineRecipeMaps.Shredder, Items.BONE).mOutputs[0].getCount() == 4, "shredder bone yield");
        for (var color : DyeColor.values()) {
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", color.getName() + "_stained_glass");
            var glass = recipeUsing(MachineRecipeMaps.Mortar, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id));
            helper.assertTrue(glass.mOutputs[0].getCount() == 9, "all glass colors recover nine glass dust");
        }
        var snow = recipeUsing(MachineRecipeMaps.Squeezer, Items.SNOWBALL);
        helper.assertTrue(snow.mFluidOutputs.length == 1 && snow.mFluidOutputs[0].getFluid() == GTFluids.still("Ice").get()
                && snow.mFluidOutputs[0].getAmount() == 250, "snowball yields actual GT ice fluid");
        helper.assertTrue(recipeUsing(MachineRecipeMaps.Squeezer, Items.PACKED_ICE).mDuration == 128, "packed ice original duration");
        helper.succeed();
    }

    private static com.gregtech.gregtech.api.recipe.Recipe recipeUsing(com.gregtech.gregtech.api.recipe.RecipeMap map, Item item) {
        return map.mRecipeList.stream().filter(r -> r.mEnabled && !r.mFakeRecipe && r.mInputs.length == 1
                && r.mInputs[0].getItem() == item).findFirst().orElseThrow(() -> new IllegalStateException("Missing recipe: " + map + " / " + item));
    }

    @GameTest(template="test_blueprint_empty")
    public static void outputChances(GameTestHelper helper) {
        var output = new ItemStack(Items.BRICK, 2);
        var recipe = new com.gregtech.gregtech.api.recipe.Recipe(null, new ItemStack[]{output}, null,
                new long[]{5000}, null, null, 64, 16, 0);
        int[] rolls = {0, 4999, 5000, 9999};
        var index = new java.util.concurrent.atomic.AtomicInteger();
        helper.assertTrue(recipe.rollOutputCount(0, 2, bound -> rolls[index.getAndIncrement()]) == 2
                && index.get() == 4 && output.getCount() == 2, "per-item independent chance across parallel operations, without mutating recipe");
        helper.assertTrue(recipe.rollOutputCount(0, 1, bound -> 9999) == 0, "failed roll does not produce guaranteed output");
        var certain = new com.gregtech.gregtech.api.recipe.Recipe(null, new ItemStack[]{output}, null,
                null, null, null, 64, 16, 0);
        helper.assertTrue(certain.rollOutputCount(0, 3, bound -> { throw new AssertionError("guaranteed output needs no RNG"); }) == 6, "guaranteed output scales with parallel count");
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void restoredMaterialForms(GameTestHelper helper) {
        var stone = GTMaterialRegistry.get("Stone").resolve();
        var stoneDust = GTItems.getStack(MaterialPrefix.dust, stone);
        helper.assertTrue(!stoneDust.isEmpty(), "stone dust registered");
        helper.assertTrue(recipeUsing(MachineRecipeMaps.Shredder, Items.STONE).mOutputs[0].getCount() == 9,
                "previous stone processing gap is closed");
        for (var prefix : List.of(MaterialPrefix.gemChipped, MaterialPrefix.gemFlawed, MaterialPrefix.gem)) {
            var stack = GTItems.getStack(prefix, Materials.Ice);
            helper.assertTrue(!stack.isEmpty(), "ice gem form registered: " + prefix.getName());
            helper.assertTrue(recipeUsing(MachineRecipeMaps.Squeezer, stack.getItem()).mFluidOutputs[0].getFluid() == GTFluids.still("Ice").get(),
                    "ice gem extraction uses the real ice fluid");
            var recipe = recipeUsing(MachineRecipeMaps.Compressor, stack.getItem());
            helper.assertTrue(recipe.mOutputs[0].is(Items.ICE), "all three ice gem grades compress into ice");
        }
        helper.assertTrue(Materials.Ice.getTextureSet() == MaterialTextureSet.CUBE_SHINY, "original ice texture family");
        var packed = recipeUsing(MachineRecipeMaps.Compressor, Items.ICE);
        helper.assertTrue(packed.mInputs[0].getCount() == 2 && packed.mOutputs[0].is(Items.PACKED_ICE)
                && packed.mEUt == 64 && packed.mDuration == 32, "original packed ice compression cost and quantities");
        helper.assertTrue(recipeUsing(MachineRecipeMaps.Compressor, Items.SNOWBALL).mInputs[0].getCount() == 4, "four snowballs per snow block");
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void anvilWorkpieceSynchronization(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 2, 1);
        var block = GTToolBlocks.ANVILS.get(0).get();
        helper.setBlock(pos, block.defaultBlockState());
        var absolute = helper.absolutePos(pos);
        var anvil = (com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity) helper.getLevel().getBlockEntity(absolute);
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        // Forge caches this fake player for the level. Other tests can leave it in creative mode,
        // which deliberately does not consume a stack on the anvil.
        var previousMode = player.gameMode.getGameModeForPlayer();
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            var input = GTItems.getStack(MaterialPrefix.ingot, GTMaterialRegistry.get("Iron"), 3);
            helper.assertTrue(MachineRecipeMaps.Anvil.containsInput(input), "iron ingots are valid anvil workpieces");
            input.getOrCreateTag().putString("workpiece_test", "preserved");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, input);
            var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(absolute.getX()+.25,absolute.getY()+.8,absolute.getZ()+.5),net.minecraft.core.Direction.UP,absolute,false);
            anvil.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(anvil.workpiece(0).getCount()==3 && player.getMainHandItem().isEmpty(), "insert consumes held stack and updates workpiece");
            var client = new com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity(absolute,block.defaultBlockState());
            client.handleUpdateTag(anvil.getUpdateTag());
            helper.assertTrue(ItemStack.isSameItemSameTags(client.workpiece(0),anvil.workpiece(0)) && client.workpiece(0).getCount()==3, "initial chunk sync retains material, count and NBT");
            helper.assertTrue(anvil.getUpdatePacket().getTag().contains("gt.work0"), "live update packet includes workpieces");
            var copy=client.workpiece(0);copy.setCount(1);
            helper.assertTrue(client.workpiece(0).getCount()==3, "renderer accessor cannot mutate inventory");
            anvil.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            client.handleUpdateTag(anvil.getUpdateTag());
            helper.assertTrue(client.workpiece(0).isEmpty(), "removing item clears client display");
            helper.succeed();
        } finally {
            player.setGameMode(previousMode);
        }
    }
    @GameTest(template="test_blueprint_empty")
    public static void anvilWorkpiecePlacement(GameTestHelper helper) {
        for (var facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            for (int shape=0;shape<=7;shape++) {
                var a=com.gregtech.gregtech.content.tool.AnvilWorkpieceGeometry.bounds(shape,0,facing);
                var b=com.gregtech.gregtech.content.tool.AnvilWorkpieceGeometry.bounds(shape,1,facing);
                helper.assertTrue(a.minY==.75 && b.minY==.75 && !a.intersects(b), "workpieces stand on top and occupy separate halves");
                helper.assertTrue(facing.getAxis()==net.minecraft.core.Direction.Axis.Z ? a.maxX<.5 && b.minX>.5 : a.maxZ<.5 && b.minZ>.5, "display halves follow the same axes as interaction");
            }
        }
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void mortarVariantsAndRecipes(GameTestHelper helper) {
        int mortars=0;
        for (var entry:GTToolBlocks.manual()) {
            if (entry.get().kind()!=com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity.Kind.MORTAR) continue;
            mortars++;
            helper.assertTrue(entry.get().tint(0)==Materials.Ceramic.getColor(), "ceramic bowl tint");
            var recipe=helper.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","tools/"+entry.getId().getPath())).orElseThrow();
            helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(entry.get().asItem()), "mortar has a loaded survival crafting recipe");
            helper.assertTrue(GTBlockEntities.MANUAL_TOOL.get().isValid(entry.get().defaultBlockState()), "variant supports the working manual-tool block entity");
        }
        helper.assertTrue(mortars==5, "all five original mortar designs registered");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","tools/ceramic_bowl_firing")).isPresent(), "ceramic bowl firing recipe loaded");
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void anvilRejectsUnrelatedItems(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 2, 1);
        helper.setBlock(pos, GTToolBlocks.ANVILS.get(0).get().defaultBlockState());
        var absolute = helper.absolutePos(pos);
        var anvil = (com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity) helper.getLevel().getBlockEntity(absolute);
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BARRIER, 7));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atLowerCornerOf(absolute).add(.25,.8,.5), net.minecraft.core.Direction.UP, absolute, false);
        anvil.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(anvil.workpiece(0).isEmpty() && player.getMainHandItem().getCount() == 7, "unrelated items stay in hand");
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void basicMachineRemovalDropsInventory(GameTestHelper helper) {
        var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValues().stream()
                .filter(b -> b instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock)
                .map(b -> (com.gregtech.gregtech.block.machine.BasicMachineBlock)b).findFirst().orElseThrow();
        var pos = new net.minecraft.core.BlockPos(1, 2, 1);
        // The GameTest world is reused between runs: drop this test's own leftovers first, or the
        // count below sees a previous run's stack (see LootCrateTests.placeCrate for the same fix).
        helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(2)).forEach(
                net.minecraft.world.entity.Entity::discard);
        helper.setBlock(pos, block.defaultBlockState());
        var absolute = helper.absolutePos(pos);
        var machine = (com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity) helper.getLevel().getBlockEntity(absolute);
        var stack = new ItemStack(Items.DIAMOND, 17);
        stack.getOrCreateTag().putBoolean("drop_test", true);
        machine.inventory().setStackInSlot(0, stack);
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        int count = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(absolute).inflate(1)).stream()
                .map(net.minecraft.world.entity.item.ItemEntity::getItem)
                .filter(s -> ItemStack.isSameItemSameTags(s, stack)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(count == 17, "breaking a machine drops each stored item once with NBT");
        helper.assertTrue(machine.inventory().getStackInSlot(0).isEmpty(), "removed inventory is emptied");
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void recipeInputsAreConserved(GameTestHelper helper) {
        var recipe=new com.gregtech.gregtech.api.recipe.Recipe(new ItemStack[]{new ItemStack(Items.APPLE,2),new ItemStack(Items.APPLE,2)},
                new ItemStack[]{new ItemStack(Items.GOLDEN_APPLE)},null,null,
                new net.minecraftforge.fluids.FluidStack[]{new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,200)},null,16,16,0);
        var water=new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,100);
        helper.assertTrue(com.gregtech.gregtech.api.recipe.RecipeInputs.consume(recipe,List.of(new ItemStack(Items.APPLE,3)),List.of(water,water),1)==null,"one stack cannot satisfy duplicate ingredients twice");
        var result=com.gregtech.gregtech.api.recipe.RecipeInputs.consume(recipe,List.of(new ItemStack(Items.APPLE,3),new ItemStack(Items.APPLE)),List.of(water,water),1);
        helper.assertTrue(result!=null && result.items().stream().allMatch(ItemStack::isEmpty) && result.fluids().stream().allMatch(net.minecraftforge.fluids.FluidStack::isEmpty),"ingredients can span slots and fluid tanks");
        helper.assertTrue(water.getAmount()==100,"simulation preserves source inputs");
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void manualJuicerProcessingAndReload(GameTestHelper helper) {
        var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("gregtech:juicer"));
        var pos=new net.minecraft.core.BlockPos(1,2,1); helper.setBlock(pos,block);
        var tool=(com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var player=helper.makeMockSurvivalPlayer();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,3));
        tool.interact(player,net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.core.Direction.NORTH);
        helper.assertTrue(player.getMainHandItem().getCount()==2 && tool.displayFluid().getAmount()==75,
                "actual hand pressing consumes one apple and produces 75 mB");
        var data=tool.saveWithoutMetadata();
        var copy=new com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity(helper.absolutePos(pos),block.defaultBlockState());copy.load(data);
        helper.assertTrue(copy.displayFluid().getAmount()==75,"juicer NBT preserves juice");
        helper.assertTrue(!tool.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent()
                && !tool.process(null),"juicer cannot run as an automated inventory machine");
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void steelSiftingTableAndManualVats(GameTestHelper helper) {
        var sift=GTToolBlocks.manual().stream().map(e->e.get()).filter(b->b.kind()==com.gregtech.gregtech.blockentity.tool.ManualToolBlockEntity.Kind.SIFTING).findFirst().orElseThrow();
        helper.assertTrue(sift.tint(0)==Materials.Steel.getColor(),"sifting table has steel tint");
        for(String id:List.of("mixing_bowl","mixing_bowl_table","bathing_pot","bathing_pot_table",
                "bathing_pot_wood","bathing_pot_table_wood","juicer")) {
            var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",id));
            helper.assertTrue(com.gregtech.gregtech.registry.GTBlockEntities.PROCESSING_TOOL.get().isValid(block.defaultBlockState()),"manual processing block entity attached: "+id);
        }
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void originalVesselTablesShareRecipesButKeepMaterialLimits(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 2, 1);
        var leadRod = GTItems.getStack(MaterialPrefix.stickLong, Materials.Lead);
        helper.assertTrue(!leadRod.isEmpty() && leadRod.is(net.minecraft.tags.ItemTags.create(
                        net.minecraft.resources.ResourceLocation.parse("forge:long_rods/lead"))),
                "the wooden bathing pot's lead long rod is registered under its shared Forge tag");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(
                        net.minecraft.resources.ResourceLocation.parse("gregtech:tools/bathing_pot_wood")).isPresent(),
                "the wooden bathing pot recipe loads with a registered lead rod ingredient");
        for (String id : List.of("mixing_bowl_table", "bathing_pot_table", "bathing_pot_wood", "bathing_pot_table_wood")) {
            var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", id));
            helper.assertTrue(block instanceof com.gregtech.gregtech.block.tool.ProcessingToolBlock,
                    "GT6 vessel has an active processing block: " + id);
            helper.setBlock(pos, block);
            var tool = (com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity)
                    helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            boolean wooden = id.endsWith("_wood");
            helper.assertTrue(tool != null && tool.recipes() == (id.startsWith("mixing_bowl")
                    ? MachineRecipeMaps.Mixer : MachineRecipeMaps.Bath), "GT6 recipe map: " + id);
            if (wooden) helper.assertTrue(block.getFlammability(block.defaultBlockState(), helper.getLevel(),
                    helper.absolutePos(pos), net.minecraft.core.Direction.UP) == 100,
                    "GT6 wooden pot has its 100 flammability: " + id);
            var fluids = tool.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).orElseThrow(
                    () -> new AssertionError("Missing fluid handler: " + id));
            for (int tank = 0; tank < fluids.getTanks(); tank++)
                helper.assertTrue(fluids.getTankCapacity(tank) == (wooden ? 4000 : 8000),
                        "GT6 tank capacity: " + id);
            helper.assertTrue(fluids.fill(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 1000),
                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) == 0 || !wooden,
                    "wood bath refuses hot lava");
            helper.assertTrue(fluids.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 5000),
                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == (wooden ? 4000 : 5000),
                    "open vessel fills to GT6 capacity: " + id);
            var copy = new com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity(helper.absolutePos(pos),
                    block.defaultBlockState());
            copy.load(tool.saveWithoutMetadata());
            helper.assertTrue(copy.displayFluid().getAmount() == (wooden ? 4000 : 5000),
                    "vessel tank persists through NBT: " + id);
            if (id.contains("_table")) {
                var shape = block.getShape(block.defaultBlockState(), helper.getLevel(), helper.absolutePos(pos),
                        net.minecraft.world.phys.shapes.CollisionContext.empty());
                helper.assertTrue(shape.bounds().maxY == 1.0 && shape.bounds().minY == 0.0,
                        "GT6 table has raised bowl and full-height plinth: " + id);
            }
        }
        var bowl = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", "mixing_bowl_table"));
        helper.setBlock(pos, bowl);
        var tool = (com.gregtech.gregtech.blockentity.tool.ProcessingToolBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var recipe = MachineRecipeMaps.Mixer.mRecipeList.stream()
                .filter(r -> r.mEnabled && !r.mFakeRecipe && r.mEUt >= 0 && r.mEUt <= 32
                        && r.mFluidInputs.length == 0 && r.mOutputs.length == 1 && r.mOutputs[0] != null
                        && r.mOutputs[0].is(Items.FERMENTED_SPIDER_EYE))
                .findFirst().orElseThrow(() -> new AssertionError("GT6 fermented-eye mixer recipe missing"));
        for (int slot = 0; slot < recipe.mInputs.length; slot++) tool.inventory().setStackInSlot(slot, recipe.mInputs[slot].copy());
        helper.assertTrue(tool.process(null), "ceramic table executes a real Mixer recipe");
        helper.assertTrue(tool.inventory().getStackInSlot(MachineRecipeMaps.Mixer.mInputItemsCount)
                .is(Items.FERMENTED_SPIDER_EYE), "ceramic table exposes the processed item");
        helper.succeed();
    }
    @GameTest(template="test_blueprint_empty")
    public static void drawerRemovalDropsAllCompartments(GameTestHelper helper) {
        var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValues().stream().filter(b->b instanceof com.gregtech.gregtech.block.inventory.DrawerQuadBlock).findFirst().orElseThrow();
        var pos=new net.minecraft.core.BlockPos(1,2,1); helper.setBlock(pos,block); var absolute=helper.absolutePos(pos);
        var drawer=(com.gregtech.gregtech.blockentity.inventory.DrawerQuadBlockEntity)helper.getLevel().getBlockEntity(absolute);
        var stack=new ItemStack(Items.EMERALD,64); stack.getOrCreateTag().putBoolean("drawer_drop_test",true);
        var area=new net.minecraft.world.phys.AABB(absolute).inflate(1);
        // Clear the volume before spilling: this structure position is shared by every test in the class
        // and the world itself is reused between gate runs, so a previous spill can still be lying here
        // — world reuse made this test report "spilled 277 (expected 192)" once (2026-09-18). §108's
        // rule for any test that reads the environment: clear your own volume first.
        int leftovers=discardItems(helper.getLevel(),area,stack);
        drawer.items().insertItem(0,stack,false); drawer.items().insertItem(1,stack,false); drawer.items().insertItem(108,stack,false);
        helper.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
        drawer.dropContents(); // Idempotent cleanup cannot duplicate already-spilled items.
        // Count one tick later, through the whole entity table: the freshly spawned drops live in a
        // section that is not tracked in the tick they were added, so a same-tick query can miss them
        // (that made this test red once while the server lagged, "Can't keep up! ... 144 ticks behind").
        // Equal stacks merge but keep their counts, so the sum is stable across the delay.
        helper.runAfterDelay(1,()->{
            int count=countItems(helper.getLevel(),area,stack);
            helper.assertTrue(count==192&&drawer.items().getStackInSlot(0).getCount()==0&&drawer.items().getStackInSlot(108).getCount()==0,
                    "all compartments spill once, including more than one stack: spilled "+count
                            +" (expected 192), stored 0="+drawer.items().getStackInSlot(0).getCount()+", 3="+drawer.items().getStackInSlot(108).getCount()
                            +", leftovers cleared first="+leftovers);
            helper.succeed();
        });
    }

    /** Drops (discards) the items matching {@code like} inside {@code area}; returns how many there were. */
    private static int discardItems(net.minecraft.server.level.ServerLevel level,net.minecraft.world.phys.AABB area,ItemStack like) {
        var doomed=new ArrayList<net.minecraft.world.entity.Entity>();
        for(var entity:level.getAllEntities())
            if(entity instanceof net.minecraft.world.entity.item.ItemEntity item&&area.contains(item.position())
                    &&ItemStack.isSameItemSameTags(item.getItem(),like))doomed.add(entity);
        doomed.forEach(net.minecraft.world.entity.Entity::discard);
        return doomed.size();
    }

    /** Total count of the items matching {@code like} inside {@code area}, whole entity table (no section filter). */
    private static int countItems(net.minecraft.server.level.ServerLevel level,net.minecraft.world.phys.AABB area,ItemStack like) {
        int count=0;
        for(var entity:level.getAllEntities())
            if(entity instanceof net.minecraft.world.entity.item.ItemEntity item&&area.contains(item.position())
                    &&ItemStack.isSameItemSameTags(item.getItem(),like))count+=item.getItem().getCount();
        return count;
    }
    @GameTest(template="test_blueprint_empty")
    public static void pendingOutputsSurvivePartialFlushAndReload(GameTestHelper helper) {
        var recipe=new com.gregtech.gregtech.api.recipe.Recipe(new ItemStack[]{new ItemStack(Items.APPLE)},
                new ItemStack[]{new ItemStack(Items.DIAMOND,4)},null,null,null,
                new net.minecraftforge.fluids.FluidStack[]{new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000)},16,16,0);
        var pending=com.gregtech.gregtech.api.recipe.MachineWorkOutputs.roll(recipe,1,bound->0);
        var inventory=new net.minecraftforge.items.ItemStackHandler(1);
        inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND,63));
        var tank=new com.gregtech.gregtech.api.fluid.FluidTankGT(500);
        helper.assertTrue(!pending.flush(inventory,0,new com.gregtech.gregtech.api.fluid.FluidTankGT[]{tank}),"partial output remains pending");
        helper.assertTrue(inventory.getStackInSlot(0).getCount()==64 && tank.getFluidInTank(0).getAmount()==500,"only available capacity is filled");
        pending=com.gregtech.gregtech.api.recipe.MachineWorkOutputs.load(pending.save());
        inventory.setStackInSlot(0,ItemStack.EMPTY);
        tank.drain(500,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(pending.flush(inventory,0,new com.gregtech.gregtech.api.fluid.FluidTankGT[]{tank}),"saved remainder completes after space is freed");
        helper.assertTrue(inventory.getStackInSlot(0).getCount()==3 && tank.getFluidInTank(0).getAmount()==500,"reload neither duplicates nor discards pending output");
        helper.succeed();
    }

    @GameTest(template="test_blueprint_empty")
    public static void machineDropCarriesPendingWorkWithoutInventoryDuplication(GameTestHelper helper) {
        var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValues().stream()
                .filter(b->b instanceof com.gregtech.gregtech.block.machine.BasicMachineBlock)
                .map(b->(com.gregtech.gregtech.block.machine.BasicMachineBlock)b)
                .filter(b->b.basicSpec().recipeMap().mOutputItemsCount>0 && b.basicSpec().recipeMap().mInputItemsCount>0).findFirst().orElseThrow();
        var pos=new net.minecraft.core.BlockPos(1,2,1); helper.setBlock(pos,block);
        var absolute=helper.absolutePos(pos);
        var machine=(com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)helper.getLevel().getBlockEntity(absolute);
        machine.inventory().setStackInSlot(0,new ItemStack(Items.APPLE,17));
        var recipe=new com.gregtech.gregtech.api.recipe.Recipe(new ItemStack[]{new ItemStack(Items.APPLE)},new ItemStack[]{new ItemStack(Items.DIAMOND,4)},null,null,null,null,16,16,0);
        recipe.mOutputs[0].getOrCreateTag().putBoolean("pending_work_test",true);
        // Distinguish this operation from tagged drops left by previous GameTest world runs.
        recipe.mOutputs[0].getOrCreateTag().putUUID("pending_work_test_run",java.util.UUID.randomUUID());
        var data=machine.saveWithoutMetadata();
        data.putLong("gt.progress",16); data.putLong("gt.max_progress",16);
        data.put("gt.pending_outputs",com.gregtech.gregtech.api.recipe.MachineWorkOutputs.roll(recipe,1,bound->0).save());
        machine.load(data);
        var drops=net.minecraft.world.level.block.Block.getDrops(machine.getBlockState(),helper.getLevel(),absolute,machine);
        helper.assertTrue(drops.size()==1,"machine has one packed block drop");
        var packed=drops.get(0).getTagElement("BlockEntityTag");
        helper.assertTrue(packed!=null && packed.contains("gt.pending_outputs") && !packed.contains("gt.inventory"),"work travels with block, separately spilled inventory is excluded");
        helper.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR);
        var restoredPos=new net.minecraft.core.BlockPos(3,2,1); helper.setBlock(restoredPos,block);
        absolute=helper.absolutePos(restoredPos);
        machine=(com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity)helper.getLevel().getBlockEntity(absolute);
        machine.load(packed);
        helper.assertTrue(machine.inventory().getStackInSlot(0).isEmpty(),"restored block does not copy spilled inventory");
        com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity.serverTick(helper.getLevel(),absolute,machine.getBlockState(),machine);
        int diamonds=0;
        for(int i=machine.inputSlots();i<machine.inventory().getSlots();i++) if(machine.inventory().getStackInSlot(i).is(Items.DIAMOND)) diamonds+=machine.inventory().getStackInSlot(i).getCount();
        diamonds+=helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(absolute).inflate(2)).stream()
                .map(net.minecraft.world.entity.item.ItemEntity::getItem).filter(stack->ItemStack.isSameItemSameTags(stack,recipe.mOutputs[0])).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(diamonds==4 && !machine.saveWithoutMetadata().contains("gt.pending_outputs"),"completed restored job delivers its output once without another energy packet: diamonds="+diamonds+", state="+machine.saveWithoutMetadata());
        helper.succeed();
    }

}
