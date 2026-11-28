package com.mimo.companion

import android.app.Application

class MimoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
