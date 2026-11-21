#!/usr/bin/env python3
"""Two-dimensional attribution scanner and audit tool for Stonecutter //? blocks.

Scans shared Java inputs (minecraft/src/main/java and api/minecraft/src),
verifies //? block closure, detects hard violations, and classifies every
conditional block along two orthogonal axes:

  - origin: MC_VERSION | LOADER | SELF_DEFECT | COMPAT
  - disposition: BASE_CLASS | REMOVE | FACADE | FOLLOW_BODY | FLOOR_KEEP

Plan A permanent floor items (Capability exposure/query & imports, pure
Loader 1:1 package swaps in imports and FQCNs, third-party mod compat under
compat/, and Mixins under mixin/) are classified as FLOOR_KEEP and separated
from the Actionable Backlog.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

ORIGINS = ("MC_VERSION", "LOADER", "SELF_DEFECT", "COMPAT")
DISPOSITIONS = ("BASE_CLASS", "REMOVE", "FACADE", "FOLLOW_BODY", "FLOOR_KEEP")
WAVES = (
    "Wave 1a",
    "Wave 1b",
    "Wave 1c",
    "Wave 2a",
    "Wave 2b",
    "Wave 2c",
    "Wave 3",
    "Wave 4",
    "Floor",
)

SCAN_ROOTS = ("minecraft/src/main/java", "api/minecraft/src")
NODE_ROOTS = (
    ("1.20.1-forge", "minecraft/versions/1.20.1-forge/src/main/java"),
    ("1.21.1-neoforge", "minecraft/versions/1.21.1-neoforge/src/main/java"),
)

IF_RE = re.compile(r"^//\?\s*if\s+(.*?)\s*\{\s*$")
ELSE_RE = re.compile(r"^(?:/\*\*|\*)?//\?\}\s*else\s*\{|^/\*//\?\}\s*else\s*\{")
CLOSE_RE = re.compile(r"^(?:/\*\*|\*)?//\?\}|^/\*//\?\}")
PLATFORM_REF_RE = re.compile(r"gregtech\.platform\.(?:forge|neoforge)\b")
CLASS_DECL_RE = re.compile(
    r"^[ \t]*(?:(?:/\*[ \t]*)?(?:public|protected|private)\s+(?:(?:abstract|final|sealed|non-sealed|static)\s+)*|"
    r"(?:abstract|final|sealed|non-sealed)\s+(?:(?:public|protected|private|static)\s+)*)"
    r"class\s+(\w+)(?:<[^>{]+>)?(?:\s+extends\s+([\w.]+)(?:<[^>{]+)?)?",
    re.MULTILINE,
)

CAP_RE = re.compile(
    r"\b(?:ForgeCapabilities|Capabilities|LazyOptional|getCapability|invalidateCaps|"
    r"reviveCaps|RegisterCapabilitiesEvent|ICapabilityProvider|BlockCapability|"
    r"ItemCapability|EntityCapability)\b|"
    r"net\.minecraftforge\.common\.capabilities|"
    r"net\.neoforged\.neoforge\.capabilities|"
    r"net\.minecraftforge\.common\.util\.LazyOptional"
)

BE_LIFECYCLE_RE = re.compile(
    r"\b(?:saveAdditional|loadAdditional|getUpdateTag|handleUpdateTag|"
    r"onDataPacket|getUpdatePacket|saveWithoutMetadata|loadStatic)\b"
)
BE_UTILITY_CLASSES = {
    "NbtCompat",
    "HandlerNbt",
    "StackData",
    "BlockEntityData",
    "SavedData",
}

BLOCK_INTERACTION_RE = re.compile(
    r"\b(?:useItemOn|useWithoutItem)\b|\bInteractionResult\s+use\s*\("
)
MAP_CODEC_RE = re.compile(r"\bMapCodec\b|\bcodec\s*\(\s*\)")

LOADER_API_RE = re.compile(
    r"net\.minecraftforge|net\.neoforged|ForgeRegistries|ForgeHooks|"
    r"ForgeEventFactory|EventHooks|MinecraftForge|NeoForge\.|ModConfigSpec|"
    r"ForgeConfigSpec|IEventBus|ModLoadingContext|FMLJavaModLoadingContext|"
    r"\bDist\b|\bOnlyIn\b|FluidType|FluidUtil|IFluidHandler|IItemHandler|"
    r"IEnergyStorage|IClientItemExtensions|RegisterColorHandlersEvent|"
    r"ModelEvent|ChunkRenderTypeSet|BakedModel|ModelBakery|NetworkHooks|"
    r"CustomPacketPayload|SimpleChannel|PayloadRegistrar|IPlantable|"
    r"PlantType|FakePlayer|ServerLifecycleHooks|initializeClient"
)

MC_API_RE = re.compile(
    r"net\.minecraft\.|com\.mojang\.|HolderLookup|CustomData|DataComponent|DataComponents|ContainerHelper|"
    r"SavedData|fromNamespaceAndPath|BuiltInRegistries|RecipeHolder|CraftingInput|"
    r"StreamCodec|\bCodec\b|\bMapCodec\b|getCloneItemStack|getDrops|"
    r"playerWillDestroy|getExpDrop|isPathfindable|GuiGraphics|\bScreen\b|"
    r"AbstractContainerScreen|ModelResourceLocation|ParticleOptions|LootTable|"
    r"Enchantment|parseOptional|isSameItemSame|isSameFluidSame|BlockEntityData|"
    r"BlockStateData|ItemStack|FluidStack|ResourceLocation|TagKey|CompoundTag|"
    r"AbstractPackResources|readMetaAndCreate|BlockEntityWithoutLevelRenderer|"
    r"FallingBlock|SandBlock|TransparentBlock|GlassBlock|MenuProvider|"
    r"FriendlyByteBuf|RegistryFriendlyByteBuf|ServerLevel|LevelReader|"
    r"TooltipFlag|TooltipContext|InteractionResult|ItemInteractionResult|"
    r"BlockState|BlockPos|FluidState|ItemLike|Entity|LivingEntity|Player|"
    r"DamageSource|FoodProperties|MobEffect|SoundEvent|CreativeModeTab|"
    r"Structure|PlacementModifier|Biome|LootContext|LootParams|RecipeSerializer|"
    r"Ingredient|Criterion|Advancement|StateDefinition|ClientboundBlockEntityDataPacket|"
    r"BlockBehaviour|TreeGrower|AbstractTreeGrower|PoseStack|VertexConsumer|"
    r"QuadBakingVertexConsumer|OverlayTexture|BakedQuad|TextureAtlasSprite|"
    r"SpriteContents|MultiBufferSource|Tesselator|BufferBuilder|VertexFormat|"
    r"LightTexture|ItemTransforms|IoSupplier|FastColor|Matrix4f|Matrix3f|"
    r"PackLocationInfo|PackSelectionConfig|PackSource|PackType|addVertex|endVertex|"
    r"overlayCoords|\buv2\b|\bFont\b|BlockEntity|MenuType|AbstractContainerMenu"
)

CLIENT_API_RE = re.compile(
    r"ModelResourceLocation|BakedModel|GuiGraphics|AbstractContainerScreen|"
    r"ItemColor|BlockColor|IClientItemExtensions|initializeClient|ModelEvent|"
    r"RegisterColorHandlersEvent|ClientPackFacade|BakedModelMap|ClientModelEvents|"
    r"readMetaAndCreate|AbstractPackResources|BlockEntityWithoutLevelRenderer|"
    r"RenderType|ItemBlockRenderTypes|EntityRenderers|MenuScreens|RegisterMenuScreensEvent"
)

VANILLA_PROVIDER_METHOD_RE = re.compile(
    r"\b(?:saveAdditional|loadAdditional|getUpdateTag|handleUpdateTag|"
    r"onDataPacket|getUpdatePacket|saveWithoutMetadata|loadStatic|"
    r"appendHoverText|ContainerHelper|SavedData|serializeNBT|deserializeNBT|"
    r"parseOptional|saveWithFullMetadata|save|createMenu|decode|encode)\b"
)


@dataclass
class RawBlock:
    file: str
    line: int
    end_line: int
    cond: str
    then_lines: list[str]
    else_lines: list[str]
    has_else: bool
    nested: bool = False


@dataclass
class ClassifiedBlock:
    file: str
    line: int
    end_line: int
    cond: str
    shape: str
    origin: str
    disposition: str
    wave: str
    reason: str
    violations: list[str] = field(default_factory=list)
    then_preview: str = ""
    else_preview: str = ""


@dataclass
class FileContext:
    rel_path: str
    text: str
    class_name: str
    superclass: str
    is_block_entity: bool
    is_block_class: bool
    is_item_class: bool
    blocks: list[RawBlock]
    outside_platform_refs: list[tuple[int, str]]
    unclosed_count: int
    stray_count: int


def strip_comment_wrappers(lines: list[str]) -> list[str]:
    """Strip Stonecutter's /* ... */ comment wrappers from inactive branch lines."""
    out: list[str] = []
    for raw in lines:
        s = raw.strip()
        if s.startswith("/**/"):
            s = s[4:].strip()
        if s.startswith("/*"):
            s = s[2:].strip()
        if s.endswith("*/"):
            s = s[:-2].strip()
        if s:
            out.append(s)
    return out


