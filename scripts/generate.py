#!/usr/bin/env python3
"""Generate per-(mc_version, loader) source trees from /template.

Usage:
    python scripts/generate.py --all          regenerate everything from versions.json
    python scripts/generate.py 1.21.1         regenerate all loaders for one MC version
    python scripts/generate.py 1.21.1 fabric  regenerate one target
    python scripts/generate.py --docs         rewrite docs/VERSION_MATRIX.md only
"""
import json
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEMPLATE = ROOT / "template"
OUT = ROOT / "build" / "generated"

BINARY_SUFFIXES = {".png", ".ico", ".jar", ".zip", ".class", ".gz"}
PROCESS_SUFFIXES = {".java", ".json", ".toml", ".gradle", ".txt", ".properties", ".mixins.json", ".mcmeta", ".md"}
SKIP_DIR_PARTS = {"build", ".gradle", "run", "run-data"}

FLAG_RE = r"!?[\w:]+"
IF_RE = re.compile(r"//\?\s*if\s+(" + FLAG_RE + r")\s*\{")
ELIF_RE = re.compile(r"//\?\s*\}\s*else\s+if\s+(" + FLAG_RE + r")\s*\{")
ELSE_RE = re.compile(r"//\?\s*\}\s*else\s*\{")
END_RE = re.compile(r"//\?\s*\}\s*$")
RENAME_RE = re.compile(r"//\?\s*rename\s+(\S+)\s+(\S+)")
TOKEN_RE = re.compile(r"\{\{(\w+)\}\}")


def eval_flag(flag: str, ctx: dict) -> bool:
    negated = flag.startswith("!")
    if negated:
        flag = flag[1:]
    if ":" in flag:
        key, val = flag.split(":", 1)
        result = ctx.get(key) == val
    else:
        result = bool(ctx.get(flag, False))
    return not result if negated else result


def process_text(text: str, ctx: dict, tokens: dict) -> str:
    out, stack, branch_taken = [], [True], [True]
    renames = []
    for line in text.splitlines():
        m = IF_RE.search(line)
        if m:
            cond = eval_flag(m.group(1), ctx)
            stack.append(stack[-1] and cond)
            branch_taken.append(cond)
            continue
        m = ELIF_RE.search(line)
        if m:
            cond = eval_flag(m.group(1), ctx) and not branch_taken[-1]
            stack[-1] = stack[-2] and cond
            branch_taken[-1] = branch_taken[-1] or cond
            continue
        if ELSE_RE.search(line):
            stack[-1] = stack[-2] and not branch_taken[-1]
            continue
        if END_RE.search(line):
            stack.pop()
            branch_taken.pop()
            continue
        m = RENAME_RE.search(line)
        if m and stack[-1]:
            renames.append((m.group(1), m.group(2)))
            continue
        if stack[-1]:
            out.append(line)
    if len(stack) != 1 or len(branch_taken) != 1:
        raise ValueError("unbalanced //? conditional block")
    result = "\n".join(out)
    if tokens:
        result = TOKEN_RE.sub(lambda m: str(tokens.get(m.group(1), m.group(0))), result)
    for old, new in renames:
        result = re.sub(r"\b" + re.escape(old) + r"\b", new, result)
    return result


def minecraft_range(mc: str) -> str:
    parts = mc.split(".")
    parts[-1] = str(int(parts[-1]) + 1)
    return f"[{mc},{'.'.join(parts)})"


def loader_range(loader_version: str) -> str:
    first, second = loader_version.split(".")[:2]
    return f"[{first}.{second},{int(first) + 1})"


def root_properties() -> dict:
    props = {}
    props_file = ROOT / "gradle.properties"
    if props_file.exists():
        for line in props_file.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line and not line.startswith(("#", "!")) and "=" in line:
                key, _, value = line.partition("=")
                props[key.strip()] = value.strip()
    return props


