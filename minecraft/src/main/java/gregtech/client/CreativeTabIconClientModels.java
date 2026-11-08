package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;


import gregtech.api.material.MaterialTextureSet;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.CreativeTabIconItem;
import gregtech.content.registry.GTCreativeTabIcons;
import gregtech.platform.Mc;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?}
import org.jetbrains.annotations.Nullable;

//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class CreativeTabIconClientModels {
    private static final MaterialTextureSet ICON_SET = MaterialTextureSet.NONE;

    private CreativeTabIconClientModels() {}

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        BlockItemModelHelper.captureFromVanillaBlockItem(models);

        for (var entry : GTCreativeTabIcons.allEntries()) {
            if (!(entry.get() instanceof CreativeTabIconItem iconItem)) {
                continue;
            }
            ResourceLocation itemId = Mc.itemId(entry.get());
            if (itemId == null) {
                continue;
            }
            BakedModel shared = resolveIconModel(models, iconItem);
            if (shared == null) {
                continue;
            }
            models.put(itemId, "inventory", shared);
        }
    }

    @Nullable
    private static BakedModel resolveIconModel(BakedModelMap models, CreativeTabIconItem iconItem) {
        MaterialPrefix materialPrefix = iconItem.materialPrefix();
        if (materialPrefix != null) {
            return lookupItemModel(models, materialPrefix);
        }
        BlockMaterialPrefix blockPrefix = iconItem.blockPrefix();
        if (blockPrefix != null) {
            return lookupBlockModel(models, blockPrefix);
        }
        return null;
    }

    private static BakedModel lookupItemModel(BakedModelMap models, MaterialPrefix prefix) {
        return models.find(MaterialIcons.sharedModelLocation(ICON_SET, prefix));
    }

    private static BakedModel lookupBlockModel(BakedModelMap models, BlockMaterialPrefix prefix) {
        BakedModel shared = models.find(BlockMaterialIcons.sharedModelLocation(ICON_SET, prefix));
        return shared == null ? null : BlockItemClientModels.asBlockItem(shared);
    }
}