def extract_code_lines(lines: list[str]) -> list[str]:
    """Return non-empty lines excluding pure comments."""
    out: list[str] = []
    for s in strip_comment_wrappers(lines):
        if s.startswith("//"):
            continue
        if s.startswith("/*") and s.endswith("*/"):
            continue
        if s in ("/*", "/**", "*/", "*"):
            continue
        if s.startswith("* ") or s.startswith("*/"):
            continue
        out.append(s)
    return out


def norm_ws(lines: list[str]) -> str:
    return re.sub(r"\s+", " ", " ".join(lines)).strip()


def norm_compact(lines: list[str]) -> str:
    return re.sub(r"\s+", "", "".join(lines))


def norm_loader_pkg(text: str) -> str:
    """Normalize 1:1 Forge <-> NeoForge package prefixes for Plan A comparison."""
    text = text.replace("net.neoforged.neoforge.", "net.minecraftforge.")
    text = text.replace("net.neoforged.fml.", "net.minecraftforge.fml.")
    text = text.replace("net.neoforged.bus.api.", "net.minecraftforge.eventbus.api.")
    text = text.replace("net.neoforged.api.distmarker.", "net.minecraftforge.api.distmarker.")
    return text


FLOOR_LOADER_TYPE_RE = re.compile(
    r"(?:net\.neoforged\.neoforge|net\.minecraftforge)\.(?:"
    r"fluids\.(?:FluidStack|FluidType|FluidUtil|capability\.IFluidHandler|capability\.IFluidHandlerItem|FluidTank|\*)"
    r"|items\.(?:IItemHandler|IItemHandlerModifiable|ItemStackHandler|ItemHandlerHelper|SlotItemHandler|wrapper\.InvWrapper|\*)"
    r"|capabilities\.\w+|common\.capabilities\.\w+|common\.util\.LazyOptional"
    r")"
)
ANY_LOADER_PKG_RE = re.compile(
    r"(?:net\.neoforged\.(?:neoforge|fml|bus\.api|api\.distmarker)|net\.minecraftforge(?:\.fml|\.eventbus\.api|\.api\.distmarker)?)\.[A-Za-z0-9_.*]+"
)


