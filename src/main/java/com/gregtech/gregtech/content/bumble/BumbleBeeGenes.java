package com.gregtech.gregtech.content.bumble;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * GT6's bumblebee genes — the {@code IItemBumbleBee.Util} companion class
 * ({@code gregapi/item/bumble/IItemBumbleBee.java:96-184}).
 *
 * <p>Every bee stack carries a {@code gt.bumble} compound with its genome:</p>
 *
 * <table>
 *   <caption>GT6's gene keys and ranges</caption>
 *   <tr><td>{@code minhum}/{@code maxhum}</td><td>humidity range, floats, min ≥ 0, max ≥ 0.01</td></tr>
 *   <tr><td>{@code mintemp}/{@code maxtemp}</td><td>temperature range in Kelvin, unbounded longs</td></tr>
 *   <tr><td>{@code offspring}</td><td>0..64</td></tr>
 *   <tr><td>{@code work}</td><td>1..10000</td></tr>
 *   <tr><td>{@code aggro}</td><td>100..10000</td></tr>
 *   <tr><td>{@code life}</td><td>1200..144000 ticks</td></tr>
 *   <tr><td>{@code rain}/{@code storm}/{@code day}/{@code night}/{@code outside}/{@code inside}</td>
 *       <td>booleans</td></tr>
 * </table>
 *
 * <p>Inheritance ({@code :109-128}) is <b>not</b> blending: every gene is taken from one of the two
 * parents, chosen independently per gene, except that a bee may never end up inactive both day and
 * night or both inside and outside — GT6 ORs those pairs so at least one side is always set.</p>
 *
 * <p>Fresh genes come from the environment ({@code :130-153}): the humidity band from the biome's
 * rainfall, the temperature band from the environment temperature ±15 ± 0..30, and the day/night and
 * inside/outside flags from where the bee was born.</p>
 */
public final class BumbleBeeGenes {

    /** GT6's NBT key on the bee stack. */
    public static final String NBT_KEY = "gt.bumble";

    /** GT6's {@code UT.Code.bind} ranges. */
    public static final long MIN_WORK = 1, MAX_WORK = 10000;
    public static final long MIN_AGGRO = 100, MAX_AGGRO = 10000;
    public static final long MIN_LIFE = 1200, MAX_LIFE = 144000;
    public static final long MIN_OFFSPRING = 0, MAX_OFFSPRING = 64;

    private BumbleBeeGenes() {}

    /** 1.20.1's {@code Mth} has no {@code clamp(long, long, long)} overload. */
    private static long clamp(long value, long min, long max) {
        return BumbleGenomeRules.clamp(value,min,max);
    }

    // ── the tag ──────────────────────────────────────────────────────────

