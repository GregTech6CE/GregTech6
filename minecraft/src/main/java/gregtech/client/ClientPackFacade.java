package gregtech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
//?}
import java.util.Optional;

/** Loader-specific pack and geometry-loader wiring kept in one place. */
public final class ClientPackFacade {
    private ClientPackFacade() {}

    public static Pack createSharedInventoryPack() {
//? if >=1.21.1 {
        /*var location = new PackLocationInfo("gt6ce:shared_inventory",
                Component.literal("GregTech shared inventory models"), PackSource.BUILT_IN, Optional.empty());
        return Pack.readMetaAndCreate(location, new Pack.ResourcesSupplier() {
            @Override
            public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo info) {
                return new SharedInventoryModelPack(info);
            }

            @Override
            public net.minecraft.server.packs.PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return new SharedInventoryModelPack(info);
            }
        }, PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.BOTTOM, true));
*///?} else {
        return Pack.readMetaAndCreate("gt6ce:shared_inventory",
                Component.literal("GregTech shared inventory models"), true,
                SharedInventoryModelPack::new, PackType.CLIENT_RESOURCES, Pack.Position.BOTTOM, PackSource.BUILT_IN);
//?}
    }

    public static void registerGeometryLoader(ModelEvent.RegisterGeometryLoaders event, ResourceLocation id,
                                              IGeometryLoader<SharedInventoryModelPack.Reference> loader) {
//? if >=1.21.1 {
        /*event.register(id, loader);
*///?} else {
        event.register(id.getPath(), loader);
//?}
    }
}
