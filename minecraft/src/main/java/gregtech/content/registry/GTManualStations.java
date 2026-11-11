package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.api.material.GTMaterial;
import gregtech.content.block.tool.*;
import gregtech.content.blockentity.tool.ManualToolBlockEntity.Kind;
import java.util.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** GT6 manual tool stations: anvils, vessels, mortars, ropes, scaffolds and shaped tool blocks. */
public final class GTManualStations {

    private GTManualStations() {}

    public static final List<RegisteredBlock<MaterialAnvilBlock>> ANVILS = new ArrayList<>();
    public static final List<RegisteredBlock<ManualToolBlock>> MANUAL = new ArrayList<>();
    public static final List<RegisteredBlock<ProcessingToolBlock>> VESSELS = new ArrayList<>();

    public static int tintOf(Kind kind) {
        return switch (kind) {
            case MORTAR -> 0xCDCDCD;
            case GRINDSTONE -> 0xB5B5B5;
            case SIFTING -> gregtech.content.material.Materials.Steel.getColor();
        };
    }

    public static final List<RegisteredBlock<ScaffoldBlock>> SCAFFOLDS = new ArrayList<>();
    public static final List<RegisteredBlock<RopeBlock>> ROPES = new ArrayList<>();
    public static final List<RegisteredBlock<ShapedToolBlock>> SHAPED = new ArrayList<>();
    public static RegisteredBlock<CoinMoldBlock> COIN_MOLD;
    public static RegisteredBlock<DustFunnelBlock> DUST_FUNNEL;
    public static RegisteredBlock<SapBagBlock> SAP_BAG;
    public static RegisteredBlock<AdvancedButtonBlock> ADVANCED_BUTTON;
    public static RegisteredBlock<CrankBlock> CRANK;
    public static RegisteredBlock<PlantPotBlock> PLANT_POT;

    public static List<RegisteredBlock<ManualToolBlock>> manual() {
        return Collections.unmodifiableList(MANUAL);
    }

    public static ScaffoldBlock scaffold(GTMaterial material) {
        for (var entry : SCAFFOLDS) if (entry.get().material() == material) return entry.get();
        throw new IllegalArgumentException("No scaffold for " + material.getName());
    }

    private static void item(
            ContentRegistrar registrar, String id, java.util.function.Supplier<? extends Block> b, int count) {
        registrar.blockItem(
                REGISTRY_NAMESPACE,
                id,
                () -> new net.minecraft.world.item.BlockItem(
                        b.get(), new net.minecraft.world.item.Item.Properties().stacksTo(count)));
    }

    private static void item(ContentRegistrar registrar, String id, java.util.function.Supplier<? extends Block> b) {
        registrar.blockItem(
                REGISTRY_NAMESPACE,
                id,
                () -> new net.minecraft.world.item.BlockItem(
                        b.get(), new net.minecraft.world.item.Item.Properties().stacksTo(16)));
    }

    private static void manual(
            ContentRegistrar registrar, String id, Kind kind, String material, BlockBehaviour.Properties p) {
        var b = registrar.blockWithoutItem(REGISTRY_NAMESPACE, id, () -> new ManualToolBlock(kind, material, p));
        MANUAL.add(b);
        item(registrar, id, b);
    }

