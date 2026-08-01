package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.UserDao
import com.fitlife.ai.data.local.entity.UserEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao
) {
    val currentUser = supabase.auth.currentUserOrNull()

    fun isLoggedIn() = currentUser != null

    suspend fun restoreSession(): Boolean = withContext(Dispatchers.IO) {
        try {
            supabase.auth.awaitInitialization()
            supabase.auth.currentUserOrNull() != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signUp(email: String, password: String) = withContext(Dispatchers.IO) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun sendPasswordReset(email: String) = withContext(Dispatchers.IO) {
        supabase.auth.resetPasswordForEmail(email)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        supabase.auth.signOut()
    }

    suspend fun getUserId(): String? =
        runCatching { supabase.auth.currentUserOrNull()?.id }.getOrNull()
            ?: userDao.getFirstUserId()

    suspend fun getCurrentUserId(): String {
        val fromAuth = runCatching { supabase.auth.currentUserOrNull()?.id }.getOrNull()
        if (!fromAuth.isNullOrBlank()) return fromAuth
        return userDao.getFirstUserId()
            ?: throw IllegalStateException("Not authenticated")
    }

    fun observeUser(userId: String): Flow<UserEntity?> = userDao.getUser(userId)

    suspend fun getUserOnce(userId: String): UserEntity? = userDao.getUserOnce(userId)

    suspend fun loadUserFromSupabase(): UserEntity? = withContext(Dispatchers.IO) {
        val userId = getUserId() ?: return@withContext null
        try {
            val profile = supabase.from("user_profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<UserEntity>()
            if (profile != null) userDao.upsert(profile)
            profile
        } catch (e: Exception) { null }
    }

    suspend fun saveProfile(user: UserEntity) {
        userDao.upsert(user)
        try {
            withContext(Dispatchers.IO) {
                supabase.from("user_profiles").upsert(user)
            }
        } catch (_: Exception) { }
    }
}