def is_pure_loader_package_rename(th_str: str, el_str: str) -> bool:
    """True only when branches differ solely by allowed Plan A Loader 1:1 data/capability types."""
    if not th_str or not el_str or th_str == el_str:
        return False
    if norm_loader_pkg(th_str) != norm_loader_pkg(el_str):
        return False
    refs = ANY_LOADER_PKG_RE.findall(th_str) + ANY_LOADER_PKG_RE.findall(el_str)
    return bool(refs) and all(FLOOR_LOADER_TYPE_RE.fullmatch(ref) for ref in refs)


def is_import_only(code_lines: list[str]) -> bool:
    return bool(code_lines) and all(line.startswith("import ") for line in code_lines)


def is_pure_annotation(code_lines: list[str]) -> bool:
    if not code_lines:
        return False
    joined = " ".join(code_lines)
    if any(tok in joined for tok in ("{", "}", ";")):
        return False
    return all(line.startswith("@") for line in code_lines)


def parse_java_file(path: Path, repo_root: Path) -> FileContext:
    rel_path = path.relative_to(repo_root).as_posix()
    text = path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines(True)

    stack: list[dict[str, Any]] = []
    blocks: list[RawBlock] = []
    outside_platform_refs: list[tuple[int, str]] = []
    stray_count = 0

    for lineno, raw in enumerate(lines, 1):
        stripped = raw.strip()
        if_match = IF_RE.match(stripped)
        if if_match:
            if stack:
                stack[-1]["nested"] = True
            stack.append(
                {
                    "line": lineno,
                    "cond": if_match.group(1).strip(),
                    "then": [],
                    "else": [],
                    "has_else": False,
                    "state": "then",
                    "nested": False,
                }
            )
            continue
        if ELSE_RE.match(stripped) or "//?} else" in stripped:
            if stack:
                stack[-1]["has_else"] = True
                stack[-1]["state"] = "else"
            else:
                stray_count += 1
            continue
        if CLOSE_RE.match(stripped) or "//?}" in stripped:
            if stack:
                top = stack.pop()
                blocks.append(
                    RawBlock(
                        file=rel_path,
                        line=top["line"],
                        end_line=lineno,
                        cond=top["cond"],
                        then_lines=top["then"],
                        else_lines=top["else"],
                        has_else=top["has_else"],
                        nested=top["nested"],
                    )
                )
            else:
                stray_count += 1
            continue
        if stack:
            stack[-1][stack[-1]["state"]].append(raw)
        elif PLATFORM_REF_RE.search(raw):
            outside_platform_refs.append((lineno, stripped))

    class_name = path.stem
    superclass = ""
    match = CLASS_DECL_RE.search(text)
    if match:
        class_name = match.group(1)
        superclass = (match.group(2) or "").split(".")[-1]

    is_be = (
        class_name.endswith("BlockEntity")
        or superclass.endswith("BlockEntity")
        or superclass == "BlockEntity"
    )
    is_block = (
        (not is_be)
        and (
            "/block/" in rel_path
            or superclass.endswith("Block")
            or superclass == "Block"
        )
    )
    is_item = (
        "/item/" in rel_path
        or superclass.endswith("Item")
        or class_name.endswith("Item")
        or class_name.endswith("BlockItem")
    )

    return FileContext(
        rel_path=rel_path,
        text=text,
        class_name=class_name,
        superclass=superclass,
        is_block_entity=is_be,
        is_block_class=is_block,
        is_item_class=is_item,
        blocks=blocks,
        outside_platform_refs=outside_platform_refs,
        unclosed_count=len(stack),
        stray_count=stray_count,
    )


