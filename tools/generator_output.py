"""Shared output handling for generators that write maintained core sources."""
from pathlib import Path


def write_or_check(path: Path, content: str, *, check: bool) -> None:
    """Compare normalized text in check mode; otherwise write deterministic UTF-8."""
    if check:
        if not path.is_file() or path.read_text(encoding="utf-8-sig") != content:
            raise SystemExit(f"Generated output differs: {path}")
        print(f"Unchanged: {path}")
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(content)
    print(f"Wrote: {path}")