    /** The stack's genome, generating a random one when the stack carries none (GT6 {@code :97}). */
    public static CompoundTag of(ItemStack stack, RandomSource random) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag genes = tag.getCompound(NBT_KEY);
        if (genes.isEmpty()) {
            genes = random(random);
            tag.put(NBT_KEY, genes);
        }
        return genes;
    }

    /** The stack's genome, or null when it has none (no generation). */
    @Nullable
    public static CompoundTag peek(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return null;
        CompoundTag genes = stack.getTag().getCompound(NBT_KEY);
        return genes.isEmpty() ? null : genes;
    }

    /** Writes a genome onto the stack and returns it (GT6 {@code :104}). */
    public static ItemStack with(ItemStack stack, CompoundTag genes) {
        stack.getOrCreateTag().put(NBT_KEY, genes);
        return stack;
    }

    /** Whether the stack carries a genome. */
    public static boolean has(ItemStack stack) {
        return peek(stack) != null;
    }

    // ── fresh genomes ────────────────────────────────────────────────────

    /** GT6's {@code getBumbleGenes(Random)}: the plains defaults. */
    public static CompoundTag random(RandomSource random) {
        return fromEnvironment(273L + Math.round(0.8F * 20.0F), 0.4F, true, true, false, random);
    }

    /**
     * GT6's {@code getBumbleGenes(temperature, biome, hasSky, day, night, random)}
     * ({@code IItemBumbleBee:132-153}).
     */
    public static CompoundTag fromEnvironment(long temperature, float rainfall, boolean hasSky,
                                              boolean day, boolean night, RandomSource random) {
        CompoundTag genes=encode(BumbleGenomeRules.fromEnvironment(temperature,rainfall,hasSky,day,night,bits(random)));
        for(String key:new String[]{"rain","storm","inside","outside"})if(!genes.getBoolean(key))genes.remove(key);
        return genes;
    }

    /**
     * GT6's {@code getBumbleGenes(princess, drone, random)} ({@code IItemBumbleBee:109-128}): every
     * gene is taken from one parent at random, with the activity pairs ORed so the offspring can
     * always work.
     */
    public static CompoundTag inherit(ItemStack princess, ItemStack drone, RandomSource random) {
        CompoundTag a=peek(princess),b=peek(drone);
        if(a==null)a=random(random);
        if(b==null)b=random(random);
        return encode(BumbleGenomeRules.inherit(decode(a),decode(b),bits(random)));
    }

    // ── genes (GT6's setters clamp exactly like {@code UT.Code.bind}) ─────

    public static void setHumidityMin(CompoundTag genes, float value) {
        genes.putFloat("minhum", value < 0.01F ? 0F : value);
    }

    public static void setHumidityMax(CompoundTag genes, float value) {
        genes.putFloat("maxhum", value < 0.01F ? 0.01F : value);
    }

    public static void setTemperatureMin(CompoundTag genes, long value) { genes.putLong("mintemp", value); }

    public static void setTemperatureMax(CompoundTag genes, long value) { genes.putLong("maxtemp", value); }

    public static void setOffspring(CompoundTag genes, long value) {
        genes.putLong("offspring", clamp(value, MIN_OFFSPRING, MAX_OFFSPRING));
    }

    public static void setAggressiveness(CompoundTag genes, long value) {
        genes.putLong("aggro", clamp(value, MIN_AGGRO, MAX_AGGRO));
    }

    public static void setWorkForce(CompoundTag genes, long value) {
        genes.putLong("work", clamp(value, MIN_WORK, MAX_WORK));
    }

    public static void setLifeSpan(CompoundTag genes, long value) {
        genes.putLong("life", clamp(value, MIN_LIFE, MAX_LIFE));
    }

    public static void setRainproof(CompoundTag genes, boolean value) { genes.putBoolean("rain", value); }

    public static void setStormproof(CompoundTag genes, boolean value) { genes.putBoolean("storm", value); }

    public static void setDayActive(CompoundTag genes, boolean value) { genes.putBoolean("day", value); }

    public static void setNightActive(CompoundTag genes, boolean value) { genes.putBoolean("night", value); }

    public static void setOutsideActive(CompoundTag genes, boolean value) { genes.putBoolean("outside", value); }

    public static void setInsideActive(CompoundTag genes, boolean value) { genes.putBoolean("inside", value); }

    public static float humidityMin(CompoundTag genes) { return Math.max(0F, genes.getFloat("minhum")); }

    public static float humidityMax(CompoundTag genes) { return Math.max(0.01F, genes.getFloat("maxhum")); }

    public static long temperatureMin(CompoundTag genes) { return genes.getLong("mintemp"); }

    public static long temperatureMax(CompoundTag genes) { return genes.getLong("maxtemp"); }

    public static long offspring(CompoundTag genes) {
        return clamp(genes.getLong("offspring"), MIN_OFFSPRING, MAX_OFFSPRING);
    }

    public static long aggressiveness(CompoundTag genes) {
        return clamp(genes.getLong("aggro"), MIN_AGGRO, MAX_AGGRO);
    }

    public static long workForce(CompoundTag genes) {
        return clamp(genes.getLong("work"), MIN_WORK, MAX_WORK);
    }

    public static long lifeSpan(CompoundTag genes) {
        return clamp(genes.getLong("life"), MIN_LIFE, MAX_LIFE);
    }

    public static boolean rainproof(CompoundTag genes) { return genes.getBoolean("rain"); }

    public static boolean stormproof(CompoundTag genes) { return genes.getBoolean("storm"); }

    public static boolean dayActive(CompoundTag genes) { return genes.getBoolean("day"); }

    public static boolean nightActive(CompoundTag genes) { return genes.getBoolean("night"); }

    public static boolean outsideActive(CompoundTag genes) { return genes.getBoolean("outside"); }

    public static boolean insideActive(CompoundTag genes) { return genes.getBoolean("inside"); }

    // ── environment checks (GT6 {@code MultiItemBumbles.bumbleCanProduce}) ─

    /** Whether the genes accept this humidity, like GT6's {@code minhum <= rainfall <= maxhum}. */
    public static boolean humidityMatches(CompoundTag genes, float rainfall) {
        return rainfall >= humidityMin(genes) && rainfall <= humidityMax(genes);
    }

    /** Whether the genes accept this temperature. */
    public static boolean temperatureMatches(CompoundTag genes, long temperature) {
        return temperature >= temperatureMin(genes) && temperature <= temperatureMax(genes);
    }

    /** Whether the bee may work right now: the time of day, the weather and inside/outside. */
    public static boolean activeNow(CompoundTag genes, boolean day, boolean raining, boolean thundering,
                                    boolean outside) {
        if (day ? !dayActive(genes) : !nightActive(genes)) return false;
        if (outside ? !outsideActive(genes) : !insideActive(genes)) return false;
        if (raining && !rainproof(genes)) return false;
        return !thundering || stormproof(genes);
    }
    private static BumbleGenomeRules.RandomBits bits(RandomSource random){return new BumbleGenomeRules.RandomBits(){public int nextInt(int bound){return random.nextInt(bound);}public boolean nextBoolean(){return random.nextBoolean();}};}
    private static BumbleGenomeRules.Genome decode(CompoundTag g){return new BumbleGenomeRules.Genome(humidityMin(g),humidityMax(g),offspring(g),workForce(g),aggressiveness(g),lifeSpan(g),temperatureMin(g),temperatureMax(g),rainproof(g),stormproof(g),nightActive(g),dayActive(g),insideActive(g),outsideActive(g));}
    private static CompoundTag encode(BumbleGenomeRules.Genome g){
        CompoundTag tag=new CompoundTag();setHumidityMin(tag,g.minhum());setHumidityMax(tag,g.maxhum());setOffspring(tag,g.offspring());setWorkForce(tag,g.work());setAggressiveness(tag,g.aggro());setLifeSpan(tag,g.life());setTemperatureMin(tag,g.mintemp());setTemperatureMax(tag,g.maxtemp());setRainproof(tag,g.rain());setStormproof(tag,g.storm());setNightActive(tag,g.night());setDayActive(tag,g.day());setInsideActive(tag,g.inside());setOutsideActive(tag,g.outside());return tag;
    }
}
