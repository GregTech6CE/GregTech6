/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon.material;

import java.util.List;
import java.util.Optional;

/** Immutable special-process method declarations from the current material publication. */
public interface SpecialProcessesView {
    SpecialProcessesView NONE = new SpecialProcessesView() {
        @Override
        public List<SpecialProcessMethod> methods() {
            return List.of();
        }

        @Override
        public Optional<SpecialProcessMethod> find(DataKey id) {
            return Optional.empty();
        }
    };

    List<SpecialProcessMethod> methods();

    Optional<SpecialProcessMethod> find(DataKey id);
}