def load_manifest() -> list:
    manifest_path = ROOT / "versions.json"
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise SystemExit(f"Cannot read {manifest_path}: {exc}") from exc

    if not isinstance(manifest, list) or not manifest:
        raise SystemExit(f"{manifest_path} must contain a non-empty JSON array")

    seen = set()
    for entry in manifest:
        if not isinstance(entry, dict):
            raise SystemExit("Every versions.json entry must be an object")
        mc = entry.get("mc")
        loaders = entry.get("loaders")
        if not isinstance(mc, str) or not mc:
            raise SystemExit("Every versions.json entry needs a non-empty string 'mc'")
        if mc in seen:
            raise SystemExit(f"Duplicate Minecraft version in versions.json: {mc}")
        seen.add(mc)
        if not isinstance(loaders, list) or not loaders:
            raise SystemExit(f"{mc}: 'loaders' must be a non-empty list")
        unknown = set(loaders) - {"fabric", "forge", "neoforge"}
        if unknown:
            raise SystemExit(f"{mc}: unknown loader(s): {', '.join(sorted(unknown))}")
        for required in ("java", "gradle", "pack_format", "deps", "flags"):
            if required not in entry:
                raise SystemExit(f"{mc}: missing required manifest key '{required}'")
    return manifest


def build_tokens(mc: str, loader: str, entry: dict) -> dict:
    deps = entry.get("deps", {})
    props = root_properties()
    standalone = bool(entry.get("standalone"))
    tokens = {
        "mc": mc,
        "slug": mc.replace(".", "_"),
        "java": entry["java"],
        "pack_format": entry["pack_format"],
        "pack_min_format": entry.get("pack_min_format", entry["pack_format"]),
        "gradle": entry.get("gradle", ""),
        "loom": entry.get("loom", props.get("loom_version", "1.10.455")),
        "loom_plugin": entry.get("loom_plugin", "dev.architectury.loom"),
        "common_project": ":common" if standalone else ":" + mc.replace(".", "_") + "-common",
        "minecraft_range": minecraft_range(mc),
        "loader_range": loader_range(deps["neoforge"]) if deps.get("neoforge") else "",
        "shadow_version": props.get("shadow_version", ""),
        "moddev_version": props.get("moddev_version", ""),
        "mod_version": props.get("mod_version", "1.0.3"),
    }
    for key, value in deps.items():
        tokens[key] = value
    return tokens


def build_context(mc: str, loader: str, entry: dict) -> dict:
    ctx = dict(entry.get("flags", {}))
    # `loader:fabric` conditionals are evaluated as ctx["loader"] == "fabric".
    ctx["loader"] = loader
    ctx["has_modmenu"] = bool(entry.get("deps", {}).get("modmenu")) and loader == "fabric"
    ctx["loom_no_remap"] = bool(entry.get("standalone"))
    ctx["pack_format_range"] = bool(entry.get("pack_format_range"))
    return ctx


def generate_file(src_file: Path, dst_file: Path, mc: str, loader: str, entry: dict):
    if src_file.suffix in BINARY_SUFFIXES:
        shutil.copy2(src_file, dst_file)
        return
    if src_file.suffix in PROCESS_SUFFIXES or src_file.suffix == "":
        text = src_file.read_text(encoding="utf-8", errors="replace")
        processed = process_text(text, build_context(mc, loader, entry), build_tokens(mc, loader, entry))
        if processed.strip():
            dst_file.write_text(processed, encoding="utf-8")
        else:
            # A conditional can intentionally remove a file for one target.  Remove
            # any old rendered copy as well; otherwise stale source survives a
            # partial regeneration and causes confusing cross-version build errors.
            dst_file.unlink(missing_ok=True)
    else:
        shutil.copy2(src_file, dst_file)


def clear_rendered_module(module_dir: Path):
    """Remove rendered source while keeping Gradle's build cache and jars."""
    if not module_dir.exists():
        return
    for child in module_dir.iterdir():
        if child.name == "build":
            continue
        if child.is_dir() and not child.is_symlink():
            shutil.rmtree(child, ignore_errors=True)
        else:
            child.unlink(missing_ok=True)


