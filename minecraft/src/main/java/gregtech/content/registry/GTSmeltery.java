package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.CrucibleCrossingBlock;
import gregtech.content.block.machine.CrucibleFaucetBlock;
import gregtech.content.block.machine.CrucibleHullBlockItem;
import gregtech.content.block.machine.MoldBasinBlock;
import gregtech.content.block.machine.MoldBlock;
import gregtech.content.block.machine.SmeltingCrucibleBlock;
import gregtech.content.block.machine.SmeltingCrucibleBlockItem;
import gregtech.engine.machine.CrucibleSpec;
import gregtech.engine.machine.InitialSmelteryDefinitions;
import gregtech.engine.machine.OriginalCrucibleDefinitions;
import gregtech.engine.machine.SmelteryCompanionDefinitions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;

/** Shared smelting crucible, mold, basin, faucet and crossing catalog. */
public final class GTSmeltery {
    private static final List<RegisteredBlock<SmeltingCrucibleBlock>> CRUCIBLES = new ArrayList<>();
    private static final List<RegisteredBlock<MoldBlock>> MOLDS = new ArrayList<>();
    private static final List<RegisteredBlock<MoldBasinBlock>> BASINS = new ArrayList<>();
    private static final List<RegisteredBlock<CrucibleFaucetBlock>> FAUCETS = new ArrayList<>();
    private static final List<RegisteredBlock<CrucibleCrossingBlock>> CROSSINGS = new ArrayList<>();
    private static RegisteredBlock<SmeltingCrucibleBlock> CERAMIC_CRUCIBLE;
    private static RegisteredBlock<MoldBlock> CERAMIC_MOLD;

    private GTSmeltery() {}

    public static List<RegisteredBlock<SmeltingCrucibleBlock>> crucibles() {
        return List.copyOf(CRUCIBLES);
    }

    public static List<RegisteredBlock<MoldBlock>> molds() {
        return List.copyOf(MOLDS);
    }

    public static List<RegisteredBlock<MoldBasinBlock>> basins() {
        return List.copyOf(BASINS);
    }

    public static List<RegisteredBlock<CrucibleFaucetBlock>> faucets() {
        return List.copyOf(FAUCETS);
    }

    public static List<RegisteredBlock<CrucibleCrossingBlock>> crossings() {
        return List.copyOf(CROSSINGS);
    }

    public static RegisteredBlock<SmeltingCrucibleBlock> ceramicCrucible() {
        return CERAMIC_CRUCIBLE;
    }

    public static RegisteredBlock<MoldBlock> ceramicMold() {
        return CERAMIC_MOLD;
    }

    public static RegisteredBlock<SmeltingCrucibleBlock> findCrucible(String id) {
        return CRUCIBLES.stream()
                .filter(block -> block.getId().getPath().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static void register(ContentRegistrar registrar) {
        if (!CRUCIBLES.isEmpty()) return;
        CERAMIC_CRUCIBLE = crucible(registrar, InitialSmelteryDefinitions.ceramicCrucible());
        CERAMIC_MOLD = mold(registrar, InitialSmelteryDefinitions.ceramicCrucible());
        registrar.item(
                REGISTRY_NAMESPACE,
                "smelting_crucible_ceramic",
                () -> new SmeltingCrucibleBlockItem(
                        CERAMIC_CRUCIBLE.get(),
                        new Item.Properties().stacksTo(16),
                        InitialSmelteryDefinitions.ceramicCrucible()));
        for (var spec : OriginalCrucibleDefinitions.all()) {
            if (spec.id().equals("smelting_crucible_ceramic")) continue;
            var holder = crucible(registrar, spec);
            registrar.item(
                    REGISTRY_NAMESPACE,
                    spec.id(),
                    () -> new SmeltingCrucibleBlockItem(holder.get(), new Item.Properties().stacksTo(16), spec));
        }
        for (var base : OriginalCrucibleDefinitions.all()) {
            if (!base.id().equals("smelting_crucible_ceramic")) mold(registrar, base);
            otherCompanions(registrar, base);
        }
    }

    private static RegisteredBlock<SmeltingCrucibleBlock> crucible(ContentRegistrar registrar, CrucibleSpec spec) {
        var block = registrar.blockWithoutItem(
                REGISTRY_NAMESPACE,
                spec.id(),
                () -> new SmeltingCrucibleBlock(
                        spec, SmeltingCrucibleBlock.defaultProperties(spec).noOcclusion()));
        CRUCIBLES.add(block);
        return block;
    }

    private static RegisteredBlock<MoldBlock> mold(ContentRegistrar registrar, CrucibleSpec base) {
        var spec = SmelteryCompanionDefinitions.of(base, SmelteryCompanionDefinitions.Kind.MOLD);
        var holder = registrar.block(
                REGISTRY_NAMESPACE,
                spec.id(),
                () -> new MoldBlock(spec, MoldBlock.defaultProperties(spec)),
                mold -> new CrucibleHullBlockItem(mold, new Item.Properties().stacksTo(16), spec));
        MOLDS.add(holder);
        return holder;
    }

    private static void otherCompanions(ContentRegistrar registrar, CrucibleSpec base) {
        var basin = SmelteryCompanionDefinitions.of(base, SmelteryCompanionDefinitions.Kind.BASIN);
        BASINS.add(registrar.block(
                REGISTRY_NAMESPACE,
                basin.id(),
                () -> new MoldBasinBlock(basin, MoldBasinBlock.defaultProperties(basin)),
                hull -> new CrucibleHullBlockItem(hull, new Item.Properties().stacksTo(16), basin)));
        var faucet = SmelteryCompanionDefinitions.of(base, SmelteryCompanionDefinitions.Kind.FAUCET);
        FAUCETS.add(registrar.block(
                REGISTRY_NAMESPACE,
                faucet.id(),
                () -> new CrucibleFaucetBlock(faucet, CrucibleFaucetBlock.defaultProperties(faucet)),
                hull -> new CrucibleHullBlockItem(hull, new Item.Properties().stacksTo(16), faucet)));
        var crossing = SmelteryCompanionDefinitions.of(base, SmelteryCompanionDefinitions.Kind.CROSSING);
        CROSSINGS.add(registrar.block(
                REGISTRY_NAMESPACE,
                crossing.id(),
                () -> new CrucibleCrossingBlock(crossing, CrucibleCrossingBlock.defaultProperties(crossing)),
                hull -> new CrucibleHullBlockItem(hull, new Item.Properties().stacksTo(16), crossing)));
    }
}
