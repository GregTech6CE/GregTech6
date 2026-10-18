"""Viewer families from actual food statistics, never MultiItem category names.

MultiItemFood contains bee combs, animal parts and clay as well as edible food;
MultiItemBottles contains industrial reagents. These boundaries only organize EMI.
"""
import collections
import re

UNFIRED_CLAY = r"clay_(?:(?:[a-z0-9_]+_)?mold|tap|funnel|jug|measuring_pot|juicer|bowl|basin|crossing|crucible|faucet)"
CHEMICAL_BOTTLES = {
    "ink_bottle", "bottled_indigo_dye", "bottle_of_poison", "tar_bottle",
    "bottle_oblood", "lubricant_bottle", "mercury_bottle", "glue_bottle",
}
CONDIMENTS = {
    "chili_sauce", "hot_sauce", "diabolo_sauce", "diablo_sauce", "there_is_no_cow_sauce",
    "barbecue_sauce", "olive_oil", "sunflower_oil", "nut_oil", "seed_oil", "hemp_oil",
    "lin_oil", "fish_oil", "whale_oil", "mayo", "dressing", "honey", "honeydew",
    "royal_jelly", "chocolate_cream", "nutella", "heavy_cream", "coconut_cream",
}
VEGETABLES = {"tomato", "maxim_tomato", "onion", "cucumber", "pickle", "chili_pepper", "carrot_slice"}
FRUITS = {"lemon", "banana", "pomegranate", "pomeraisins", "blueberry", "gooseberry", "candleberry",
          "cranberry", "black_currants", "white_currants", "red_currants", "blackberry", "raspberry",
          "strawberry", "apple", "ananas", "coconut", "peanut", "hazelnut"}
MEDICINES = {"medicine", "laxative", "empty_wax_pill", "radaway", "peppermint", "blue_pill", "red_pill", "antidote", "cure_all"}


def catalog(java):
    multi = (java / "registry/GTMultiItemsGen.java").read_text(encoding="utf-8")
    rows = re.findall(r'"([a-z0-9_]+)", "[^"]*", "(food|bottles)", "[^"]*"', multi)
    statistics = (java / "data/generated/GTFoodItemsGen.java").read_text(encoding="utf-8")
    stats = dict(re.findall(r'new FoodNutritionRows.Entry\("([^"]+)".*?, (true|false), \d+\)', statistics, re.S))
    assert len(stats) == sum(category == "food" for _, category in rows), "Review new food registrations/statistics"
    groups = collections.defaultdict(list)
    for item, category in rows:
        if item == "clouded_bottle":
            continue  # The existing loot group owns this bottle.
        if item in MEDICINES:
            group = "medicines"
        elif category == "bottles":
            group = "chemical_bottles" if item in CHEMICAL_BOTTLES else "food_condiments" if item in CONDIMENTS else "drinks"
        elif item.endswith("_comb"):
            group = "bumble_combs"
        elif re.fullmatch(UNFIRED_CLAY, item) or item in {"blue_clay", "brown_clay", "red_clay", "white_clay", "yellow_clay"}:
            continue  # Existing clay groups retain their contents and priority.
        elif re.search(r"(?:tusk|hoof|horn|antler)$", item):
            group = "animal_parts"
        elif stats[item] != "true" and item != "egg":
            group = "plant_materials"
        elif item.endswith("ice_cream"):
            group = "ice_cream"
        elif "burger" in item or "sandwich" in item:
            group = "food_sandwiches"
        elif "pizza" in item or item == "flat_dough_with_ketchup":
            group = "food_pizzas"
        elif "dough" in item or item == "raw_cake_bottom":
            group = "food_dough"
        elif re.search(r"(?:ham|bacon|ribs|steak|meat|mutton|dogmeat|fish_bar|chum)", item):
            group = "food_meat"
        elif any(word in item for word in ("egg", "cheese", "butter")):
            group = "food_eggs_dairy"
        elif re.sub(r"_slice$", "", item) in VEGETABLES:
            group = "food_vegetables"
        elif re.sub(r"(?:_slice)?(?:_[2-4])?$", "", item) in FRUITS or re.search(r"(?:grapes|raisins)$", item):
            group = "food_fruits"
        elif re.search(r"(?:bread|bun|baguette|toast|cookie|cake_bottom)", item):
            group = "food_bakery"
        elif any(word in item for word in ("potato", "fries", "chips")):
            group = "food_potatoes"
        else:
            group = "foods"
        groups[group].append(item)
    assert not CHEMICAL_BOTTLES.intersection(groups["drinks"])
    assert len(groups["bumble_combs"]) == 20
    return {group: sorted(set(items)) for group, items in groups.items()}


def resources(java):
    for group, members in sorted(catalog(java).items()):
        assert len(members) > 1, group
        yield "functional_" + group, {"type": "remi:group", "name": "emi.group.gt6ce.functional_" + group,
                "contents": ["item:gt6ce:" + item for item in members], "priority": 120}
