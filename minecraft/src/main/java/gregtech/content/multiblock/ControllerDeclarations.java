package gregtech.content.multiblock;

import gregtech.content.blockentity.machine.*;
import gregtech.engine.addon.AddonMachines;
import gregtech.engine.machine.BasicMachineSpec;
import gregtech.engine.multiblock.ControllerBinding;
import gregtech.engine.multiblock.ControllerDeclaration;
import gregtech.engine.multiblock.tracking.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Controller families bind their entity type and structure here; loaders only register the declaration. */
public final class ControllerDeclarations {
    private ControllerDeclarations() {}

    public static final ControllerBinding<CokeOvenControllerBlockEntity> COKE_OVEN = binding(
            "gt6ce",
            "coke_oven",
            "cokeoven",
            CokeOvenControllerBlockEntity.class,
            CokeOvenControllerBlockEntity::createStructureDefinition,
            () -> cells(OriginalCokeOvenRules.cells()));
    public static final ControllerBinding<CryoDistillationControllerBlockEntity> CRYO_DISTILLATION = binding(
            "gt6ce",
            "cryo_distillation",
            "cryodistillationtower",
            CryoDistillationControllerBlockEntity.class,
            CryoDistillationControllerBlockEntity::createStructureDefinition,
            ControllerDeclarations::distillation);
    public static final ControllerBinding<LargeHeatExchangerControllerBlockEntity> HEAT_EXCHANGER = fixed(
            "large_heat_exchanger",
            "heatexchanger",
            LargeHeatExchangerControllerBlockEntity.class,
            LargeHeatExchangerControllerBlockEntity::createStructureDefinition);
    public static final ControllerBinding<LogisticsCoreControllerBlockEntity> LOGISTICS = fixed(
            "logistics_core",
            "logisticscore",
            LogisticsCoreControllerBlockEntity.class,
            LogisticsCoreControllerBlockEntity::createStructureDefinition);
    public static final ControllerBinding<MultiblockTankControllerBlockEntity> TANK = fixed(
            "multiblock_tank",
            "tank",
            MultiblockTankControllerBlockEntity.class,
            MultiblockTankControllerBlockEntity::createStructureDefinition);
    public static final ControllerBinding<OriginalLargeBoilerControllerBlockEntity> BOILER = fixed(
            "original_large_boiler",
            "largeboiler",
            OriginalLargeBoilerControllerBlockEntity.class,
            OriginalLargeBoilerControllerBlockEntity::createStructureDefinition);
    public static final ControllerBinding<LargeTurbineControllerBlockEntity> STEAM_TURBINE = fixed(
            "large_turbine",
            "largeturbine",
            LargeTurbineControllerBlockEntity.class,
            LargeTurbineControllerBlockEntity::createStructureDefinition);
    public static final ControllerBinding<LargeDynamoControllerBlockEntity> DYNAMO = fixed(
            "large_dynamo",
            "largedynamo",
            LargeDynamoControllerBlockEntity.class,
            LargeDynamoControllerBlockEntity::createStructureDefinition);
    public static final ControllerBinding<LargeGasTurbineControllerBlockEntity> GAS_TURBINE = fixed(
            "large_gas_turbine",
            "largegasturbine",
            LargeGasTurbineControllerBlockEntity.class,
            LargeGasTurbineControllerBlockEntity::createStructureDefinition);
    public static final ControllerBinding<LargeCrucibleControllerBlockEntity> CRUCIBLE = fixed(
            "large_crucible",
            "largecrucible",
            LargeCrucibleControllerBlockEntity.class,
            LargeCrucibleControllerBlockEntity::createStructureDefinition);
    public static final ControllerBinding<VonDaGraaggControllerBlockEntity> VAN_DE_GRAAFF = fixed(
            "von_da_graagg",
            "vondagraagg",
            VonDaGraaggControllerBlockEntity.class,
            VonDaGraaggControllerBlockEntity::createStructureDefinition);
    public static final ControllerBinding<LightningRodControllerBlockEntity> LIGHTNING = fixed(
            "lightning_rod",
            "lightningrod",
            LightningRodControllerBlockEntity.class,
            LightningRodControllerBlockEntity::createStructureDefinition);
    public static final ControllerBinding<BedrockDrillControllerBlockEntity> DRILL = fixed(
            "bedrock_drill",
            "bedrockdrill",
            BedrockDrillControllerBlockEntity.class,
            BedrockDrillControllerBlockEntity::createStructureDefinition);
    public static final ControllerBinding<ImplosionCompressorControllerBlockEntity> IMPLOSION =
            implosion("gt6ce", "implosion_compressor");
    public static final ControllerBinding<FusionReactorControllerBlockEntity> FUSION =
            fusion("gt6ce", "fusion_reactor");

    private static LargeMachineLayouts.Layout cells(List<SharedLargeMachineLayouts.Cell> cells) {
        return new LargeMachineLayouts.Layout(LargeMachineLayouts.fromShared(cells), List.of());
    }

