package com.gregtech.gregtech.content.logistics;

/** Source Loader_MultiTileEntities 30000-30002 / 30500-30502. */
public enum ExtenderSpec {
    INVENTORY("inventory_extender","inv",true,false,false,30000),
    TANK("tank_extender","tank",false,true,false,30001),
    COMBINED("inventory_tank_extender","inv_tank",true,true,false,30002),
    INVENTORY_BRIDGE("inventory_bridge","bridge_inv",true,false,true,30500),
    TANK_BRIDGE("tank_bridge","bridge_tank",false,true,true,30501),
    COMBINED_BRIDGE("inventory_tank_bridge","bridge_inv_tank",true,true,true,30502),
    UNIVERSAL("universal_extender","universal",true,true,false,30255),
    UNIVERSAL_BRIDGE("universal_bridge","bridge_universal",true,true,true,30755);
    public final String id,texture;
    public final boolean items,fluids,bridge;
    public final int originalId;
    public boolean universal(){return this==UNIVERSAL||this==UNIVERSAL_BRIDGE;}
    public com.gregtech.gregtech.api.material.GTMaterial material(){return universal()?com.gregtech.gregtech.content.material.Materials.StainlessSteel:com.gregtech.gregtech.content.material.Materials.Steel;}
    ExtenderSpec(String id,String texture,boolean items,boolean fluids,boolean bridge,int originalId) {
        this.id=id;this.texture=texture;this.items=items;this.fluids=fluids;this.bridge=bridge;this.originalId=originalId;
    }
}
