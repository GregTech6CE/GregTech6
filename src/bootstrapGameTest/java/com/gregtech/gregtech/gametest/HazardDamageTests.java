package com.gregtech.gregtech.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gregtech.gregtech.api.energy.GTVoltageTiers;
import com.gregtech.gregtech.api.energy.WireSpec;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.blockentity.energy.ElectricWireBlockEntity;
import com.gregtech.gregtech.blockentity.energy.ReactorCoreBlockEntity;
import com.gregtech.gregtech.blockentity.machine.FluidPipeBlockEntity;
import com.gregtech.gregtech.content.nuclear.PlayerRadiation;
import com.gregtech.gregtech.damage.GTDamageTypes;
import com.gregtech.gregtech.damage.GTHazmat;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTEnergyNodes;
import com.gregtech.gregtech.registry.GTFluidPipes;
import com.gregtech.gregtech.registry.GTFuelRods;
import com.gregtech.gregtech.registry.GTRadiationProtection;
import com.gregtech.gregtech.registry.GTWires;
import com.gregtech.gregtech.util.GTEntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.RegistryObject;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * GT6's hazard system: the damage types themselves ({@code gregapi/damage/DamageSource*.java} +
 * {@code DamageSources}), the {@code UT.Entities} helpers that decide who is protected, and the four
 * places that actually hurt somebody - hazard suits, bare electric wires, hot/cold fluid pipes and a
 * running reactor core.
 *
 * <p>GT6's damage is dealt in exact numbers, so most tests below assert the exact health a cow (armour
 * 0, 10 HP) loses rather than "it got hurt": {@code tierMax(voltage) * amperage * 4} for electricity,
 * {@code max(1, min(cap, (T - 300) / 50))} for heat, {@code 5} for a reactor core and {@code 3.0} per
 * carried hot ingot.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class HazardDamageTests {
    private static final int BASE_X = 66000;
    private static final int BASE_Z = 66000;
    private static final int BASE_Y = 100;

    private static BlockPos at(int dx, int dz) {
        return new BlockPos(BASE_X + dx, BASE_Y, BASE_Z + dz);
    }

    /** Stone floor plus clean air above, so a spawned animal stands still where the test wants it. */
    private static BlockPos floor(ServerLevel level, int dx, int dz) {
        BlockPos pos = at(dx, dz);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
        return pos;
    }

    private static Cow cow(GameTestHelper helper, BlockPos pos) {
        Cow cow = helper.spawn(EntityType.COW, pos);
        cow.invulnerableTime = 0;
        return cow;
    }

    /**
     * 100 HP and no armour: the exact-damage target. A cow only has 10 HP, so GT6's 12 or 14 point
     * hits clamp its health at 0 and the delta stops being readable.
     */
    private static IronGolem golem(GameTestHelper helper, BlockPos pos) {
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, pos);
        golem.invulnerableTime = 0;
        return golem;
    }

    private static float lost(GameTestHelper helper, LivingEntity entity, float expected) {
        return 10.0F - entity.getHealth() - expected;
    }

    private static float lostGolem(GameTestHelper helper, LivingEntity entity, float expected) {
        return 100.0F - entity.getHealth() - expected;
    }

    // ---------------------------------------------------------------------------------------------
    // The data: 15 damage types, their vanilla tags and their death messages
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void damageTypesCoverGt6sDamageSources(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Set<ResourceLocation> ids = new TreeSet<>();
        for (ResourceKey<DamageType> key : GTDamageTypes.ALL) {
            var holder = registry.getHolder(key);
            helper.assertTrue(holder.isPresent(), "damage type " + key.location() + " is registered");
            holder.ifPresent(h -> {
                ids.add(key.location());
                helper.assertTrue(("gregtech." + key.location().getPath()).equals(h.value().msgId()),
                        key.location() + " uses the gregtech-namespaced death message id, got " + h.value().msgId());
                helper.assertTrue(h.value().scaling() == net.minecraft.world.damagesource.DamageScaling.NEVER,
                        key.location() + " scales never (GT6 deals fixed damage)");
            });
        }
        helper.assertTrue(ids.size() == GTDamageTypes.ALL.size(),
                "all " + GTDamageTypes.ALL.size() + " damage types are distinct, got " + ids.size());

        // GT6's flags, as the vanilla tags they become in 1.20.1.
        for (ResourceKey<DamageType> key : List.of(GTDamageTypes.BUMBLE, GTDamageTypes.RADIATION,
                GTDamageTypes.EXPLODED, GTDamageTypes.ALCOHOL, GTDamageTypes.CAFFEINE,
                GTDamageTypes.DEHYDRATION, GTDamageTypes.SUGAR, GTDamageTypes.FAT)) {
            helper.assertTrue(registry.getHolderOrThrow(key).is(DamageTypeTags.BYPASSES_ARMOR),
                    key.location() + " bypasses armour (GT6 setDamageBypassesArmor)");
        }
        for (ResourceKey<DamageType> key : List.of(GTDamageTypes.ALCOHOL, GTDamageTypes.CAFFEINE,
                GTDamageTypes.DEHYDRATION, GTDamageTypes.SUGAR, GTDamageTypes.FAT, GTDamageTypes.EXPLODED)) {
            helper.assertTrue(registry.getHolderOrThrow(key).is(DamageTypeTags.BYPASSES_RESISTANCE)
                            && registry.getHolderOrThrow(key).is(DamageTypeTags.BYPASSES_ENCHANTMENTS),
                    key.location() + " is absolute (GT6 setDamageIsAbsolute)");
        }
        helper.assertTrue(registry.getHolderOrThrow(GTDamageTypes.EXPLODED)
                        .is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                        && registry.getHolderOrThrow(GTDamageTypes.EXPLODED).is(DamageTypeTags.IS_EXPLOSION),
                "exploded is allowed in creative and counts as an explosion");
        helper.assertTrue(registry.getHolderOrThrow(GTDamageTypes.FROST).is(DamageTypeTags.IS_FREEZING),
                "frost counts as freezing");
        // The reverse direction: the plain hazards must NOT bypass armour, or GT6's balance is gone.
        for (ResourceKey<DamageType> key : List.of(GTDamageTypes.HEAT, GTDamageTypes.FROST, GTDamageTypes.SPIKE,
                GTDamageTypes.CRUSHER, GTDamageTypes.SHREDDER, GTDamageTypes.CHEMICAL, GTDamageTypes.ELECTRIC)) {
            helper.assertTrue(!registry.getHolderOrThrow(key).is(DamageTypeTags.BYPASSES_ARMOR),
                    key.location() + " does not bypass armour");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void deathMessagesExistInBothLanguages(GameTestHelper helper) {
        for (String lang : List.of("en_us", "zh_cn")) {
            JsonObject table = readLang(lang);
            helper.assertTrue(table != null, "assets/gregtech/lang/" + lang + ".json is readable");
            if (table == null) return;
            for (ResourceKey<DamageType> key : GTDamageTypes.ALL) {
                String messageKey = "death.attack.gregtech." + key.location().getPath();
                helper.assertTrue(table.has(messageKey), lang + " has " + messageKey);
                if (table.has(messageKey)) {
                    String text = table.get(messageKey).getAsString();
                    helper.assertTrue(text.contains("%1$s"),
                            messageKey + " names the victim: " + text);
                }
            }
        }
        // The message texts are GT6's own, e.g. DamageSourceSpike's "was impaled by a Spike!".
        JsonObject english = readLang("en_us");
        if (english != null) {
            helper.assertTrue(english.get("death.attack.gregtech.spike").getAsString().contains("Spike"),
                    "spike keeps GT6's wording: " + english.get("death.attack.gregtech.spike").getAsString());
            helper.assertTrue(english.get("death.attack.gregtech.chemical").getAsString()
                            .contains("chemical accident"),
                    "chemical keeps GT6's wording");
        }
        helper.succeed();
    }

    private static JsonObject readLang(String lang) {
        try (var stream = HazardDamageTests.class.getResourceAsStream("/assets/gregtech/lang/" + lang + ".json")) {
            if (stream == null) return null;
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The tier table and the electricity formula
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void voltageTierTableMatchesGt6(GameTestHelper helper) {
        // GT6 CS.V.
        helper.assertTrue(GTVoltageTiers.VOLTAGES[0] == 8L && GTVoltageTiers.VOLTAGES[1] == 32L
                        && GTVoltageTiers.VOLTAGES[3] == 512L && GTVoltageTiers.VOLTAGES[5] == 8192L
                        && GTVoltageTiers.VOLTAGES[15] == 8589934592L,
                "the tier ladder is GT6's CS.V");
        helper.assertTrue(GTVoltageTiers.tierMax(8L) == 0 && GTVoltageTiers.tierMax(9L) == 1
                        && GTVoltageTiers.tierMax(32L) == 1 && GTVoltageTiers.tierMax(512L) == 3
                        && GTVoltageTiers.tierMax(8192L) == 5,
                "tierMax returns the tier index like UT.Code.tierMax");
        helper.assertTrue(GTVoltageTiers.tierMin(32L) == 1 && GTVoltageTiers.tierMin(512L) == 3,
                "tierMin of an exact tier value is that tier (GT6 UT.Code.tierMin)");
        helper.assertTrue(GTVoltageTiers.tierMin(40L) == 1 && GTVoltageTiers.tierMin(520L) == 3,
                "tierMin of a value between tiers is the tier below it");
        helper.assertTrue(GTVoltageTiers.maxVoltageOf(40L) == 128L && GTVoltageTiers.nameOf(32L).equals("LV"),
                "maxVoltageOf/nameOf agree with BasicMachineOriginalParams' tiers");
        helper.assertTrue(GTVoltageTiers.tierMax(-32L) == 1, "tierMax uses the magnitude");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void electricityDamageFollowsGt6Formula(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // tierMax(voltage) * amperage * 4 — 8 V is tier 0, so ULV hurts nobody.
        IronGolem ulv = golem(helper, floor(level, 0, 0));
        helper.assertTrue(!GTEntityHelper.applyElectricityDamage(ulv, 8L, 1L),
                "8 V (tier 0) deals no damage at all");
        helper.assertTrue(ulv.getHealth() == 100.0F, "the ULV golem is untouched, health " + ulv.getHealth());

        IronGolem lv = golem(helper, floor(level, 4, 0));
        helper.assertTrue(GTEntityHelper.applyElectricityDamage(lv, 32L, 1L), "32 V hurts");
        helper.assertTrue(Math.abs(lostGolem(helper, lv, 4.0F)) < 0.001F,
                "32 V * 1 A is tier 1 * 1 * 4 = 4 damage, health " + lv.getHealth());

        IronGolem lv2 = golem(helper, floor(level, 8, 0));
        helper.assertTrue(GTEntityHelper.applyElectricityDamage(lv2, 32L, 2L), "32 V at 2 A hurts");
        helper.assertTrue(Math.abs(lostGolem(helper, lv2, 8.0F)) < 0.001F,
                "32 V * 2 A is 8 damage, health " + lv2.getHealth());

        IronGolem hv = golem(helper, floor(level, 12, 0));
        helper.assertTrue(GTEntityHelper.applyElectricityDamage(hv, 512L, 1L), "512 V hurts");
        helper.assertTrue(Math.abs(lostGolem(helper, hv, 12.0F)) < 0.001F,
                "512 V * 1 A is tier 3 * 4 = 12 damage, health " + hv.getHealth());

        // The wattage overload: tierMax(wattage) * 4, GT6's reactor/wire call path.
        IronGolem watt = golem(helper, floor(level, 16, 0));
        helper.assertTrue(GTEntityHelper.applyElectricityDamage(watt, 2048L), "2048 EU/t hurts");
        helper.assertTrue(Math.abs(lostGolem(helper, watt, 16.0F)) < 0.001F,
                "2048 EU/t is tier 4 * 4 = 16 damage, health " + watt.getHealth());

        // Protections: creative and (once ported) the lightning suit; nothing else is exempt.
        Player creative = helper.makeMockPlayer();
        helper.assertTrue(creative.isCreative(), "makeMockPlayer is creative");
        helper.assertTrue(!GTEntityHelper.applyElectricityDamage(creative, 512L, 1L),
                "a creative player is not shocked");

        Player survivor = helper.makeMockSurvivalPlayer();
        helper.assertTrue(!GTHazmat.isElectroProtected(survivor),
                "no lightning suit is ported yet, so a survival player is not protected");
        helper.assertTrue(GTHazmat.isElectroProtected(creative), "creative counts as protected");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // Temperature, chemicals, and GT6's hot-ingot-in-your-pocket rule
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void temperatureDamageBurnsAndFreezes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // GT6 UT.Entities.applyTemperatureDamage: >320 K is heat, <260 K is frost, in between nothing.
        IronGolem hot = golem(helper, floor(level, 0, 8));
        helper.assertTrue(GTEntityHelper.applyTemperatureDamage(hot, 1000L, 1.0F, 5.0F), "1000 K burns");
        helper.assertTrue(Math.abs(lostGolem(helper, hot, 5.0F)) < 0.001F,
                "1000 K is capped at 5 damage, health " + hot.getHealth());

        IronGolem warm = golem(helper, floor(level, 4, 8));
        helper.assertTrue(!GTEntityHelper.applyTemperatureDamage(warm, 320L, 1.0F, 5.0F),
                "320 K is not above GT6's threshold");
        helper.assertTrue(!GTEntityHelper.applyTemperatureDamage(warm, 300L, 1.0F, 5.0F), "300 K is room temperature");
        helper.assertTrue(warm.getHealth() == 100.0F, "the warm golem is untouched");

        IronGolem cold = golem(helper, floor(level, 8, 8));
        helper.assertTrue(GTEntityHelper.applyTemperatureDamage(cold, 100L, 1.0F, 5.0F), "100 K freezes");
        helper.assertTrue(Math.abs(lostGolem(helper, cold, 5.0F)) < 0.001F,
                "100 K is capped at 5 frost damage, health " + cold.getHealth());

        // The uncapped variant is what the temperature-based helpers use.
        IronGolem boiling = golem(helper, floor(level, 12, 8));
        helper.assertTrue(GTEntityHelper.applyTemperatureDamage(boiling, 1000L, 1.0F), "uncapped 1000 K burns");
        helper.assertTrue(Math.abs(lostGolem(helper, boiling, 14.0F)) < 0.001F,
                "(1000 - 300) / 50 = 14 damage uncapped, health " + boiling.getHealth());

        // GT6's immunities: a blaze cannot be boiled, fire resistance stops it, empty suit tags do not.
        var blaze = helper.spawn(EntityType.BLAZE, floor(level, 16, 8));
        blaze.invulnerableTime = 0;
        helper.assertTrue(!GTEntityHelper.applyHeatDamage(blaze, 5.0F), "a blaze is immune to heat damage");
        IronGolem resistant = golem(helper, floor(level, 20, 8));
        resistant.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        helper.assertTrue(!GTEntityHelper.applyHeatDamage(resistant, 5.0F), "fire resistance stops heat damage");
        helper.assertTrue(GTEntityHelper.applyFrostDamage(resistant, 5.0F), "frost damage ignores fire resistance");
        helper.assertTrue(Math.abs(lostGolem(helper, resistant, 5.0F)) < 0.001F,
                "fire resistance only blocks the burn, health " + resistant.getHealth());
        helper.assertTrue(!GTHazmat.isHeatProtected(resistant), "an armourless golem wears no heat suit");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void chemDamagePoisonsButNotSkeletons(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Cow victim = cow(helper, floor(level, 0, 16));
        helper.assertTrue(GTEntityHelper.applyChemDamage(victim, 2.0F), "chemical damage hurts a cow");
        helper.assertTrue(Math.abs(lost(helper, victim, 2.0F)) < 0.001F,
                "2 chemical damage, health " + victim.getHealth());
        var poison = victim.getEffect(MobEffects.POISON);
        helper.assertTrue(poison != null, "chemical damage also poisons (GT6 adds Potion.poison)");
        if (poison != null) {
            helper.assertTrue(poison.getAmplifier() == 1, "amplifier 1 (Poison II), got " + poison.getAmplifier());
            helper.assertTrue(poison.getDuration() == 200,
                    "duration is max(20, damage * 100) = 200, got " + poison.getDuration());
        }

        // GT6 skips EntitySkeleton by exact class.
        Skeleton skeleton = helper.spawn(EntityType.SKELETON, floor(level, 4, 16));
        helper.assertTrue(!GTEntityHelper.applyChemDamage(skeleton, 2.0F), "a skeleton is immune to chemicals");

        Cow untouched = cow(helper, floor(level, 8, 16));
        helper.assertTrue(!GTEntityHelper.applyChemDamage(untouched, 0.0F), "zero damage is refused");
        helper.assertTrue(untouched.getHealth() == 10.0F, "the untouched cow has full health");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void carryingHotIngotsBurnsTheirHolder(GameTestHelper helper) {
        // GT6 OP.java:578 ingotHot.mHeatDamage = 3.0F, read by UT.Entities.getHeatDamageFromItem.
        helper.assertTrue(MaterialPrefix.ingotHot.heatDamage() == 3.0F,
                "the hot ingot prefix carries GT6's 3.0 heat damage, got " + MaterialPrefix.ingotHot.heatDamage());
        helper.assertTrue(MaterialPrefix.ingot.heatDamage() == 0.0F,
                "an ordinary ingot does not burn its holder");

        ItemStack hot = com.gregtech.gregtech.registry.GTItems.getStack(MaterialPrefix.ingotHot,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Iron"));
        helper.assertTrue(!hot.isEmpty(), "an iron hot ingot item exists");
        helper.assertTrue(GTEntityHelper.heatDamageFromItem(hot) == 3.0F,
                "heatDamageFromItem reads the prefix, got " + GTEntityHelper.heatDamageFromItem(hot));
        helper.assertTrue(GTEntityHelper.heatDamageFromItem(ItemStack.EMPTY) == 0.0F,
                "an empty stack deals no heat damage");
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------
    // Hazard suits
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void hazmatSetsMatchWhatIsPorted(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // Only the lead/radiation suit is ported, so exactly that tag is populated.
        var radiation = net.minecraftforge.registries.ForgeRegistries.ITEMS.tags().getTag(GTHazmat.RADIATION);
        helper.assertTrue(radiation.size() == 4,
                "gregtech:radiation_protection holds the four lead suit pieces, got " + radiation.size());
        for (TagKey<Item> empty : List.of(GTHazmat.CHEM, GTHazmat.HEAT, GTHazmat.FROST, GTHazmat.ELECTRIC,
                GTHazmat.BIO, GTHazmat.INSECT, GTHazmat.GAS)) {
            helper.assertTrue(net.minecraftforge.registries.ForgeRegistries.ITEMS.tags().getTag(empty).size() == 0,
                    empty.location() + " is empty until those suits are ported (GT6 sources them from IC2)");
        }

        Player suited = helper.makeMockSurvivalPlayer();
        helper.assertTrue(!GTHazmat.isRadioProtected(suited), "a naked player is not protected");
        equipFullSuit(suited);
        helper.assertTrue(GTHazmat.isRadioProtected(suited), "the full lead suit protects against radiation");
        helper.assertTrue(GTHazmat.isWearingFull(suited, GTHazmat.RADIATION), "all four slots are the suit");
        suited.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        helper.assertTrue(!GTHazmat.isRadioProtected(suited), "missing one piece removes the protection");

        Player creative = helper.makeMockPlayer();
        for (TagKey<Item> any : List.of(GTHazmat.CHEM, GTHazmat.HEAT, GTHazmat.FROST, GTHazmat.ELECTRIC,
                GTHazmat.BIO, GTHazmat.INSECT, GTHazmat.GAS, GTHazmat.RADIATION)) {
            helper.assertTrue(GTHazmat.isWearingFull(creative, any) || GTHazmat.isCreative(creative),
                    "creative mode is protected against " + any.location());
        }
        helper.assertTrue(GTHazmat.isImmuneToBreathingGases(creative), "creative mode can breathe anything");
        helper.succeed();
    }

    private static void equipFullSuit(LivingEntity entity) {
        entity.setItemSlot(EquipmentSlot.HEAD, suit(ArmorItem.Type.HELMET));
        entity.setItemSlot(EquipmentSlot.CHEST, suit(ArmorItem.Type.CHESTPLATE));
        entity.setItemSlot(EquipmentSlot.LEGS, suit(ArmorItem.Type.LEGGINGS));
        entity.setItemSlot(EquipmentSlot.FEET, suit(ArmorItem.Type.BOOTS));
    }

    private static ItemStack suit(ArmorItem.Type type) {
        RegistryObject<com.gregtech.gregtech.item.RadiationSuitItem> piece = GTRadiationProtection.SUIT.get(type);
        return piece == null ? ItemStack.EMPTY : new ItemStack(piece.get());
    }

    // ---------------------------------------------------------------------------------------------
    // The four call sites
    // ---------------------------------------------------------------------------------------------

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void bareWireShocksWithTheWattageItCarries(GameTestHelper helper) {
        ElectricWireRepairTests.bareWireContactUsesRealPreviousTickPower(helper);
    }

    private static ElectricWireBlock bareWire() {
        for (RegistryObject<ElectricWireBlock> entry : GTWires.allWires()) {
            if (entry.isPresent() && entry.get().spec().voltage() >= 32L) {
                return entry.get();
            }
        }
        throw new IllegalStateException("no bare electric wire is registered");
    }

    private static ElectricWireBlock insulatedCable() {
        for (RegistryObject<ElectricWireBlock> entry : GTWires.allCables()) {
            if (entry.isPresent() && entry.get().spec().voltage() >= 32L) {
                return entry.get();
            }
        }
        throw new IllegalStateException("no insulated cable is registered");
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void fluidPipeBurnsOrFreezesOnContact(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FluidPipeBlock pipe = GTFluidPipes.all().get(0).get();

        BlockPos pos = at(0, 32);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(pos, pipe.defaultBlockState(), 2);
        helper.assertTrue(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity,
                "the fluid pipe has its block entity");
        if (!(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity be)) return;

        // GT6 MultiTileEntityPipeFluid:460 — applyTemperatureDamage(entity, mTemperature, 1, 5.0F).
        setTemperature(be, 1000L);
        helper.assertTrue(be.getTemperature() == 1000L, "the pipe temperature round-trips through NBT");
        Cow hot = cow(helper, at(0, 32));
        pipe.entityInside(level.getBlockState(pos), level, pos, hot);
        helper.assertTrue(Math.abs(lost(helper, hot, 5.0F)) < 0.001F,
                "a pipe of 1000 K fluid burns for the capped 5, health " + hot.getHealth());

        setTemperature(be, 100L);
        Cow cold = cow(helper, at(0, 32));
        pipe.entityInside(level.getBlockState(pos), level, pos, cold);
        helper.assertTrue(Math.abs(lost(helper, cold, 5.0F)) < 0.001F,
                "a pipe of 100 K fluid freezes for the capped 5, health " + cold.getHealth());

        setTemperature(be, 300L);
        Cow warm = cow(helper, at(0, 32));
        pipe.entityInside(level.getBlockState(pos), level, pos, warm);
        helper.assertTrue(warm.getHealth() == 10.0F,
                "a pipe at ambient temperature does nothing, health " + warm.getHealth());

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        helper.succeed();
    }

    private static void setTemperature(FluidPipeBlockEntity be, long kelvin) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("gt.temperature", kelvin);
        be.load(tag);
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void runningReactorCoreBurnsAndIrradiatesOnContact(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Block core = GTEnergyNodes.REACTOR_CORE_BLOCK.get();

        BlockPos pos = at(0, 40);
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(pos, core.defaultBlockState(), 2);
        helper.assertTrue(level.getBlockEntity(pos) instanceof ReactorCoreBlockEntity,
                "the reactor core has its block entity");
        if (!(level.getBlockEntity(pos) instanceof ReactorCoreBlockEntity be)) return;

        // A stopped core is inert, GT6 checks mRunning.
        helper.assertTrue(be.stopped, "a fresh core is stopped");
        Cow ignored = cow(helper, at(0, 40));
        core.entityInside(level.getBlockState(pos), level, pos, ignored);
        helper.assertTrue(ignored.getHealth() == 10.0F,
                "a stopped core does not hurt, health " + ignored.getHealth());

        // Running: GT6 MultiTileEntityReactorCore:303 -> heat 5 + applyRadioactivity(entity, 3, 1).
        helper.assertTrue(be.insertRod(GTFuelRods.stack(9201)), "an empty reactor rod fits");
        be.setStopped(false);
        helper.assertTrue(!be.stopped, "the core now runs");

        Cow burned = cow(helper, at(0, 40));
        core.entityInside(level.getBlockState(pos), level, pos, burned);
        helper.assertTrue(Math.abs(lost(helper, burned, 5.0F)) < 0.001F,
                "touching a running core burns for 5, health " + burned.getHealth());
        helper.assertTrue(burned.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null,
                "and irradiates (ReactorRadiation.apply(entity, 3, 1) adds the dose effects)");

        // A player in the lead suit takes the burn but no dose.
        Player naked = helper.makeMockSurvivalPlayer();
        helper.assertTrue(com.gregtech.gregtech.content.nuclear.ReactorRadiation.apply(naked, 3, 1),
                "an unsuited player absorbs the dose");
        helper.assertTrue(PlayerRadiation.dose(naked) == 3, "dose is 3, got " + PlayerRadiation.dose(naked));
        Player suited = helper.makeMockSurvivalPlayer();
        equipFullSuit(suited);
        helper.assertTrue(!com.gregtech.gregtech.content.nuclear.ReactorRadiation.apply(suited, 3, 1),
                "the lead suit blocks the dose");
        helper.assertTrue(PlayerRadiation.dose(suited) == 0, "suited dose stays 0");

        be.setStopped(true);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        helper.succeed();
    }
}
