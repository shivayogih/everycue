package com.everycue.feature.pack

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private fun newId(): Long = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE

private val Context.packDataStore by preferencesDataStore(name = "everycue_pack_data")

class PackRepository(context: Context) : PackStore {
    private val appContext = context.applicationContext
    private val dataKey = stringPreferencesKey("everycue_pack_json")
    private val backupKey = stringPreferencesKey("everycue_pack_json_backup")
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val data: Flow<PackData> = appContext.packDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::decode)

    override suspend fun createTrip(draft: TripDraft): Long {
        val tripId = newId()
        val templateItems = TemplateCatalog.find(draft.templateId)
            ?.items
            .orEmpty()
            .mapIndexed { index, item ->
                PackingItem(
                    id = newId(),
                    name = appContext.getString(item.nameResource),
                    category = item.category,
                    quantity = item.quantity,
                    position = index,
                )
            }

        mutate { current ->
            current.copy(
                trips = listOf(
                    Trip(
                        id = tripId,
                        name = draft.name.trim(),
                        destination = draft.destination.trim(),
                        startDateMillis = draft.startDateMillis,
                        endDateMillis = draft.endDateMillis,
                        items = templateItems,
                    ),
                ) + current.trips,
            )
        }
        return tripId
    }

    override suspend fun updateTrip(tripId: Long, draft: TripDraft) = mutateTrips { trips ->
        trips.map { trip ->
            if (trip.id == tripId) trip.copy(
                name = draft.name.trim(),
                destination = draft.destination.trim(),
                startDateMillis = draft.startDateMillis,
                endDateMillis = draft.endDateMillis,
            ) else trip
        }
    }

    override suspend fun addItem(
        tripId: Long,
        name: String,
        category: PackingCategory,
        quantity: Int,
    ) = mutateTrips { trips ->
        trips.map { trip ->
            if (trip.id != tripId) return@map trip
            val nextId = newId()
            val nextPosition = (trip.items.maxOfOrNull(PackingItem::position) ?: -1) + 1
            trip.copy(
                items = trip.items + PackingItem(
                    id = nextId,
                    name = name.trim(),
                    category = category,
                    quantity = quantity.coerceAtLeast(1),
                    position = nextPosition,
                ),
            )
        }
    }

    override suspend fun setPacked(tripId: Long, itemId: Long, packed: Boolean) = mutateTrips { trips ->
        trips.map { trip ->
            if (trip.id == tripId) {
                trip.copy(items = trip.items.map { item ->
                    if (item.id == itemId) item.copy(isPacked = packed) else item
                })
            } else trip
        }
    }

    override suspend fun deleteItem(tripId: Long, itemId: Long) = mutateTrips { trips ->
        trips.map { trip ->
            if (trip.id == tripId) trip.copy(items = trip.items.filterNot { it.id == itemId })
            else trip
        }
    }

    override suspend fun moveItem(tripId: Long, itemId: Long, offset: Int) = mutateTrips { trips ->
        trips.map { trip ->
            if (trip.id != tripId) return@map trip
            val ordered = trip.items.sortedBy(PackingItem::position).toMutableList()
            val from = ordered.indexOfFirst { it.id == itemId }
            if (from < 0) return@map trip
            val to = (from + offset).coerceIn(0, ordered.lastIndex)
            if (from != to) {
                val moved = ordered.removeAt(from)
                ordered.add(to, moved)
            }
            trip.copy(items = ordered.mapIndexed { index, item -> item.copy(position = index) })
        }
    }

    override suspend fun unpackAll(tripId: Long) = mutateTrips { trips ->
        trips.map { trip ->
            if (trip.id == tripId) trip.copy(items = trip.items.map { it.copy(isPacked = false) })
            else trip
        }
    }

    override suspend fun deleteTrip(tripId: Long) = mutateTrips { trips ->
        trips.filterNot { it.id == tripId }
    }

    override suspend fun addDemoTrip(): Long {
        val id = newId()
        val demo = Trip(
            id = id,
            name = appContext.getString(R.string.demo_trip_name),
            destination = appContext.getString(R.string.demo_destination),
            startDateMillis = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1_000L,
            endDateMillis = System.currentTimeMillis() + 9 * 24 * 60 * 60 * 1_000L,
            items = TemplateCatalog.find("weekend")!!.items.mapIndexed { index, item ->
                PackingItem(
                    id = newId(),
                    name = appContext.getString(item.nameResource),
                    category = item.category,
                    quantity = item.quantity,
                    isPacked = index < 2,
                    position = index,
                )
            },
        )
        mutate { current -> current.copy(trips = listOf(demo) + current.trips) }
        return id
    }

    override suspend fun resetAll() {
        appContext.packDataStore.edit { preferences ->
            preferences.remove(dataKey)
        }
    }

    override suspend fun snapshot(): PackData = data.first()

    override suspend fun replaceAll(restored: PackData) {
        mutate { restored }
    }

    private suspend fun mutateTrips(transform: (List<Trip>) -> List<Trip>) {
        mutate { current -> current.copy(trips = transform(current.trips)) }
    }

    private suspend fun mutate(transform: (PackData) -> PackData) {
        appContext.packDataStore.edit { preferences ->
            val current = decode(preferences)
            preferences[dataKey]?.let { preferences[backupKey] = it }
            preferences[dataKey] = json.encodeToString(transform(current))
        }
    }

    private fun decode(preferences: Preferences): PackData {
        val primary = preferences[dataKey] ?: return PackData()
        return decodeRaw(primary) ?: preferences[backupKey]?.let(::decodeRaw) ?: PackData()
    }

    private fun decodeRaw(raw: String): PackData? = try {
        json.decodeFromString<PackData>(raw)
    } catch (error: SerializationException) {
        Log.e("PackRepository", "Could not decode local packing data", error)
        null
    }
}

