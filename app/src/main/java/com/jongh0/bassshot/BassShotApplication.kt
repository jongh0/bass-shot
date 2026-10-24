package com.jongh0.bassshot

import android.app.Application
import com.jongh0.bassshot.domain.util.AppLogger

class BassShotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
    }
}
