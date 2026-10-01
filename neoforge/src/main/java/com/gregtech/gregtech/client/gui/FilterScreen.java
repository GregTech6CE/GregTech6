package com.gregtech.gregtech.client.gui;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
public class FilterScreen extends AbstractContainerScreen<FilterMenu> {
    private Button mode;
    public FilterScreen(FilterMenu menu,Inventory inventory,Component title) { super(menu,inventory,title);imageHeight=menu.prefixMode()?132:222;inventoryLabelY=menu.prefixMode()?38:128; }
    @Override protected void init() {
        super.init();mode=addRenderableWidget(Button.builder(modeText(),button->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+92,topPos+3,76,13).build());
    }
    private Component modeText() { return Component.translatable(menu.blacklist()?"gregtech.filter.blacklist":"gregtech.filter.whitelist"); }
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY) {
        var texture=ResourceLocation.parse("minecraft:textures/gui/container/generic_54.png");
        if(menu.prefixMode()){
            graphics.blit(texture,leftPos,topPos,0,0,imageWidth,35);
            graphics.fill(leftPos+7,topPos+17,leftPos+169,topPos+35,0xFFC6C6C6);
            graphics.blit(texture,leftPos+79,topPos+17,7,17,18,18);
            graphics.blit(texture,leftPos,topPos+35,0,126,imageWidth,96);return;
        }
        graphics.blit(texture,leftPos,topPos,0,0,imageWidth,125);
        graphics.blit(texture,leftPos,topPos+125,0,126,imageWidth,96);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial) { renderBackground(graphics,mouseX,mouseY,partial);mode.setMessage(modeText());super.render(graphics,mouseX,mouseY,partial);renderTooltip(graphics,mouseX,mouseY); }
}
