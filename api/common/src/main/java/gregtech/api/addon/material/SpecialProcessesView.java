/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.List;
import java.util.Optional;

/** Immutable special-process method declarations from the current material publication. */
public interface SpecialProcessesView {
    List<SpecialProcessMethod> methods();

    Optional<SpecialProcessMethod> find(DataKey id);
}
