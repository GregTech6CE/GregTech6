package gregtech.gametest;

import gregtech.content.food.GTFoodStats;
import gregtech.content.food.PlayerFoodStats;
import gregtech.content.nuclear.PlayerRadiation;
import gregtech.content.nuclear.ReactorRadiation;
import gregtech.damage.GTHazmat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real ServerPlayer restoration emits the loader Clone event; no synthetic event or handler call. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class FoodCloneImmunityTests {
    private static final String FOOD_KEY = "gt.props.food";

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void nonDeathRestoreCopiesFoodStatsWithoutAliasing(GameTestHelper helper) {
        var oldPlayer = player(helper);
        var replacement = player(helper);
        int[] values = {25, 50, 75, 100, 127};
        for (int stat = 0; stat < values.length; stat++) {
            PlayerFoodStats.change(oldPlayer, values[stat], stat);
            PlayerFoodStats.change(replacement, 3, stat);
        }
        oldPlayer.getPersistentData().getCompound(FOOD_KEY).putString("extension", "retained");
        replacement.getPersistentData().putString("unrelated", "keep");
        PlayerRadiation.change(oldPlayer, 70);
        PlayerRadiation.change(replacement, 3);
        replacement.restoreFrom(oldPlayer, true);
        for (int stat = 0; stat < values.length; stat++)
            helper.assertTrue(PlayerFoodStats.get(replacement, stat) == values[stat], "original statistic copied: " + stat);
        helper.assertTrue(PlayerRadiation.dose(replacement) == 70, "existing radiation clone listener still retains dose");
        helper.assertTrue(replacement.getPersistentData().getString("unrelated").equals("keep"), "unrelated destination data untouched");
        helper.assertTrue(replacement.getPersistentData().getCompound(FOOD_KEY).getString("extension").equals("retained"), "food compound extension retained");
        PlayerFoodStats.change(replacement, 1, GTFoodStats.ALCOHOL);
        helper.assertTrue(PlayerFoodStats.get(oldPlayer, GTFoodStats.ALCOHOL) == 25, "new player mutation cannot change old player");
        PlayerFoodStats.change(oldPlayer, 1, GTFoodStats.CAFFEINE);
        helper.assertTrue(PlayerFoodStats.get(replacement, GTFoodStats.CAFFEINE) == 50, "old player mutation cannot change new player");
        replacement.getPersistentData().getCompound(FOOD_KEY).putString("extension", "new");
        helper.assertTrue(oldPlayer.getPersistentData().getCompound(FOOD_KEY).getString("extension").equals("retained"), "NBT compounds are detached");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void emptyOriginalClearsStaleCloneFoodStats(GameTestHelper helper) {
        var oldPlayer = player(helper);
        var replacement = player(helper);
        for (int stat = 0; stat < GTFoodStats.TRACKED; stat++) PlayerFoodStats.change(replacement, 37, stat);
        replacement.restoreFrom(oldPlayer, true);
        for (int stat = 0; stat < GTFoodStats.TRACKED; stat++)
            helper.assertTrue(PlayerFoodStats.get(replacement, stat) == 0, "absent original resets destination statistic: " + stat);
        helper.assertTrue(!replacement.getPersistentData().contains(FOOD_KEY), "absent original does not create empty food compound");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void deathRestoreStillClearsFoodStatsAndDose(GameTestHelper helper) {
        var oldPlayer = player(helper);
        var replacement = player(helper);
        for (int stat = 0; stat < GTFoodStats.TRACKED; stat++) {
            PlayerFoodStats.change(oldPlayer, 90, stat);
            PlayerFoodStats.change(replacement, 5, stat);
        }
        PlayerRadiation.change(oldPlayer, 120);
        PlayerRadiation.change(replacement, 40);
        replacement.restoreFrom(oldPlayer, false);
        for (int stat = 0; stat < GTFoodStats.TRACKED; stat++)
            helper.assertTrue(PlayerFoodStats.get(replacement, stat) == 0, "death clears statistic: " + stat);
        helper.assertTrue(PlayerRadiation.dose(replacement) == 0, "death clears radiation");
        helper.assertTrue(!replacement.getPersistentData().contains(FOOD_KEY), "death removes food compound");
        helper.assertTrue(PlayerFoodStats.get(oldPlayer, GTFoodStats.ALCOHOL) == 90, "death clearing does not mutate original");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void reactorRadiationRespectsCreativeAndArmorProtection(GameTestHelper helper) {
        helper.assertTrue(ReactorRadiation.PROTECTION.equals(GTHazmat.RADIATION), "shared protection tag remains initialized and identical");
        var creative = player(helper);
        creative.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        helper.assertTrue(!ReactorRadiation.apply(creative, 2, 3), "creative player is immune");
        helper.assertTrue(PlayerRadiation.dose(creative) == 0, "creative dose unchanged");
        var survival = player(helper);
        helper.assertTrue(ReactorRadiation.apply(survival, 2, 3), "unprotected survival receives exposure");
        helper.assertTrue(PlayerRadiation.dose(survival) == 6, "survival dose still equals level times amount");
        var armored = player(helper);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
        for (int index = 0; index < slots.length; index++) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "radiation_suit_" + pieces[index]));
            helper.assertTrue(item != Items.AIR, "real registered radiation armor exists");
            var stack = new ItemStack(item);
            helper.assertTrue(stack.is(ReactorRadiation.PROTECTION), "registered armor is in real data tag");
            armored.setItemSlot(slots[index], stack);
        }
        helper.assertTrue(!ReactorRadiation.apply(armored, 2, 3), "complete tagged suit protects");
        helper.assertTrue(PlayerRadiation.dose(armored) == 0, "complete suit dose unchanged");
        var universal = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("gt6ce", "universal_suit_chestplate")));
        helper.assertTrue(!universal.isEmpty() && universal.is(ReactorRadiation.PROTECTION), "universal armor uses same protection tag");
        armored.setItemSlot(EquipmentSlot.CHEST, universal);
        helper.assertTrue(!ReactorRadiation.apply(armored, 2, 3), "mixed complete tagged suit protects");
        armored.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        helper.assertTrue(ReactorRadiation.apply(armored, 2, 3), "partial suit is not immune");
        helper.assertTrue(PlayerRadiation.dose(armored) == 6, "partial suit still gets full exposure");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void reactorRadiationPreservesSpeciesExclusionsAndFallback(GameTestHelper helper) {
        var golem = EntityType.IRON_GOLEM.create(helper.getLevel());
        var wither = EntityType.WITHER.create(helper.getLevel());
        var zombie = EntityType.ZOMBIE.create(helper.getLevel());
        var spider = EntityType.SPIDER.create(helper.getLevel());
        var pig = EntityType.PIG.create(helper.getLevel());
        helper.assertTrue(golem != null && wither != null && zombie != null && spider != null && pig != null, "native entities constructed");
        helper.assertTrue(!ReactorRadiation.apply(golem, 1, 2), "iron golem uses shared immunity");
        helper.assertTrue(golem.getActiveEffects().isEmpty(), "iron golem receives no fallback effects");
        helper.assertTrue(!ReactorRadiation.apply(wither, 1, 2), "wither remains immune");
        helper.assertTrue(!ReactorRadiation.apply(zombie, 1, 2), "undead gate preserved");
        helper.assertTrue(!ReactorRadiation.apply(spider, 1, 2), "arthropod gate preserved");
        helper.assertTrue(ReactorRadiation.apply(pig, 1, 2), "ordinary unprotected mob still receives fallback");
        helper.assertTrue(pig.getActiveEffects().size() == 6, "six original fallback effects retained");
        helper.assertTrue(pig.getEffect(MobEffects.WITHER).getDuration() == 260, "fallback duration unchanged");
        helper.assertTrue(ReactorRadiation.apply(pig, 1, 2), "repeat exposure still accepted");
        helper.assertTrue(pig.getEffect(MobEffects.WITHER).getDuration() == 520, "fallback durations still accumulate");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "FoodCloneTest");
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, cookie.clientInformation());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player, cookie) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) {}
        };
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        return player;
    }
}
