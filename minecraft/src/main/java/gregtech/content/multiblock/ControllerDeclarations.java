package gregtech.content.multiblock;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.MultiblockControllerBlock;
import gregtech.content.blockentity.machine.*;
import gregtech.engine.addon.AddonMachines;
import gregtech.engine.machine.BasicMachineSpec;
import gregtech.engine.machine.MachineControllerKind;
import gregtech.engine.multiblock.ControllerBinding;
import gregtech.engine.multiblock.ControllerDeclaration;
import gregtech.engine.multiblock.tracking.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Controller families bind their entity type and structure here; loaders only register the declaration. */
public final class ControllerDeclarations {
    private ControllerDeclarations() {}

    public static final RecipeControllerFamily COKE_OVEN = RecipeControllerFamily.multiblock(
            binding(
                    REGISTRY_NAMESPACE,
                    "coke_oven",
                    "cokeoven",
                    CokeOvenControllerBlockEntity.class,
                    CokeOvenControllerBlockEntity::createStructureDefinition,
                    () -> cells(OriginalCokeOvenRules.cells())),
            (type, pos, state) -> new CokeOvenControllerBlockEntity(pos, state));
    public static final RecipeControllerFamily CRYO_DISTILLATION = RecipeControllerFamily.multiblock(
            binding(
                    REGISTRY_NAMESPACE,
                    "cryo_distillation",
                    "cryodistillationtower",
                    CryoDistillationControllerBlockEntity.class,
                    CryoDistillationControllerBlockEntity::createStructureDefinition,
                    ControllerDeclarations::distillation),
            (type, pos, state) -> new CryoDistillationControllerBlockEntity(pos, state));
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
    private static final RecipeControllerFamily BASIC =
            RecipeControllerFamily.single(BasicMachineBlockEntity.class, BasicMachineBlockEntity::new);
    private static final RecipeControllerFamily COMPACT_MATTER = RecipeControllerFamily.single(
            CompactMatterFabricatorBlockEntity.class, CompactMatterFabricatorBlockEntity::new);

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
        return binding(REGISTRY_NAMESPACE, entity, name, type, factory, null);
    }

    public static ControllerDeclaration<BasicMachineBlock> recipe(
            BasicMachineSpec spec, Supplier<BlockBehaviour.Properties> properties) {
        var family = recipeFamily(spec);
        Supplier<BasicMachineBlock> block = () -> spec.controllerKind() == MachineControllerKind.DISTILLATION
                ? new MultiblockControllerBlock(spec, properties.get(), family)
                : new BasicMachineBlock(spec, properties.get(), family);
        var binding = family.binding();
        return binding == null
                ? ControllerDeclaration.single(spec.namespace(), spec.id(), block)
                : ControllerDeclaration.multiblock(spec.namespace(), spec.id(), block, binding);
    }

    private static RecipeControllerFamily recipeFamily(BasicMachineSpec spec) {
        String namespace = spec.namespace(), path = spec.id(), entity = "be_" + path;
        boolean builtIn = REGISTRY_NAMESPACE.equals(namespace);
        if (builtIn && spec.machineName().equals("fusionreactor"))
            return RecipeControllerFamily.multiblock(
                    binding(
                            namespace,
                            entity,
                            "fusionreactor",
                            FusionReactorControllerBlockEntity.class,
                            FusionReactorControllerBlockEntity::createStructureDefinition,
                            () -> FusionStructure.LAYOUT),
                    FusionReactorControllerBlockEntity::new,
                    FusionReactorControllerBlockEntity::serverTick);
        if (builtIn && spec.machineName().equals("implosioncompressor"))
            return RecipeControllerFamily.multiblock(
                    binding(
                            namespace,
                            entity,
                            "implosioncompressor",
                            ImplosionCompressorControllerBlockEntity.class,
                            ImplosionCompressorControllerBlockEntity::createStructureDefinition,
                            null),
                    ImplosionCompressorControllerBlockEntity::new);
        return switch (spec.controllerKind()) {
            case DISTILLATION ->
                RecipeControllerFamily.multiblock(
                        binding(
                                namespace,
                                entity,
                                "distillationtower",
                                DistillationTowerControllerBlockEntity.class,
                                DistillationTowerControllerBlockEntity::createStructureDefinition,
                                ControllerDeclarations::distillation),
                        DistillationTowerControllerBlockEntity::new);
            case MATTER ->
                RecipeControllerFamily.multiblock(
                        binding(
                                namespace,
                                entity,
                                "largemassfab",
                                MatterFabricatorBlockEntity.class,
                                MatterFabricatorBlockEntity::createStructureDefinition,
                                () -> LargeMachineLayouts.layout("largemassfab")),
                        MatterFabricatorBlockEntity::new);
            case LARGE_RECIPE -> {
                String name = builtIn
                        ? RecipeControllerFamilies.requireStructure(spec.machineName())
                        : requiredAddonStructure(namespace, path);
                yield RecipeControllerFamily.multiblock(
                        binding(
                                namespace,
                                entity,
                                name,
                                LargeRecipeMachineBlockEntity.class,
                                LayoutStructures::recipe,
                                () -> LargeMachineLayouts.layout(name)),
                        LargeRecipeMachineBlockEntity::new);
            }
            case BASIC -> BASIC;
            case COMPACT_MATTER -> COMPACT_MATTER;
            case BEHAVIOR ->
                throw new IllegalStateException("Behavior machines are not recipe controllers: " + spec.machineName());
        };
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
