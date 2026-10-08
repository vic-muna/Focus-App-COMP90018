package com.example.focusapp.data.remote

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.PartyInvite
import com.example.focusapp.domain.model.PartyMemberStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The Firebase Realtime Database version of [RemoteDataSource].
 * Every path is keyed by the signed-in user's id (guest or account - see AccountManager).
 * Needs app/google-services.json.
 *
 * Where the data lives in the database:
 *   users/{uid}/sessions/{sessionId}       focus sessions
 *   users/{uid}/zone                       the saved location
 *   users/{uid}/appGroups/{groupId}        app groups
 *   users/{uid}/incomingInvites/{partyId}  invites sent to this user
 *   parties/{partyId}/invites/{uid}        invite status (pending / accepted / declined)
 *   parties/{partyId}/members/{uid}        each member's PartyMemberStatus
 */
class FirebaseRemoteDataSource(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : RemoteDataSource {

    // Signing in (as a guest or with an account) happens on the login screen - see AccountManager.
    // No automatic guest sign-in here: a background upload after logging out would otherwise
    // quietly create a new guest.
    override suspend fun getUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

    // Read field by field: the domain models have no empty constructor, which
    // Firebase's automatic getValue(Class) needs.
    override suspend fun fetchUserData(): CloudUserData {
        val uid = getUid()
        val user = db.getReference("users/$uid").get().await()

        val sessions = user.child("sessions").children.mapNotNull { child ->
            val id = child.child("id").getValue(String::class.java) ?: child.key ?: return@mapNotNull null
            val start = child.child("startTimeMillis").getValue(Long::class.java) ?: return@mapNotNull null
            FocusSession(
                id = id,
                startTimeMillis = start,
                endTimeMillis = child.child("endTimeMillis").getValue(Long::class.java),
                distractingAppOpenCount = child.child("distractingAppOpenCount").getValue(Long::class.java)?.toInt() ?: 0,
                wasCompletedSuccessfully = child.child("wasCompletedSuccessfully").getValue(Boolean::class.java) ?: false,
                groupId = child.child("groupId").getValue(String::class.java),
            )
        }

        val zoneNode = user.child("zone")
        val zone = run {
            val id = zoneNode.child("id").getValue(String::class.java) ?: return@run null
            FocusZone(
                id = id,
                name = zoneNode.child("name").getValue(String::class.java) ?: "Focus Zone",
                latitude = zoneNode.child("latitude").getValue(Double::class.java) ?: return@run null,
                longitude = zoneNode.child("longitude").getValue(Double::class.java) ?: return@run null,
                radiusMeters = zoneNode.child("radiusMeters").getValue(Double::class.java)?.toFloat() ?: return@run null,
            )
        }

        val appGroups = user.child("appGroups").children.mapNotNull { child ->
            val id = child.child("id").getValue(String::class.java) ?: child.key ?: return@mapNotNull null
            AppGroup(
                id = id,
                groupName = child.child("groupName").getValue(String::class.java) ?: "",
                packageNames = child.child("packageNames").children.mapNotNull { it.getValue(String::class.java) },
            )
        }

        return CloudUserData(sessions = sessions, zone = zone, appGroups = appGroups)
    }

    override suspend fun pushSession(session: FocusSession) {
        val uid = getUid()
        db.getReference("users/$uid/sessions/${session.id}").setValue(session).await()
    }

    override suspend fun pushFocusZone(zone: FocusZone) {
        val uid = getUid()
        db.getReference("users/$uid/zone").setValue(zone).await()
    }

    override suspend fun pushAppGroup(group: AppGroup) {
        val uid = getUid()
        db.getReference("users/$uid/appGroups/${group.id}").setValue(group).await()
    }

    override suspend fun sendPartyInvite(partyId: String, toUid: String) {
        // [Claude, 2026-10-03] No longer fire-and-forget: both writes are awaited now, so a
        // rejected write (see RemoteDataSource.sendPartyInvite's doc comment - most likely a
        // Database Rules problem, since this writes into ANOTHER user's users/$toUid subtree)
        // throws here instead of vanishing. getUid() instead of auth.currentUser?.uid ?: "" for
        // the same reason every other method in this class uses it: a blank fromUid used to be
        // possible (and silently accepted) if this ran before sign-in somehow completed.
        val fromUid = getUid()
        db.getReference("parties/$partyId/invites/$toUid")
            .setValue(mapOf("from" to fromUid, "status" to "pending"))
            .await()
        // Fan-out mirror so $toUid's own client can observe "invites addressed to me" without
        // needing to already know partyId - see this class's doc comment for why this exists.
        db.getReference("users/$toUid/incomingInvites/$partyId")
            .setValue(mapOf("from" to fromUid))
            .await()
    }

    override suspend fun getOrCreateMyFriendCode(): String {
        val uid = getUid()
        val existingRef = db.getReference("users/$uid/friendCode")
        val existing = existingRef.get().await().getValue(String::class.java)
        if (existing != null) return existing

        // Not generated yet - try random codes until an unclaimed one is found.
        repeat(MAX_FRIEND_CODE_ATTEMPTS) {
            val candidate = randomFriendCode()
            // transaction(), not get()-then-setValue(): without it, two devices generating a
            // code at the same moment could both read "unclaimed" and then both write,
            // silently overwriting one of them. The transaction only commits if nothing
            // claimed this exact code in between its read and its write.
            if (tryClaimFriendCode(candidate, uid)) {
                existingRef.setValue(candidate).await()
                return candidate
            }
        }
        error("Couldn't claim a friend code after $MAX_FRIEND_CODE_ATTEMPTS tries")
    }

    /** True if [code] was free and is now claimed for [uid]; false if someone already has it.
     *  See [getOrCreateMyFriendCode] for why this needs to be an atomic transaction rather than
     *  a plain read-then-write. runTransaction()'s own callback is not a suspend callback (it
     *  fires later, on its own thread) - suspendCancellableCoroutine is what turns "wait for
     *  that callback" into something this suspend function can actually wait for (the same
     *  trick observeMyIncomingInvites()/observePartyMembers() use via callbackFlow for a
     *  listener that fires more than once; here it's a one-off result instead of a stream). */
    private suspend fun tryClaimFriendCode(code: String, uid: String): Boolean =
        suspendCancellableCoroutine { cont ->
            db.getReference("friendCodes/$code").runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    if (currentData.value != null) return Transaction.abort()
                    currentData.value = uid
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (!cont.isActive) return
                    if (error != null) cont.resumeWithException(error.toException()) else cont.resume(committed)
                }
            })
        }

    override suspend fun resolveFriendCode(code: String): String? =
        db.getReference("friendCodes/${code.trim().uppercase()}").get().await()
            .getValue(String::class.java)

    override fun observeMyIncomingInvites(): Flow<List<PartyInvite>> = callbackFlow {
        val uid = getUid()
        val ref = db.getReference("users/$uid/incomingInvites")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    snapshot.children.mapNotNull { child ->
                        val partyId = child.key ?: return@mapNotNull null
                        val fromUid = child.child("from").getValue(String::class.java) ?: return@mapNotNull null
                        PartyInvite(partyId = partyId, fromUid = fromUid)
                    }
                )
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun respondToPartyInvite(partyId: String, accept: Boolean) {
        val uid = getUid()
        db.getReference("parties/$partyId/invites/$uid/status")
            .setValue(if (accept) "accepted" else "declined")
            .await()
        // Responded to - stop showing it in the incoming-invites list (see
        // observeMyIncomingInvites()). The full record still lives under
        // parties/$partyId/invites/$uid above, this just clears the mirror.
        db.getReference("users/$uid/incomingInvites/$partyId").removeValue().await()
    }

    override fun observePartyMembers(partyId: String): Flow<List<PartyMemberStatus>> = callbackFlow {
        val ref = db.getReference("parties/$partyId/members")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Read by hand, not getValue(PartyMemberStatus::class.java): the automatic mapping
                // can't tell that the stored "focusing" belongs to the isFocusing property, so it
                // can leave it false - which would stop anyone ever following the host into focus.
                trySend(snapshot.children.map { child ->
                    PartyMemberStatus(
                        uid = child.child("uid").value as? String ?: child.key.orEmpty(),
                        displayName = child.child("displayName").value as? String ?: "",
                        latitude = (child.child("latitude").value as? Number)?.toDouble(),
                        longitude = (child.child("longitude").value as? Number)?.toDouble(),
                        isFocusing = child.child("focusing").value as? Boolean ?: false,
                        accessibilityReady = child.child("accessibilityReady").value as? Boolean ?: true,
                        waitingForPermission = child.child("waitingForPermission").value as? Boolean ?: false
                    )
                })
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun updateMyPartyStatus(partyId: String, status: PartyMemberStatus) {
        db.getReference("parties/$partyId/members/${status.uid}").setValue(status)
    }

    /** Six characters from a 32-symbol alphabet (no 0/O/1/I, matching PartyGroupCards.newPartyCode()'s
     *  reasoning for the same exclusions) - easy to read aloud and tell apart. */
    private fun randomFriendCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private companion object {
        const val MAX_FRIEND_CODE_ATTEMPTS = 5
    }
}
