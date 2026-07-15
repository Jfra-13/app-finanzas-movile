package com.example.finanzas_independientes_app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point. Hilt builds the dependency graph for the process. */
@HiltAndroidApp
class FinanzasApp : Application()
