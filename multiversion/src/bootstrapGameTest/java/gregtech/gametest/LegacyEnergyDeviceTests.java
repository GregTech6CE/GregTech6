package gregtech.gametest;

import gregtech.block.energy.MagicFieldAbsorberBlock;
import gregtech.blockentity.GTEnergyBlockEntity;
import gregtech.blockentity.energy.MagicFieldAbsorberBlockEntity;
import gregtech.content.energy.BatteryBoxEnergy;
import gregtech.content.energy.MagicFieldAbsorberRules;
import gregtech.content.energy.PowerCellSpec;
import gregtech.data.GregTechTags;
import gregtech.item.PowerCellItem;
import gregtech.registry.GTLegacyEnergyDevices;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.*;

//? if >=1.21.1 {
/*@net.neoforged.neoforge.gametest.GameTestHolder("gt6ce_legacy_energy")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
@net.minecraftforge.gametest.GameTestHolder("gt6ce_legacy_energy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
public final class LegacyEnergyDeviceTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void fuel_cell_survival_and_empty_shell(GameTestHelper h) {
        for (PowerCellSpec spec : PowerCellSpec.ALL) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id(spec.id())));
            h.assertTrue(stack.getItem() instanceof PowerCellItem, "Actual block item " + spec.id());
            PowerCellItem item = (PowerCellItem) stack.getItem();
            h.assertTrue(item.stored(stack) == spec.capacity(), "Full filled cells and empty shells");
            h.assertTrue(item.doEnergyInjection(GregTechTags.Energy.EU, stack, 1, 100, h.getLevel(), BlockPos.ZERO, true) == 0,
                    "Fuel cells never recharge electrically");
            if (!spec.filled()) continue;
            item.setCharge(stack, spec.voltage() * 2);
            h.assertTrue(item.doEnergyExtraction(GregTechTags.Energy.EU, stack, spec.voltage(), 1, h.getLevel(), BlockPos.ZERO, false) == 1
                    && item.stored(stack) == spec.voltage() * 2, "Simulation retains charge and identity");
            h.assertTrue(item.doEnergyExtraction(GregTechTags.Energy.EU, stack, spec.voltage() + 1, 1, h.getLevel(), BlockPos.ZERO, true) == 0,
                    "Reject overvoltage extraction");
            var inventory = new ArrayList<ItemStack>(); inventory.add(stack);
            var box = new BatteryBoxEnergy(inventory, spec.voltage());
            box.tick(1, h.getLevel(), BlockPos.ZERO);
            h.assertTrue(box.buffer() == spec.voltage() * 2 && inventory.get(0).is(BuiltInRegistries.ITEM.get(id(spec.emptyId()))),
                    "Battery box extracts real packets and immediately replaces exhausted cell");
            var pos = h.absolutePos(new BlockPos(2, 2, 2));
            var block = ((PowerCellItem) new ItemStack(BuiltInRegistries.ITEM.get(id(spec.id()))).getItem()).getBlock();
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var placed = (gregtech.blockentity.energy.PowerCellBlockEntity) h.getLevel().getBlockEntity(pos);
            placed.setCharge(spec.voltage() * 7);
            h.assertTrue(item.stored(placed.asItem()) == spec.voltage() * 7, "Placed cell retains remaining energy on recovery");
        }
        for (var name : List.of("power_cell_empty", "aneutronic_fusion_power_cell_empty", "magic_field_absorber")) {
            var entry = h.getLevel().getRecipeManager().byKey(id("hand/energy/" + name));
            h.assertTrue(entry.isPresent(), "Hand recipe present: " + name);
//? if >=1.21.1 {
            /*var recipe = entry.orElseThrow().value();
*///?} else {
            var recipe = entry.orElseThrow();
