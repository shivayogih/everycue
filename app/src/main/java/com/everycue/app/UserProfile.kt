package com.everycue.app

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.everycue.core.security.TextCipher
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

    fun validationErrors(): ProfileValidationErrors {
        val namePattern = Regex("^[\\p{L}][\\p{L}\\p{M} .'-]{1,49}$")
        val normalizedMobile = mobileNumber.trim()
        return ProfileValidationErrors(
            firstName = if (firstName.trim().matches(namePattern)) null else R.string.error_first_name,
            lastName = if (lastName.trim().matches(namePattern)) null else R.string.error_last_name,
            countryCode = if (countryCode.trim().matches(Regex("\\+[1-9][0-9]{0,2}"))) null else R.string.error_country_code,
            mobile = if (
                normalizedMobile.matches(Regex("[0-9]{7,15}")) &&
                normalizedMobile.toSet().size > 1
            ) null else R.string.error_mobile,
            email = if (
                email.length <= 254 && email.trim().matches(Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"))
            ) null else R.string.error_email,
            address = when {
                address.trim().length < 8 -> R.string.error_address_required
                address.length > 300 -> R.string.error_address_length
                address.lines().size > 5 -> R.string.error_address_lines
                address.none(Char::isLetterOrDigit) -> R.string.error_address_required
                else -> null
            },
            pincode = if (pincode.trim().matches(Regex("[0-9]{4,10}"))) null else R.string.error_pincode,
        )
    }

    fun validate() {
        validationErrors().firstError()?.let { throw ProfileValidationException(it) }
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

data class ProfileValidationErrors(
    @StringRes val firstName: Int? = null,
    @StringRes val lastName: Int? = null,
    @StringRes val countryCode: Int? = null,
    @StringRes val mobile: Int? = null,
    @StringRes val email: Int? = null,
    @StringRes val address: Int? = null,
    @StringRes val pincode: Int? = null,
) {
    val isValid: Boolean get() = firstError() == null
    fun firstError(): Int? = firstName ?: lastName ?: countryCode ?: mobile ?: email ?: address ?: pincode
}

class ProfileValidationException(@StringRes val messageResource: Int) : IllegalArgumentException()

interface UserProfileStore {
    val profile: Flow<LocalProfile?>
    suspend fun snapshot(): LocalProfile?
    suspend fun save(value: LocalProfile)
    suspend fun replace(value: LocalProfile?)
}

class LocalUserProfileRepository(context: Context, private val cipher: TextCipher) : UserProfileStore {
    private val appContext = context.applicationContext
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val profileKey = stringPreferencesKey("local_profile_json")

    override val profile: Flow<LocalProfile?> = appContext.profileDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            preferences[profileKey]?.let { raw -> runCatching { json.decodeFromString<LocalProfile>(cipher.decrypt(raw)) }.getOrNull() }
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
        value?.validate()
        appContext.profileDataStore.edit { preferences ->
            if (value == null) preferences.remove(profileKey)
            else preferences[profileKey] = cipher.encrypt(json.encodeToString(value))
        }
    }
}
