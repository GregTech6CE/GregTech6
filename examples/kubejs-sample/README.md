# KubeJS sample

Copy this directory into an instance `kubejs/` folder, then start GT6CE with KubeJS. From a GT6CE checkout the development flag is `runClient -PkubejsRuntime=true`. KubeJS is not inside the production jar.

The client and the dedicated server must load the same `startup_scripts/gt6ce_machines.js`. If GregTech reads the declarations before those scripts have loaded, startup fails with `KubeJS startup scripts have not loaded`.

`startup_scripts/gt6ce_machines.js` adds a `kubejs:largepress` large recipe machine, a steel casing, an energy-output part, and a three-cell layout. Put `server_scripts/forge/gt6ce_recipes.js` into `kubejs/server_scripts/` on Forge 1.20.1. Use `server_scripts/neoforge/gt6ce_recipes.js` on NeoForge 1.21.1. `Item.of().withChance` exists only on 1.20.1. On 1.21.1 the chance is `{ item, chance }`.

`data/kubejs/gt6ce_layouts/largepress.json` replaces that layout with iron blocks, and its energy input cell uses a two-entry `match` array. The press recipe also carries a 0 mB water input as a presence-only catalyst. Edit it and run `/reload`. The next structure check uses the new cells, and the client preview follows the server copy. Delete the file and reload to restore the startup layout. `optional/gt6ce_layout.js` writes the same kind of file from a script. Copy it into `server_scripts/` only when the JSON file is not also present, or the later resource id wins.

把本目录拷进实例的 `kubejs/`。开发运行用 `runClient -PkubejsRuntime=true`。客户端和专服必须用同一份 startup 脚本。启动失败时信息含 `KubeJS startup scripts have not loaded`。1.20.1 用 `server_scripts/forge/`，1.21.1 用 `server_scripts/neoforge/`。`withChance` 只在 1.20.1 有效。布局 JSON 里的 `match` 可以写成数组，表示一格多选一；压印配方带一条 0 mB 的水输入，只检查存在、不消耗。布局 JSON 改完后执行 `/reload`。