    public static void initialize(ContentRegistrar registrar) {
        COIN_MOLD = registrar.blockWithoutItem(
                REGISTRY_NAMESPACE,
                "coin_mold",
                () -> new CoinMoldBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(6f, 6f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL)));
        SHAPED.add((RegisteredBlock) COIN_MOLD);
        item(registrar, "coin_mold", COIN_MOLD);
        for (var v : gregtech.content.tool.UtilityToolRules.ROPES) {
            var b = registrar.blockWithoutItem(
                    REGISTRY_NAMESPACE,
                    v.id(),
                    () -> new RopeBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOL)
                                    .strength(.5f, .5f)
                                    .sound(SoundType.WOOL)
                                    .noOcclusion()
                                    .noCollission(),
                            gregtech.engine.material.GTMaterialRegistry.get(v.material())
                                    .getColor()));
            ROPES.add(b);
            item(registrar, v.id(), b, 64);
        }
        for (var v : GTStorageMetals.ALL) {
            String id = v.suffix().equals("steel") ? "scaffold" : "scaffold_" + v.suffix();
            var b = registrar.blockWithoutItem(
                    REGISTRY_NAMESPACE,
                    id,
                    () -> new ScaffoldBlock(
                            v.material(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(5f, v.resistance())
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));
            SCAFFOLDS.add(b);
            item(registrar, id, b, 64);
        }
        CRANK = registrar.blockWithoutItem(
                REGISTRY_NAMESPACE,
                "crank",
                () -> new CrankBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(1f, 6f)
                        .sound(SoundType.METAL)
                        .noOcclusion()));
        item(registrar, "crank", CRANK);
        DUST_FUNNEL = registrar.blockWithoutItem(
                REGISTRY_NAMESPACE,
                "dust_funnel",
                () -> new DustFunnelBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(1f, 5f)
                        .requiresCorrectToolForDrops()
                        .noOcclusion()));
        item(registrar, "dust_funnel", DUST_FUNNEL);
        PLANT_POT = registrar.blockWithoutItem(
                REGISTRY_NAMESPACE,
                "plant_pot",
                () -> new PlantPotBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.CLAY)
                        .strength(1f, 5f)
                        .sound(SoundType.STONE)));
        SHAPED.add((RegisteredBlock) PLANT_POT);
        item(registrar, "plant_pot", PLANT_POT);
        SAP_BAG = registrar.blockWithoutItem(
                REGISTRY_NAMESPACE,
                "sap_bag",
                () -> new SapBagBlock(
                        "sap_bag",
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.COLOR_BROWN)
                                .strength(.5f, .5f)
                                .sound(SoundType.WOOL)));
        SHAPED.add((RegisteredBlock) SAP_BAG);
        item(registrar, "sap_bag", SAP_BAG);
        ADVANCED_BUTTON = registrar.blockWithoutItem(
                REGISTRY_NAMESPACE,
                "advanced_button",
                () -> new AdvancedButtonBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(6f, 6f)
                        .sound(SoundType.METAL)
                        .noOcclusion()));
        item(registrar, "advanced_button", ADVANCED_BUTTON, 64);

        for (var v : gregtech.content.tool.OpenVesselRules.ALL) {
            var p = BlockBehaviour.Properties.of().strength(v.hardness(), v.resistance());
            if (v.wooden()) p.mapColor(MapColor.WOOD).sound(SoundType.WOOD);
            else if (v.bowl()) p.mapColor(MapColor.CLAY).sound(SoundType.STONE);
            else if (v.juicer()) p.mapColor(MapColor.STONE).requiresCorrectToolForDrops();
            else p.mapColor(MapColor.METAL).requiresCorrectToolForDrops().sound(SoundType.METAL);
            var b = registrar.blockWithoutItem(REGISTRY_NAMESPACE, v.id(), () -> new ProcessingToolBlock(v.id(), p));
            VESSELS.add(b);
            item(registrar, v.id(), b);
        }

        for (var v : gregtech.content.tool.AnvilVariantCatalog.ALL) {
            var b = registrar.blockWithoutItem(
                    REGISTRY_NAMESPACE,
                    v.id(),
                    () -> new MaterialAnvilBlock(
                            () -> gregtech.engine.material.GTMaterialRegistry.get(v.material()),
                            v.durability(),
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(10f, 1200f)
                                    .requiresCorrectToolForDrops()
                                    .sound(SoundType.ANVIL)));
            ANVILS.add(b);
            item(registrar, v.id(), b);
        }
        manual(
                registrar,
                "mortar_block",
                Kind.MORTAR,
                "Steel",
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(1f, 5f)
                        .requiresCorrectToolForDrops()
                        .noOcclusion());
        for (String m : List.of("Netherite", "Sapphire", "Diamond", "Amethyst"))
            manual(
                    registrar,
                    "mortar_" + m.toLowerCase(Locale.ROOT),
                    Kind.MORTAR,
                    m,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(1f, 5f)
                            .noOcclusion());
        manual(
                registrar,
                "grindstone_block",
                Kind.GRINDSTONE,
                "Steel",
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.STONE)
                        .strength(1f, 6f)
                        .requiresCorrectToolForDrops()
                        .noOcclusion());
        manual(
                registrar,
                "sifting_table",
                Kind.SIFTING,
                "Steel",
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(1f, 6f)
                        .sound(SoundType.METAL)
                        .noOcclusion());
    }
}
