package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.tool.ElectricToolAssembly;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.item.*;
import com.gregtech.gregtech.recipe.ToolShapedRecipe;
import com.gregtech.gregtech.registry.*;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class ElectricToolAssemblyTests {
    @GameTest(template="test_empty")
    public static void originalLvPatternsMaterialsAndToolRemainders(GameTestHelper h) {
        for(var spec:ElectricToolAssembly.values()) {
            var recipe=spec.recipe(Materials.Steel);
            h.assertTrue(recipe!=null,"steel assembly exists: "+spec);
            var grid=grid(recipe);
            int toolCount=0;
            for(int i=0;i<9;i++) if(grid.getItem(i).getItem() instanceof GTToolItem tool) {
                grid.setItem(i,GTToolHelper.displayTool(tool.toolType())); toolCount++;
            }
            h.assertTrue(recipe.matches(grid,h.getLevel()),"GT6 pattern matches: "+spec);
            var result=recipe.assemble(grid,h.getLevel().registryAccess());
            h.assertTrue(result.is(spec.item())&&spec.item().headMaterial(result)==Materials.Steel,"head retained");
            h.assertTrue(spec.item().getEnergyCapacity(result,GregTechTags.Energy.EU)==100000,"LV battery capacity retained even for wrench/screwdriver");
            h.assertTrue(spec.item().getEnergyStored(result,GregTechTags.Energy.EU)==0,"assembly starts uncharged");
            var remaining=recipe.getRemainingItems(grid);
            int returned=0;
            for(int i=0;i<9;i++) if(grid.getItem(i).getItem() instanceof GTToolItem tool) {
                h.assertTrue(remaining.get(i).is(tool)&&remaining.get(i).getDamageValue()==tool.toolType().damagePerCraft(),"crafting tool returned with GT6 wear"); returned++;
            } else h.assertTrue(remaining.get(i).isEmpty(),"parts and battery consumed");
            h.assertTrue(returned==toolCount&&toolCount==(spec==ElectricToolAssembly.DRILL?2:1),"correct tool count");
            if(spec==ElectricToolAssembly.DRILL) {
                h.assertTrue(grid.getItem(1).is(GTItems.getStack(MaterialPrefix.stick,Materials.Steel).getItem()),"LV drill uses rod, not drill head");
                grid.setItem(3,GTItems.getStack(MaterialPrefix.screw,Materials.Titanium));
                h.assertTrue(!recipe.matches(grid,h.getLevel()),"cannot mix rod/head material with a different screw");
            } else {
                grid.setItem(spec==ElectricToolAssembly.SCREWDRIVER?6:7,new ItemStack(GTElectricItems.BATTERY_MV.get()));
                h.assertTrue(!recipe.matches(grid,h.getLevel()),"MV battery is not an LV battery");
            }
            grid=grid(recipe);
            for(int y=0;y<3;y++) {var first=grid.getItem(y*3);grid.setItem(y*3,grid.getItem(y*3+2));grid.setItem(y*3+2,first);}
            h.assertTrue(!recipe.matches(grid,h.getLevel()),"GT6 row has no mirroring");
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void generatedRowsReachServerAndNetwork(GameTestHelper h) {
        var recipes=ElectricToolAssembly.build();
        h.assertTrue(recipes.size()>4,"more than a fixed steel-only recipe");
        for(var recipe:recipes) {
            h.assertTrue(h.getLevel().getRecipeManager().byKey(recipe.getId()).isPresent(),"server registered "+recipe.getId());
            var buf=new FriendlyByteBuf(Unpooled.buffer());
            try {
                ToolShapedRecipe.SERIALIZER.toNetwork(buf,recipe);
                int end=buf.writerIndex();buf.writeInt(0x13572468);
                var copy=ToolShapedRecipe.SERIALIZER.fromNetwork(recipe.getId(),buf);
                h.assertTrue(buf.readerIndex()==end&&buf.readInt()==0x13572468,"packet boundary intact");
                h.assertTrue(!copy.allowMirror()&&ItemStack.matches(copy.getResultItem(h.getLevel().registryAccess()),recipe.getResultItem(h.getLevel().registryAccess())),"material/capacity NBT reaches client recipe/JEI");
            } finally {buf.release();}
        }
        for(String name:new String[]{"Wood","Rubber","Plastic","Teflon","PVC","Bakelite","HardPlastic"}) {
            var material=GTMaterialRegistry.get(name);
            for(var spec:ElectricToolAssembly.values()) h.assertTrue(spec.recipe(material)==null,"GT6 excludes "+name);
        }
        h.succeed();
    }

    @GameTest(template="test_empty")
    public static void capacityTintAndEnergyArithmetic(GameTestHelper h) {
        var tool=GTElectricItems.ELECTRIC_DRILL.get();
        var stack=tool.assembled(Materials.Titanium,76543);
        h.assertTrue(tool.tint(stack,2)==Materials.Titanium.getColor()&&tool.tint(stack,0)==GTMaterialRegistry.get("Orange").getColor()&&tool.tint(stack,1)==0xFFFFFF,"material head, LV orange body, untinted overlay");
        long packets=tool.doEnergyInjection(GregTechTags.Energy.EU,stack,32,Long.MAX_VALUE,h.getLevel(),h.absolutePos(net.minecraft.core.BlockPos.ZERO),false);
        h.assertTrue(packets==64&&tool.getEnergyStored(stack,GregTechTags.Energy.EU)==0,"simulation and multiplication overflow safe");
        tool.doEnergyInjection(GregTechTags.Energy.EU,stack,32,Long.MAX_VALUE,null,null,true);
        h.assertTrue(tool.getEnergyStored(stack,GregTechTags.Energy.EU)==2048,"original tool accepts at most 64 packets");
        var copy=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));
        h.assertTrue(tool.getEnergyCapacity(copy,GregTechTags.Energy.EU)==76543&&tool.headMaterial(copy)==Materials.Titanium,"material and capacity survive save");
        h.assertTrue(tool.consumeEnergy(copy)&&tool.getEnergyStored(copy,GregTechTags.Energy.EU)==1948,"drill use still consumes 100 EU");
        h.assertTrue(tool.doEnergyExtraction(GregTechTags.Energy.EU,copy,32,Long.MAX_VALUE,null,null,true)==0&&tool.getEnergyStored(copy,GregTechTags.Energy.EU)==1948,"GT6 tools cannot discharge into networks");
        copy.getOrCreateTag().putLong("gt.charge",Long.MAX_VALUE);
        h.assertTrue(tool.getBarWidth(copy)==13,"overcapacity NBT clamped");
        copy.getOrCreateTag().putLong("gt.charge",-1);
        h.assertTrue(tool.getEnergyStored(copy,GregTechTags.Energy.EU)==0,"negative NBT cannot become negative charge");
        h.succeed();
    }

    private static TransientCraftingContainer grid(ToolShapedRecipe recipe) {
        var menu=new AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        };
        var grid=new TransientCraftingContainer(menu,3,3);
        for(int i=0;i<9;i++) grid.setItem(i,recipe.getIngredients().get(i).getItems()[0].copy());
        return grid;
    }
}
