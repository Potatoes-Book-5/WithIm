package com.potatoes.book5.realestate

import android.app.Application
import com.potatoes.book5.realestate.di.AppContainer

class RealEstateApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
