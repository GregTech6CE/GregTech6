/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.addon;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Planned deletion of an addon API entry that is also
 * {@link Deprecated}{@code (since = "0.N", forRemoval = true)}.
 * {@code removeAt} is the earliest {@code 0.MINOR} that may delete it,
 * at least two minor versions after {@code since}. The Javadoc
 * {@code @deprecated} note names the replacement and that minor.
 * Both addon API documents list the same {@code id}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface AddonApiRemoval {
    /** Package and type, plus {@code #member} for a method, field or constructor. */
    String id();

    /** Earliest API artifact minor that may delete this entry, written as {@code 0.MINOR}. */
    String removeAt();
}
