package gregtech.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import gregtech.engine.machine.OrthogonalMachineModels;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.platform.Mc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
//?}
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Reads the {@code {"loader":"gt6ce:orthogonal_machine","spec":"<row>"}} JSON the shared pack emits
 * for every row of {@code machine_models.tsv} and bakes it through
 * {@link OrthogonalMachineBakedModel}.
 *
 * <p>The JSON only names the row; the geometry, the faces and the sprites all come from the table,
 * so one file per state and facing is no longer needed.
 */
public final class OrthogonalMachineModelLoader implements IGeometryLoader<OrthogonalMachineModelLoader.Geometry> {
    private static final String SPEC = "spec";
    private static final FileToIdConverter TEXTURES = new FileToIdConverter("textures", ".png");
    private static final String MACHINE_TEXTURE_DIRECTORY = "textures/block/machines";
    private final Map<ResourceLocation, Integer> packPriorities;

    private OrthogonalMachineModelLoader(Map<ResourceLocation, Integer> packPriorities) {
        this.packPriorities = packPriorities;
    }

    public static void loaders(ModelEvent.RegisterGeometryLoaders event) {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        ClientPackFacade.registerGeometryLoader(event, Mc.gregtechId("orthogonal_machine"),
                new OrthogonalMachineModelLoader(texturePackPriorities(resources)));
    }

    /** Snapshot the filtered texture stack once per reload, before model JSON is read in parallel. */
    private static Map<ResourceLocation, Integer> texturePackPriorities(ResourceManager resources) {
        Map<PackResources, Integer> packs = new IdentityHashMap<>();
        resources.listPacks().forEachOrdered(pack -> packs.put(pack, packs.size()));
        Map<ResourceLocation, Integer> textures = new HashMap<>();
        resources.listResources(MACHINE_TEXTURE_DIRECTORY,
                id -> GregTechIdentity.REGISTRY_NAMESPACE.equals(id.getNamespace()) && id.getPath().endsWith(".png"))
                .forEach((id, resource) -> textures.put(TEXTURES.fileToId(id), packs.getOrDefault(resource.source(), -1)));
        return Map.copyOf(textures);
    }

    @Override
    public Geometry read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
        JsonElement element = json.get(SPEC);
        if (element == null || !element.isJsonPrimitive())
            throw new JsonParseException("Orthogonal machine model needs a \"" + SPEC + "\" table row");
        String modelPath = element.getAsString();
        OrthogonalMachineModels.Entry spec = OrthogonalMachineModels.get(modelPath);
        if (spec == null)
            throw new JsonParseException("No machine_models.tsv row for \"" + modelPath + "\"");
        return new Geometry(OrthogonalMachineModels.resolveLegacyTextures(spec, path ->
                packPriorities.getOrDefault(ResourceLocation.parse(OrthogonalMachineModels.fullTexturePath(path)), -1)));
    }

    public record Geometry(OrthogonalMachineModels.Entry spec) implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                               Function<Material, TextureAtlasSprite> sprites, ModelState state,
//? if >=1.21.1 {
                               /*ItemOverrides overrides) {
            // 1.21.1's geometry signature carries no model location; only Forge's FaceBakery
            // names one in its errors.
            ResourceLocation modelLocation = null;
*///?} else {
                               ItemOverrides overrides, ResourceLocation modelLocation) {
//?}
            return OrthogonalMachineBakedModel.bake(
                    spec, id -> sprites.apply(new Material(TextureAtlas.LOCATION_BLOCKS, id)), state,
                    modelLocation, context.getTransforms(), overrides,
                    context.useAmbientOcclusion(), context.useBlockLight());
        }
    }
}
