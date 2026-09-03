# Focus-App-COMP90018

## Project structure

```
app/src/main/java/com/example/focusapp/
├── MainActivity.kt              # Single Activity, hosts the Compose UI tree
├── ui/
│   ├── theme/
│   │   └── WireframeColors.kt     # Plain color constants matching the pre-set greyscale design
│   ├── navigation/                 # Bottom nav bar (Apps/Map/Settings) + NavHost wiring
│   └── screens/
│       ├── apps/                    # Apps tab: AppsScreen (list), EditAppGroupScreen, AddAppGroupScreen
│       ├── map/                      # Map tab: MapScreen (location list)
│       ├── settings/                  # Settings tab (placeholder - no wireframe yet)
│       ├── focus/                      # NOT wired into navigation at this satge
│       ├── history/                     # NOT wired into navigation at this satge
│       └── rewards/                      # NOT wired into navigation at this satge
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
