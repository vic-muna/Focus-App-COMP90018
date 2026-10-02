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
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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

    override fun sendPartyInvite(partyId: String, toUid: String) {
        // Fire-and-forget: a failed write just means the invite doesn't
        // show up, and no local/offline state depends on the result.
        val fromUid = auth.currentUser?.uid ?: ""
        db.getReference("parties/$partyId/invites/$toUid")
            .setValue(mapOf("from" to fromUid, "status" to "pending"))
        // Fan-out mirror so $toUid's own client can observe "invites addressed to me" without
        // needing to already know partyId - see this class's doc comment for why this exists.
        db.getReference("users/$toUid/incomingInvites/$partyId")
            .setValue(mapOf("from" to fromUid))
    }

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
                trySend(snapshot.children.mapNotNull { it.getValue(PartyMemberStatus::class.java) })
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
}
