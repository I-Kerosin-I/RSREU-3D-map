# UI Architecture and Requirements

## 1. Purpose

This document defines the baseline UI architecture for the university 3D
map application.

The application is a production-oriented interactive 3D map of the
university. The 3D scene is the primary workspace, while the graphical
user interface is rendered as an overlay above the scene.

This document establishes architectural rules and interaction
principles. It intentionally does **not** define the final visual
design, exact positions, colors, typography, icons, or complete set of
controls. Those details will be designed later.

The UI must be designed from the beginning with both desktop and Android
use in mind.

------------------------------------------------------------------------

## 2. Technology

The application uses:

-   Java
-   libGDX
-   Scene2D UI for the application interface
-   libGDX 3D rendering for the map
-   Gradle
-   Desktop and Android targets

Scene2D is the primary UI technology.

Do not introduce JavaFX, Swing, Android XML layouts, Compose, or another
independent UI framework for the main application interface.

The desktop and Android versions should use the same UI implementation
wherever reasonably possible.

Platform-specific code should be limited to platform integration and
input differences.

------------------------------------------------------------------------

## 3. General UI Concept

The 3D map is the main content of the application.

UI controls are layered on top of the 3D viewport.

Conceptually:

    Application
    |
    +-- 3D world
    |     +-- buildings
    |     +-- floors
    |     +-- rooms
    |     +-- highlights
    |     +-- navigation routes
    |
    +-- UI overlay
          +-- search
          +-- navigation controls
          +-- view mode controls
          +-- contextual information
          +-- menus
          +-- dialogs

The UI must not replace the 3D viewport with a traditional collection of
screens unless a particular workflow genuinely requires a separate
screen.

The default experience should always keep the map visually central.

------------------------------------------------------------------------

## 4. UI Layering

The application should conceptually divide the UI into several layers.

### 4.1 Persistent overlay

Contains controls that are useful during normal map interaction.

Examples:

-   search field;
-   main menu button;
-   view mode indicator;
-   basic navigation controls.

These controls should occupy as little of the viewport as reasonably
possible.

### 4.2 Contextual overlay

Appears when the user selects or focuses an object.

Examples:

-   room information panel;
-   building information;
-   department information;
-   selected floor information.

Contextual UI must appear only when relevant.

### 4.3 Temporary overlay

Used for short-lived interaction states.

Examples:

-   search results;
-   confirmation dialogs;
-   transient notifications;
-   loading indicators;
-   error messages.

### 4.4 Modal UI

Modal windows should be used sparingly.

They are appropriate when the user must explicitly complete or dismiss
an operation.

The main map should remain the normal interaction context.

------------------------------------------------------------------------

## 5. UI and 3D Scene Separation

UI code must not directly contain university domain logic or low-level
3D rendering logic.

Avoid code where a button directly manipulates arbitrary model nodes,
camera matrices, meshes, or rendering resources.

For example, avoid architecture equivalent to:

    button.addListener(() -> {
        scene.getNode("Room_312").setVisible(true);
        camera.position.set(...);
        infoLabel.setText(...);
    });

Instead, UI actions should communicate with application-level
controllers/services/state.

Conceptually:

    User input
        |
        v
    UI widget
        |
        v
    Application action / controller
        |
        +----> university state
        |
        +----> camera/navigation state
        |
        +----> rendering state
        |
        +----> contextual UI state

The exact implementation may evolve, but the separation of
responsibilities must be preserved.

------------------------------------------------------------------------

## 6. Application State

UI should reflect application state rather than becoming the source of
truth.

Relevant application state may include:

-   selected building;
-   selected floor;
-   selected room;
-   selected department;
-   current visualization mode;
-   current search query;
-   search results;
-   current navigation route;
-   camera target;
-   active dialogs/panels;
-   loading state;
-   error state.

A UI widget should display or modify this state through defined
application-level interfaces.

Do not store important university data only inside Scene2D widgets.

For example, a selected room must remain known to the application even
if the room information panel is closed.

------------------------------------------------------------------------

## 7. Main User Workflows

The UI must support the following workflows.

### 7.1 Search

The user should be able to search for:

-   room number;
-   room name;
-   department;
-   building;
-   other useful university entities as the data model grows.

Search results should be presented as selectable items.

Selecting a result should be able to:

1.  identify the corresponding domain object;
2.  focus the 3D camera on the object;
3.  highlight the object;
4.  open relevant information.

Search must not require the user to manually navigate through the
building.

------------------------------------------------------------------------

### 7.2 Selecting a room in 3D

The user should be able to select a visible room directly in the 3D
scene.

The resulting state should be reflected in the UI.

The UI may display:

-   room number;
-   room name;
-   building;
-   floor;
-   department;
-   purpose;
-   other metadata available in the domain model.

The exact information displayed will be specified later.

------------------------------------------------------------------------

### 7.3 Selecting a department

The user should be able to choose a department through the UI.

The application should then enter a department visualization state.

The 3D renderer is responsible for visually representing this state.

The UI is responsible for selecting the department and displaying
relevant information.

