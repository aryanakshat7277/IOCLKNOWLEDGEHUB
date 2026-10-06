package com.iocl.knowledgehub.domain.repository

import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val isLoggedIn: Boolean
    suspend fun login(email: String, password: String): Resource<User>
    suspend fun signInWithGoogle(idToken: String): Resource<User>
    suspend fun signInAnonymously(): Resource<User>
    suspend fun logout(): Resource<Unit>
    suspend fun resetPassword(email: String): Resource<Unit>
    suspend fun getCurrentUser(): Resource<User>
    suspend fun updateProfile(user: User): Resource<User>
    fun saveRole(role: com.iocl.knowledgehub.domain.model.UserRole)
    fun getSavedRole(): com.iocl.knowledgehub.domain.model.UserRole
}
