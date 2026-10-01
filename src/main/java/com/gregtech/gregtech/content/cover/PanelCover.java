package com.gregtech.gregtech.content.cover;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Interactive GT6 attachments. Controller/detector conditions remain in MachineCoverSpec. */
public enum PanelCover {
    MANUAL("manual_selector"), REDSTONE("redstone_selector"), BUTTONS("button_panel_selector"),
    EMITTER("redstone_emitter"), PROGRESS("progress_sensor"), ENERGY("energy_sensor"),
    ENERGY_DISPLAY("energy_display_cover"), STATUS("machine_status_display_cover"),
    CONDUCTOR_IN("redstone_conductor_cover_accept"), CONDUCTOR_OUT("redstone_conductor_cover_emit"),
    CONTROLLER("cover_controller"), SHUTTER("shutter_cover");
    public final String id;
    PanelCover(String id){this.id=id;}
    public boolean selector(){return this==MANUAL||this==REDSTONE||this==BUTTONS;}
    public boolean energy(){return this==ENERGY||this==ENERGY_DISPLAY;}
    public boolean output(){return this==EMITTER||this==PROGRESS||this==ENERGY||this==CONDUCTOR_OUT;}
    public boolean strongConfig(){return output()&&this!=CONDUCTOR_OUT;}
    public boolean invertible(){return this==PROGRESS||this==ENERGY||this==CONTROLLER||this==SHUTTER;}
    public static PanelCover of(ItemStack stack){
        if(stack.isEmpty())return null;
        var key=ForgeRegistries.ITEMS.getKey(stack.getItem());
        if(key!=null&&key.getNamespace().equals("gregtech"))for(var cover:values())if(cover.id.equals(key.getPath()))return cover;
        return null;
    }
}
