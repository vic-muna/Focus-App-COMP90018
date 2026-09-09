
## Project structure

```
app/src/main/java/com/example/focusapp/
├── MainActivity.kt              # Single Activity, hosts the Compose UI tree
├── ui/
│   ├── theme/
│   │   └── WireframeColors.kt     # Plain color constants 
│   ├── navigation/                 # Bottom nav bar (Apps/Map/Settings) + NavHost wiring
│   └── screens/
│       ├── apps/                    # Apps tab: AppsScreen (list + test panels), EditAppGroupScreen, AddAppGroupScreen
│       ├── map/                      # Map tab: MapScreen (location list)
│       ├── settings/                  # Settings tab (placeholder - no wireframe yet)
│       ├── focus/                      # NOT wired into navigation at this stage
│       ├── history/                     # NOT wired into navigation at this stage
│       └── rewards/                      # NOT wired into navigation at this stage
├── domain/
│   ├── model/                     # Plain data classes (FocusZone, AppGroup, FocusSession, RewardProgress)
│   ├── usecase/                    # Business-rule stubs (EvaluateFocusTriggerUseCase, CalculateFocusRewardUseCase)
│   └── repository/                  # FocusRepository interface
└── data/
    ├── local/                      # LocalDataSource - AppGroup persistence is REAL (SharedPreferences+JSON); FocusZone/FocusSession still in-memory stubs
    ├── remote/                      # RemoteDataSource - empty interface, REST-vs-Firebase decision still open
    ├── repository/                  # FocusRepositoryImpl + FocusRepositoryProvider (manual singleton until real DI exists)
    ├── sensor/                       # SensorDataSource - GPS setup in progress (see below); accelerometer/gyroscope/mic still untouched
    ├── apps/                        # InstalledAppsProvider - real PackageManager queries (launchable apps, labels, icons)
      └── InstalledAppsProvider.kt        # queries PackageManager for launchable apps + app labels
    └── accessibility/                
      ├── AccessibilityBridge.kt          # singleton StateFlow "mailbox" between the service and the UI
      └── FocusAccessibilityService.kt    # the actual AccessibilityService
```
