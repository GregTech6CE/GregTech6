# Jade 配置翻译缺失：原因与防复发检查

记录日期：2026-09-12。适用于本项目新增或修改 Jade 提供器时的开发检查。

反馈这类错误已多次出现。本次确认的具体遗漏是多方块主控的配置名称；更早的具体键未在这里逐一复核，不将它们记为已确认案例。

## 本次错误与原因

```text
Caused by: java.lang.AssertionError: Missing config translation: config.jade.plugin_gregtech.multiblock
```

`MultiblockJadeProvider.getUid()` 返回 `gregtech:multiblock`，并在 `GTJadePlugin.registerClient()` 中注册为方块提示组件。对应的 Jade 配置项需要名称翻译：

```text
提供器 UID：gregtech:multiblock
配置名称键：config.jade.plugin_gregtech.multiblock
```

上一轮只添加了 `jade.gregtech.multiblock.formed` 等悬浮提示文字，漏掉配置名称。两者用途不同，不能互相替代。本次中英文语言文件都缺少该键，客户端触发 Jade 的翻译校验时因此报错。

## 已修复的位置

- `src/main/resources/assets/gregtech/lang/en_us.json`：`"config.jade.plugin_gregtech.multiblock": "GT Multiblock Controller"`。
- `src/main/resources/assets/gregtech/lang/zh_cn.json`：`"config.jade.plugin_gregtech.multiblock": "GT 多方块主控"`。
- `tools/tests/test_jade_translations.py`：检查本项目 Jade 提供器对应的中英文配置名称是否存在且非空。

不要通过删除提供器、改掉既有 UID 或关闭断言来掩盖翻译遗漏。应补齐实际缺失的资源，并保留配置项身份。

## 后续新增或修改 Jade 功能时

1. 检查提供器的 `getUid()`，以及 `GTJadePlugin` 中的客户端注册。
2. 根据 UID 补齐 `config.jade.plugin_<命名空间>.<路径>`；本项目当前使用的提供器路径均为简单名称，例如 `basic_machine`、`multiblock`。
3. 同时更新 `en_us.json` 和 `zh_cn.json`。不能只补中文；英文是回退语言。
4. 另外检查悬浮提示正文的翻译。配置名称存在，不代表提示中的每个翻译键都存在。
5. 若语言文件由脚本更新，确认脚本保留这些键；运行生成器之后再次执行检查。
6. 在项目根目录执行下面的专项检查，并实际启动一次客户端检查 Jade 配置页及方块提示。

```powershell
python -m unittest tools.tests.test_jade_translations
```

也可以随现有 Python 测试一起执行：

```powershell
python -m unittest discover -s tools/tests
```

## 自动检查的边界

当前测试读取 `integration/jade` 下的 `*Provider.java`，识别字面量形式的 `ResourceLocation.fromNamespaceAndPath("namespace", "path")`，检查对应中英文配置名称。无法识别提供器 UID 时会失败，避免直接跳过该文件。

以后如果改为公共常量、动态 UID、其他构造方式，或者更改提供器目录/命名规则，需要同步调整测试。它不是 Java 语义解析器，也不自动发现所有自定义配置项或全部提示正文翻译。

这项检查目前属于 Python 测试集，**没有自动接入 Gradle `build`**。Java 编译通过、服务器 GameTest 通过，均不能代替 Jade 客户端翻译检查。前一轮服务端和资源检查没有覆盖这一遗漏，不能据此声称客户端启动已验证。

## 修复后仍报同一个键时

先核对客户端实际读取的是不是修改后的资源：

- IDEA `runClient`：确认运行的是当前项目，并让正常的 `processResources` 处理本次语言文件修改；不要跳过资源处理。
- 独立客户端：重新构建并使用更新后的 JAR。只改源码不会更新已安装的旧 JAR。
- 检查输出资源或 JAR 中的 `assets/gregtech/lang/en_us.json` 是否确实包含报错键，再检查语言文件 JSON 是否有效、键名是否拼错。

本次源码修复的专项检查先复现中英文两处缺失，再补键后通过；本次未重新打包 JAR，也未把该检查结果当作客户端完整启动验收。
