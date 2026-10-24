package com.gregtech.gregtech.api.addon;

import java.util.regex.Pattern;

/** SPDX-License-Identifier: LGPL-3.0-or-later. Shared name patterns so each check does not recompile a regex. */
public final class AddonNames {
    /** Loader mod id used as {@code addonId()}. Two to 64 characters. */
    public static final Pattern ADDON_ID = Pattern.compile("[a-z][a-z0-9_]{1,63}");
    /** Lower-case registry path: one to 64 characters. */
    public static final Pattern LOWER_PATH = Pattern.compile("[a-z][a-z0-9_]{0,63}");
    /** Material and composition names. */
    public static final Pattern MATERIAL = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
    /** Recipe-map and structure-key names. */
    public static final Pattern RECIPE_MAP = Pattern.compile("[a-z0-9_.]+");
    /** Machine type: letters, digits and underscore. */
    public static final Pattern MACHINE_TYPE = Pattern.compile("[a-z0-9_]+");

    private AddonNames() {}

    public static boolean isAddonId(String id) {
        return id != null && ADDON_ID.matcher(id).matches();
    }

    public static String requireAddonId(String id) {
        if (!isAddonId(id)) throw new IllegalArgumentException("Invalid addon mod ID: " + id);
        return id;
    }

    public static boolean matches(Pattern pattern, String value) {
        return value != null && pattern.matcher(value).matches();
    }
}
