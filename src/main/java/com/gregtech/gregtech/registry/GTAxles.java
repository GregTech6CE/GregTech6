package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.api.energy.AxleSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.AxleBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 rotational axles — RU conductors in several material/size tiers. */
public final class GTAxles {
    private static final List<RegistryObject<AxleBlock>> ALL = new ArrayList<>();

    private GTAxles() {}

    public static List<RegistryObject<AxleBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void add(AxleSpec spec) {
        RegistryObject<AxleBlock> block = GTBlocks.BLOCKS.register(spec.id(),
                () -> new AxleBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(spec.material() == WoodMaterials.WoodTreated ? MapColor.WOOD : MapColor.METAL)
                        .strength(2.0f, 6.0f)
                        .sound(spec.material() == WoodMaterials.WoodTreated ? SoundType.WOOD : SoundType.METAL)
                        .noOcclusion()));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(),
                () -> new net.minecraft.world.item.BlockItem(block.get(), new Item.Properties().stacksTo(spec.size()<=2?64:spec.size()==3?32:16)));
    }

    public static void registerAll(){com.gregtech.gregtech.content.energy.AxleCatalog.specifications().forEach(GTAxles::add);}

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTAxles failed to initialize");
    }
}
