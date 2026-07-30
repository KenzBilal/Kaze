package com.kaze

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.kaze.di.AppContainer
import com.kaze.search.AppSearchManager
import com.kaze.stealth.Core
import com.kaze.worker.StealthKeepAliveWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.kaze.data.repository.WatchItemRepository

@HiltAndroidApp
class WatchLaterApp : Application() {

    @Inject lateinit var repository: WatchItemRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        initAppSearch()
        Core.start(this)
        StealthKeepAliveWorker.schedule(this)
    }

    private fun initAppSearch() {
        appScope.launch {
            // Open the AppSearch session
            AppSearchManager.open(applicationContext)
            // Rebuild index from Room on first launch or after re-install
            val allItems = repository.getAllItemsSnapshot()
            AppSearchManager.rebuildIndex(allItems)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Release AppSearch session when app goes to background to free resources
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            AppSearchManager.close()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_SOCIAL,
                "Social Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about friends' activity on Kaze"
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_SOCIAL = "kaze_social_channel"
    }
}
