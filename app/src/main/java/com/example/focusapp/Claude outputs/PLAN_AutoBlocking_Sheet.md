# Implementation Plan — Auto Blocking Sheet Redesign

> For Claude Code. Project: **Focus** (COMP90018, Group T01/01-04)
> Owner of this scope: Jia-Ying Lee — Front-End / UI-UX
> Stack: Android · Kotlin · Jetpack Compose · Material 3 · Navigation Compose
> Date: 2026-09-15

---

## 0. Read this first

This plan **replaces** the Blocked Apps bottom-sheet design described in
`claude/Homepage_BlockedAppsGroup_Handoff.md`. Where the two disagree, this
document wins.

Core change: the summary sheet stops being a read-only card with an `Edit` button
that navigates to a separate editing page. Instead **the summary cards are
themselves the edit entry points**, and every edit happens in a layered sheet or
dialog on top of the summary. Two full-screen destinations are deleted as a result.

Constraints that still apply:

- UI/UX only. No ViewModel, no Room, no Firebase. All state hoisted to
  `FocusAppNavGraph` and passed down as callback lambdas.
- Fake data only (`generateFakeGroups()` etc.).
- Every screen/component file keeps an `@Preview`.
- Any list of apps must use `LazyColumn` (100+ items), never `Column`.

---

## 1. Target design

The sheet shows one group's automatic blocking rule.

```
┌──────────────────────────────────────────┐
│                                          │
│             Auto Blocking                │   static title, never changes
│             ──────────                   │
│                                          │
│  ┌──────────────┐  ┌──────────────────┐  │
│  │ Block Apps   │  │ Active Time      │  │
│  │              │  │                  │  │
│  │ ▢▢▢▢   +8   │  │ 07:20 AM         │  │
│  │              │  │   |       Weekend│  │
│  │              │  │ 04:20 PM   Wed   │  │
│  │              │  │            Tue   │  │
│  │              │  │            Mon   │  │
│  └──────────────┘  └──────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │                 ∨                  │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

Rules:

- **Title** is the literal string `"Auto Blocking"`. It is not the group name and
  never changes.
- **Block Apps card** shows at most **4** app icons; any remaining apps collapse into
  a `+N` label (N = `apps.size - 4`). Write it as `+8`, not `8+`.
- **Active Time card** shows the start time above and the end time below, separated by
  a short vertical rule. Both reflect the stored setting.
- **Day list** is right-aligned inside the Active Time card, stacked vertically, and
  reflects the stored day selection. When all 7 days are selected it collapses to a
  single line `Every day`.
- **Bottom bar** contains only a chevron (`∨`). Tapping anywhere on it navigates to
  the group list. It has no other content.
- Card fills are **translucent white over the sheet background**, not solid colors.

### Colour tokens (approximate — confirm against Figma before finalising)

| Token | Value | Use |
|---|---|---|
| `SheetBackground` | `#2E2E9E` | sheet surface |
| `CardSurface` | `White @ 12%` | Block Apps / Active Time cards |
| `BottomBarSurface` | `White @ 35%` | bottom chevron bar |
| `OnSheet` | `#FFFFFF` | all text and icons |
| `OnSheetMuted` | `White @ 70%` | day labels, `+N` |

---

## 2. Interaction map

| Tap target | Opens | Component |
|---|---|---|
| Block Apps card | App picker | `ModalBottomSheet` |
| Start time (`07:20 AM`) | Start time input | `Dialog` |
| End time (`04:20 PM`) | End time input | `Dialog` |
| Day list | Day multi-select | `ModalBottomSheet` |
| Bottom `∨` bar | Group list screen | navigation |

**The Active Time card is not clickable as a whole.** The start time, the end time and
the day list are three separate tap targets. A single card-wide target would be
ambiguous because one card holds three different settings.

All edits **apply immediately** (no Save button, no Cancel). Closing a picker is
"done". This matches the layered-sheet model and removes the need for draft state.

---

## 3. Technical decisions

### 3.1 The outer sheet stays `ModalBottomSheet`

The reason the project previously moved to `BottomSheetScaffold` was the need for a
two-stage drag (peek → expand to reveal an `Edit` button). **That requirement is gone**
— the new design shows everything at once and uses the bottom chevron instead of a
drag gesture. `ModalBottomSheet` is therefore correct again, and it brings back the
free scrim and tap-outside-to-dismiss.

