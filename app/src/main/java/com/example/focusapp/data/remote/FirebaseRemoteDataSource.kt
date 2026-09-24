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
 * FirebaseRemoteDataSource
 * ---------------------------
 * Firebase Realtime Database implementation of [RemoteDataSource].
 *
 * Uses Firebase Anonymous Auth purely to get a stable per-install uid -
 * there's no login screen anywhere in the app yet and this doesn't need
 * one. If the team later adds real accounts, swap the sign-in call inside
 * getUid(); nothing else here needs to change since every other method
 * just reads auth.currentUser.
 *
 * NEEDS (not something code alone can provide - see chat for details):
 *  - A real google-services.json from an actual Firebase project, placed
 *    in app/. This class will crash at runtime without it.
 *  - INTERNET (and ideally ACCESS_NETWORK_STATE) permission uncommented
 *    in AndroidManifest.xml.
 *
 * Realtime Database layout used below - confirm these path names with
 * whoever builds the invite / friend-list UI before relying on them
 * elsewhere:
 *   users/{uid}/sessions/{sessionId}       <- FocusSession, mirrors the Room row
 *   users/{uid}/zone                       <- FocusZone, the user's one saved zone
 *   users/{uid}/appGroups/{groupId}        <- AppGroup, mirrors the Room row
 *   users/{uid}/incomingInvites/{partyId}  <- { "from": fromUid } - fan-out mirror of the row
 *                                              below, written/removed alongside it, purely so a
 *                                              recipient can observe "invites addressed to me"
 *                                              without needing to already know every partyId.
 *   parties/{partyId}/invites/{uid}        <- { "from": uid, "status": "pending" | "accepted" | "declined" }
 *   parties/{partyId}/members/{uid}        <- PartyMemberStatus
 */
class FirebaseRemoteDataSource(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : RemoteDataSource {

    override suspend fun getUid(): String {
        auth.currentUser?.let { return it.uid }
        return auth.signInAnonymously().await().user!!.uid
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
