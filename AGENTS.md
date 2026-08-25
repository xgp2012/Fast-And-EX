# AGENTS.md

Minecraft 1.8.9 Chinese PvP client (named "EXClient") built on Tritium-X. Java + Gradle using the custom `chocolate_gradle` plugin (not the standard `application` plugin).

## Required environment
- **JDK 8 only.** CI uses Zulu JDK 8 with Gradle 8.5. Newer JDKs will break the 1.8.9 client.
- `JavaCompile` is forced to UTF-8 (Chinese sources/comments); keep source files UTF-8.

## Source is split into 3 parts — do not skip this
- `src/main/java/cn/howxu/**` — this repo's own code (gui/module/render). Edit here.
- `src/main/java/tritium/**` and `src/main/java/me/imflowow/**` — imported Tritium-X base.
- `src/main/java/net/**` and `src/main/java/javax/**` are **git submodules** (`Chocolate_mcp_src`, `Chocolate_javax_src`). They are deobfuscated Minecraft + javax source from external repos — do **not** edit them; changes live upstream.

The build will not compile without the submodules present:
```bash
git submodule init && git submodule update
# or: git clone --recursive ...
```

## Developer commands (order matters)
```bash
./gradlew check              # 1. pulls dependencies — MUST run first, before anything else
./gradlew getRuntimeResources  # 2. fetches runtime assets (natives included) — needed to run
./gradlew runClient          # runs the client
./gradlew build              # single jar -> build/libs
./gradlew buildArch          # release zip -> build/cache/EXClient.zip
```
`./gradlew getNativesResources` is only needed if natives get corrupted; it is already part of `getRuntimeResources`.

## Things to avoid assuming
- **No tests exist** in this repo (no `src/test`, no JUnit). Do not introduce test commands or expect a test suite to verify changes.
- The `chocolate_gradle` plugin (jitpack `com.github.howxu:chocolate_gradle:v1.4`) provides the `runClient`/`getRuntimeResources`/`buildArch` tasks. Don't replace it with stock Gradle Java tasks expecting the same behavior.
- Release flow is CI-only: push a `v*` tag → `check` then `buildArch` → GitHub release of `build/cache/*.zip` (see `.github/workflows/release.yml`).
