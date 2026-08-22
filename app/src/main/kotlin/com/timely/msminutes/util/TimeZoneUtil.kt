package com.timely.msminutes.util

import android.icu.text.TimeZoneNames
import android.icu.util.TimeZone
import java.util.Locale

object TimeZoneUtil {
    fun getCityName(id: String): String {
        val names = TimeZoneNames.getInstance(Locale.getDefault())
        val canonicalId = TimeZone.getCanonicalID(id) ?: id
        val city = names.getExemplarLocationName(canonicalId)
        if (!city.isNullOrEmpty()) return city
        
        return id.substringAfterLast('/').replace('_', ' ')
    }

    fun getCountryName(id: String): String {
        val region = try {
            TimeZone.getRegion(id)
        } catch (_: Exception) {
            null
        }
        
        if (region != null && region != "001" && region.length == 2) {
            return Locale.Builder().setRegion(region).build().getDisplayCountry(Locale.getDefault())
        }
        
        val parts = id.split("/")
        return if (parts.size > 1) parts[0].replace("_", " ") else "Other"
    }
}
