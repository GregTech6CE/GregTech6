# 许可审查记录

## 2026-10-02 / GitHub 许可导航去重

要求将 GitHub 仓库页面的四个许可标签缩减为一个。根目录仅保留原文不变的 LICENSE（LGPL 3.0）；删除其逐字重复的 LICENSE.txt 与 COPYING.LESSER。原 COPYING 是 GPL 3.0 全文，并非另一份 LGPL，原文不变移至 docs/licenses/GPL-3.0.txt，作为 LGPL 引用条款保留。双语 README 与 NOTICE 链接同步更新。

两个模组及 addon SDK 的 META-INF/gregtech6 中同样仅保留 LICENSE、GPL-3.0.txt 和 NOTICE；不删除来源审计中的第三方许可，不变更 LGPL-3.0-or-later 声明和作者署名。新打包后生效，旧成品仍保留旧布局。

## 当前代码许可 — 2026-10-02

确认“我就是saltnya所以不管这个项目所属”，并明确要求协议沿用另外两个项目。此前“刚刚这些是坩埚工艺作者说的”属于反馈转述，未用作saltnya授权依据；采用许可依据是随后本人身份确认与同一会话的直接许可选择。

项目代码及两平台元数据现统一为LGPL-3.0-or-later。GNU LGPL3/GPL3全文按原GT6保留副本复制，未改写许可正文；根LICENSE/NOTICE和两包META-INF/gregtech6包含声明。旧Forge MDK模板原文保留在audits/notices/saltnya/LICENSE.txt，不能再误认作当前项目代码许可。

原作者署名、历史和第三方资产许可继续保留。此更新解决saltnya代码授权声明歧义，不把来源审查、单独第三方素材权限或玩法验证都宣称完成。以下为历史审查证据；其中“当前All Rights Reserved/未获得saltnya确认”已被本次明确声明取代，其他分项来源与范围继续适用。

---

# 分项许可证与发布前缺口

更新：2026-09-30。此记录依据被提供的源文件、许可/notice及授权截图，保存原文并区分采用内容。不重新授权第三方作品，不把模板文件许可自动扩展到模组代码。GitHub远程归属和作者身份核查尚未完成，对外发布门未通过。

## saltnya 来源：授权范围未决

`gregtech6reborn/gradle.properties:59` 声明 All Rights Reserved，`:67` 作者仍为MDK占位字段；`LICENSE.txt:1-7,23-28` 指明 Forge/FML LGPL2.1及普通Java引用的模组代码不受该模板许可自动约束，`CREDITS.txt` 同为Forge credits。因此当前包没有清楚覆盖该移植代码、GT6派生内容、纹理/语言等资源的授权声明。

已保存 [LICENSE 原文](audits/notices/saltnya/LICENSE.txt)、[CREDITS 原文](audits/notices/saltnya/CREDITS.txt)、源元数据副本。根 `LICENSE.txt` 保持原文，它不能证明已解决该移植整体授权。需要作者/原始仓库中可核对的许可声明、原作及资源出处与逐项归属；在此之前继续本地审查与开发，不将本地整合产物当作已获公开分发许可。

## brokestar233 来源：代码与资产分开

- 代码：根 `LICENSE` 为LGPLv3附加许可文本，NOTICE与保留的源代码头声明LGPL-3.0-or-later。保存 [LICENSE](audits/notices/brokestar233/LICENSE)、[NOTICE](audits/notices/brokestar233/NOTICE.md)，保留GregTech-6 Team与Gregorius Techneticies原始头。
- ModularUI：NOTICE声明fork为LGPL-3.0，且jarJar分发。当前空子模块没有源码、LICENSE、FORK/THIRD_PARTY/DIVERGE与gitlink。恢复实际版本及完整源码/第三方义务材料之前不采用其框架；不能把其许可标为or-later，也不能仅以公开仓库URL推定完整对应源码已包含在本快照。
- 上游GT6资产：NOTICE称默认CC0，logo及衍生为CC-BY-NC-4.0且本来源未采用。真正采用每项仍需核对asset manifest和适用原件；当前没有 `LICENSE.assets` 原件。不得把根代码许可用作全部纹理授权。
- amazawa UI：资产README:9937-9942、10643-10646明确tfc-amazawa-light-gui v1.0.5g、Apache-2.0、作者天沢香，且有许可借用/要求署名的截图。保存 [资产来源manifest原文](audits/notices/brokestar233/assets-README.md) 与 [授权截图](audits/notices/brokestar233/amazawa-gui-authorization.png)。保留作者、链接、逐文件SHA及裁切来源；根README的泛CC0概述不能覆盖这个分项。Apache全文未随快照提供，后续采用时补齐适用原件/NOTICE。
- JEI/Jade/KubeJS为compile-only/运行时可选，source NOTICE称未捆绑；EMI新增compile-only未同步到主NOTICE。最终分发jar需查验嵌入实际组件，再完成notice，不能按依赖声明直接认定打包形态。

该源根LGPL文本引用GNU GPL条款，但没有GPL全文；许可原件与实际分发范围需在发布前补齐。此处记录可见文件缺口，不自行改写第三方文本。

## masson 来源：明确代码声明，仍有分项边界

保存 [LICENSE](audits/notices/masson/LICENSE)、[NOTICE](audits/notices/masson/NOTICE)、[CREDITS](audits/notices/masson/CREDITS.md)。LICENSE作者Lorbineitte Masson，SPDX LGPL-3.0-or-later；文件给声明与全文网址，没有完整许可全文。NOTICE将自写source/resources、GT6来源数据与默认资产分开记录，并保留NeoForged MDK模板MIT全文。

GT6来源固定ref `3703e40308c8c030763fd6297dea8b210d2a77b1`、GTM管几何/命名参考固定ref `de5d2c4a4c863b94a10bfb5d0839df2de8246628`。采用派生数据/几何时保留相关source-policy artifact与代码作者头；参考副本中的NeoForge LGPL-2.1-only、Minecraft代码不是生产源码集，不能无差别复制到整合发行源码。textures中的参考/编辑资产需逐项确认。

来源玩家指南的“小群私测/不要公开发布”描述发布准备状态；不能据此覆盖其明确LICENSE，也不能反过来据LICENSE宣称其玩法达到可公开发布标准。

## 新项目的发布资料门

1. 建立“最终实际分发文件→来源文件/哈希→作者→适用许可→原文保留位置”清单；研究候选与已采用内容分别记录。
2. 解决项目1代码/资源授权歧义；取得可获得的原始历史，明确贡献者与来源关联。提供的作者标签和新导入提交不能代替原历史。
3. 补齐采用分项需要的完整许可、NOTICE、资产授权材料；不统一重标CC0、MIT或LGPL，不覆盖根模板LICENSE来制造已解决的印象。
4. 如果采用缺失ModularUI，先恢复实际fork源码/gitlink及其内嵌依赖清单；采用amazawa则保留其专门署名、链接、截图和manifest。
5. 检查两平台最终jar中的实际资源、组件、参考副本排除与嵌入许可证。构建CI只上传测试制品，不自动发布Release/Maven或推送远程。

许可证门与运行门分别验收。当前未决授权不妨碍继续授权的本地工作，但没有授权结论/双腿运行证据的制品不得标为正式完成。
