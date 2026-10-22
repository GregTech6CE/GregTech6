ServerEvents.recipes(event => {
  event.recipes.gt6ce.machine('kubejs.recipe.press')
    .itemInputs('minecraft:iron_ingot')
    .itemOutputs({ item: 'minecraft:emerald', chance: 5000 })
    .eu(16)
    .duration(64)

  // GT6CE.removeMachine(event, { map: 'gt.recipe.crusher', itemInputs: ['minecraft:iron_ore'] })
  // GT6CE.clearMap(event, 'gt.recipe.crusher')
})