def clear_rendered_version(version_dir: Path):
    """Clear generated source/configuration without deleting existing build output."""
    if not version_dir.exists():
        return
    module_names = {"common", "fabric", "forge", "neoforge"}
    for child in version_dir.iterdir():
        if child.name == "build":
            continue
        if child.name in module_names and child.is_dir():
            clear_rendered_module(child)
        elif child.is_dir() and not child.is_symlink():
            shutil.rmtree(child, ignore_errors=True)
        else:
            child.unlink(missing_ok=True)


def generate_target(
    mc: str,
    loader: str,
    entry: dict,
    *,
    clean: bool = True,
    include_common: bool = True,
):
    if clean:
        # A target is a complete render, not an overlay. Removing its two module
        # directories prevents deleted or renamed template files from surviving
        # a later generation run.
        modules = ("common", loader) if include_common else (loader,)
        for module in modules:
            clear_rendered_module(OUT / mc / module)
    modules = ("common", loader) if include_common else (loader,)
    for module in modules:
        src_dir = TEMPLATE / module
        if not src_dir.exists():
            continue
        for src_file in src_dir.rglob("*"):
            if src_file.is_dir():
                continue
            if any(part in SKIP_DIR_PARTS for part in src_file.parts):
                continue
            rel = src_file.relative_to(src_dir)
            dst_file = OUT / mc / module / rel
            dst_file.parent.mkdir(parents=True, exist_ok=True)
            generate_file(src_file, dst_file, mc, loader, entry)


def run_renames(mc: str, loader: str, entry: dict):
    """Apply entry-level renames (e.g. ResourceLocation -> Identifier at 26.x) to generated text files."""
    renames = entry.get("renames", {})
    if not renames:
        return
    for module in ("common", loader):
        module_dir = OUT / mc / module
        if not module_dir.exists():
            continue
        for dst_file in module_dir.rglob("*"):
            relative = dst_file.relative_to(module_dir)
            if (dst_file.is_file()
                    and dst_file.suffix in PROCESS_SUFFIXES
                    and not any(part in SKIP_DIR_PARTS for part in relative.parts)):
                text = dst_file.read_text(encoding="utf-8", errors="replace")
                for old, new in renames.items():
                    text = re.sub(r"\b" + re.escape(old) + r"\b", new, text)
                dst_file.write_text(text, encoding="utf-8")


def generate_standalone(mc: str, entry: dict):
    """Emit a self-contained Gradle project (own settings, wrapper) for versions that need
    a different Gradle major version than the root build (e.g. 26.x needs Gradle 9.4+)."""
    src_dir = TEMPLATE / "standalone"
    dst_dir = OUT / mc
    for src_file in src_dir.rglob("*"):
        if src_file.is_dir():
            continue
        rel = src_file.relative_to(src_dir)
        dst_file = dst_dir / rel
        dst_file.parent.mkdir(parents=True, exist_ok=True)
        generate_file(src_file, dst_file, mc, "fabric", entry)
    for name in ("gradlew", "gradlew.bat"):
        src = ROOT / name
        if src.exists():
            shutil.copy2(src, dst_dir / name)
    wrapper_jar = ROOT / "gradle" / "wrapper" / "gradle-wrapper.jar"
    if wrapper_jar.exists():
        (dst_dir / "gradle" / "wrapper").mkdir(parents=True, exist_ok=True)
        shutil.copy2(wrapper_jar, dst_dir / "gradle" / "wrapper" / "gradle-wrapper.jar")


def generate_version(mc: str, entry: dict):
    print(f"generating {mc} / {', '.join(entry['loaders'])}")
    # Version-wide generation also owns standalone Gradle scaffolding. Preserve
    # build directories so regeneration does not erase existing jars and caches.
    clear_rendered_version(OUT / mc)
    loaders = entry["loaders"]
    # Render common exactly once.  Rendering it once per loader used to make a
    # future loader-specific common conditional resolve according to whichever
    # loader happened to run last.
    first_loader = loaders[0]
    generate_target(mc, first_loader, entry, clean=False, include_common=True)
    run_renames(mc, first_loader, entry)
    for loader in loaders[1:]:
        generate_target(mc, loader, entry, clean=False, include_common=False)
        run_renames(mc, loader, entry)
    if entry.get("standalone"):
        generate_standalone(mc, entry)
    validate_rendered_version(mc, entry)


