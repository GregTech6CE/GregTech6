package com.gregtech.gregtech.client.gui;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Uses the original GT6 texture, including its 16-slot store and five tool slots. */
public final class AdvancedCraftingScreen extends AbstractContainerScreen<AdvancedCraftingMenu> {
    public AdvancedCraftingScreen(AdvancedCraftingMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=166;}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){}
    @Override protected void renderBg(GuiGraphics graphics,float tick,int x,int y){
        graphics.blit(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",(menu.modes()&32)!=0?"textures/gui/machines/advanced_crafting_table_charging.png":"textures/gui/machines/advanced_crafting_table.png"),leftPos,topPos,0,0,imageWidth,imageHeight);
        var blueprint=menu.getSlot(0).getItem();
        if(minecraft != null && minecraft.level != null){
            var pattern=com.gregtech.gregtech.content.data.CraftingBlueprintData.readItem(minecraft.level,blueprint);
            for(int i=0;i<pattern.length;i++)if(!pattern[i].isEmpty()&&!menu.getSlot(22+i).hasItem()){
                var ghost=pattern[i];
                int sx=leftPos+80+i%3*18,sy=topPos+28+i/3*18;
                graphics.renderItem(ghost,sx,sy);graphics.fill(sx,sy,sx+16,sy+16,0x664080FF);
            }
        }
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float tick){super.render(graphics,x,y,tick);renderTooltip(graphics,x,y);
        if(hoveredSlot!=null){int slot=hoveredSlot.index;
            String key=slot==0?"gt.tooltip.advanced_crafting.blueprint":slot==AdvancedCraftingMenu.FLUSH?"gt.tooltip.advanced_crafting.flush":slot==AdvancedCraftingMenu.STORE?"gt.tooltip.advanced_crafting.store":slot==AdvancedCraftingMenu.RESULT?"gt.tooltip.advanced_crafting.craft":null;
            if(key!=null)graphics.renderTooltip(font,Component.translatable(key),x,y);
        }
    }
}
