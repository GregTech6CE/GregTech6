package gregtech.gametest;

import gregtech.api.addon.platform.MachineBehaviorContext;
import gregtech.api.addon.platform.PartContext;
import gregtech.content.blockentity.machine.BehaviorMachineBlockEntity;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Public ownership contracts against the real sample machine and a legacy part provider. */
@GameTestHolder("gt6ce_api_contracts")
@PrefixGameTestTemplate(false)
public final class AddonInventoryAccessTests {
    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void snapshotAccessPreservesMachineStorage(GameTestHelper helper) throws ReflectiveOperationException {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("addon_sample:irrigator"));
        helper.assertTrue(block != net.minecraft.world.level.block.Blocks.AIR,
                "run this namespace with -PaddonSample=true");
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, block);
        var host = (BehaviorMachineBlockEntity) helper.getBlockEntity(pos);
        // Inspect the same private context passed to actual addon callbacks; no production test hook.
        var field = BehaviorMachineBlockEntity.class.getDeclaredField("context");
        field.setAccessible(true);
        var context = (MachineBehaviorContext) field.get(host);
        var inventory = context.inventoryAccess();
        ItemStack argument = new ItemStack(Items.BONE_MEAL, 8);
        inventory.set(0, argument);
        argument.shrink(3);
        inventory.snapshot(0).shrink(4);
        helper.assertTrue(host.inventory().getStackInSlot(0).getCount() == 8, "writes and reads isolate caller stacks");
        helper.assertTrue(inventory.extract(0, 3, true).getCount() == 3
                && inventory.snapshot(0).getCount() == 8, "simulated extraction does not consume");
        helper.assertTrue(inventory.extract(0, 3, false).getCount() == 3
                && inventory.snapshot(0).getCount() == 5, "executed extraction changes the host once");

        var tanks = context.fluidAccess();
        FluidStack water = new FluidStack(Fluids.WATER, 400);
        helper.assertTrue(tanks.fill(0, water, true) == 400 && tanks.snapshot(0).isEmpty(), "simulated fill is dry");
        helper.assertTrue(tanks.fill(0, water, false) == 400, "real fill");
        water.setAmount(1);
        tanks.snapshot(0).setAmount(2);
        helper.assertTrue(tanks.drain(0, 100, true).getAmount() == 100
                && tanks.snapshot(0).getAmount() == 400, "fluid snapshots and simulated drain do not write");
        helper.assertTrue(tanks.drain(0, 100, false).getAmount() == 100
                && tanks.snapshot(0).getAmount() == 300, "actual drain changes the real tank");
        expect(IndexOutOfBoundsException.class, () -> inventory.extract(-1, 0, false));
        expect(IndexOutOfBoundsException.class, () -> tanks.fill(tanks.tanks(), null, true));
        expect(IllegalArgumentException.class, () -> inventory.extract(0, -1, false));
        expect(IllegalArgumentException.class, () -> tanks.drain(0, -1, false));
        helper.assertTrue(inventory.snapshot(0).getCount() == 5 && tanks.snapshot(0).getAmount() == 300,
                "invalid requests leave real storage unchanged");
        host.state().setItem("ownership", new ItemStack(Items.DIAMOND, 2));
        host.state().item("ownership").shrink(1);
        helper.assertTrue(host.state().item("ownership").getCount() == 2, "state already follows snapshot ownership");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void partAdapterIsolatesLegacyReferences(GameTestHelper helper) {
        ItemStack[] slot = {new ItemStack(Items.STICK, 4)};
        FluidStack[] tank = {new FluidStack(Fluids.WATER, 200)};
        int[] writes = {0};
        PartContext part = (PartContext) Proxy.newProxyInstance(PartContext.class.getClassLoader(),
                new Class<?>[]{PartContext.class}, (proxy, method, args) -> {
                    if (method.isDefault()) return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method,
                            args == null ? new Object[0] : args);
                    return switch (method.getName()) {
                        case "slots", "tanks" -> 1;
                        case "item" -> slot[0];
                        case "setItem" -> { slot[0] = (ItemStack) args[1]; writes[0]++; yield null; }
                        case "fluid" -> tank[0];
                        case "tankCapacity" -> 1000;
                        default -> throw new AssertionError("unexpected part operation " + method.getName());
                    };
                });
        var inventory = part.inventoryAccess();
        inventory.snapshot(0).setCount(50);
        part.fluidAccess().snapshot(0).setAmount(50);
        helper.assertTrue(slot[0].getCount() == 4 && tank[0].getAmount() == 200,
                "part adapter copies even a legacy provider's live references");
        inventory.extract(0, 2, true);
        helper.assertTrue(writes[0] == 0, "simulation never invokes setter notifications");
        inventory.extract(0, 2, false);
        helper.assertTrue(writes[0] == 1 && slot[0].getCount() == 2, "explicit write invokes setter once");
        ItemStack input = new ItemStack(Items.STICK, 3);
        inventory.set(0, input);
        input.setCount(6);
        helper.assertTrue(slot[0].getCount() == 3, "new access protects callers of old implementations");
        helper.succeed();
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try { action.run(); }
        catch (RuntimeException failure) {
            if (type.isInstance(failure)) return;
            throw failure;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
