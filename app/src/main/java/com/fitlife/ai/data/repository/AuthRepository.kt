package com.fitlife.ai.data.repository

import com.fitlife.ai.data.local.dao.UserDao
import com.fitlife.ai.data.local.entity.UserEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao
) {
    val currentUser = supabase.auth.currentUserOrNull()

    fun isLoggedIn() = currentUser != null

    suspend fun signUp(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    suspend fun getUserId(): String? =
        supabase.auth.currentUserOrNull()?.id

    suspend fun getCurrentUserId(): String =
        supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not authenticated")

    fun observeUser(userId: String): Flow<UserEntity?> = userDao.getUser(userId)

    suspend fun loadUserFromSupabase(): UserEntity? {
        val userId = getUserId() ?: return null
        return try {
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
            supabase.from("user_profiles").upsert(user)
        } catch (_: Exception) { }
    }
}
