package gregtech.gametest;

import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.blockentity.machine.BasicMachineBlockEntity;
import gregtech.content.blockentity.machine.LargeRecipeMachineBlockEntity;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.content.data.MachineRecipeMaps;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.internal.multiblock.tracking.StructureTracking;
import gregtech.internal.multiblock.tracking.WorldStructureClaims;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.lang.reflect.*;
import java.util.*;

/** Desired behavior regression: unpatched Desh fails EARLY after observed real machine refusal. */
@GameTestHolder("gt6ce_decomposition_capacity")
@PrefixGameTestTemplate(false)
public final class DecompositionCapacityNativeTests {
    @GameTest(template="test_capacity_empty", batch="decomposition_capacity_native", timeoutTicks=7000)
    public static void smallDeshCompletes(GameTestHelper h) { launch(h,false,true); }
    @GameTest(template="test_capacity_empty", batch="decomposition_capacity_native", timeoutTicks=7000)
    public static void smallUltimetControlCompletes(GameTestHelper h) { launch(h,false,false); }
    @GameTest(template="test_capacity_empty", batch="decomposition_capacity_native", timeoutTicks=7000)
    public static void largeDeshCompletes(GameTestHelper h) { launch(h,true,true); }
    @GameTest(template="test_capacity_empty", batch="decomposition_capacity_native", timeoutTicks=7000)
    public static void largeUltimetControlCompletes(GameTestHelper h) { launch(h,true,false); }

    private static void launch(GameTestHelper h, boolean large, boolean desh) {
        Session session=new Session(h,large,desh);
        try { session.attachCleanup(); session.place(); session.later(); }
        catch(RuntimeException|Error e) { session.cleanup(); throw e; }
    }

