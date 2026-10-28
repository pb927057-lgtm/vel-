package com.vel.secure

import android.app.Application
import androidx.room.Room
import com.vel.secure.data.local.VelDatabase

class VelApplication : Application() {
    lateinit var database: VelDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            VelDatabase::class.java,
            "vel_secure_vault.db"
        ).build()
    }
}