def find_split_duplicate_lines(blocks: list[RawBlock]) -> set[int]:
    """Return starting lines of one-sided blocks forming split-duplicate pairs."""
    one_sided: list[tuple[RawBlock, bool, str]] = []
    for blk in blocks:
        th = extract_code_lines(blk.then_lines)
        el = extract_code_lines(blk.else_lines)
        if bool(th) != bool(el):
            then_empty = not th
            body = norm_compact(el if then_empty else th)
            if body:
                one_sided.append((blk, then_empty, body))

    dup_lines: set[int] = set()
    for i in range(len(one_sided)):
        b1, empty1, body1 = one_sided[i]
        for j in range(i + 1, len(one_sided)):
            b2, empty2, body2 = one_sided[j]
            if b1.cond == b2.cond and empty1 != empty2 and body1 == body2:
                dup_lines.add(b1.line)
                dup_lines.add(b2.line)
    return dup_lines


def classify_file(ctx: FileContext) -> tuple[list[ClassifiedBlock], list[dict[str, Any]]]:
    split_dup_lines = find_split_duplicate_lines(ctx.blocks)
    file_violations: list[dict[str, Any]] = []

    if ctx.unclosed_count or ctx.stray_count:
        file_violations.append(
            {
                "kind": "UNCLOSED_MARKER",
                "file": ctx.rel_path,
                "line": 0,
                "detail": f"unclosed={ctx.unclosed_count}, stray={ctx.stray_count}",
            }
        )
    for lineno, snippet in ctx.outside_platform_refs:
        file_violations.append(
            {
                "kind": "SHARED_PLATFORM_REF",
                "file": ctx.rel_path,
                "line": lineno,
                "detail": snippet[:160],
            }
        )

    # Pre-scan file body signatures for FOLLOW_BODY import attribution.
    has_be_sig = False
    has_non_be_holder_use = False
    has_wave1b_body = False
    has_hover_body = False
    for blk in ctx.blocks:
        both_code = "\n".join(
            extract_code_lines(blk.then_lines) + extract_code_lines(blk.else_lines)
        )
        is_imp = is_import_only(extract_code_lines(blk.then_lines)) or is_import_only(
            extract_code_lines(blk.else_lines)
        )
        if BE_LIFECYCLE_RE.search(both_code) or (
            re.search(r"\bload\s*\(\s*CompoundTag\b", both_code) and ctx.is_block_entity
        ):
            if ctx.class_name not in BE_UTILITY_CLASSES and (ctx.is_block_entity or ctx.is_block_class):
                has_be_sig = True
        elif not is_imp and "HolderLookup" in both_code:
            has_non_be_holder_use = True
        if BLOCK_INTERACTION_RE.search(both_code) or (
            MAP_CODEC_RE.search(both_code)
            and not is_imp
            and "/recipe/" not in ctx.rel_path
            and "/worldgen/" not in ctx.rel_path
        ):
            has_wave1b_body = True
        if "appendHoverText" in both_code:
            has_hover_body = True

    classified: list[ClassifiedBlock] = []
    for blk in ctx.blocks:
        th_code = extract_code_lines(blk.then_lines)
        el_code = extract_code_lines(blk.else_lines)
        th_str = "\n".join(th_code)
        el_str = "\n".join(el_code)
        both_str = f"{th_str}\n{el_str}".strip()
        raw_both = "".join(blk.then_lines) + "".join(blk.else_lines)

        if not th_code and not el_code:
            shape = "empty-both"
        elif th_code and el_code:
            shape = "two-sided"
        else:
            shape = "one-sided"

        blk_violations: list[str] = []
        if PLATFORM_REF_RE.search(raw_both):
            blk_violations.append("SHARED_PLATFORM_REF")
            file_violations.append(
                {
                    "kind": "SHARED_PLATFORM_REF",
                    "file": ctx.rel_path,
                    "line": blk.line,
                    "detail": norm_ws(th_code or el_code)[:160],
                }
            )
        if not th_code and not el_code:
            blk_violations.append("EMPTY_BLOCK")
            file_violations.append(
                {
                    "kind": "EMPTY_BLOCK",
                    "file": ctx.rel_path,
                    "line": blk.line,
                    "detail": "empty or comment-only //? block",
                }
            )
        elif blk.has_else and th_code and el_code and norm_ws(th_code) == norm_ws(el_code):
            blk_violations.append("IDENTICAL_BRANCH")
            file_violations.append(
                {
                    "kind": "IDENTICAL_BRANCH",
                    "file": ctx.rel_path,
                    "line": blk.line,
                    "detail": norm_ws(th_code)[:160],
                }
            )
        if blk.line in split_dup_lines:
            blk_violations.append("SPLIT_DUPLICATE")
            file_violations.append(
                {
                    "kind": "SPLIT_DUPLICATE",
                    "file": ctx.rel_path,
                    "line": blk.line,
                    "detail": norm_ws(th_code or el_code)[:160],
                }
            )

        imp_only = (bool(th_code) and is_import_only(th_code)) and (
            not el_code or is_import_only(el_code)
        ) or (
            not th_code and bool(el_code) and is_import_only(el_code)
        )

        origin, disposition, wave, reason = _classify_block(
            ctx=ctx,
            blk_violations=blk_violations,
            th_code=th_code,
            el_code=el_code,
            th_str=th_str,
            el_str=el_str,
            both_str=both_str,
            imp_only=imp_only,
            has_be_sig=has_be_sig,
            has_non_be_holder_use=has_non_be_holder_use,
            has_wave1b_body=has_wave1b_body,
            has_hover_body=has_hover_body,
        )

        classified.append(
            ClassifiedBlock(
                file=ctx.rel_path,
                line=blk.line,
                end_line=blk.end_line,
                cond=blk.cond,
                shape=shape,
                origin=origin,
                disposition=disposition,
                wave=wave,
                reason=reason,
                violations=blk_violations,
                then_preview=norm_ws(th_code)[:120],
                else_preview=norm_ws(el_code)[:120],
            )
        )

    return classified, file_violations