Do **not** reintroduce `BottomSheetScaffold`, and do **not** reintroduce a variable
`sheetPeekHeight` — animating peek height between `0.dp` and a non-zero value made the
`PartiallyExpanded` and `Hidden` anchors collide and froze the main thread
(`Choreographer: Skipped 1254 frames`).

### 3.2 Stacking sheets — set the inner scrim to transparent

The app picker and day picker are `ModalBottomSheet`s opened **on top of** the summary
`ModalBottomSheet`. This works (each is its own popup window), but each sheet draws its
own scrim, and two stacked scrims render almost black.

```kotlin
ModalBottomSheet(
    onDismissRequest = { showAppPicker = false },
    sheetState = appPickerState,
    scrimColor = Color.Transparent   // REQUIRED when stacking
) { … }
```

### 3.3 Time entry uses `TimeInput`, not a wheel

Material 3 ships two time pickers: `TimePicker` (clock dial) and `TimeInput` (two
numeric fields plus AM/PM). There is **no wheel picker** in Material 3.

Use `TimeInput` inside an `AlertDialog`:

- it matches the `07:20 AM` format shown on the card,
- the clock dial is cramped in a dialog on small screens,
- hand-rolling a wheel (`LazyColumn` + `snapFlingBehavior`) is not worth the time budget
  for this assignment.

Use a `Dialog`, not a third stacked sheet — three stacked sheets make Android's back
behaviour confusing.

### 3.4 Sheet-level state

Three independent flags in the composable that owns the summary sheet content:

```kotlin
var showAppPicker by remember { mutableStateOf(false) }
var showDayPicker by remember { mutableStateOf(false) }
var editingTime by remember { mutableStateOf<TimeTarget?>(null) }   // START / END / null
```

They are mutually exclusive in practice, so no enum state machine is needed.

---

## 4. Data model changes

### 4.1 `TimeSlot` — structured, not a display string

The current model stores `timeText: String` (e.g. `"04:00pm - 06:00pm"`), which cannot
be edited by a time picker. Replace it:

```kotlin
// ui/screens/home/TimeSlot.kt
package com.example.focusapp.ui.screens.home

/** Minute-precision wall-clock time. Ints avoid java.time desugaring concerns. */
data class ClockTime(
    val hour: Int,     // 0..23
    val minute: Int    // 0..59
) {
    /** "07:20 AM" style, split so the card can style AM/PM smaller. */
    fun formatted(): Pair<String, String> {
        val suffix = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "%02d:%02d".format(displayHour, minute) to suffix
    }
}

data class TimeSlot(
    val activeDays: Set<String>,
    val start: ClockTime,
    val end: ClockTime
)
```

`MAX_TIME_SLOTS` is no longer needed — see 4.2.

### 4.2 `BlockedAppGroup` — one schedule, not a list

The new design shows a single start/end pair. Collapse the list:

```kotlin
// ui/screens/home/BlockedAppGroup.kt
data class BlockedAppGroup(
    val id: String,
    val name: String,
    val apps: List<AppItem>,
    val schedule: TimeSlot
)
```

Update `generateFakeGroups()` accordingly. Every call site that iterated
`group.timeSlots.take(MAX_TIME_SLOTS)` must be updated to read `group.schedule`.

### 4.3 Day keys

Existing data uses `"Wen"` for Wednesday. The correct abbreviation is `"Wed"`.
**Fix this while touching the model** — the `WEEKDAYS` / `Every day` comparisons in the
formatter depend on exact string matching, and a typo there silently breaks the
collapse logic. Define the canonical list once:

```kotlin
val DAY_KEYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
```

---

## 5. Files

### Create

| File | Contents |
|---|---|
| `ui/screens/home/AutoBlockingSheetContent.kt` | the summary layout in §1, plus private `BlockAppsCard`, `ActiveTimeCard`, `BottomChevronBar` |
| `ui/screens/home/AppPickerSheet.kt` | `ModalBottomSheet` + search field + `LazyColumn` of checkable app rows |
| `ui/screens/home/DayPickerSheet.kt` | `ModalBottomSheet` + 7 toggleable day rows |
| `ui/screens/home/ClockTimeDialog.kt` | `AlertDialog` wrapping `TimeInput` |

### Modify

