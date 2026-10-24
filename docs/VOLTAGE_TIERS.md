# Original GregTech 6 voltage tiers / 原 GT6 电压等级

The project follows original GT6 `gregapi.data.CS.V` / `CS.VN`, not the GT5/GTCE naming ladder. Both loaders consume the shared `GTVoltageTiers`; legacy `GregTechConstants.V` / `VN` are copies of that canonical table. Public arrays are compatibility views; callers must not mutate them.

项目遵循原GT6的电压值和名称。此前显示层在UV后混入UHV～UXV/MAX，而旧常量最高两项误用2147483647和Long.MAX_VALUE；现在统一为原GT6值。此变更保留ULV～UV值，不把HU/KU/RU误作EU电压，也不改变柴油的RU功率包设计。

```text
index   name    maximum EU packet size (V)
0       ULV     8
1       LV      32
2       MV      128
3       HV      512
4       EV      2048
5       IV      8192
6       LuV     32768
7       ZPM     131072
8       UV      524288
9       PUV1    2097152
10      PUV2    8388608
11      PUV3    33554432
12      PUV4    134217728
13      PUV5    536870912
14      XV      2147483648
15      XV      8589934592
```

`tierMax` returns the first tier ceiling containing the absolute packet size; above the table it returns `VOLTAGES.length` (16), as original GT6 does, so it is not always a valid index. `tierMin` uses the original lower-tier boundary lookup and stays within 0..15. `Long.MIN_VALUE` keeps GT6's signed-magnitude result, tier 0. EU uses packet size × packet count, rather than a voltage-free total stored energy number. Forge Energy conversion remains a separate adapter; one API constant does not prove all high-tier machines/wires/bridges have been exercised. The final two corrections can affect any addon/layout that assumed the former sentinels; no universal saved-world migration is claimed.

原始依据：本地只读原GT6 `src/main/java/gregapi/data/CS.java:146–157`。来源SHA和本批检查见 `core/provenance/addon-voltage-20261002.json`、`docs/integration/verification/addon-voltage-20261002.md`。
