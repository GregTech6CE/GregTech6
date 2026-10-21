# P3 — Native bookshelves, safes and crafting tables

2026-10-22 lifecycle follow-up: ALIGN-300 records the original unsafe-output preview action, without restoring explosions; 172 now includes the worktable's explicit adjacent-inventory sender. Normal physical-slot drops, native world Inventory/Modes and virtual-slot clearing are present. World saving, item retention and cover-only loot data are separate contracts; see [lifecycle audit](WORKTABLE_LIFECYCLE_AUDIT.md). Final recipes, native events, protection and saved-world restart remain unverified.

2026-10-22 further worktable IO audit: ALIGN-298 records direct filling of the five tool slots; this is separate from adjacent crafting refill 296. ALIGN-299 records the original infinite-tip guard when merging into an occupied result stack; the current table retains tips but has no equivalent menu gate. Five-slot charging, RF size-1 dispatch, screen and reload hooks have source consumers, without full native/client acceptance. See [IO follow-up](WORKTABLE_IO_AUDIT.md).

2026-10-22 worktable follow-up: the historical claims of source consumption order/tool-head equality below describe only the adopted port's local behavior. Original GT6 also accepts selector circuits, prioritizes connected inventories, refills fluid containers from connected tanks and ignores container-tool NBT in equalTools. ALIGN-294–297 record those actual gaps through registered native consumers; local slot arrays, transaction/remainder protection, reload hooks and viewer transfers are present. See [advanced-worktable audit](ADVANCED_WORKTABLE_AUDIT.md); no new gameplay acceptance is claimed.

Use source1 complete75 wood/metal shelves,28 addressed slots, two-sided pixel interaction/pincers/magnifier, redstone secret buttons/levers, enchantment and300tick delayed loot; actual renderer and automation registered. Both versions consume shared variant data, book rules, slot selection and geometry. brokestar has extensible book tags but explicitly defers pixel/redstone/render/proximity; masson uses storage variant/filter/menu synchronization. Preserve richer baseline interactions; tag/filter generalization pending.

Use source1 complete120 safes over existing60 shared metals and10 original key IDs. Preserve15 slots, owner claim, front access, unlocked-only key copy, bind/toggle, no automation and retained27-slot overflow/dungeon loot. Both versions consume shared key order/owner gate/claim-copy-toggle rules. brokestar owner routing is explicitly dropped; retain source1 ownership rather than importing second storage registry. No separate masson safe implementation located in inspected source inventory.

Use source1 complete60 advanced+60 charging tables with71 slots, transaction planning/no remainder loss, blueprint pattern, batch crafting/flush/store/filter modes, source consumption order,5 packet-charging tool slots and RF adapter. Both versions actually consume shared slot layout, exact ingredient priority and automation order. Native recipe holders/CraftingInput/components, crafting events, screen and capability invalidation adapted. Blueprint plain items now included in existing native MultiItems IDs. brokestar has independent ghost patterns/selector while dropping blueprint/tools/connected inventory; masson tool-recipe projection is separate from this workstation and remains a later comparison/adoption candidate.

- Compile success is not actual table crafting, lock security, shelf clicking/rendering, energy charging, independent saved-world reload or old-save compatibility.
- No new fixture was added. One grouped ordinary Neo startup checks registration/data/server startup only, and does not exercise these315 placed blocks.
- Bookshelf source positive redstone countdown dirtiness, delayed loot collisions/discard policy and fixed book whitelist remain source gaps. Extensible brokestar tags and masson filters are candidates, not claimed adopted.
- Source safes retain owner acquisition even through permitted destroy-progress/tool queries. Explosion/removal permission/drop behavior,15-slot legacy overflow and lock client synchronization remain unverified.
- Crafting tables retain source tool-head equality, per-menu planning cache, slot filters and output transaction behavior. Connected inventories/tanks, persistent arbitrary ghost patterns, selector features and full recipe/tool/bucket correctness remain pending.
- Only existing original resources are retained; fewer per-item model files than registered variants remain source resource gaps. Both platform model-loader files are preserved separately.
- Grouped startup occurred before model-loader boundary split and blueprint-model relocation; those resource changes need later client/native resource verification, not a repeat server startup.
- Forge runtime, client rendering, survival chain, releases and migration from brokestar/masson namespaces remain unverified. Full integration goal stays active.

315 real block variants; grouped dual compile20s. Full goal remains active.


2026-10-01 集中Neo专服启动PASS：本轮315书架/保险箱/工作台、10钥匙/2蓝图及前批10草土/19建筑栏杆尖刺/4物品堆全部完成注册与数据加载，200tick普通运行并正常保存三维度退出。总7m55s，观察10.56秒；无新增夹具或额外玩法检测。回执p3-neoforge-storage-workstations-startup.json。模型loader分离/蓝图模型搬迁随后完成，运行交互/客户端/独立重载仍未验证。静态共享资源本地启动可选 -PdirectCoreResources=true 避免整库复制；正常CI/发布仍沿用原流程，该选项留下一次集中启动验证。
