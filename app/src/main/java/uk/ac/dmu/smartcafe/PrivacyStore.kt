package uk.ac.dmu.smartcafe

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "smart_cafe_privacy_prefs")

class PrivacyStore(private val context: Context) {

    companion object {
        const val TERMS_VERSION = "2026-11-21-v1"
        val TERMS_VERSION_KEY = stringPreferencesKey("termsVersion")
        val ACCEPTED_AT_KEY = longPreferencesKey("acceptedAt")
        val ANALYTICS_KEY = booleanPreferencesKey("analytics")
        val LOCAL_AI_KEY = booleanPreferencesKey("localAi")
        val LOCATION_KEY = booleanPreferencesKey("location")
        val PHOTOS_KEY = booleanPreferencesKey("photos")
    }

    val privacyPreferencesFlow: Flow<PrivacyPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            PrivacyPreferences(
                termsVersion = preferences[TERMS_VERSION_KEY],
                acceptedAt = preferences[ACCEPTED_AT_KEY],
                analytics = preferences[ANALYTICS_KEY] ?: false,
                localAi = preferences[LOCAL_AI_KEY] ?: false,
                location = preferences[LOCATION_KEY] ?: false,
                photos = preferences[PHOTOS_KEY] ?: false
            )
        }

    suspend fun acceptTerms() {
        context.dataStore.edit { preferences ->
            preferences[TERMS_VERSION_KEY] = TERMS_VERSION
            preferences[ACCEPTED_AT_KEY] = System.currentTimeMillis()
            preferences[ANALYTICS_KEY] = false
            preferences[LOCAL_AI_KEY] = false
            preferences[LOCATION_KEY] = false
            preferences[PHOTOS_KEY] = false
        }
    }

    suspend fun updatePreference(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[key] = enabled
        }
    }
}

data class PrivacyPreferences(
    val termsVersion: String?,
    val acceptedAt: Long?,
    val analytics: Boolean,
    val localAi: Boolean,
    val location: Boolean,
    val photos: Boolean
)
