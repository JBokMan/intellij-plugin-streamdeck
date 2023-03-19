package com.github.jbokman.intellijpluginstreamdeck.authentication

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.passwordSafe.impl.PasswordSafeImpl
import com.intellij.openapi.application.ApplicationManager
import io.ktor.server.auth.Principal
import io.ktor.server.auth.UserIdPrincipal
import java.security.MessageDigest
import java.security.SecureRandom
import javax.xml.bind.DatatypeConverter

class SecretManager {
    private val applicationManager = ApplicationManager.getApplication()
    private val service: PasswordSafe by lazy {
        applicationManager.getService(PasswordSafe::class.java) as PasswordSafeImpl
    }

    companion object {
        private const val USER_ID = "stream-deck-connector-user"
        private const val TOKEN_SALT = "green-dragon-12x43"
    }

    private val credentialAttributes = createCredentialAttributes()

    private var cachedToken: AuthenticationToken? = null

    private fun createCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(
            "SecretManager",
            "SecretManager.SecretKey.$USER_ID"
        )
    }

    fun validateToken(token: String): Principal? {
        if (token.isBlank()) return null
        val savedToken = getToken()
        return if (savedToken.value == token) {
            UserIdPrincipal(USER_ID)
        } else {
            null
        }
    }

    fun getToken(): AuthenticationToken {
        cachedToken?.let {
            return it
        }
        val tokenString = service.getPassword(credentialAttributes)
        val token: AuthenticationToken = if (tokenString == null) {
            generateNewToken()
        } else {
            AuthenticationToken(tokenString)
        }
        cachedToken = token
        return token
    }

    fun generateNewToken(): AuthenticationToken {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val token = DatatypeConverter.printHexBinary(bytes)
        val hashedToken = hashToken(token)
        service.setPassword(credentialAttributes, hashedToken)
        return AuthenticationToken(hashedToken)
    }

    private fun hashToken(token: String): String {
        val md = MessageDigest.getInstance("SHA-512")
        val saltedToken = "$TOKEN_SALT:${token}"
        val bytes = saltedToken.toByteArray(Charsets.UTF_8)
        val hashedBytes = md.digest(bytes)
        return DatatypeConverter.printHexBinary(hashedBytes)
    }
}