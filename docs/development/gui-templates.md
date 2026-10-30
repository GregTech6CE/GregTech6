# 用碎片贴图制作机器 GUI

机器背景由九宫格面板、槽位、装饰图和进度条组合，不再要求为每台机器画一张 256×256 PNG。素材全部来自 GT6CE；没有使用 GTCEu Modern 的贴图。

先从自动槽位模板开始。特殊机器可以改用显式布局，仍复用相同素材。

## 文件位置

| 文件 | 用途 |
| --- | --- |
| `core/src/main/resources/assets/gt6ce/textures/gui/components/` | 按 `base`、`slot`、`overlay`、`progress_bar`、`icon`、`special`、`widget` 分类的独立 PNG |
| `assets/<命名空间>/gui/sprites.json` | 图标名、PNG 路径与逻辑像素尺寸 |
| `assets/<命名空间>/gui/templates/machines/<名字>.json` | 机器和配方查看器共用的面板布局，可引用其他模板 |
| `assets/<命名空间>/gui/template_aliases.json` | 可选，旧路径到公共模板的映射 |
| [自动槽位模板](../../core/src/main/resources/assets/gt6ce/gui/templates/base/automatic.json) | 按 Menu 的真实槽位绘制框，不重复声明槽位坐标 |
| [固定背包模板](../../core/src/main/resources/assets/gt6ce/gui/templates/base/machine.json) | 176×166 面板与固定的 36 格玩家背包，供特殊静态布局继承 |
| [完整附属资源示例](../../tools/gui/examples/assets/example/gui/templates/machines/washer.json) | 复用 GT6CE 素材的新机器，无需新增 PNG |
| [预览工具](../../tools/gui/README.md) | 检查资源并生成本地 PNG/HTML，不必启动游戏 |

附属模组把相同结构放入自己的 `src/main/resources/assets/<modid>/`。资源包也能覆盖这些文件。JSON 路径区分大小写，建议全小写。

## 最短接入步骤

1. 使用已有附属 API 声明配方表与机器，槽位数量仍由服务器声明决定。配方表的 `.gui(...)` 指向自己的名字。
2. 创建对应的 `gui/templates/machines/<名字>.json`，继承 `gt6ce:base/automatic`。
3. 选一个进度条，按需加入装饰图。无需提供旧的大 PNG，也无需增加 Screen 类。

配方表声明示例（机器绑定与注册沿用[附属 API](../ADDON_API.zh-CN.md)流程）：

```java
RecipeTable.machine("example.washer")
        .title("Washer")
        .gui("example:textures/gui/machines/washer.png")
        .inputItems(1).outputItems(1).inputFluids(1)
        .minimalInputItems(1).minimalInputs(1).needsOutputs(true)
        .build();
```

机器界面和 JEI/EMI 共用 `example:machines/washer`，实际布局文件是 `assets/example/gui/templates/machines/washer.json`，只维护这一份。API 的 `.png` 字符串沿用原签名，不要求存在对应图片。内部旧配方调用的 `recipes/washer` 会自动解析到 `machines/washer`。`assembler`、`chisel` 两个旧名字仍映射到 `default`，新机器使用独立名字。

内置名称差异（例如 `recipes/cokeoven` → `machines/coke_oven`）集中在 `assets/gt6ce/gui/template_aliases.json`，不再为每个旧路径存一份转发 JSON。附属也可提供自己的别名表，键为本命名空间的旧路径、值为带命名空间的新 ID。优先读取请求路径下显式存在的模板，旧资源包/附属的 `recipes/*.json` 仍可覆盖；只有不存在时才按别名或同名 `machines/` 查找。别名循环与超过 16 层会报错。

```json
{
  "recipes/oldwasher": "example:machines/washer"
}
```

新模板 `assets/example/gui/templates/machines/washer.json` 的内容：

```json
{
  "parent": "gt6ce:base/automatic",
  "progress": {
    "sprite": "gt6ce:progress_bar/progress_bar_bath",
    "x": 78,
    "y": 24
  }
}
```

自动模板会画出物品槽、流体槽和玩家背包。标准机器的所有槽位坐标来自 Menu；这些客户端资源不能更改库存、槽位数量、插入规则或网络数据。

## 图标与尺寸

`sprites.json` 是图标表，键是不带命名空间的图标名。比如附属新增一张自己的图：

```json
{
  "overlay/washing": {
    "texture": "example:textures/gui/overlay/washing.png",
    "width": 16,
    "height": 16
  }
}
```

布局引用 `example:overlay/washing`。只引用 GT6CE 图标时不需要自己的 `sprites.json`。逻辑尺寸使用 GUI 像素；高清资源包可提供等比例放大的 PNG，逻辑尺寸保持不变。

常用图标：

| 名字 | 尺寸与使用方法 |
| --- | --- |
| `gt6ce:base/background` | 16×16，四边各 4 像素；固定四角，拉伸中间与边条 |
| `gt6ce:base/label` | 11×14，左 4、中 4、右 3；标题组件自动重复中段 |
| `gt6ce:slot/item`、`gt6ce:slot/fluid` | 18×18，放在 Menu 槽坐标减 1 的位置 |
| `gt6ce:overlay/*` | 通常 16×16，放在槽内坐标，即槽框坐标加 1 |
| `gt6ce:progress_bar/*` | 左半未工作、右半工作中，按实际图宽均分 |