//?}
            h.assertTrue(recipe.getIngredients().stream().allMatch(ingredient -> ingredient.getItems().length > 0), "All nine crafting inputs resolve: " + name);
        }
        var input = new ItemStack(BuiltInRegistries.ITEM.get(id("power_cell_empty")));
        h.assertTrue(gregtech.data.MachineRecipeMaps.Canner.mRecipeList.stream()
                .anyMatch(r -> r.mInputs.length > 0 && r.mInputs[0].is(input.getItem()) && r.mFluidInputs.length == 1 && r.mFluidInputs[0].getAmount() == 200_000), "Canner fuel recipe is registered");
        h.assertTrue(BuiltInRegistries.BLOCK.get(id("flower_vindicator_flower")) instanceof gregtech.block.plant.BedrockFlowerBlock,
                "Vindicator is a real indicator flower");
        var empty = new ItemStack(BuiltInRegistries.ITEM.get(id("power_cell_empty")));
        var clean = empty.copy(); ((PowerCellItem) empty.getItem()).setCharge(empty, 0);
//? if >=1.21.1 {
        /*h.assertTrue(ItemStack.isSameItemSameComponents(clean, empty), "Empty shell keeps canonical stackable identity");
*///?} else {
        h.assertTrue(ItemStack.isSameItemSameTags(clean, empty), "Empty shell keeps canonical stackable identity");
//?}
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void absorber_trophies_output_faces_and_switch(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(5, 3, 5));
        for (Direction face : Direction.values()) {
            if (face == Direction.UP) continue;
            for (Direction clear : Direction.values()) level.setBlockAndUpdate(pos.relative(clear), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, GTLegacyEnergyDevices.MAGIC_FIELD_ABSORBER.get().defaultBlockState().setValue(MagicFieldAbsorberBlock.FACING, face));
            var absorber = (MagicFieldAbsorberBlockEntity) level.getBlockEntity(pos);
            level.setBlockAndUpdate(pos.relative(face), Blocks.BARREL.defaultBlockState());
            var receiver = new Receiver(pos.relative(face)); level.setBlockEntity(receiver);
            level.setBlockAndUpdate(pos.above(), Blocks.DRAGON_EGG.defaultBlockState());
            absorber.serverTick();
            h.assertTrue(receiver.type == GregTechTags.Energy.QU && receiver.energy == 64 && receiver.side == face.getOpposite(), "Dragon egg emits 64 QU on " + face);
            h.assertTrue(absorber.getBlockState().getValue(MagicFieldAbsorberBlock.ACTIVE), "Active model follows trophy");
            absorber.setEnabled(false); absorber.serverTick();
            h.assertTrue(receiver.energy == 64 && !absorber.active(), "Stopped produces nothing");
            level.setBlockAndUpdate(pos.above(), Blocks.SKELETON_SKULL.defaultBlockState());
            absorber.setEnabled(true); absorber.serverTick();
            h.assertTrue(receiver.type == GregTechTags.Energy.TU && receiver.energy == 65, "Vanilla skull produces 1 TU");
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState()); absorber.recheck(); absorber.serverTick();
            h.assertTrue(!absorber.active() && receiver.energy == 65, "Removing trophy stops production");
        }
        var naga = MagicFieldAbsorberRules.trophy("twilightforest:naga_trophy", false);
        h.assertTrue(naga.packetSize(0) == -64 && naga.packetSize(64) == 64 && naga.packetSize(128) == -64, "Original alternating KU phase");
        h.assertTrue(MagicFieldAbsorberRules.trophy("minecraft:stone", false) == null, "No arbitrary block energy");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 160)
    public static void kilogibblometer_reads_real_large_boiler_through_shell(GameTestHelper h) {
        // Existing native scenario checks actual assembled boiler, both pressure meters, broken/repaired wall and removed controller.
        SensorSourceTests.original_parts_delegate_sensor_measurements(h);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("gt6ce", path); }
    private static final class Receiver extends GTEnergyBlockEntity {
        long energy; GregTechTags.Tag type; Direction side;
        Receiver(BlockPos pos) { super(BlockEntityType.BARREL, pos, Blocks.BARREL.defaultBlockState()); }
        @Override public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) { return !emitting; }
        @Override public long doEnergyInjection(GregTechTags.Tag type, Direction side, long size, long amount, boolean execute) {
            if (execute) { this.type = type; this.side = side; energy += Math.abs(size) * amount; } return amount;
        }
        @Override public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) { return 64; }
        @Override public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) { return 0; }
        @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) { return 64; }
        @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) { return 0; }
    }
}
