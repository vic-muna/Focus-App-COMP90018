# The Focus
 
**COMP90018 – Mobile Computing Systems Programming**
**Assignment 1 – Project Plan**
**Group Number:** T01/01 – 04
 
---
 
## 📋 Table of Contents
 
- [Group Members](#group-members)
- [Project Overview](#project-overview)
- [Planned System and Technical Approach](#planned-system-and-technical-approach)
- [System Architecture](#system-architecture)
- [Data Flow](#data-flow)
- [Key Technical Decisions](#key-technical-decisions)
- [Real-Time, Algorithmic and Integration Design](#real-time-algorithmic-and-integration-design)
- [Privacy & Permissions](#privacy--permissions)
- [Diagrams](#diagrams)
- [UI / UX](#ui--ux)
- [Requirement Coverage and Justification](#requirement-coverage-and-justification)
- [Group Member Tasks](#group-member-tasks)
- [Declaration](#declaration)
---
 
## Group Members
 
| Full Name | Student Number | Email | Primary Responsibility | GitHub Username |
|---|---|---|---|---|
| David Shiau | 1688033 | david.shiau@student.unimelb.edu.au | Back-end | DavidShiau1688033 |
| KAI-JIUN CHAN | 1780997 | kaijiunc@student.unimelb.edu.au | Front-End design | kecvinC |
| YU-HAO LU | 1781474 | ylu13963@student.unimelb.edu.au | Back-End | nicklu356 |
| Victor Munacoha | 1929045 | victor.munacoha@student.unimelb.edu.au | Back-End | vic-muna |
| Jia-Ying Lee | 1790130 | jiaying.lee.1@student.unimelb.edu.au | Front-End design | YAME87 |
 
---
 
## Project Overview
 
**Problem / motivation:**
Students are often distracted by social media and phone notifications when studying. Manual screen control setups can be troublesome to manage and may not adapt well to situations where users need to focus.
 
**Proposed solution:**
Focus is a context-aware mobile app that automatically helps users enter a focused state based on their location, time and phone activity. Physical behaviours, such as shaking or placing the phone face-down, can be detected using mobile sensors to improve the convenience of control. It also tracks focus sessions and provides feedback and rewards to help users reduce digital distractions.
 
**Novelty:**
Unlike conventional timers or app blockers, Focus uses mobile sensors and contextual information to understand when and where the user is likely to be focusing, allowing the app to respond and adapt automatically rather than relying entirely on manual control.
 
---
 
## Planned System and Technical Approach
 
- **Mobile platform:** Android
- **Development Language:** Kotlin
### Mechanisms
 
1. **Application restriction** – Users set up restricted apps. Once Focus Mode is on, opening a restricted app triggers a pop-up to deter usage.
2. **User location navigation** – Users set a focus radius (e.g. around the library). Entering the radius auto-activates Focus Mode. Also supports inviting friends to a **study party**.
3. **Focusing time slot** – Users define recurring time slots (e.g. 5–7pm weekdays). The system senses the time and auto-activates.
4. **Screen usage detection** – Detects whether the user is actively using the phone or just glancing (e.g. checking messages).
5. **Restrict apps using times** – Users can set a limited number of "tea breaks" per focus session, with a defined break duration. Focus mode resumes automatically after.
6. **Calculating screen usage time** – Tracks screen time and interruption frequency to provide customized time suggestions / weekly goals.
7. **Rewarding system** – Gamified UX (e.g. growing a tree, climbing a mountain). Reward difficulty adapts to user behaviour (harder to distract = harder rewards).
8. **Volume detection** – Monitors ambient sound levels during focus sessions and can generate ambient noise if the environment is too loud (user-configurable preference).
9. **Screen prevention shortcuts** – Physical gesture shortcuts:
   - Flip phone face-down → closes restricted apps + activates Focus Mode
   - Shake phone → closes Focus Mode during a focus period
---
 
## System Architecture
 
Following Android's official recommended app architecture, the app is structured into **three layers**:
 
- **UI Layer** – Built with Activity/Fragment and ViewModel. Renders focus status, timer, and reward progress.
- **Domain Layer** – Core business logic as Use Cases (e.g. determining focus activation from location/time, calculating rewards).
- **Data Layer** – A Repository unifies three data sources:
  - **Sensor Data Source** – GPS, accelerometer, gyroscope, light sensor, app-usage stats via Android system APIs
  - **Local Data Source** – Caches session history and settings for offline use
  - **Remote Data Source** – Cloud database API syncing plans, restrictions, and history (used by Party Study Mode)
---
 
## Data Flow
 
1. Sensor Data Source continuously streams raw readings to the Repository.
2. Repository forwards relevant state to the Domain layer.
3. Domain layer's Use Cases evaluate rules (e.g. *"inside focus radius" AND "inside time slot"*) and compute the Focus Points score.
4. Results are exposed to the UI layer via ViewModel (LiveData/StateFlow), updating the interface without blocking the main thread.
5. Session results are cached locally, then asynchronously synced to the cloud when network is available.
```
Sensor Data Source → Repository → Domain Layer → ViewModel → UI Layer
                                        ↓
                                 Local Storage → Cloud (sync when online)
```
 
---
 
## Key Technical Decisions
 
| Decision | Purpose | Trade-off |
|---|---|---|
| **AccessibilityService** | App restriction — tracks usage status even when app is closed | Raises privacy concerns |
| **Geofencing API + Wi-Fi** | Location zone detection | Higher battery consumption & system complexity, but more accurate localization |
| **AlarmManager** | Time slot trigger in background | May be delayed by Doze mode; limits max number of slots |
| **App Category + Touch Frequency** (via UsageStatsManager) | Screen usage detection | Sacrifices gesture-level detail for simplicity & privacy compliance |
| **Threshold + Debounce** | Shortcut gesture detection | Trade-off between detection accuracy and power consumption |
| **AudioRecord** | Volume detection (amplitude only) | Requires user permission but lower privacy risk than raw audio |
| **MediaPlayer/SoundPool (local audio)** | Ambient noise generation | Avoids cost/privacy risk of external AI audio API |
| **Firebase** | Real-time party mode sync | Cheaper alternative (WebSocket) rejected — Firebase better supports dynamic user sets |
 
---
 
## Real-Time, Algorithmic and Integration Design
 
- **Sensor fusion / context engine:** Combines GPS, Wi-Fi signal, and clock to determine if the user is at a focus location during a focus time.
- **Localization:** GPS + user-defined location with configurable radius, supplemented by Wi-Fi source detection for zone entry/exit.
- **Algorithm:** Analyzes app usage duration and distracting-app open frequency to generate usage statistics and feedback.
- **Rewarding system:** Tracks streaks based on successful focus periods vs. distraction events, granting points as virtual rewards.
- **Real-time behaviour:** Background listeners + async processing; UI updates only when necessary to minimize lag.
- **External integration:** Google Maps/Location Services for location selection and GPS-based focus zones.
---
 
## Privacy & Permissions
 
- Only necessary runtime permissions requested (location, usage access).
- Users can disable focus monitoring at any time.
- Only **aggregated statistics** are retained long-term — no raw sensor logs.
---
 
## Diagrams
 
- **Run-Time Diagram** – rule evaluation flow (time/location → app group check → block/notify)
- **User Workflow Diagram** – see high-resolution originals: `UserFlowChart1.png`, `UserFlowChart2.png`
---
 
## UI / UX
 
- Prototyped in **Figma**, implemented in Android using **Jetpack Compose** + **Material Design 3**.
- Custom Canvas drawing may be used for distinctive visuals, time permitting.
**System language:**
- English as the main system language, with support for additional languages via Android string resources.
- Short text labels + clear icons (e.g. "Start Focus", "Take a Break", "Focus Score", "Focus Zone") to reduce reading effort.
**Navigation concept:**
Bottom navigation with four destinations — **Focus Mode, Focus History, Rewards, Settings** — for simple one-tap access.
 
📎 [Figma Prototype](#) *(link in original document)*
 
---
 
## Requirement Coverage and Justification
 
### Material – Report & Video
- **Screen recording:** Android built-in screen recording, edited in Adobe Premiere Pro.
- **External recording:** Physical device recorded via external camera to show physical interactions (e.g. shaking, face-down placement).
- Demonstrates the full user flow: creating a plan → setting restrictions → entering location → activating Focus mode → gestures → reviewing statistics.
- **Editing:** Captions and annotations added in Adobe Premiere Pro.
> *Note: Final video content may be adjusted according to implemented functions.*
 
### Material – Screenshot
- Android Studio Console showing a successful Gradle build.
- Application compiled and running on an Android emulator.
### Material – Commit Log
- Git used for version control.
- Commit history documents development progress, feature implementation, and individual contributions.
### Material – Itemized Contributions
- One-page contribution summary in the final report, listing tasks per member and collaborative features.
- Based on Git commit history for task assignment/progress tracking.
- Microsoft Planner used alongside Git for task allocation.
### Implementation – Quality
- Benchmarked against real-world products: **Focus Traveller** (WEI JEN CHEN) and **Forest** (Seekrtech).
- Follows Kotlin coding conventions and Android development guidelines.
- Meaningful naming, comments where necessary, consistent formatting, and modular components/classes.
### Innovation – Novelty
- **Context-aware focus:** Geofencing + time-based rules instead of manual app blocking.
- **Physical interaction:** Face-down phone detection as a Focus Mode trigger.
- **Usage-aware focus:** Historical app usage analysis enabling streak-based rewards.
- **Adaptive focus:** Usage-pattern-based recommendations for focus duration.
  > *Note: Adaptive recommendations are a potential feature, dependent on available historical data.*
### Innovation – Surprise
- **Shake trigger:** Deliberate shake gesture to activate Focus Mode.
- **Environmental reaction:** Auto mute/reduce notification sounds in noisy environments via microphone-based detection.
- **Gamified behaviour:** Rewards, achievements, and challenges based on focus behaviour.
  > *Note: Reward system scope is subject to change based on available data.*
### Innovation – Tech Knowledge
- **Mobile sensing:** GPS, Wi-Fi, clock, accelerometer, gyroscope, light sensor, microphone, screen usage.
- **Android systems:** App access permissions, background services, notifications.
- **Database systems:** Room/SQLite for local storage.
- **Cloud systems:** Firebase or REST API for remote sync.
- **Asynchronous processing:** Threading for concurrency.
  > *Note: Final storage architecture (Room/Firebase/REST) may be a combination depending on feasibility.*
### Innovation – Cross-Disciplinary
- **Productivity:** Scheduling and database management concepts.
- **Gamification:** Points, streaks, achievements, virtual rewards.
- **Behavioural Psychology:** Usage pattern feedback as positive reinforcement.
- **Human-Computer Interaction:** Physical gestures (e.g. face-down) minimizing manual actions.
### Innovation – Impact
- **Distraction reduction:** Restricts distracting apps during focus periods.
- **Habit building:** Rewards and streaks encourage consistent study behaviour.
- **Self-awareness:** Historical usage and focus statistics displayed to users.
- **Reduced manual effort:** Auto-triggered by location, time, or gesture.
---
 
## Group Member Tasks
 
| Member | Planned Contributions |
|---|---|
| **Victor Munacoha** | System architecture design (UI/Domain/Data layers); GPS + Geofencing + Accelerometer sensor integration; Cloud REST API integration |
| **Yu-Hao Lu** | Local data layer (Room/SQLite); Firebase real-time sync for Study Party feature; Commit log compilation for final submission |
| **David Shiau** | AccessibilityService integration (App Restriction, Screen Usage Detection); Focus Points algorithm & rule engine (Domain layer logic) |
| **Kai-Jiun Chan** | Reward/Progress UI (animations, progress rings); Reactive UI updates (ViewModel/LiveData binding) |
| **Jia-Ying Lee** | Figma wireframes & UI screens (Focus Mode, History, Settings); Jetpack Compose implementation of core screens |
 
**Shared tasks (whole team):**
- Report writing – System Architecture & Sensor Integration sections
- Report writing – Algorithm Design section
- Video demonstration recording & editing
---
 
## Declaration
 
We acknowledge the use of ChatGPT (chatgpt.com) to generate images for this assignment. Prompts such as *"generate a flowchart of [app flowchart bullet points]"* and *"generate an image of [description of prototype of app UI design]"* were entered. The outputs were used as sample demonstration use.
 
A full record of prompts and outputs is available upon request.
---

## 09172026 Update: Files That Need Change

A review pass was done against the Group Member Tasks table above. The
following files already exist in `app/src/main/java/com/example/focusapp/`
and now have inline `[HANDOFF -> Name | README task: "..."]` comments marking
exactly where each person's part plugs in. Look up your name below, open the
file, and search for `[HANDOFF -> <your name>` to find your spot.

| File | Owner(s) | README Task |
|---|---|---|
| `data/sensor/SensorDataSource.kt` | Victor Munacoha | GPS + Geofencing + Accelerometer sensor integration |
| `data/remote/RemoteDataSource.kt` | Victor Munacoha (REST) / Yu-Hao Lu (Firebase) | Cloud REST API integration / Firebase real-time sync — **decide one approach together first** |
| `data/local/LocalDataSource.kt` | Yu-Hao Lu | Local data layer (Room/SQLite) |
| `data/accessibility/FocusAccessibilityService.kt` | David Shiau | AccessibilityService integration (App Restriction, Screen Usage Detection) |
| `domain/usecase/CalculateFocusRewardUseCase.kt` | David Shiau | Focus Points algorithm & rule engine (Domain layer logic) |
| `domain/usecase/EvaluateFocusTriggerUseCase.kt` | Victor Munacoha | Geofencing-driven real-time trigger |
| `ui/screens/rewards/RewardsViewModel.kt` | Kai-Jiun Chan | Reactive UI updates (ViewModel/LiveData binding) |
| `ui/screens/rewards/RewardsScreen.kt` | Kai-Jiun Chan | Reward/Progress UI (animations, progress rings) |
| `ui/screens/history/HistoryViewModel.kt` | Yu-Hao Lu (data) + Kai-Jiun Chan (binding) | Local data layer / Reactive UI updates |
| `ui/screens/map/MapScreen.kt` | Victor Munacoha | GPS + Geofencing sensor integration |
| `ui/screens/party/PartyModeScreen.kt` | Yu-Hao Lu | Firebase real-time sync for Study Party feature |
| `ui/navigation/NavGraph.kt` (`onAvatarClick`) | Kai-Jiun Chan | Reward/Progress UI — route the avatar tap to the Report screen (History/Rewards tabs, see `MainActivity.md`) |

Note: these are comment-only edits — no logic or function signatures were
changed, so the project still compiles as-is.

---

## 20/09/2026 Update (David Shiau)

- **Real app picker**: "Blocked Apps" now shows the phone's actual installed
  apps (real icons + names) instead of placeholder data, with multi-select
  to build a block group.
- **App blocking now works end-to-end**: starting Quick Focus restricts the
  currently selected group's apps via `FocusAccessibilityService`; opening a
  restricted app shows a full-screen "blocked" screen with a button back to
  the home screen, instead of silently bouncing home. Prompts the user to
  grant the Accessibility permission first if it isn't on yet.
- **Settings**: added a "Check App Usage Duration" button that queries the
  real Android `UsageStatsManager` and displays today's per-app usage
  totals (display-only for now).

## 21/09/2026 Update (David Shiau)                                                                                                                              
- **Persistent notification-shade timer**: while a focus session is active,
    a foreground service (`FocusTimerService`) now shows an ongoing, non-
    dismissable notification with a live timer alongside the 
    existing full-screen session timer. Tapping it brings the app back to 
    the foreground. The notification is removed automatically when a session
    ends, whether by completing normally or by the existing hold-to-cancel
    gesture.
  - Requests the Android 13+ `POST_NOTIFICATIONS` runtime permission on
    launch so the notification can actually be shown.
  - No changes to existing app-blocking (`FocusAccessibilityService`) or
    in-app timer logic — this is purely additive.  
