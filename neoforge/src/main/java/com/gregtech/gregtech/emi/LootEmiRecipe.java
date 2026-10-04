package com.gregtech.gregtech.emi;
import com.gregtech.gregtech.content.loot.LootViewerData;
import com.gregtech.gregtech.client.LootPageCaptions;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;
/** One scrollable source page, with all outputs indexed and per-rule tooltips. */
public final class LootEmiRecipe implements EmiRecipe {
    private final EmiRecipeCategory category;
    private final LootViewerData.Group group;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    public LootEmiRecipe(EmiRecipeCategory category, LootViewerData.Group group) {
        this.category=category; this.group=group;
        inputs=List.of(EmiIngredient.of(group.inputs().stream().map(EmiStack::of).toList()));
        outputs=group.rows().stream().map(row -> EmiStack.of(row.output())).toList();
    }
    public LootViewerData.Group group() { return group; }
    public static void register(EmiRegistry registry) {
        for (boolean mobs : new boolean[]{false,true}) {
            String path=mobs?"mob_drops":"loot_tables";
            var category=new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("gregtech",path),EmiStack.of(new ItemStack(mobs?Items.ZOMBIE_SPAWN_EGG:Items.CHEST))) {
                @Override public Component getName() { return Component.translatable("gregtech.jei.category."+path); }
            };
            registry.addCategory(category);
            for (var group : mobs?LootViewerData.mobGroups():LootViewerData.lootGroups()) registry.addRecipe(new LootEmiRecipe(category,group));
        }
    }
    @Override public EmiRecipeCategory getCategory() { return category; }
    @Override public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath("gregtech","/loot/"+group.id()); }
    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public boolean supportsRecipeTree() { return false; }
    @Override public boolean hideCraftable() { return true; }
    @Override public int getDisplayWidth() { return LootPageCaptions.WIDTH; }
    @Override public int getDisplayHeight() { return LootPageCaptions.HEIGHT; }
    @Override public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0),2,18);
        int height=widgets.getHeight();
        int visibleRows=Math.max(1,Math.min(LootPageCaptions.VISIBLE_ROWS,(height-54)/18));
        var grid=new Grid(visibleRows);
        for(int i=0;i<LootPageCaptions.COLUMNS*visibleRows;i++) {
            final int cell=i;
            widgets.add(new SlotWidget(EmiStack.EMPTY,LootPageCaptions.GRID_X+i%9*18,LootPageCaptions.GRID_Y+i/9*18) {
                @Override public EmiIngredient getStack() { int index=grid.offset*9+cell; return index<outputs.size()?outputs.get(index):EmiStack.EMPTY; }
                @Override public List<ClientTooltipComponent> getTooltip(int x,int y) {
                    int index=grid.offset*9+cell;
                    if(index>=outputs.size())return List.of();
                    var lines=new ArrayList<>(super.getTooltip(x,y));
                    for(var line:LootPageCaptions.lines(group.rows().get(index))) lines.add(ClientTooltipComponent.create(line.getVisualOrderText()));
                    return lines;
                }
            }.recipeContext(this));
        }
        widgets.add(grid);
        widgets.addDrawable(0,0,getDisplayWidth(),height,(g,x,y,d)->LootPageCaptions.draw(g,group,height));
    }
    private final class Grid extends EmiInteractiveWidget {
        int offset;
        double dragOffset;
        final int max, gridHeight, travel;
        Grid(int visibleRows) {max=Math.max(0,(outputs.size()+8)/9-visibleRows);gridHeight=visibleRows*18;travel=gridHeight-14;}
        @Override public Bounds getBounds() { return new Bounds(2,40,172,gridHeight); }
        @Override public void render(GuiGraphics g,int x,int y,float delta) {
            EmiWidgetInput.rendered(this,g);
            if(max==0)return;
            g.fill(166,40,173,40+gridHeight,0xff777777);
            int top=40+(int)(offset*(double)travel/max);
            g.fill(167,top,172,top+14,0xffcccccc);
        }
        @Override public boolean scrolled(double x,double y,double delta) { if(delta==0)return false; offset=Math.max(0,Math.min(max,offset+(delta>0?-1:1)));dragOffset=offset;return true; }
        @Override public boolean pressed(double x,double y,int button) {
            if(button!=0||x<166||max==0)return false;
            dragOffset=Math.max(0,Math.min(max,(y-47)*max/travel));offset=(int)Math.round(dragOffset);return true;
        }
        @Override public boolean dragged(int button,double dx,double dy) { if(button!=0)return false; dragOffset=Math.max(0,Math.min(max,dragOffset+dy*max/travel));offset=(int)Math.round(dragOffset);return true; }
    }
}