| File | Change |
|---|---|
| `ui/screens/home/TimeSlot.kt` | new `ClockTime` + restructured `TimeSlot` (§4.1) |
| `ui/screens/home/BlockedAppGroup.kt` | `schedule: TimeSlot` replaces `timeSlots: List<TimeSlot>` |
| `ui/screens/home/HomeScreenWithSheet.kt` | back to `ModalBottomSheet`; host the three picker flags; pass edit callbacks up |
| `ui/screens/home/GroupListScreen.kt` | `+` now opens a name dialog instead of navigating to an edit page; add long-press to delete |
| `ui/navigation/NavGraph.kt` | remove the section/edit routes; add group create/update/delete callbacks |
| `ui/navigation/Destinations.kt` | remove `GROUP_SECTION`, `EDIT_BLOCKED_APPS_GROUP`, `NEW_GROUP_ID` |

### Delete

| File | Reason |
|---|---|
| `ui/screens/home/EditBlockedAppsScreen.kt` | editing now happens in the layered pickers |
| `ui/screens/home/GroupSectionScreen.kt` | it displayed exactly what the summary sheet already shows |
| `ui/screens/home/BlockedAppsBottomSheet.kt` | superseded by `AutoBlockingSheetContent.kt` |

**Do not touch** `AddAppGroupScreen.kt`, `EditAppGroupScreen.kt`, `AppsScreen.kt` or
their routes — they belong to a separate feature owned by another team member.

---

## 6. Navigation after this change

```
Home
 └─ Auto Blocking sheet  (ModalBottomSheet)
      ├─ App picker      (ModalBottomSheet, scrim transparent)
      ├─ Day picker      (ModalBottomSheet, scrim transparent)
      ├─ Time dialog     (Dialog)
      └─ ∨ → GroupListScreen  (full screen destination)
             └─ + → name dialog → creates group, selects it, pops back to Home
```

Depth drops from four full-screen levels to one.

`GroupListScreen`'s back arrow keeps the existing behaviour: set
`savedStateHandle["reopenSheet"] = true` on the Home back stack entry before
`popBackStack()`, so the summary sheet reopens showing the selected group.

---

## 7. Group creation, rename, delete

Deleting `EditBlockedAppsScreen` removes the only place a group could be named, so
`GroupListScreen` must absorb those operations:

- **Create** — `+` opens a small `AlertDialog` with a single text field and a
  Create button (disabled while blank). On confirm: build a `BlockedAppGroup` with an
  empty app list and a sensible default schedule, select it, and pop back to Home.
- **Rename** — long-press a group card → the same dialog, pre-filled.
- **Delete** — long-press a group card → a `Delete` option → confirmation dialog.
  Refuse to delete the last remaining group (the summary sheet has nothing to show
  otherwise); disable the option and explain why rather than crashing on
  `groups.first()`.

---

## 8. Acceptance criteria

1. App launches and Home renders with no dropped-frame warning in Logcat.
2. Tapping the Blocked Apps carousel card opens the Auto Blocking sheet.
3. The Block Apps card shows at most 4 icons; with 12 apps it reads `+8`.
4. Tapping the Block Apps card opens the app picker over the summary, with a visibly
   normal (not doubled) scrim. Checking an app updates the icon row and the `+N`
   immediately on dismiss.
5. Tapping the start time opens a dialog; confirming updates only the start time.
   The end time is unaffected.
6. Selecting all 7 days collapses the day list to `Every day`.
7. The bottom `∨` bar navigates to the group list; the back arrow returns Home with
   the sheet reopened on the selected group.
8. `+` creates a named group; long-press renames or deletes; the last group cannot be
   deleted.
9. Every new file has a working `@Preview`.
10. The project contains no reference to `EditBlockedAppsScreen`, `GroupSectionScreen`,
    `MAX_TIME_SLOTS` or `timeText` after the change.

---

## 9. Open design questions — ask before deciding

1. **The group name has no home in the new layout.** The title is fixed at
   "Auto Blocking", so the sheet does not show which group is active. The user picks
   "Study Group" in the list, returns, and sees only "Auto Blocking". Proposed fix: a
   muted subtitle under the title, or the name on the left of the bottom chevron bar.
   Do not pick one unilaterally — confirm with the designer.
2. **Is one schedule per group final?** §4.2 collapses `timeSlots` to a single
   `schedule`. If multiple slots return later this is a breaking model change, so
   confirm before removing the list.

---

## 10. Commit

Conventional Commits, scope `home`. Suggested split:

1. `refactor(home): restructure TimeSlot into ClockTime and single schedule`
2. `feat(home): add auto blocking summary sheet with app and time cards`
3. `feat(home): add app picker, day picker and time input dialogs`
4. `refactor(home): remove group section and edit screens`
