# GUI 模板检查与预览

从仓库根目录运行。模板格式、接入方法见[开发指南](../../docs/development/gui-templates.md)。

仅检查需要 Python 3，无第三方依赖。输出 PNG/HTML 预览另需 Pillow（`python -m pip install Pillow`）。

```powershell
# 检查全部公共模板、旧路径别名、图标引用、PNG 尺寸和进度条两半
python tools/gui/preview.py

# 按正式资源生成静态预览；不会改写源资源
python tools/gui/preview.py --output work/gui-preview

# 检查附属示例，并模拟 1 物品输入、1 物品输出、1 流体输入的 Menu
python tools/gui/preview.py --assets core/src/main/resources/assets --assets tools/gui/examples/assets --template example:machines/washer --machine-slots 1 1 1 0 --progress 0.5 --output work/gui-example
```

打开输出目录的 `index.html` 查看布局，`sprites.html` 查看所有碎片的名称、像素尺寸和图片，可直接复制图标名到 JSON。`--assets` 可重复，后者覆盖前者；与 Minecraft 一样，按整个 JSON 文件覆盖，不合并目录文件的字段。`--template` 可重复。`--progress` 为 0～1。

`--machine-slots` 仅模拟标准配方机器的槽位坐标。自定义 Menu、物品、标题、流体内容、交互和动画插值需要游戏内验证。预览不会执行注册、修改配方或启动服务器。

`examples/assets` 是可直接复制的附属资源示例，不会打入 GT6CE 发行包。不重跑一次性的图片提取程序；编辑生产资源与示例即可。