def validate_rendered_version(mc: str, entry: dict):
    version_dir = OUT / mc
    expected_modules = {"common", *entry["loaders"]}
    missing = [module for module in expected_modules if not (version_dir / module).is_dir()]
    if entry.get("standalone") and not (version_dir / "gradlew.bat").is_file():
        missing.append("standalone wrapper")
    if missing:
        raise SystemExit(f"{mc}: generation did not produce: {', '.join(sorted(missing))}")

    unresolved = []
    for rendered in version_dir.rglob("*"):
        relative = rendered.relative_to(version_dir)
        if (not rendered.is_file()
                or rendered.suffix in BINARY_SUFFIXES
                or any(part in SKIP_DIR_PARTS for part in relative.parts)):
            continue
        try:
            text = rendered.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if "{{" in text or "}}" in text or re.search(r"^\s*//\?", text, re.MULTILINE):
            unresolved.append(str(rendered.relative_to(ROOT)))
    if unresolved:
        sample = ", ".join(unresolved[:5])
        suffix = " ..." if len(unresolved) > 5 else ""
        raise SystemExit(f"{mc}: unresolved template markers in {sample}{suffix}")


def write_docs(manifest: list):
    docs = ROOT / "docs"
    docs.mkdir(exist_ok=True)
    lines = ["# Version Matrix", "", "| MC | loaders | java | gradle | pack_format |",
             "| --- | --- | --- | --- | --- |"]
    for entry in manifest:
        lines.append(f"| {entry['mc']} | {', '.join(entry['loaders'])} | {entry['java']} | {entry['gradle']} | {entry['pack_format']} |")
    lines += ["", "Flag legend:", ""]
    flag_names = sorted({f for e in manifest for f in e["flags"]})
    for flag in flag_names:
        active = ", ".join(e["mc"] for e in manifest if e["flags"].get(flag))
        lines.append(f"- `{flag}` — {active if active else 'never'}")
    (docs / "VERSION_MATRIX.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("wrote docs/VERSION_MATRIX.md")


def wrapper_gradle_version() -> str:
    props = ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties"
    try:
        content = props.read_text(encoding="utf-8")
        m = re.search(r"gradle-([0-9.]+)-bin\.zip", content)
        return m.group(1) if m else ""
    except FileNotFoundError:
        return ""


def main():
    manifest = load_manifest()
    args = sys.argv[1:]
    if "--docs" in args:
        write_docs(manifest)
        return
    if not args or args[0] == "--all":
        for entry in manifest:
            generate_version(entry["mc"], entry)
    elif len(args) == 1:
        mc = args[0]
        for entry in manifest:
            if entry["mc"] == mc:
                generate_version(mc, entry)
                break
        else:
            sys.exit(f"Unknown Minecraft version {mc!r} in versions.json")
    elif len(args) == 2:
        mc, loader = args
        for entry in manifest:
            if entry["mc"] == mc and loader in entry["loaders"]:
                # Standalone projects include both loaders in one settings file;
                # generating one module alone produces a broken project.
                if entry.get("standalone"):
                    generate_version(mc, entry)
                else:
                    generate_target(mc, loader, entry)
                    run_renames(mc, loader, entry)
                break
        else:
            sys.exit(f"Unknown target {mc!r} {loader!r} in versions.json")
    else:
        sys.exit(__doc__)
    write_docs(manifest)
    wrapper = wrapper_gradle_version()
    for entry in manifest:
        if entry.get("standalone"):
            continue
        if entry.get("gradle") and entry["gradle"] != wrapper:
            print(f"note: {entry['mc']} wants Gradle {entry['gradle']} but the wrapper is {wrapper or '?'}; "
                  f"run: .\\gradlew wrapper --gradle-version {entry['gradle']}")


if __name__ == "__main__":
    main()
