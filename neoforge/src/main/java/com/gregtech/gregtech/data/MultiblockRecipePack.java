package com.gregtech.gregtech.data;

import com.google.gson.Gson;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Crafting recipes for the multiblock machines and their structural parts, generated from the
 * original GT6 "Multiblock Machines" registrations (see {@link MultiblockCraftingRecipes}).
 * <p>
 * Before this pack the multiblock family had no recipe at all: {@link BasicMachineRecipePack}
 * skips the multiblock machine types and no resource-pack recipe existed for the controller
 * blocks, so every one of them was unobtainable in survival.
 * </p>
 */
@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class MultiblockRecipePack extends AbstractPackResources {
    private static final Gson GSON = new Gson();
    /** Material used when a recipe is not bound to a machine spec. */
    private static final String DEFAULT_MATERIAL = "StainlessSteel";

    private Map<ResourceLocation, byte[]> resources;
    private static final Set<String> EMITTED = new HashSet<>();

    public MultiblockRecipePack(net.minecraft.server.packs.PackLocationInfo info) {
        super(info);
    }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
            var location=new net.minecraft.server.packs.PackLocationInfo("gregtech:multiblock_recipes",Component.literal("GregTech multiblock machine manufacturing recipes"),PackSource.BUILT_IN,java.util.Optional.empty());
            Pack pack=Pack.readMetaAndCreate(location,new Pack.ResourcesSupplier(){
                public PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info){return new MultiblockRecipePack(info);}
                public PackResources openFull(net.minecraft.server.packs.PackLocationInfo info,Pack.Metadata metadata){return new MultiblockRecipePack(info);}
            },PackType.SERVER_DATA,new net.minecraft.server.packs.PackSelectionConfig(true,Pack.Position.BOTTOM,true));
            if (pack != null) output.accept(pack);
        });
    }

    /** Port block ids that received a recipe in the last generated pack. */
    public static Set<String> emittedBlockIds() {
        return Collections.unmodifiableSet(EMITTED);
    }

    private synchronized Map<ResourceLocation, byte[]> data() {
        if (resources != null) return resources;
        Map<ResourceLocation, byte[]> generated = new HashMap<>();
        EMITTED.clear();

        for (MultiblockCraftingRecipes.Entry entry : MultiblockCraftingRecipes.ENTRIES) {
            ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath("gregtech", entry.blockId());
            Block block = BuiltInRegistries.BLOCK.get(blockId);
            if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) continue;
            Item item = block.asItem();
            if (item == net.minecraft.world.item.Items.AIR) continue;

            GTMaterial material = materialOf(block);
            Map<String, Object> recipe = buildRecipe(entry, material, item);
            if (recipe == null) continue;

            generated.put(ResourceLocation.fromNamespaceAndPath("gregtech",
                    "recipe/machines/multiblock/" + entry.blockId() + ".json"),
                    GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));
            EMITTED.add(entry.blockId());
        }

        com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original multiblock crafting datapack: {} emitted, {} not registered or unresolved", generated.size(), MultiblockCraftingRecipes.ENTRIES.size()-generated.size());
        resources = Collections.unmodifiableMap(generated);
        return resources;
    }

    private static GTMaterial materialOf(Block block) {
        if (block instanceof BasicMachineBlock machine) {
            return machine.basicSpec().material();
        }
        return GTMaterialRegistry.get(DEFAULT_MATERIAL);
    }

    private static Map<String, Object> buildRecipe(MultiblockCraftingRecipes.Entry entry, GTMaterial material,
                                                   Item resultItem) {
        Map<Character, Object> ingredients = MachineRecipeIngredients.resolveAll(entry.keys(), material, 1);
        if (ingredients.isEmpty()) return null;
        Map<String, Object> key = new HashMap<>();
        for (var pair : ingredients.entrySet()) {
            key.put(String.valueOf(pair.getKey()), pair.getValue());
        }

        List<String> pattern = new ArrayList<>(List.of(entry.rows()));
        while (!pattern.isEmpty() && pattern.get(pattern.size() - 1).isBlank()) {
            pattern.remove(pattern.size() - 1);
        }
        if (pattern.isEmpty()) return null;

        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(resultItem);
        if (resultId == null) return null;

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("type", "gregtech:tool_shaped");
        recipe.put("pattern", pattern);
        recipe.put("key", key);
        // Multiblock recipes come from GT6's aRegistry.add(...) rows as well: CR.DEF_REV, no MIR.
        recipe.put("allow_mirror", false);
        recipe.put("result", Map.of("id", resultId.toString()));
        recipe.put("group", "gregtech_multiblocks");
        return recipe;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
        return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":48,\"description\":"
                + "\"GregTech multiblock recipes\"}}").getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != PackType.SERVER_DATA) return null;
        byte[] bytes = data().get(id);
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType type, String namespace, String path,
                              PackResources.ResourceOutput output) {
        if (type != PackType.SERVER_DATA) return;
        data().forEach((id, bytes) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(path + "/")) {
                output.accept(id, () -> new ByteArrayInputStream(bytes));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? Set.of("gregtech") : Set.of();
    }

    @Override
    public void close() {
        resources = null;
    }
}
