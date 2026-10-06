package com.iocl.knowledgehub.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.iocl.knowledgehub.data.remote.firebase.FirebaseAuthSource
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.model.User
import com.iocl.knowledgehub.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authSource: FirebaseAuthSource,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override val currentUser: Flow<User?> = authSource.observeAuthState().map { firebaseUser ->
        firebaseUser?.let {
            val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
            val googleName = googleAccount?.displayName
            val googleEmail = googleAccount?.email
            val googlePhotoUrl = googleAccount?.photoUrl?.toString()

            val savedRole = getSavedRole()
            
            when (val result = authSource.getUserProfile(it.uid)) {
                is Resource.Success -> {
                    val user = result.data.copy(role = savedRole) // Override with locally saved role
                    if (user.name.isEmpty() || user.name == "User" || user.name == "IOCL Employee" || user.name.contains("dummy")) {
                        user.copy(
                            name = googleName ?: googleEmail?.substringBefore("@") ?: user.name,
                            email = googleEmail ?: user.email,
                            profileImageUrl = googlePhotoUrl ?: user.profileImageUrl
                        )
                    } else {
                        user
                    }
                }
                else -> User(
                    uid = it.uid,
                    name = googleName ?: googleEmail?.substringBefore("@") ?: it.displayName ?: "User",
                    email = googleEmail ?: it.email ?: "",
                    profileImageUrl = googlePhotoUrl ?: it.photoUrl?.toString() ?: "",
                    role = savedRole
                )
            }
        }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.IO),
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    override fun saveRole(role: com.iocl.knowledgehub.domain.model.UserRole) {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("saved_role", role.name)
            .apply()
    }

    override fun getSavedRole(): com.iocl.knowledgehub.domain.model.UserRole {
        val roleStr = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("saved_role", "EMPLOYEE") ?: "EMPLOYEE"
        return try {
            com.iocl.knowledgehub.domain.model.UserRole.valueOf(roleStr)
        } catch (e: Exception) {
            com.iocl.knowledgehub.domain.model.UserRole.EMPLOYEE
        }
    }

    override val isLoggedIn: Boolean get() = authSource.isLoggedIn

    override suspend fun login(email: String, password: String): Resource<User> {
        return when (val result = authSource.login(email, password)) {
            is Resource.Success -> authSource.getUserProfile(result.data.uid)
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Resource<User> {
        return when (val result = authSource.signInWithGoogleCredential(idToken)) {
            is Resource.Success -> authSource.getUserProfile(result.data.uid)
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun signInAnonymously(): Resource<User> {
        return when (val result = authSource.signInAnonymously()) {
            is Resource.Success -> authSource.getUserProfile(result.data.uid)
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun logout(): Resource<Unit> {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("saved_role")
            .apply()
        return authSource.logout()
    }

    override suspend fun resetPassword(email: String): Resource<Unit> = authSource.resetPassword(email)

    override suspend fun getCurrentUser(): Resource<User> {
        val uid = authSource.currentFirebaseUser?.uid ?: return Resource.Error("Not logged in")
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        val googleName = googleAccount?.displayName
        val googleEmail = googleAccount?.email
        val googlePhotoUrl = googleAccount?.photoUrl?.toString()
        val savedRole = getSavedRole()

        return when (val result = authSource.getUserProfile(uid)) {
            is Resource.Success -> {
                val user = result.data.copy(role = savedRole)
                if (user.name.isEmpty() || user.name == "User" || user.name == "IOCL Employee") {
                    Resource.Success(
                        user.copy(
                            name = googleName ?: user.name,
                            email = googleEmail ?: user.email,
                            profileImageUrl = googlePhotoUrl ?: user.profileImageUrl
                        )
                    )
                } else {
                    Resource.Success(user)
                }
            }
            is Resource.Error -> {
                val firebaseUser = authSource.currentFirebaseUser
                if (firebaseUser != null) {
                    Resource.Success(
                        User(
                            uid = firebaseUser.uid,
                            name = googleName ?: firebaseUser.displayName ?: "User",
                            email = googleEmail ?: firebaseUser.email ?: "",
                            profileImageUrl = googlePhotoUrl ?: firebaseUser.photoUrl?.toString() ?: "",
                            role = savedRole
                        )
                    )
                } else {
                    Resource.Error(result.message)
                }
            }
            is Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun updateProfile(user: User): Resource<User> = authSource.updateUserProfile(user)
}
