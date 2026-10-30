# 发行资源检查

`check_compact_model_json.py` 使用 Python 3 标准库，只读取文件，不修改源码或 JAR。

输入是当前构建的发行 JAR，以及同一次构建的 core 和平台资源输出目录。先完成对应平台的发行构建，再在仓库根目录执行，例如：

```powershell
python tools/resources/check_compact_model_json.py minecraft/versions/1.21.1-neoforge/build/libs/gt6ce-neoforge-1.21.1-1.0.2.jar --resources core/build/resources/main minecraft/versions/1.21.1-neoforge/build/resources/main
```

按当前版本号调整 JAR 文件名。Forge 使用 `distributionJar` 的 `build/libs` 产物，不使用中间 JAR。

检查内容：

- 模板生成的模型和方块状态 JSON 与打包前定义语义一致。
- 静态保留的 JSON、全部 PNG 路径和字节不变。
- 模板索引没有重复路径、参数数量错误或资源缺失。

成功时输出资源数量、JAR 大小和粗略节省量；不匹配时以非零退出码结束。节省量不包含 ZIP 写入器特有的附加字段，不能作为精确构建前后差值。

本工具不替代游戏内渲染及资源包重载验证。
