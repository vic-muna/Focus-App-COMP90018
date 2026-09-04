package com.example.focusapp.data.remote

/**
 * RemoteDataSource
 * -------------------
 * Placeholder for cloud sync, mainly needed by the Study Party feature
 * (syncing plans/restrictions/history and other users' locations).
 *
 * IMPORTANT - open decision flagged during plan review:
 * The project plan currently describes this as EITHER a REST API OR
 * Firebase (and even hedges with "may adopt one or a combination"), while
 * the Group Member Tasks table assigns REST API work to one member and
 * Firebase work to another. Confirm ONE approach with the team before
 * implementing this interface for real, to avoid building two backends.
 *
 * TODO: to be implemented later, once the REST-vs-Firebase decision is
 * made - e.g. suspend fun syncFocusZones(...),
 * suspend fun observePartyMembers(...), etc.
 */
interface RemoteDataSource
