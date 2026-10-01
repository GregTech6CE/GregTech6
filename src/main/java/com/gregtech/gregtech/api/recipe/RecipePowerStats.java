package com.gregtech.gregtech.api.recipe;

import java.math.BigInteger;

/** Original NEI cost/tier/power and time semantics, independent of the energy unit. */
public record RecipePowerStats(BigInteger costs, BigInteger usage, BigInteger tier,
                               long power, boolean generating, long time, String timeUnit) {
    public static RecipePowerStats of(Recipe recipe, long power) {
        if (power <= 0) throw new IllegalArgumentException("Recipe display power must be positive");
        BigInteger usage = BigInteger.valueOf(recipe.mEUt).abs();
        long ticks = recipe.mDuration;
        return new RecipePowerStats(usage.multiply(BigInteger.valueOf(ticks)), usage,
                usage.divide(BigInteger.valueOf(power)), power, recipe.mEUt < 0,
                ticks < 1200 ? ticks : ticks < 36000 ? ticks / 20 : ticks / 1200,
                ticks < 1200 ? "ticks" : ticks < 36000 ? "secs" : "mins");
    }
}
