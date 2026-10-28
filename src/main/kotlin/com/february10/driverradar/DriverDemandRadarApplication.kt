package com.february10.driverradar

import android.app.Application
import com.february10.driverradar.di.AppContainer

/**
 * Wires the hand-rolled [AppContainer] so every component gets its deps from
 * a single root object instead of a DI framework.
 */
class DriverDemandRadarApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.create(this)
    }
}
