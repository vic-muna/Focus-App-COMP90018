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

> **Outdated (28/09/2026):** several files in this table were removed or
> moved, and the `[HANDOFF -> ...]` comments were replaced by short TODOs.
> See the 28/09/2026 update at the end of this file.

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
| `data/repository/FocusRepositoryImpl.kt` (`deleteFocusZone`) | Victor Munacoha (REST) / Yu-Hao Lu (Firebase) | Cloud REST API integration / Firebase real-time sync — deleting a location only removes the local copy; add a remote delete |

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

## 23/09/2026 Update (David Shiau)
- **Today's Focus Time**: added a Settings button showing total focus time
  for today; `FocusSession` now has real persistence (was in-memory only).
- **Blocked-app-group persistence fix**: the selected group and its apps/
  schedule now survive an app restart (were only held in memory before).
- **Auto-suggestion banner permission fix**: accepting the "start a focus
  session?" banner now prompts for the Accessibility permission on first
  use, same as Quick Focus already did.
- **Wi-Fi source trigger**: added a "Check Wi-Fi Network" Settings
  button that displays the currently-connected SSID.
  added an on/off switch, tap-to-tag on
  the checked SSID, and a "Stored Wi-Fi Source List" button to view/untag
  saved networks. Connecting to a tagged network now shows the same
  auto-suggestion banner as schedule/location triggers.  

## 26/09/2026 Update (David Shiau)
- **Scheduled Limits** (renamed from "Blocked Apps"): during a group's
  scheduled time, each app can have a daily **Max Open Times** and
  **Max Duration** limit. Going over either one shows the blocked screen,
  even mid-use. Only usage inside the schedule window counts.
- **Schedule banner**: no longer starts a focus session; it opens a page
  showing each app's opens/usage in today's window against its limits.
- **Schedule editor**: the start time can no longer be later than the end
  time.
- **Location Zone groups**: Location now has its own app groups (picker,
  rename, group list), separate from Scheduled Limits. Accepting the
  location banner (or Quick Focus) blocks the selected location group.
- **Wi-Fi Source Detection**: moved from Settings to a third card on Home,
  with the same trigger switch, network check and tagged list, plus its own
  app groups. Accepting the Wi-Fi banner blocks the selected Wi-Fi group.
- **Banners**: the schedule, location and Wi-Fi banners can now all show at
  once, each dismissed separately.
- **Priority**: a started location/Wi-Fi focus session blocks its apps
  outright, overriding any Scheduled Limit on the same apps.
- **Wi-Fi name fix**: the network name now reads correctly on Android 12+,
  and the check says why when it can't (e.g. precise location needed,
  Location turned off).

## 26/09/2026 Update (Jia-Ying Lee)

### Known limitation: one focus location at a time (architecture unchanged)

