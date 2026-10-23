package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.api.fluid.FluidDisplayBinding;
import com.gregtech.gregtech.registry.GTFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Candidate extension: server dispatch checks original display-item survival cancellation and creative fill. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class MachineFluidFrameworkControlTests {
    @GameTest(template="test_empty")
    public static void registeredBathAcceptsDieselDisplayCarrier(GameTestHelper h) {
        var block = BuiltInRegistries.BLOCK.stream()
                .filter(b -> b instanceof BasicMachineBlock basic
                        && basic.basicSpec().machineName().equals("bath") && basic.basicSpec().tier() == 1)
                .findFirst().orElseThrow();
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        try {
            var machine = (BasicMachineBlockEntity) h.getLevel().getBlockEntity(pos);
            var fuel = new FluidStack(GTFluids.still("Diesel").get(), 1000);
//? if >=1.21.1 {
            /*var carrier = FluidDisplayBinding.display(h.getLevel().registryAccess(), fuel);
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
            var carrier = FluidDisplayBinding.display(fuel);
            var player = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "[fluid-control]"));
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.setItemInHand(InteractionHand.MAIN_HAND, carrier);
            h.assertTrue(!carrier.isEmpty(), "real registered fluid carrier");
            h.assertTrue(machine.getTanksInput().length > 0 && machine.getTanksInput()[0].isEmpty(), "fresh bath input");
            var state = h.getLevel().getBlockState(pos);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var before = carrier.copy();
            var survival = player.gameMode.useItemOn(player, h.getLevel(), carrier, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(survival.consumesAction() && machine.getTanksInput()[0].isEmpty(),
                    "source item-first consumes survival display click before permissive bath block callback");
            h.assertTrue(player.getMainHandItem().getItem() == before.getItem()
                    && player.getMainHandItem().getCount() == before.getCount(), "survival no-op retains display carrier");
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            h.assertTrue(player.getMainHandItem().getItem() == before.getItem()
                    && player.getMainHandItem().getCount() == before.getCount(),
                    "creative display carrier is retained, not an emptied survival vessel");
            h.assertTrue(machine.getTanksInput()[0].getAmount() > 0
                    && machine.getTanksInput()[0].getFluid().getFluid() == fuel.getFluid(),
                    "same capability-only carrier pours through server gameMode into registered bath");
            h.succeed();
        } finally {
            h.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }
}