进度条不固定为 40×18。切割机、滚轮等完整图案更宽，以图标表为准。渲染采用浮点裁切；标准机器还保留单 tick 进度插值。`icon/lathe` 是独立静态装饰，仍可在布局中使用。

## 布局字段

| 字段 | 默认值 / 语义 |
| --- | --- |
| `format` | `1`；布局格式版本 |
| `parent` | 可选，另一个模板 ID；继承尺寸、背景、进度条、开关与元素 |
| `width`、`height` | 无父模板时为 176、166；必须与自定义 Screen/Menu 匹配 |
| `background` | `gt6ce:base/background`，使用四像素边框的九宫格图 |
| `menu_slots` | `false`；为 `true` 时按传入的 Menu 画槽框 |
| `elements` | 按顺序叠加，追加到父模板元素之后 |
| `progress` | `{ "sprite": "...", "x": 78, "y": 24 }`；`null` 可取消父模板进度条 |
| `brand` | `false`；允许标准机器标题组件在固定空白位置显示 G 图标，实际槽位相交时会跳过 |

普通元素：

```json
{
  "sprite": "gt6ce:slot/item",
  "x": 16,
  "y": 6,
  "columns": 3,
  "rows": 2,
  "step_x": 18,
  "step_y": 18
}
```

`columns`、`rows` 默认 1；步距默认图标宽高。像素原点是面板左上角。绘制顺序为背景 → 自动槽框 → 父/子静态元素 → 进度条。不要同时用自动槽框与固定背包模板重复绘制同一批槽位。需要替换父元素时，选更基础的父模板；继承不会按名字删除元素。

父链最多 16 层；禁止循环引用。尺寸、重复数量、坐标都有边界检查，错误会记录模板路径。缺少模板或模板解析失败时，现有机器回退到旧 PNG 路径。旧整图仅在 `textures/gui/machines/` 保留一份作为兼容回退；`recipes` 的重复整图已经移除，旧路径会查找对应的机器整图。新机器不需要制作整图。

内置已迁移界面优先使用模板。以前只替换整机 PNG 的资源包需要把修改迁移到 `textures/gui/components/`，或覆盖布局 JSON；单独替换旧整图不会覆盖新的组合背景。碎片 PNG 仍是独立文件，不合并为跨机器图集。

## 自定义 Screen

标准配方机器已自动接入。完全自定义 Menu 的客户端 Screen 可以复用渲染器：

```java
GuiTemplates.background(graphics, ResourceLocation.parse("example:textures/gui/custom_panel.png"),
        leftPos, topPos, imageWidth, imageHeight, menu.slots);
```

它查找 `example:custom_panel`，对应 `assets/example/gui/templates/custom_panel.json`。自定义 Screen 仍负责真实交互和动态数据；需要进度时取 `GuiTemplates.find(templateId)` 返回的 `GuiTemplate.Progress`，调用 `draw(graphics, left, top, fraction)`。自动绘制背景只画未工作状态。

旧路径别名由 `GuiTemplates` 解析，不会给 Minecraft 的所有 PNG 请求添加全局重定向。外部 Screen 如果直接 `graphics.blit` 已移除的 `recipes/*.png`，应改用这个模板入口，或使用保留的 `machines/*.png` 路径。

`GuiTemplates`、`GuiTemplate`、`GuiSprite` 属于客户端实现层，不要在公共静态注册、服务端或公共 API 类里引用它们。这里没有改动 API 0.3.0 的公共签名。已有 `BehaviorMachine`/`LayoutPart` 的特殊能量、长流体槽、开关仍沿用它们原有的 GUI sheet UV 契约；新模板可替换其背景，不自动替代这些动态控件。

## JEI / EMI

两者共用 `RecipeGuiLayout`：按实际物品/流体组成排列，只画有效槽位、进度条、特殊工具和必要文字。外框和底色由配方查看器提供，不再叠加机器的 base 面板，也不会带上玩家背包。槽内装饰与进度条复用同一模板素材。未指定等级的配方省略等级行，后续说明和所需高度随之收紧。

- EMI 每条配方使用自己的宽高；可用窗口更小时，图形与槽位命中区域一起缩放。
- JEI 一个分类必须固定宽高，取该分类所有实际展示配方的最大内容范围。较简单的配方不会补画多余槽位。
- 文本保留最小可读宽度 140 像素；只排列实际出现的说明行，不为缺失功率/时间留空行。
- 原配方索引与材料替代、流体搜索、催化剂、概率、快捷键、配方树保持独立于紧凑坐标。砧配方中有意义的空工件位置保留。

## 预览、重载与验证

```powershell
python tools/gui/preview.py --output work/gui-preview
python tools/gui/preview.py --assets core/src/main/resources/assets --assets tools/gui/examples/assets --template example:machines/washer --machine-slots 1 1 1 0 --progress 0.5 --output work/gui-example
```

游戏中用 `F3+T` 重载素材与布局缓存。槽位坐标的服务器定义没有被重载；改变自定义 Menu 后需要按原开发流程重编译。

修改共享渲染代码后执行 `compileBothPlatforms`；改变资源后执行 `assembleBothPlatforms`。静态预览与编译不能证明游戏内效果，验收时还需检查：有/无流体、进度重置、标题宽度、不同 GUI 缩放、JEI/EMI 悬停和快捷键、资源包重载，以及两个加载器。预览工具不会覆盖用户整理目录或生成新的整机 PNG 到生产资源中。
