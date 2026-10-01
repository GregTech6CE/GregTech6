# NOTICE — 第三方组件台账

本仓库（GregTech 6 现代移植）是 GregTech 6 的衍生作品（derivative work），包含以下第三方组件。

## GregTech 6（上游）

- 上游：GregTech 6 for Minecraft 1.7.10，作者 Gregorius Techneticies 及 GregTech-6 Team；
- 代码许可：GNU Lesser General Public License v3.0 or later（LGPL-3.0-or-later）——上游 `LICENSE` 与上游源文件版权头双证，本仓 `LICENSE` 为同证副本；
- 资产许可：CC0 1.0 Universal（跟随上游 `LICENSE.assets` 惯例）；
- GregTech logo 及其衍生：CC BY-NC 4.0——**本仓未引入**任何 logo 或其衍生资产，不携带该许可项。

## ModularUI（fork，随 jar 分发）

- 本仓 `third-party/modularui/` 是 [brachy84/ModularUI-Modern](https://github.com/brachy84/ModularUI-Modern) 的仓内自维护 fork（contains modifications），以 git submodule 形式挂载；
- fork 仓库：<https://github.com/Meow404club/modularui>；
- 许可：LGPL-3.0（全文随 fork 保留于 `third-party/modularui/LICENSE`，未删改）；
- fork 身份、基线与逐文件偏离台账：`third-party/modularui/` 下 `FORK.md`、`THIRD_PARTY.md`、`DIVERGE.md`；
- 分发方式：经 Gradle jarJar 嵌入各发布腿 mod jar（`META-INF/jarjar`）。LGPL §4/§6 的对应源码义务由本公开仓库内的完整可构建源码满足。

## JEI / Jade / KubeJS（未捆绑，无分发）

- Just Enough Items（JEI）、Jade、KubeJS 仅作为 compileOnly / runtime-optional 依赖参与编译与可选运行时集成，**不打入任何分发 jar**；
- 因此不产生再分发义务，此处仅作存在性声明。

---

本仓代码整体以 LGPL-3.0-or-later 提供，全文见 [LICENSE](LICENSE)。
