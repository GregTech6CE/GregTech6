package gregtech.client;

import gregtech.api.material.MaterialTextureSet;
import gregtech.content.block.MaterialBlockLike;
import gregtech.content.block.OreBlock;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.CreativeTabIconItem;
import gregtech.engine.material.MaterialFormItem;
import gregtech.engine.material.MaterialIconDefinitions;
import gregtech.engine.material.MaterialItemDefinitions;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.engine.prefix.BlockPrefixRegistry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The shared material models the registries actually ask for: one entry per texture set and prefix
 * combination that a registered item or block resolves to, kept only when some set in the
 * inheritance chain ships the base sprite.
 *
 * <p>Both the model registration and the in-memory model pack read this list, so registration can
 * never name a model the pack does not serve.</p>
 */
public final class MaterialSharedModelIndex {
    public record ItemModel(MaterialTextureSet set, MaterialPrefix prefix) {}

    public record BlockModel(MaterialTextureSet set, BlockMaterialPrefix prefix) {}

    private MaterialSharedModelIndex() {}

    public static List<ItemModel> items() {
        Set<ItemModel> models = new LinkedHashSet<>();
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof MaterialFormItem form && !form.getPrefix().isAddon()) {
                models.add(new ItemModel(MaterialIcons.resolveTextureSet(form.getMaterial()), form.getPrefix()));
            } else if (item instanceof CreativeTabIconItem icon && icon.materialPrefix() != null) {
                models.add(new ItemModel(MaterialTextureSet.NONE, icon.materialPrefix()));
            }
        }
        // Fallback root tool templates so dynamic tool handles/heads always find a baked ancestor model.
        for (MaterialPrefix prefix : MaterialItemDefinitions.candidatePrefixes()) {
            if (prefix == MaterialPrefix.stick
                    || prefix == MaterialPrefix.lens
                    || prefix.getName().startsWith("toolHead")) {
                models.add(new ItemModel(MaterialTextureSet.DULL, prefix));
                models.add(new ItemModel(MaterialTextureSet.STONE, prefix));
            }
        }
        List<ItemModel> resolved = new ArrayList<>(models.size());
        for (ItemModel model : models) {
            if (MaterialIconDefinitions.resolveItemTextureOwner(
                            model.set(), model.prefix(), false, MaterialIcons::resourceExists)
                    != null) {
                resolved.add(model);
            }
        }
        return resolved;
    }

    public static List<BlockModel> blocks() {
        BlockPrefixRegistry.ensurePrefixesLoaded();
        Set<BlockModel> models = new LinkedHashSet<>();
        for (var block : BuiltInRegistries.BLOCK) {
            // Ore blocks render through OreBakedModel, which paints the host stone itself.
            if (!(block instanceof MaterialBlockLike like) || like.prefix().isAddon() || block instanceof OreBlock) {
                continue;
            }
            models.add(new BlockModel(MaterialIcons.resolveTextureSet(like.material()), like.prefix()));
        }
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof CreativeTabIconItem icon && icon.blockPrefix() != null) {
                models.add(new BlockModel(MaterialTextureSet.NONE, icon.blockPrefix()));
            }
        }
        // The creative-tab ore entries point straight at these two templates.
        models.add(new BlockModel(MaterialTextureSet.NONE, BlockMaterialPrefix.ore));
        models.add(new BlockModel(MaterialTextureSet.NONE, BlockMaterialPrefix.oreSmall));
        List<BlockModel> resolved = new ArrayList<>(models.size());
        for (BlockModel model : models) {
            if (MaterialIconDefinitions.resolveBlockTextureOwner(
                            model.set(), model.prefix().getTextureFileName(), MaterialIcons::resourceExists)
                    != null) {
                resolved.add(model);
            }
        }
        return resolved;
    }
}
