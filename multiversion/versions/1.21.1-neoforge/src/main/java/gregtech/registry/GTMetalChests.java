package gregtech.registry;

import gregtech.block.inventory.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class GTMetalChests {

    private GTMetalChests() {
    }

    public static final java.util.List<DeferredHolder<Block, MetalChestBlock>> METAL = new java.util.ArrayList<>(),
            WOOD = new java.util.ArrayList<>();

    public static java.util.List<DeferredHolder<Block, ? extends MetalChestBlock>> all() {
        var out = new java.util.ArrayList<DeferredHolder<Block, ? extends MetalChestBlock>>();
        out.addAll(METAL);
        out.addAll(WOOD);
        out.addAll(GTLootChests.LOOT_CHESTS);
        return out;
    }

    public static MetalChestBlock chest(gregtech.api.material.GTMaterial material, boolean wood) {
        for (var chestEntry : wood ? WOOD : METAL) if (chestEntry.get().material() == material) return chestEntry.get();
        throw new IllegalArgumentException("No chest for " + material.getName());
    }

    public static void initialize() {
        for (var spec : GTStorageMetals.ALL) {
            var registeredChest = GTBlocks.BLOCKS.register("chest_" + spec.suffix(), () -> new MetalChestBlock(spec.material(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(spec.hardness(), spec.resistance())
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));
            METAL.add(registeredChest);
            GTBlocks.BLOCK_ITEMS.register("chest_" + spec.suffix(), () -> new MetalChestBlockItem(registeredChest.get(),
                    new net.minecraft.world.item.Item.Properties()));
            var registeredWoodChest = GTBlocks.BLOCKS.register("reinforced_wood_chest_" + spec.suffix(), () -> new MetalChestBlock(spec.material(),
                    MetalChestBlock.Shell.REINFORCED_WOOD,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .sound(SoundType.WOOD)
                            .strength(spec.hardness() / 2, spec.resistance() / 2)
                            .noOcclusion()));
            WOOD.add(registeredWoodChest);
            GTBlocks.BLOCK_ITEMS.register("reinforced_wood_chest_" + spec.suffix(), () -> new MetalChestBlockItem(registeredWoodChest.get(),
                    new net.minecraft.world.item.Item.Properties().stacksTo(16)));
        }
        GTLootChests.registerAll();
    }
}
