package gregtech.gametest;
import gregtech.api.material.*;
import gregtech.internal.material.*;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import gregtech.content.data.*;
import gregtech.content.registry.GTItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}
/** ISOLATED NATIVE PROFILE ONLY, one case/fresh world. Do not add this global reload probe to a
 * mixed suite until harness hard-timeout isolation is verified. No server-thread join/get.
 * Ordinary failures are collected, original packs are reloaded, state is checked and files removed
 * BEFORE finishing. If restoration itself fails or never completes, abort this isolated profile;
 * never claim clean state or advance to another shared-world batch.
 */
@GameTestHolder("gregtech_nonore_tag_audit")
@PrefixGameTestTemplate(false)
public final class NonOreTaggedContinuationTests {
    private static boolean started;
    @GameTest(template="test_empty",batch="nonore_external_tag_isolated",timeoutTicks=8000)
    public static void taggedForeignPvcOreInputsKeepContinuationAndDatapackPrecedence(GameTestHelper h)throws Exception{
        String enabled=System.getProperty("neoforge.enabledGameTestNamespaces",System.getProperty("forge.enabledGameTestNamespaces",""));
        h.assertTrue("gregtech_nonore_tag_audit".equals(enabled),"exact isolated namespace required; no combined suite");
        h.assertTrue(!started,"single cold probe per JVM required");started=true;
        String configured=System.getenv("GT6_NONORE_TAG_DIRECTORY");
        h.assertTrue(configured!=null&&!configured.isBlank(),"explicit fresh disposable run directory required");
        Path directory=Path.of(configured).toRealPath();
        Path world=h.getLevel().getServer().getWorldPath(LevelResource.ROOT).toRealPath();
        h.assertTrue(directory.getFileName().toString().startsWith("gt6-nonore-tag-"),"dedicated disposable directory prefix required");
        h.assertTrue(world.startsWith(directory)&&!world.equals(directory),"actual world inside disposable run directory");
        h.assertTrue(h.getLevel().getServer().getPlayerList().getPlayerCount()==0,"no players allowed in disposable diagnostic world");
        Path marker=world.resolve(".nonore-tag-probe-used");
        h.assertTrue(!Files.exists(marker),"refuse reused fixture world; allocate a fresh directory");
        Files.writeString(marker,UUID.randomUUID().toString(),StandardOpenOption.CREATE_NEW);
        new Probe(h).start();
    }
    private static final class Probe {
        private final GameTestHelper h;
        private final net.minecraft.server.MinecraftServer server;
        private final List<String> originalPacks;
        private final RecipeMap crusher=MachineRecipeMaps.Crusher;
        private final ItemStack foreign=probeItem("nonore_raw_probe"), crushedForeign=probeItem("nonore_crushed_probe");
        private final ItemStack collisionForeign=probeItem("nonore_collision_probe");
        private final GTMaterial rubber=GTMaterialRegistry.get("Rubber").resolve();
        private final ItemStack collisionFine=GTItems.getStack(MaterialPrefix.crushedTiny,rubber,1);
        private Recipe startupWinner;
        private final GTMaterial material=GTMaterialRegistry.get("PVC").resolve();
        private final ItemStack fine=GTItems.getStack(MaterialPrefix.crushedTiny,material,1);
        private final ItemStack purified=GTItems.getStack(MaterialPrefix.crushedPurified,material,1);
        private final Path root,tag,crushedTag,collisionTag,removal,addition;
        private final String metadata;
        private RecipeMap addon;
        private Recipe sentinel;
        private List<String> fineBefore,purifiedBefore;
        private boolean inFlight,closing,restoring,finished,ownsDirectory;
        private Throwable failure;
        private int verifiedRounds;
        private List<String> withProbe;
        Probe(GameTestHelper h){
            this.h=h;server=h.getLevel().getServer();originalPacks=List.copyOf(server.getPackRepository().getSelectedIds());
            root=server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("gt-nonore-tag-continuation-probe");
//? if >=1.21.1 {
/*            tag=root.resolve("data/forge/tags/item/raw_ore_chunks/pvc.json");
            crushedTag=root.resolve("data/forge/tags/item/crushed/pvc.json");
            collisionTag=root.resolve("data/forge/tags/item/raw_ore_chunks/rubber.json");
            removal=root.resolve("data/gt6ce/recipe/nonore_probe/remove.json");
            addition=root.resolve("data/gt6ce/recipe/nonore_probe/add.json");
            metadata="{\"pack\":{\"pack_format\":48,\"description\":\"Temporary nonore continuation probe\"}}";
*///?} else {
            tag=root.resolve("data/forge/tags/items/raw_ore_chunks/pvc.json");
            crushedTag=root.resolve("data/forge/tags/items/crushed/pvc.json");
            collisionTag=root.resolve("data/forge/tags/items/raw_ore_chunks/rubber.json");
            removal=root.resolve("data/gt6ce/recipes/nonore_probe/remove.json");
            addition=root.resolve("data/gt6ce/recipes/nonore_probe/add.json");
            metadata="{\"pack\":{\"pack_format\":15,\"description\":\"Temporary nonore continuation probe\"}}";
//?}
        }
        void start(){
            try {
                require(!Files.exists(root),"temporary pack must not overwrite existing data");
                require(!fine.isEmpty()&&!purified.isEmpty(),"legacy downstream form IDs exist");
                require(!foreign.isEmpty()&&!crushedForeign.isEmpty(),"registered plain addon probe items present");
                require(ItemMaterialRegistry.base(foreign.getItem()).isEmpty()
                        &&ItemMaterialRegistry.base(crushedForeign.getItem()).isEmpty(),"foreign fixture metadata absent: raw="+ItemMaterialRegistry.base(foreign.getItem())
                                +", crushed="+ItemMaterialRegistry.base(crushedForeign.getItem()));
                require(lookup(crusher,foreign)==null,"foreign raw chunk fixture has no existing Crusher row");
                fineBefore=contents(lookup(crusher,fine));purifiedBefore=contents(lookup(MachineRecipeMaps.Centrifuge,purified));
                startupWinner=lookup(crusher,collisionFine);
                System.out.println("[nonore-baseline] fine="+fineBefore+" purified="+purifiedBefore+" startupRubber="+contents(startupWinner));
                require(!collisionForeign.isEmpty()&&!collisionFine.isEmpty()&&ItemMaterialRegistry.base(collisionForeign.getItem()).isEmpty(),"unbound registered third fixture and retained Rubber input");
                require(lookup(crusher,collisionForeign)==null,"third foreign fixture has no existing Crusher route");
                require(lookup(crusher,fine)==null&&lookup(MachineRecipeMaps.Centrifuge,purified)==null,
                        "reviewed NO owned PVC continuations are absent before external tags");
                require(startupWinner!=null&&startupWinner.mOutputs.length==1&&startupWinner.mOutputs[0].is(Items.EMERALD)
                        &&startupWinner.mOutputs[0].getCount()==1&&startupWinner.mDuration==37&&startupWinner.mEUt==16,
                        "supported onRecipesReady same-input Rubber winner accepted before replay");
                require(bath(crushedForeign)==null,"foreign crushed fixture has no prior Bath route");
                addon=RecipeMap.RECIPE_MAPS.get("addon_sample.recipe.press");
                require(addon!=null,"actual startup addon sample present");
                sentinel=lookup(addon,new ItemStack(Items.IRON_INGOT));
                require(sentinel!=null&&sentinel.mOutputs[0].is(Items.IRON_NUGGET),"startup sentinel present");
                Files.createDirectory(root);ownsDirectory=true;
                Files.createDirectories(tag.getParent());Files.createDirectories(crushedTag.getParent());
                Files.writeString(root.resolve("pack.mcmeta"),metadata);
                Files.writeString(tag,"{\"replace\":false,\"values\":[\"addon_sample:nonore_raw_probe\"]}");
                Files.writeString(crushedTag,"{\"replace\":false,\"values\":[\"addon_sample:nonore_crushed_probe\"]}");
                Files.createDirectories(collisionTag.getParent());
                Files.writeString(collisionTag,"{\"replace\":false,\"values\":[\"addon_sample:nonore_collision_probe\"]}");
                server.getPackRepository().reload();
                withProbe=new ArrayList<>(originalPacks);withProbe.add("file/gt-nonore-tag-continuation-probe");
                // Soft deadline requests cleanup, never finishes while reload is in flight.
                h.runAfterDelay(6000,()->{if(!finished){record(new IllegalStateException("soft reload deadline"));closing=true;if(!inFlight)restore();}});
                reloadProbe(0);
            }catch(Throwable error){
                record(error);
                if(ownsDirectory)restore();
                else {
                    // No private pack was created, selection changed, or reload attempted. Do not claim restoration.
                    finished=true;
                    h.fail("ISOLATED PROFILE SETUP FAILED before pack creation/reload; fresh JVM required: "+failure);
                }
            }
        }
        private void reloadProbe(int round){
            try {
                lookup(crusher,foreign);lookup(crusher,fine);lookup(MachineRecipeMaps.Centrifuge,purified);
                if(round==1){
                    String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(fine.getItem()).toString();
                    Files.createDirectories(removal.getParent());
                    Files.writeString(removal,"{\"type\":\"gt6ce:machine_removal\",\"map\":\""+crusher.mNameInternal
                            +"\",\"item_inputs\":[{\"item\":\""+id+"\",\"count\":1}]}");
                    Files.writeString(addition,"{\"type\":\"gt6ce:machine\",\"map\":\""+crusher.mNameInternal
                            +"\",\"item_inputs\":[{\"item\":\""+id+"\"}],\"item_outputs\":[{\"item\":\"minecraft:nether_star\"}],\"duration\":16,\"eu_per_tick\":16}");
                }
                inFlight=true;
                server.reloadResources(withProbe).whenCompleteAsync((ignored,error)->{
                    inFlight=false;
                    if(error!=null)record(error);
                    if(closing||failure!=null){restore();return;}
                    try {verifyTagged(round);verifiedRounds++;if(round==0)reloadProbe(1);else restore();}
                    catch(Throwable assertion){record(assertion);restore();}
                },server);
            }catch(Throwable error){inFlight=false;record(error);restore();}
        }
        private void verifyTagged(int round){
            var collision=ItemMaterialRegistry.base(collisionForeign.getItem()).orElseThrow();
            require(ItemMaterialRegistry.explicit(collisionForeign.getItem()).isEmpty()&&collision.prefix()==MaterialPrefix.rawOreChunk
                    &&collision.material().resolve()==rubber,"actual foreign Rubber trigger for dormant conflicting producer");
            var upstreamRubber=lookup(crusher,collisionForeign);
            require(upstreamRubber!=null&&upstreamRubber.mOutputs.length==1&&upstreamRubber.mOutputs[0].is(collisionFine.getItem())
                    &&upstreamRubber.mOutputs[0].getCount()==3,"foreign Rubber raw chunk reaches same-input startup winner");
            require(lookup(crusher,collisionFine)==startupWinner&&lookup(crusher,collisionFine)==startupWinner,
                    "warm same-input startup winner survives attempted dormant admission collision");
            var chunk=ItemMaterialRegistry.base(foreign.getItem()).orElseThrow();
            var crushed=ItemMaterialRegistry.base(crushedForeign.getItem()).orElseThrow();
            require(ItemMaterialRegistry.explicit(foreign.getItem()).isEmpty()&&chunk.prefix()==MaterialPrefix.rawOreChunk
                    &&chunk.material().resolve()==material,"actual tag-only raw chunk composition");
            require(ItemMaterialRegistry.explicit(crushedForeign.getItem()).isEmpty()&&crushed.prefix()==MaterialPrefix.crushed
                    &&crushed.material().resolve()==material,"actual supported foreign crushed/PVC tag");
            var bath=bath(crushedForeign);
            require(bath!=null&&Arrays.stream(bath.mOutputs).anyMatch(i->i.is(purified.getItem())),"foreign crushed washing retains builtin purified output");
            require(lookup(MachineRecipeMaps.Centrifuge,purified)!=null,"purified continuation remains usable");
            Recipe upstream=lookup(crusher,foreign);
            require(upstream!=null&&upstream.mOutputs.length==1&&upstream.mOutputs[0].is(fine.getItem())
                    &&upstream.mOutputs[0].getCount()==3,"supported external rawOreChunk emits builtin crushedTiny3");
            require(gregtech.content.recipe.ExternalOreProcessing.owned().contains(upstream),"actual external producer ownership");
            Recipe selected=lookup(crusher,fine);
            require(selected!=null&&lookup(crusher,fine)==selected,"warm builtin downstream lookup remains usable");
            if(round==1)require(selected.mOutputs.length==1&&selected.mOutputs[0].is(Items.NETHER_STAR)
                    &&selected.mOutputs[0].getCount()==1,"datapack override has final precedence");
            require(lookup(addon,new ItemStack(Items.IRON_INGOT))==sentinel,"startup addon identity preserved");
        }
        private void restore(){
            closing=true;
            if(inFlight||restoring||finished)return;
            restoring=true;inFlight=true;
            try {server.reloadResources(originalPacks).whenCompleteAsync((ignored,error)->{
                inFlight=false;
                boolean clean=error==null;
                if(error!=null)record(error);
                if(clean)try {
                    require(List.copyOf(server.getPackRepository().getSelectedIds()).equals(originalPacks),"original pack selection restored");
                    require(lookup(crusher,foreign)==null&&ItemMaterialRegistry.base(foreign.getItem()).isEmpty()
                            &&ItemMaterialRegistry.base(crushedForeign.getItem()).isEmpty()&&bath(crushedForeign)==null,"tag metadata and both external input routes removed");
                    require(ItemMaterialRegistry.base(collisionForeign.getItem()).isEmpty()&&lookup(crusher,collisionForeign)==null,
                            "third foreign trigger removed after restoration");
                    require(lookup(crusher,collisionFine)==startupWinner,"same-input startup winner retained after tags removed");
                    require(lookup(crusher,fine)==null&&lookup(MachineRecipeMaps.Centrifuge,purified)==null,
                            "PVC continuations return to source-NO absence after completed cleanup reload");
                    require(contents(lookup(crusher,fine)).equals(fineBefore)
                            &&contents(lookup(MachineRecipeMaps.Centrifuge,purified)).equals(purifiedBefore),
                            "downstream outputs/chances/work return to pre-probe state; no stale datapack override");
                    require(lookup(addon,new ItemStack(Items.IRON_INGOT))==sentinel,"startup addon survives restoration");
                }catch(Throwable assertion){record(assertion);clean=false;}
                finish(clean);
            },server);}catch(Throwable error){inFlight=false;record(error);finish(false);}
        }
        private void finish(boolean restored){
            if(finished)return;
            try {if(ownsDirectory&&Files.exists(root))try(var paths=Files.walk(root)){
                for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);
            }}catch(Throwable cleanup){record(cleanup);restored=false;}
            if(restored)try {
                var receipt=new com.google.gson.JsonObject();receipt.addProperty("restored",true);receipt.addProperty("sourceNoPolicyVerified",failure==null&&verifiedRounds==2);receipt.addProperty("sameInputStartupWinnerVerified",failure==null&&verifiedRounds==2);
                receipt.addProperty("probePassed",failure==null);receipt.addProperty("verifiedRounds",verifiedRounds);
                receipt.addProperty("world",server.getWorldPath(LevelResource.ROOT).toRealPath().toString());
                var packs=new com.google.gson.JsonArray();for(String pack:originalPacks)packs.add(pack);receipt.add("originalPacks",packs);
                Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(".nonore-tag-cleanup.json"),receipt.toString());
                System.out.println("[nonore-tag-cleanup] restored=true verifiedRounds="+verifiedRounds+" probePassed="+(failure==null));
            }catch(Throwable receiptFailure){record(receiptFailure);restored=false;}
            finished=true;
            if(failure!=null)h.fail((restored?"Probe failed after completed restoration: ":"ISOLATED PROFILE TAINTED; abort, do not run another batch: ")+failure);
            else if(!restored)h.fail("ISOLATED PROFILE restoration not established");
            else h.succeed();
        }
        private void record(Throwable error){if(failure==null)failure=error;else if(failure!=error)failure.addSuppressed(error);}
    }
    private static ItemStack probeItem(String path){
        return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("addon_sample",path)));
    }
    private static Recipe lookup(RecipeMap map,ItemStack input){return map.findRecipe(List.of(input),List.of(),false,1,12);}
    private static Recipe bath(ItemStack input){return MachineRecipeMaps.Bath.findRecipe(List.of(input),
            List.of(new FluidStack(Fluids.WATER,1000)),false,1,12);}
    /** Content comparison only, NEVER recipe ownership or removal by signature. */
    private static List<String> contents(Recipe row){
        if(row==null)return List.of("<none>");
        var result=new ArrayList<String>();
        result.add("work="+row.mDuration+"/"+row.mEUt+"/"+row.mSpecialValue);
        result.add("chances="+Arrays.toString(row.mChances));
        for(var item:row.mInputs)result.add("in="+itemContent(item));
        for(var item:row.mOutputs)result.add("out="+itemContent(item));
        for(var fluid:row.mFluidInputs)result.add("fluidIn="+fluidContent(fluid));
        for(var fluid:row.mFluidOutputs)result.add("fluidOut="+fluidContent(fluid));
        return List.copyOf(result);
    }
    private static String itemContent(ItemStack item){return item==null||item.isEmpty()?"EMPTY":
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem())+"x"+item.getCount();}
    private static String fluidContent(FluidStack fluid){return fluid==null||fluid.isEmpty()?"EMPTY":
            net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid.getFluid())+"x"+fluid.getAmount();}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
