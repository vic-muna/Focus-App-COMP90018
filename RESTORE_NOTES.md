# Restore Notes - your Room + Firebase data-layer branch, standalone

This is `2nd-temp-UI-for-testing` repackaged as its own standalone,
buildable branch - just the local data layer (Room) + cloud sync
(Firebase) work, none of the other four branches' UI. Everything below
is either exactly what was already in that zip, or something restored to
where it was clearly meant to go (explained one by one).

## What's identical to your original zip (untouched)

Every `.kt`/`.xml` file except the three listed below - all of Room
(`data/local/`), Firebase (`data/remote/`), the repository
(`data/repository/FocusRepositoryImpl.kt`), domain models, and the
Apps/Map/Settings UI + nav graph this branch already had.

## What changed, and why

1. **`data/repository/FocusRepositoryProvider.kt` - moved, not written.**
   This file already existed in your zip, but under
   `app/src/test/java/.../data/repository/` instead of
   `app/src/main/java/.../data/repository/`. Its own doc comment says
   *"Renamed from di/AppContainer.kt... DELETE di/AppContainer.kt when
   applying this - this file replaces it"* - so this was clearly staged,
   ready to move into `main`, and just never got moved (the `di/` folder
   in your zip is empty). Moved it to the correct location and trimmed
   its comment (removed the part assuming teammates' screens like
   `FocusSessionScreen`/`HomeScreenWithSheet` exist, since they're not in
   this standalone branch). **Nothing calls it yet** - no screen in this
   branch reads/writes through `FocusRepository`, so Room/Firebase are
   reachable but still dormant until a screen is wired up. That wiring is
   deliberately not done here (see "what you could still add" from chat).

2. **`AndroidManifest.xml` - `INTERNET` / `ACCESS_NETWORK_STATE`
   uncommented.** Everything else in that permissions block stays
   commented out (location/mic/usage-stats/foreground-service belong to
   other features, not this branch's scope). Without these two, every
   Firebase call in `FirebaseRemoteDataSource` fails outright - this is
   the one live feature in this branch, so it needs them.

3. **Removed 6 stray files under `app/src/test/java/...`** that were
   near-duplicate, slightly older copies of real `src/main` classes
   (`FocusAppDatabase.kt`, `FocusRepositoryImpl.kt`,
   `FocusSessionEntity.kt`, `FocusSession.kt`, `FocusRepository.kt`, plus
   the Provider moved in step 1). Same fully-qualified class name in both
   `src/main` and `src/test` = a duplicate-class compile error the moment
   Gradle builds the test variant. These look like leftover copies from
   mid-edit, not intentional tests (no `@Test` in any of them) - kept only
   the real `ExampleUnitTest.kt` / `ExampleInstrumentedTest.kt`.

4. **`LocalDataSource.kt`'s doc comment** - one line updated to point at
   `FocusRepositoryProvider` instead of the never-created `di/AppContainer.kt`.

## Left out on purpose

- `app/build/`, `.gradle/`, `.idea/`, `.kotlin/` - IDE/build caches, not
  source, ~117 MB combined.
- `local.properties` and `app/google-services.json` - both contained a
  real Firebase API key. `.gitignore` already excludes both, so this just
  follows the convention already in place. **Drop your own
  `local.properties` back in** (with the `FIREBASE_API_KEY=...` line) at
  the project root before building - `app/build.gradle.kts`'s
  `generateGoogleServicesJson` task regenerates `google-services.json`
  from the template + that key automatically on every build.

## Build config (untouched, confirmed self-consistent)

AGP 8.13.2 / Gradle 8.14.5 / compileSdk 35 / Kotlin 2.0.0, wired through
`gradle/libs.versions.toml` (this branch already used a proper version
catalog, unlike the SharedPreferences-based branches). Room 2.8.4 +
Firebase BOM 33.1.2 + KSP 2.0.0-1.0.24.

## What's demonstrably working vs. still just plumbing

**Working if you build and run this:** the Apps/Map/Settings tabs
navigate, `AppsScreen`/`EditAppGroupScreen`/`AddAppGroupScreen` render.

**Written, wired to each other, but not called from any screen yet:**
the entire Room + Firebase stack - `FocusRepositoryProvider.get(context)`
would return a real, working repository if something called it, but
nothing does in this branch. This matches what came up in chat about
what's still left to add.