    private static LargeMachineLayouts.Layout distillation() {
        return cells(SharedDistillationTowerStructure.CELLS);
    }

    private static <E extends BlockEntity & TrackedMultiblock> ControllerBinding<E> binding(
            String namespace,
            String entity,
            String name,
            Class<E> type,
            Function<E, StructureDefinition.Declarative> factory,
            Supplier<LargeMachineLayouts.Layout> layout) {
        return new ControllerBinding<>(
                name, ResourceLocation.fromNamespaceAndPath(namespace, entity), type, factory, layout);
    }

    private static <E extends BlockEntity & TrackedMultiblock> ControllerBinding<E> fixed(
            String entity, String name, Class<E> type, Function<E, StructureDefinition.Declarative> factory) {
        return binding("gt6ce", entity, name, type, factory, null);
    }

    private static ControllerBinding<ImplosionCompressorControllerBlockEntity> implosion(
            String namespace, String entity) {
        return binding(
                namespace,
                entity,
                "implosioncompressor",
                ImplosionCompressorControllerBlockEntity.class,
                ImplosionCompressorControllerBlockEntity::createStructureDefinition,
                null);
    }

    private static ControllerBinding<FusionReactorControllerBlockEntity> fusion(String namespace, String entity) {
        return binding(
                namespace,
                entity,
                "fusionreactor",
                FusionReactorControllerBlockEntity.class,
                FusionReactorControllerBlockEntity::createStructureDefinition,
                () -> FusionStructure.LAYOUT);
    }

    public static <B extends Block> ControllerDeclaration<B> recipe(BasicMachineSpec spec, Supplier<B> block) {
        String namespace = spec.namespace(), path = spec.id(), entity = "be_" + path;
        ControllerBinding<?> structure;
        // NeoForge's basic-machine catalog also owns these two specialized controllers.
        if (namespace.equals("gt6ce") && spec.machineName().equals("fusionreactor"))
            structure = fusion(namespace, entity);
        else if (namespace.equals("gt6ce") && spec.machineName().equals("implosioncompressor"))
            structure = implosion(namespace, entity);
        else
            structure = switch (spec.controllerKind()) {
                case DISTILLATION ->
                    binding(
                            namespace,
                            entity,
                            "distillationtower",
                            DistillationTowerControllerBlockEntity.class,
                            DistillationTowerControllerBlockEntity::createStructureDefinition,
                            ControllerDeclarations::distillation);
                case MATTER ->
                    binding(
                            namespace,
                            entity,
                            "largemassfab",
                            MatterFabricatorBlockEntity.class,
                            MatterFabricatorBlockEntity::createStructureDefinition,
                            () -> LargeMachineLayouts.layout("largemassfab"));
                case LARGE_RECIPE -> {
                    String name = namespace.equals("gt6ce")
                            ? RecipeControllerFamilies.requireStructure(spec.machineName())
                            : requiredAddonStructure(namespace, path);
                    yield binding(
                            namespace,
                            entity,
                            name,
                            LargeRecipeMachineBlockEntity.class,
                            LayoutStructures::recipe,
                            () -> LargeMachineLayouts.layout(name));
                }
                default -> null;
            };
        return structure == null
                ? ControllerDeclaration.single(namespace, path, block)
                : ControllerDeclaration.multiblock(namespace, path, block, structure);
    }

    public static <B extends Block> ControllerDeclaration<B> behavior(
            AddonMachines.AcceptedBehavior accepted, Supplier<B> block) {
        String namespace = accepted.addonId(),
                path = accepted.declaration().casing().id();
        String name = AddonMachines.structureName(namespace, path);
        return name == null
                ? ControllerDeclaration.single(namespace, path, block)
                : ControllerDeclaration.multiblock(
                        namespace,
                        path,
                        block,
                        binding(
                                namespace,
                                accepted.blockEntityPath(),
                                name,
                                BehaviorMachineBlockEntity.class,
                                LayoutStructures::behavior,
                                () -> LargeMachineLayouts.layout(name)));
    }

    public static <B extends Block> ControllerDeclaration<B> fuel(
            AddonMachines.AcceptedFuel accepted, Supplier<B> block) {
        String namespace = accepted.addonId(),
                path = accepted.declaration().casing().id();
        if (!accepted.declaration().multi()) return ControllerDeclaration.single(namespace, path, block);
        String name = requiredAddonStructure(namespace, path);
        return ControllerDeclaration.multiblock(
                namespace,
                path,
                block,
                binding(
                        namespace,
                        "be_" + path.replace('/', '_'),
                        name,
                        FuelTurbineBlockEntity.class,
                        FuelTurbineBlockEntity::createStructureDefinition,
                        () -> LargeMachineLayouts.layout(name)));
    }

    private static String requiredAddonStructure(String namespace, String path) {
        String name = AddonMachines.structureName(namespace, path);
        if (name == null)
            throw new IllegalStateException("Multiblock has no explicit structure binding: " + namespace + ":" + path);
        return name;
    }
}
