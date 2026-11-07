package gregtech.content.registry;

import static gregtech.engine.mod.GregTechIdentity.REGISTRY_NAMESPACE;

import gregtech.content.block.FluidSpringBlock;
import gregtech.content.worldgen.FluidSpringRules;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import java.util.*;

/** Six fixed output/rate variants plus a legacy metadata carrier. */
public final class GTFluidSprings {

    private GTFluidSprings() {
    }

    public static RegisteredBlock<FluidSpringBlock> FLUID_SPRING;
    private static final Map<String, RegisteredBlock<FluidSpringBlock>> VARIANTS = new LinkedHashMap<>();

    public static void initialize(ContentRegistrar registrar) {
        if (FLUID_SPRING != null) throw new IllegalStateException("Spring registration repeated");
        FLUID_SPRING = register(registrar, "fluid_spring", null);
        for (var spring : FluidSpringRules.SPRINGS) register(registrar, FluidSpringRules.blockPath(spring), spring);
    }

    private static RegisteredBlock<FluidSpringBlock> register(
            ContentRegistrar registrar, String path, FluidSpringRules.Spring spring) {
        var block = registrar.block(
                REGISTRY_NAMESPACE,
                path,
                () -> new FluidSpringBlock(spring, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WATER).strength(-1.0F, Blocks.BEDROCK.getExplosionResistance())
                        .requiresCorrectToolForDrops().sound(SoundType.STONE)),
                bare -> new BlockItem(bare, new Item.Properties()) {

                    @Override
                    public net.minecraft.network.chat.Component getName(ItemStack stack) {
                        if (spring == null) return super.getName(stack);
                        var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(
                                net.minecraft.resources.ResourceLocation.tryParse(spring.fluidId()));
                        return net.minecraft.network.chat.Component.translatable("block.gt6ce.fluid_spring")
//? if >=1.21.1 {
                                /*.append(" (").append(new net.neoforged.neoforge.fluids.FluidStack(fluid, 1000).getDisplayName()).append(")");
*///?} else {
                                .append(" (").append(new net.minecraftforge.fluids.FluidStack(fluid, 1000).getDisplayName()).append(")");
//?}
                    }
                });
        VARIANTS.put(path, block);
        return block;
    }

    public static FluidSpringBlock byFluid(String fluid) {
        var spring = FluidSpringRules.byFluid(fluid);
        if (spring == null) return null;
        return VARIANTS.get(FluidSpringRules.blockPath(spring)).get();
    }

    public static Block[] allBlocks() {
        return VARIANTS.values().stream().map(e -> (Block) e.get()).toArray(Block[]::new);
    }
}
