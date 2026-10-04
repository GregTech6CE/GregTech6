package com.gregtech.gregtech.data;

import com.google.gson.Gson;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.registry.GTBasicMachines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Crafting recipes for the single-block machines, generated from the original GT6
 * "Basic Machines" registrations (see {@link BasicMachineCraftingRecipes}).
 * <p>
 * The previous implementation invented three generic patterns based only on the energy
 * type, so every machine's crafting grid differed from GregTech 6. This pack now emits
 * the original pattern rows and key symbols for every tier, with ingredient translation
 * handled by {@link MachineRecipeIngredients}.
 * </p>
 */
@Mod.EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BasicMachineRecipePack extends AbstractPackResources {
    /** Machines whose crafting recipe comes from the multiblock table instead. */
    public static final Set<String> MULTIBLOCK_CONTROLLERS = Set.of(
            "fusionreactor", "cryodistillationtower", "distillationtower",
            "cokeoven", "implosioncompressor",
            "largecentrifuge", "largeelectrolyzer", "largecoagulator", "largeautoclave",
            "largebath", "largemixer", "largefermenter", "largeoven", "largesluice",
            "largecrusher", "largeshredder", "largesqueezer", "largemassfab");
    private static final Gson GSON = new Gson();

    private Map<ResourceLocation, byte[]> resources;

    public BasicMachineRecipePack(String id) {
        super(id, true);
    }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
            Pack pack = Pack.readMetaAndCreate("gregtech:basic_machine_recipes",
                    Component.literal("GregTech basic machine manufacturing recipes"), true,
                    BasicMachineRecipePack::new, PackType.SERVER_DATA, Pack.Position.BOTTOM,
                    PackSource.BUILT_IN);
            if (pack != null) output.accept(pack);
        });
    }

    private synchronized Map<ResourceLocation, byte[]> data() {
        if (resources != null) return resources;
        Map<ResourceLocation, byte[]> generated = new HashMap<>();

        for (var entry : GTBasicMachines.all()) {
            if (!entry.isPresent()) continue;
            BasicMachineBlock machine = entry.get();
            var spec = machine.basicSpec();
            if (MULTIBLOCK_CONTROLLERS.contains(spec.machineName())) continue;

            ResourceLocation outputId = ForgeRegistries.ITEMS.getKey(machine.asItem());
            if (outputId == null) continue;

            BasicMachineCraftingRecipes.Entry table = BasicMachineCraftingRecipes.find(spec.machineName(), spec.tier());
            if (table == null) continue;

            Map<String, Object> recipe = buildRecipe(table, spec.material(), spec.tier(), outputId);
            if (recipe == null) continue;

            generated.put(ResourceLocation.fromNamespaceAndPath("gregtech",
                    "recipes/machines/basic/" + spec.id() + ".json"),
                    GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));
        }

        // The pack data is requested once per datapack reload; report translations here.
        MachineRecipeIngredients.logSubstitutions();
        resources = Collections.unmodifiableMap(generated);
        return resources;
    }

    private static Map<String, Object> buildRecipe(BasicMachineCraftingRecipes.Entry table, GTMaterial material,
                                                   int tier, ResourceLocation outputId) {
        Map<Character, Object> ingredients = MachineRecipeIngredients.resolveAll(table.keys(), material, tier);
        Map<String, Object> key = new HashMap<>();
        for (var entry : ingredients.entrySet()) {
            key.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        if (key.isEmpty()) return null;

        List<String> pattern = new ArrayList<>(List.of(table.rows()));
        // GT6 omits trailing all-space rows from the shaped pattern.
        while (!pattern.isEmpty() && pattern.get(pattern.size() - 1).isBlank()) {
            pattern.remove(pattern.size() - 1);
        }
        if (pattern.isEmpty()) return null;

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("type", "gregtech:tool_shaped");
        recipe.put("pattern", pattern);
        recipe.put("key", key);
        // GT6's machine patterns are aRegistry.add(...) recipes, i.e. CR.DEF_REV without the MIR
        // flag (CR.java:161-163); 1.20.1 mirrors shaped recipes by default.
        recipe.put("allow_mirror", false);
        recipe.put("result", Map.of("item", outputId.toString()));
        recipe.put("group", "gregtech_machines");
        return recipe;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
        return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":15,\"description\":"
                + "\"GregTech basic machine recipes\"}}").getBytes(StandardCharsets.UTF_8));
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
