package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.api.machine.*;
import gregtech.content.block.machine.EngineBlock;
import gregtech.content.blockentity.machine.*;
import gregtech.content.energy.EngineCatalog;
import gregtech.engine.machine.*;
import java.util.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class GTEngines {
    private GTEngines() {}

    private static final List<RegisteredBlock<EngineBlock>> ALL = new ArrayList<>();

    public static List<RegisteredBlock<EngineBlock>> all() {
        return List.copyOf(ALL);
    }

    public static void registerAll(ContentRegistrar registrar) {
        for (EngineSpec spec : EngineCatalog.all()) {
            MachineSpec itemSpec = new MachineSpec(
                    spec.id(),
                    spec.materialName(),
                    spec.tintRgb(),
                    10000,
                    spec.outputRate(),
                    "engines/" + spec.type().textureFolder(),
                    spec.hardness(),
                    spec.blastResistance());
            boolean wood = spec instanceof RotationEngineSpec rotation
                    && rotation.material() == gregtech.content.material.generated.WoodMaterials.WoodTreated;
            var typeRef = new java.util.concurrent.atomic.AtomicReference<BlockEntityType<?>>();
            var block = registrar.block(
                    REGISTRY_NAMESPACE,
                    itemSpec.id(),
                    () -> {
                        var props = BlockBehaviour.Properties.of()
                                .mapColor(wood ? MapColor.WOOD : MapColor.METAL)
                                .strength(itemSpec.hardness(), itemSpec.blastResistance())
                                .requiresCorrectToolForDrops();
                        if (wood) props.sound(SoundType.WOOD);
                        var b = spec.type() == EngineType.ROTATION || spec.type() == EngineType.DIESEL
                                ? new EngineBlock(spec, itemSpec, props)
                                : new gregtech.content.block.machine.WaterloggedEngineBlock(spec, itemSpec, props);
                        b.setBeTypeSupplier(typeRef::get);
                        return b;
                    },
                    bare -> new EngineItem(bare, new net.minecraft.world.item.Item.Properties().stacksTo(16)));
            ALL.add(block);
            registrar.blockEntity(REGISTRY_NAMESPACE, "be_" + itemSpec.id(), () -> {
                var type = buildType(block.get());
                typeRef.set(type);
                return type;
            });
        }
    }

    private static BlockEntityType<?> buildType(EngineBlock block) {
        switch (block.engineType()) {
            case ELECTRIC: {
                var ref = new java.util.concurrent.atomic.AtomicReference<
                        BlockEntityType<KineticElectricEngineBlockEntity>>();
                var type = BlockEntityType.Builder.of(
                                (net.minecraft.core.BlockPos pos,
                                        net.minecraft.world.level.block.state.BlockState state) -> {
                                    var be = new KineticElectricEngineBlockEntity(ref.get(), pos, state);
                                    be.setSpec(block.engineSpec(ElectricEngineSpec.class));
                                    return be;
                                },
                                block)
                        .build(null);
                ref.set(type);
                return type;
            }
            case FLUX: {
                var ref = new java.util.concurrent.atomic.AtomicReference<
                        BlockEntityType<KineticFluxEngineBlockEntity>>();
                var type = BlockEntityType.Builder.of(
                                (net.minecraft.core.BlockPos pos,
                                        net.minecraft.world.level.block.state.BlockState state) -> {
                                    var be = new KineticFluxEngineBlockEntity(ref.get(), pos, state);
                                    be.setSpec(block.engineSpec(FluxEngineSpec.class));
                                    return be;
                                },
                                block)
                        .build(null);
                ref.set(type);
                return type;
            }
            case STEAM: {
                var ref = new java.util.concurrent.atomic.AtomicReference<
                        BlockEntityType<KineticSteamEngineBlockEntity>>();
                var type = BlockEntityType.Builder.of(
                                (net.minecraft.core.BlockPos pos,
                                        net.minecraft.world.level.block.state.BlockState state) -> {
                                    var be = new KineticSteamEngineBlockEntity(ref.get(), pos, state);
                                    be.setSpec(block.engineSpec(SteamEngineData.class));
                                    return be;
                                },
                                block)
                        .build(null);
                ref.set(type);
                return type;
            }
            case ROTATION: {
                var ref = new java.util.concurrent.atomic.AtomicReference<
                        BlockEntityType<KineticRotationEngineBlockEntity>>();
                var type = BlockEntityType.Builder.of(
                                (net.minecraft.core.BlockPos pos,
                                        net.minecraft.world.level.block.state.BlockState state) -> {
                                    var be = new KineticRotationEngineBlockEntity(ref.get(), pos, state);
                                    be.setSpec(block.engineSpec(RotationEngineSpec.class));
                                    return be;
                                },
                                block)
                        .build(null);
                ref.set(type);
                return type;
            }
            case DIESEL: {
                var ref = new java.util.concurrent.atomic.AtomicReference<
                        BlockEntityType<KineticDieselEngineBlockEntity>>();
                var type = BlockEntityType.Builder.of(
                                (net.minecraft.core.BlockPos pos,
                                        net.minecraft.world.level.block.state.BlockState state) -> {
                                    var be = new KineticDieselEngineBlockEntity(ref.get(), pos, state);
                                    be.setSpec(block.engineSpec(DieselEngineSpec.class));
                                    return be;
                                },
                                block)
                        .build(null);
                ref.set(type);
                return type;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private static final class EngineItem extends gregtech.content.item.GTBlockItem {
        EngineItem(EngineBlock block, Properties p) {
            super(block, p);
        }

        @Override
        protected void appendTooltip(
                net.minecraft.world.item.ItemStack stack,
                @javax.annotation.Nullable net.minecraft.world.level.Level level,
                List<net.minecraft.network.chat.Component> lines,
                net.minecraft.world.item.TooltipFlag flag) {
            gregtech.client.EngineTooltips.appendEngine((EngineBlock) getBlock(), lines);
        }
    }
}
