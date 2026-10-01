package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Fixed GT6 tool part textures under {@code textures/item/iconsets/} (lowercase paths). */
public final class ToolIconSets {
    /** Tint index for untinted iconset overlay layers (pass 1 in GT6). */
    public static final int OVERLAY_TINT = 2;

    public static final ResourceLocation WRENCH = icon("wrench");
    public static final ResourceLocation CROWBAR = icon("crowbar");
    public static final ResourceLocation WIRE_CUTTER = icon("wire_cutter");
    public static final ResourceLocation SCOOP = icon("scoop");
    public static final ResourceLocation GRAFTER = icon("grafter");
    public static final ResourceLocation PLUNGER = icon("plunger");
    public static final ResourceLocation KNIFE = icon("knife");
    public static final ResourceLocation BUTCHERY_KNIFE = icon("butcheryknife");
    public static final ResourceLocation CLUB = icon("club");
    public static final ResourceLocation VOID = icon("void");

    public static final ResourceLocation HANDLE_SWORD = icon("handle_sword");
    public static final ResourceLocation HANDLE_SAW = icon("handle_saw");
    public static final ResourceLocation HANDLE_FILE = icon("handle_file");
    public static final ResourceLocation HANDLE_CHISEL = icon("handle_chisel");
    public static final ResourceLocation HANDLE_SCREWDRIVER = icon("handle_screwdriver");

    public static final ResourceLocation ROLLING_PIN = icon("rolling_pin");
    public static final ResourceLocation FLINT_TINDER = icon("flint_tinder");
    public static final ResourceLocation MONKEYWRENCH = icon("monkeywrench");
    public static final ResourceLocation BENDING_CYLINDER = icon("bending_cylinder");
    public static final ResourceLocation BENDING_CYLINDER_SMALL = icon("bending_cylinder_small");
    public static final ResourceLocation MAGNIFYING_GLASS = icon("magnifying_glass");
    public static final ResourceLocation SCISSORS = icon("scissors");
    public static final ResourceLocation PINCERS = icon("pincers");
    public static final ResourceLocation HAND_DRILL = icon("hand_drill");

    private ToolIconSets() {}

    public static ResourceLocation icon(String name) {
        return GregTech.id("item/iconsets/" + name);
    }

    public static ResourceLocation overlayTexture(ResourceLocation base) {
        return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "_overlay");
    }

    public static String baseName(ResourceLocation icon) {
        return icon.getPath().substring(icon.getPath().lastIndexOf('/') + 1);
    }

    /** All iconset textures that need baked item models (base + overlay variants). */
    public static List<ResourceLocation> allRegisteredIcons() {
        List<ResourceLocation> icons = new ArrayList<>();
        icons.add(WRENCH);
        icons.add(CROWBAR);
        icons.add(WIRE_CUTTER);
        icons.add(SCOOP);
        icons.add(GRAFTER);
        icons.add(PLUNGER);
        icons.add(KNIFE);
        icons.add(BUTCHERY_KNIFE);
        icons.add(CLUB);
        icons.add(HANDLE_SWORD);
        icons.add(HANDLE_SAW);
        icons.add(HANDLE_FILE);
        icons.add(HANDLE_CHISEL);
        icons.add(HANDLE_SCREWDRIVER);
        icons.add(ROLLING_PIN);
        icons.add(FLINT_TINDER);
        icons.add(MONKEYWRENCH);
        icons.add(BENDING_CYLINDER);
        icons.add(BENDING_CYLINDER_SMALL);
        icons.add(MAGNIFYING_GLASS);
        icons.add(SCISSORS);
        icons.add(PINCERS);
        icons.add(HAND_DRILL);
        return icons;
    }
}
