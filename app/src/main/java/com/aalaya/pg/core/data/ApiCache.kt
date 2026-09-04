package com.aalaya.pg.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.coroutines.flow.first

private val Context.apiCacheDataStore by preferencesDataStore(name = "hamsa_api_cache")

@Serializable
private data class CachedValue(
    val savedAtMillis: Long,
    val payload: kotlinx.serialization.json.JsonElement,
)

/**
 * Small persistent response cache used for stale-while-revalidate screens.
 * It intentionally stores only server responses, never credentials or request
 * bodies. A cache miss is harmless: callers simply use the network result.
 */
@Singleton
class ApiCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    suspend fun <T> read(key: String, serializer: KSerializer<T>, maxAgeMillis: Long): T? {
        val raw = context.apiCacheDataStore.data.first()[stringPreferencesKey(key)] ?: return null
        val cached = runCatching { json.decodeFromString(CachedValue.serializer(), raw) }.getOrNull()
            ?: return null
        if (System.currentTimeMillis() - cached.savedAtMillis > maxAgeMillis) return null
        return runCatching { json.decodeFromJsonElement(serializer, cached.payload) }.getOrNull()
    }

    suspend fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        val raw = json.encodeToString(
            CachedValue.serializer(),
            CachedValue(System.currentTimeMillis(), json.encodeToJsonElement(serializer, value)),
        )
        context.apiCacheDataStore.edit { it[stringPreferencesKey(key)] = raw }
    }

    suspend fun clear() {
        context.apiCacheDataStore.edit { it.clear() }
    }
}
