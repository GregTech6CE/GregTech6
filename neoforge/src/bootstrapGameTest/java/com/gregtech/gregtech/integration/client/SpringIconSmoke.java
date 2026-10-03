package com.gregtech.gregtech.integration.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import java.util.List;

final class SpringIconSmoke {
    private static final String[] WORLD_FLUIDS={"liquid_light_oil","gas_natural_gas","watergeothermal"};
    static void prepareWorld(net.minecraft.server.level.ServerLevel level,net.minecraft.server.level.ServerPlayer player) {
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.getAbilities().flying=true;
        player.onUpdateAbilities();
        for(int i=0;i<WORLD_FLUIDS.length;i++) {
            var pos=new net.minecraft.core.BlockPos(i*2,200,0);
            if(!level.getBlockState(pos).isAir()) {
                // A repeated EMI run can reuse only this exact spring fixture; never replace other blocks.
                var expected=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",WORLD_FLUIDS[i]);
                if (!(level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.FluidSpringBlockEntity spring)
                        || spring.fluid()==null || !net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(spring.fluid()).equals(expected))
                    throw new IllegalStateException("Smoke refuses to overwrite world specimen at "+pos);
                continue;
            }
            level.setBlockAndUpdate(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos.above(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos,com.gregtech.gregtech.registry.GTFluidSprings.FLUID_SPRING.get().defaultBlockState());
            ((com.gregtech.gregtech.blockentity.FluidSpringBlockEntity)level.getBlockEntity(pos)).setSpring("gregtech:"+WORLD_FLUIDS[i],6000);
        }
        player.connection.teleport(6.5,201,6.5,145,20);
    }
    static boolean checkWorld(Minecraft client) {
        for(int i=0;i<WORLD_FLUIDS.length;i++) {
            var pos=new net.minecraft.core.BlockPos(i*2,200,0);
            if(!(client.level.getBlockEntity(pos) instanceof com.gregtech.gregtech.blockentity.FluidSpringBlockEntity spring)
                    || spring.fluid()==null) return false;
            var expected=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",WORLD_FLUIDS[i]);
            if(!net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(spring.fluid()).equals(expected)) return false;
            var model=client.getBlockRenderer().getBlockModel(spring.getBlockState());
            var quads=model.getQuads(spring.getBlockState(),null,RandomSource.create(42),spring.getModelData(),net.minecraft.client.renderer.RenderType.cutout());
            var texture=net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(spring.fluid()).getStillTexture();
            if(quads.size()!=12||quads.stream().filter(q->q.getTintIndex()==0).anyMatch(q->!q.getSprite().contents().name().equals(texture)))
                throw new IllegalStateException("World spring ignores synchronized fluid "+expected);
        }
        return true;
    }

    static void check(Minecraft client, List<ItemStack> gallery) {
        for (String fluid : new String[]{"liquid_light_oil", "gas_natural_gas", "watergeothermal"}) {
            var stack = new ItemStack(com.gregtech.gregtech.registry.GTFluidSprings.FLUID_SPRING.get());
            var tag = new CompoundTag(); tag.putString("id", "gregtech:fluid_spring"); tag.putString("spring", "gregtech:"+fluid);
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            var model = client.getItemRenderer().getModel(stack, null, null, 0);
            var quads = model.getQuads(null, null, RandomSource.create(42));
            if (quads.size() != 12 || quads.stream().map(q -> q.getDirection()).distinct().count() != 6)
                throw new IllegalStateException("Spring must have six solid faces and six overlay faces");
            var source = com.gregtech.gregtech.client.FluidSpringClient.fluid(stack);
            var texture = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(source).getStillTexture();
            if (quads.stream().filter(q -> q.getTintIndex()==0).count()!=6
                    || quads.stream().filter(q -> q.getTintIndex()==0).anyMatch(q -> !q.getSprite().contents().name().equals(texture)))
                throw new IllegalStateException("Spring lost stored fluid texture " + fluid);
            gallery.add(stack);
        }
        com.mojang.logging.LogUtils.getLogger().info("SPRING_ICON_SMOKE_SUCCESS oil, gas and geothermal cube layers");
    }
}