def _classify_block(
    ctx: FileContext,
    blk_violations: list[str],
    th_code: list[str],
    el_code: list[str],
    th_str: str,
    el_str: str,
    both_str: str,
    imp_only: bool,
    has_be_sig: bool,
    has_non_be_holder_use: bool,
    has_wave1b_body: bool,
    has_hover_body: bool,
) -> tuple[str, str, str, str]:
    # 1. Hard violations & zero-tolerance noise -> Wave 2a
    if "SHARED_PLATFORM_REF" in blk_violations:
        return "SELF_DEFECT", "REMOVE", "Wave 2a", "shared-platform-ref"
    if blk_violations:
        return "SELF_DEFECT", "REMOVE", "Wave 2a", "dead-or-identical-branch"
    if (not th_code or not el_code) and is_pure_annotation(th_code or el_code):
        return "SELF_DEFECT", "REMOVE", "Wave 2a", "one-sided-annotation"
    if ctx.class_name == "GTWorldFluidBlock" and "fluid" in both_str and not BE_LIFECYCLE_RE.search(both_str):
        return "SELF_DEFECT", "REMOVE", "Wave 2a", "redundant-fluid-accessor"

    # 2. Plan A Permanent Floor -> FLOOR_KEEP / Floor
    if "/mixin/" in ctx.rel_path:
        origin = "LOADER" if (CAP_RE.search(both_str) or LOADER_API_RE.search(both_str)) else "MC_VERSION"
        return origin, "FLOOR_KEEP", "Floor", "mixin-target-or-cap"
    if "/compat/" in ctx.rel_path:
        return "COMPAT", "FLOOR_KEEP", "Floor", "third-party-compat"
    if CAP_RE.search(both_str):
        return "LOADER", "FLOOR_KEEP", "Floor", "capability-api"
    if is_pure_loader_package_rename(th_str, el_str):
        reason = "pure-loader-1to1-import" if imp_only else "pure-loader-1to1-fqcn"
        return "LOADER", "FLOOR_KEEP", "Floor", reason

    # 3. Wave 1 Thin Base Classes -> BASE_CLASS / FACADE (non-BE caller) / FOLLOW_BODY
    if ctx.class_name not in BE_UTILITY_CLASSES and (
        BE_LIFECYCLE_RE.search(both_str)
        or (re.search(r"\bload\s*\(\s*CompoundTag\b", both_str) and ctx.is_block_entity)
    ):
        if ctx.is_block_entity:
            return "MC_VERSION", "BASE_CLASS", "Wave 1a", "be-lifecycle-signature"
        if ctx.is_block_class:
            return "MC_VERSION", "FACADE", "Wave 1a", "be-caller-facade"
        return "MC_VERSION", "FACADE", "Wave 3", "vanilla-be-caller-facade"
    if imp_only and has_be_sig and not has_non_be_holder_use and (
        "HolderLookup" in both_str or "ClientboundBlockEntityDataPacket" in both_str
    ):
        return "MC_VERSION", "FOLLOW_BODY", "Wave 1a", "be-follow-body-import"

    if BLOCK_INTERACTION_RE.search(both_str):
        return "MC_VERSION", "BASE_CLASS", "Wave 1b", "block-interaction-adapter"
    if (
        MAP_CODEC_RE.search(both_str)
        and not imp_only
        and "/recipe/" not in ctx.rel_path
        and "/worldgen/" not in ctx.rel_path
    ):
        return "MC_VERSION", "BASE_CLASS", "Wave 1b", "block-mapcodec-stub"
    if imp_only and has_wave1b_body and (
        "ItemInteractionResult" in both_str or "MapCodec" in both_str
    ):
        return "MC_VERSION", "FOLLOW_BODY", "Wave 1b", "block-follow-body-import"

    if "appendHoverText" in both_str:
        if ctx.is_block_class and not ctx.is_item_class:
            return "MC_VERSION", "BASE_CLASS", "Wave 1b", "block-append-hover-text"
        return "MC_VERSION", "BASE_CLASS", "Wave 1c", "item-append-hover-text"
    if imp_only and has_hover_body and (
        "TooltipFlag" in both_str
        or "TooltipContext" in both_str
        or "net.minecraft.world.level.Level;" in both_str
        or "net.minecraft.world.item.Item;" in both_str
    ):
        wave = "Wave 1b" if (ctx.is_block_class and not ctx.is_item_class) else "Wave 1c"
        return "MC_VERSION", "FOLLOW_BODY", wave, "hover-follow-body-import"

    # 4. Remaining SELF_DEFECT -> Wave 2b / Wave 2c
    has_provider_arg_diff = (
        ("HolderLookup.Provider" in th_str) != ("HolderLookup.Provider" in el_str)
    ) or (
        ("registryAccess()" in th_str) != ("registryAccess()" in el_str)
    )
    if (
        has_provider_arg_diff
        and not VANILLA_PROVIDER_METHOD_RE.search(both_str)
        and not imp_only
    ):
        return "SELF_DEFECT", "REMOVE", "Wave 2b", "internal-provider-signature"

    if imp_only and "import gregtech." in both_str:
        return "SELF_DEFECT", "REMOVE", "Wave 2c", "gt-internal-import-split"

    if (
        re.search(r"\bclass\s+\w+.*?\b(?:extends|implements)\b", both_str)
        and not re.search(r"\b(?:FallingBlock|SandBlock|TransparentBlock|GlassBlock)\b", both_str)
    ):
        return "SELF_DEFECT", "REMOVE", "Wave 2c", "class-declaration-split"

    if not LOADER_API_RE.search(both_str) and not MC_API_RE.search(both_str):
        return "SELF_DEFECT", "REMOVE", "Wave 2c", "internal-logic-or-structure-split"

    # 5. Wave 3 (Facades) vs Wave 4 (Client unification)
    origin = "LOADER" if LOADER_API_RE.search(both_str) else "MC_VERSION"
    disposition = "FOLLOW_BODY" if imp_only else "FACADE"
    is_client = "/client/" in ctx.rel_path or bool(CLIENT_API_RE.search(both_str))
    wave = "Wave 4" if is_client else "Wave 3"
    reason = "client-unification" if is_client else "api-facade-or-follow"
    return origin, disposition, wave, reason


