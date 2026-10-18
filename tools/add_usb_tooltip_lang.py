"""Adds the §114 data-storage tooltip keys to both lang files, in sorted position.

The keys come from GT6's two data-storage behaviours (`Behavior_DataStorage:42`,
`Behavior_DataStorage16:42-54`) and the shared data renderer (`UT.java:2237-2269`).
Idempotent: re-running rewrites the same values and keeps the file's key order.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import io
import json
import os

ASSETS = os.path.join(str(native_path('src')), 'main', 'resources', 'assets', 'gt6ce', 'lang')

EN = {
    "gt.tooltip.usb.tier": "Data: USB %s.0",
    "gt.tooltip.usb.empty": "This Stick is Empty",
    "gt.tooltip.usb.formatted": "Perfectly Formatted",
    "gt.tooltip.usb.unclean": "Uncleanly Formatted",
    "gt.tooltip.usb.slot_empty": "Data Slot %s is Empty",
    "gt.tooltip.usb.material_data": "Material Data: %s",
    "gt.tooltip.usb.material_short": "Mat Data: %s (%s/%s/%s)",
    "gt.tooltip.usb.replicable_hint": "Can be Replicated using",
    "gt.tooltip.usb.neutral_matter": "Neutral Matter: ",
    "gt.tooltip.usb.charged_matter": "Charged Matter: ",
    "gt.tooltip.usb.energy": "Energy: ",
    "gt.tooltip.usb.not_replicable": " (Not Replicatable)",
}
ZH = {
    "gt.tooltip.usb.tier": "数据：USB %s.0",
    "gt.tooltip.usb.empty": "这根存储棒是空的",
    "gt.tooltip.usb.formatted": "格式完好",
    "gt.tooltip.usb.unclean": "格式不干净",
    "gt.tooltip.usb.slot_empty": "数据槽 %s 为空",
    "gt.tooltip.usb.material_data": "材料数据：%s",
    "gt.tooltip.usb.material_short": "材料数据：%s（%s/%s/%s）",
    "gt.tooltip.usb.replicable_hint": "可用以下物质复制",
    "gt.tooltip.usb.neutral_matter": "中性物质：",
    "gt.tooltip.usb.charged_matter": "带电物质：",
    "gt.tooltip.usb.energy": "能量：",
    "gt.tooltip.usb.not_replicable": "（不可复制）",
}


def update(name, additions):
    path = os.path.join(ASSETS, name)
    data = json.load(io.open(path, encoding='utf-8'))
    added = 0
    for key, value in additions.items():
        if data.get(key) != value:
            data[key] = value
            added += 1
    ordered = {key: data[key] for key in sorted(data)}
    with io.open(path, 'w', encoding='utf-8', newline='\n') as fh:
        json.dump(ordered, fh, ensure_ascii=False, indent=2)
        fh.write('\n')
    print('%s: %d keys, %d updated' % (name, len(ordered), added))


update('en_us.json', EN)
update('zh_cn.json', ZH)
