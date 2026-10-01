package com.gregtech.gregtech.damage;

import com.gregtech.gregtech.content.nuclear.ReactorRadiation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * GT6's hazard suits ({@code ArmorsGT.HAZMATS_*}) and the {@code UT.Entities.isWearingFull*Hazmat}
 * checks that guard every hazard in {@link com.gregtech.gregtech.util.GTEntityHelper}.
 *
 * <p>GT6 keeps one armour <em>set</em> per hazard (chem, heat, frost, lightning, radioactive, bio,
 * insects, gas) and answers "protected?" by testing all four armour slots against it, plus a
 * hard-coded immunity list per hazard (a blaze cannot be boiled, an iron golem cannot be poisoned).
 * In 1.20.1 the sets become item tags, which is also how the port already ships its lead hazard suit
 * ({@code gregtech:radiation_protection}, see {@link ReactorRadiation#PROTECTION}).
 *
 * <p><b>State of the sets:</b> the port has ported GT6's lead/radiation suit only, so
 * {@link #RADIATION} is populated and the other seven tags are deliberately empty - GT6 sources most
 * of them from IC2's hazmat suit, which does not exist here. The checks below are wired anyway so
 * that a later armour batch only has to add items to a tag; {@code HazardDamageTests} pins the
 * current emptiness so it cannot be mistaken for protection.
 */
public final class GTHazmat {
    /** Chemical protection (GT6 {@code ArmorsGT.HAZMATS_CHEM}, IC2's hazmat suit). */
    public static final TagKey<Item> CHEM = tag("hazmat_chem");
    /** Heat protection (GT6 {@code ArmorsGT.HAZMATS_HEAT}). */
    public static final TagKey<Item> HEAT = tag("hazmat_heat");
    /** Frost protection (GT6 {@code ArmorsGT.HAZMATS_FROST}). */
    public static final TagKey<Item> FROST = tag("hazmat_frost");
    /** Electric protection (GT6 {@code ArmorsGT.HAZMATS_LIGHTNING}, IC2's hazmat suit). */
    public static final TagKey<Item> ELECTRIC = tag("hazmat_electric");
    /** Bio protection (GT6 {@code ArmorsGT.HAZMATS_BIO}). */
    public static final TagKey<Item> BIO = tag("hazmat_bio");
    /** Insect protection (GT6 {@code ArmorsGT.HAZMATS_INSECTS}) - the bee-keeping suit. */
    public static final TagKey<Item> INSECT = tag("hazmat_insect");
    /** Gas protection (GT6 {@code ArmorsGT.HAZMATS_GAS}) - also GT6's "can breathe gases" test. */
    public static final TagKey<Item> GAS = tag("hazmat_gas");
    /** Radioactive protection (GT6 {@code ArmorsGT.HAZMATS_RADIOACTIVE}); the port's lead suit. */
    public static final TagKey<Item> RADIATION = ReactorRadiation.PROTECTION;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private GTHazmat() {}

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("gregtech", path));
    }

    /** GT6 {@code UT.Entities.isCreative}/{@code isInvincible}: creative mode ignores every hazard. */
    public static boolean isCreative(LivingEntity entity) {
        return entity instanceof Player player && player.isCreative();
    }

    /** GT6 {@code UT.Entities.isInvincible} (creative, the only invincibility GT6 models). */
    public static boolean isInvincible(LivingEntity entity) {
        return isCreative(entity);
    }

    /** The four armour slots all hold an item from {@code set}; an empty slot is not protection. */
    public static boolean isWearingFull(LivingEntity entity, TagKey<Item> set) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.is(set)) {
                return false;
            }
        }
        return true;
    }

    /** GT6 {@code isWearingFullFrostHazmat}: creative or a full frost set. */
    public static boolean isFrostProtected(LivingEntity entity) {
        return isCreative(entity) || isWearingFull(entity, FROST);
    }

    /**
     * GT6 {@code isWearingFullHeatHazmat}: creative, a full heat set, or an entity that is made of
     * fire to begin with (wither, blaze, zombie pigman, magma cube, ghast).
     */
    public static boolean isHeatProtected(LivingEntity entity) {
        return isCreative(entity) || entity instanceof WitherBoss || entity instanceof Blaze
                || entity instanceof ZombifiedPiglin || entity instanceof MagmaCube || entity instanceof Ghast
                || isWearingFull(entity, HEAT);
    }

    /** GT6 {@code isWearingFullBioHazmat}: creative, a full bio set, or a wither/iron golem. */
    public static boolean isBioProtected(LivingEntity entity) {
        return isCreative(entity) || entity instanceof WitherBoss || entity instanceof IronGolem
                || isWearingFull(entity, BIO);
    }

    /** GT6 {@code isWearingFullChemHazmat}: creative or a full chemical set. */
    public static boolean isChemProtected(LivingEntity entity) {
        return isCreative(entity) || isWearingFull(entity, CHEM);
    }

    /** GT6 {@code isWearingFullInsectHazmat}: creative, a full insect set, or a wither/iron golem. */
    public static boolean isInsectProtected(LivingEntity entity) {
        return isCreative(entity) || entity instanceof WitherBoss || entity instanceof IronGolem
                || isWearingFull(entity, INSECT);
    }

    /** GT6 {@code isWearingFullRadioHazmat}: creative, a full radioactive set, or a wither/iron golem. */
    public static boolean isRadioProtected(LivingEntity entity) {
        return isCreative(entity) || entity instanceof WitherBoss || entity instanceof IronGolem
                || isWearingFull(entity, RADIATION);
    }

    /** GT6 {@code isWearingFullElectroHazmat}: creative or a full lightning set. */
    public static boolean isElectroProtected(LivingEntity entity) {
        return isCreative(entity) || isWearingFull(entity, ELECTRIC);
    }

    /** GT6 {@code isWearingFullGasHazmat}: creative, a full gas set, or a wither/iron golem. */
    public static boolean isGasProtected(LivingEntity entity) {
        return isCreative(entity) || entity instanceof WitherBoss || entity instanceof IronGolem
                || isWearingFull(entity, GAS);
    }

    /** GT6 {@code UT.Entities.isImmuneToBreathingGases} - the gas set is the breathing set. */
    public static boolean isImmuneToBreathingGases(LivingEntity entity) {
        return isGasProtected(entity);
    }
}
