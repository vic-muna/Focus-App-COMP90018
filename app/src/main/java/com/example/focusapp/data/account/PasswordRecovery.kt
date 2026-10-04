package com.example.focusapp.data.account

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * PasswordRecovery
 * -------------------
 * [Claude, 2026-10-04] No-email "forgot password", for an app whose accounts have no real
 * email address to send a reset link to (see AccountManager's own doc comment).
 *
 * The trick: don't try to RESET the Firebase Auth password directly - that needs privileged
 * (Admin SDK) access to someone else's account, which only a backend can do, and this app has
 * no backend. Instead, RECOVER the original password: at account creation, the password is
 * encrypted with a key derived from the answer to a security question the user picked, and the
 * encrypted blob is saved to the cloud (see AccountManager.createAccount). To recover, the user
 * answers the same question; if the answer is right, decryption yields the original password
 * back, and the normal sign-in flow (signInWithEmailAndPassword, then Firebase's own
 * updatePassword() once authenticated) takes it from there - entirely within what a signed-out
 * client is actually allowed to do.
 *
 * Security tradeoff, stated plainly: this is only as strong as the security answer. Unlike a
 * real password-reset email (which proves the requester controls the account's inbox), anyone
 * who can guess or look up the answer can recover the account - the classic weakness of
 * security questions in general. Reasonable for a class project; swap for email or a second
 * sign-in factor before this app has data worth protecting from a determined attacker.
 *
 * [salt] must be freshly random per account (stored alongside the ciphertext, not secret) -
 * reusing one salt across every account would let an attacker precompute a single table of
 * guesses for a common question ("What's your pet's name?") and try it against everyone at
 * once. [iv] must never be reused with the same key for GCM's security guarantee to hold -
 * generated fresh on every single encrypt call, even a re-encrypt of the same password.
 */
internal object PasswordRecovery {

    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    data class EncryptedBlob(val ciphertextBase64: String, val ivBase64: String, val saltBase64: String)

    /** Encrypts [password] under a key derived from [answer] (normalized the same way on both
     *  encrypt and decrypt - see [normalizeAnswer]). Generates a fresh salt and IV each call. */
    fun encrypt(password: String, answer: String): EncryptedBlob {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(answer, salt)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        return EncryptedBlob(
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
        )
    }

    /** Reverses [encrypt]. Returns null for a wrong [answer] (GCM's authentication tag fails to
     *  verify - this is the actual "is the answer correct?" check; there's no separate
     *  right/wrong comparison anywhere) rather than throwing, so a wrong guess is just another
     *  normal result to show an error for, not a crash. */
    fun decrypt(blob: EncryptedBlob, answer: String): String? {
        return try {
            val salt = Base64.decode(blob.saltBase64, Base64.NO_WRAP)
            val iv = Base64.decode(blob.ivBase64, Base64.NO_WRAP)
            val ciphertext = Base64.decode(blob.ciphertextBase64, Base64.NO_WRAP)
            val key = deriveKey(answer, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            // Wrong answer (bad GCM tag) and corrupt/missing data both land here on purpose -
            // from the caller's point of view both just mean "couldn't recover the password".
            null
        }
    }

    /** Case/whitespace-insensitive, the same reasoning as AccountManager.normalizeUsername():
     *  "Fluffy" and "fluffy " should both work, not just the exact string typed at sign-up. */
    fun normalizeAnswer(input: String): String = input.trim().lowercase()

    private fun deriveKey(answer: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(normalizeAnswer(answer).toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
