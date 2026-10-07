package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.TrackBlock;
import com.gregtech.gregtech.loaders.Loader_TrackRecipes;
import com.gregtech.gregtech.registry.GTTrackBlocks;
import com.gregtech.gregtech.registry.GTIconSetBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * GT6's tracks ({@code Loader_Rails:41-72}, {@link GTTrackBlocks}).
 *
 * <p>The portable part of a GT6 rail is its top speed: Forge 1.20.1 routes
 * {@code IForgeBaseRailBlock.getRailMaxSpeed} through {@code AbstractMinecart.moveMinecartOnRail},
 * so GT6's straight-run rule can be checked directly. The cart's own ceiling
 * ({@code IForgeAbstractMinecart.getMaxCartSpeedOnRail()}, 1.2) still caps the three fastest rails,
 * which the last test pins so the difference can never drift silently.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class TrackTests {

    private static final BlockPos POS = new BlockPos(3, 2, 3);

    /** A rail needs a floor under it, exactly like a placed vanilla rail. */
    private static void floor(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel.below(), Blocks.STONE);
    }

    /** All thirty tracks are registered with GT6's speed and resistance. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void gt6ParametersAreRegistered(GameTestHelper h) {
        List<GTTrackBlocks.Track> tracks = GTTrackBlocks.all();
        h.assertTrue(tracks.size() == 30, "GT6 registers ten materials x three families: " + tracks.size());
        String[][] expected = {
                {"Aluminium", "0.20", "6"}, {"Bronze", "0.30", "8"}, {"Magnalium", "0.60", "12"},
                {"Steel", "0.60", "12"}, {"StainlessSteel", "0.80", "10"}, {"Tungsten", "1.00", "20"},
                {"Titanium", "1.20", "16"}, {"Tungstensteel", "1.40", "20"},
                {"TungstenCarbide", "1.60", "24"}, {"Adamantium", "4.00", "100"}};
        int index = 0;
        for (String[] row : expected) {
            for (GTTrackBlocks.Family family : GTTrackBlocks.Family.values()) {
                GTTrackBlocks.Track track = tracks.get(index++);
                h.assertTrue(track.material().equals(row[0]) && track.family() == family,
                        "registration order is GT6's: " + track.id());
                h.assertTrue(Math.abs(track.speed() - Float.parseFloat(row[1])) < 1e-6,
                        track.id() + " speed " + track.speed() + " != GT6 " + row[1]);
                h.assertTrue(Math.abs(track.resistance() - Float.parseFloat(row[2])) < 1e-6,
                        track.id() + " resistance " + track.resistance() + " != GT6 " + row[2]);
                h.assertTrue(track.block().isPresent(), track.id() + " registered a block");
            }
        }
        h.assertTrue(GTTrackBlocks.get("track_booster_adamantium") != null, "tracks are addressable by id");
        h.assertTrue(GTTrackBlocks.boosterMaterial("Adamantium").equals("Osmium")
                        && GTTrackBlocks.boosterMaterial("Steel").equals("Gold")
                        && GTTrackBlocks.boosterMaterial("Aluminium").equals("Silver"),
                "GT6's per-rail booster ingredient material");
        h.succeed();
    }

    /** GT6's six rail icon variants are textures of tracks, never extra blocks/items. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void railIconTexturesDoNotRegisterAsBlocks(GameTestHelper h) {
        String[] materials = {"adamantium", "aluminium", "bronze", "iron", "magnalium",
                "stainlesssteel", "steel", "titanium", "tungsten", "tungstencarbide",
                "tungstensteel"};
        String[] icons = {"rail_straight_", "rail_turned_", "rail_booster_",
                "rail_booster_active_", "rail_detector_", "rail_detector_active_"};
        int checked = 0;
        for (String material : materials) {
            for (String icon : icons) {
                String id = icon + material;
                ResourceLocation key = new ResourceLocation("gregtech", id);
                h.assertTrue(!ForgeRegistries.BLOCKS.containsKey(key)
                                && !ForgeRegistries.ITEMS.containsKey(key),
                        id + " is a rail texture, not a GT6 BlockRailBase registration");
                checked++;
            }
        }
        h.assertTrue(checked == 66, "all 66 icon variants checked");
        h.assertTrue(GTIconSetBlocks.all().stream().noneMatch(entry ->
                        entry.getId().getPath().startsWith("rail_")),
                "the iconset registry does not expose rail textures as blocks");
        h.assertTrue(ForgeRegistries.BLOCKS.containsKey(new ResourceLocation("gregtech", "railroad")),
                "GT6's separate Road Stripe rail remains registered");
        h.succeed();
    }

    /** Forge ignores rails outside {@code #minecraft:rails}, so every track must be in the tag. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void everyTrackIsARailTagMember(GameTestHelper h) {
        int tagged = 0;
        for (GTTrackBlocks.Track track : GTTrackBlocks.all()) {
            BlockState state = track.block().get().defaultBlockState();
            h.assertTrue(state.is(BlockTags.RAILS), track.id() + " is in #minecraft:rails");
            h.assertTrue(state.getBlock() instanceof BaseRailBlock, track.id() + " is a rail block");
            tagged++;
        }
        h.assertTrue(tagged == 30, "rails checked: " + tagged);
        h.succeed();
    }

    /** GT6's straight-run rule: full speed on a straight run, 0.4 on a curve or a dead end. */
    @GameTest(template = "test_blueprint_empty", timeoutTicks = 300)
    public static void speedFollowsGt6sStraightRunRule(GameTestHelper h) {
        var level = h.getLevel();
        AbstractMinecart cart = EntityType.MINECART.create(level);
        h.assertTrue(cart != null, "a minecart can be created for the query");

        // Track is laid on a floor: GT6's rails are placed like vanilla's.
        for (int z = 0; z < 3; z++) {
            floor(h, POS.offset(0, 0, z));
            h.setBlock(POS.offset(0, 0, z), GTTrackBlocks.get("track_adamantium").block().get());
        }
        BlockPos middlePos = h.absolutePos(POS.offset(0, 0, 1));
        BlockState middle = level.getBlockState(middlePos);
        h.assertTrue(middle.getBlock() instanceof BaseRailBlock,
                "the middle rail is still there: " + middle + " at " + middlePos);
        h.assertTrue(middle.getValue(((BaseRailBlock) middle.getBlock()).getShapeProperty())
                        == RailShape.NORTH_SOUTH,
                "the placed track runs north-south: " + middle);

        float straight = ((BaseRailBlock) middle.getBlock()).getRailMaxSpeed(middle, level, middlePos, cart);
        h.assertTrue(Math.abs(straight - 4.0F) < 1e-6, "a straight run gives GT6's 4.00: " + straight);

        // An end of the run is a dead end: GT6 caps it at the vanilla 0.4.
        BlockPos endPos = h.absolutePos(POS.offset(0, 0, 0));
        BlockState end = level.getBlockState(endPos);
        h.assertTrue(end.getBlock() instanceof BaseRailBlock, "the end rail is still there: " + end);
        float deadEnd = ((BaseRailBlock) end.getBlock()).getRailMaxSpeed(end, level, endPos, cart);
        h.assertTrue(Math.abs(deadEnd - 0.4F) < 1e-6, "a dead end is capped at 0.4: " + deadEnd);

        // A curve is capped too, even in the middle of a run.
        floor(h, POS.offset(1, 0, 1));
        h.setBlock(POS.offset(1, 0, 1), GTTrackBlocks.get("track_adamantium").block().get()
                .defaultBlockState().setValue(RailBlock.SHAPE, RailShape.SOUTH_EAST));
        BlockPos curvePos = h.absolutePos(POS.offset(1, 0, 1));
        BlockState curve = level.getBlockState(curvePos);
        h.assertTrue(curve.getBlock() instanceof BaseRailBlock, "the curve rail is still there: " + curve);
        float curved = ((BaseRailBlock) curve.getBlock()).getRailMaxSpeed(curve, level, curvePos, cart);
        h.assertTrue(Math.abs(curved - 0.4F) < 1e-6,
                "a curve is capped at 0.4 (shape " + curve.getValue(
                        ((BaseRailBlock) curve.getBlock()).getShapeProperty()) + "): " + curved);

        // The slow rails keep their own speed on a straight run.
        for (int z = 0; z < 3; z++) {
            h.setBlock(POS.offset(0, 0, z), GTTrackBlocks.get("track_aluminium").block().get());
        }
        BlockPos slowPos = h.absolutePos(POS.offset(0, 0, 1));
        BlockState slow = level.getBlockState(slowPos);
        h.assertTrue(slow.getBlock() instanceof BaseRailBlock, "the aluminium rail is still there: " + slow);
        float aluminium = ((BaseRailBlock) slow.getBlock()).getRailMaxSpeed(slow, level, slowPos, cart);
        h.assertTrue(Math.abs(aluminium - 0.20F) < 1e-6, "aluminium runs at GT6's 0.20: " + aluminium);

        for (int x = 0; x <= 1; x++) {
            for (int z = 0; z < 3; z++) {
                h.setBlock(POS.offset(x, 0, z), Blocks.AIR);
                h.setBlock(POS.offset(x, -1, z), Blocks.AIR);
            }
        }
        h.succeed();
    }

    /** GT6's booster: doubles a moving cart, brakes and stops an unpowered one. */
    @GameTest(template = "test_blueprint_empty", timeoutTicks = 300)
    public static void boosterBoostsAndBrakes(GameTestHelper h) {
        var level = h.getLevel();
        AbstractMinecart cart = EntityType.MINECART.create(level);
        h.assertTrue(cart != null, "a minecart can be created");
        floor(h, POS);
        cart.setPos(h.absolutePos(POS).getX() + 0.5, h.absolutePos(POS).getY(), h.absolutePos(POS).getZ() + 0.5);

        BlockState powered = GTTrackBlocks.get("track_booster_steel").block().get().defaultBlockState()
                .setValue(PoweredRailBlock.POWERED, true);
        h.setBlock(POS, powered);
        cart.setDeltaMovement(0.10D, 0, 0.20D);
        ((BaseRailBlock) powered.getBlock()).onMinecartPass(powered, level, h.absolutePos(POS), cart);
        h.assertTrue(Math.abs(cart.getDeltaMovement().x - 0.20D) < 1e-9
                        && Math.abs(cart.getDeltaMovement().z - 0.40D) < 1e-9,
                "a powered booster doubles the cart: " + cart.getDeltaMovement());

        BlockState idle = powered.setValue(PoweredRailBlock.POWERED, false);
        h.setBlock(POS, idle);
        cart.setDeltaMovement(0.10D, 0.05D, 0.20D);
        ((BaseRailBlock) idle.getBlock()).onMinecartPass(idle, level, h.absolutePos(POS), cart);
        h.assertTrue(Math.abs(cart.getDeltaMovement().x - 0.05D) < 1e-9
                        && Math.abs(cart.getDeltaMovement().z - 0.10D) < 1e-9
                        && Math.abs(cart.getDeltaMovement().y) < 1e-9,
                "an unpowered booster halves and levels the cart: " + cart.getDeltaMovement());

        cart.setDeltaMovement(0.01D, 0, 0.01D);
        ((BaseRailBlock) idle.getBlock()).onMinecartPass(idle, level, h.absolutePos(POS), cart);
        h.assertTrue(cart.getDeltaMovement().length() < 1e-9,
                "an unpowered booster stops a slow cart: " + cart.getDeltaMovement());
        h.setBlock(POS, Blocks.AIR);
        h.succeed();
    }

    /** GT6's thirty-track crafting rows exist, and the fastest rails hit the vanilla cart ceiling. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void recipesAndTheVanillaCartCeiling(GameTestHelper h) {
        // 30 track rows plus GT6's 15 replacements of the four vanilla rail recipes.
        h.assertTrue(Loader_TrackRecipes.registeredIds().size() == 45,
                "GT6 has one row per track plus the vanilla replacements: "
                        + Loader_TrackRecipes.registeredIds().size()
                        + " " + Loader_TrackRecipes.skipped());
        h.assertTrue(Loader_TrackRecipes.skipped().isEmpty(),
                "every GT6 rail row is registered: " + Loader_TrackRecipes.skipped());

        var recipes = h.getLevel().getRecipeManager().getRecipes();
        for (String id : Loader_TrackRecipes.registeredIds()) {
            h.assertTrue(recipes.stream().anyMatch(r -> r.getId().toString().equals(id)),
                    "the crafting manager holds " + id);
        }
        boolean plain = recipes.stream().anyMatch(r -> r.getId().toString().equals("gregtech:tracks/track_steel")
                && r.getResultItem(h.getLevel().registryAccess()).getCount() == 4);
        h.assertTrue(plain, "GT6's plain track row yields four tracks");

        // §115: Forge's 1.2 ceiling is still the cart's own default - a cart that never meets a GT6
        // rail is untouched. What changed is that a GT6 rail can now hand its own speed over; the test
        // below drives that.
        AbstractMinecart cart = EntityType.MINECART.create(h.getLevel());
        var ceiling = cart.getMaxCartSpeedOnRail();
        h.assertTrue(Math.abs(ceiling - 1.2F) < 1e-6, "the vanilla cart ceiling Forge documents: " + ceiling);
        h.assertTrue(Math.abs(cart.getCurrentCartSpeedCapOnRail() - ceiling) < 1e-6,
                "and a fresh cart starts at it, got " + cart.getCurrentCartSpeedCapOnRail());
        h.succeed();
    }

    /**
     * §115: a GT6 rail hands its own speed to the cart, so the three rails that used to be clamped at
     * Forge's 1.2 now run at GT6's numbers.
     *
     * <p>The assertion on {@code getCurrentCartSpeedCapOnRail} is also the mixin's guard: Forge's
     * setter clamps to {@code getMaxCartSpeedOnRail()} (1.2), so without
     * {@code AbstractMinecartSpeedCapMixin} this test cannot pass, and a mixin that silently failed to
     * load would break the gate instead of quietly restoring the old ceiling.</p>
     */
    @GameTest(template = "test_blueprint_empty", timeoutTicks = 300)
    public static void gtRailsHandTheirSpeedToTheCart(GameTestHelper h) {
        var level = h.getLevel();
        AbstractMinecart cart = EntityType.MINECART.create(level);
        h.assertTrue(cart != null, "a minecart can be created");

        // A straight run of the fastest GT6 rail.
        for (int z = 0; z < 3; z++) {
            floor(h, POS.offset(0, 0, z));
            h.setBlock(POS.offset(0, 0, z), GTTrackBlocks.get("track_adamantium").block().get());
        }
        BlockPos middlePos = h.absolutePos(POS.offset(0, 0, 1));
        BlockState middle = level.getBlockState(middlePos);
        h.assertTrue(middle.getBlock() instanceof BaseRailBlock,
                "the middle rail is still there: " + middle + " at " + middlePos);

        ((BaseRailBlock) middle.getBlock()).onMinecartPass(middle, level, middlePos, cart);
        h.assertTrue(Math.abs(cart.getCurrentCartSpeedCapOnRail() - 4.0F) < 1e-6,
                "the adamantium rail handed its 4.00 over (Forge's setter would have clamped it to 1.2), got "
                        + cart.getCurrentCartSpeedCapOnRail());
        // §110's lesson: a freshly created entity sits at the world origin, so the cart has to be put on
        // the rail before anything reads the rail under it.
        cart.moveTo(middlePos.getX() + 0.5D, middlePos.getY(), middlePos.getZ() + 0.5D, 0.0F, 0.0F);
        h.assertTrue(Math.abs(cart.getMaxSpeedWithRail() - 4.0F) < 1e-6,
                "and the cart's rail speed is now GT6's 4.00, got " + cart.getMaxSpeedWithRail());

        // A slower GT6 rail still takes it back down - the vanilla setter would flatten both to 1.2.
        h.setBlock(POS.offset(0, 0, 1), GTTrackBlocks.get("track_tungstensteel").block().get());
        BlockState slower = level.getBlockState(middlePos);
        ((BaseRailBlock) slower.getBlock()).onMinecartPass(slower, level, middlePos, cart);
        h.assertTrue(Math.abs(cart.getCurrentCartSpeedCapOnRail() - 1.4F) < 1e-6,
                "the tungstensteel rail lowers the cap to its own 1.40, got "
                        + cart.getCurrentCartSpeedCapOnRail());

        // A curve is still capped at 0.4: the rail hands over what its own rule computed.
        BlockPos curvePos = h.absolutePos(POS.offset(0, 0, 0));
        BlockState curve = level.getBlockState(curvePos);
        ((BaseRailBlock) curve.getBlock()).onMinecartPass(curve, level, curvePos, cart);
        h.assertTrue(Math.abs(cart.getCurrentCartSpeedCapOnRail() - 0.4F) < 1e-6,
                "the end of the run is a dead end and hands 0.4 over, got "
                        + cart.getCurrentCartSpeedCapOnRail());
        h.succeed();
    }

    /** GT6 replaces the four vanilla rail recipes instead of adding a second path to them. */
    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void vanillaRailRecipesAreReplaced(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager().getRecipes();
        var access = h.getLevel().registryAccess();
        // Every shaped crafting recipe that yields a vanilla rail is one of GT6's own rows.
        // GT6's DEL_OTHER_SHAPED_RECIPES keeps shapeless and non-crafting recipes.
        int vanillaRails = 0;
        for (var recipe : recipes) {
            if (!(recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe)
                    || recipe.getType() != net.minecraft.world.item.crafting.RecipeType.CRAFTING) continue;
            var result = recipe.getResultItem(access);
            if (result.isEmpty() || !VANILLA_RAILS.contains(result.getItem())) continue;
            vanillaRails++;
            h.assertTrue(recipe.getId().getNamespace().equals("gregtech"),
                    "no vanilla recipe for " + result.getItem() + " survives: " + recipe.getId());
        }
        h.assertTrue(vanillaRails == 15,
                "GT6's four replacement rows plus twelve activator rows, got " + vanillaRails);

        // minecraft:rail is now four tracks from three iron tracks and three treated sticks.
        var rail = recipes.stream()
                .filter(r -> r.getId().toString().equals("gregtech:tracks/vanilla/rail"))
                .findFirst().orElse(null);
        h.assertTrue(rail != null, "GT6's minecraft:rail row is registered");
        h.assertTrue(rail.getResultItem(access).getCount() == 4,
                "it yields four vanilla rails, got " + rail.getResultItem(access));
        h.assertTrue(rail.getIngredients().stream().anyMatch(ingredient -> ingredient.getItems().length > 0
                        && ingredient.getItems()[0].is(com.gregtech.gregtech.registry.GTItems
                        .getStack(com.gregtech.gregtech.data.MaterialPrefix.railGt,
                                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Iron"), 1)
                        .getItem())),
                "it is built from GT6's iron rail items, not vanilla iron ingots");

        // The activator rail's yield climbs with the material, up to GT6's 64 adamantium tracks.
        var adamantium = recipes.stream()
                .filter(r -> r.getId().toString().equals("gregtech:tracks/vanilla/activator_rail_adamantium"))
                .findFirst().orElse(null);
        h.assertTrue(adamantium != null, "GT6's adamantium activator rail row is registered");
        h.assertTrue(adamantium.getResultItem(access).getCount() == 64,
                "GT6's adamantium activator rail yields 64, got " + adamantium.getResultItem(access));
        h.succeed();
    }

    private static final List<net.minecraft.world.item.Item> VANILLA_RAILS = List.of(
            net.minecraft.world.item.Items.RAIL, net.minecraft.world.item.Items.POWERED_RAIL,
            net.minecraft.world.item.Items.DETECTOR_RAIL, net.minecraft.world.item.Items.ACTIVATOR_RAIL);
}
