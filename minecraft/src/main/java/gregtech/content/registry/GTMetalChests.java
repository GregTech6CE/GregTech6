package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.api.material.*;
import gregtech.content.block.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTMetalChests {

    private GTMetalChests() {}

    public static final java.util.List<RegisteredBlock<MetalChestBlock>> METAL = new java.util.ArrayList<>(),
            WOOD = new java.util.ArrayList<>();

    public static java.util.List<RegisteredBlock<? extends MetalChestBlock>> all() {
        var out = new java.util.ArrayList<RegisteredBlock<? extends MetalChestBlock>>();
        out.addAll(METAL);
        out.addAll(WOOD);
        out.addAll(GTLootChests.LOOT_CHESTS);
        return out;
    }

    public static MetalChestBlock chest(gregtech.api.material.GTMaterial material, boolean wood) {
        for (var chestEntry : wood ? WOOD : METAL) if (chestEntry.get().material() == material) return chestEntry.get();
        throw new IllegalArgumentException("No chest for " + material.getName());
    }

    public static void initialize(ContentRegistrar registrar) {
        for (var spec : GTStorageMetals.ALL) {
            var registeredChest = registrar.block(
                    REGISTRY_NAMESPACE,
                    "chest_" + spec.suffix(),
                    () -> new MetalChestBlock(
                            spec.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(spec.hardness(), spec.resistance())
                                    .requiresCorrectToolForDrops()
                                    .noOcclusion()),
                    bare -> new MetalChestBlockItem(bare, new Item.Properties()));
            METAL.add(registeredChest);
            var registeredWoodChest = registrar.block(
                    REGISTRY_NAMESPACE,
                    "reinforced_wood_chest_" + spec.suffix(),
                    () -> new MetalChestBlock(
                            spec.material(),
                            MetalChestBlock.Shell.REINFORCED_WOOD,
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .sound(SoundType.WOOD)
                                    .strength(spec.hardness() / 2, spec.resistance() / 2)
                                    .noOcclusion()),
                    bare -> new MetalChestBlockItem(bare, new Item.Properties().stacksTo(16)));
            WOOD.add(registeredWoodChest);
        }
    }
}
