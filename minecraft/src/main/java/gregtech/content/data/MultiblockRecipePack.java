package gregtech.content.data;

import com.google.gson.Gson;
import gregtech.api.material.GTMaterial;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.block.machine.BasicMachineBlock;
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
//? if >=1.21.1 {
/*import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
//?}

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
//? if >=1.21.1 {
/*@EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
*///?} else {
@Mod.EventBusSubscriber(modid = gregtech.engine.mod.GregTechIdentity.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?}
public final class MultiblockRecipePack extends AbstractPackResources {
    private static final Gson GSON = new Gson();
    /** Material used when a recipe is not bound to a machine spec. */
    private static final String DEFAULT_MATERIAL = "StainlessSteel";

    // Loader_MultiTileEntities' Umber/Livingrock/Holystone/Betweenstone utilities use external
    // OP.stone entries. Without those mods the original ore ingredient cannot match anything;
    // omit that unavailable row instead of letting the machine-ingredient fallback use iron.
    private static final Set<String> OPTIONAL_STONE_INPUTS = Set.of(
            "gt6ce:stone_umber_stone", "gt6ce:stone_livingrock_stone",
            "gt6ce:stone_holystone_stone", "gt6ce:stone_betweenstone_stone");

    private Map<ResourceLocation, byte[]> resources;
    private static final Set<String> EMITTED = new HashSet<>();

//? if >=1.21.1 {
    /*public MultiblockRecipePack(net.minecraft.server.packs.PackLocationInfo info) {
        super(info);
*///?} else {
    public MultiblockRecipePack(String id) {
        super(id, true);
//?}
    }

