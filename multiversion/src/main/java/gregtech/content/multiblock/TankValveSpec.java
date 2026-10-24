package gregtech.content.multiblock;

import gregtech.internal.material.GTMaterialRegistry;
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

    private static Map<Integer, TankValveSpec> definitions() {
        var specifications = new LinkedHashMap<Integer, TankValveSpec>();
        for (var parameters : TankValveParameters.all())
            specifications.put(parameters.originalId(), new TankValveSpec(
                    parameters.originalId(), parameters.size(), parameters.wallId(), parameters.capacity(),
                    parameters.material(), parameters.gasProof(), parameters.acidProof(),
                    parameters.plasmaProof(), parameters.magicProof(), parameters.simpleOnly(), parameters.hardness()));
        return Map.copyOf(specifications);
    }

    public static TankValveSpec find(int originalId) {
        return BY_ID.get(originalId);
    }

    public static List<TankValveSpec> all() {
        return List.copyOf(BY_ID.values());
    }

    public Block wall() {
        return LargeMachineParts.block(wallId);
    }

    public int meltingPoint() {
        return GTMaterialRegistry.get(material).getMeltingPoint();
    }
}
