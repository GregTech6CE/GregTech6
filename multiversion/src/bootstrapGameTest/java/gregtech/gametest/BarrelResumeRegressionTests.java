package gregtech.gametest;

import gregtech.api.sensor.*;
import gregtech.internal.sensor.*;
import gregtech.internal.sensor.*;
import gregtech.content.blockentity.sensor.SensorBlockEntity;
import gregtech.api.machine.MachineControl;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.material.*;
import gregtech.content.blockentity.inventory.MassStorageBlockEntity;
import gregtech.content.blockentity.machine.TankBlockEntity;
import gregtech.content.blockentity.energy.ZpmDischargerBlockEntity;
import gregtech.content.material.Materials;
import gregtech.content.energy.ZpmEnergy;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTFluids;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_barrel_resume")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
@net.minecraftforge.gametest.GameTestHolder("gregtech_barrel_resume")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
/** Explicit native host operations; not a claim of server tick-order or cold-JVM persistence parity. */
public final class BarrelResumeRegressionTests {
    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path));
    }
    private static void fixture(GameTestHelper h, String name, java.util.function.Consumer<BlockPos> body) {
        h.runAfterDelay(2, () -> {
            var saved = new LinkedHashMap<BlockPos, BlockState>();
            for (var cursor : BlockPos.betweenClosed(h.absolutePos(new BlockPos(1,1,1)), h.absolutePos(new BlockPos(7,7,7)))) {
                var pos = cursor.immutable();
                h.assertTrue(h.getLevel().getBlockEntity(pos) == null, "owned fixture must not overwrite a block entity " + pos);
                saved.put(pos, h.getLevel().getBlockState(pos));
            }
            var bounds=new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(1,1,1))),net.minecraft.world.phys.Vec3.atLowerCornerOf(h.absolutePos(new BlockPos(8,8,8))));
            var existingItems=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,bounds).stream()
                    .map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            try {
                saved.keySet().forEach(p -> h.getLevel().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));
                body.accept(h.absolutePos(new BlockPos(4,3,4)));
            } finally {
                // Remove tested block entities before restoring the original states.
                saved.keySet().forEach(p -> h.getLevel().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));
                saved.forEach((p,s) -> h.getLevel().setBlockAndUpdate(p,s));
                h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,bounds).stream()
                        .filter(entity -> !existingItems.contains(entity.getUUID())).forEach(net.minecraft.world.entity.Entity::discard);
                for (var entry : saved.entrySet()) h.assertTrue(h.getLevel().getBlockState(entry.getKey()).equals(entry.getValue())
                        && h.getLevel().getBlockEntity(entry.getKey()) == null, "fixture restored " + name);
                System.out.println("BARREL_RESUME_CLEANUP " + name + " cells=" + saved.size());
            }
            h.succeed();
        });
    }
    private static CompoundTag save(GameTestHelper h, BlockEntity be) {
//? if >=1.21.1 {
        /*return be.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
        return be.saveWithoutMetadata();
//?}
    }
    private static void load(GameTestHelper h, BlockEntity be, CompoundTag data) {
//? if >=1.21.1 {
        /*be.loadWithComponents(data,h.getLevel().registryAccess());
*///?} else {
        be.load(data);
//?}
    }
    private static BlockEntity place(GameTestHelper h, BlockPos pos, Block block) {
        h.assertTrue(block!=Blocks.AIR,"actual registered fixture block");
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        var entity=h.getLevel().getBlockEntity(pos);
        h.assertTrue(entity!=null&&entity.getType().isValid(entity.getBlockState()),"valid actual registered block entity");
        return entity;
    }

    private static void ticks(GameTestHelper h,TankBlockEntity tank,int count) {
        for(int i=0;i<count;i++) TankBlockEntity.serverTick(h.getLevel(),tank.getBlockPos(),tank.getBlockState(),tank);
    }
    private static void fill(GameTestHelper h,TankBlockEntity tank,String fluid,int amount) {
        var stack=GTFluids.stack(fluid,amount);
        h.assertTrue(!stack.isEmpty()&&tank.getFluidTank().fill(stack,FluidAction.EXECUTE)==amount,"native fill "+fluid);
    }
    private static TankBlockEntity working(GameTestHelper h,BlockPos pos) {
        var tank=(TankBlockEntity)place(h,pos,block("drum_steel"));
        fill(h,tank,"Juice_Apple",50);tank.toggleSoftHammerState();ticks(h,tank,128);
        h.assertTrue(tank.getSealedTime()==128&&tank.getMaxSealedTime()==1024,"actual native work before serialization");
        return tank;
    }
    private static CompoundTag binaryRoundTrip(CompoundTag saved) {
        try {
            var bytes=new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.NbtIo.write(saved,new java.io.DataOutputStream(bytes));
            var input=new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()));
//? if >=1.21.1 {
            /*return net.minecraft.nbt.NbtIo.read(input,net.minecraft.nbt.NbtAccounter.unlimitedHeap());
*///?} else {
            return net.minecraft.nbt.NbtIo.read(input);