    @SubscribeEvent
    public static void packs(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(output -> {
//? if >=1.21.1 {
            /*var location=new net.minecraft.server.packs.PackLocationInfo("gt6ce:multiblock_recipes",Component.literal("GregTech multiblock machine manufacturing recipes"),PackSource.BUILT_IN,java.util.Optional.empty());
            Pack pack=Pack.readMetaAndCreate(location,new Pack.ResourcesSupplier(){
                public PackResources openPrimary(net.minecraft.server.packs.PackLocationInfo info){return new MultiblockRecipePack(info);}
                public PackResources openFull(net.minecraft.server.packs.PackLocationInfo info,Pack.Metadata metadata){return new MultiblockRecipePack(info);}
            },PackType.SERVER_DATA,new net.minecraft.server.packs.PackSelectionConfig(true,Pack.Position.BOTTOM,true));
*///?} else {
            Pack pack = Pack.readMetaAndCreate("gt6ce:multiblock_recipes",
                    Component.literal("GregTech multiblock machine manufacturing recipes"), true,
                    MultiblockRecipePack::new, PackType.SERVER_DATA, Pack.Position.BOTTOM,
                    PackSource.BUILT_IN);
//?}
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
            // The native wall family owns these rows, including its complete tool tags.
            if (OriginalHandCraftingRows.hasMetalWallRecipe(entry.blockId())) continue;
            ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath("gt6ce", entry.blockId());
//? if >=1.21.1 {
            /*Block block = BuiltInRegistries.BLOCK.get(blockId);
*///?} else {
            Block block = ForgeRegistries.BLOCKS.getValue(blockId);
//?}
            if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) continue;
            Item item = block.asItem();
            if (item == net.minecraft.world.item.Items.AIR) continue;

            GTMaterial material = materialOf(block);
            Map<String, Object> recipe = buildRecipe(entry, material, item);
            if (recipe == null) continue;

            generated.put(ResourceLocation.fromNamespaceAndPath("gt6ce",
//? if >=1.21.1 {
                    /*"recipe/machines/multiblock/" + entry.blockId() + ".json"),
*///?} else {
                    "recipes/machines/multiblock/" + entry.blockId() + ".json"),
//?}
                    GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));
            EMITTED.add(entry.blockId());
        }

//? if >=1.21.1 {
        /*com.mojang.logging.LogUtils.getLogger().info("[gregtech] Original multiblock crafting datapack: {} emitted, {} not registered or unresolved", generated.size(), MultiblockCraftingRecipes.ENTRIES.size()-generated.size());
*///?} else {
//?}
        for (var row : java.util.stream.Stream.concat(OriginalSurvivalRecipes.allRows().stream(), UtilityCraftingRows.ROWS.stream()).toList()) {
            var blockId = ResourceLocation.fromNamespaceAndPath("gt6ce", row.id());
//? if >=1.21.1 {
            /*Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(blockId);
*///?} else {
            Item item = ForgeRegistries.ITEMS.getValue(blockId);
//?}
            if (item == null || item == net.minecraft.world.item.Items.AIR)
                throw new IllegalStateException("Unregistered original recipe output " + row.id());
            if (!concreteInputsAvailable(row)) continue;
            var recipe = buildRecipe(row.entry(), row.material(), item);
            if (recipe == null) throw new IllegalStateException("Unresolved original recipe " + row.id());
//? if >=1.21.1 {
            /*recipe.put("result", Map.of("id", row.id().contains(":") ? row.id() : "gt6ce:" + row.id(), "count", row.count()));
            generated.put(ResourceLocation.fromNamespaceAndPath("gt6ce", "recipe/survival_repairs/" + row.id() + ".json"),
*///?} else {
            recipe.put("result", Map.of("item", row.id().contains(":") ? row.id() : "gt6ce:" + row.id(), "count", row.count()));
            generated.put(ResourceLocation.fromNamespaceAndPath("gt6ce", "recipes/survival_repairs/" + row.id() + ".json"),
//?}
                    GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));
            EMITTED.add(row.id());
        }

        addClayUtilityRecipes(generated);
        resources = Collections.unmodifiableMap(generated);
        return resources;
    }

    private static boolean concreteInputsAvailable(OriginalSurvivalRecipes.Row row) {
        boolean available = true;
        for (String spec : row.keys().values()) {
            if (!spec.startsWith("item:")) continue;
            String id = spec.substring(5);
            Item input = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            if (input != null && input != net.minecraft.world.item.Items.AIR) continue;
            if (!OPTIONAL_STONE_INPUTS.contains(id))
                throw new IllegalStateException("Unregistered original recipe input " + id + " for " + row.id());
            available = false;
        }
        return available;
    }

    /** Original raw utilities return their clay before firing; only firing produces ceramic parts. */
    private static void addClayUtilityRecipes(Map<ResourceLocation,byte[]> generated) {
        for(var row:gregtech.content.recipe.ClayUtilityCatalog.ROWS) {
            for(String type:List.of("smelting","blasting")) {
                var firing=new HashMap<String,Object>();
                firing.put("type","minecraft:"+type);
                firing.put("ingredient",Map.of("item","gt6ce:"+row.raw()));
//? if >=1.21.1 {
                /*firing.put("result",Map.of("id","gt6ce:"+row.fired()));
*///?} else {
                firing.put("result","gt6ce:"+row.fired());
//?}
                firing.put("experience",0);
                firing.put("cookingtime",type.equals("smelting")?200:100);
                clayRecipe(generated,type+"_"+row.raw(),firing);
            }
            var reclaim=new HashMap<String,Object>();
            reclaim.put("type","minecraft:crafting_shapeless");
            reclaim.put("ingredients",List.of(Map.of("item","gt6ce:"+row.raw())));
//? if >=1.21.1 {
            /*reclaim.put("result",Map.of("id","minecraft:clay_ball","count",row.clayBalls()));
*///?} else {
            reclaim.put("result",Map.of("item","minecraft:clay_ball","count",row.clayBalls()));
//?}
            clayRecipe(generated,"reclaim_"+row.raw(),reclaim);
        }
        // MultiItemRandomTools:130 also permits the vertically reversed crossing layout.
        var crossing=new HashMap<String,Object>();
        crossing.put("type","gt6ce:tool_shaped");
        crossing.put("pattern",List.of(" C ","CCC","kCR"));
        crossing.put("key",Map.of("C",Map.of("tag","gt6ce:clay_balls"),
                "k",Map.of("item","gt6ce:tool_knife"),"R",Map.of("item","gt6ce:tool_rolling_pin")));
        crossing.put("allow_mirror",false);
//? if >=1.21.1 {
        /*crossing.put("result",Map.of("id","gt6ce:clay_crossing"));
*///?} else {
        crossing.put("result",Map.of("item","gt6ce:clay_crossing"));
//?}
        clayRecipe(generated,"form_clay_crossing_flipped",crossing);
    }

    private static void clayRecipe(Map<ResourceLocation,byte[]> generated,String name,Map<String,Object> recipe) {
//? if >=1.21.1 {
        /*String folder="recipe/";
*///?} else {
        String folder="recipes/";
//?}
        generated.put(ResourceLocation.fromNamespaceAndPath("gt6ce",folder+"ceramic_utilities/"+name+".json"),
                GSON.toJson(recipe).getBytes(StandardCharsets.UTF_8));
    }

    private static GTMaterial materialOf(Block block) {
        if (block instanceof BasicMachineBlock machine) {
            return machine.basicSpec().material();
        }
        return GTMaterialRegistry.get(DEFAULT_MATERIAL);
    }

    private static Map<String, Object> buildRecipe(MultiblockCraftingRecipes.Entry entry, GTMaterial material,
                                                   Item resultItem) {
        Map<Character, String> specs = entry.keys();
        if (entry.blockId().equals("coke_oven_main")) {
            specs = new HashMap<>(specs);
            specs.put('P', "tag:gt6ce:technology/any_iron_plate");
            specs.put('R', "tag:gt6ce:technology/any_iron_rod");
            specs.put('T', "tag:gt6ce:technology/any_iron_screw");
        }
        Map<Character, Object> ingredients = MachineRecipeIngredients.resolveAll(specs, material, 1);
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

//? if >=1.21.1 {
        /*ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(resultItem);
*///?} else {
        ResourceLocation resultId = ForgeRegistries.ITEMS.getKey(resultItem);
//?}
        if (resultId == null) return null;

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("type", "gt6ce:tool_shaped");
        recipe.put("pattern", pattern);
        recipe.put("key", key);
        // Multiblock recipes come from GT6's aRegistry.add(...) rows as well: CR.DEF_REV, no MIR.
        recipe.put("allow_mirror", false);
//? if >=1.21.1 {
        /*recipe.put("result", Map.of("id", resultId.toString()));
*///?} else {
        recipe.put("result", Map.of("item", resultId.toString()));
//?}
        recipe.put("group", "gregtech_multiblocks");
        return recipe;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length != 1 || !path[0].equals("pack.mcmeta")) return null;
//? if >=1.21.1 {
        /*return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":48,\"description\":"
*///?} else {
        return () -> new ByteArrayInputStream(("{\"pack\":{\"pack_format\":15,\"description\":"
//?}
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
        return type == PackType.SERVER_DATA ? Set.of("gt6ce") : Set.of();
    }

    @Override
    public void close() {
        resources = null;
    }
}
