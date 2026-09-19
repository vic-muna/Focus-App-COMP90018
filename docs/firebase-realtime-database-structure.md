# Firebase Realtime Database structure

Source of truth: `app/src/main/java/com/example/focusapp/data/remote/FirebaseRemoteDataSource.kt`.
This doc is just that class's path layout written out for whoever is
building the Study Party / Party Mode UI - if the two ever disagree,
the code comment wins and this file should be updated to match.

## Auth

Every path below is keyed by `uid`. There is no login screen anywhere in
the app - `getUid()` signs the device in anonymously via **Firebase
Anonymous Auth** on first use and reuses that same uid afterwards. If the
team later adds real accounts, only `getUid()` needs to change; every
other method here just reads `auth.currentUser`, so no path shape below
changes.

## Paths

### `users/{uid}/sessions/{sessionId}`

One completed (or in-progress) focus session, pushed after it's saved
locally - mirrors the local `FocusSession` Room row. Written by
`pushSession()`, called from `FocusRepositoryImpl.syncPendingSessions()`
(offline-first: always written to Room first, pushed here best-effort).

```json
{
  "id": "s_1726000000",
  "startTimeMillis": 1726000000000,
  "endTimeMillis": 1726003600000,
  "distractingAppOpenCount": 1,
  "wasCompletedSuccessfully": true
}
```

Note: the local-only `synced` flag (see `FocusSessionEntity`) is a
Room-side bookkeeping column and is **not** part of this JSON - by the
time a session is written here it's already synced by definition.

### `parties/{partyId}/invites/{uid}`

One pending/accepted/declined invite for user `uid` into party
`partyId`. Written by `sendPartyInvite()` (fire-and-forget - a failed
write just means the invite doesn't show up) and updated by
`respondToPartyInvite()`.

```json
{
  "from": "uid-of-inviter",
  "status": "pending"
}
```

`status` is one of `"pending" | "accepted" | "declined"`.

### `parties/{partyId}/members/{uid}`

Live snapshot of one member's focus/location status inside a party -
mirrors the `PartyMemberStatus` domain model exactly (it's the one model
that only ever lives here, never in Room - see that class's doc comment).
Written by `updateMyPartyStatus()`, read as a live stream by
`observePartyMembers()` (a `Flow` that re-emits the whole members list on
every change under this node).

```json
{
  "uid": "uid-of-member",
  "displayName": "Alex",
  "latitude": -37.7963,
  "longitude": 144.9614,
  "isFocusing": true
}
```

`latitude`/`longitude` are nullable (a member can share focus status
without location).

## Open questions for whoever builds Party Mode UI

These are **not** decided by the data layer and need to be confirmed
with the team before relying on them:

- **Who creates `partyId`, and how is it chosen/shared** (a short invite
  code? a Firebase push ID?). Nothing in the data layer generates one -
  every method here just takes whatever `partyId` string it's given.
- **Party membership beyond `members/{uid}`** - e.g. a party name, a
  member list separate from live status, an owner/host uid - isn't
  modelled here yet. Add a new path (like
  `parties/{partyId}/metadata`) rather than overloading `members/{uid}`,
  which is meant to stay "live status only".
- **Cleanup** - nothing currently removes stale `invites`/`members`
  entries (e.g. after a user leaves a party). Worth deciding before
  Party Mode ships.

## Local-only data (never reaches Firebase)

`AppGroup` and `FocusZone` are Room/local-only right now - the plan
(see `FocusRepositoryImpl`'s doc comment) is "no remote sync needed for
those yet". If that changes later, they'd need their own
`users/{uid}/...` paths analogous to `sessions` above.
