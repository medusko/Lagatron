package com.medusko.lagatron

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds

class LagatronApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.SHOW_ADS) {
            try {
                MobileAds.initialize(this) {}
            } catch (e: Exception) {
                Log.e("LagatronApp", "Failed to initialize MobileAds", e)
            }
        }
    }
}
