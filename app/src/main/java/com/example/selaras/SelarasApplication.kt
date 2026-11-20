package com.example.selaras

import android.app.Application
import com.example.selaras.core.di.AppContainer
import com.example.selaras.core.di.DefaultAppContainer

class SelarasApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }
}