def scan_node_subclasses(repo_root: Path, wave1a_classes: set[str]) -> dict[str, Any]:
    """Inspect node-specific Java sources for BlockEntity hierarchy and Wave 1a subclasses."""
    node_be_classes: list[dict[str, str]] = []
    wave1a_node_subclasses: list[dict[str, str]] = []

    for node_name, rel_root in NODE_ROOTS:
        root_dir = repo_root / rel_root
        if not root_dir.exists():
            continue
        for dirpath, _, filenames in os.walk(root_dir):
            for fname in sorted(filenames):
                if not fname.endswith(".java"):
                    continue
                fpath = Path(dirpath) / fname
                text = fpath.read_text(encoding="utf-8", errors="replace")
                match = CLASS_DECL_RE.search(text)
                if not match:
                    continue
                cls_name = match.group(1)
                super_name = (match.group(2) or "").split(".")[-1]
                rel_file = fpath.relative_to(repo_root).as_posix()
                if super_name in wave1a_classes:
                    wave1a_node_subclasses.append(
                        {
                            "node": node_name,
                            "file": rel_file,
                            "class": cls_name,
                            "extends": super_name,
                        }
                    )
                if "BlockEntity" in cls_name or "BlockEntity" in super_name:
                    node_be_classes.append(
                        {
                            "node": node_name,
                            "file": rel_file,
                            "class": cls_name,
                            "extends": super_name,
                        }
                    )

    return {
        "wave1a_node_subclass_count": len(wave1a_node_subclasses),
        "wave1a_node_subclasses": wave1a_node_subclasses,
        "node_block_entity_classes": node_be_classes,
    }


