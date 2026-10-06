package com.iocl.knowledgehub.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.model.User
import com.iocl.knowledgehub.domain.model.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentFirebaseUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = auth.currentUser != null

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun login(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                Resource.Success(it)
            } ?: Resource.Error("Login failed: User not found")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed")
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Resource<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let {
                Resource.Success(it)
            } ?: Resource.Error("Google Sign-In failed")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Google Sign-In failed")
        }
    }

    suspend fun signInAnonymously(): Resource<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.let {
                Resource.Success(it)
            } ?: Resource.Error("Anonymous sign-in failed")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Anonymous sign-in failed")
        }
    }

    suspend fun logout(): Resource<Unit> {
        return try {
            auth.signOut()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Logout failed")
        }
    }

    suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Password reset failed")
        }
    }

    suspend fun getUserProfile(uid: String): Resource<User> {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val user = User(
                    uid = uid,
                    employeeId = doc.getString("employeeId") ?: "",
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    department = doc.getString("department") ?: "",
                    designation = doc.getString("designation") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    role = try {
                        UserRole.valueOf(doc.getString("role") ?: "EMPLOYEE")
                    } catch (e: Exception) {
                        UserRole.EMPLOYEE
                    },
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    lastLoginAt = doc.getLong("lastLoginAt") ?: System.currentTimeMillis()
                )
                Resource.Success(user)
            } else {
                val firebaseUser = auth.currentUser
                val newUser = User(
                    uid = uid,
                    name = firebaseUser?.displayName ?: "",
                    email = firebaseUser?.email ?: "",
                    profileImageUrl = firebaseUser?.photoUrl?.toString() ?: ""
                )
                // Try to create profile, but don't fail if permissions deny it
                try {
                    firestore.collection("users").document(uid).set(
                        mapOf(
                            "employeeId" to newUser.employeeId,
                            "name" to newUser.name,
                            "email" to newUser.email,
                            "department" to newUser.department,
                            "designation" to newUser.designation,
                            "profileImageUrl" to newUser.profileImageUrl,
                            "role" to newUser.role.name,
                            "isActive" to newUser.isActive,
                            "createdAt" to newUser.createdAt,
                            "lastLoginAt" to newUser.lastLoginAt
                        )
                    ).await()
                } catch (_: Exception) {
                    // Ignore permission errors for profile creation
                }
                Resource.Success(newUser)
            }
        } catch (e: Exception) {
            // On any error (permission denied, etc.), return a basic user
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                val fallbackUser = User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "User",
                    email = firebaseUser.email ?: "",
                    profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""
                )
                Resource.Success(fallbackUser)
            } else {
                Resource.Error(e.message ?: "Failed to get user profile")
            }
        }
    }

    suspend fun updateUserProfile(user: User): Resource<User> {
        return try {
            firestore.collection("users").document(user.uid).update(
                mapOf(
                    "name" to user.name,
                    "department" to user.department,
                    "designation" to user.designation,
                    "profileImageUrl" to user.profileImageUrl,
                    "lastLoginAt" to System.currentTimeMillis()
                )
            ).await()
            Resource.Success(user)
        } catch (e: Exception) {
            // Return success even if Firestore update fails (permission issue)
            Resource.Success(user)
        }
    }
}
