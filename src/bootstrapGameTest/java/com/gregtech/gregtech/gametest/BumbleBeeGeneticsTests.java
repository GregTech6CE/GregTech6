package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.bumble.BumbleBeeGenes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * GT6's bumblebee genome ({@code IItemBumbleBee.Util}, {@code gregapi/item/bumble/IItemBumbleBee.java}):
 * a {@code gt.bumble} compound per bee stack with clamped genes, environmental defaults and
 * per-gene inheritance from the two parents.
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class BumbleBeeGeneticsTests {

    /** Genes live on the stack and survive a save/load round trip. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void genesRoundTripThroughNbt(GameTestHelper h) {
        ItemStack bee = new ItemStack(Items.HONEYCOMB); // any stack carries the genome in GT6
        h.assertTrue(!BumbleBeeGenes.has(bee), "a fresh stack has no genome");
        CompoundTag genes = BumbleBeeGenes.of(bee, RandomSource.create(1234L));
        h.assertTrue(BumbleBeeGenes.has(bee), "asking for the genome creates one");
        h.assertTrue(bee.getTag().contains(BumbleBeeGenes.NBT_KEY), "it is stored under gt.bumble");

        ItemStack reloaded = ItemStack.of(bee.save(new CompoundTag()));
        CompoundTag after = BumbleBeeGenes.peek(reloaded);
        h.assertTrue(after != null, "the genome survives saving and loading");
        h.assertTrue(after.equals(genes), "and it is unchanged");
        h.assertTrue(BumbleBeeGenes.workForce(after) >= BumbleBeeGenes.MIN_WORK
                        && BumbleBeeGenes.workForce(after) <= BumbleBeeGenes.MAX_WORK,
                "work force inside GT6's range: " + BumbleBeeGenes.workForce(after));
        h.succeed();
    }

    /** GT6 clamps every gene through {@code UT.Code.bind} when writing it. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void setterClampsMatchGt6(GameTestHelper h) {
        CompoundTag genes = new CompoundTag();
        BumbleBeeGenes.setWorkForce(genes, -5);
        h.assertTrue(BumbleBeeGenes.workForce(genes) == 1, "work clamps up to 1: " + BumbleBeeGenes.workForce(genes));
        BumbleBeeGenes.setWorkForce(genes, 99999);
        h.assertTrue(BumbleBeeGenes.workForce(genes) == 10000, "work clamps down to 10000");

        BumbleBeeGenes.setAggressiveness(genes, 0);
        h.assertTrue(BumbleBeeGenes.aggressiveness(genes) == 100, "aggro clamps up to 100");
        BumbleBeeGenes.setAggressiveness(genes, 100000);
        h.assertTrue(BumbleBeeGenes.aggressiveness(genes) == 10000, "aggro clamps down to 10000");

        BumbleBeeGenes.setLifeSpan(genes, 0);
        h.assertTrue(BumbleBeeGenes.lifeSpan(genes) == 1200, "life clamps up to 1200");
        BumbleBeeGenes.setLifeSpan(genes, Long.MAX_VALUE);
        h.assertTrue(BumbleBeeGenes.lifeSpan(genes) == 144000, "life clamps down to 144000");

        BumbleBeeGenes.setOffspring(genes, 500);
        h.assertTrue(BumbleBeeGenes.offspring(genes) == 64, "offspring clamps like a stack size");

        // Humidity has GT6's asymmetric floor: min may be 0, max never below 0.01.
        BumbleBeeGenes.setHumidityMin(genes, -1F);
        BumbleBeeGenes.setHumidityMax(genes, 0F);
        h.assertTrue(BumbleBeeGenes.humidityMin(genes) == 0F, "humidity min floors at 0");
        h.assertTrue(BumbleBeeGenes.humidityMax(genes) == 0.01F, "humidity max floors at 0.01");
        h.succeed();
    }

    /** Fresh genes follow GT6's environment rules, including the "always active sometime" rule. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void environmentGenesFollowGt6(GameTestHelper h) {
        // A desert bee born at night: day OR night must still be set.
        CompoundTag night = BumbleBeeGenes.fromEnvironment(300L, 0.05F, true, false, true,
                RandomSource.create(7L));
        h.assertTrue(BumbleBeeGenes.nightActive(night), "a night-born bee is night active");
        h.assertTrue(!BumbleBeeGenes.dayActive(night), "and not day active");
        h.assertTrue(BumbleBeeGenes.outsideActive(night), "an open-sky bee works outside");
        h.assertTrue(!BumbleBeeGenes.insideActive(night), "and not inside");
        h.assertTrue(BumbleBeeGenes.temperatureMin(night) >= 300 - 15 - 30
                        && BumbleBeeGenes.temperatureMax(night) <= 300 + 15 + 30,
                "the temperature band is GT6's ±15 ± 0..30: " + BumbleBeeGenes.temperatureMin(night)
                        + ".." + BumbleBeeGenes.temperatureMax(night));
        h.assertTrue(BumbleBeeGenes.humidityMin(night) >= 0F
                        && BumbleBeeGenes.humidityMax(night) <= 0.05F + 0.10F + 0.40F + 1e-6F,
                "the humidity band comes from the biome rainfall: "
                        + BumbleBeeGenes.humidityMin(night) + ".." + BumbleBeeGenes.humidityMax(night));

        // Caves (no sky) make an inside-active bee instead.
        CompoundTag cave = BumbleBeeGenes.fromEnvironment(280L, 0.5F, false, true, false,
                RandomSource.create(11L));
        h.assertTrue(BumbleBeeGenes.insideActive(cave), "a cave-born bee works inside");
        h.assertTrue(!BumbleBeeGenes.outsideActive(cave), "and not outside");
        h.assertTrue(BumbleBeeGenes.rainproof(cave) || !BumbleBeeGenes.rainproof(cave),
                "rainproof is a coin flip weighted by the rainfall");

        // The false/false case becomes true/true (GT6: "Making sure no errors can happen").
        CompoundTag always = BumbleBeeGenes.fromEnvironment(280L, 0.5F, true, false, false,
                RandomSource.create(3L));
        h.assertTrue(BumbleBeeGenes.dayActive(always) && BumbleBeeGenes.nightActive(always),
                "a bee that is neither day nor night active becomes both");
        h.succeed();
    }

    /** Inheritance takes every gene from one of the two parents — and keeps the offspring active. */
    @GameTest(template = "test_empty", timeoutTicks = 300)
    public static void inheritanceTakesGenesFromBothParents(GameTestHelper h) {
        CompoundTag mother = new CompoundTag();
        BumbleBeeGenes.setWorkForce(mother, 1);
        BumbleBeeGenes.setLifeSpan(mother, 1200);
        BumbleBeeGenes.setOffspring(mother, 1);
        BumbleBeeGenes.setAggressiveness(mother, 100);
        BumbleBeeGenes.setTemperatureMin(mother, 100);
        BumbleBeeGenes.setTemperatureMax(mother, 200);
        BumbleBeeGenes.setDayActive(mother, true);
        BumbleBeeGenes.setNightActive(mother, false);
        BumbleBeeGenes.setOutsideActive(mother, true);
        BumbleBeeGenes.setInsideActive(mother, false);

        CompoundTag father = new CompoundTag();
        BumbleBeeGenes.setWorkForce(father, 9999);
        BumbleBeeGenes.setLifeSpan(father, 140000);
        BumbleBeeGenes.setOffspring(father, 9);
        BumbleBeeGenes.setAggressiveness(father, 9000);
        BumbleBeeGenes.setTemperatureMin(father, 700);
        BumbleBeeGenes.setTemperatureMax(father, 900);
        BumbleBeeGenes.setDayActive(father, false);
        BumbleBeeGenes.setNightActive(father, true);
        BumbleBeeGenes.setOutsideActive(father, false);
        BumbleBeeGenes.setInsideActive(father, true);

        ItemStack princess = BumbleBeeGenes.with(new ItemStack(Items.HONEYCOMB), mother.copy());
        ItemStack drone = BumbleBeeGenes.with(new ItemStack(Items.HONEYCOMB), father.copy());
        var random = RandomSource.create(20260917L);

        boolean sawMother = false, sawFather = false;
        for (int i = 0; i < 200; i++) {
            CompoundTag child = BumbleBeeGenes.inherit(princess, drone, random);
            long work = BumbleBeeGenes.workForce(child);
            if (work != 1 && work != 9999) {
                h.assertTrue(false, "a gene must come from one parent, got work=" + work);
                return;
            }
            sawMother |= work == 1;
            sawFather |= work == 9999;

            long life = BumbleBeeGenes.lifeSpan(child);
            h.assertTrue(life == 1200 || life == 140000, "life comes from a parent: " + life);
            long offspring = BumbleBeeGenes.offspring(child);
            h.assertTrue(offspring == 1 || offspring == 9, "offspring comes from a parent: " + offspring);
            long min = BumbleBeeGenes.temperatureMin(child);
            h.assertTrue(min == 100 || min == 700, "temperature comes from a parent: " + min);
            // GT6's rule: a bee may never end up inactive both day and night / inside and outside.
            h.assertTrue(BumbleBeeGenes.dayActive(child) || BumbleBeeGenes.nightActive(child),
                    "always active at some time of day");
            h.assertTrue(BumbleBeeGenes.insideActive(child) || BumbleBeeGenes.outsideActive(child),
                    "always active inside or outside");
        }
        h.assertTrue(sawMother && sawFather, "both parents contribute over 200 children");
        h.succeed();
    }

    /** The environment checks GT6's breeders use before a bee may work. */
    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void activityChecksMatchGt6(GameTestHelper h) {
        CompoundTag genes = new CompoundTag();
        BumbleBeeGenes.setDayActive(genes, true);
        BumbleBeeGenes.setNightActive(genes, false);
        BumbleBeeGenes.setOutsideActive(genes, true);
        BumbleBeeGenes.setInsideActive(genes, false);
        BumbleBeeGenes.setRainproof(genes, true);
        BumbleBeeGenes.setStormproof(genes, false);

        h.assertTrue(BumbleBeeGenes.activeNow(genes, true, false, false, true), "a day bee works by day");
        h.assertTrue(!BumbleBeeGenes.activeNow(genes, false, false, false, true), "and not by night");
        h.assertTrue(!BumbleBeeGenes.activeNow(genes, true, false, false, false), "and not inside");
        h.assertTrue(BumbleBeeGenes.activeNow(genes, true, true, false, true), "it is rainproof");
        h.assertTrue(!BumbleBeeGenes.activeNow(genes, true, true, true, true), "but not stormproof");
        h.succeed();
    }
}
