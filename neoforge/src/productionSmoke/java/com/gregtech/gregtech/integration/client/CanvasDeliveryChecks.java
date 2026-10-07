package com.gregtech.gregtech.integration.client;
import com.google.gson.JsonObject;
import com.gregtech.gregtech.client.CanvasCoverRenderer;
import com.gregtech.gregtech.content.cover.*;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
/** Installed-model/color/name checks and the same directional sprite selector as world covers. */
final class CanvasDeliveryChecks {
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Canvas delivery: "+why);}
    static JsonObject capture(Minecraft client,GuiGraphics graphics){
        int meshes=0;graphics.pose().pushPose();graphics.pose().translate(0,0,600);
        graphics.fill(4,4,422,117,0xff14141c);graphics.drawString(client.font,"Canvas: 16 colors + copied block faces",8,8,0xffffff,false);
        for(var variant:CanvasRules.VARIANTS){
            var stack=new ItemStack(GTTechnological.get(variant.path()));var model=client.getItemRenderer().getModel(stack,null,null,0);int count=0;
            for(var pass:model.getRenderPasses(stack,false))for(var layer:pass.getRenderTypes(stack,false))for(int side=-1;side<6;side++)for(var quad:pass.getQuads(null,side<0?null:Direction.from3DDataValue(side),RandomSource.create(1),net.neoforged.neoforge.client.model.data.ModelData.EMPTY,layer)){
                require(quad.getSprite().contents().name().equals(ResourceLocation.fromNamespaceAndPath("gregtech","item/canvas/"+variant.dye())),"original colored item sprite "+variant.path());count++;
            }
            require(count>0,"nonempty item mesh "+variant.path());meshes++;
            require(!stack.getHoverName().getString().contains("item.gregtech."),"localized canvas name");
            int i=variant.originalId()-7030,x=8+(i%8)*50,y=24+(i/8)*28;graphics.renderItem(stack,x+16,y);graphics.drawString(client.font,client.font.plainSubstrByWidth(stack.getHoverName().getString(),48),x,y+17,0xffffff,false);
        }
        var equipment=new java.util.ArrayList<>(com.gregtech.gregtech.loaders.b.OriginCreativeContents.contents("equipment"));
        int first=-1,magic=-1;
        for(int i=0;i<equipment.size();i++) { var id=BuiltInRegistries.ITEM.getKey(equipment.get(i).getItem()).getPath();if(id.equals("canvas_black"))first=i;if(id.equals("magic_research_paper_introduction"))magic=i; }
        require(first>=0&&magic>first+15,"equipment page places canvas before research papers");
        for(int i=0;i<16;i++) require(BuiltInRegistries.ITEM.getKey(equipment.get(first+i).getItem()).getPath().equals(CanvasRules.VARIANTS.get(i).path()),"original sixteen-color creative order");
        var printed=new ItemStack(GTTechnological.get("canvas_white"));CanvasData.write(printed,CanvasData.fromState(Blocks.CRAFTING_TABLE.defaultBlockState()));
        var top=CanvasCoverRenderer.imageFace(printed,Direction.UP,null,null);var east=CanvasCoverRenderer.imageFace(printed,Direction.EAST,null,null);var west=CanvasCoverRenderer.imageFace(printed,Direction.WEST,null,null);
        require(top!=null&&top.sprite().contents().name().getPath().equals("block/crafting_table_top"),"top face uses top texture");
        require(east!=null&&!top.sprite().contents().name().equals(east.sprite().contents().name()),"side face differs from top rather than particle sprite");
        require(west!=null,"opposite face resolved");
        graphics.blit(12,87,0,22,22,top.sprite());graphics.blit(42,87,0,22,22,east.sprite());graphics.blit(72,87,0,22,22,west.sprite());
        CanvasData.write(printed,CanvasData.fromState(Blocks.WATER.defaultBlockState()));var water=CanvasCoverRenderer.imageFace(printed,Direction.NORTH,null,null);require(water!=null&&!water.sprite().contents().name().getPath().contains("missing"),"mapped water bucket has fluid image");
        graphics.blit(102,87,0,22,22,water.sprite());
        CanvasData.write(printed,CanvasData.fromState(Blocks.GLOWSTONE.defaultBlockState()));require(CanvasCoverRenderer.imageFace(printed,Direction.SOUTH,null,null).bright(),"glowstone image retains bright rendering");
        int pageModels=0;
        for(String path:java.util.List.of("printed_pages","many_printed_pages")) {
            var pages=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",path)));
            require(pages.getItem() instanceof com.gregtech.gregtech.item.PrintedPagesItem,"native printed-page behavior "+path);
            var model=client.getItemRenderer().getModel(pages,null,null,0);
            int count=0;
            for(var pass:model.getRenderPasses(pages,false))for(var layer:pass.getRenderTypes(pages,false))for(int side=-1;side<6;side++)for(var quad:pass.getQuads(null,side<0?null:Direction.from3DDataValue(side),RandomSource.create(1),net.neoforged.neoforge.client.model.data.ModelData.EMPTY,layer)) {
                require(quad.getSprite().contents().name().equals(ResourceLocation.fromNamespaceAndPath("gregtech","item/randomtools/"+path)),"original paper bundle sprite "+path);count++;
            }
            require(count>0,"visible printed-page item mesh "+path);
            require(!pages.getHoverName().getString().contains("item.gregtech."),"localized page name "+path);
            graphics.renderItem(pages,140+pageModels*30,88);pageModels++;
        }
        int bookHints=0;
        for(var variant:com.gregtech.gregtech.content.book.ColoredBookRules.VARIANTS) {
            var book=com.gregtech.gregtech.registry.GTColoredBooks.stack(variant.originalId());
            require(com.gregtech.gregtech.data.MachineRecipeMaps.ScannerVisuals.mRecipeList.stream().anyMatch(r->r.mFakeRecipe&&r.mDuration==512&&r.mEUt==16&&r.mInputs.length==2&&r.mInputs[0].is(book.getItem())),"source scanner viewer row "+variant.path());bookHints++;
        }
        for(String path:java.util.List.of("printed_pages","many_printed_pages")) {
            var item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",path));
            require(com.gregtech.gregtech.data.MachineRecipeMaps.Printer.mRecipeList.stream().anyMatch(r->r.mFakeRecipe&&r.mOutputs.length==1&&r.mOutputs[0].is(item)&&r.mEUt==16&&r.mDuration==(path.equals("printed_pages")?512:1024)),"source printer viewer row "+path);
        }
        var iron=com.gregtech.gregtech.api.material.GTMaterialRegistry.get(260).resolve();
        var dataStick=com.gregtech.gregtech.content.recipe.GTMaterialDataRecipes.withMaterialData(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech","usb3_stick"))),iron);
        var matter=java.util.List.of(com.gregtech.gregtech.registry.GTFluids.stack("MatterNeutral",(int)iron.getNeutrons()),com.gregtech.gregtech.registry.GTFluids.stack("MatterCharged",(int)iron.getProtons()));
        var replication=com.gregtech.gregtech.data.MachineRecipeMaps.Replicator.findRecipe(java.util.List.of(dataStick),matter,false,3,3);
        require(replication!=null&&replication.mEUt==1&&replication.mDuration==(iron.getProtons()+iron.getNeutrons())*256L&&!replication.mCanBeBuffered,"installed original molecular work fields");
        require(!com.gregtech.gregtech.api.material.GTMaterialRegistry.get(8214).has(com.gregtech.gregtech.api.material.MaterialProperty.UUM),"installed obsidian source gate");
        var mercury=com.gregtech.gregtech.registry.GTFluids.stack("GenLiquid_Mercury",1000);
        require(mercury!=null&&!mercury.isEmpty()&&BuiltInRegistries.FLUID.getKey(mercury.getFluid()).getPath().equals("mercury"),"installed ordinary mercury liquid");
        int electricalRows=0;
        for(int tier=0;tier<10;tier++) {
            var key="gregtech:circuits_tier_"+tier+"_plus";
            require(com.gregtech.gregtech.data.MachineRecipeIngredients.resolve("circuit:"+tier,iron,1).equals(java.util.Map.of("tag",key)),"installed original circuit grade "+tier);
            String path="data/gregtech/tags/items/circuits_tier_"+tier+"_plus.json";
            try(var stream=CanvasDeliveryChecks.class.getClassLoader().getResourceAsStream(path)) {
                require(stream!=null,"shared installed circuit tag "+tier);
                var tag=com.google.gson.JsonParser.parseString(new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                require(!tag.get("replace").getAsBoolean(),"external circuit provider can merge");
                boolean external=false,next=tier==9;
                for(var value:tag.getAsJsonArray("values")) {
                    if(value.isJsonObject()&&value.getAsJsonObject().get("id").getAsString().equals("#gt:circuit"+tier))external=true;
                    if(value.isJsonPrimitive()&&value.getAsString().equals("#gregtech:circuits_tier_"+(tier+1)+"_plus"))next=true;
                }
                require(external&&next,"installed external circuit key and cumulative inheritance");
            }catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
        }
        for(String suffix:java.util.List.of("zpm","uv","xv"))for(boolean large:new boolean[]{false,true}) {
            if(large&&suffix.equals("xv"))continue;
            String id=(large?"energy_storage_":"battery_box_")+suffix;
            require(com.gregtech.gregtech.content.recipe.EquipmentCraftingCatalog.FILES.contains("energy_nodes/"+id+".json"),"native recipe pack includes high box "+id);
            var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech",id)));
            require(stack.getItem() instanceof net.minecraft.world.item.BlockItem b&&b.getBlock() instanceof com.gregtech.gregtech.block.energy.EnergyNodeBlock node&&node.spec().batterySlots()==(large?16:4),"installed high box original slots "+id);electricalRows++;
        }
        graphics.pose().popPose();var result=new JsonObject();result.addProperty("originalCircuitTagGrades",10);result.addProperty("externalHighBatteryBoxRecipes",electricalRows);result.addProperty("originalMolecularDataFields",true);result.addProperty("mercuryAmbientUnitMb",1000);result.addProperty("originalCanvasItemModels",meshes);result.addProperty("printedPageModels",pageModels);result.addProperty("coloredBookScanHints",bookHints);result.addProperty("directionalAndFluidFaces",4);result.addProperty("brightImage",true);return result;
    }
}
