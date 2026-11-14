# 发行资源检查

本目录提供发行包资源完整性检查脚本，均使用 Python 3 标准库只读运行。

## check_compact_model_json.py

核对模板化模型、方块状态 JSON 与 PNG 资源在发行 JAR 中的语义和字节一致性。

输入是当前构建的发行 JAR，以及同一次构建的 core 和平台资源输出目录。先完成对应平台的发行构建，再在仓库根目录执行，例如：

```powershell
python tools/resources/check_compact_model_json.py minecraft/versions/1.21.1-neoforge/build/libs/gt6ce-neoforge-1.21.1-<version>.jar --resources core/build/resources/main minecraft/versions/1.21.1-neoforge/build/resources/main
```

按当前版本号调整 JAR 文件名。Forge 使用 `distributionJar` 的 `build/libs` 产物，不使用中间 JAR。

检查内容：

- 模板生成的模型和方块状态 JSON 与打包前定义语义一致。
- 静态保留的 JSON 路径不变，全部 PNG 与 `build/resources/main` 中的处理后字节一致。
- 模板索引没有重复路径、参数数量错误或资源缺失。

PNG 比较的是处理后产物：`processResources` 在复制时剥离贴图元数据块，JAR 与 `build/resources/main` 因此逐字节相同，两者都不同于提交树里的原始贴图。成功时输出资源数量、JAR 大小和粗略节省量；不匹配时以非零退出码结束。节省量不包含 ZIP 写入器特有的附加字段，不能作为精确构建前后差值。

## check_png_metadata.py

审计 PNG 的块结构与透明像素覆盖，可指向发行 JAR、处理后资源目录或源码目录：

```powershell
# 发行产物必须零元数据、零尾随字节、零保留块 CRC 错误
python tools/resources/check_png_metadata.py minecraft/versions/1.21.1-neoforge/build/libs/gt6ce-neoforge-1.21.1-<version>.jar --strict
python tools/resources/check_png_metadata.py core/build/resources/main minecraft/versions/1.21.1-neoforge/build/resources/main --strict

# 源码树允许保留编辑器元数据：给出基线即变成只对未登记文件报错
python tools/resources/check_png_metadata.py core/src/main/resources --baseline work/png-baseline.txt

# 只读巡检：列出发现但不因元数据失败
python tools/resources/check_png_metadata.py core/src/main/resources
```

检查内容：

- 8 字节 PNG 签名、`IHDR` 必须位于首块、`IEND` 必须存在、块长度不得越过文件末尾。
- 块类型集合必须为 `IHDR`、`PLTE`、`tRNS`、`IDAT`、`IEND`；其余块（`pHYs`、`tIME`、`iCCP`、`tEXt` 等）报告为元数据。
- 保留块的 CRC32 校验与 `IEND` 之后的尾随字节。保留块 CRC 错误、签名缺失、结构截断始终失败：这些块会原样进入发行包，客户端无法解码。
- 被丢弃块的 CRC 错误与元数据一起列为可豁免项：源码树里已有若干贴图的 `iCCP` 校验错误，构建会整块删除，不影响产物。
- `--transparent` 额外用标准库解码 IDAT 并反过滤，报告全透明（无可见像素）贴图。
- `--baseline` 逐行读取允许列表，支持 `#` 注释与 shell 通配；未命中的发现即失败，用于源码审计。
- `--strict` 与 `--baseline` 互斥，要求零元数据、零尾随字节、零 CRC 错误，用于打包产物。

`-PdirectCoreResources=true` 会让 core 直接以源码目录作为资源输出并跳过 `processResources`，此时 PNG 块过滤不生效。检查 `build/resources/main` 或发行 JAR 时保持默认配置。

本工具不替代游戏内渲染及资源包重载验证。
