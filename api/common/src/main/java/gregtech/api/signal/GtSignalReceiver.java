/* SPDX-License-Identifier: LGPL-3.0-or-later */

package gregtech.api.signal;

/**
 * A block entity that accepts signal-wire power and is not itself a source.
 * A strongly powered solid block with this marker is skipped while a signal wire solves its network.
 * Vanilla receivers stay on the wire's existing {@code instanceof} checks and are not listed in the receiver tag.
 */
public interface GtSignalReceiver {
}
