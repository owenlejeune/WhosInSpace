package com.owenlejeune.whosinspace

import android.app.Application
import com.kieronquinn.monetcompat.core.MonetCompat
import com.owenlejeune.whosinspace.di.modules.preferencesModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class WISApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        // start koin
        startKoin {
            androidLogger(
                if (BuildConfig.DEBUG) Level.ERROR else Level.NONE
            )
            androidContext(this@WISApplication)
            modules(
                preferencesModule
            )
        }

        MonetCompat.enablePaletteCompat()
    }

}