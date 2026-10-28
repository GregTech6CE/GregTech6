#!/usr/bin/env python3
"""Prove that a working-tree change touched comments only.

Compares each modified/untracked Java file against HEAD with every comment
replaced by whitespace (newlines kept). Differences mean code, literals or
Stonecutter markers changed.

Usage:
  python tools/verify_comments_only.py [--base REV] [paths...]
"""
import argparse
import os
import subprocess
import sys


def strip_comments(text):
    """Replace comments with spaces, keeping newlines and literals intact."""
    out = []
    i, n = 0, len(text)
    while i < n:
        if text.startswith('"""', i):
            j = text.find('"""', i + 3)
            j = n if j < 0 else j + 3
            out.append(text[i:j])
            i = j
        elif text[i] in "\"'":
            quote = text[i]
            j = i + 1
            while j < n and text[j] != quote:
                j += 2 if text[j] == "\\" else 1
            j = min(j + 1, n)
            out.append(text[i:j])
            i = j
        elif text.startswith("//", i):
            j = text.find("\n", i)
            j = n if j < 0 else j
            out.append(" " * (j - i))
            i = j
        elif text.startswith("/*", i):
            j = text.find("*/", i + 2)
            j = n if j < 0 else j + 2
            out.append("".join(c if c == "\n" else " " for c in text[i:j]))
            i = j
        else:
            out.append(text[i])
            i += 1
    return "".join(out)


def markers(text):
    return [ln for ln in text.splitlines() if ln.strip().startswith("//?")]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="HEAD")
    ap.add_argument("paths", nargs="*")
    args = ap.parse_args()

    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    if args.paths:
        paths = args.paths
    else:
        diff = subprocess.run(["git", "diff", "--name-only", args.base],
                              cwd=root, capture_output=True, text=True).stdout.split()
        paths = [p for p in diff if p.endswith(".java")]

    bad = 0
    for rel in paths:
        head = subprocess.run(["git", "show", f"{args.base}:{rel}"], cwd=root,
                              capture_output=True)
        if head.returncode != 0:
            print(f"skip (not in {args.base}): {rel}")
            continue
        cur = open(os.path.join(root, rel), "rb").read()
        def norm(text):
            lines = strip_comments(text.replace("\r\n", "\n")).split("\n")
            return "\n".join(ln.rstrip() for ln in lines if ln.strip())

        a = norm(head.stdout.decode("utf-8"))
        b = norm(cur.decode("utf-8"))
        if a != b:
            bad += 1
            print(f"CODE CHANGED: {rel}")
            for k, (x, y) in enumerate(zip(a.split("\n"), b.split("\n")), 1):
                if x != y:
                    print(f"  line {k}\n   HEAD: {x.strip()[:100]!r}\n    now: {y.strip()[:100]!r}")
                    break
        ma, mb = markers(head.stdout.decode("utf-8")), markers(cur.decode("utf-8"))
        if ma != mb:
            bad += 1
            print(f"STONECUTTER MARKERS CHANGED: {rel}")
    print(f"\n{len(paths)} files, {bad} with non-comment changes", file=sys.stderr)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
