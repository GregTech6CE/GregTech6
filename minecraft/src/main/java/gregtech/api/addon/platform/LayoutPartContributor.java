/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.platform;

import java.util.Set;

/**
 * Creates the behavior for one addon's functional parts. List this class in
 * {@code META-INF/services/gregtech.api.addon.platform.LayoutPartContributor}.
 * {@link #partNames()} must cover every part that declares slots, a tick, a click handler or a field.
 * A casing or an energy-only part is not listed. {@link #create} is called once per placed block entity.
 */
public interface LayoutPartContributor {
    String addonId();

    Set<String> partNames();

    PartBehavior create(String partName);
}
