package gregtech.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gregtech.engine.mod.GregTechIdentity;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

/** Restores design 4 when a resource pack supplies art at the original metal-wall sprite paths. */
public final class LegacyPartDesignModels {
    private static final List<String> FACE_TEXTURES = List.of("top", "bottom", "side");

    private LegacyPartDesignModels() {}

    public static @Nullable String createModel(ResourceManager resources, String folder) {
        String path = "block/machines/multiblockparts/" + folder + "/4/";
        JsonObject children = new JsonObject();
        addLayer(children, resources, path, "colored", true);
        addLayer(children, resources, path, "overlay", false);
        if (children.size() == 0) return null;

        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        //? if >=1.21.1 {
        /*model.addProperty("loader", "neoforge:composite");
        *///?} else {
        model.addProperty("loader", "forge:composite");
        //?}
        model.add("children", children);
        JsonObject firstLayer = children.entrySet().iterator().next().getValue().getAsJsonObject();
        JsonObject textures = new JsonObject();
        textures.add("particle", firstLayer.getAsJsonObject("textures").get("particle"));
        model.add("textures", textures);
        return model.toString();
    }

    private static void addLayer(JsonObject children, ResourceManager resources, String path, String layer,
                                 boolean tinted) {
        JsonObject textures = new JsonObject();
        for (String face : FACE_TEXTURES) {
            String sprite = path + layer + "/" + face;
            var file = ResourceLocation.fromNamespaceAndPath(
                    GregTechIdentity.REGISTRY_NAMESPACE, "textures/" + sprite + ".png");
            if (resources.getResource(file).isPresent()) {
                textures.addProperty(face, GregTechIdentity.REGISTRY_NAMESPACE + ":" + sprite);
            }
        }
        if (textures.size() == 0) return;
        textures.add("particle", textures.entrySet().iterator().next().getValue());

        JsonObject faces = new JsonObject();
        for (Direction direction : Direction.values()) {
            String slot = switch (direction) {
                case UP -> "top";
                case DOWN -> "bottom";
                default -> "side";
            };
            // Missing faces used a transparent placeholder in the original design.
            if (!textures.has(slot)) continue;
            String name = direction.getSerializedName();
            JsonObject face = new JsonObject();
            face.addProperty("texture", "#" + slot);
            face.addProperty("cullface", name);
            if (tinted) face.addProperty("tintindex", 0);
            faces.add(name, face);
        }

        JsonObject cube = new JsonObject();
        cube.add("from", corner(0));
        cube.add("to", corner(16));
        cube.add("faces", faces);
        JsonArray elements = new JsonArray();
        elements.add(cube);

        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        model.addProperty("render_type", "minecraft:cutout");
        model.add("textures", textures);
        model.add("elements", elements);
        children.add(layer, model);
    }

    private static JsonArray corner(int coordinate) {
        JsonArray values = new JsonArray();
        values.add(coordinate);
        values.add(coordinate);
        values.add(coordinate);
        return values;
    }
}
