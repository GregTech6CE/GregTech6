package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.item.GTToolItem;
import com.gregtech.gregtech.registry.GTEnergyNodes;
import com.gregtech.gregtech.registry.GTGearboxes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class RotationTransformerRepairTests {
    private static EnergyNodeBlock block(String name) {
        var value = ForgeRegistries.BLOCKS.getValue(GregTech.id(name));
        if (!(value instanceof EnergyNodeBlock node)) throw new IllegalStateException("Missing " + name);
        return node;
    }

    private static EnergyNodeBlockEntity place(GameTestHelper h, BlockPos relative, String name, Direction facing) {
        var b = block(name);
        h.setBlock(relative, b.defaultBlockState().setValue(DirectionalBlock.FACING, facing));
        return (EnergyNodeBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(relative));
    }

    @GameTest(template = "test_empty")
    public static void existingFourRatesAndFrontBackMode(GameTestHelper h) {
        long[] inputs = {32, 128, 512, 2048};
        long[] outputs = {8, 32, 128, 512};
        String[] names = {"bronze", "steel", "titanium", "tungstensteel"};
        h.assertTrue(GTGearboxes.allTransformers().size() == 13,
                "all 13 original rotation transformer grades are registered");
        for (int i = 0; i < names.length; i++) {
            var spec = block("rotation_transformer_" + names[i]).spec();
            h.assertTrue(spec.inputRate() == inputs[i] && spec.outputRate() == outputs[i]
                    && spec.inputRate() == 4 * spec.outputRate(), "GT6 packet ratings: " + names[i]);
        }

        var node = place(h, new BlockPos(1, 1, 1), "rotation_transformer_bronze", Direction.NORTH);
        var ru = GregTechTags.Energy.RU;
        h.assertTrue(node.isRotationTransformer(), "dedicated RU conversion path");
        h.assertTrue(node.isEnergyAcceptingFrom(ru, Direction.NORTH, false)
                && !node.isEnergyAcceptingFrom(ru, Direction.SOUTH, false)
                && !node.isEnergyAcceptingFrom(ru, Direction.WEST, false)
                && node.isEnergyEmittingTo(ru, Direction.SOUTH, false)
                && !node.isEnergyEmittingTo(ru, Direction.NORTH, false), "front input, back output only");
        h.assertTrue(node.doEnergyInjection(ru, Direction.WEST, 32, 1, true) == 0
                && node.doEnergyInjection(ru, Direction.NORTH, 32, 1, true) == 1
                && node.stored() == 32, "wrong faces reject, one front packet stores 32 RU");
        h.assertTrue(node.getEnergyOffered(ru, Direction.SOUTH, 8) == 0
                && node.getEnergyOffered(ru, Direction.SOUTH, 32) == 0
                && node.doEnergyExtraction(ru, Direction.SOUTH, 32, 1, true) == 0
                && node.stored() == 32
                && node.doEnergyExtraction(ru, Direction.NORTH, 8, 4, true) == 0
                && node.doEnergyExtraction(ru, Direction.SOUTH, 8, 4, true) == 0
                && node.stored() == 32, "GT6 converter output is push-only and cannot be pulled");

        node.doEnergyInjection(ru, Direction.NORTH, 32, 1, true);
        h.assertTrue(node.toggleInverted(), "reverse mode enabled");
        h.assertTrue(node.stored() == 0, "GT6 discards buffered RU when reversing the transformer");
        h.assertTrue(node.isEnergyAcceptingFrom(ru, Direction.SOUTH, false)
                && !node.isEnergyAcceptingFrom(ru, Direction.NORTH, false)
                && node.isEnergyEmittingTo(ru, Direction.NORTH, false)
                && !node.isEnergyEmittingTo(ru, Direction.SOUTH, false), "reverse exchanges both energy faces");
        h.assertTrue(node.getEnergySizeInputRecommended(ru, Direction.SOUTH) == 32
                && node.getEnergySizeInputMin(ru, Direction.SOUTH) == 1
                && node.getEnergySizeInputMax(ru, Direction.SOUTH) == 64
                && node.getEnergySizeOutputMin(ru, Direction.NORTH) == 24
                && node.getEnergySizeOutputRecommended(ru, Direction.NORTH) == 32
                && node.doEnergyInjection(ru, Direction.SOUTH, 8, 4, true) == 4
                && node.stored() == 32, "four 8 RU inputs accumulate 32 RU");
        h.assertTrue(node.getEnergyOffered(ru, Direction.NORTH, 32) == 0
                && node.getEnergyOffered(ru, Direction.NORTH, 8) == 0
                && node.doEnergyExtraction(ru, Direction.NORTH, 8, 4, true) == 0
                && node.doEnergyExtraction(ru, Direction.NORTH, 32, 1, true) == 0
                && node.stored() == 32, "reverse mode is also push-only");
        var player = h.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND,
                GTToolItem.create(GTToolType.MONKEY_WRENCH, Materials.Steel, GTMaterialRegistry.get("Wood")));
        var pos = node.getBlockPos();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        block("rotation_transformer_bronze").use(node.getBlockState(), h.getLevel(), pos, player,
                InteractionHand.MAIN_HAND, hit);
        h.assertTrue(node.isEnergyAcceptingFrom(ru, Direction.NORTH, false)
                && node.getBlockState().getValue(DirectionalBlock.FACING) == Direction.NORTH,
                "monkey wrench changes conversion mode without rotating the block");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void realNetworkEmitsFourSmallThenOneLargePacket(GameTestHelper h) {
        var first = place(h, new BlockPos(1, 1, 1), "rotation_transformer_bronze", Direction.NORTH);
        var second = place(h, new BlockPos(1, 1, 2), "rotation_transformer_bronze", Direction.SOUTH);
        var third = place(h, new BlockPos(1, 1, 3), "rotation_transformer_bronze", Direction.NORTH);
        second.toggleInverted();
        var ru = GregTechTags.Energy.RU;
        h.assertTrue(first.doEnergyInjection(ru, Direction.NORTH, -32, 1, true) == 1, "signed RU enters front");
        var level = h.getLevel();
        var firstPos = h.absolutePos(new BlockPos(1, 1, 1));
        EnergyNodeBlockEntity.serverTick(level, firstPos, level.getBlockState(firstPos), first);
        h.assertTrue(first.stored() == 0 && second.stored() == 32
                && second.saveWithoutMetadata().getBoolean("gt.rotation_negative_input"),
                "network sends four negative 8 RU packets from the back");
        var secondPos = h.absolutePos(new BlockPos(1, 1, 2));
        var saved = second.saveWithoutMetadata();
        var reloaded = new EnergyNodeBlockEntity(secondPos, level.getBlockState(secondPos));
        reloaded.load(saved);
        h.assertTrue(reloaded.stored() == 32
                && reloaded.saveWithoutMetadata().getBoolean("gt.rotation_negative_input")
                && reloaded.isEnergyAcceptingFrom(ru, Direction.NORTH, false),
                "saved buffer, reverse mode and signed RU survive NBT load");
        EnergyNodeBlockEntity.serverTick(level, secondPos, level.getBlockState(secondPos), second);
        h.assertTrue(second.stored() == 0 && third.stored() == 32
                && third.saveWithoutMetadata().getBoolean("gt.rotation_negative_input"),
                "reverse sends one negative 32 RU packet from the front");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void disconnectedAndSubthresholdInputIsWasted(GameTestHelper h) {
        var node = place(h, new BlockPos(1, 1, 1), "rotation_transformer_bronze", Direction.NORTH);
        var ru = GregTechTags.Energy.RU;
        var level = h.getLevel();
        var pos = node.getBlockPos();
        h.assertTrue(node.getEnergyCapacity(ru, Direction.NORTH) == 64,
                "GT6 capacitor is twice the original 32 RU input rating");
        h.assertTrue(node.doEnergyInjection(ru, Direction.NORTH, 32, 1, true) == 1,
                "front accepts rated input without a receiver");
        EnergyNodeBlockEntity.serverTick(level, pos, node.getBlockState(), node);
        h.assertTrue(node.stored() == 0,
                "GT6 NBT_WASTE_ENERGY empties buffered RU even when the back has no receiver");

        node.toggleInverted();
        h.assertTrue(node.doEnergyInjection(ru, Direction.SOUTH, 8, 2, true) == 2
                && node.stored() == 16, "reverse mode accepts smaller RU packets");
        EnergyNodeBlockEntity.serverTick(level, pos, node.getBlockState(), node);
        h.assertTrue(node.stored() == 0,
                "reverse converter also wastes 16 RU below its 24 RU minimum output packet");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void capacitorChargeScalesPacketSizeAndLastInputSetsPolarity(GameTestHelper h) {
        var first = place(h, new BlockPos(1, 1, 1), "rotation_transformer_bronze", Direction.NORTH);
        var second = place(h, new BlockPos(1, 1, 2), "rotation_transformer_bronze", Direction.SOUTH);
        second.toggleInverted();
        var ru = GregTechTags.Energy.RU;
        h.assertTrue(first.doEnergyInjection(ru, Direction.NORTH, -32, 1, true) == 1
                && first.doEnergyInjection(ru, Direction.NORTH, 16, 1, true) == 1
                && first.stored() == 48,
                "GT6 stores packet magnitudes and uses the sign of the latest input packet");
        h.assertTrue(first.getEnergyOffered(ru, Direction.SOUTH, 12) == 0
                && first.getEnergyOffered(ru, Direction.SOUTH, -12) == 0
                && first.getEnergyOffered(ru, Direction.SOUTH, 8) == 0,
                "RU output cannot be pulled before the converter ticks");
        EnergyNodeBlockEntity.serverTick(h.getLevel(), first.getBlockPos(), first.getBlockState(), first);
        h.assertTrue(first.stored() == 0 && second.stored() == 48
                && !second.saveWithoutMetadata().getBoolean("gt.rotation_negative_input"),
                "forward tick transfers all 48 RU and drains its input capacitor");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void ordinaryElectricTransformerUsesOriginalBidirectionalRules(GameTestHelper h) {
        var ordinary = place(h, new BlockPos(1, 1, 1), "transformer_ulv_lv", Direction.NORTH);
        var eu = GregTechTags.Energy.EU;
        h.assertTrue(!ordinary.isRotationTransformer() && GTEnergyNodes.all().stream()
                .anyMatch(e -> e.get() == block("transformer_ulv_lv")), "ordinary EU transformer stays generic");
        h.assertTrue(ordinary.isEnergyAcceptingFrom(eu, Direction.NORTH, false)
                && !ordinary.isEnergyAcceptingFrom(eu, Direction.WEST, false)
                && ordinary.isEnergyEmittingTo(eu, Direction.SOUTH, false)
                && !ordinary.isEnergyEmittingTo(eu, Direction.NORTH, false), "GT6 EU transformer receives on front and emits on other sides");
        h.assertTrue(ordinary.doEnergyInjection(eu, Direction.NORTH, 32, 1, true) == 1
                && ordinary.stored() == 32, "ordinary EU buffer works");
        ordinary.toggleInverted();
        h.assertTrue(ordinary.stored() == 0 && ordinary.isEnergyAcceptingFrom(eu, Direction.WEST, false)
                && ordinary.isEnergyEmittingTo(eu, Direction.NORTH, false), "GT6 mode switch clears buffer and reverses faces");
        h.succeed();
    }
}
