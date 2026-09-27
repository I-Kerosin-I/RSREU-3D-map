# Development Process

Follow this process before modifying architecture or introducing cross-layer dependencies.

## Pre-Change Checklist

1. **Inspect existing implementation** — read the current code
2. **Explain the proposed change** — describe what, why, and what breaks
3. **Implement the smallest coherent change** — one logical step at a time
4. **Run relevant build/tests** — verify it compiles and works
5. **Report what changed and what was verified**

## Architectural Constraints

### Do not introduce dependencies without checking the existing stack.

Before adding any library, verify that:
- libGDX or gdx-gltf does not already provide the functionality
- the need cannot be satisfied with plain Java

### Layer dependency rules

```
desktop  →  core  →  application  →  domain
android  ↗                          ↑
```

- `domain` must not import anything outside JDK
- `application` must not import libGDX, Android, or rendering
- `core` must not import LWJGL or Android APIs directly
- `desktop` and `android` must not contain domain or rendering logic

### No embedded room metadata in rendering code

Room names, IDs, navigation data, and building structure must come from `manifest.json`, not from Java constants or enums.

## Creating a New Module

1. Create `modules/<name>/pom.xml` inheriting from parent
2. Add `<module>modules/<name></module>` to root `pom.xml`
3. Create `src/main/java/...` structure
4. Add dependencies in `pom.xml`
5. Verify `mvn compile` succeeds

## Working on core/render

Rules for rendering code:
- Renderer receives domain-converted `Render*` objects
- Renderer does not call use cases or repositories
- Renderer does not parse JSON or load assets
- All conversions (domain → render model) happen in `core/controller` or dedicated converter classes

## Working on domain

Rules for domain code:
- Plain Java only (no external imports)
- No mutable static state
- Records for immutable value objects
- Entities are final classes with final fields

## Git Commit Messages

Use conventional format:

```
<type>(<scope>): <description>

[optional body]
```

Types: `feat`, `fix`, `refactor`, `docs`, `chore`, `architecture`

Examples:
```
architecture(domain): add NavigationGraph entity
refactor(core): extract RoomHighlighter from BuildingRenderer
docs: add MANIFEST_SCHEMA.md
chore: bump libGDX version to 1.12.1
```

## First Implementation Priority

Implement in this order:

1. **Parent POM** — modules, dependency management, Java 17
2. **domain** — entities and value objects
3. **application** — use cases and repository interface
4. **core** — MapGame, content loader, basic renderer, desktop launcher
5. **One GLB + manifest** — single building, one floor, few rooms
6. **Camera + selection** — click to highlight a room
7. **Navigation** — graph, pathfinding, route rendering

Steps 1–4 produce a compile-ready skeleton with no game logic. Step 5–7 deliver a working vertical slice.

## Adding Android (when ready)

1. Create `android/` as separate Gradle project (not Maven module)
2. Add `core` as a dependency via Maven artifact or project reference
3. Copy `assets/` into `android/app/src/main/assets/`
4. Implement `AndroidLauncher` and `MainActivity`
5. Verify touch input and lifecycle on a physical device

Do not copy domain or application code to Android — reuse the Maven modules.