package com.gregtech.gregtech.content.energy;
import com.gregtech.gregtech.content.material.Materials;import com.gregtech.gregtech.api.material.GTMaterial;import com.gregtech.gregtech.api.machine.PumpSpec;
public final class PumpCatalog {private PumpCatalog(){}
    private record Tier(String name, GTMaterial mat, long speed) {}
    private static final Tier[] TIERS = {
                new Tier("bronze", Materials.Bronze, 32),
                new Tier("steel", Materials.Steel, 128),
                new Tier("titanium", Materials.Titanium, 512),
                new Tier("tungstensteel", Materials.Tungstensteel, 2048),
        };

public static java.util.List<PumpSpec> all(){var out=new java.util.ArrayList<PumpSpec>();for(int i=0;i<TIERS.length;i++){var t=TIERS[i];out.add(new PumpSpec("rotational_pump_"+t.name(),t.mat(),t.speed(),i+1));}return java.util.List.copyOf(out);}}
