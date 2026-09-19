package com.example.focusapp.ui.screens.party

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.Friend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * FriendListViewModel
 * -----------------------
 * Backs [FriendListScreen] - loads/adds/removes the local friend address
 * book (FocusRepository.getFriends/saveFriend/deleteFriend, Room-backed -
 * see data/local/dao/FriendDao.kt) and exposes this device's own uid so
 * the person can share it with whoever they want to add them back.
 */
class FriendListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusRepositoryProvider.get(application)

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _myUid = MutableStateFlow("")
    /** This device's own uid - share it with a friend so they can add you back (see FriendListScreen.kt). */
    val myUid: StateFlow<String> = _myUid.asStateFlow()

    init {
        viewModelScope.launch { _myUid.value = repository.getMyUid() }
        loadFriends()
    }

    fun loadFriends() {
        viewModelScope.launch { _friends.value = repository.getFriends() }
    }

    /** No-ops on a blank uid. Nickname falls back to the uid itself if left blank. */
    fun addFriend(uid: String, nickname: String) {
        val trimmedUid = uid.trim()
        if (trimmedUid.isBlank()) return
        viewModelScope.launch {
            repository.saveFriend(Friend(uid = trimmedUid, nickname = nickname.trim().ifBlank { trimmedUid }))
            loadFriends()
        }
    }

    fun removeFriend(uid: String) {
        viewModelScope.launch {
            repository.deleteFriend(uid)
            loadFriends()
        }
    }
}
