package com.watchlater

import android.app.Application
import com.watchlater.di.AppContainer

class WatchLaterApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
