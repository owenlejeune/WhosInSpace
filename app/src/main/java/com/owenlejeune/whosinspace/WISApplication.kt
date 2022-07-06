package com.owenlejeune.whosinspace

import android.app.Application
import com.kieronquinn.monetcompat.core.MonetCompat

class WISApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        MonetCompat.enablePaletteCompat()
    }

}