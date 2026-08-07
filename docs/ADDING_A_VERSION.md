# Adding a Minecraft Version

All version-specific data lives in one place: `versions.json` (a list of version
entries). Everything else (templates, build scripts, flags) is shared.

## 1. Add an entry to `versions.json`

```jsonc
{
  "mc": "1.21.12",                       // version key; used as the build/generated dir name
  "loaders": ["fabric", "neoforge"],     // loader modules to generate (forge only exists for 1.20.1)
  "java": 25,                            // toolchain level for the generated build
  "gradle": "8.11.1",                    // Gradle version; ONLY informational for the root build,
                                         // but the standalone wrapper uses it (see below)
  "pack_format": 86,                     // data pack format (usually "+ the previous" via pack_format_range)
  "pack_format_range": true,
  "loom": "1.17.17",                     // optional; defaults to loom_version in gradle.properties
  "loom_plugin": "net.fabricmc.fabric-loom", // optional; defaults to dev.architectury.loom
  "standalone": true,                    // optional. true = self-contained project with its own
                                         // Gradle wrapper, built in build/generated/<mc>/ directly.
                                         // REQUIRED for any version that needs a different Gradle
                                         // major than the root wrapper (8.11.1).
  "deps": {
    "neoforge": "26.1.2.94",             // neoforge only
    "fabric_loader": "0.19.3",           // fabric only
    "fabric_api": "0.155.2+26.1.2",      // fabric only
    "modmenu": "18.0.0"                  // optional; makes the config screen reachable on fabric
  },
  "flags": { /* see below */ },
  "renames": { "ResourceLocation": "Identifier" } // optional word-level renames applied to all generated text
}
```

## 2. Rules to respect

- **Gradle major:** ForgeGradle 6 (used by 1.20.1) hard-rejects Gradle 9+. So any version
  sharing the root build must keep `"gradle": "8.11.1"` (the wrapper). Versions that need
  Gradle 9+ (NeoForge 26.x needs ≥9.1, Fabric 26.x needs ≥9.4) must set
  `"standalone": true` — the generator then emits `template/standalone` into
  `build/generated/<mc>/` (own `settings.gradle`, `build.gradle`, `gradle.properties`,
  wrapper scripts + jar) and the wrapper `gradle-wrapper.properties` is tokenised with
  `{{gradle}}`.
- **Unobfuscated releases (26.1+):** use `loom_plugin: "net.fabricmc.fabric-loom"`,
  `"loom": "<version>"`, and rely on `loom_no_remap` (set automatically for standalone
  entries) which switches fabric to plain `implementation` deps, no mappings, no
  shadow/remap, and wires `:common` via the `apiElements` variant.

## 3. Iterating on compile errors

1. `python scripts/generate.py <mc>` then build (root build for ≤1.21.11, standalone
   `.\gradlew.bat build` in `build\generated\<mc>\` for 26.x).
2. When a 26.x API symbol is missing, verify the real signature with `javap` on the
   version's deobf jar before writing the branch:
   `C:\Users\asus\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\<mc>\minecraft-merged-deobf-<mc>.jar`
   (the jar only exists after the first fabric compile).
3. Add a boolean flag (e.g. `"new_api_thing": true`) to this entry's `flags` and branch
   the template:
   ```java
   //? if new_api_thing {
   //  new-API code
   //?} else {
   //  old-API code
   //?}
   ```
   Flags are false when absent, so older versions keep the existing code path. For a
   flag that only the NEW version must NOT get, leave it off the old entries and put it
   only on the new one (e.g. `eventbus_bus_attribute` is true only on 1.21.1).
4. Regenerate and rebuild until green. `docs/VERSION_MATRIX.md` (auto-written on every
   generate) will list the new flag.

## 4. Check the flag legend

`python scripts/generate.py --docs` rewrites `docs/VERSION_MATRIX.md`; a flag listed as
`— never` means no entry currently sets it — safe to remove from templates.
