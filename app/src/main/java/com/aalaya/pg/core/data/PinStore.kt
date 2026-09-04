package com.aalaya.pg.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aalaya.pg.core.domain.model.Role
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

private val Context.hamsaPinStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    by preferencesDataStore(name = "hamsa_pin")

/**
 * The staff app PIN (Revision 3 §12): a 4-digit code a staff member sets once
 * and enters to open the app. Client-side only — the PIN hash is stored on the
 * device, never sent to the server. This is the second factor behind the
 * emailed OTP, and it closes the SIM-swap gap the OTP alone can't: a hijacked
 * mailbox gets the code but not this PIN.
 */
@Singleton
class PinStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // The stored key keeps its original name. Renaming it would orphan every
    // PIN already set on a device and silently demand a new one on next launch.
    private val pinHashKey = stringPreferencesKey("owner_pin_hash")

    suspend fun hasPinNow(): Boolean =
        !context.hamsaPinStore.data.first()[pinHashKey].isNullOrBlank()

    suspend fun setPin(pin: String) {
        context.hamsaPinStore.edit { it[pinHashKey] = sha256(pin) }
    }

    suspend fun checkPin(pin: String): Boolean =
        context.hamsaPinStore.data.first()[pinHashKey] == sha256(pin)

    /** Wipe the stored PIN hash. Called on sign-out so a different staff member
     * on the same device isn't asked for (or compared against) the previous
     * user's PIN. */
    suspend fun clear() {
        context.hamsaPinStore.edit { it.remove(pinHashKey) }
    }

    /**
     * Whether this role is covered by the required-PIN rule.
     *
     * Every staff role, not just Owner: a manager can move residents between
     * beds, record cash and act on approvals, so a phone left unlocked on a
     * desk is the same risk in their hands as in the owner's. Residents and
     * prospects are not gated — they hold no one else's money or records.
     */
    fun requiresPin(role: Role): Boolean = role.isStaff

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
