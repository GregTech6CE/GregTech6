package gregtech.gametest;

import com.google.gson.JsonObject;
import gregtech.internal.material.ItemMaterialRegistry;
import gregtech.internal.recipe.Recipe;
import gregtech.internal.recipe.RecipeMap;
import gregtech.data.MachineRecipeMaps;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
//? if >=1.21.1 {
/*import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fluids.FluidStack;
//?}
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/** Test-only observer of the REAL stopped event. Never clears recipes or posts lifecycle events.
 * GameTest XML finishes before this callback: an external checker MUST require its receipt.
 */
//? if >=1.21.1 {
/*@EventBusSubscriber(modid="gt6ce")
*///?} else {
@Mod.EventBusSubscriber(modid="gt6ce")
//?}
public final class NonOreActualStopObserver {
    private NonOreActualStopObserver() {}
    private static final String NS="gregtech_nonore_stop_audit";
    private static final String CASE="nonoreactiverowsclearonactualserverstop";
    private static Armed armed;
    private static boolean consumed;
    private record Armed(MinecraftServer server,Path world,String nonce,List<String> original,List<String> selected,
                         ItemStack foreign,ItemStack crushedForeign,ItemStack collisionForeign,
                         ItemStack fine,ItemStack purified,ItemStack collisionFine,
                         Recipe fineRow,Recipe purifiedRow,Recipe winner) {}

