package com.example.finanzas_independientes_app

import android.app.Application
import com.example.finanzas_independientes_app.di.ServiceLocator

/** Application entry point. Boots the manual DI container once per process. */
class FinanzasApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
