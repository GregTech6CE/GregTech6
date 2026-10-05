package com.gregtech.gregtech.integration.client;

import com.google.gson.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.jei.RecipeMapCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Source flags and actual registered machine browser rows; finite isolated viewer launch only. */
public final class RecipePresentationRuntimeSmoke {
    private static final boolean EMI=net.minecraftforge.fml.ModList.get().isLoaded("emi");
    private static int stage,frames;
    private static JsonObject receipt;
    private static Throwable failure;
    private static RecipeMap selected;
    private static boolean checked;
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Recipe presentation: "+detail);}
    public static boolean frame(Minecraft minecraft,JsonObject result) {
        if(failure!=null)throw new IllegalStateException("Recipe presentation screenshot failed",failure);
        if(stage==3)return true;
        if(stage>0)return false;
        if(RecipePresentationJeiSmoke.runtime==null)return false;
        var manager=RecipePresentationJeiSmoke.runtime.getRecipeManager();
        var all=new ArrayList<>(MachineRecipeMapDefinitions.all());
        try {for(var field:FuelRecipeMapDefinitions.class.getFields())if(field.getType()==RecipeMapSpec.class)all.add((RecipeMapSpec)field.get(null));}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        var actualRows=new HashMap<String,java.util.List<dev.emi.emi.api.recipe.EmiRecipe>>();
        if(EMI)for(var row:dev.emi.emi.api.EmiApi.getRecipeManager().getRecipes()) {
            var id=row.getCategory().getId();if(id.getNamespace().equals("gregtech"))actualRows.computeIfAbsent(id.getPath(),key->new ArrayList<>()).add(row);
        }
        int projected=0,hidden=0,categories=0;long jeiRows=0,emiRows=0,retained=0;
        var hiddenPools=new JsonObject();
        for(var definition:all) {
            var map=RecipeMap.RECIPE_MAPS.get(definition.mNameInternal);
            require(map!=null,"missing native map "+definition.mNameInternal);
            require(map.mViewerAllowed==definition.mViewerAllowed&&map.mShowVoltageAmperage==definition.mShowVoltageAmperage,"native flag projection "+map.mNameInternal);projected++;
            long expected=map.mRecipeList.stream().filter(r->r.mEnabled&&!r.mHidden).count();
            var type=RecipeMapCategory.recipeType(map);var registered=manager.getRecipeType(type.getUid()).isPresent();
            require(registered==(map.mViewerAllowed&&expected>0),"JEI category eligibility "+map.mNameInternal);
            if(!map.mViewerAllowed) {hidden++;retained+=map.mRecipeList.size();hiddenPools.addProperty(map.mNameInternal,map.mRecipeList.size());}
            if(registered) {
                long count=manager.createRecipeLookup(type).get().count();
                require(count==(EMI?0:expected),"JEI visible/bridge count "+map.mNameInternal+" "+count+"/"+expected);jeiRows+=count;categories++;
            }
            if(EMI) {
                var rows=actualRows.getOrDefault(map.mNameInternal,List.of());
                require(rows.size()==(map.mViewerAllowed?expected:0),"EMI rows or duplicate bridge "+map.mNameInternal+" "+rows.size()+"/"+expected);
                require(rows.stream().allMatch(com.gregtech.gregtech.emi.MachineEmiRecipe.class::isInstance),"unexpected JEI machine bridge "+map.mNameInternal);emiRows+=rows.size();
            }
        }
        require(projected==93&&hidden==6,"full source viewer catalog");
        require(MachineRecipeMaps.Assembler.mRecipeList.size()>0,"Assembler compatibility pool was deleted");
        require(manager.createRecipeLookup(com.gregtech.gregtech.jei.ToolAssemblyCategory.TYPE).get().count()>0,"manual tool assembly remains registered");
        result.addProperty("recipePresentationNativeMapsChecked",projected);result.addProperty("recipePresentationHiddenMapsChecked",hidden);
        result.add("recipePresentationRetainedHiddenPools",hiddenPools);result.addProperty("recipePresentationRetainedHiddenRows",retained);
        result.addProperty("recipePresentationCategories",categories);result.addProperty("recipePresentationJeiRows",jeiRows);result.addProperty("recipePresentationEmiRows",emiRows);result.addProperty("recipePresentationEmiAndJeiInstalled",EMI);
        receipt=result;checked=true;stage=1;frames=0;show();return false;
    }
    private static void show() {
        selected=stage==1?MachineRecipeMaps.Mortar:MachineRecipeMaps.ToolHeads;
        if(selected.mRecipeList.stream().noneMatch(r->r.mEnabled&&!r.mHidden))selected=MachineRecipeMaps.DidYouKnow;
        require(selected.mViewerAllowed&&selected.mRecipeList.stream().anyMatch(r->r.mEnabled&&!r.mHidden),"actual displayed source recipe");
        if(EMI) {
            var row=dev.emi.emi.api.EmiApi.getRecipeManager().getRecipes().stream().filter(r->r.getCategory().getId().getNamespace().equals("gregtech")&&r.getCategory().getId().getPath().equals(selected.mNameInternal)).findFirst().orElseThrow();
            dev.emi.emi.api.EmiApi.displayRecipe(row);
        } else {
            var row=selected.mRecipeList.stream().filter(r->r.mEnabled&&!r.mHidden).findFirst().orElseThrow();
            RecipePresentationJeiSmoke.runtime.getRecipesGui().showRecipes(RecipePresentationJeiSmoke.runtime.getRecipeManager().getRecipeCategory(RecipeMapCategory.recipeType(selected)),List.of(row),List.of());
        }
    }
    public static void screen(Minecraft minecraft,net.minecraft.client.gui.screens.Screen screen) {
        if(!checked||stage<1||stage>2||frames<0||failure!=null)return;
        if(!screen.getClass().getName().contains(EMI?"RecipeScreen":"RecipesGui"))return;
        if(EMI)try {
            if(!(boolean)Class.forName("dev.emi.emi.runtime.EmiReloadManager").getMethod("isLoaded").invoke(null)){frames=0;return;}
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        if(++frames<30)return;
        if(EMI) {
            try {
                var field=screen.getClass().getDeclaredField("currentPage");field.setAccessible(true);
                var groups=(java.util.List<?>)field.get(screen);
                int slots=0;
                for(var groupObject:groups) {
                  if(!(groupObject.getClass().getField("recipe").get(groupObject) instanceof com.gregtech.gregtech.emi.MachineEmiRecipe))continue;
                  var group=(dev.emi.emi.api.widget.WidgetHolder)groupObject;
                  for(var widget:(java.util.List<?>)groupObject.getClass().getField("widgets").get(groupObject)) {
                    if(!(widget instanceof dev.emi.emi.api.widget.SlotWidget slot))continue;
                    var b=slot.getBounds();require(b.left()>=0&&b.top()>=0&&b.right()<=group.getWidth()&&b.bottom()<=group.getHeight(),"actual fitted slot hitbox "+b+" / "+group.getWidth()+"x"+group.getHeight());
                    require(!slot.getStack().isEmpty()&&!slot.getTooltip(b.x()+b.width()/2,b.y()+b.height()/2).isEmpty(),"actual fitted slot tooltip");slots++;
                  }
                }
                require(slots>0,"actual fitted browser widgets");receipt.addProperty("recipePresentationFittedSlotsStage"+stage,slots);
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        int captured=stage;frames=-1;
        String file="source-machine-"+(EMI?"emi":"jei")+"-"+stage+"-"+UUID.randomUUID()+".png";
        Screenshot.grab(minecraft.gameDirectory,file,minecraft.getMainRenderTarget(),ignored->{
            try {
                var path=minecraft.gameDirectory.toPath().resolve("screenshots").resolve(file).toAbsolutePath();require(java.nio.file.Files.isRegularFile(path),"screenshot file");
                receipt.addProperty(captured==1?"sourcePoweredRecipeScreenshot":"sourceInformationalRecipeScreenshot",path.toString());
                minecraft.execute(()->{if(captured==1){stage=2;frames=0;show();}else{stage=3;minecraft.setScreen(null);}});
            }catch(Throwable e){failure=e;minecraft.execute(()->minecraft.setScreen(null));}
        });
    }
}
