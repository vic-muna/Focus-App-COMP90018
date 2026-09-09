# Focus - Project Skeleton

This is a bare-bones, **compile-and-run skeleton** for the Focus app described in
the project plan. It sets up the screens, navigation, and the
UI / Domain / Data architecture from the plan, but contains **no real
feature logic yet** - every function that should eventually do something
real is marked with a `// TODO: to be implemented later` comment.

## What actually works right now

- The app launches and shows 3 tabs: **Apps, Map, Settings** - this
  matches a teammate's wireframes and replaces the earlier
  Focus Mode/History/Rewards tab bar (see "Open decisions" below).
- **Apps** tab: lists app groups (one hard-coded "App Group 1" to start).
  - Tapping **Edit** opens a schedule-editing screen ("Block during" /
    "Daily opens" / "Open duration" text fields).
  - Tapping **+** opens a search + app-picker screen with toggle circles.
  - Both screens' **Save** button just navigates back - nothing is
    actually created or persisted yet.
- **Map** tab: lists saved focus locations (one hard-coded "Location 1").
  The pencil-edit button and "+" button are visible but intentionally do
  nothing yet - no wireframe exists yet for what tapping them should show.
- **Settings** tab: empty placeholder (no wireframe provided for it yet).
- Nothing reads sensors, nothing restricts apps, nothing saves data
  permanently, nothing talks to the network. All of that is stubbed out.

## How to open and run it

1. Open Android Studio (a recent stable version - anything that supports
   AGP 8.6 / Kotlin 2.0 works, e.g. Android Studio Ladybug or newer).
2. **File -> Open**, and select the `FocusApp` folder (the one containing
   `settings.gradle.kts`).
3. This project does **not** include the Gradle wrapper jar. When you open
   it, Android Studio will detect this and offer to fix it automatically
   (usually a banner/prompt like "Gradle wrapper is missing, create one?").
   Accept that, or use **File -> Sync Project with Gradle Files** and let
   Android Studio use its own bundled Gradle. This requires an internet
   connection the first time (as does any new Android project).
4. Once Gradle sync finishes, create/select an emulator
   (**Tools -> Device Manager**) with **API 26+**, then press **Run ▶**.
5. You should see the 4-tab app launch in the emulator with no crashes.

## Project structure

```
app/src/main/java/com/example/focusapp/
├── MainActivity.kt              # Single Activity, hosts the Compose UI tree
├── ui/
│   ├── theme/
│   │   └── WireframeColors.kt     # Plain color constants matching the teammate's greyscale mockups
│   ├── navigation/                 # Bottom nav bar (Apps/Map/Settings) + NavHost wiring
│   └── screens/
│       ├── apps/                    # Apps tab: AppsScreen (list), EditAppGroupScreen, AddAppGroupScreen
│       ├── map/                      # Map tab: MapScreen (location list)
│       ├── settings/                  # Settings tab (placeholder - no wireframe yet)
│       ├── focus/                      # NOT wired into navigation - see "Open decisions" below
│       ├── history/                     # NOT wired into navigation - see "Open decisions" below
│       └── rewards/                      # NOT wired into navigation - see "Open decisions" below
├── domain/
│   ├── model/                     # Plain data classes (FocusZone, AppGroup, FocusSession, RewardProgress)
│   ├── usecase/                    # Business-rule stubs (EvaluateFocusTriggerUseCase, CalculateFocusRewardUseCase)
│   └── repository/                  # FocusRepository interface
└── data/
    ├── local/                      # LocalDataSource - in-memory stub, replace with Room later
    ├── remote/                      # RemoteDataSource - empty interface, REST-vs-Firebase decision still open
    ├── sensor/                       # SensorDataSource - GPS/accelerometer/gyroscope/mic stubs
    └── repository/                   # FocusRepositoryImpl - wires local+remote together
```

This mirrors the plan's own "UI / Domain / Data" architecture description,
so each team member should be able to find where their assigned feature
belongs:

| Plan task | Where to start |
|---|---|
| GPS + Geofencing + Accelerometer integration | `data/sensor/SensorDataSource.kt` |
| Cloud REST API integration | `data/remote/RemoteDataSource.kt` (**resolve REST-vs-Firebase first**) |
| Local data layer (Room) | `data/local/LocalDataSource.kt` |
| Firebase real-time sync (Study Party) | `data/remote/RemoteDataSource.kt` (**resolve REST-vs-Firebase first**) |
| AccessibilityService (App Restriction / Screen Usage) | new class under `data/` - not stubbed yet, since its exact scope was ambiguous in the plan (see review notes) |
| Focus Points algorithm / rule engine | `domain/usecase/EvaluateFocusTriggerUseCase.kt`, `domain/usecase/CalculateFocusRewardUseCase.kt` |
| Reward/Progress UI | `ui/screens/rewards/RewardsScreen.kt` |
| Figma -> Compose screens | `ui/screens/**` |

## App Groups (real, persisted app grouping)

