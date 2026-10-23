"""Load reviewed positive workability evidence. Missing entries are NOT negative evidence.
Source hashes pin audited factory/conditional semantics, including WaxRefractory's post-heat flag.
IDs plus canonical names prevent the overlay from leaking onto unrelated addon materials.
"""
from pathlib import Path
import hashlib,json,re
FLAGS = ("FURNACE", "SOFT", "MORTAR", "EXTRUDER_SIMPLE")
def load_positive_profile(profile_path: Path, source: Path):
    data=json.loads(profile_path.read_text(encoding="utf-8"))
    if data.get("schema") != 1: raise ValueError("Unsupported workability positive profile schema")
    digest=hashlib.sha256(source.read_bytes()).hexdigest()
    if digest not in {s["sha256"] for s in data["sources"]}:
        raise ValueError("Unaudited MT.java source: regenerate/review the positive workability profile; do not infer absent flags")
    result={f:[] for f in FLAGS};seen=set()
    for row in data["rows"]:
        id=row["id"];name=re.sub(r"[^a-z0-9]","",row["canonical_name"].lower())
        if not isinstance(id,int) or not 0<id<10000 or not name or id in seen:
            raise ValueError("Ambiguous or invalid built-in workability identity: "+str(row))
        seen.add(id)
        if not row.get("evidence") or not row["flags"] or any(f not in FLAGS for f in row["flags"]):
            raise ValueError("Invalid workability evidence: "+str(row))
        for flag in row["flags"]:result[flag].append(str(id)+":"+name)
    return {flag:sorted(keys) for flag,keys in result.items()}
