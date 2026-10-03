package com.gregtech.gregtech.integration.client;

import com.gregtech.gregtech.api.material.GTMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Actual registered machine world models, stitched sprites and native color callbacks. */
public final class MachineModelSmoke {
    private MachineModelSmoke() {}
    public static void check(Minecraft minecraft, List<ItemStack> gallery) {
        var missing = minecraft.getModelManager().getMissingModel();
        var errors = new ArrayList<String>(); int blocks = 0, quads = 0, materials = 0;
        var unknownTints = new TreeMap<String,Integer>();
        var renderShapes = new TreeMap<String,Integer>();
        for (var block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals("gregtech")) continue;
            var state = block.defaultBlockState();
            var model = minecraft.getBlockRenderer().getBlockModel(state);
            if (model == missing) { errors.add("model " + id); continue; }
            blocks++;
            var shape = state.getRenderShape(); renderShapes.merge(shape.name(),1,Integer::sum);
            GTMaterial material = null;
            for (String method : List.of("material", "spec", "basicSpec")) {
                try {
                    Object value = block.getClass().getMethod(method).invoke(block);
                    if (value instanceof GTMaterial found) material = found;
                    else if (value != null && value.getClass().getMethod("material").invoke(value) instanceof GTMaterial found) material = found;
                } catch (ReflectiveOperationException ignored) {}
                if (material != null) break;
            }
            boolean primaryTint = false; int count = 0;
            for (int face = -1; face < 6; face++) {
                var side = face == -1 ? null : Direction.values()[face];
                for (var quad : model.getQuads(state, side, RandomSource.create(0), ModelData.EMPTY, null)) {
                    count++; quads++;
                    var name = quad.getSprite().contents().name().toString();
                    if (name.contains("missingno")) errors.add("sprite " + id + " face=" + side);
                    if (quad.getTintIndex() == 0) primaryTint = true;
                }
            }
            if (material != null) {
                materials++;
                int actual = minecraft.getBlockColors().getColor(state, null, null, 0) & 0xFFFFFF;
                if (shape == net.minecraft.world.level.block.RenderShape.MODEL && primaryTint && actual != material.getColor()) {
                    unknownTints.merge(block.getClass().getSimpleName(), 1, Integer::sum);
                    if (errors.size() < 100) errors.add("tint " + id + " expected=" + Integer.toHexString(material.getColor()) + " actual=" + Integer.toHexString(actual));
                }
                if (shape == net.minecraft.world.level.block.RenderShape.MODEL && !primaryTint && count > 0 && !model.isCustomRenderer()) unknownTints.merge("noTint:"+block.getClass().getSimpleName(),1,Integer::sum);
            }
            if (id.getPath().equals("gearbox_arsenic_bronze") || id.getPath().equals("rotation_transformer_arsenic_bronze")
                    || id.getPath().startsWith("steam_turbine_") || id.getPath().equals("electric_dynamo_lv")
                    || id.getPath().equals("electrolyzer_galvanized_steel") || id.getPath().startsWith("gearbox_") && gallery.size() < 20)
                gallery.add(new ItemStack(block.asItem()));
        }
        com.mojang.logging.LogUtils.getLogger().info("MACHINE_MODEL_CHECK {}", new com.google.gson.Gson().toJson(Map.of(
            "blocks", blocks, "quads", quads, "materialBlocks", materials, "tintFamilies", unknownTints, "renderShapes",renderShapes, "errors", errors)));
        if (!errors.isEmpty()) throw new IllegalStateException("Machine model/color failures: " + errors.subList(0, Math.min(30, errors.size())));
    }
}
