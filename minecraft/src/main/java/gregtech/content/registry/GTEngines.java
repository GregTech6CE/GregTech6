package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.api.machine.*;
import gregtech.engine.machine.*;
import gregtech.content.block.machine.EngineBlock;
import gregtech.content.blockentity.machine.*;
import gregtech.content.energy.EngineCatalog;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import java.util.*;

public final class GTEngines {
    private GTEngines() {}

    private static final List<RegisteredBlock<EngineBlock>> ALL = new ArrayList<>();

    public static List<RegisteredBlock<EngineBlock>> all() {
        return List.copyOf(ALL);
    }

    public static void registerAll(ContentRegistrar registrar) {
        for (var e : EngineCatalog.all()) {
            MachineSpec itemSpec = machineSpec(e.spec());
            boolean wood = e.spec() instanceof RotationEngineSpec rotation
                    && rotation.material() == gregtech.content.material.generated.WoodMaterials.WoodTreated;
            var typeRef = new java.util.concurrent.atomic.AtomicReference<BlockEntityType<?>>();
            var block = registrar.block(
                    REGISTRY_NAMESPACE,
                    itemSpec.id(),
                    () -> {
                        var props = BlockBehaviour.Properties.of().mapColor(wood ? MapColor.WOOD : MapColor.METAL)
                                .strength(itemSpec.hardness(), itemSpec.blastResistance()).requiresCorrectToolForDrops();
                        if (wood) props.sound(SoundType.WOOD);
                        var b = e.type() == EngineType.ROTATION || e.type() == EngineType.DIESEL
                                ? new EngineBlock(e.type(), itemSpec, e.spec(), props)
                                : new gregtech.content.block.machine.WaterloggedEngineBlock(e.type(), itemSpec, e.spec(), props);
                        b.setBeTypeSupplier(typeRef::get);
                        return b;
                    },
                    bare -> new EngineItem(bare, new net.minecraft.world.item.Item.Properties().stacksTo(16)));
            ALL.add(block);
            GTBlockEntities.BLOCK_ENTITIES.register("be_" + itemSpec.id(), () -> {
                var type = buildType(block.get());
                typeRef.set(type);
                return type;
            });
        }
    }

    private static MachineSpec machineSpec(Object spec) {
        if (spec instanceof ElectricEngineSpec v)
            return new MachineSpec(v.id(), v.materialName(), v.tintRgb(), 10000, v.outputRate(),
                    "engines/" + v.textureFolder(), v.hardness(), v.blastResistance());
        if (spec instanceof FluxEngineSpec v)
            return new MachineSpec(v.id(), v.materialName(), v.tintRgb(), 10000, v.outputRate(),
                    "engines/" + v.textureFolder(), v.hardness(), v.blastResistance());
        if (spec instanceof SteamEngineData v)
            return new MachineSpec(v.id(), v.materialName(), v.tintRgb(), 10000, v.outputRate(),
                    "engines/" + v.textureFolder(), v.hardness(), v.blastResistance());
        if (spec instanceof RotationEngineSpec v)
            return new MachineSpec(v.id(), v.materialName(), v.tintRgb(), 10000, v.outputRate(),
                    "engines/" + v.textureFolder(), v.hardness(), v.blastResistance());
        if (spec instanceof DieselEngineSpec v)
            return new MachineSpec(v.id(), v.materialName(), v.tintRgb(), 10000, v.outputRate(),
                    "engines/" + v.textureFolder(), v.hardness(), v.blastResistance());
        throw new IllegalArgumentException("Unknown engine specification");
    }

    private static BlockEntityType<?> buildType(EngineBlock block) {
        switch (block.engineType()) {
            case ELECTRIC: {
                var ref = new java.util.concurrent.atomic.AtomicReference<BlockEntityType<KineticElectricEngineBlockEntity>>();
                var type = BlockEntityType.Builder.of((net.minecraft.core.BlockPos pos,
                                                       net.minecraft.world.level.block.state.BlockState state) -> {
                    var be = new KineticElectricEngineBlockEntity(ref.get(), pos, state);
                    be.setSpec(block.engineSpec(ElectricEngineSpec.class));
                    return be;
                }, block).build(null);
                ref.set(type);
                return type;
            }
            case FLUX: {
                var ref = new java.util.concurrent.atomic.AtomicReference<BlockEntityType<KineticFluxEngineBlockEntity>>();
                var type = BlockEntityType.Builder.of((net.minecraft.core.BlockPos pos,
                                                       net.minecraft.world.level.block.state.BlockState state) -> {
                    var be = new KineticFluxEngineBlockEntity(ref.get(), pos, state);
                    be.setSpec(block.engineSpec(FluxEngineSpec.class));
                    return be;
                }, block).build(null);
                ref.set(type);
                return type;
            }
            case STEAM: {
                var ref = new java.util.concurrent.atomic.AtomicReference<BlockEntityType<KineticSteamEngineBlockEntity>>();
                var type = BlockEntityType.Builder.of((net.minecraft.core.BlockPos pos,
                                                       net.minecraft.world.level.block.state.BlockState state) -> {
                    var be = new KineticSteamEngineBlockEntity(ref.get(), pos, state);
                    be.setSpec(block.engineSpec(SteamEngineData.class));
                    return be;
                }, block).build(null);
                ref.set(type);
                return type;
            }
            case ROTATION: {
                var ref = new java.util.concurrent.atomic.AtomicReference<BlockEntityType<KineticRotationEngineBlockEntity>>();
                var type = BlockEntityType.Builder.of((net.minecraft.core.BlockPos pos,
                                                       net.minecraft.world.level.block.state.BlockState state) -> {
                    var be = new KineticRotationEngineBlockEntity(ref.get(), pos, state);
                    be.setSpec(block.engineSpec(RotationEngineSpec.class));
                    return be;
                }, block).build(null);
                ref.set(type);
                return type;
            }
            case DIESEL: {
                var ref = new java.util.concurrent.atomic.AtomicReference<BlockEntityType<KineticDieselEngineBlockEntity>>();
                var type = BlockEntityType.Builder.of((net.minecraft.core.BlockPos pos,
                                                       net.minecraft.world.level.block.state.BlockState state) -> {
                    var be = new KineticDieselEngineBlockEntity(ref.get(), pos, state);
                    be.setSpec(block.engineSpec(DieselEngineSpec.class));
                    return be;
                }, block).build(null);
                ref.set(type);
                return type;
            }
            default:
                throw new IllegalStateException();
        }
    }

    private static final class EngineItem extends net.minecraft.world.item.BlockItem {
        EngineItem(EngineBlock block, Properties p) {
            super(block, p);
        }

//? if >=1.21.1 {
        /*@Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
                                    List<net.minecraft.network.chat.Component> lines,
                                    net.minecraft.world.item.TooltipFlag flag) {
            super.appendHoverText(stack, context, lines, flag);
            gregtech.client.EngineTooltips.appendEngine((EngineBlock) getBlock(), lines);
        }
*///?} else {
        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level,
                                    List<net.minecraft.network.chat.Component> lines,
                                    net.minecraft.world.item.TooltipFlag flag) {
            super.appendHoverText(stack, level, lines, flag);
            gregtech.client.EngineTooltips.appendEngine((EngineBlock) getBlock(), lines);
        }
//?}
    }
}