    public static synchronized void arm(GameTestHelper h,String nonce,List<String> originalPacks,List<String> selectedPacks,
            ItemStack foreign,ItemStack crushedForeign,ItemStack collisionForeign,ItemStack fine,ItemStack purified,
            ItemStack collisionFine,Recipe fineRow,Recipe purifiedRow,Recipe startupWinner) throws IOException {
        require(namespace().equals(NS),"exact stop-only namespace required");
        require(armed==null&&!consumed,"one arm per JVM");
        require(nonce!=null&&nonce.matches("[0-9a-f]{32}")&&nonce.equals(System.getenv("GT6_NONORE_STOP_NONCE")),"expected nonce required");
        MinecraftServer server=h.getLevel().getServer();
        Path run=Path.of(System.getenv("GT6_NONORE_STOP_DIRECTORY")).toRealPath();
        Path world=server.getWorldPath(LevelResource.ROOT).toRealPath();
        require(run.getFileName().toString().startsWith("gt6-nonore-stop-")&&world.startsWith(run)&&!world.equals(run),"owned disposable world required");
        require(server.getPlayerList().getPlayerCount()==0,"no players");
        require(Files.readString(world.resolve(".nonore-stop-used")).trim().equals(nonce),"fresh marker nonce mismatch");
        require(!Files.exists(world.resolve(".nonore-stop-event.json")),"receipt already exists");
        for(ItemStack stack:List.of(foreign,crushedForeign,collisionForeign,fine,purified,collisionFine))require(!stack.isEmpty(),"missing arm input");
        require(fineRow!=null&&purifiedRow!=null&&startupWinner!=null,"missing active handles");
        require(lookup(MachineRecipeMaps.Crusher,fine)==fineRow&&lookup(MachineRecipeMaps.Centrifuge,purified)==purifiedRow,"real active rows must be selected at arm");
        require(MachineRecipeMaps.Crusher.mRecipeList.contains(fineRow)&&MachineRecipeMaps.Centrifuge.mRecipeList.contains(purifiedRow),"active rows accepted");
        require(lookup(MachineRecipeMaps.Crusher,collisionFine)==startupWinner&&winnerSemantics(startupWinner),"startup winner required");
        require(List.copyOf(server.getPackRepository().getSelectedIds()).equals(selectedPacks)
                &&selectedPacks.size()==originalPacks.size()+1
                &&selectedPacks.subList(0,originalPacks.size()).equals(originalPacks)
                &&selectedPacks.get(selectedPacks.size()-1).equals("file/gt-nonore-stop-probe"),"exact probe pack selection active");
        armed=new Armed(server,world,nonce,List.copyOf(originalPacks),List.copyOf(selectedPacks),foreign.copy(),crushedForeign.copy(),collisionForeign.copy(),fine.copy(),purified.copy(),collisionFine.copy(),fineRow,purifiedRow,startupWinner);
        JsonObject log=identity(armed);log.addProperty("armedActiveRows",2);log.addProperty("passed",true);
        System.out.println("[nonore-stop-arm] "+log);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static synchronized void stopped(ServerStoppedEvent event) {
        if(!namespace().equals(NS)||armed==null)return;
        Armed state=armed;armed=null;consumed=true;
        JsonObject result=identity(state);
        result.addProperty("event","ServerStoppedEvent");result.addProperty("priority","LOWEST");
        result.addProperty("armedActiveRows",2);result.addProperty("passed",false);
        try {
            require(event.getServer()==state.server,"stopped server differs from armed instance");
            require(event.getServer().getWorldPath(LevelResource.ROOT).toRealPath().equals(state.world),"stopped world differs");
            result.addProperty("realEventObserved",true);
            require(!MachineRecipeMaps.Crusher.mRecipeList.contains(state.fineRow)&&!MachineRecipeMaps.Centrifuge.mRecipeList.contains(state.purifiedRow)
                    &&lookup(MachineRecipeMaps.Crusher,state.fine)==null&&lookup(MachineRecipeMaps.Centrifuge,state.purified)==null,"active source rows survived actual stop");
            result.addProperty("rowsAbsent",true);
            for(ItemStack stack:List.of(state.foreign,state.crushedForeign,state.collisionForeign))
                require(ItemMaterialRegistry.explicit(stack.getItem()).isEmpty()&&ItemMaterialRegistry.base(stack.getItem()).isEmpty(),"foreign metadata survived stop");
            result.addProperty("foreignMetadataAbsent",true);
            require(lookup(MachineRecipeMaps.Crusher,state.foreign)==null&&lookup(MachineRecipeMaps.Crusher,state.collisionForeign)==null
                    &&MachineRecipeMaps.Bath.findRecipe(List.of(state.crushedForeign),List.of(new FluidStack(Fluids.WATER,1000)),false,1,12)==null,"foreign routes survived stop");
            result.addProperty("foreignRoutesAbsent",true);
            require(lookup(MachineRecipeMaps.Crusher,state.collisionFine)==state.winner&&winnerSemantics(state.winner),"independent startup winner lost");
            result.addProperty("startupWinnerPreserved",true);
            result.addProperty("passed",true);
        } catch(Throwable failure) {result.addProperty("error",failure.toString());}
        try {
            var original=new com.google.gson.JsonArray();state.original.forEach(original::add);result.add("originalPacks",original);
            var selected=new com.google.gson.JsonArray();state.selected.forEach(selected::add);result.add("selectedProbePacks",selected);
            Path target=state.world.resolve(".nonore-stop-event.json"),temporary=state.world.resolve(".nonore-stop-event."+state.nonce+".tmp");
            require(!Files.exists(target),"refuse receipt overwrite");
            Files.writeString(temporary,result.toString(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);
        } catch(Throwable failure) {result.addProperty("passed",false);result.addProperty("receiptError",failure.toString());}
        System.out.println("[nonore-stop-event] "+result);
    }
    private static JsonObject identity(Armed state){var o=new JsonObject();o.addProperty("schema",1);o.addProperty("namespace",NS);o.addProperty("test",CASE);o.addProperty("nonce",state.nonce);o.addProperty("world",state.world.toString());return o;}
    private static String namespace(){return System.getProperty("neoforge.enabledGameTestNamespaces",System.getProperty("forge.enabledGameTestNamespaces",""));}
    private static Recipe lookup(RecipeMap map,ItemStack input){return map.findRecipe(List.of(input),List.of(),false,1,12);}
    private static boolean winnerSemantics(Recipe r){return r.mOutputs.length==1&&r.mOutputs[0].is(Items.EMERALD)&&r.mOutputs[0].getCount()==1&&r.mDuration==37&&r.mEUt==16;}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
