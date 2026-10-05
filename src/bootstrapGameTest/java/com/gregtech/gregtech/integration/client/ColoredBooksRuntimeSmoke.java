package com.gregtech.gregtech.integration.client;

import com.google.gson.JsonObject;
import com.gregtech.gregtech.content.book.*;
import com.gregtech.gregtech.registry.*;
import com.gregtech.gregtech.item.ColoredBookItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import java.util.*;

/** Finite native book checks, run beside the provided-device coke oven world check. */
final class ColoredBooksRuntimeSmoke {
    private static JsonObject receipt;
    private static int phase,frames;
    private static volatile Throwable failure;
    private static boolean screenDone,modelsDone;
    private static final String MANUAL="Manual_Steam";
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static ResourceLocation id(String path){return new ResourceLocation("gregtech",path);}
    private static ItemStack payload(ItemStack target,ItemStack source){if(source.hasTag())target.setTag(source.getTag().copy());return target;}
    private static int generation(ItemStack stack){return WrittenBookItem.getGeneration(stack);}
    private static void generation(ItemStack stack,int value){stack.getOrCreateTag().putInt("generation",value);}
    private static Object data(ItemStack stack){return stack.getTag();}
    private static net.minecraft.world.inventory.CraftingContainer input(List<ItemStack> stacks){
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        },3,3);
        for(int i=0;i<stacks.size();i++)grid.setItem(i,stacks.get(i));return grid;
    }
    private static CraftingRecipe recipe(net.minecraft.server.MinecraftServer server,String path){return (CraftingRecipe)server.getRecipeManager().byKey(id("books/"+path)).orElseThrow(()->new IllegalStateException("Missing native book recipe "+path));}
    static void server(net.minecraft.server.MinecraftServer server,JsonObject result){
        receipt=result;var player=server.getPlayerList().getPlayers().get(0);var level=player.serverLevel();
        int bindings=0,copies=0,covers=0,textPages=0;var written=GTBooks.bookStack(MANUAL);generation(written,0);
        require(!written.isEmpty(),"Missing steam manual");
        for(var variant:ColoredBookRules.VARIANTS){
            var stack=GTColoredBooks.stack(variant.originalId());require(stack.getItem() instanceof ColoredBookItem,"Missing cover "+variant.path());
            var book=(ColoredBookItem)stack.getItem();var composition=com.gregtech.gregtech.api.material.ItemMaterialRegistry.get(stack).orElseThrow();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack.copy());
            require(book.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND).getResult()==net.minecraft.world.InteractionResult.PASS,"Untitled cover opens invalid native book "+variant.path());
            require(composition.components().get(0).amount()==com.gregtech.gregtech.api.material.GTValues.U*(variant.large()?6:3),"Paper composition "+variant.path());
            require(composition.components().size()==(variant.emblem().equals("bronze")||variant.emblem().equals("radiation")?2:1),"Emblem composition "+variant.path());
            require(book.getBurnTime(stack,RecipeType.SMELTING)==(variant.large()||variant.emblem().equals("radiation")?400:200),"Source burn time "+variant.path());
            require(BookShelfRules.enchantPower("gregtech:"+variant.path())==1,"Shelf rejects book "+variant.path());
            require(stack.is(net.minecraft.tags.ItemTags.LECTERN_BOOKS)&&stack.is(net.minecraft.tags.ItemTags.BOOKSHELF_BOOKS),"Native book tags missing "+variant.path());
            var conversion=com.gregtech.gregtech.data.MachineRecipeMaps.Generifier.findRecipe(List.of(stack),List.of(),false,1,9);
            require(conversion!=null&&conversion.mOutputs[0].is(Items.WRITTEN_BOOK)&&conversion.mDuration==1&&conversion.mEUt==0,"Missing source generifier "+variant.path());
            if(!variant.hidden())for(String group:List.of("empty","recolor","printed")){
                var recipe=recipe(server,group+"/"+variant.path());var stacks=new ArrayList<ItemStack>();
                for(var ingredient:recipe.getIngredients()){
                    require(ingredient.getItems().length>0,"Unbound native ingredient "+group+"/"+variant.path());
                    stacks.add(ingredient.getItems()[0].copy());
                }
                if(group.equals("recolor"))stacks.set(0,payload(stack.copy(),written));
                if(group.equals("printed"))stacks.set(0,payload(stacks.get(0),written));
                var grid=input(stacks);require(recipe.matches(grid,level),"Binding match "+group+"/"+variant.path());
                var output=recipe.assemble(grid,level.registryAccess());require(output.is(stack.getItem())&&output.getCount()==1,"Binding result "+variant.path());
                if(!group.equals("empty"))require(Objects.equals(data(output),data(written)),"Binding loses content "+variant.path());
                bindings++;
            }
            for(int count=1;count<=8;count++){
                var recipe=recipe(server,"copy/"+variant.path()+"/"+count);var source=payload(stack.copy(),written);
                var stacks=new ArrayList<ItemStack>();stacks.add(source);for(int j=0;j<count;j++)stacks.add(new ItemStack(Items.WRITABLE_BOOK));
                var grid=input(stacks);require(recipe.matches(grid,level),"Copy match "+variant.path());
                var output=recipe.assemble(grid,level.registryAccess());require(output.is(stack.getItem())&&output.getCount()==count&&generation(output)==1,"Copy result "+variant.path());
                require(Objects.equals(data(payload(stack.copy(),output)),data(output)),"Copy payload inaccessible");
                var comparable=output.copy();generation(comparable,0);
                require(Objects.equals(data(comparable),data(source)),"Copy loses pages/custom data "+variant.path());
                var remaining=recipe.getRemainingItems(grid);require(remaining.get(0).getCount()==1&&Objects.equals(data(remaining.get(0)),data(source)),"Copy loses original "+variant.path());
                generation(source,2);require(!recipe.matches(grid,level)&&recipe.assemble(grid,level.registryAccess()).isEmpty(),"Copy generation limit "+variant.path());copies++;
            }
            covers++;
        }
        int manuals=0;for(String name:GTBooks.names()){
            var stack=GTBooks.bookStack(name);require(stack.is(GTColoredBooks.stack(ColoredBookRules.manualCover(name,GTBooks.pagesOf(name).size())).getItem()),"Wrong source manual cover "+name);manuals++;
            var access=new net.minecraft.client.gui.screens.inventory.BookViewScreen.WrittenBookAccess(stack);
            require(access.getPageCount()==GTBooks.pagesOf(name).size(),"Native manual page count "+name);
            for(int p=0;p<access.getPageCount();p++){require(access.getPageRaw(p).getString().equals(GTBooks.pagesOf(name).get(p)),"Native manual text changed "+name+" page "+p);textPages++;}
        }
        var material=com.gregtech.gregtech.content.material.Materials.Iron;var dictionary=GTMaterialDictionary.bookStack(material);
        require(dictionary.getItem() instanceof ColoredBookItem&&GTMaterialDictionary.materialOf(dictionary)==material,"Dictionary metadata missing");
        var dictionaryAccess=new net.minecraft.client.gui.screens.inventory.BookViewScreen.WrittenBookAccess(dictionary);
        for(int p=0;p<dictionaryAccess.getPageCount();p++)require(dictionaryAccess.getPageRaw(p).getString().equals(GTMaterialDictionary.pages(material).get(p)),"Dictionary text changed "+p);
        // Real native placement/resolve/page/retrieval path on a supplied lectern.
        var pos=player.blockPosition().offset(3,13,-4);level.setBlock(pos,Blocks.LECTERN.defaultBlockState(),3);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,written.copy());var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        require(player.getMainHandItem().getItem().useOn(context).consumesAction(),"Native lectern placement rejected cover");
        var lectern=(LecternBlockEntity)level.getBlockEntity(pos);require(lectern.hasBook()&&lectern.getBook().is(written.getItem())&&level.getBlockState(pos).getValue(LecternBlock.HAS_BOOK),"Lectern did not resolve native book");
        require(lectern.getRedstoneSignal()==1,"Lectern first page signal");
        var reader=new net.minecraft.client.gui.screens.inventory.BookViewScreen.WrittenBookAccess(lectern.getBook());
        require(reader.getPageCount()==GTBooks.pagesOf(MANUAL).size(),"Lectern lost pages");
        var recovered=lectern.getBook().copy();lectern.setBook(ItemStack.EMPTY);
        require(recovered.is(written.getItem())&&!lectern.hasBook(),"Lectern retrieval loses identity");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        result.addProperty("coloredBookNativeCovers",covers);result.addProperty("coloredBookBindingRows",bindings);result.addProperty("coloredBookCloneRows",copies);
        result.addProperty("coloredBookManualCovers",manuals);result.addProperty("coloredBookLecternChecks",4);result.addProperty("coloredBookRecipesLoaded",BookCraftingCatalog.FILES.size());
        result.addProperty("coloredBookTextPagesVerified",textPages);result.addProperty("coloredBookEmptyUseChecks",28);
    }
    static boolean frame(Minecraft mc,JsonObject result){
        receipt=result;if(failure!=null)throw new IllegalStateException("Native book reader failed",failure);
        if(!modelsDone){client(mc);modelsDone=true;}
        if(phase==0){phase=1;mc.getSingleplayerServer().execute(()->{
            var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,GTBooks.bookStack(MANUAL));player.inventoryMenu.broadcastChanges();
        });return false;}
        if(phase==1&&mc.player.getMainHandItem().is(GTBooks.bookStack(MANUAL).getItem())){
            if(++frames<20)return false;phase=2;mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);return false;
        }
        return screenDone;
    }
    private static void client(Minecraft mc){
        int models=0;for(var variant:ColoredBookRules.VARIANTS){
            var stack=GTColoredBooks.stack(variant.originalId());var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);int quads=0;
            for(var pass:model.getRenderPasses(stack,false))for(var type:pass.getRenderTypes(stack,false))for(int face=-1;face<6;face++)
                for(var quad:pass.getQuads(null,face<0?null:Direction.from3DDataValue(face),net.minecraft.util.RandomSource.create(1),net.minecraftforge.client.model.data.ModelData.EMPTY,type)){
                    require(!quad.getSprite().contents().name().getPath().contains("missing"),"Missing book sprite "+variant.path());quads++;
                }
            require(quads>0&&!stack.getHoverName().getString().startsWith("item.gregtech"),"Transparent/untranslated book "+variant.path());models++;
        }
        var visible=com.gregtech.gregtech.loaders.b.OriginCreativeContents.pages().get("books");
        for(var variant:ColoredBookRules.VARIANTS)require(!GTColoredBooks.stack(variant.originalId()).hasFoil(),"Vanilla book glint alters source cover "+variant.path());
        require(visible!=null&&visible.stream().filter(s->s.getItem() instanceof ColoredBookItem).count()==22,"Wrong original visible book family");
        var coke=new ItemStack(GTMultiblocks.COKE_OVEN_MAIN.get());var model=mc.getItemRenderer().getModel(coke,mc.level,mc.player,0);
        require(model.getQuads(null,null,net.minecraft.util.RandomSource.create(1)).size()>0||java.util.Arrays.stream(Direction.values()).anyMatch(d->!model.getQuads(null,d,net.minecraft.util.RandomSource.create(1)).isEmpty()),"Transparent coke icon");
        receipt.addProperty("coloredBookInventoryModels",models);receipt.addProperty("coloredBookCreativeCovers",22);receipt.addProperty("cokeInventoryPreview",true);
    }
    static void screen(Minecraft mc,net.minecraft.client.gui.screens.Screen screen){
        if(phase!=2||screenDone)return;
        try{
            require(screen instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen,"Wrong native book screen "+screen.getClass());
            var access=new net.minecraft.client.gui.screens.inventory.BookViewScreen.WrittenBookAccess(mc.player.getMainHandItem());
            require(access.getPageCount()==GTBooks.pagesOf(MANUAL).size(),"Native reader lost source pages");
            require(access.getPageRaw(0).getString().equals(GTBooks.pagesOf(MANUAL).get(0)),"Native reader truncated the source paragraph");
            receipt.addProperty("coloredBookFirstPageTextVerified",true);
            if(++frames<40)return;
            String file="colored-book-reader-"+java.util.UUID.randomUUID()+".png";
            Screenshot.grab(mc.gameDirectory,file,mc.getMainRenderTarget(),ignored->{
                try{var path=mc.gameDirectory.toPath().resolve("screenshots").resolve(file).toAbsolutePath();require(java.nio.file.Files.isRegularFile(path),"Book screenshot missing");receipt.addProperty("coloredBookReaderScreenshot",path.toString());receipt.addProperty("coloredBookReaderPages",access.getPageCount());screenDone=true;mc.execute(()->mc.setScreen(null));}
                catch(Throwable e){failure=e;}
            });phase=3;
        }catch(Throwable e){failure=e;}
    }
    static void renderInventory(net.minecraft.client.gui.GuiGraphics graphics,Minecraft mc){
        int x=8,y=8,index=0;graphics.fill(x-4,y-4,x+195,y+110,0xDD202020);graphics.drawString(mc.font,"GT6 original book covers",x,y,0xFFFFFF);
        for(var variant:ColoredBookRules.VARIANTS){graphics.renderItem(GTColoredBooks.stack(variant.originalId()),x+(index%11)*17,y+14+(index/11)*24);index++;}
        graphics.renderItem(new ItemStack(GTMultiblocks.COKE_OVEN_MAIN.get()),x,y+88);
    }
}
