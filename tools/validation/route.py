#!/usr/bin/env python3
"""Read-only router and consistency checker for the validation documentation."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
VALIDATION_DIR = REPO_ROOT / "docs" / "development" / "validation"
ROUTES_FILE = VALIDATION_DIR / "routes.yaml"
CHECKS_DIR = VALIDATION_DIR / "checks"
OVERRIDES_DIR = VALIDATION_DIR / "overrides"
DIMENSION_DOC_DIRS = {
    "packages": REPO_ROOT / "docs" / "architecture" / "packages",
    "overrides": OVERRIDES_DIR,
}
SOURCE_PREFIXES = ("api/", "core/", "minecraft/")


class YamlError(ValueError):
    """Raised when the restricted YAML reader cannot parse the route file."""


def strip_comment(line: str) -> str:
    if line.lstrip().startswith("#"):
        return ""
    return line.rstrip("\n")


def split_flow(text: str) -> list[str]:
    parts: list[str] = []
    buf: list[str] = []
    quote: str | None = None
    for char in text:
        if quote:
            buf.append(char)
            if char == quote:
                quote = None
        elif char in "\"'":
            quote = char
            buf.append(char)
        elif char == ",":
            parts.append("".join(buf).strip())
            buf = []
        else:
            buf.append(char)
    if buf:
        parts.append("".join(buf).strip())
    return parts


def parse_scalar(text: str):
    text = text.strip()
    if text == "[]":
        return []
    if text.startswith("[") and text.endswith("]"):
        inner = text[1:-1].strip()
        if not inner:
            return []
        return [parse_scalar(part) for part in split_flow(inner)]
    if len(text) >= 2 and text[0] == text[-1] and text[0] in "\"'":
        return text[1:-1]
    if re.fullmatch(r"-?\d+", text):
        return int(text)
    return text


class MiniYaml:
    """Reads the restricted YAML subset used by routes.yaml and override frontmatter."""

    def __init__(self, text: str):
        self.lines: list[tuple[int, str]] = []
        for raw in text.splitlines():
            line = strip_comment(raw)
            if not line.strip():
                continue
            indent = len(line) - len(line.lstrip(" "))
            self.lines.append((indent, line.strip()))
        self.index = 0

    def parse(self):
        if not self.lines:
            return {}
        value = self._parse_block(self.lines[0][0])
        if self.index != len(self.lines):
            raise YamlError(f"无法解析第 {self.index + 1} 个有效行")
        return value

    def _parse_block(self, indent: int):
        if self.index >= len(self.lines):
            return None
        current_indent, text = self.lines[self.index]
        if current_indent < indent:
            return None
        if text.startswith("- "):
            return self._parse_list(current_indent)
        return self._parse_mapping(current_indent)

    def _parse_list(self, indent: int) -> list:
        result: list = []
        while self.index < len(self.lines):
            current_indent, text = self.lines[self.index]
            if current_indent != indent or not text.startswith("- "):
                break
            item_text = text[2:].strip()
            self.index += 1
            if not item_text:
                result.append(self._parse_block(indent + 2))
                continue
            is_mapping = ":" in item_text and not item_text.startswith(("\"", "'", "["))
            if not is_mapping:
                result.append(parse_scalar(item_text))
                continue
            key, rest = item_text.split(":", 1)
            item: dict = {}
            item[key.strip()] = parse_scalar(rest) if rest.strip() else self._parse_block(indent + 2)
            while self.index < len(self.lines):
                next_indent, next_text = self.lines[self.index]
                if next_indent != indent + 2 or next_text.startswith("- "):
                    break
                if ":" not in next_text:
                    raise YamlError(f"无法解析列表项: {next_text}")
                next_key, next_rest = next_text.split(":", 1)
                self.index += 1
                item[next_key.strip()] = (
                    parse_scalar(next_rest) if next_rest.strip() else self._parse_block(indent + 4)
                )
            result.append(item)
        return result

    def _parse_mapping(self, indent: int) -> dict:
        result: dict = {}
        while self.index < len(self.lines):
            current_indent, text = self.lines[self.index]
            if current_indent != indent or text.startswith("- "):
                break
            if ":" not in text:
                raise YamlError(f"无法解析映射行: {text}")
            key, rest = text.split(":", 1)
            self.index += 1
            result[key.strip()] = (
                parse_scalar(rest) if rest.strip() else self._parse_block(indent + 2)
            )
        return result


def load_yaml(text: str):
    try:
        import yaml  # type: ignore
    except ImportError:
        return MiniYaml(text).parse()
    return yaml.safe_load(text)


def compile_glob(pattern: str) -> re.Pattern[str]:
    pattern = pattern.replace("\\", "/")
    output: list[str] = []
    index = 0
    while index < len(pattern):
        char = pattern[index]
        if char == "*":
            if index + 1 < len(pattern) and pattern[index + 1] == "*":
                if index + 2 < len(pattern) and pattern[index + 2] == "/":
                    output.append("(?:.*/)?")
                    index += 3
                else:
                    output.append(".*")
                    index += 2
            else:
                output.append("[^/]*")
                index += 1
        elif char == "?":
            output.append("[^/]")
            index += 1
        else:
            output.append(re.escape(char))
            index += 1
    return re.compile("^" + "".join(output) + "$")


def route_patterns(route: dict) -> list[re.Pattern[str]]:
    return [compile_glob(path) for path in route.get("paths", [])]


def matches(path: str, patterns: list[re.Pattern[str]]) -> bool:
    return any(pattern.match(path) for pattern in patterns)


def load_routes() -> dict:
    if not ROUTES_FILE.exists():
        raise SystemExit(f"缺少路由文件: {ROUTES_FILE}")
    data = load_yaml(ROUTES_FILE.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise SystemExit("routes.yaml 顶层必须是映射")
    return data


def tracked_files() -> list[str]:
    command = [
        "git",
        "-C",
        str(REPO_ROOT),
        "ls-files",
        "--cached",
        "--others",
        "--exclude-standard",
    ]
    result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8")
    if result.returncode != 0:
        raise SystemExit(f"git ls-files 失败: {result.stderr.strip()}")
    files = []
    for line in result.stdout.splitlines():
        line = line.strip().replace("\\", "/")
        if line:
            files.append(line)
    return sorted(files)


def normalize_path(value: str) -> str:
    path = Path(value)
    if path.is_absolute():
        try:
            path = path.resolve().relative_to(REPO_ROOT)
        except ValueError:
            return value.replace("\\", "/")
    return str(path).replace("\\", "/")


def route_changes(data: dict, changed: list[str]) -> tuple[list[str], list[str], list[str]]:
    routes = data.get("routes") or []
    route_entries = [(route, route_patterns(route)) for route in routes]
    packages: set[str] = set()
    checks: set[str] = set()
    overrides: set[str] = set()
    for raw in changed:
        path = normalize_path(raw)
        matched = [(route, patterns) for route, patterns in route_entries if matches(path, patterns)]
        if not matched:
            raise SystemExit(f"改动路径未命中任何路由: {path}")
        for route, _ in matched:
            packages.update(route.get("packages", []))
            checks.update(route.get("checks", []))
            overrides.update(route.get("overrides", []))
    checks = apply_overrides(checks, overrides)
    return sorted(packages), sorted(checks), sorted(overrides)


def read_frontmatter(path: Path) -> dict:
    if not path.exists():
        raise SystemExit(f"特例文档不存在: {path}")
    lines = path.read_text(encoding="utf-8").splitlines()
    if not lines or lines[0].strip() != "---":
        raise SystemExit(f"特例文档缺少 frontmatter: {path}")
    try:
        end = lines.index("---", 1)
    except ValueError:
        raise SystemExit(f"特例文档 frontmatter 未闭合: {path}") from None
    data = load_yaml("\n".join(lines[1:end]))
    return data if isinstance(data, dict) else {}


def apply_overrides(checks: set[str], overrides: set[str]) -> set[str]:
    result = set(checks)
    for override in sorted(overrides):
        data = read_frontmatter(OVERRIDES_DIR / f"{override}.md")
        add = set(data.get("add") or [])
        remove = set(data.get("remove") or [])
        conflict = add & remove
        if conflict:
            raise SystemExit(f"特例 {override} 同时添加和删除: {', '.join(sorted(conflict))}")
        result.update(add)
        result.difference_update(remove)
    return result


def check_check_doc(path: Path) -> list[str]:
    errors: list[str] = []
    text = re.sub(r"```.*?```", "", path.read_text(encoding="utf-8"), flags=re.S)
    allowed_h2 = [
        ["## 验证方式", "## 注意事项", "## 运行命令"],
        ["## 验证方式", "## 验证原理", "## 注意事项", "## 运行命令"],
    ]
    h2: list[str] = []
    misplaced_h3: list[str] = []
    current_h2: str | None = None
    for line in text.splitlines():
        if line.startswith("## "):
            current_h2 = line.strip()
            h2.append(current_h2)
        elif line.startswith("### ") and current_h2 != "## 验证原理":
            misplaced_h3.append(line.strip())
    if h2 not in allowed_h2:
        errors.append(f"检查文档二级章节必须为 验证方式、[验证原理]、注意事项、运行命令: checks/{path.name}")
    if misplaced_h3:
        errors.append(f"检查文档 ### 小节只能出现在 ## 验证原理 下: checks/{path.name}")
    return errors


def check_docs() -> list[str]:
    data = load_routes()
    errors: list[str] = []
    if data.get("version") != 1:
        errors.append("routes.yaml version 必须为 1")
    dimensions = data.get("dimensions")
    if not isinstance(dimensions, dict):
        return ["routes.yaml 缺少 dimensions 映射"]
    dimension_values: dict[str, list[str]] = {}
    for name, values in dimensions.items():
        if not isinstance(values, list):
            errors.append(f"dimensions.{name} 必须是数组")
            continue
        dimension_values[name] = [str(value) for value in values]

    routes = data.get("routes")
    if not isinstance(routes, list):
        return errors + ["routes 必须是数组"]
    files = tracked_files()
    used_dimensions: dict[str, set[str]] = {name: set() for name in dimension_values}
    check_ids: set[str] = set()
    override_ids: set[str] = set()
    route_entries: list[tuple[int, dict, list[re.Pattern[str]]]] = []

    for index, route in enumerate(routes, start=1):
        if not isinstance(route, dict):
            errors.append(f"第 {index} 条路由必须是映射")
            continue
        paths = route.get("paths")
        if not isinstance(paths, list) or not paths:
            errors.append(f"第 {index} 条路由缺少 paths")
            continue
        allowed = set(dimension_values) | {"paths", "checks"}
        unknown = set(route) - allowed
        if unknown:
            errors.append(f"第 {index} 条路由含未知字段: {', '.join(sorted(unknown))}")
        for name in dimension_values:
            values = route.get(name, [])
            if not isinstance(values, list):
                errors.append(f"第 {index} 条路由的 {name} 必须是数组")
                continue
            for value in values:
                if value not in dimension_values[name]:
                    errors.append(f"第 {index} 条路由引用未声明的 {name} 值: {value}")
                else:
                    used_dimensions[name].add(value)
        checks = route.get("checks", [])
        if not isinstance(checks, list):
            errors.append(f"第 {index} 条路由的 checks 必须是数组")
            continue
        check_ids.update(checks)
        route_entries.append((index, route, route_patterns(route)))

    for name, values in dimension_values.items():
        for value in values:
            if value not in used_dimensions[name]:
                errors.append(f"维度值未使用: {name}.{value}")
            if name == "overrides":
                continue
            doc_dir = DIMENSION_DOC_DIRS.get(name, VALIDATION_DIR / name)
            doc_path = doc_dir / f"{value}.md"
            if not doc_path.exists():
                errors.append(f"维度文档不存在: {doc_path.name}")
            elif name == "packages":
                lines = doc_path.read_text(encoding="utf-8").splitlines()
                for heading in ("## 验收", "## 自动化测试范围"):
                    if heading not in lines:
                        errors.append(f"package 文档缺少 {heading}: {doc_path.name}")
    for check in sorted(check_ids):
        check_path = CHECKS_DIR / f"{check}.md"
        if not check_path.exists():
            errors.append(f"检查文档不存在: checks/{check}.md")
            continue
        errors.extend(check_check_doc(check_path))
    for override in dimension_values.get("overrides", []):
        if not (OVERRIDES_DIR / f"{override}.md").exists():
            errors.append(f"特例文档不存在: overrides/{override}.md")

    for index, route, patterns in route_entries:
        if not any(matches(path, patterns) for path in files):
            errors.append(f"第 {index} 条路由没有匹配任何文件")

    for path in files:
        if not path.startswith(SOURCE_PREFIXES):
            continue
        if "/src/" not in path:
            continue
        matched = [
            (route, patterns)
            for _, route, patterns in route_entries
            if matches(path, patterns)
        ]
        if not matched:
            errors.append(f"源码文件未命中路由: {path}")
            continue
        checks: set[str] = set()
        packages: set[str] = set()
        for route, _ in matched:
            checks.update(route.get("checks", []))
            packages.update(route.get("packages", []))
        if "format" not in checks:
            errors.append(f"源码路由缺少 format: {path}")
        if not packages:
            errors.append(f"源码路由缺少 package: {path}")

    errors.extend(check_links())
    return errors


def check_links() -> list[str]:
    errors: list[str] = []
    link_pattern = re.compile(r"(?<!!)\[[^\]]*\]\(([^)]+)\)")
    for markdown in sorted(VALIDATION_DIR.rglob("*.md")):
        text = markdown.read_text(encoding="utf-8")
        text = re.sub(r"```.*?```", "", text, flags=re.S)
        for raw_target in link_pattern.findall(text):
            target = raw_target.strip().strip("<>")
            if not target or target.startswith(("http://", "https://", "mailto:", "#")):
                continue
            target = target.split("#", 1)[0]
            if not target:
                continue
            if target.startswith("/"):
                resolved = REPO_ROOT / target.lstrip("/")
            else:
                resolved = (markdown.parent / target).resolve()
            if not resolved.exists():
                relative = markdown.relative_to(REPO_ROOT)
                errors.append(f"链接失效: {relative} -> {raw_target}")
    return errors


def print_route(data: dict, changed: list[str], as_json: bool) -> int:
    packages, checks, overrides = route_changes(data, changed)
    manual = sorted(check for check in checks if check.startswith("manual-"))
    automated = sorted(check for check in checks if not check.startswith("manual-"))
    if as_json:
        print(
            json.dumps(
                {
                    "packages": packages,
                    "checks": automated,
                    "manual": manual,
                    "overrides": overrides,
                },
                ensure_ascii=False,
                indent=2,
            )
        )
    else:
        print(f"packages: {', '.join(packages) or '-'}")
        print(f"checks: {', '.join(automated) or '-'}")
        print(f"manual: {', '.join(manual) or '-'}")
        print(f"overrides: {', '.join(overrides) or '-'}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    route_parser = subparsers.add_parser("route", help="计算改动路径对应的域、层与检查")
    route_parser.add_argument("--changed", nargs="+", required=True, help="改动文件路径")
    route_parser.add_argument("--json", action="store_true", help="输出 JSON")

    subparsers.add_parser("check", help="校验路由闭合、检查文档与相对链接")

    args = parser.parse_args()
    data = load_routes()
    if args.command == "route":
        return print_route(data, args.changed, args.json)
    errors = check_docs()
    if errors:
        for error in errors:
            print(f"错误: {error}", file=sys.stderr)
        print(f"共 {len(errors)} 项问题", file=sys.stderr)
        return 1
    print("路由闭合、检查文档与相对链接均通过")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
