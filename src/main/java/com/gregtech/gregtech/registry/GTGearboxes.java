package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.api.energy.EnergyNodeSpec;
import com.gregtech.gregtech.api.energy.GearboxSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.EnergyNodeBlock;
import com.gregtech.gregtech.block.energy.GearboxBlock;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.content.material.generated.WoodMaterials;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The 13 original GT6 custom gearboxes and RU rotation transformers. */
public final class GTGearboxes {
    private static final List<RegistryObject<GearboxBlock>> ALL_GEARBOXES = new ArrayList<>();
    private static final List<RegistryObject<EnergyNodeBlock>> ALL_TRANSFORMERS = new ArrayList<>();

    // GT6 Loader_MultiTileEntities.kinetic(): 24778–24899. Registry names of
    // the existing variants remain stable; the source's numeric IDs are in the
    // resource generator so that rates and names can be checked against GT6.
    private record Tier(String suffix, GTMaterial material, int voltageTier, long maxSpeed) {}
    private static final Tier[] TIERS = {
            new Tier("wood", WoodMaterials.WoodTreated, 0, 16),
            new Tier("bronze", Materials.Bronze, 1, 64),
            new Tier("brass", Materials.Brass, 1, 64),
            new Tier("arsenic_copper", Materials.ArsenicCopper, 1, 64),
            new Tier("arsenic_bronze", Materials.ArsenicBronze, 1, 64),
            new Tier("steel", Materials.Steel, 2, 256),
            new Tier("titanium", Materials.Titanium, 3, 1024),
            new Tier("tungstensteel", Materials.Tungstensteel, 4, 4096),
            new Tier("iridium", Materials.Iridium, 5, 16384),
            new Tier("iritanium", Materials.TitaniumIridium, 6, 65536),
            new Tier("trinitanium", Materials.Trinitanium, 7, 262144),
            new Tier("trinaquadalloy", Materials.Trinaquadalloy, 8, 1048576),
            new Tier("adamantium", Materials.Adamantium, 9, 4194304),
    };
    private static final long[] V = {8, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, 2097152};

    private GTGearboxes() {}

    public static List<RegistryObject<GearboxBlock>> allGearboxes() { return Collections.unmodifiableList(ALL_GEARBOXES); }
    public static List<RegistryObject<EnergyNodeBlock>> allTransformers() { return Collections.unmodifiableList(ALL_TRANSFORMERS); }

    public static void registerAll() {
        for (Tier tier : TIERS) {
            registerGearbox(tier);
            registerTransformer(tier);
        }
    }

    private static void registerGearbox(Tier tier) {
        String id = "gearbox_" + tier.suffix();
        // The original custom gearbox only has NBT_INPUT = VMAX[t]. maxPower
        // is a legacy display field in this port and must not limit transfers.
        GearboxSpec spec = new GearboxSpec(id, tier.material(), tier.maxSpeed(), tier.maxSpeed() / 4);
        boolean wooden = tier.voltageTier() == 0;
        RegistryObject<GearboxBlock> block = GTBlocks.BLOCKS.register(id,
                () -> new GearboxBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(wooden ? MapColor.WOOD : MapColor.METAL)
                        .strength(6.0f, 6.0f)
                        .sound(wooden ? SoundType.WOOD : SoundType.METAL)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()));
        ALL_GEARBOXES.add(block);
        GTBlocks.BLOCK_ITEMS.register(id,
                () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
    }

    private static void registerTransformer(Tier tier) {
        // NBT_INPUT = V[t], NBT_OUTPUT = V[t-1] (wood: 2); multiplier 4.
        int t = tier.voltageTier();
        long input = V[t];
        long output = t == 0 ? 2 : V[t - 1];
        String id = "rotation_transformer_" + tier.suffix();
        EnergyNodeSpec spec = new EnergyNodeSpec(id, tier.material(), EnergyNodeSpec.Kind.CONVERTER,
                "transformers/rotation_transformer", GregTechTags.Energy.RU, GregTechTags.Energy.RU,
                input, output, input * 2,
                tier.material().getLocalName() + " Transformer Gearbox",
                tier.material().getLocalName() + "变速箱", 0);
        boolean wooden = t == 0;
        RegistryObject<EnergyNodeBlock> block = GTBlocks.BLOCKS.register(id,
                () -> new EnergyNodeBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(wooden ? MapColor.WOOD : MapColor.METAL)
                        .strength(6.0f, 6.0f)
                        .sound(wooden ? SoundType.WOOD : SoundType.METAL)
                        .requiresCorrectToolForDrops()));
        ALL_TRANSFORMERS.add(block);
        GTBlocks.BLOCK_ITEMS.register(id,
                () -> new BlockItem(block.get(), new Item.Properties().stacksTo(16)));
    }

    public static void bootstrap() {
        if (ALL_GEARBOXES.size() != 13 || ALL_TRANSFORMERS.size() != 13)
            throw new IllegalStateException("GTGearboxes must register the original 13+13 variants");
    }
}
