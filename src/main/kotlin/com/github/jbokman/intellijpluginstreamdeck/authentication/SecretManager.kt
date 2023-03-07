package com.github.jbokman.intellijpluginstreamdeck.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.passwordSafe.impl.PasswordSafeImpl
import com.intellij.openapi.application.ApplicationManager
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class SecretManager {
    private val applicationManager = ApplicationManager.getApplication()
    private val service: PasswordSafe by lazy {
        applicationManager.getService(PasswordSafe::class.java) as PasswordSafeImpl
    }

    companion object {
        private const val USER_ID = "stream-deck-connector-user"
        private const val JWT_AUDIENCE = "stream-deck-intellij-plugin"
        private const val JWT_ISSUER = "intellij-stream-deck-connector-plugin"
        private val ENCODER = Base64.getEncoder()
        private val DECODER = Base64.getDecoder()
    }

    private val credentialAttributes = createCredentialAttributes()

    private var cachedSecret: ByteArray? = null

    private fun createCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(
            "SecretManager",
            "SecretManager.SecretKey.$USER_ID"
        )
    }

    private fun getAlgorithm(): Algorithm? {
        val secret = getSecret() ?: return null
        return Algorithm.HMAC256(secret)
    }

    fun generateNewToken(): String {
        generateSecret()
        val algorithm = getAlgorithm()
        return JWT.create()
            .withAudience(JWT_AUDIENCE)
            .withIssuer(JWT_ISSUER)
            .sign(algorithm!!)
    }

    fun getToken(): String? {
        val algorithm = getAlgorithm() ?: return null
        return JWT.create()
            .withAudience(JWT_AUDIENCE)
            .withIssuer(JWT_ISSUER)
            .sign(algorithm)
    }

    private fun generateSecret(): SecretKeySpec {
        val password = generatePassword().toString()
        val salt = generateSalt()
        val secretKey = generateSecretKey(password.toCharArray(), salt)
        val encodedSecret = ENCODER.encodeToString(secretKey.encoded)
        service.setPassword(credentialAttributes, encodedSecret)
        return secretKey
    }

    private fun getSecret(): ByteArray? {
        if (cachedSecret != null) {
            return cachedSecret
        }
        val secret = service.getPassword(credentialAttributes) ?: return null
        val decodedSecret = DECODER.decode(secret)
        cachedSecret = decodedSecret
        return decodedSecret
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun generatePassword(): ByteArray {
        val password = ByteArray(32)
        SecureRandom().nextBytes(password)
        return password
    }

    private fun generateSecretKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        val secretKeyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(secretKeyBytes, "AES")
    }

    fun getVerifier(): JWTVerifier? {
        val audience = "stream-deck-intellij-plugin"
        val issuer = "intellij-stream-deck-connector-plugin"
        val algorithm = getAlgorithm() ?: return null
        return JWT
            .require(algorithm)
            .withAudience(audience)
            .withIssuer(issuer)
            .build()
    }
}