The UI must not implement the transparency algorithm itself.

------------------------------------------------------------------------

### 7.4 Selecting a floor

The user should be able to select a floor.

The selected floor should be reflected in the application state and in
the 3D visualization.

The UI should provide an appropriate way to understand which floor is
currently active.

The exact floor selector design will be decided later.

------------------------------------------------------------------------

### 7.5 Navigation

The future application may support navigation from a starting point to a
selected room.

The UI should therefore leave room for:

-   route information;
-   route activation/deactivation;
-   destination information;
-   navigation status.

Navigation UI should remain separate from the implementation of the
navigation algorithm.

------------------------------------------------------------------------

## 8. 3D Viewport Interaction

The UI must coexist with direct 3D interaction.

Desktop input is expected to support:

-   mouse;
-   keyboard where useful;
-   mouse wheel.

Android input is expected to support:

-   touch;
-   multi-touch gestures;
-   virtual keyboard where text entry is required.

The UI must correctly consume input events when the user interacts with
a UI widget.

A click/touch on a UI element must not accidentally select or manipulate
a 3D object underneath it.

Likewise, input intended for the 3D viewport must not accidentally
activate UI controls.

Input routing must therefore distinguish between:

    UI interaction
    3D interaction

The exact gesture scheme will be specified later.

------------------------------------------------------------------------

## 9. Desktop and Android

The UI architecture must support different screen sizes and input
methods.

Do not design the desktop interface around a fixed resolution.

Do not assume that a large desktop layout can simply be scaled down for
a phone.

The same application state and UI components should be reusable, but
layouts may adapt to the available screen size.

The application should support at least two broad layout classes:

### Desktop / large screen

The interface may use:

-   side panels;
-   wider search controls;
-   multiple controls visible simultaneously.

### Mobile / small screen

The interface should favor:

-   compact controls;
-   expandable panels;
-   bottom sheets or similar contextual surfaces where appropriate;
-   larger touch targets;
-   reduced persistent UI.

The final responsive strategy will be designed later.

------------------------------------------------------------------------

## 10. UI Components

The application is expected to use reusable UI components rather than
constructing every screen from unrelated Scene2D widgets.

Potential components include:

-   SearchBar;
-   SearchResults;
-   RoomInfoPanel;
-   BuildingInfoPanel;
-   DepartmentSelector;
-   FloorSelector;
-   ViewModeSelector;
-   NavigationPanel;
-   MainMenu;
-   LoadingOverlay;
-   ErrorOverlay;
-   Notification/Toast component.

These names describe responsibilities, not mandatory final class names.

Components should have narrow responsibilities.

For example:

`SearchBar` should deal with entering and submitting search queries, not
perform database searches itself.

`RoomInfoPanel` should display room information, not determine how rooms
are stored.

`FloorSelector` should request a floor change, not directly manipulate
3D nodes.

------------------------------------------------------------------------

## 11. UI Data Flow

A preferred conceptual flow is:

    Domain data
        |
        v
    Application state
        |
        v
    UI presentation
        |
        v
    User action
        |
        v
    Application command/action
        |
        v
    Application state / 3D state

The exact state-management implementation may be selected during
architecture work.

Avoid creating a large global UI manager that contains all application
logic.

Avoid tightly coupling individual widgets to each other.

------------------------------------------------------------------------

## 12. Rendering Order

The application should conceptually render:

1.  3D scene;
2.  3D overlays/highlights that belong to the world;
3.  Scene2D UI overlay;
4.  temporary/modal UI when necessary.

World-space elements such as room labels may belong to the 3D rendering
system rather than the normal screen-space UI.

Screen-space controls such as search fields and panels belong to
Scene2D.

The distinction between world-space and screen-space UI must be
maintained.

------------------------------------------------------------------------

## 13. World-Space Labels

The project will likely require labels for rooms and other objects
inside the 3D scene.

These labels should be treated differently from ordinary UI controls.

A room number displayed above a room is a world-space visualization
element.

A search field at the top of the screen is a screen-space UI element.

Do not implement all labels as ordinary fixed-position Scene2D widgets.

The final label rendering solution will be selected during graphics
architecture work.

Potential concerns include:

-   camera distance;
-   label scaling;
-   occlusion;
-   visibility;
-   overlapping labels;
-   performance;
-   selected-object highlighting.

------------------------------------------------------------------------

## 14. Styling

The final visual style has not yet been fully specified.

The current project direction is:

-   minimalistic;
-   technical/architectural;
-   white background;
-   blue-toned 3D visualization;
-   no dependence on photographic textures;
-   visually lightweight UI;
-   UI should not compete with the 3D model.

Do not hard-code a large collection of colors, dimensions, or typography
values throughout individual widgets.

Centralize visual constants and styles so the visual design can be
changed later.

The project should be able to evolve from a prototype visual style to
the final course-project design without rewriting UI logic.

------------------------------------------------------------------------

## 15. UI Theme

Use libGDX Scene2D skin/style mechanisms rather than setting visual
properties individually on every widget.

UI styles should be centralized.

