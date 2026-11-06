package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.*;
import gregtech.content.data.MaterialPrefix;
import gregtech.engine.material.ItemMaterialRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Actual original crystal/rock/vanilla ore blocks from GTIconSetBlocks; no decorative substitutes. */
public final class GTSpecialOreBlocks {
    private static final List<RegisteredBlock<Block>> ALL = new ArrayList<>();

    private GTSpecialOreBlocks() {}

    public static List<RegisteredBlock<Block>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void registerAll(ContentRegistrar registrar) {
        for (String icon : SpecialOreDefinitions.ORDERED_ICONS) {
            String id = icon.startsWith("ore_") ? "block_" + icon : icon;
            var block = registrar.block(
                    REGISTRY_NAMESPACE, id, () -> create(icon), bare -> new BlockItem(bare, new Item.Properties().stacksTo(64)));
            ALL.add(block);
        }
        // Original plate/WoodTreated exception is this real, placeable treated plank.
        RegisteredBlock<Block> treated = registrar.blockWithoutItem(
                REGISTRY_NAMESPACE,
                "planks_treated",
                () -> new IconSetBlock(
                        BlockBehaviour.Properties.of().strength(2.0F, 3.0F).sound(SoundType.WOOD), "planks_treated"));
        RegisteredItem<Item> treatedItem = registrar.blockItem(
                REGISTRY_NAMESPACE,
                "planks_treated",
                () -> new BlockItem(treated.get(), new Item.Properties().stacksTo(64)));
        GTItems.bind(
                MaterialPrefix.plate,
                gregtech.content.material.generated.WoodMaterials.WoodTreated,
                treatedItem);
        ALL.add(treated);
    }

    private static Block create(String icon) {
        var rock = RockOreBlock.spec(icon);
        if (rock != null)
            return new RockOreBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.5F * rock.hardnessMultiplier(), 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE),
                    icon,
                    rock);
        var vanilla = VanillaOreBlock.spec(icon);
        if (vanilla != null)
            return new VanillaOreBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.5F * vanilla.hardnessMultiplier(), 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE),
                    icon,
                    vanilla);
        String slug = icon.substring("crystal_ore_".length());
        return new CrystalOreBlock(
                BlockBehaviour.Properties.of()
                        .strength(0.3F, 0.3F)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.GLASS),
                icon,
                Character.toUpperCase(slug.charAt(0)) + slug.substring(1));
    }

    public static void registerCompositions() {
        for (var holder : ALL) {
            Block block = holder.get();
            if (block instanceof DenseOreBlock ore)
                ItemMaterialRegistry.register(block.asItem(), MaterialPrefix.oreDense, ore.material());
            else if (block instanceof VanillaOreBlock ore)
                ItemMaterialRegistry.register(block.asItem(), MaterialPrefix.oreVanillastone, ore.material());
        }
        ItemMaterialRegistry.register(
                GTItems.getStack(MaterialPrefix.plate, gregtech.content.material.generated.WoodMaterials.WoodTreated)
                        .getItem(),
                MaterialPrefix.plate,
                gregtech.content.material.generated.WoodMaterials.WoodTreated);
    }
}
