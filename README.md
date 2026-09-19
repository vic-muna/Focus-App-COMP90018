# Focus-App-COMP90018 (data-layer branch)

## What changed in this pass

**HistoryViewModel.kt (owner task)** - now loads session history for
real: `FocusRepositoryProvider.get(context).getSessionHistory()` inside
an `AndroidViewModel`, exposed as `sessions: StateFlow<List<FocusSession>>`
(plus `isLoading: StateFlow<Boolean>`). `HistoryScreen.kt` was left
untouched on purpose - binding the UI to this StateFlow is a separate
task. `viewModel()` already used there resolves `AndroidViewModel`
automatically, no extra factory wiring needed.

**Single focus zone (was: multiple zones).** `FocusRepository`,
`FocusRepositoryImpl`, `LocalDataSource`, and `RoomLocalDataSource` now
expose `getFocusZone(): FocusZone?` / `saveFocusZone(zone)` instead of a
`List<FocusZone>`. `FocusZoneDao`/`FocusZoneEntity` were **not** made
single-row - the DAO still supports many rows - `RoomLocalDataSource
.saveFocusZone()` just calls `deleteAll()` before every `upsert()`, so
exactly one row ever exists. `FakeLocalDataSource` and the test suites
(`FocusRepositoryImplTest`, `FocusZoneDaoTest`) were updated to match.

**Brought in line with the team's current merged UI/sensor layer**
(`SensorDataSource.kt`, `MapScreen.kt`, `NavGraph.kt`, `Destinations.kt`,
`AppsScreen.kt`, `AddAppGroupScreen.kt`, `EditAppGroupScreen.kt`) - these
7 files are now byte-for-byte what the team's merged branch has, per
team decision to defer to those over this branch's older versions.
Pulling those in required also bringing across everything they depend on
that this branch didn't have yet: `data/accessibility/`,
`data/apps/InstalledAppsProvider.kt`, `data/sensor/LocationDataSource.kt`
+ `GeofenceDataSource.kt` + `GeofenceBroadcastReceiver.kt`,
`ui/screens/home/` (Home screen + its sheets), `ui/screens/party/`,
`ui/screens/session/`, `ui/common/LocationPermission.kt`, and syncing
`domain/model/FocusZone.kt` (added `containsLocation`),
`domain/model/FocusSession.kt` (added `groupId`),
`domain/usecase/EvaluateFocusTriggerUseCase.kt` (now returns
`FocusTriggerResult` instead of `Boolean`), and `ui/theme/WireframeColors.kt`
(one color tweak) - all copied verbatim from the team's merged branch so
there's nothing left to reconcile later.

**Gradle / Manifest / resources**, so the above actually builds:
- `app/build.gradle.kts` + `gradle/libs.versions.toml`: added
  `com.google.android.gms:play-services-location` (needed by the new GPS/
  geofencing code; this branch's own Room+Firebase dependencies were
  already complete and needed no changes).
- `AndroidManifest.xml`: uncommented the three location permissions,
  added the `<queries>` package-visibility block (needed by
  `InstalledAppsProvider.getLaunchableApps()`), the
  `FocusAccessibilityService` `<service>` declaration, and the
  `GeofenceBroadcastReceiver` `<receiver>` declaration. Switched
  `android:theme` to this project's own `@style/Theme.FocusApp` (already
  defined in `res/values/themes.xml`, just wasn't referenced anywhere).
  INTERNET/ACCESS_NETWORK_STATE (needed for Firebase) were already
  active and untouched.
- `res/values/strings.xml`: added `accessibility_service_description`
  (required by `res/xml/accessibility_service_config.xml`, which was
  also added).

## Left alone / left for later

- `MainActivity.kt` - already correct (calls `FocusAppNavGraph()`,
  matching the copied `NavGraph.kt`); not touched.
- `RemoteDataSource.kt` / `FirebaseRemoteDataSource.kt` - untouched, no
  conflict with anything above.
- `FocusSessionEntity`/Room does not yet persist a session's `groupId` -
  `FocusSession.groupId` exists on the domain model now (needed to
  compile `FocusSessionScreen.kt`), but `RoomLocalDataSource` doesn't
  round-trip it yet. Not a compile issue, just a known gap.
- `domain/mock/MockFocusData.kt` / `ui/screens/debug/DataLayerPreviewScreen.kt`
  doc comments were reworded where they referred to the old "multi-zone
  goal" - the sample data itself (3 illustrative zones) was left as-is
  since this screen is disconnected from the real repository either way.

## Verification done

No Android SDK is available in the environment this was prepared in, so
this could not be run through an actual Gradle build. Instead, every
`com.example.focusapp.*` import across all 69 Kotlin files (main + test +
androidTest) was checked against the project's actual declared classes/
functions/objects, checked for duplicate top-level declarations, and
checked for brace/paren balance. All clear. Please run a Gradle sync in
Android Studio as a final check - if anything comes up, send the error
and it can be fixed directly.
