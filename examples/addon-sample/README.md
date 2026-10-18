# GregTech 6 Community Edition addon sample

API version **0**, mod version **0.0.0**. API 0 is a development interface; the version reset retains the existing extension capabilities. These Forge 1.20.1 and NeoForge 1.21.1 consumers use only the public loader SDK, Minecraft and loader dependencies. Their material, recipe-machine, behavior-machine and fluid-generator contributors are ordinary `ServiceLoader` entries. They do not compile against GregTech implementation classes.

The samples were adopted from `SaltNya/GregTech6` commit `c899462a6` under its LGPL-3.0-or-later project license. Preserve the repository's author and third-party asset notices. Source packages retain their original names; dependencies and resource references target the `gt6ce` identity.

From the repository root, run each command separately:

```text
./gradlew :multiversion:1.20.1-forge:compileAddonSample :multiversion:1.20.1-forge:addonApiClosureCheck
./gradlew :multiversion:1.21.1-neoforge:compileAddonSample :multiversion:1.21.1-neoforge:addonApiClosureCheck
```

The SDK is generated in the corresponding node's `build/addon-sdk/`. Compiled fixture classes go to `build/classes/addon-sample/`. The checks reject references to implementation classes, even if a future classpath change accidentally exposes them.

To include the sample mod in a development run, use `-PaddonSample=true` with that node's `runClient` or `runServer`. This optional source set has its own metadata/resources; samples are never included in the production mod JAR or SDK. Require `gt6ce` version `[0.0.0,)` in an external addon's loader descriptor. See the [public API guide](../../docs/ADDON_API.md).
