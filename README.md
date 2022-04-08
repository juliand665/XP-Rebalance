# XP Rebalance

This mod addresses many issue in the vanilla XP (enchanting/repairing/etc.) system that I've grown increasingly annoyed with over the years. Notable changes:

- XP dropped on death is 80% of what you had (in points), rather than the vanilla system of `min(7 x level, 100)`, which can never even get you to level 8.
- Enchanting costs are not subtracted as levels but rather as the amount of XP it would take if you were at the optimal level. So a 2-level enchant with minimum level 17 will take the amount of XP it takes to go from level 15 to 17.
- The enchanting table not only randomly shows one of the enchantments you're guaranteed to get, but also the number of enchantments it would give you.
- Anvil costs have been reworked from the ground up and also work with raw XP rather than levels.

Yet to come:

- Mending will be changed to always go into any damaged items before going into your XP bar.
- Enchant generation (from enchanting table and loot) might be reworked as well.
- Much of this will be configurable.

## Anvil Costs

- Repairing an item repeatedly no longer makes it (exponentially! jeez) more expensive each time.
- There is no such thing as "too expensive" (removed the vanilla 40 level limit).
- Renaming costs a flat amount of XP per item; removing a custom name is always free.
- Combining enchantments into a higher level costs a flat amount when it happens.
- Combining enchantments from different sources is scaled according to the level differences: Each point of durability that can't be covered by one item needs to come from the other, which requires XP to come up for its missing enchants (or levels thereof).
- Repairing with raw materials is cheaper in terms of XP than repairing with a copy.

### Examples

- Repairing an E4 U3 (Efficiency IV, Unbreaking III) pickaxe with another E4 U3 one costs no XP.
- Repairing that same pickaxe with one that only has E3 U3 will cost some XP, proportional to the amount of durability missing from the former to the output.
    - So if the former pickaxe has 80% durability, the XP cost will be half as much as if it had only 60% durability.
- Repairing the pickaxe with one that only has U3 will cost more XP: it needs to come up for 4 levels of efficiency rather than just 1.
- Repairing the pickaxe with an unenchanted pickaxe is the most expensive option.
- Repairing the pickaxe with raw materials will cost much less (currently 1/10), though it will still depend on the enchantments.