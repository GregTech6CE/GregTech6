package com.gregtech.gregtech.api.recipe.data;

/** One item input. Exactly one of {@code item} or {@code tag} is set. A count of 0 means any count. */
public record ItemComponent(String item, String tag, int count) {
    public ItemComponent {
        if ((item == null) == (tag == null)) throw new IllegalArgumentException("item component needs exactly one of item or tag");
        if (count < 0) throw new IllegalArgumentException("count");
    }
}