def run_audit(repo_root: Path) -> dict[str, Any]:
    java_files: list[Path] = []
    for rel_root in SCAN_ROOTS:
        root_dir = repo_root / rel_root
        if not root_dir.exists():
            raise SystemExit(f"Scan root does not exist: {root_dir}")
        for dirpath, _, filenames in os.walk(root_dir):
            for fname in sorted(filenames):
                if fname.endswith(".java"):
                    java_files.append(Path(dirpath) / fname)
    java_files.sort()

    file_contexts: list[FileContext] = []
    all_blocks: list[ClassifiedBlock] = []
    all_violations: list[dict[str, Any]] = []
    unclosed_stack_total = 0
    stray_marker_total = 0

    for fpath in java_files:
        ctx = parse_java_file(fpath, repo_root)
        file_contexts.append(ctx)
        unclosed_stack_total += ctx.unclosed_count
        stray_marker_total += ctx.stray_count
        if ctx.blocks or ctx.outside_platform_refs or ctx.unclosed_count or ctx.stray_count:
            cblocks, fviolations = classify_file(ctx)
            all_blocks.extend(cblocks)
            all_violations.extend(fviolations)

    # Closure metrics
    files_with_markers = sum(1 for c in file_contexts if c.blocks)
    two_sided_blocks = sum(1 for b in all_blocks if b.shape == "two-sided")
    one_sided_blocks = sum(1 for b in all_blocks if b.shape == "one-sided")
    empty_both_blocks = sum(1 for b in all_blocks if b.shape == "empty-both")
    nested_blocks = sum(1 for c in file_contexts for b in c.blocks if b.nested)

    # 2D Matrix
    matrix: dict[str, dict[str, int]] = {
        o: {d: 0 for d in DISPOSITIONS} for o in ORIGINS
    }
    for b in all_blocks:
        matrix[b.origin][b.disposition] += 1

    actionable_total = sum(
        matrix[o][d] for o in ORIGINS for d in DISPOSITIONS if d != "FLOOR_KEEP"
    )
    floor_total = sum(matrix[o]["FLOOR_KEEP"] for o in ORIGINS)

    # Wave breakdown
    wave_files: dict[str, Counter[str]] = {w: Counter() for w in WAVES}
    wave_blocks: dict[str, list[ClassifiedBlock]] = {w: [] for w in WAVES}
    wave_dispositions: dict[str, dict[str, int]] = {
        w: {d: 0 for d in DISPOSITIONS} for w in WAVES
    }
    for b in all_blocks:
        wave_files[b.wave][b.file] += 1
        wave_blocks[b.wave].append(b)
        wave_dispositions[b.wave][b.disposition] += 1

    # Wave 1a BlockEntity superclass distribution & node subclass check
    ctx_by_file = {c.rel_path: c for c in file_contexts}
    wave1a_be_superclasses: Counter[str] = Counter()
    wave1a_non_be_callers: list[str] = []
    wave1a_be_classes: set[str] = set()
    for fname in sorted(wave_files["Wave 1a"]):
        ctx = ctx_by_file[fname]
        if ctx.is_block_entity:
            wave1a_be_classes.add(ctx.class_name)
            wave1a_be_superclasses[ctx.superclass or "<none>"] += 1
        else:
            wave1a_non_be_callers.append(fname)

    node_check = scan_node_subclasses(repo_root, wave1a_be_classes)

    return {
        "meta": {
            "scan_roots": list(SCAN_ROOTS),
            "vcs_active_node": "1.20.1-forge",
            "nodes": ["1.20.1-forge", "1.21.1-neoforge"],
        },
        "closure": {
            "total_java_files": len(java_files),
            "files_with_markers": files_with_markers,
            "total_blocks": len(all_blocks),
            "two_sided_blocks": two_sided_blocks,
            "one_sided_blocks": one_sided_blocks,
            "empty_both_blocks": empty_both_blocks,
            "nested_blocks": nested_blocks,
            "unclosed_stack": unclosed_stack_total,
            "stray_markers": stray_marker_total,
        },
        "matrix": {
            "cells": matrix,
            "actionable_backlog": actionable_total,
            "floor_keep": floor_total,
            "grand_total": len(all_blocks),
        },
        "waves": {
            w: {
                "files": len(wave_files[w]),
                "blocks": len(wave_blocks[w]),
                "by_disposition": wave_dispositions[w],
                "file_counts": dict(sorted(wave_files[w].items())),
            }
            for w in WAVES
        },
        "wave_1a_block_entity_hierarchy": {
            "block_entity_file_count": len(wave1a_be_classes),
            "non_be_caller_count": len(wave1a_non_be_callers),
            "non_be_callers": wave1a_non_be_callers,
            "superclass_distribution": dict(wave1a_be_superclasses.most_common()),
            "node_subclass_check": node_check,
        },
        "violations": {
            "total": len(all_violations),
            "by_kind": dict(Counter(v["kind"] for v in all_violations)),
            "items": all_violations,
        },
        "blocks": [
            {
                "file": b.file,
                "line": b.line,
                "end_line": b.end_line,
                "cond": b.cond,
                "shape": b.shape,
                "origin": b.origin,
                "disposition": b.disposition,
                "wave": b.wave,
                "reason": b.reason,
                "violations": b.violations,
            }
            for b in all_blocks
        ],
    }