//?}
        } catch(java.io.IOException error) {throw new AssertionError("NBT binary round trip",error);}
    }
    private static TankBlockEntity recreate(GameTestHelper h,TankBlockEntity old,CompoundTag saved) {
        var replacement=(TankBlockEntity)place(h,old.getBlockPos(),old.getBlockState().getBlock());
        h.assertTrue(replacement!=old&&replacement.getType()==old.getType(),"fresh valid BE, not cached same-object load");
        load(h,replacement,binaryRoundTrip(saved));
        return replacement;
    }
    private static void zero(GameTestHelper h,TankBlockEntity tank,String label) {
        h.assertTrue(tank.getSealedTime()==0&&tank.getMaxSealedTime()==0,label+" resets work and maximum");
    }
    private static void resumeAndComplete(GameTestHelper h,TankBlockEntity tank) {
        h.assertTrue(tank.getSoftHammerState()&&tank.getSealedTime()==128,"restored sealed saved work");
        ticks(h,tank,1);
        h.assertTrue(tank.getSealedTime()==129&&tank.getMaxSealedTime()==1024,"first native tick resumes128 to129, not1; current="+tank.getSealedTime());
        ticks(h,tank,895);
        h.assertTrue(tank.getSealedTime()==1024&&tank.getFluidTank().getFluid().getFluid()==GTFluids.stack("Juice_Apple",1).getFluid(),"full work still juice until following conversion tick");
        ticks(h,tank,1);
        var result=tank.getFluidTank().getFluid();
        h.assertTrue(result.getFluid()==GTFluids.stack("Cider_Apple",1).getFluid()&&result.getAmount()==25,"resumed complete conversion50 juice to25 cider");
        zero(h,tank,"completed recipe");
        var recipe=TankBlockEntity.findFermentationRecipe(result);
        h.assertTrue(recipe!=null,"next cider recipe available");
        long duration=TankBlockEntity.sealedDuration(recipe,25);
        ticks(h,tank,1);
        h.assertTrue(tank.getSealedTime()==1&&tank.getMaxSealedTime()==duration,"normal next recipe starts fresh with existing native cadence");
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void serializedSealedBarrelResumesThroughCompletion(GameTestHelper h) {
        fixture(h,"resume_complete",pos->{var tank=working(h,pos);resumeAndComplete(h,recreate(h,tank,save(h,tank)));});
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void savedWorkWithoutCachedMaximumResumes(GameTestHelper h) {
        fixture(h,"resume_without_max",pos->{
            var tank=working(h,pos);var saved=save(h,tank);
            // Original GT6 persists work only. The port accepts absent derived maximum too.
            saved.remove("gt.max_sealed_time");
            tank=recreate(h,tank,saved);
            h.assertTrue(tank.getMaxSealedTime()==0,"absent derived maximum starts unresolved");
            resumeAndComplete(h,tank);
        });
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void invalidEmptyAndGasOutputDoNotResumeSavedWork(GameTestHelper h) {
        fixture(h,"invalid_resume",pos->{
            for(String input:new String[]{"Water","Biomass","Nitrogen",""}) {
                var tank=working(h,pos);tank.getFluidTank().setEmpty();
                if(!input.isEmpty()) fill(h,tank,input,40);
                h.assertTrue(TankBlockEntity.findFermentationRecipe(tank.getFluidTank().getFluid())==null,"invalid/empty/gas fixture has no permitted barrel recipe "+input);
                tank=recreate(h,tank,save(h,tank));ticks(h,tank,1);
                zero(h,tank,"reloaded "+input);
                h.assertTrue(tank.getSoftHammerState(),"invalid recipe does not silently unseal drum");
            }
        });
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void changedFluidAndExplicitResealStillResetWork(GameTestHelper h) {
        fixture(h,"change_reseal",pos->{
            var tank=working(h,pos);tank.getFluidTank().setEmpty();fill(h,tank,"Cider_Apple",25);
            var recipe=TankBlockEntity.findFermentationRecipe(tank.getFluidTank().getFluid());
            h.assertTrue(recipe!=null,"actual changed-fluid recipe available");
            long duration=TankBlockEntity.sealedDuration(recipe,25);
            ticks(h,tank,1);
            h.assertTrue(tank.getSealedTime()==1&&tank.getMaxSealedTime()==duration,"live cached fluid identity change starts fresh");
            tank.toggleSoftHammerState();zero(h,tank,"unseal");
            tank=recreate(h,tank,save(h,tank));ticks(h,tank,128);
            h.assertTrue(!tank.getSoftHammerState(),"unsealed state survives NBT recreation");zero(h,tank,"unsealed ticks");
            tank.toggleSoftHammerState();zero(h,tank,"reseal before work");ticks(h,tank,1);
            h.assertTrue(tank.getSealedTime()==1&&tank.getMaxSealedTime()==duration,"explicit reseal starts new work");
        });
    }
}
