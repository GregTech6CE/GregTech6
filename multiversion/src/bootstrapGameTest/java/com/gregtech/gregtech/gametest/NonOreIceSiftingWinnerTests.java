package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
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
@GameTestHolder("gregtech_nonore_ice_winner_audit")
@PrefixGameTestTemplate(false)
public final class NonOreIceSiftingWinnerTests {
    private static boolean started;
    @GameTest(template="test_empty",batch="nonore_ice_winner_isolated",timeoutTicks=8000)
    public static void iceTinyStartupWinnerSurvivesRealTagsAndCleanup(GameTestHelper h)throws Exception{
        String enabled=System.getProperty("neoforge.enabledGameTestNamespaces",System.getProperty("forge.enabledGameTestNamespaces",""));
        h.assertTrue("gregtech_nonore_ice_winner_audit".equals(enabled),"exact isolated namespace required; no combined suite");
        h.assertTrue(!started,"single cold probe per JVM required");started=true;
        String configured=System.getenv("GT6_NONORE_ICE_WINNER_DIRECTORY");
        h.assertTrue(configured!=null&&!configured.isBlank(),"explicit fresh disposable run directory required");
        Path directory=Path.of(configured).toRealPath();
        Path world=h.getLevel().getServer().getWorldPath(LevelResource.ROOT).toRealPath();
        h.assertTrue(directory.getFileName().toString().startsWith("gt6-nonore-ice-winner-"),"dedicated disposable directory prefix required");
        h.assertTrue(world.startsWith(directory)&&!world.equals(directory),"actual world inside disposable run directory");
        h.assertTrue(h.getLevel().getServer().getPlayerList().getPlayerCount()==0,"no players allowed in disposable diagnostic world");
        Path marker=world.resolve(".nonore-ice-winner-used");
        h.assertTrue(!Files.exists(marker),"refuse reused fixture world; allocate a fresh directory");
        String nonce=System.getenv("GT6_NONORE_ICE_WINNER_NONCE");
        h.assertTrue(nonce!=null&&nonce.matches("[0-9a-f]{32}"),"fresh external nonce required");
        Files.writeString(marker,nonce,StandardOpenOption.CREATE_NEW);
        new Probe(h).start();
    }
    private static final class Probe {
        private final GameTestHelper h;
        private final net.minecraft.server.MinecraftServer server;
        private final List<String> originalPacks;
        private final RecipeMap crusher=MachineRecipeMaps.Crusher;
        private final ItemStack foreign=probeItem("nonore_raw_probe"), crushedForeign=probeItem("nonore_crushed_probe");
        private final GTMaterial material=GTMaterialRegistry.get("Ice").resolve();
        private final ItemStack fine=GTItems.getStack(MaterialPrefix.crushedTiny,material,1);
        private final ItemStack purified=GTItems.getStack(MaterialPrefix.crushedPurified,material,1);
        private final Path root,tag,crushedTag,removal,addition;
        private final String metadata;
        private RecipeMap addon;
        private Recipe sentinel;
        private List<String> fineBefore,purifiedBefore;
        private Recipe cinnabarWhole,cinnabarTiny,startupWinner;
        private final ItemStack purifiedTiny=GTItems.getStack(MaterialPrefix.crushedPurifiedTiny,material,1);
        private boolean inFlight,closing,restoring,finished,ownsDirectory;
        private Throwable failure;
        private int verifiedRounds;
        private List<String> withProbe;
        Probe(GameTestHelper h){
            this.h=h;server=h.getLevel().getServer();originalPacks=List.copyOf(server.getPackRepository().getSelectedIds());
            root=server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("gt-nonore-ice-winner-probe");
//? if >=1.21.1 {
/*            tag=root.resolve("data/forge/tags/item/raw_ore_chunks/ice.json");
            crushedTag=root.resolve("data/forge/tags/item/crushed/ice.json");
            removal=root.resolve("data/gt6ce/recipe/nonore_probe/remove.json");
            addition=root.resolve("data/gt6ce/recipe/nonore_probe/add.json");
            metadata="{\"pack\":{\"pack_format\":48,\"description\":\"Temporary nonore continuation probe\"}}";
*///?} else {
            tag=root.resolve("data/forge/tags/items/raw_ore_chunks/ice.json");
            crushedTag=root.resolve("data/forge/tags/items/crushed/ice.json");
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
                require(!purifiedTiny.isEmpty(),"retained tiny purified Ice input exists");
                System.out.println("[nonore-ice-winner-initial] whole="+contents(lookup(MachineRecipeMaps.Sifting,purified))+" tiny="+contents(lookup(MachineRecipeMaps.Sifting,purifiedTiny)));
                startupWinner=lookup(MachineRecipeMaps.Sifting,purifiedTiny);
                require(startupWinner!=null&&startupWinner.mInputs.length==1&&startupWinner.mInputs[0].is(purifiedTiny.getItem())
                        &&startupWinner.mInputs[0].getCount()==1&&startupWinner.mOutputs.length==1&&startupWinner.mOutputs[0].is(Items.EMERALD)
                        &&startupWinner.mOutputs[0].getCount()==1&&startupWinner.getOutputChance(0)==10000
                        &&startupWinner.mDuration==37&&startupWinner.mEUt==16&&startupWinner.mFluidInputs.length==0&&startupWinner.mFluidOutputs.length==0,
                        "real public onRecipesReady Ice tiny winner accepted before any replay");
                require(lookup(MachineRecipeMaps.Sifting,purified)==null,"owned whole Ice row dormant before external tags");
                winner("initial");
                require(lookup(crusher,fine)==null&&lookup(MachineRecipeMaps.Centrifuge,purified)==null,
                        "existing bounded Ice maceration/washing policy remains active");
                cinnabarWhole=lookup(MachineRecipeMaps.Sifting,OreSourceDifferentialTests.s("crushedPurified","Cinnabar",1));
                cinnabarTiny=lookup(MachineRecipeMaps.Sifting,OreSourceDifferentialTests.s("crushedPurifiedTiny","Cinnabar",1));
                require(cinnabarWhole!=null&&cinnabarTiny!=null,"independent existing Cinnabar pair present");
                for(String form:new String[]{"gem","gemFlawed","gemChipped"})OreSourceDifferentialTests.s(form,"Ice",1);
                require(bath(crushedForeign)==null,"foreign crushed fixture has no prior Bath route");
                addon=RecipeMap.RECIPE_MAPS.get("addon_sample.recipe.press");
                require(addon!=null,"actual startup addon sample present");
                sentinel=lookup(addon,new ItemStack(Items.IRON_INGOT));
                require(sentinel!=null&&sentinel.mOutputs.length==1&&sentinel.mOutputs[0].is(Items.IRON_NUGGET)
                        &&sentinel.mOutputs[0].getCount()==1&&sentinel.getOutputChance(0)==5000
                        &&sentinel.mDuration==20&&sentinel.mEUt==16&&sentinel.mFluidInputs.length==0&&sentinel.mFluidOutputs.length==0,
                        "exact supported startup sentinel present");
                Files.createDirectory(root);ownsDirectory=true;
                Files.createDirectories(tag.getParent());Files.createDirectories(crushedTag.getParent());
                Files.writeString(root.resolve("pack.mcmeta"),metadata);
                Files.writeString(tag,"{\"replace\":false,\"values\":[\"addon_sample:nonore_raw_probe\"]}");
                Files.writeString(crushedTag,"{\"replace\":false,\"values\":[\"addon_sample:nonore_crushed_probe\"]}");
                server.getPackRepository().reload();
                withProbe=new ArrayList<>(originalPacks);withProbe.add("file/gt-nonore-ice-winner-probe");
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
                lookup(crusher,foreign);lookup(crusher,fine);lookup(MachineRecipeMaps.Sifting,purified);lookup(MachineRecipeMaps.Sifting,purifiedTiny);
                if(round==1){
                    String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(purified.getItem()).toString();
                    Files.createDirectories(removal.getParent());
                    Files.writeString(removal,"{\"type\":\"gt6ce:machine_removal\",\"map\":\""+MachineRecipeMaps.Sifting.mNameInternal
                            +"\",\"item_inputs\":[{\"item\":\""+id+"\",\"count\":1}]}");
                    Files.writeString(addition,"{\"type\":\"gt6ce:machine\",\"map\":\""+MachineRecipeMaps.Sifting.mNameInternal
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
            var chunk=ItemMaterialRegistry.base(foreign.getItem()).orElseThrow();
            var crushed=ItemMaterialRegistry.base(crushedForeign.getItem()).orElseThrow();
            require(ItemMaterialRegistry.explicit(foreign.getItem()).isEmpty()&&chunk.prefix()==MaterialPrefix.rawOreChunk
                    &&chunk.material().resolve()==material,"actual tag-only raw chunk composition");
            require(ItemMaterialRegistry.explicit(crushedForeign.getItem()).isEmpty()&&crushed.prefix()==MaterialPrefix.crushed
                    &&crushed.material().resolve()==material,"actual supported foreign crushed/Ice tag");
            var bath=bath(crushedForeign);
            require(bath!=null&&Arrays.stream(bath.mOutputs).anyMatch(i->i.is(purified.getItem())),"foreign crushed washing retains builtin purified output");
            require(lookup(MachineRecipeMaps.Centrifuge,purified)!=null,"purified continuation remains usable");
            Recipe upstream=lookup(crusher,foreign);
            require(upstream!=null&&upstream.mOutputs.length==1&&upstream.mOutputs[0].is(fine.getItem())
                    &&upstream.mOutputs[0].getCount()==3,"supported external rawOreChunk emits builtin crushedTiny3");
            require(com.gregtech.gregtech.content.recipe.ExternalOreProcessing.owned().contains(upstream),"actual external producer ownership");
            Recipe selected=lookup(crusher,fine);
            require(selected!=null&&lookup(crusher,fine)==selected,"warm upstream continuation remains usable");
            winner("tagged-"+round); // tiny source-output proof belongs exclusively to the separate literal-pair profile
            if(round==0)sourceWholeSifting();
            else {
                Recipe override=lookup(MachineRecipeMaps.Sifting,purified);
                require(override!=null&&lookup(MachineRecipeMaps.Sifting,purified)==override&&override.mOutputs.length==1
                        &&override.mOutputs[0].is(Items.NETHER_STAR)&&override.mOutputs[0].getCount()==1
                        &&override.getOutputChance(0)==10000&&override.mDuration==16&&override.mEUt==16
                        &&override.mFluidInputs.length==0&&override.mFluidOutputs.length==0,
                        "real datapack Ice Sifting override has final precedence");
            }
            require(lookup(MachineRecipeMaps.Sifting,OreSourceDifferentialTests.s("crushedPurified","Cinnabar",1))==cinnabarWhole
                    &&lookup(MachineRecipeMaps.Sifting,OreSourceDifferentialTests.s("crushedPurifiedTiny","Cinnabar",1))==cinnabarTiny,
                    "independent Cinnabar identities survive Ice reload");
            require(lookup(addon,new ItemStack(Items.IRON_INGOT))==sentinel,"startup addon identity preserved");
        }
        private void winner(String phase){
            require(startupWinner!=null&&MachineRecipeMaps.Sifting.mRecipeList.contains(startupWinner)
                    &&lookup(MachineRecipeMaps.Sifting,purifiedTiny)==startupWinner&&lookup(MachineRecipeMaps.Sifting,purifiedTiny)==startupWinner,
                    "same-input independent tiny winner survives collision/cleanup: "+phase);
            System.out.println("[nonore-ice-tiny-winner] "+phase+" verified=true");
        }
        private void sourceWholeSifting(){
            // Original Ores whole/tiny: Ice forces only common/flawed/chipped; absent upper grades
            // fall back to common gem8/4/2. Do not compute expectations with production policies.
            var out=new ItemStack[]{OreSourceDifferentialTests.s("gem","Ice",8),OreSourceDifferentialTests.s("gem","Ice",4),
                    OreSourceDifferentialTests.s("gem","Ice",2),OreSourceDifferentialTests.s("gem","Ice",1),
                    OreSourceDifferentialTests.s("gemFlawed","Ice",2),OreSourceDifferentialTests.s("gemChipped","Ice",4),
                    OreSourceDifferentialTests.s("dust","Ice",1)};
            ItemStack input=purified;
            OreSourceDifferentialTests.row(h,"actual external Ice whole continuation literal",MachineRecipeMaps.Sifting,input,
                    new FluidStack[0],out,new long[]{9,90,360,1350,1800,3600,4500},
                    new FluidStack[0],144,16);
            Recipe row=lookup(MachineRecipeMaps.Sifting,input);
            require(row!=null&&lookup(MachineRecipeMaps.Sifting,input)==row,"actual Ice Sifting warm selection");
            System.out.println("[nonore-ice-winner-literal] "+"whole"+" "+contents(row));
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
                    require(lookup(crusher,fine)==null&&lookup(MachineRecipeMaps.Centrifuge,purified)==null,
                            "Ice continuations return to source-NO absence after completed cleanup reload");
                    require(lookup(MachineRecipeMaps.Sifting,purified)==null,"owned whole Ice row returns absent after restoration");
                    winner("restored");
                    require(lookup(MachineRecipeMaps.Sifting,OreSourceDifferentialTests.s("crushedPurified","Cinnabar",1))==cinnabarWhole
                            &&lookup(MachineRecipeMaps.Sifting,OreSourceDifferentialTests.s("crushedPurifiedTiny","Cinnabar",1))==cinnabarTiny,
                            "Cinnabar identities retained after restoration");
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
                var receipt=new com.google.gson.JsonObject();receipt.addProperty("restored",true);receipt.addProperty("sourceNoPolicyVerified",failure==null&&verifiedRounds==2);receipt.addProperty("wholeLiteralAndTinyWinnerVerified",failure==null&&verifiedRounds==2);
                receipt.addProperty("schema",1);receipt.addProperty("namespace","gregtech_nonore_ice_winner_audit");
                receipt.addProperty("nonce",System.getenv("GT6_NONORE_ICE_WINNER_NONCE"));
                receipt.addProperty("probePassed",failure==null);receipt.addProperty("verifiedRounds",verifiedRounds);
                receipt.addProperty("world",server.getWorldPath(LevelResource.ROOT).toRealPath().toString());
                var packs=new com.google.gson.JsonArray();for(String pack:originalPacks)packs.add(pack);receipt.add("originalPacks",packs);
                var restoredPacks=new com.google.gson.JsonArray();for(String pack:server.getPackRepository().getSelectedIds())restoredPacks.add(pack);receipt.add("restoredPacks",restoredPacks);
                Path receiptPath=server.getWorldPath(LevelResource.ROOT).resolve(".nonore-ice-winner-cleanup.json");
                Path temporary=receiptPath.resolveSibling(".nonore-ice-winner-cleanup.tmp");
                Files.writeString(temporary,receipt.toString(),StandardOpenOption.CREATE_NEW);
                Files.move(temporary,receiptPath,StandardCopyOption.ATOMIC_MOVE);
                System.out.println("[nonore-ice-winner-cleanup] "+receipt);
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
