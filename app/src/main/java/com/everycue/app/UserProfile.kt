package com.everycue.app

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.profileDataStore by preferencesDataStore(name = "everycue_profile")

@Serializable
data class LocalProfile(
    val firstName: String,
    val lastName: String,
    val countryCode: String,
    val mobileNumber: String,
    val email: String,
    val address: String,
    val pincode: String,
) {
    val fullName: String get() = "$firstName $lastName".trim()

    fun validate() {
        if (firstName.isBlank()) throw ProfileValidationException(R.string.error_first_name)
        if (lastName.isBlank()) throw ProfileValidationException(R.string.error_last_name)
        if (!countryCode.matches(Regex("\\+[0-9]{1,4}"))) throw ProfileValidationException(R.string.error_country_code)
        if (!mobileNumber.matches(Regex("[0-9]{6,15}"))) throw ProfileValidationException(R.string.error_mobile)
        if (!email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) throw ProfileValidationException(R.string.error_email)
        if (address.isBlank()) throw ProfileValidationException(R.string.error_address_required)
        if (address.lines().size > 5) throw ProfileValidationException(R.string.error_address_lines)
        if (pincode.isBlank()) throw ProfileValidationException(R.string.error_pincode)
    }

    fun asShareText(phone: String, emailLabel: String, addressLabel: String, pincodeLabel: String): String = buildString {
        appendLine(fullName)
        appendLine(phone)
        appendLine(emailLabel)
        appendLine(addressLabel)
        appendLine(address.trim())
        append(pincodeLabel)
    }
}

class ProfileValidationException(@StringRes val messageResource: Int) : IllegalArgumentException()

interface UserProfileStore {
    val profile: Flow<LocalProfile?>
    suspend fun snapshot(): LocalProfile?
    suspend fun save(value: LocalProfile)
    suspend fun replace(value: LocalProfile?)
}

class LocalUserProfileRepository(context: Context) : UserProfileStore {
    private val appContext = context.applicationContext
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val profileKey = stringPreferencesKey("local_profile_json")

    override val profile: Flow<LocalProfile?> = appContext.profileDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            preferences[profileKey]?.let { raw -> runCatching { json.decodeFromString<LocalProfile>(raw) }.getOrNull() }
        }

    override suspend fun snapshot(): LocalProfile? = profile.first()

    override suspend fun save(value: LocalProfile) {
        value.validate()
        replace(value.copy(
            firstName = value.firstName.trim(),
            lastName = value.lastName.trim(),
            countryCode = value.countryCode.trim(),
            mobileNumber = value.mobileNumber.trim(),
            email = value.email.trim(),
            address = value.address.trim(),
            pincode = value.pincode.trim(),
        ))
    }

    override suspend fun replace(value: LocalProfile?) {
        appContext.profileDataStore.edit { preferences ->
            if (value == null) preferences.remove(profileKey)
            else preferences[profileKey] = json.encodeToString(value)
        }
    }
}

