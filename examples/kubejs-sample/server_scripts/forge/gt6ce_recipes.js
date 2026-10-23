ServerEvents.recipes(event => {
  event.recipes.gt6ce.machine('kubejs.recipe.press')
    .itemInputs('minecraft:iron_ingot')
    // 0 mB is a presence-only catalyst: the tank must hold water and nothing is drained.
    .fluidInputs(Fluid.of('minecraft:water', 0))
    .itemOutputs(Item.of('minecraft:emerald').withChance(5000))
    .eu(16)
    .duration(64)

  // GT6CE.removeMachine(event, { map: 'gt.recipe.crusher', itemInputs: ['minecraft:iron_ore'] })
  // GT6CE.clearMap(event, 'gt.recipe.crusher')
})
