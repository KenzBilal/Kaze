package com.kaze.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.kaze.data.repository.UserRepository
import com.kaze.utils.BackupManager
import com.kaze.utils.BackupResult
import com.kaze.utils.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    suspend fun getLocalUsername() = userRepository.getLocalUsername()
    suspend fun getLocalUserId() = userRepository.getLocalUserId()

    suspend fun exportToUri(uri: Uri): BackupResult = backupManager.exportToUri(uri)
    suspend fun importFromJson(json: String): RestoreResult = backupManager.importFromJson(json)
    
    suspend fun restoreFromCloud(userId: String): Int = backupManager.restoreFromCloud(userId)
    suspend fun uploadToCloud(userId: String): Int = backupManager.uploadToCloud(userId)
}
