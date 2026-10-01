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

    private record Tier(String name, GTMaterial mat, long speed, long[] powers) {}

    public static void registerAll() {
        // GT6 Loader_MultiTileEntities: 52 axles, four diameters per material.
        // NBT_PIPESIZE is the maximum RU speed; NBT_PIPEBANDWIDTH is the maximum power.
        // Names are stable registry IDs; the source's legacy numeric IDs are noted below.
        Tier[] tiers = {
                new Tier("wood", WoodMaterials.WoodTreated, 16, new long[]{1, 2, 4, 8}),                           // 24800-24803
                new Tier("bronze", Materials.Bronze, 64, new long[]{2, 4, 8, 16}),                                  // 24810-24813
                new Tier("brass", Materials.Brass, 64, new long[]{2, 4, 8, 16}),                                    // 24770-24773
                new Tier("arsenic_copper", Materials.ArsenicCopper, 64, new long[]{2, 4, 8, 16}),                  // 24780-24783
                new Tier("arsenic_bronze", Materials.ArsenicBronze, 64, new long[]{3, 6, 12, 24}),                // 24790-24793
                new Tier("steel", Materials.Steel, 256, new long[]{4, 8, 16, 32}),                                // 24820-24823
                new Tier("titanium", Materials.Titanium, 1024, new long[]{8, 16, 32, 64}),                         // 24830-24833
                new Tier("tungstensteel", Materials.Tungstensteel, 4096, new long[]{16, 32, 64, 128}),            // 24840-24843
                new Tier("iridium", Materials.Iridium, 16384, new long[]{32, 64, 128, 256}),                      // 24850-24853
                new Tier("iritanium", Materials.TitaniumIridium, 65536, new long[]{64, 128, 256, 512}),           // 24860-24863
                new Tier("trinitanium", Materials.Trinitanium, 262144, new long[]{128, 256, 512, 1024}),          // 24870-24873
                new Tier("trinaquadalloy", Materials.Trinaquadalloy, 1048576, new long[]{256, 512, 1024, 2048}), // 24880-24883
                new Tier("adamantium", Materials.Adamantium, 4194304, new long[]{512, 1024, 2048, 4096}),        // 24890-24893
        };

        for (Tier t : tiers) {
            for (int s = 1; s <= 4; s++) {
                long power = t.powers()[s - 1];
                long loss = 0; // GT6 axles do not lose speed per block
                String id = String.format("axle_%s_%d", t.name(), s);
                add(new AxleSpec(id, t.mat(), s, t.speed(), power, loss));
            }
        }
    }

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTAxles failed to initialize");
    }
}
