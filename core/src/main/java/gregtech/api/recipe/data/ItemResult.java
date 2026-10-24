package gregtech.api.recipe.data;

/** One item output. A count or chance of 0 means any, and is only valid on a removal. */
public record ItemResult(String item, int count, int chance) {
    public ItemResult {
        if (item == null || item.isBlank()) throw new IllegalArgumentException("item");
        if (count < 0) throw new IllegalArgumentException("count");
        if (chance < 0 || chance > MachineRecipeFields.CHANCE_CERTAIN) throw new IllegalArgumentException("chance");
    }
}
