package gregtech.compat.jade;

import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.MultiblockPortBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.engine.multiblock.MultiblockLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import snownee.jade.api.BlockAccessor;

@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class RecipeMachineJadeRegressionTests {
    private RecipeMachineJadeRegressionTests() {}

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void recipeFamiliesShareCompleteSnapshotsWithoutLegacyRows(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (String family : new String[]{"crusher", "replicator", "massfab", "printer", "electrolyzer", "centrifuge", "distillationtower"}) {
            var block = machineBlock(family);
            helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
            var machine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(pos);
            var inventory = machine.inventory();
            for (int i = 0; i < inventory.getSlots(); i++) inventory.setStackInSlot(i, new ItemStack(Items.IRON_INGOT, 32));
            for (var tank : machine.getTanksInput()) tank.setFluid(new FluidStack(Fluids.WATER, 1000));
            for (var tank : machine.getTanksOutput()) tank.setFluid(new FluidStack(Fluids.LAVA, 500));
            var saved = machine.saveWithoutMetadata();
            saved.putLong("gt.progress", 128);
            saved.putLong("gt.max_progress", 1024);
            saved.putLong("gt.min_energy", 64);
            saved.putInt("gt.parallel", 2);
            var pending = new CompoundTag();
            var items = new ListTag();
            items.add(new ItemStack(Items.DIAMOND, 8).save(new CompoundTag()));
            pending.put("items", items);
            var fluids = new ListTag();
            fluids.add(new FluidStack(Fluids.WATER, 250).writeToNBT(new CompoundTag()));
            pending.put("fluids", fluids);
            saved.put("gt.pending_outputs", pending);
            machine.load(saved);
            var before = machine.saveWithoutMetadata();
            var data = inspect(machine);
            var snapshot = data.getCompound(BasicMachineJadeProvider.DATA_KEY);
            helper.assertTrue(snapshot.getString("unit").equals(machine.spec().energyType()), family + " must retain its own energy type");
            helper.assertTrue(snapshot.getLong("work") == before.getLong("gt.max_progress")
                    && snapshot.getLong("progress") == before.getLong("gt.progress"), family + " must show real work");
            // Loading clamps/re-derives costs and parallelism using each machine's own rules.
            helper.assertTrue(snapshot.getInt("parallel") == before.getInt("gt.parallel")
                    && snapshot.getLong("recipe") == before.getLong("gt.min_energy"), family + " must show actual recipe power/parallel");
            helper.assertTrue(snapshot.getList("pendingItems", 10).size() == 1 && snapshot.getList("pendingFluids", 10).size() == 1,
                    family + " must show rolled outputs before output slots receive them");
            helper.assertTrue(snapshot.getList("inputItems", 10).size() == machine.inputSlots(), family + " input grouping");
            helper.assertTrue(snapshot.getList("outputItems", 10).size() == machine.outputSlots(), family + " output grouping");
            helper.assertTrue(snapshot.getList("inputFluids", 10).size() == machine.getTanksInput().length, family + " input fluids");
            helper.assertTrue(snapshot.getList("outputFluids", 10).size() == machine.getTanksOutput().length, family + " output fluids");
            helper.assertTrue(data.sizeInBytes() < 12_288, family + " must leave space in Jade's 16 KiB packet: " + data.sizeInBytes());
            helper.assertTrue(data.getAllKeys().stream().noneMatch(key -> key.startsWith("gt_bm")), "Old machine payload must be removed");
            var omni = new CompoundTag();
            OmniJadeProvider.INSTANCE.appendServerData(omni, accessor(machine, omni));
            helper.assertTrue(!omni.getBoolean("gt_omni_inventory"), family + " must not have a second inventory renderer");
            helper.assertTrue(machine.saveWithoutMetadata().equals(before), "Reading must not tick, consume or complete a recipe");
        }
        helper.succeed();
    }

    @GameTest(template = "centrifuge_empty", timeoutTicks = 100)
    public static void boundPartsUseTheSameMachineSnapshot(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, machineBlock("distillationtower").defaultBlockState());
        var machine = (BasicMachineBlockEntity) helper.getLevel().getBlockEntity(pos);
        var wall = BuiltInRegistries.BLOCK.stream().filter(MultiblockPortBlock.class::isInstance).findFirst().orElseThrow();
        helper.getLevel().setBlockAndUpdate(pos.east(), wall.defaultBlockState());
        var part = (MultiblockPortBlockEntity) helper.getLevel().getBlockEntity(pos.east());
        helper.assertTrue(inspect(part).isEmpty(), "Unbound walls must not invent a machine snapshot");
        part.bind(pos, MultiblockLayout.Role.ITEM_IO);
        helper.assertTrue(BasicMachineJadeProvider.machine(part) == machine, "Part must resolve its cached controller");
        helper.assertTrue(inspect(part).equals(inspect(machine)), "Controller and bound part must use exactly the same snapshot");
        helper.assertTrue(!MultiblockJadeProvider.inspect(machine).contains("progress"), "Structure provider must not duplicate recipe progress");
        helper.succeed();
    }

    private static BasicMachineBlock machineBlock(String family) {
        return BuiltInRegistries.BLOCK.stream().filter(BasicMachineBlock.class::isInstance).map(BasicMachineBlock.class::cast)
                .filter(block -> block.basicSpec().machineName().equals(family)).findFirst().orElseThrow();
    }

    private static CompoundTag inspect(BlockEntity entity) {
        var data = new CompoundTag();
        BasicMachineJadeProvider.INSTANCE.appendServerData(data, accessor(entity, data));
        return data;
    }

    private static BlockAccessor accessor(BlockEntity entity, CompoundTag data) {
        return (BlockAccessor) java.lang.reflect.Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(),
                new Class<?>[]{BlockAccessor.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getBlockEntity" -> entity;
                    case "getBlock" -> entity.getBlockState().getBlock();
                    case "getBlockState" -> entity.getBlockState();
                    case "getLevel" -> entity.getLevel();
                    case "getPosition" -> entity.getBlockPos();
                    case "getServerData" -> data;
                    case "getHitResult" -> new net.minecraft.world.phys.BlockHitResult(
                            net.minecraft.world.phys.Vec3.atCenterOf(entity.getBlockPos()), net.minecraft.core.Direction.NORTH,
                            entity.getBlockPos(), false);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
