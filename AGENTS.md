# University 3D Map

## Project

Interactive 3D navigation map of RSREU buildings.

## Stack

- Java
- libGDX
- Maven
- gdx-gltf

## Architecture

- `core/` contains platform-independent code.
- `desktop/` contains desktop launcher code.
- Rendering must not contain university domain logic.
- University data must not depend on libGDX classes.

## 3D assets

- Final runtime format: glTF 2.0 / GLB.
- Models are authored in Fusion 360 and prepared in Blender.
- Model nodes representing rooms must have stable names.
- Do not embed room metadata directly into Java rendering code.
- Models don’t have textures, only a mesh.

## Development

Before modifying architecture:
1. Inspect existing implementation.
2. Explain the proposed change.
3. Implement the smallest coherent change.
4. Run the relevant build/tests.
5. Report what changed and what was verified.

## Important

Do not introduce dependencies without checking whether
the existing stack already provides the required functionality.