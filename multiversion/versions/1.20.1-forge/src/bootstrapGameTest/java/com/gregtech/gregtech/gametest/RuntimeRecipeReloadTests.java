package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.recipe.RuntimeRecipeLifecycle;
import com.gregtech.gregtech.registry.GTItems;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.*;
import java.util.*;

/** Real server reloads, including a temporary data pack in this GameTest world's own directory. */
@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class RuntimeRecipeReloadTests {
    private static final ResourceLocation OVERRIDE=GregTech.id("hand/storage/bottle_crate");
    private static final ResourceLocation PROBE=ResourceLocation.fromNamespaceAndPath("gregtech_repair","reload_probe");
    private static final ResourceLocation NEVER=ResourceLocation.fromNamespaceAndPath("gregtech_repair","reload_never_furnace");

    @GameTest(template="test_empty",batch="runtime_recipe_reload",timeoutTicks=4000)
    public static void realReloadsPreserveGeneratedRecipesOverridesAndFurnaceMirrors(GameTestHelper h) throws Exception {
        var server=h.getLevel().getServer(); var manager=server.getRecipeManager();
        h.assertTrue(manager.byKey(OVERRIDE).isPresent(),"generated recipe exists on initial startup");
        var baseline=snapshot(manager.getRecipes(),server.registryAccess());
        var ids=new ArrayList<>(server.getPackRepository().getSelectedIds());
        RuntimeRecipeLifecycle.rebuild(server);
        h.assertTrue(snapshot(manager.getRecipes(),server.registryAccess()).equals(baseline),"same-manager sync is idempotent");
        for(int round=0;round<2;round++) {
            server.reloadResources(ids).join();
            assertSame(h,baseline,snapshot(server.getRecipeManager().getRecipes(),server.registryAccess()),"real reload "+round);
        }
        packetRoundTrip(h,server.getRecipeManager().getRecipes());

        Path root=server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("gt-repair-reload-probe");
        h.assertTrue(!Files.exists(root),"temporary probe pack must not overwrite an existing pack");
        Path override=root.resolve("data/gregtech/recipes/hand/storage/bottle_crate.json");
        Path probe=root.resolve("data/gregtech_repair/recipes/reload_probe.json");
        Path never=root.resolve("data/gregtech_repair/recipes/reload_never_furnace.json");
        Path rail=root.resolve("data/minecraft/recipes/rail.json");
        Path metadata=root.resolve("pack.mcmeta");
        Files.createDirectories(override.getParent()); Files.createDirectories(probe.getParent()); Files.createDirectories(rail.getParent());
        try {
            Files.writeString(metadata,"{\"pack\":{\"pack_format\":15,\"description\":\"Temporary GT recipe reload regression probe\"}}");
            Files.writeString(override,"{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"minecraft:dirt\"}],\"result\":{\"item\":\"minecraft:diamond\",\"count\":3}}");
            Files.writeString(rail,"{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"minecraft:dirt\"}],\"result\":{\"item\":\"minecraft:rail\",\"count\":3}}");
            Files.writeString(probe,smelting("minecraft:structure_void","minecraft:emerald"));
            String steel=ForgeRegistries.ITEMS.getKey(GTItems.getStack(MaterialPrefix.ingot,Materials.Steel).getItem()).toString();
            Files.writeString(never,smelting("minecraft:jigsaw",steel));
            h.assertTrue(!hasMirror(Items.STRUCTURE_VOID),"probe input is absent from baseline GT furnace");
            server.getPackRepository().reload();
            var withProbe=new ArrayList<>(ids); withProbe.add("file/gt-repair-reload-probe");
            server.reloadResources(withProbe).join();
            var changed=server.getRecipeManager();
            var output=changed.byKey(OVERRIDE).orElseThrow().getResultItem(server.registryAccess());
            h.assertTrue(output.is(Items.DIAMOND)&&output.getCount()==3,"data-pack override wins over generated same-ID recipe");
            h.assertTrue(changed.byKey(ResourceLocation.fromNamespaceAndPath("minecraft","rail")).orElseThrow().getResultItem(server.registryAccess()).getCount()==3,"explicit data-pack vanilla rail replacement is preserved");
            h.assertTrue(hasMirror(Items.STRUCTURE_VOID),"new data-pack smelting recipe reaches GT furnace");
            var scrap=changed.byKey(NEVER).orElseThrow().getResultItem(server.registryAccess());
            h.assertTrue(ItemStack.matches(scrap,MachineRecipeMaps.neverFurnaceOutput(GTItems.getStack(MaterialPrefix.ingot,Materials.Steel))),"NEVER_FURNACE rule reapplied after reload");
            packetRoundTrip(h,changed.getRecipes());
            server.reloadResources(ids).join();
            h.assertTrue(!hasMirror(Items.STRUCTURE_VOID),"removed data-pack recipe leaves no stale machine mirror");
            h.assertTrue(server.getRecipeManager().byKey(PROBE).isEmpty()&&server.getRecipeManager().byKey(NEVER).isEmpty(),"disabled pack rows removed");
            assertSame(h,baseline,snapshot(server.getRecipeManager().getRecipes(),server.registryAccess()),"after removing override pack");
        } finally {
            if(!new HashSet<>(server.getPackRepository().getSelectedIds()).equals(new HashSet<>(ids))) server.reloadResources(ids).join();
            // Only files/directories created above; never recursively remove a computed user path.
            for(Path path:List.of(override,probe,never,rail,metadata,rail.getParent(),root.resolve("data/minecraft"),override.getParent(),override.getParent().getParent(),
                    override.getParent().getParent().getParent(),root.resolve("data/gregtech"),probe.getParent(),root.resolve("data/gregtech_repair"),root.resolve("data"),root))
                Files.deleteIfExists(path);
        }
        h.succeed();
    }

    private static String smelting(String input,String output) {
        return "{\"type\":\"minecraft:smelting\",\"ingredient\":{\"item\":\""+input+"\"},\"result\":\""+output+"\",\"experience\":0.0,\"cookingtime\":200}";
    }
    private static boolean hasMirror(net.minecraft.world.item.Item item) {
        return MachineRecipeMaps.Furnace.mRecipeList.stream().anyMatch(r->Arrays.stream(r.mInputs).anyMatch(s->!s.isEmpty()&&s.is(item)));
    }
    private static Map<String,String> snapshot(Collection<Recipe<?>> recipes,net.minecraft.core.RegistryAccess access) {
        var result=new TreeMap<String,String>();
        for(var recipe:recipes) {
            StringBuilder value=new StringBuilder(recipe.getType().toString()).append('|').append(recipe.getClass().getName()).append('|')
                    .append(recipe.getResultItem(access).save(new CompoundTag()));
            if(recipe instanceof ShapedRecipe shaped) value.append('|').append(shaped.getWidth()).append('x').append(shaped.getHeight());
            if(recipe instanceof AbstractCookingRecipe cooking) value.append('|').append(cooking.getCookingTime()).append('|').append(cooking.getExperience());
            if(recipe instanceof com.gregtech.gregtech.recipe.ToolShapedRecipe tools) value.append('|').append(tools.allowMirror());
            for(var ingredient:recipe.getIngredients()) value.append('|').append(Arrays.stream(ingredient.getItems())
                    .map(s->s.save(new CompoundTag()).toString()).sorted().toList());
            if(result.put(recipe.getId().toString(),value.toString())!=null) throw new AssertionError("duplicate recipe id "+recipe.getId());
        }
        return result;
    }
    private static void assertSame(GameTestHelper h,Map<String,String> before,Map<String,String> after,String stage) {
        var changed=new TreeSet<String>(); changed.addAll(before.keySet()); changed.addAll(after.keySet());
        changed.removeIf(id->Objects.equals(before.get(id),after.get(id)));
        h.assertTrue(changed.isEmpty(),stage+": recipe content changed: "+changed.stream().limit(5).toList()+" ("+changed.size()+" IDs)");
    }
    private static void packetRoundTrip(GameTestHelper h,Collection<Recipe<?>> recipes) {
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try {
            new ClientboundUpdateRecipesPacket(recipes).write(buffer);
            var decoded=new ClientboundUpdateRecipesPacket(buffer);
            h.assertTrue(!buffer.isReadable(),"complete recipe sync packet decoded without trailing bytes");
            assertSame(h,snapshot(recipes,h.getLevel().registryAccess()),snapshot(decoded.getRecipes(),h.getLevel().registryAccess()),"client packet round trip");
        } finally {buffer.release();}
    }
}
