package com.owenlejeune.whosinspace.di.modules

import com.owenlejeune.whosinspace.preferences.AppPreferences
import org.koin.dsl.module

val preferencesModule = module {
    single { AppPreferences(get()) }
}