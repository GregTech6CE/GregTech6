package com.gregtech.gregtech.content.multiblock;

import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GT6 {@code Loader_MultiTileEntities} 17001–17067. The original numeric IDs
 * remain at this registration boundary; each playable valve uses its own wall
 * design and fluid limits rather than a generic tank tier.
 */
public record TankValveSpec(int originalId, int size, int wallId, long capacity,
                            String material, boolean gasProof, boolean acidProof,
                            boolean plasmaProof, boolean magicProof, boolean simpleOnly,
                            float hardness) {
    private static final Map<Integer, TankValveSpec> BY_ID = definitions();

    private static Map<Integer,TankValveSpec> definitions(){var all=new LinkedHashMap<Integer,TankValveSpec>();for(var p:TankValveParameters.all())all.put(p.originalId(),new TankValveSpec(p.originalId(),p.size(),p.wallId(),p.capacity(),p.material(),p.gasProof(),p.acidProof(),p.plasmaProof(),p.magicProof(),p.simpleOnly(),p.hardness()));return Map.copyOf(all);}
    public static TankValveSpec find(int originalId) { return BY_ID.get(originalId); }
    public static List<TankValveSpec> all() { return List.copyOf(BY_ID.values()); }
    public Block wall() { return LargeMachineParts.block(wallId); }
    public int meltingPoint() { return GTMaterialRegistry.get(material).getMeltingPoint(); }
}
