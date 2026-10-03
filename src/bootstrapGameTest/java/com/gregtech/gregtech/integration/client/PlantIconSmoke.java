package com.gregtech.gregtech.integration.client;

import com.gregtech.gregtech.block.plant.BushBlock;
import com.gregtech.gregtech.registry.GTBushes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Final registered hive inventory models and all 28 bush state geometries. */
final class PlantIconSmoke {
    private static void requireItemModel(Minecraft client, ItemStack stack) {
        var model = client.getItemRenderer().getModel(stack, null, null, 0);
        var quads = new java.util.ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
        for (var pass : model.getRenderPasses(stack, false)) {
            quads.addAll(pass.getQuads(null, null, RandomSource.create(42), net.minecraftforge.client.model.data.ModelData.EMPTY, null));
            for (var side : Direction.values()) quads.addAll(pass.getQuads(null, side, RandomSource.create(42), net.minecraftforge.client.model.data.ModelData.EMPTY, null));
        }
        if (quads.isEmpty() || quads.stream().anyMatch(q -> q.getSprite().contents().name().getPath().equals("missingno")))
            throw new IllegalStateException("Missing hive inventory model " + stack);
    }
    static void check(Minecraft client, List<ItemStack> gallery) {
        for (var color : DyeColor.values()) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", "bumble_hive_" + color.getName()));
            var stack = new ItemStack(item);
            requireItemModel(client, stack);
            gallery.add(stack);
        }
        int states = 0;
        for (var state : GTBushes.BUSH.get().getStateDefinition().getPossibleStates()) {
            var model = client.getBlockRenderer().getBlockModel(state);
            if (model == client.getModelManager().getMissingModel()) throw new IllegalStateException("Missing bush state model " + state);
            var quads = new java.util.ArrayList<net.minecraft.client.renderer.block.model.BakedQuad>();
            quads.addAll(model.getQuads(state, null, RandomSource.create(42), net.minecraftforge.client.model.data.ModelData.EMPTY, null));
            for (var side : Direction.values()) quads.addAll(model.getQuads(state, side, RandomSource.create(42), net.minecraftforge.client.model.data.ModelData.EMPTY, null));
            if (quads.isEmpty()) throw new IllegalStateException("Empty bush state model " + state);
            double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
            for (var quad : quads) {
                if (quad.getSprite().contents().name().getPath().equals("missingno")) throw new IllegalStateException("Missing bush sprite " + state);
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) for (int axis = 0; axis < 3; axis++) {
                    double position = Float.intBitsToFloat(vertices[vertex * stride + axis]);
                    min[axis] = Math.min(min[axis], position); max[axis] = Math.max(max[axis], position);
                }
            }
            var bounds = state.getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO).bounds();
            for (var axis : Direction.Axis.values()) {
                int index = axis.ordinal();
                if (Math.abs(min[index] - bounds.min(axis)) > 0.00001 || Math.abs(max[index] - bounds.max(axis)) > 0.00001)
                    throw new IllegalStateException("Bush rendered bounds differ from selection " + state + ": " + java.util.Arrays.toString(min) + "/" + java.util.Arrays.toString(max));
            }
            states++;
        }
        var bush = new ItemStack(GTBushes.BUSH.get());
        var data = new net.minecraft.nbt.CompoundTag();
        data.putString("id", "gregtech:bush"); data.putString("berry", "blueberry");
        bush.addTagElement("BlockEntityTag", data);
        if ((client.getItemColors().getColor(bush, 0) & 0xFFFFFF) != 0x22FF22) throw new IllegalStateException("Typed bush item lacks its berry tint");
        gallery.add(bush);
        for (String color : new String[]{"white", "red", "blue"})
            gallery.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gregtech", "glowtus_" + color))));
        com.mojang.logging.LogUtils.getLogger().info("PLANT_ICON_SMOKE_SUCCESS 16 hive inventory models, {} bush state geometries", states);
    }
}