The new Location screen shows a *list* of location groups (Figma "Location
Focuse"), but the data layer still follows its original **single-zone
contract**, and this update deliberately does **not** change that:

- `FocusRepository` only exposes `getFocusZone()` / `saveFocusZone()`, and
  `RoomLocalDataSource.saveFocusZone()` clears the `focus_zones` table before
  every insert. So saving a new location **replaces** the existing one, and
  the list never shows more than one entry.
- Editing a location (tap a card) saves it under the same id, so it updates
  in place. Deleting (hold a card, then confirm) goes through the new
  `FocusRepository.deleteFocusZone()`, which only removes the **local** Room
  row; see the handoff row above for the missing cloud delete.
- A multi-zone local API already exists (`LocalDataSource.getFocusZones()` /
  `addFocusZone()` / `deleteFocusZone()`, used by
  `data/sensor/GeofenceDataSource.kt`), but it isn't exposed through
  `FocusRepository`, and `GeofenceDataSource` isn't wired to any UI yet.

**Team decision needed:** whether to move the app to multiple saved zones
(expose the multi-zone API through `FocusRepository` and stop clearing the
table on save), and who owns that change. The UI already renders any number
of zones, so no UI change is needed once the repository returns a list.

Also still pending on the Location screen: the map itself is a placeholder
(`ui/screens/location/MapPlaceholder.kt`) until the team picks a map SDK
(Google Maps vs OpenStreetMap), and a zone's on/off switch is UI-only
(`FocusZone` has no "enabled" field).

## 28/09/2026 Update (Jia-Ying Lee)

Branches: `Dev/Alison-----UI-&-Workflow----Version-2-with-Schedule` (new UI on
top of David's 26/09 logic) and `Dev/Alison-----Cleanup` (clean-up and the
Quick Focus changes below). David's logic is kept; only the UI and wiring changed.

### New screens and flows
- **Bottom nav:** Home · Location · Time Focus (clock) · Wi-Fi.
- **Time Focus tab:** one card per schedule group, each with an on/off switch.
  Tap = summary card, pencil = edit, hold = delete. Adding/editing is a
  three-step fly card: apps → days + time → daily limits (Max Open Times /
  Max Minutes) + name. Starts empty on a fresh install (no fake groups).
- **Location tab:** each location has its own blocked apps (picked with the
  same app card). Still one saved location at a time (see 26/09 note above).
- **Wi-Fi tab:** one entry per network (id = SSID) with its own blocked apps
  and switch. Adding: network (connected now / seen before / typed) → apps → name.
- **Quick Focus:** tap → accessibility permission check → pick the apps to
  block (last pick is pre-ticked and remembered) → the check starts focusing.
- **Party Mode:** group icon at Home's top-left → friend list with search
  (friend IDs are a TODO) → **Create group** / **Join group** fly cards using
  David's Firebase party logic (the 6-letter code is the party id). The host's
  start uses the same permission check and app card as Quick Focus. When the host
  starts, everyone who joined starts focusing too, and the host can't start until
  every member has App Blocking on (see the 08/10/2026 update).
- **Time Focus banner:** the old "today's app usage" page was removed. Tapping
  the banner opens a fly card asking for Usage access if it's off (the daily
  limits need it), otherwise it opens the Time Focus tab.
