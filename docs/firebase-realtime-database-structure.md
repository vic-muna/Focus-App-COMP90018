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

### `users/{uid}/username`

The account's username (lowercase), e.g. `"testa1"`. It is only a label for
people reading the database in the Firebase Console, so a uid folder can be
matched to a person - the app never reads it back. Written by
`AccountManager.saveUsername()` when an account is created, logged in to,
recovered, and each time the app starts while signed in. Guests have no
username, so their folder has no such child. The existing `users/$uid` rule
already lets the owner write it - no rule change needed.

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

### `users/{uid}/zones/{zoneId}`

One saved [`FocusZone`](../app/src/main/java/com/example/focusapp/domain/model/FocusZone.kt) per child,
keyed by the zone's id - "restrictions/plans" in the original project plan's Remote
Data Source description. Written by `pushFocusZone()`, called from
`FocusRepositoryImpl.syncPendingZoneAndAppGroups()` (which pushes **every** unsynced
zone, each to its own node) - same offline-first shape as sessions above (Room first,
this best-effort). Removed by `deleteFocusZone()` when a location is deleted (best-effort:
a delete made while offline is not retried, so that zone's cloud copy can come back on
the next restore).

```json
{
  "id": "zone_1726000000",
  "name": "Library",
  "latitude": -37.7963,
  "longitude": 144.9614,
  "radiusMeters": 150.0
}
```

**Old single node `users/{uid}/zone`:** before several locations were supported, the whole
app had one zone, stored as a single object here, and every push overwrote it. It is no
longer written. `fetchUserData()` still reads it (so a backup made before this change
restores), and `deleteFocusZone()` clears it if it holds the zone being deleted. A zone
in `zones/` wins over the old node when both have the same id.

### `users/{uid}/appGroups/{groupId}`

One [`AppGroup`](../app/src/main/java/com/example/focusapp/domain/model/AppGroup.kt) ("restrictions/plans"),
mirrors the local Room row. Written by `pushAppGroup()`, same
offline-first shape as sessions/zones above.

```json
{
  "id": "g_1726000000",
  "groupName": "Social Media",
  "packageNames": ["com.instagram.android", "com.zhiliaoapp.musically"]
}
```

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

### `users/{uid}/incomingInvites/{partyId}`

Fan-out mirror of the row above, keyed the other way round (by the
*recipient's* uid, then partyId) purely so a client can observe "every
invite addressed to me" via `observeMyIncomingInvites()` without needing
to already know which partyIds to look under - Realtime Database has no
"query across all parties for my uid" without this kind of denormalised
index. Written alongside `parties/{partyId}/invites/{uid}` by
`sendPartyInvite()`, and removed (not just status-updated) by
`respondToPartyInvite()` once the user has acted on it - so this node is
always exactly "still-pending invites", nothing more.

```json
{
  "from": "uid-of-inviter"
}
```

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
  "focusing": true,
  "accessibilityReady": true,
  "waitingForPermission": false
}
```

Note the JSON key is `focusing`, not `isFocusing` - `PartyMemberStatus.isFocusing` carries an
explicit `@PropertyName("focusing")` to force this. Without it, Firebase's reflection-based
mapper writes and reads that one property under two *different* keys, so it always reads back as
`false` no matter what was written (a real bug hit during testing - see that class's doc comment).
Because of that, `FirebaseRemoteDataSource.observePartyMembers()` does not use the automatic
mapping at all: it reads each key by hand.

`latitude`/`longitude` are nullable (a member can share focus status
without location).

`accessibilityReady` is whether that phone has App Blocking (Accessibility) turned on. The host
can't start the group's session until every member has it. A missing key counts as ready.
`waitingForPermission` is only ever true on the host's entry: the host tried to start while someone
wasn't ready. Members who aren't ready react to it with a notification and the permission dialog.
A member who leaves is written back as ready and not waiting, since their entry stays in the list.
No rule change is needed - each member writes only their own `members/{uid}`.

## Security Rules

Not part of this repo - these live in **Firebase Console → Realtime
Database → Rules** (or the Firebase CLI, if the team sets that up
later), so whoever has console access needs to paste this in and hit
Publish. Enabling Anonymous Auth does **not** by itself grant any read/
write access - that's this file's job, and a rules mismatch is exactly
what a `DatabaseError: Permission denied` in Logcat means (as opposed to
an auth failure, which shows up differently - see FirebaseRemoteDataSource's
`getUid()`).

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid === $uid",
        ".write": "auth != null && auth.uid === $uid",
        "friendCode": {
          ".validate": "newData.isString() && root.child('friendCodes').child(newData.val()).val() === auth.uid"
        },
        "incomingInvites": {
          "$partyId": {
            ".write": "auth != null && auth.token.firebase.sign_in_provider !== 'anonymous' && !data.exists() && newData.child('from').val() === auth.uid"
          }
        }
      }
    },
    "friendCodes": {
      "$code": {
        ".read": "auth != null",
        ".write": "auth != null && auth.token.firebase.sign_in_provider !== 'anonymous' && !data.exists() && newData.val() === auth.uid && !root.child('users').child(auth.uid).child('friendCode').exists()",
        ".validate": "$code.matches(/^[A-Z2-9]{6}$/)"
      }
    },
    "parties": {
      "$partyId": {
        ".read": "auth != null",
        "members": {
          "$uid": {
            ".write": "auth != null && auth.uid === $uid"
          }
        },
        "invites": {
          "$uid": {
            ".write": "auth != null && (auth.uid === $uid || newData.child('from').val() === auth.uid)"
          }
        }
      }
    }
  }
}
```

`invites/$uid` is intentionally writable by any authenticated user (not
just `$uid`) since the inviter writes into the *invitee's* slot - the
`auth.uid === $uid` pattern used everywhere else doesn't fit that one
path. That's permissive enough that any signed-in user could write a
bogus invite to anyone; fine for a class project, but worth tightening
(e.g. requiring `newData.child('from').val() === auth.uid`) before this
goes anywhere near real users.

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
- **Cleanup** - responding to an invite now removes its
  `users/{uid}/incomingInvites/{partyId}` mirror (see that path above),
  but nothing yet removes stale `parties/{partyId}/members/{uid}` entries
  (e.g. after a user leaves a party) or old `parties/{partyId}/invites/{uid}`
  rows once responded-to. Worth deciding before Party Mode ships.

## Local-only data (never reaches Firebase)

`Friend` (the local address book behind the Study Party invite picker -
see `ui/screens/party/FriendListScreen.kt`) is Room/local-only by design:
there's no server-side "friend request" concept, so each side just saves
the other's uid on their own device. `FocusZone`/`AppGroup` used to be
local-only too but now sync via the paths above.