The exact theme should be designed later.

At minimum, the architecture should allow:

-   global font changes;
-   global color changes;
-   button style changes;
-   text field style changes;
-   panel style changes;
-   selection/highlight style changes;
-   scaling adjustments.

Do not scatter style construction throughout application logic.

------------------------------------------------------------------------

## 16. Localization

The first version may use Russian text only.

However, user-visible strings should not be deeply embedded into
application logic.

Prefer a centralized mechanism for user-facing text so that future
localization can be introduced without rewriting UI components.

At minimum, avoid constructing important UI text from hard-coded strings
in unrelated classes.

------------------------------------------------------------------------

## 17. Accessibility and Usability

The application is primarily a visual 3D tool, but basic usability
requirements still apply.

UI controls should:

-   have sufficiently large touch targets on mobile;
-   remain readable against the 3D scene;
-   provide clear selected/active states;
-   avoid relying solely on color to communicate state;
-   avoid covering important parts of the 3D scene unnecessarily.

Exact accessibility requirements will be refined later.

------------------------------------------------------------------------

## 18. Performance

The UI must not unnecessarily interfere with 3D rendering performance.

Avoid:

-   rebuilding large widget hierarchies every frame;
-   allocating objects every frame;
-   constantly recreating UI styles;
-   unnecessary text/layout recalculation;
-   polling the 3D scene from every widget.

Scene2D should be updated and rendered through the normal libGDX
lifecycle.

Expensive operations such as search, route calculation, or loading large
assets should not block rendering or input unnecessarily.

------------------------------------------------------------------------

## 19. Resource Management

UI resources such as:

-   fonts;
-   textures;
-   skins;
-   icons;
-   drawables;

must have clear ownership and lifecycle.

Do not load the same large UI resource independently for every widget.

Resources should be loaded through an appropriate centralized
resource-management mechanism once the application architecture is
established.

The exact asset/resource manager is intentionally not specified by this
document.

------------------------------------------------------------------------

## 20. Responsive Layout

UI layout must be based on available viewport dimensions rather than
hard-coded pixel positions.

Prefer Scene2D layout containers such as:

-   Table;
-   Stack;
-   VerticalGroup;
-   HorizontalGroup;
-   ScrollPane;
-   Window;

where appropriate.

Avoid manually positioning every widget with absolute coordinates unless
there is a specific reason.

The UI should remain functional when:

-   the desktop window is resized;
-   the device orientation changes;
-   the Android screen has a different aspect ratio;
-   UI panels are opened or closed.

------------------------------------------------------------------------

## 21. Future UI Features

The architecture should leave room for future functionality without
requiring a rewrite.

Potential future features include:

-   route navigation;
-   favorites;
-   recently viewed rooms;
-   building selection;
-   department browsing;
-   room schedules;
-   accessibility information;
-   current location;
-   offline map data;
-   multiple university campuses;
-   localization.

These features are not part of the initial implementation unless
explicitly added to the project roadmap.

Do not implement speculative functionality merely because the
architecture could support it.

------------------------------------------------------------------------

## 22. Architectural Restrictions

The following rules apply to UI development.

### Do

-   use Scene2D for the main application UI;
-   keep UI separate from domain logic;
-   keep UI separate from low-level 3D rendering;
-   centralize styles;
-   make layouts responsive;
-   reuse components;
-   support desktop and Android;
-   keep user-visible strings organized;
-   make input routing explicit;
-   keep world-space and screen-space elements conceptually separate.

### Do not

-   introduce another UI framework without an explicit architectural
    decision;
-   put university data directly into UI widgets;
-   make UI widgets directly manipulate arbitrary 3D nodes;
-   hard-code screen coordinates throughout the application;
-   create a single monolithic UI class;
-   rebuild the entire UI every frame;
-   assume desktop resolution;
-   design the desktop UI in a way that cannot reasonably adapt to
    mobile;
-   implement detailed visual design before the interaction model is
    agreed upon.

------------------------------------------------------------------------

## 23. Current Scope

This document defines the **foundation** for the UI architecture.

It does not yet define:

-   exact screen layouts;
-   final color palette;
-   typography;
-   icons;
-   exact button placement;
-   final mobile navigation;
-   animation design;
-   complete information architecture;
-   final search UX;
-   final floor selector;
-   final department selector.

Those decisions should be made after the first functional 3D prototype
exists.

The current priority is to establish an architecture that can support
those decisions without requiring a major rewrite.

------------------------------------------------------------------------

## 24. Initial Implementation Direction

The first production-oriented implementation should establish:

1.  a libGDX application with a 3D viewport;
2.  a Scene2D `Stage` for screen-space UI;
3.  a centralized UI/root container;
4.  centralized UI styling;
5.  explicit input routing between UI and 3D;
6.  a basic application state mechanism;
7.  reusable UI component boundaries;
8.  desktop-compatible layout;
9.  architecture that can later be reused by Android.

The initial implementation should remain visually simple.

The goal is not to finish the UI immediately.

The goal is to establish a stable foundation on which the final UI can
be designed and implemented incrementally.