def print_summary(report: dict[str, Any]) -> None:
    cl = report["closure"]
    print("=== Closure Check ===")
    print(f"Java files scanned : {cl['total_java_files']}")
    print(f"Files with //?     : {cl['files_with_markers']}")
    print(f"Total //? blocks   : {cl['total_blocks']}")
    print(f"  Two-sided        : {cl['two_sided_blocks']}")
    print(f"  One-sided        : {cl['one_sided_blocks']}")
    print(f"  Empty-both       : {cl['empty_both_blocks']}")
    print(f"Nested blocks      : {cl['nested_blocks']}")
    print(f"Unclosed stack     : {cl['unclosed_stack']}")
    print(f"Stray markers      : {cl['stray_markers']}")
    print()

    print("=== 2D Matrix (Origin x Disposition) ===")
    header = (
        f"{'Origin':<14}"
        + "".join(f"{d:>12}" for d in DISPOSITIONS)
        + f"{'Total':>10}{'Actionable':>12}"
    )
    print(header)
    print("-" * len(header))
    cells = report["matrix"]["cells"]
    for o in ORIGINS:
        row_total = sum(cells[o].values())
        row_actionable = row_total - cells[o]["FLOOR_KEEP"]
        print(
            f"{o:<14}"
            + "".join(f"{cells[o][d]:>12}" for d in DISPOSITIONS)
            + f"{row_total:>10}{row_actionable:>12}"
        )
    print("-" * len(header))
    col_totals = {d: sum(cells[o][d] for o in ORIGINS) for d in DISPOSITIONS}
    print(
        f"{'TOTAL':<14}"
        + "".join(f"{col_totals[d]:>12}" for d in DISPOSITIONS)
        + f"{report['matrix']['grand_total']:>10}"
        + f"{report['matrix']['actionable_backlog']:>12}"
    )
    print()

    print("=== Wave Summary ===")
    w_header = (
        f"{'Wave':<12}{'Files':>7}"
        + "".join(f"{d:>12}" for d in DISPOSITIONS)
        + f"{'Total':>9}"
    )
    print(w_header)
    print("-" * len(w_header))
    for w in WAVES:
        info = report["waves"][w]
        by_d = info["by_disposition"]
        print(
            f"{w:<12}{info['files']:>7}"
            + "".join(f"{by_d[d]:>12}" for d in DISPOSITIONS)
            + f"{info['blocks']:>9}"
        )
    print("-" * len(w_header))
    print(
        f"{'Actionable':<12}{'-':>7}"
        + "".join(
            f"{(col_totals[d] if d != 'FLOOR_KEEP' else 0):>12}"
            for d in DISPOSITIONS
        )
        + f"{report['matrix']['actionable_backlog']:>9}"
    )
    print(
        f"{'Floor':<12}{'-':>7}"
        + "".join(
            f"{(col_totals[d] if d == 'FLOOR_KEEP' else 0):>12}"
            for d in DISPOSITIONS
        )
        + f"{report['matrix']['floor_keep']:>9}"
    )
    print(
        f"{'Total':<12}{'-':>7}"
        + "".join(f"{col_totals[d]:>12}" for d in DISPOSITIONS)
        + f"{report['matrix']['grand_total']:>9}"
    )
    print()


def print_violations(report: dict[str, Any]) -> None:
    viol = report["violations"]
    print(f"=== Hard Violations (total={viol['total']}) ===")
    for kind, count in sorted(viol["by_kind"].items()):
        print(f"  {kind}: {count}")
    for item in viol["items"]:
        loc = f"{item['file']}:{item['line']}" if item["line"] else item["file"]
        print(f"  [{item['kind']}] {loc}  {item['detail']}")


def print_wave(report: dict[str, Any], wave: str) -> None:
    info = report["waves"][wave]
    print(f"=== {wave} (files={info['files']}, blocks={info['blocks']}) ===")
    for fname, count in sorted(
        info["file_counts"].items(), key=lambda kv: (-kv[1], kv[0])
    ):
        print(f"  {count:>4}  {fname}")

    if wave == "Wave 1a":
        hier = report["wave_1a_block_entity_hierarchy"]
        by_d = info["by_disposition"]
        print()
        print("--- Wave 1a BlockEntity Superclass Distribution ---")
        print(
            f"  Disposition breakdown: BASE_CLASS={by_d['BASE_CLASS']}, "
            f"FACADE(non-BE callers)={by_d['FACADE']}, "
            f"FOLLOW_BODY={by_d['FOLLOW_BODY']}"
        )
        print(f"  BlockEntity classes : {hier['block_entity_file_count']}")
        print(f"  Non-BE Block callers: {hier['non_be_caller_count']}")
        for sc, cnt in hier["superclass_distribution"].items():
            print(f"    extends {sc:<32} {cnt}")
        nchk = hier["node_subclass_check"]
        print(
            f"  Node subclasses extending Wave 1a BEs: {nchk['wave1a_node_subclass_count']}"
        )


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Two-dimensional attribution audit for Stonecutter //? blocks."
    )
    parser.add_argument(
        "repo_root",
        nargs="?",
        default=".",
        help="Repository root directory (default: current directory)",
    )
    parser.add_argument(
        "--wave",
        choices=WAVES,
        help="Export file list and block count for the specified wave.",
    )
    parser.add_argument(
        "--violations",
        action="store_true",
        help="List hard violations (shared platform refs, empty blocks, identical/split branches).",
    )
    parser.add_argument(
        "--json",
        metavar="PATH",
        help="Write full structured JSON audit report to PATH.",
    )
    parser.add_argument(
        "--check-hard-violations",
        action="store_true",
        help="Exit non-zero if any hard violations or unclosed markers exist.",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    repo_root = Path(args.repo_root).resolve()
    report = run_audit(repo_root)

    if args.json:
        out_path = Path(args.json)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

    if args.wave:
        print_wave(report, args.wave)
    elif args.violations:
        print_violations(report)
    else:
        print_summary(report)
        print_violations(report)

    if args.check_hard_violations:
        has_violations = (
            report["violations"]["total"] > 0
            or report["closure"]["unclosed_stack"] > 0
            or report["closure"]["stray_markers"] > 0
        )
        return 1 if has_violations else 0
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
