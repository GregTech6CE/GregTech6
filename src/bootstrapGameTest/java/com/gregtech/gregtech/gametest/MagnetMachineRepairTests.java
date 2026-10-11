package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.block.energy.MagnetMachineBlock;
import com.gregtech.gregtech.blockentity.energy.EnergyNodeBlockEntity;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.content.energy.MagnetMachineDefinitions;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTMagnets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class MagnetMachineRepairTests {
    private static EnergyNodeBlock block(String name) {
        var value = ForgeRegistries.BLOCKS.getValue(GregTech.id(name));
        if (!(value instanceof MagnetMachineBlock magnet)) throw new IllegalStateException("Missing " + name);
        return magnet;
    }

    private static EnergyNodeBlockEntity magnet(GameTestHelper h, BlockPos at, String name) {
        return magnet(h, at, name, Direction.NORTH);
    }

    private static EnergyNodeBlockEntity magnet(GameTestHelper h, BlockPos at, String name, Direction facing) {
        h.setBlock(at, block(name).defaultBlockState().setValue(DirectionalBlock.FACING, facing));
        return (EnergyNodeBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(at));
    }

    private static BasicMachineBlockEntity polarizer(GameTestHelper h, BlockPos at) {
        var block = ForgeRegistries.BLOCKS.getValue(GregTech.id("polarizer_galvanized_steel"));
        if (block == null) throw new IllegalStateException("Missing LV polarizer");
        h.setBlock(at, block.defaultBlockState());
        return (BasicMachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(at));
    }

    @GameTest(template = "test_empty")
    public static void originalTenRatingsAndDistinctDecorations(GameTestHelper h) {
        long[] electric = {32, 128, 512, 2048, 8192};
        long[] flux = {128, 512, 2048, 8192, 32768};
        long[] magnetic = {16, 64, 256, 1024, 4096};
        String[] tiers = {"lv", "mv", "hv", "ev", "iv"};
        h.assertTrue(MagnetMachineDefinitions.specifications().size() == 10
                && GTMagnets.all().size() == 10, "ten powered magnets and ten passive material magnets coexist");
        for (int i = 0; i < tiers.length; i++) {
            EnergyNodeSpec eu = block("electromagnet_" + tiers[i]).spec();
            EnergyNodeSpec rf = block("flux_magnet_" + tiers[i]).spec();
            h.assertTrue(eu.kind() == EnergyNodeSpec.Kind.MAGNET
                    && eu.inType() == GregTechTags.Energy.EU
                    && eu.outType() == GregTechTags.Energy.MU
                    && eu.inputRate() == electric[i] && eu.outputRate() == magnetic[i]
                    && eu.capacity() == electric[i] * 2, "GT6 electric magnet grade " + tiers[i]);
            h.assertTrue(rf.kind() == EnergyNodeSpec.Kind.MAGNET
                    && rf.inType() == GregTechTags.Energy.RF
                    && rf.outType() == GregTechTags.Energy.MU
                    && rf.inputRate() == flux[i] && rf.outputRate() == magnetic[i]
                    && rf.capacity() == flux[i] * 2, "GT6 flux magnet grade " + tiers[i]);
            for (String id : new String[]{"electromagnet_" + tiers[i], "flux_magnet_" + tiers[i]}) {
                var recipe = h.getLevel().getRecipeManager().byKey(GregTech.id("magnets/" + id));
                h.assertTrue(recipe.isPresent()
                        && !recipe.orElseThrow().getResultItem(h.getLevel().registryAccess()).isEmpty(),
                        "powered magnet crafting recipe loaded: " + id);
                for (var ingredient : recipe.orElseThrow().getIngredients()) {
                    if (!ingredient.isEmpty()) h.assertTrue(ingredient.getItems().length > 0,
                            "magnet recipe ingredient resolves to at least one item: " + id);
                }
                h.assertTrue(block(id).defaultBlockState().is(net.minecraft.tags.BlockTags.create(GregTech.id("mineable/wrench"))),
                        "original machine housing uses wrench harvest classification: " + id);
            }
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void bipolarFieldReachesBothMachinesAndWastesPower(GameTestHelper h) {
        // GT6 polarizers accept MU only through their top/bottom faces. Mount the
        // bipolar magnet vertically so both poles meet real receiver input faces.
        var magnet = magnet(h, new BlockPos(2, 2, 2), "electromagnet_lv", Direction.UP);
        var positive = polarizer(h, new BlockPos(2, 3, 2));
        var negative = polarizer(h, new BlockPos(2, 1, 2));
        var eu = GregTechTags.Energy.EU;
        var mu = GregTechTags.Energy.MU;
        h.assertTrue(magnet.isEnergyAcceptingFrom(eu, Direction.WEST, false)
                && magnet.isEnergyAcceptingFrom(eu, Direction.NORTH, false)
                && !magnet.isEnergyAcceptingFrom(eu, Direction.UP, false)
                && !magnet.isEnergyAcceptingFrom(eu, Direction.DOWN, false)
                && magnet.isEnergyEmittingTo(mu, Direction.UP, false)
                && magnet.isEnergyEmittingTo(mu, Direction.DOWN, false)
                && !magnet.isEnergyEmittingTo(mu, Direction.WEST, false), "bipolar face contract");
        h.assertTrue(positive.isEnergyAcceptingFrom(mu, Direction.DOWN, false)
                && negative.isEnergyAcceptingFrom(mu, Direction.UP, false),
                "both polarizer faces must accept the magnet's MU poles");
        h.assertTrue(magnet.doEnergyInjection(eu, Direction.UP, 32, 1, true) == 0
                && magnet.doEnergyInjection(eu, Direction.WEST, 32, 1, false) == 1
                && magnet.stored() == 0, "wrong face rejects; simulation has no effect");
        h.assertTrue(magnet.doEnergyInjection(eu, Direction.WEST, 32, 1, true) == 1,
                "rated EU enters capacitor");
        EnergyNodeBlockEntity.serverTick(h.getLevel(), magnet.getBlockPos(), magnet.getBlockState(), magnet);
        h.assertTrue(positive.getEnergyTick() == 16 && negative.getEnergyTick() == 16
                && magnet.stored() == 0,
                "expected +16/-16 MU and zero EU buffer; got upper=" + positive.getEnergyTick()
                        + ", lower=" + negative.getEnergyTick() + ", buffer=" + magnet.stored());
        h.assertTrue(magnet.getBlockState().getValue(MagnetMachineBlock.ACTIVE), "live field uses active texture");
        EnergyNodeBlockEntity.serverTick(h.getLevel(), magnet.getBlockPos(), magnet.getBlockState(), magnet);
        h.assertTrue(magnet.getBlockState().getValue(MagnetMachineBlock.ACTIVE), "source trinary overlay remembers possible power");
        for (int i = 0; i < 63; i++) EnergyNodeBlockEntity.serverTick(h.getLevel(), magnet.getBlockPos(), magnet.getBlockState(), magnet);
        h.assertTrue(!magnet.getBlockState().getValue(MagnetMachineBlock.ACTIVE), "source activity history clears after64 empty ticks");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fluxCapabilityStopModeAndNbt(GameTestHelper h) {
        var magnet = magnet(h, new BlockPos(2, 1, 2), "flux_magnet_lv");
        var rf = GregTechTags.Energy.RF;
        var mu = GregTechTags.Energy.MU;
        h.assertTrue(!magnet.getCapability(ForgeCapabilities.ENERGY, Direction.NORTH).isPresent()
                && magnet.getCapability(ForgeCapabilities.ENERGY, Direction.WEST).isPresent(),
                "FE only on four input faces");
        var cap = magnet.getCapability(ForgeCapabilities.ENERGY, Direction.WEST).resolve().orElseThrow();
        h.assertTrue(cap.receiveEnergy(128, true) == 128 && magnet.stored() == 0,
                "FE simulation does not change magnetic capacitor");
        h.assertTrue(cap.receiveEnergy(128, false) == 128 && magnet.stored() == 128,
                "real FE charges RF input without EU conversion");
        var saved = magnet.saveWithoutMetadata();
        var restored = new EnergyNodeBlockEntity(magnet.getBlockPos(), magnet.getBlockState());
        restored.load(saved);
        h.assertTrue(restored.stored() == 128 && restored.magnetEnabled(), "charge survives save/load");
        h.assertTrue(!magnet.toggleMagnetEnabled() && !magnet.isEnergyAcceptingFrom(rf, Direction.WEST, false)
                && cap.receiveEnergy(128, false) == 0, "stopped magnet rejects cached FE handler");
        h.assertTrue(magnet.machineControl(null).setMode(1) == 1 && magnet.magnetMode() == 1,
                "source selector interface adjusts field limit");
        saved = magnet.saveWithoutMetadata();
        restored.load(saved);
        h.assertTrue(!restored.magnetEnabled() && restored.magnetMode() == 1,
                "on/off and mode persist through NBT");
        EnergyNodeBlockEntity.serverTick(h.getLevel(), magnet.getBlockPos(), magnet.getBlockState(), magnet);
        h.assertTrue(magnet.stored() == 0 && magnet.getEnergyOffered(mu, Direction.NORTH, 16) == 0
                && magnet.doEnergyExtraction(mu, Direction.NORTH, 16, 1, true) == 0,
                "stopped capacitor wastes RF and magnetic output is push-only");
        magnet.toggleMagnetEnabled();
        h.getLevel().setBlock(magnet.getBlockPos(),
                magnet.getBlockState().setValue(DirectionalBlock.FACING, Direction.WEST), 3);
        h.assertTrue(!cap.canReceive() && cap.receiveEnergy(128, false) == 0,
                "previously cached west FE capability cannot bypass a rotated pole");
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fieldModeAlsoScalesWasteEnergy(GameTestHelper h) {
        var low = magnet(h, new BlockPos(1, 1, 1), "electromagnet_lv");
        var half = magnet(h, new BlockPos(3, 1, 1), "electromagnet_lv");
        var eu = GregTechTags.Energy.EU;
        h.assertTrue(low.doEnergyInjection(eu, Direction.WEST, 32, 1, true) == 1
                && half.doEnergyInjection(eu, Direction.WEST, 32, 2, true) == 2,
                "one or two LV packets fit the GT6 64 EU capacitor");
        low.machineControl(null).setMode(15);
        half.machineControl(null).setMode(8);
        h.assertTrue(low.magnetMode() == 15 && half.magnetMode() == 8,
                "field modes change independently");
        EnergyNodeBlockEntity.serverTick(h.getLevel(), low.getBlockPos(), low.getBlockState(), low);
        EnergyNodeBlockEntity.serverTick(h.getLevel(), half.getBlockPos(), half.getBlockState(), half);
        h.assertTrue(low.stored() == 28 && half.stored() == 32,
                "mode 15 wastes ceil(64/16)=4 EU; mode 8 wastes ceil(64*8/16)=32 EU");
        h.assertTrue(!low.getBlockState().getValue(MagnetMachineBlock.ACTIVE),
                "mode 15 also limits MU output below its minimum packet");
        EnergyNodeBlockEntity.serverTick(h.getLevel(), low.getBlockPos(), low.getBlockState(), low);
        h.assertTrue(low.stored() == 24, "mode-scaled idle drain repeats without a receiver");
        h.succeed();
    }
}