    private static final class Session {
        final GameTestHelper h; final boolean large,desh; final BlockPos origin;
        final List<BlockPos> placed=new ArrayList<>();
        final Map<BlockPos,net.minecraft.world.level.block.Block> ownedBlocks=new HashMap<>();
        final Map<BlockPos,net.minecraft.world.level.block.entity.BlockEntity> ownedEntities=new HashMap<>();
        final Map<RecipeMap,List<String>> snapshots=new IdentityHashMap<>();
        final Map<RecipeMap,IdentityHashMap<Recipe,Integer>> identities=new IdentityHashMap<>();
        BasicMachineBlockEntity machine; MultiblockPortBlockEntity energyPort;
        Recipe recipe; long startTime,workStart,packet,accepted; boolean prepared,started,closed;
        Session(GameTestHelper h,boolean large,boolean desh) {
            this.h=h;this.large=large;this.desh=desh;origin=h.absolutePos(new BlockPos(4,3,3));
        }
        void attachCleanup() {
            try {
                GameTestInfo info=null;
                for(Field f:GameTestHelper.class.getDeclaredFields()) if(f.getType()==GameTestInfo.class) {
                    f.setAccessible(true);info=(GameTestInfo)f.get(h);break;
                }
                if(info==null)throw new IllegalStateException("GameTestInfo cleanup listener unavailable");
                info.addListener(new GameTestListener() {
                    public void testStructureLoaded(GameTestInfo t) {}
                    public void testPassed(GameTestInfo t) { cleanup(); }
                    public void testFailed(GameTestInfo t) { cleanup(); }
                });
            } catch(ReflectiveOperationException e) {throw new AssertionError(e);}
        }
        void place() {
            // Capture the actual startup profile, including legitimate addon rows. The default
            // 784/168 whole-table baseline belongs to the separate strict exporter/diff gate.
            for(RecipeMap map:List.of(MachineRecipeMaps.Centrifuge,MachineRecipeMaps.Electrolyzer)) {
                var rows=snapshot(map);snapshots.put(map,rows);identities.put(map,identityCounts(map));
                System.out.println("[capacity-native-profile] map="+map.mNameLocal+" count="+rows.size()
                        +" inProcessContentSha256="+digest(rows)+" uniqueRowObjects="+identities.get(map).size());
            }
            String id=large?"largecentrifuge_tungsten_steel":"centrifuge_steel";
            var block=BuiltInRegistries.BLOCK.get(new ResourceLocation("gt6ce",id));
            h.assertTrue(block instanceof BasicMachineBlock,"exact registered machine exists: "+id);
            put(origin,block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,Direction.NORTH));
            machine=(BasicMachineBlockEntity)h.getLevel().getBlockEntity(origin);
            h.assertTrue(machine!=null && machine.spec().energyType().equals("RU"),"real centrifuge RU spec");
            if(large) {
                h.assertTrue(machine instanceof LargeRecipeMachineBlockEntity,"real large controller");
                for(var cell:LargeMachineLayouts.cells("largecentrifuge"))put(cell.at(origin,Direction.NORTH),cell.block().defaultBlockState());
                // Attach only. Natural server scheduler performs evaluation and claims; never flush/tick directly.
                StructureTracking.attach((LargeRecipeMachineBlockEntity)machine);
            } else {
                // Real automatic output ejects into air. Inert neighboring blocks retain products
                // without disabling automatic I/O or altering inventory/energy semantics.
                for(Direction side:Direction.values())put(origin.relative(side),Blocks.STONE.defaultBlockState());
            }
            startTime=h.getLevel().getGameTime();
        }
        void put(BlockPos pos,net.minecraft.world.level.block.state.BlockState state) {
            h.assertTrue(h.getLevel().isEmptyBlock(pos),"fixture cell initially air: "+pos);
            BlockPos key=pos.immutable();placed.add(key);
            try {h.getLevel().setBlock(pos,state,3);}
            finally {
                // Capture actual owned instances even if initialization fails after placement.
                if(h.getLevel().getBlockState(pos).getBlock()==state.getBlock()) {
                    ownedBlocks.put(key,state.getBlock());
                    var entity=h.getLevel().getBlockEntity(pos);ownedEntities.put(key,entity);
                    if(pos.equals(origin)&&entity instanceof BasicMachineBlockEntity own)machine=own;
                }
            }
        }
        boolean formed() {
            if(!large)return true;
            if(!StructureTracking.query((LargeRecipeMachineBlockEntity)machine))return false;
            for(var cell:LargeMachineLayouts.cells("largecentrifuge")) {
                BlockPos p=cell.at(origin,Direction.NORTH);
                h.assertTrue(origin.equals(WorldStructureClaims.owner(h.getLevel(),p)),"formal claim "+p);
                h.assertTrue(h.getLevel().getBlockEntity(p) instanceof MultiblockPortBlockEntity part && part.isBoundTo(origin),"bound native part "+p);
            }
            energyPort=(MultiblockPortBlockEntity)h.getLevel().getBlockEntity(origin.south());
            return true;
        }
        void prepare() {
            String fluid="gt6ce:molten_"+(desh?"desh":"ultimet");
            List<Recipe> matches=new ArrayList<>();
            for(Recipe r:MachineRecipeMaps.Centrifuge.mRecipeList)
                for(FluidStack f:r.mFluidInputs)if(!f.isEmpty()&&BuiltInRegistries.FLUID.getKey(f.getFluid()).toString().equals(fluid)){matches.add(r);break;}
            h.assertTrue(matches.size()==1,"unique full-table fluid identity: "+fluid);recipe=matches.get(0);
            h.assertTrue(recipe.mInputs.length==0&&recipe.mFluidInputs.length==1&&recipe.mFluidInputs[0].getAmount()==1296,"actual pure-fluid input1296");
            h.assertTrue(recipe.mFluidOutputs.length==0&&recipe.mEUt==126&&recipe.mDuration==2628,"actual unchanged power/time and no fluid outputs");
            h.assertTrue(recipe.mEnabled&&recipe.mCanBeBuffered&&!recipe.mHidden&&!recipe.mFakeRecipe
                    &&!recipe.mNeedsEmptyOutput&&!recipe.mMaterialRecovery&&!recipe.mRequiresEmptyContainerInputs
                    &&!recipe.mExactItemInputs&&!recipe.mExplicitCatalystsOnly
                    &&recipe.mSpecialItems==null&&recipe.mSpecialValue==0,"complete actual row flags and special admission");
            h.assertTrue(recipe.mFluidInputs[0].getTag()==null&&recipe.explicitCatalystInputs().length==0
                    &&recipe.explicitCatalystFluidInputs().length==0&&recipe.explicitPresenceFluidInputs().length==0,
                    "no input NBT, explicit catalyst or presence-gate substitution");
            h.assertTrue(recipe.mChances.length==recipe.mOutputs.length&&recipe.mMaxChances.length==recipe.mOutputs.length,
                    "raw chance arrays match actual outputs");
            for(int i=0;i<recipe.mOutputs.length;i++)h.assertTrue(recipe.mChances[i]==10000&&recipe.mMaxChances[i]==10000,
                    "raw actual guaranteed/default max chance "+i);
            String[] names=desh?new String[]{"boron","lanthanium","neodymium","niobium","cobalt","cerium","lithium"}:new String[]{"cobalt","nickel","chromium","molybdenum"};
            int[] amounts=desh?new int[]{2,2,1,1,1,1,1}:new int[]{5,1,2,1};
            h.assertTrue(desh?(recipe.mOutputs.length==6||recipe.mOutputs.length==7):recipe.mOutputs.length==4,"actual baseline/fixed shape, not fabricated row");
            for(int i=0;i<recipe.mOutputs.length;i++) {
                ItemStack s=recipe.mOutputs[i];
                h.assertTrue(BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals("gt6ce:dust_"+names[i])&&s.getCount()==amounts[i]&&s.getTag()==null,"ordered actual output "+i);
                h.assertTrue(recipe.getOutputChance(i)==10000,"guaranteed output "+i);
            }
            h.assertTrue(machine.inputSlots()==1&&machine.outputSlots()==6&&machine.inventory().getSlots()==7,"real six-slot output inventory");
            for(int i=0;i<7;i++)h.assertTrue(machine.inventory().getStackInSlot(i).isEmpty(),"initial empty inventory");
            h.assertTrue(machine.getTanksInput().length==1&&machine.getTanksInput()[0].isEmpty(),"initial empty input tank");
            int filled=machine.getTanksInput()[0].fill(recipe.mFluidInputs[0].copy(),IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(filled==1296,"real input tank accepted exactly one batch");machine.inventoryChanged();
            Recipe found=MachineRecipeMaps.Centrifuge.findRecipe(List.of(ItemStack.EMPTY),List.of(machine.getTanksInput()[0].getFluid()),false,1,6,h.getLevel(),machine,ItemStack.EMPTY);
            h.assertTrue(found==recipe,"real contextual lookup returns exact unique actual row");
            packet=machine.getEnergySizeInputMax(machine.spec().energyTag(),null);
            h.assertTrue(packet>=126&&packet>=machine.getEnergySizeInputMin(machine.spec().energyTag(),null),"correct legal packet range");
            if(large)h.assertTrue(energyPort.isEnergyAcceptingFrom(machine.spec().energyTag(),Direction.DOWN,false),"formed real energy port accepts RU");
            else h.assertTrue(machine.isEnergyAcceptingFrom(machine.spec().energyTag(),null,false),"real controller unsided energy API accepts RU");
            var cost=gregtech.internal.recipe.MachineWorkCost.calculate(recipe.mEUt,recipe.mDuration,1,
                    machine.spec().parallelScalesDuration(),machine.spec().processingEfficiency(),
                    machine.getEnergySizeInputMin(machine.spec().energyTag(),null),packet,machine.spec().cheapOverclocking());
            h.assertTrue(cost!=null && cost.minimumPower()<=packet,"real work cost admits this energy level");
            h.assertTrue((cost.totalWork()+packet-1)/packet+100<6000,"real unshortened work fits admitted deadline");
            workStart=h.getLevel().getGameTime();prepared=true;
            System.out.println("[capacity-native-admission] large="+large+" desh="+desh+" duration="+recipe.mDuration+" native_recipe_power="+recipe.mEUt+" packet="+packet+" outputs="+recipe.mOutputs.length+" outputFit="+fit());
        }
        int fit() {
            try {Method m=BasicMachineBlockEntity.class.getDeclaredMethod("outputLimitedParallel",Recipe.class,int.class);m.setAccessible(true);return (int)m.invoke(machine,recipe,1);}
            catch(ReflectiveOperationException e){throw new AssertionError(e);}
        }
        void later() {h.runAfterDelay(1,()->{if(closed)return;try{step();}catch(RuntimeException|Error e){cleanup();throw e;}});}
        void step() {
            long now=h.getLevel().getGameTime();
            if(!prepared) {
                h.assertTrue(now-startTime<=500,"natural scheduler formation deadline");
                if(!formed()){later();return;}prepare();
            }
            h.assertTrue(formed(),"formation and formal claims retained throughout work");
            long in=machine.getTanksInput()[0].getAmount();
            if(in==0&&!started) {
                started=true;
                h.assertTrue(machine.machineControl(null).progressMax()>0,"consumed input has a real pending job");
                h.assertTrue(!desh||recipe.mOutputs.length==6,"Desh must use reference map-sized prefix");
                System.out.println("[capacity-native-started] large="+large+" desh="+desh+" tick="+(now-workStart));
            }
            if(started&&machine.machineControl(null).progressMax()==0) {
                verifyOutputs();
                h.assertTrue(accepted>0,"actual energy accepted");
                System.out.println("[capacity-native-completed] large="+large+" desh="+desh+" elapsed="+(now-workStart)+" acceptedPackets="+accepted+" packet="+packet+" unchangedDuration=2628");
                cleanup();h.succeed();return;
            }
            if(!started&&now-workStart>=40) {
                h.assertTrue(in==1296,"refused batch retains input");
                h.assertTrue(machine.machineControl(null).progressMax()==0,"refused batch has no pending job");
                h.assertTrue(accepted>=20,"refusal observed with accepted real energy for many natural ticks");
                for(int i=1;i<7;i++)h.assertTrue(machine.inventory().getStackInSlot(i).isEmpty(),"refusal leaves output empty");
                int limit=fit();
                h.assertTrue(desh && recipe.mOutputs.length==7 && limit==0,
                        "unexpected refusal, not proven capacity-only: outputFit="+limit+" desh="+desh);
                h.fail("CAPACITY_START_REFUSED large="+large+" desh="+desh+" actualOutputFit="+limit+" actualOutputs="+recipe.mOutputs.length+" acceptedPackets="+accepted+" inputRetained="+in+" expected actual recipe to start and complete");
                return;
            }
            h.assertTrue(now-workStart<6000,"real unshortened recipe completion deadline");
            long simulated=inject(false),actual=inject(true);
            h.assertTrue(simulated==1&&actual==1,"one legal packet accepted by actual energy interface each natural tick");accepted+=actual;
            later();
        }
        long inject(boolean execute) {return large?energyPort.doInject(machine.spec().energyTag(),Direction.DOWN,packet,1,execute):machine.doInject(machine.spec().energyTag(),null,packet,1,execute);}
        void verifyOutputs() {
            Map<String,Integer> expected=new TreeMap<>(),actual=new TreeMap<>();
            for(ItemStack s:recipe.mOutputs)expected.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),s.getCount(),Integer::sum);
            for(int i=1;i<7;i++){ItemStack s=machine.inventory().getStackInSlot(i);if(!s.isEmpty()){h.assertTrue(s.getTag()==null,"no unexpected output NBT");actual.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),s.getCount(),Integer::sum);}}
            h.assertTrue(actual.equals(expected),"exact completed outputs expected="+expected+" actual="+actual);
            h.assertTrue(machine.getTanksInput()[0].getAmount()==0&&machine.inventory().getStackInSlot(0).isEmpty(),"one batch consumed, no invented inputs");
            for(var t:machine.getTanksOutput())h.assertTrue(t.isEmpty(),"no fluid coproduct");
            if(desh)h.assertTrue(!actual.containsKey("gt6ce:dust_lithium"),"documented original truncated tail, not a hidden seventh output");
        }
        void cleanup() {
            if(closed)return;closed=true;List<Throwable> errors=new ArrayList<>();
            // Measurements are finished. onRemove calls dropContents: empty only this session's
            // actual still-installed controller, never a replacement BE or detached stale object.
            if(machine!=null&&h.getLevel().getBlockEntity(origin)==machine) {
                var inventory=machine.inventory();
                if(inventory!=null)for(int i=0;i<inventory.getSlots();i++)try{inventory.setStackInSlot(i,ItemStack.EMPTY);}catch(Throwable e){errors.add(e);}
                var inputs=machine.getTanksInput();
                if(inputs!=null)for(var tank:inputs)if(tank!=null)try{tank.setFluid(FluidStack.EMPTY);}catch(Throwable e){errors.add(e);}
                var outputs=machine.getTanksOutput();
                if(outputs!=null)for(var tank:outputs)if(tank!=null)try{tank.setFluid(FluidStack.EMPTY);}catch(Throwable e){errors.add(e);}
            }
            // Controller first releases ownership; attempt EVERY owned cell even after a failure.
            // Changed block/entity ownership is a diagnostic, not permission to destroy a replacement.
            for(BlockPos pos:placed)try {
                if(!ownedBlocks.containsKey(pos))continue;
                h.assertTrue(h.getLevel().getBlockState(pos).getBlock()==ownedBlocks.get(pos)
                        &&h.getLevel().getBlockEntity(pos)==ownedEntities.get(pos),"cleanup ownership changed "+pos);
                h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            }catch(Throwable e){errors.add(e);}
            for(BlockPos pos:placed)try{h.assertTrue(h.getLevel().isEmptyBlock(pos),"fixture removed "+pos);h.assertTrue(WorldStructureClaims.owner(h.getLevel(),pos)==null,"claim released "+pos);}catch(Throwable e){errors.add(e);}
            try{h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(origin).inflate(3)).isEmpty(),"cleanup leaves no fixture ItemEntity drops");}catch(Throwable e){errors.add(e);}
            for(var e:snapshots.entrySet())try {
                var after=snapshot(e.getKey());
                h.assertTrue(e.getValue().equals(after),"full table content/count untouched: "+e.getKey().mNameLocal);
                var oldIds=identities.get(e.getKey());var newIds=identityCounts(e.getKey());
                h.assertTrue(oldIds.size()==newIds.size(),"row identity cardinality unchanged");
                for(var id:oldIds.entrySet())h.assertTrue(id.getValue().equals(newIds.get(id.getKey())),"exact row object multiplicity unchanged");
                h.assertTrue(digest(e.getValue()).equals(digest(after)),"in-process content digest unchanged");
                System.out.println("[capacity-native-profile-unchanged] map="+e.getKey().mNameLocal+" count="+after.size()+" inProcessContentSha256="+digest(after));
            }catch(Throwable t){errors.add(t);}
            if(!errors.isEmpty()){AssertionError failure=new AssertionError("capacity fixture cleanup/table assertions failed: "+errors.size());for(Throwable t:errors)failure.addSuppressed(t);throw failure;}
        }
    }
    // In-process read-only mutation sentinel. Full cross-process codec fingerprints remain exporter responsibility.
    private static List<String> snapshot(RecipeMap map) {
        List<String> rows=new ArrayList<>();
        for(Recipe r:map.mRecipeList) {
            StringBuilder b=new StringBuilder(r.getClass().getName());
            Field[] fields=Recipe.class.getFields();Arrays.sort(fields,Comparator.comparing(Field::getName));
            for(Field f:fields)if(!Modifier.isStatic(f.getModifiers()))try{b.append('|').append(f.getName()).append('=').append(value(f.get(r)));}catch(IllegalAccessException e){throw new AssertionError(e);}
            b.append(Arrays.toString(r.explicitCatalystInputs())).append(Arrays.toString(r.explicitCatalystFluidInputs())).append(Arrays.toString(r.explicitPresenceFluidInputs()));rows.add(b.toString());
        }
        Collections.sort(rows);return rows; // retain duplicates, ignore backing iteration order
    }
    private static IdentityHashMap<Recipe,Integer> identityCounts(RecipeMap map) {
        IdentityHashMap<Recipe,Integer> counts=new IdentityHashMap<>();
        for(Recipe recipe:map.mRecipeList)counts.merge(recipe,1,Integer::sum);
        return counts;
    }
    private static String digest(List<String> rows) {
        try {
            var hash=java.security.MessageDigest.getInstance("SHA-256");
            for(String row:rows) {
                byte[] bytes=row.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                hash.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());hash.update(bytes);
            }
            return java.util.HexFormat.of().formatHex(hash.digest());
        } catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static String value(Object x) {
        if(x==null)return "null";
        if(x instanceof ItemStack s)return s.getCount()+":"+s.save(new CompoundTag());
        if(x instanceof FluidStack s)return s.getAmount()+":"+s.writeToNBT(new CompoundTag());
        if(x.getClass().isArray()){StringBuilder b=new StringBuilder("[");for(int i=0;i<Array.getLength(x);i++)b.append(value(Array.get(x,i))).append(';');return b.append(']').toString();}
        return x.getClass().getName()+":"+x;
    }
}