- **Time Focus notification:** while a time slot with limits is on, a
  status-bar card (in the theme's colour) shows the slot's name and
  time, the icons of its apps, and the last opened app with its opens/minutes
  left. It updates on every app switch, disappears when the slot ends, and a
  tap opens that slot's summary card.
- **Blocked screen:** shows David's block reason. **Got it** / Back returns to
  the focus timer during a session, or to the phone's home screen for a
  daily-limit block.
- **Dashboard** (tap Home's picture): theme ID card, weekly chart, history,
  background theme picker, and an X to close.
- **Settings:** permission rows open the matching Android settings page.

### What each focus session blocks
| Started by | Blocks |
|---|---|
| Quick Focus, Party Mode | the apps picked in the Quick Focus card |
| Location banner | that location's apps |
| Wi-Fi banner | that network's apps |
| Time Focus | no session; apps over their daily limit are blocked during the time slot |

### Code clean-up (for readability)
- **Removed** (no entry point any more): the old Home sheets, Apps / Map /
  Rewards / old Focus Mode / debug screens, the old `PartyModeScreen` and
  `FriendListScreen`, `WireframeColors`, and unused test-panel code in
  `AccessibilityBridge`. Room, Firebase and `WifiTriggerStorage` are kept.
- **Moved:** `BlockedAppGroup`, `BlockedAppGroupStorage`, `TimeSlot`, `AppItem`
  → `data/blocking/`.
- **Shared components** in `ui/components/`, grouped by type:
  - `button/`: X / check / next / back / edit buttons, pill and icon buttons
  - `card/`: `FocusCard` (card frame, title, labels, sunken panel),
    `SwitchListCard`, `AppSelectCard`, `FlyCardOverlay`, `DiscardDialog` /
    `DeleteDialog`
  - `input/`: text field, search field, switch, checkbox
  - `bar/`: bottom nav, top bars, settings list, scrollbar
  - `layout/`: `GroupListLayout` (the Time Focus / Wi-Fi page skeleton)
- Widgets used by one screen only now live in that screen's folder.
- Comments were rewritten to be short and plain; every composable keeps an
  `@Preview`.

## 01/10/2026 Update (Victor Munacoha)
- Interactive Map added to the Location Screen.
- Fine GPS Coordinates are used to display current Location on the Map
- Selecting, adding apps and confirming the Focus Zone adds the area to the Geofence list
- Geofences successfully send a trigger to Focus to block apps
- Interruption screen display the name of the Focus Zone which blocks the opening
- Overlaps of the Focus Zones does not lead to unwanted behaviour

## 02/10/2026 Update (David Shiau)
- Wi-Fi tab: the add/edit card is back to four rows - the connected Wi-Fi (tap to
  save it), the saved Wi-Fi list (tap to remove), the Wi-Fi that trigger blocking
  (tick one or more) and the blocked apps. The "type its name" box is gone, and one
  entry can now watch several networks.
- Wi-Fi blocking is automatic: `FocusAccessibilityService` follows the phone's Wi-Fi
  (`data/wifi/WifiWatcher.kt`) and blocks an entry's apps while the phone is on one of
  its networks, even with Focus closed; leaving the Wi-Fi unblocks them. The Home
  banner now only starts a focus session to record the time.
- The Wi-Fi tab warns when App Blocking, precise location, "Allow all the time"
  location or the phone's Location setting is missing.
- Wi-Fi detection also works when Wi-Fi isn't the default network (VPN on, or Wi-Fi
  without internet).
- `AccessibilityBridge` keeps a separate blocked-app list per source (focus session,
  Location, Wi-Fi), so ending a session or leaving a zone no longer unblocks the
  other sources' apps.
- History: "Distracting app opens" now counts each attempt to open a blocked app
  during a focus session.

### Still open
- Friend ID system (search and friend list are placeholders).
- A break ("tea break") during a focus session is not built yet.

## 02/10/2026 Update (David Shiau) - User accounts
- **Login screen** on first launch (and after logging out): **Log in**, **Create
  account** (username + password, no email) or **Continue as guest** (the old
  anonymous mode). Phones that were already using the app as a guest go straight in.
- **Guest → account:** Settings → Account → **Create account** links the new
  username/password to the guest, so the uid and all saved data stay the same.
- **Restore on login:** logging in on a new phone (or after a reinstall) downloads
  the account's sessions, Focus Zone and app groups from Firebase into Room and
  re-registers the zone geofences. A failed download is retried on the next start.
- **Switching users:** when a different user signs in, the previous user's data on
  the phone (Room, blocked-app lists, saved Wi-Fi, geofences) is cleared first.
- Settings shows who is signed in and has **Log out** (guests are warned their data
  can't be recovered).
- Code: `data/account/AccountManager.kt` (Firebase Auth, a username is stored as
  `username@users.focusapp.example.com`), `ui/screens/account/AccountScreen.kt`.
- **Setup:** Firebase Console → Authentication → Sign-in method → enable
  **Email/Password** (keep **Anonymous** on for guests).

### Known limitations
- No email, so a forgotten password can't be reset.
- Only sessions, the Focus Zone and app groups sync. Per-feature blocked-app lists,
  Wi-Fi networks, friends and the theme are still phone-only.
- Firebase keeps one `zone` per user, so only the last saved location is restored.

## 03/10/2026 Update (David Shiau) - Rewards
- **Rewards page:** the trophy at the dashboard's top-left opens it. Two independent parts,
  both worked out from the saved sessions (`domain/usecase/CalculateFocusRewardUseCase.kt`):
  - **Today's milestones:** today's total focus time against 5 min, 30 min, 1 h, 5 h and
    10 h - back to zero at midnight.
  - **Daily streak:** days in a row whose focus time reaches the goal (2 h by default).
    Today only breaks the streak once it ends without reaching the goal.
- **Settings → Rewards → Daily streak goal:** 30 min to 8 h in 30-minute steps. The streak
  is recalculated from history, so changing the goal also applies to past days.
- A session that runs past midnight is split between the two days.

## 03/10/2026 Update (David Shiau) - Focus Coach (AI weekly feedback)
- **Focus Coach button** at the dashboard's bottom-right: a one-off report on the last
  7 days from Gemini, then preset follow-ups ("How can I reduce distractions?", "When is
  my best time to focus?", "How do I keep my streak going?").
- Only a summary worked out on the phone is sent - totals vs the week before, minutes per
  day and time of day, distracting-app attempts per hour, streak - no names or ids
  (`domain/usecase/BuildWeeklyFocusSummaryUseCase.kt`). A one-time notice asks first.
- **5 AI answers per day** (report and follow-ups), counted on the phone; failed requests
  don't count. Today's report is saved, so reopening doesn't ask again.
- Gemini is called through **Firebase AI Logic** (`data/ai/FocusCoach.kt`, model
  `gemini-3.5-flash`), so no Gemini key is in the app. Firebase BOM updated to 34.19.0.
- **App Check:** debug builds use debug tokens, release builds Play Integrity
  (`src/debug` / `src/release` `AppCheckSetup.kt`).
- **Setup:** Firebase Console → AI Logic → Get started → **Gemini Developer API**. For
  App Check, add the debug token printed in Logcat ("Enter this debug secret...") under
  App Check → Apps → ⋮ → Manage debug tokens. Keep enforcement off for Realtime Database
  and Authentication.

### Known limitations
- The daily limit is kept on the phone, so reinstalling resets it.
- Play Integrity isn't registered yet (the console's Terms of Service step failed), so
  App Check enforcement for AI Logic is off for now.

## 03/10/2026 Update (Yu-Hao Lu)

Party invites were failing silently: sendPartyInvite() was fire-and-forget (no error handling), so a rejected write (most likely blocked by Realtime Database Rules, since it writes into another user's data) never surfaced — the inviter saw no error, and the invite just never arrived. It's now a suspend function that awaits the write and surfaces failures through the existing error banner.

Friends are no longer added by pasting the other person's raw Firebase user ID (a 28-character string). Each user now has a short 6-character friend code (shown under "My Code" in Party Mode, with a copy button) that resolves to their real ID behind the scenes — same format as the existing 6-character group codes.

Added the ability to remove a saved friend from the Friends list.

## 03/10/2026 Update (David Shiau) - Noise Alert, Flip to Focus, Shake to End
- **Noise Alert:** while the Focus Mode timer is on screen, the microphone measures the
  room's sound level (measured only, never recorded). If the 10-second average goes above
  the threshold, a "It's too loud for studying" banner shows; it hides after 10 s back at
  or below it, so it doesn't flicker. **Settings → Noise Alert:** on/off and threshold
  (40-85 dB, 55 dB by default). A raw/average dB readout under the timer is temporary.
  - `data/sensor/NoiseLevelDataSource.kt`: `AudioRecord`, `UNPROCESSED` source (no
    automatic gain control), falling back to `VOICE_RECOGNITION`; one reading per 500 ms.
  - dB is approximate: dBFS + 90, uncalibrated, varies by phone (max reading 90 dB).
  - `domain/usecase/NoiseLevelTracker.kt`: moving average and banner timing.
- **Flip to Focus:** while the app is open and no session is running, lying the phone
  face-down (gravity + proximity sensors) for 2 s starts a focus session with a short
  vibration. Flipping back does nothing. **Settings → Flip to Focus:** on/off (off by
  default) and its own list of apps to block. Needs App Blocking on, like Quick Focus.
- **Shake to End:** on the Focus Mode screen, 3 jolts above 2.5 g within 1 s ends and
  saves the session, with a short vibration (`domain/usecase/ShakeDetector.kt`).
- New permissions: `RECORD_AUDIO` (asked at runtime) and `VIBRATE`.

## 04/10/2026 Update (Jia-Ying Lee) - Friend codes need an account

Friend codes could be read and overwritten by any signed-in user, so a code didn't
reliably point at its owner. Fixed in the Realtime Database Rules and in Party Mode:

- **Friend codes belong to accounts.** Only a signed-in account (not a guest) can claim
  a code, only for its own uid, and only one. A claimed code can't be changed or deleted,
  and the list of all codes can't be read - only one code at a time can be looked up.
- **Invites can't be forged.** An invite must carry the sender's own uid, and guests
  can't send one.
- **Guests:** Party Mode shows no My Code, friend list, search or invites - just a note
  and **Create group** / **Join group**, which still work with a 6-letter group code.
  Creating an account (Settings -> Account) turns the friend features on.
- **Add friend:** a code that doesn't exist now keeps the card open and shows a message,
  instead of closing without saying anything.
- Code: `ui/screens/party/PartyModeViewModel.kt` (`isGuest`), `ui/screens/party/FriendsScreen.kt`.
- Setup (Firebase Console, once): the rules are in
  `docs/firebase-realtime-database-structure.md` -> **Security Rules**. Paste them into
  Realtime Database -> Rules and publish. They are already published for this project.

### Demo accounts

Three accounts for testing friends and invites (log in with **Log in**, not as a guest):

| Username | Password |
|---|---|
| `testa1` | `Unimelb_Test123` |
| `testb1` | `Unimelb_Test123` |
| `testc1` | `Unimelb_Test123` |

To try it: log in as `testa1` on one phone and `testb1` on another, add each other with
the code under **My Code**, then **Create group** on one and tap **Invite**.

### Testing tip
- The Android 17 preview emulator image froze while testing Party Mode (the system's GPS
  service hung). An API 35/36 image, or denying the app's location permission on that
  emulator, avoids it.

## 04/10/2026 Update (Jia-Ying Lee) - Merge and tidy-up of noise alert and gestures

David's noise alert, Flip to Focus and Shake to End are merged with the Party Mode fix.
No behaviour changed; the code was reorganised so it is easier to read:

- **One app picker card.** Quick Focus, Party Mode and Flip to Focus each had their own
  copy of the "pick apps to block" card. They now share
  `ui/components/card/AppPickerCard.kt` (`QuickFocusAppsCard` and `FlipFocusAppsCard`
  were removed).
- **Noise alert has its own file.** The microphone permission, the dB readout and the
  "too loud" banner moved out of `FocusSessionScreen.kt` into
  `ui/screens/session/NoiseAlert.kt`, with previews. The timer screen just calls
  `NoiseAlert()`.
- **Settings:** the microphone check is one helper, `hasMicrophonePermission()` in
  `ui/common/MicrophonePermission.kt`.
- Note: `app/google-services.json` is still tracked in git on `main`.
- Known issue: 4 tests in `FocusRepositoryImplTest` (`saveFocusZone...`) already fail on
  `main`; they are not related to this change.

## 04/10/2026 Update (Jia-Ying Lee) - Reward backgrounds

Kevin's idea (focus to unlock rewards), built on David's Rewards data: keeping a daily
streak now unlocks a new background theme.

- **New theme: Valley** ("Enjoy the calm of the valley"), with its own Home and Focus
  Mode art. The Time Focus banner crops the Focus Mode art until a header is drawn.
- **How it unlocks:** reach the daily goal (Settings -> Rewards -> Daily streak goal)
  7 days in a row. It uses the *best* streak so far, so a missed day later does not lock
  it again. Nothing new is stored: it is worked out from the saved focus sessions.
- **Where it shows:**
  - Theme picker: a locked theme is dimmed with a lock and "7-day streak to unlock".
  - Rewards page: a **Backgrounds** card with the best streak and how far each theme is.
  - A "New background unlocked!" dialog (once per theme) when the app opens or a session
    ends, with **Use it now** / **Later**.
- **Testing shortcut:** in the theme picker, tap a locked theme 3 times and type
  `Unimelb_90018` to unlock it straight away.
- **All the rules are in one file:** `domain/model/RewardRules.kt` - daily milestones,
  the daily goal (default and range), the streak each background needs, and the test
  code. Change the numbers there.
- To add a reward background: add its streak to `RewardRules.kt`, then the theme (name,
  intro, art) to `ui/theme/BackgroundThemes.kt`.
- Code: `domain/usecase/CalculateFocusRewardUseCase.kt` (`bestStreakDays`),
  `ui/screens/history/ThemePickerScreen.kt`, `ui/screens/history/ThemeUnlockCodeCard.kt`,
  `ui/screens/rewards/RewardsScreen.kt`, `ui/navigation/NavGraph.kt` (the dialog).
- Note: the "already shown" and "unlocked by code" records are kept on the phone, not in
  the account.

## 04/10/2026 Update (Yu-Hao Lu)
- The real user account has been connected (visitors are displayed as "Guest").

- AI merging is complete. FocusCoach now has sessionFeedback(...), sharing the same Firebase AI Logic settings as the weekly report, but a second GenerativeModel is enabled with different system prompts (the "3-5 suggestions" format of the weekly report is not suitable for a single session's one-sentence feedback). It's intentionally sharing the same daily quota of 5 times with the weekly report, instead of creating a separate set—otherwise, users running Quick Focus multiple times a day would exhaust their free quota prematurely. The FocusRepositoryProvider has also added the context passing step.

- Password reset via security questions:
The password is encrypted using a "key calculated from the answer" and stored in Firebase (PasswordRecovery.kt); security questions (set during registration with a question and answer; if you forget your password, answering correctly resets it). If you forget your password, answering the question correctly decrypts the password, retrieves the original password, logs in normally, and then calls Firebase's native updatePassword() to change the password.

## 05/10/2026 Update (Victor Munacoha)
- Bug where Focus Location was deleted after toggling it was fixed
- Added Button to add more Time Frames for Schedule Focus Times
- Daily Opens open the app unrestricted for the set Open Time
- Interruption Screen shows how many opens are left
- Daily opens for one app is shared between different Time Frames and Groups (e.g., a total of 3 opens for the whole day within the Scheduled times, not each time frame)
- Slider in the ScheduleDial.kt wheel was adjusted to only change if the orange part itself is touched, none of the other parts
- Search bar was added in the Location Tab

## 08/10/2026 Update (Jia-Ying Lee) - Party Mode: everyone starts together

Before, only the host's phone went into focus. Now the group starts together:

- **Members follow the host.** When the host starts, every member who has joined (and
  is on the join card) starts focusing too, blocking the apps picked in their own Quick
  Focus card. Without the Accessibility permission they get the permission dialog
  instead of starting.
- **The host waits for everyone to be ready.** Each phone says in Firebase whether App
  Blocking (Accessibility) is on. If a member has it off when the host taps start, the
  host stays on the group card and sees "Wait for the participant to open the
  permission." (or "...N participants..."). The message goes away by itself when they
  turn it on, and the host taps start again. It is checked again after the host picks
  the apps, in case someone turned it off in between.
- **Members who aren't ready are notified.** They get a notification ("Your group is
  waiting for you") and the permission dialog. Both go away once the permission is on.
- **Fixed: members never saw the host focusing.** The `focusing` value was being read
  back as `false`, so nothing could follow it. The members are now read key by key in
  `FirebaseRemoteDataSource.observePartyMembers()`.
- **Fixed: the "focusing" flag was never reset.** It stayed `true` after a session, so
  the next person to join would have been pulled straight into focus. It is cleared when
  a phone leaves the group or the screen closes.
- **Limits:**
  - The notification comes from the phone's own Firebase listener, so it only appears
    while the app is running. A push to a closed app would need a server (Cloud
    Functions / FCM).
  - A member who is killed without leaving keeps their last status in the party and can
    keep blocking the host's start.
  - Members have to be on the join card when the host starts; ending the host's session
    does not end theirs.
- Code: `ui/screens/party/PartyModeViewModel.kt` (`requestStart`, `partyFocusStarted`,
  `permissionNeeded`, `waitingForMembers`), `ui/screens/party/FriendsScreen.kt`,
  `data/notification/PartyNotification.kt`, `domain/model/PartyMemberStatus.kt`
  (`accessibilityReady`, `waitingForPermission`).
- Firebase: two new keys under `parties/{partyId}/members/{uid}`; no rule change. See
  `docs/firebase-realtime-database-structure.md`.
- Known issue: 4 tests in `FocusRepositoryImplTest` (`saveFocusZone...`) still fail; they
  are not related to this change.