Creating a group ("Group1: Facebook + Instagram", "Group2: Chrome +
Facebook + YouTube", etc.) is now a genuinely working, persisted feature,
not a placeholder:

```
app/src/main/java/com/example/focusapp/data/local/LocalDataSource.kt        # real SharedPreferences+JSON persistence for AppGroup
app/src/main/java/com/example/focusapp/data/repository/FocusRepositoryProvider.kt  # minimal singleton until real DI exists
app/src/main/java/com/example/focusapp/data/apps/InstalledAppsProvider.kt    # + getAppIcon() for single-package icon lookup
```

- **Apps tab → "+"** opens a real installed-app picker (with a working
  search filter) plus a group-name field. Selecting apps and tapping Save
  genuinely creates an `AppGroup` and persists it via
  `LocalDataSource`'s SharedPreferences storage - it survives an app restart.
- **Apps tab → "Edit"** on a group now loads that exact group's real data
  (by id) and shows its real app icons. The 3 schedule fields
  ("Block during" etc.) are still local-only placeholders, unchanged from
  before.
- **AccessibilityService Test Panel → "Select Group to Block"** lists your
  saved groups and blocks every app in the chosen one at once, via a new
  `AccessibilityBridge.addRestrictedPackages(...)` - this is the
  "select the grouped apps you want to block when you start Focus"
  behaviour, built on the same restricted-package list the single-app
  picker already used.

There's no dependency-injection framework in this project yet, so
`FocusRepositoryProvider` is a small hand-written singleton standing in
for one - delete it once Hilt/Koin gets added, since that's exactly what
those frameworks generate for you automatically.

## AccessibilityService prototype (app detection + restriction + usage duration)

A working `FocusAccessibilityService` now exists, plus test panels at the
top of the **Apps** tab so you can see it work without logcat:

```
app/src/main/java/com/example/focusapp/data/accessibility/
├── AccessibilityBridge.kt          # singleton StateFlow "mailbox" between the service and the UI
└── FocusAccessibilityService.kt    # the actual AccessibilityService
app/src/main/java/com/example/focusapp/data/apps/
└── InstalledAppsProvider.kt        # queries PackageManager for launchable apps + app labels
app/src/main/res/xml/accessibility_service_config.xml   # which events the service listens for
```

**To try it on a device/emulator:**
1. Run the app, open the **Apps** tab, tap **"Open Accessibility Settings"**
   in the test panel, and manually turn Focus's accessibility service on
   (Android requires this to be a deliberate user action - an app can't
   enable it for itself).
2. Come back to the app - "Service status" should flip to Connected.
3. Switch to any other app and back - "Current foreground app" updates
   live, and the second panel ("App Usage Duration") starts a running
   timer for whichever app you just switched to.
4. Tap **"Select App to Block"** to pick a real installed app (with its
   real icon and name) from a scrollable list, instead of typing a raw
   package name.
5. Open that app again - the service should immediately bounce you back
   to the home screen, and a line should appear under "Recent block
   events".

This is a real, functioning prototype, not a placeholder - it genuinely
detects foreground-app changes, genuinely enforces blocking (via
`GLOBAL_ACTION_HOME`, since apps can't be force-closed without root), and
genuinely tracks per-app usage duration since the app process started, all
from the same AccessibilityService event stream (no `UsageStatsManager` or
extra "Usage access" permission needed - we tried that approach first, but
removed it since it only offers historical polling, not the ongoing timer
this project actually wants; see git history / prior conversation for why).
What it does NOT do yet: read `AppGroup`'s schedule fields, group multiple
packages together, persist usage data across app restarts, or show a
nicer "blocked" screen instead of silently returning to home - see the
TODOs in `FocusAccessibilityService.kt` and `AccessibilityBridge.kt`.

## Known open decisions (carried over from the plan review)

These are intentionally **not** resolved in this skeleton - decide as a
team, then implement:
1. **REST API vs. Firebase** for `RemoteDataSource`.
2. Whether Screen Usage Detection uses `AccessibilityService` or
   `UsageStatsManager` + touch frequency.
3. Whether noisy-environment handling plays ambient masking audio or
   mutes/reduces notification sounds (or both).
4. The reward formula (streak/completion-based vs. leniency for easily
   distracted users) and the final reward visual (tree/mountain vs.
   progress rings).
5. **Where Focus Mode / History / Rewards fit into the new Apps/Map/Settings
   navigation** - a teammate's wireframes only covered the Apps and Map
   flows, so those 3 earlier screens got unplugged from the bottom nav
   rather than deleted (`ui/screens/focus`, `ui/screens/history`,
   `ui/screens/rewards`). Options include: a 4th tab, a screen reached
   automatically when a restriction rule triggers (matching the "Runtime
   mode" half of the original flow diagram, which isn't really a
   user-navigated tab), or something else - needs a team decision.
6. **What tapping "+" or the pencil icon on the Map screen should open** -
   no wireframe exists yet for a location-add/edit flow (presumably an
   embedded map), so both are visible but intentionally inert
   (`ui/screens/map/MapScreen.kt`).
7. **What the circular toggle next to the search bar on the Add-App-Group
   screen is for** - rendered per the mockup, but its purpose (select-all?
   a filter?) wasn't specified (`ui/screens/apps/AddAppGroupScreen.kt`).

## Coding conventions used here

- Every class, function, and non-obvious block has a comment explaining
  *why* it exists, not just what it does, since multiple people will be
  editing this codebase.
- `// TODO: to be implemented later` marks every spot intentionally left
  unfinished - search for that exact phrase to find all remaining work.
